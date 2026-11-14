package mips;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Asignación de registros con getReg(): descriptores de registro y de dirección,
 * derrame (spill) por LRU y escritura diferida (dirty bits).
 *
 * Reglas:
 *  - Temporales y globales prefieren $t0-$t9 (caller-saved); locales/parámetros prefieren
 *    $s0-$s7 (callee-saved, sobreviven a las llamadas). Si una clase se llena se usa la otra.
 *  - Antes de saltos, etiquetas y llamadas el estado se sincroniza con memoria, de modo que
 *    en cualquier entrada a un bloque básico "todo está en memoria".
 *  - Un uso de getReg()/getRegDestino() bloquea el registro hasta liberarBloqueos() (fin de la
 *    instrucción TAC), para que traducir "x = y + z" no desaloje y ni z.
 */
public class AsignadorRegistros {
    public static final List<String> REG_T =
            List.of("$t0", "$t1", "$t2", "$t3", "$t4", "$t5", "$t6", "$t7", "$t8", "$t9");
    public static final List<String> REG_S =
            List.of("$s0", "$s1", "$s2", "$s3", "$s4", "$s5", "$s6", "$s7");

    private final MarcoPilaMIPS marco;
    private final Consumer<String> emisor;
    private final Predicate<String> esTemporal;

    private final Map<String, String> varAReg = new HashMap<>();
    private final Map<String, String> regAVar = new HashMap<>();
    private final Set<String> sucios = new HashSet<>();
    private final Map<String, Long> ultimoUso = new HashMap<>();   // registro -> reloj
    private final Set<String> bloqueados = new HashSet<>();
    private final Set<String> literales = new HashSet<>();         // pseudo-variables "#litN"
    private final Set<String> sUsados = new TreeSet<>();
    private Function<String, String> internarCadena = c -> {
        throw new GeneracionMIPSException("Literal de cadena sin tabla de cadenas: " + c);
    };
    private long reloj = 0;
    private int contLiterales = 0;
    private int derrames = 0;

    public AsignadorRegistros(MarcoPilaMIPS marco, Consumer<String> emisor, Predicate<String> esTemporal) {
        this.marco = marco;
        this.emisor = emisor;
        this.esTemporal = esTemporal;
    }

    /** Función que registra una cadena en .data y devuelve su etiqueta. */
    public void usarCadenas(Function<String, String> internar) { this.internarCadena = internar; }

    // ---------- literales ----------
    public static boolean esCadena(String op) { return op.length() >= 2 && op.startsWith("\"") && op.endsWith("\""); }

    public static boolean esLiteral(String op) {
        return op.matches("-?\\d+") || op.equals("true") || op.equals("false") || op.equals("null");
    }

    public static String valorLiteral(String op) {
        switch (op) {
            case "true":  return "1";
            case "false":
            case "null":  return "0";
            default:      return op;
        }
    }

    // ---------- API principal ----------

    /** getReg() para un operando FUENTE: devuelve un registro que contiene su valor. */
    public String getReg(String op) {
        if (esCadena(op)) {
            String clave = "#lit" + (contLiterales++);
            String r = reservar(clave);
            emisor.accept("la " + r + ", " + internarCadena.apply(op));
            literales.add(clave);
            bloqueados.add(r);
            return r;
        }
        if (esLiteral(op)) {
            String v = valorLiteral(op);
            if (v.equals("0")) return "$zero";
            String clave = "#lit" + (contLiterales++);
            String r = reservar(clave);
            emisor.accept("li " + r + ", " + v);
            literales.add(clave);
            bloqueados.add(r);
            return r;
        }
        String r = varAReg.get(op);
        if (r == null) {
            r = reservar(op);
            emisor.accept("lw " + r + ", " + marco.ubicacion(op));
        } else {
            toca(r);
        }
        bloqueados.add(r);
        return r;
    }

    /** getReg() para el DESTINO: no carga el valor anterior; lo marca sucio. */
    public String getRegDestino(String dst) {
        String r = varAReg.get(dst);
        if (r == null) r = reservar(dst); else toca(r);
        bloqueados.add(r);
        sucios.add(dst);
        return r;
    }

    /** Fin de la instrucción TAC: desbloquea y descarta los registros de literales. */
    public void liberarBloqueos() {
        bloqueados.clear();
        for (String clave : literales) {
            String r = varAReg.remove(clave);
            if (r != null) regAVar.remove(r);
        }
        literales.clear();
    }

    /** Última lectura de un temporal: su registro queda libre sin escribirlo a memoria. */
    public void liberar(String var) {
        String r = varAReg.remove(var);
        if (r != null) {
            regAVar.remove(r);
            sucios.remove(var);
        }
    }

    /** Escribe a memoria los valores sucios (los registros siguen asignados). */
    public void sincronizar() {
        for (String v : new TreeSet<>(sucios)) {
            emisor.accept("sw " + varAReg.get(v) + ", " + marco.ubicacion(v));
        }
        sucios.clear();
    }

    /** sincronizar() + olvidar todas las asignaciones (entrada a etiqueta / fin de bloque). */
    public void invalidar() {
        liberarBloqueos();
        sincronizar();
        descartar();
    }

    /** Olvida todo SIN escribir (código muerto tras return / salto incondicional ya sincronizado). */
    public void descartar() {
        varAReg.clear();
        regAVar.clear();
        sucios.clear();
        ultimoUso.clear();
        bloqueados.clear();
        literales.clear();
    }

    /**
     * Antes de jal: sincroniza y libera los $t y las globales (el callee puede modificarlas).
     * Los $s con locales se conservan porque el callee los preserva.
     */
    public void preLlamada() {
        liberarBloqueos();
        sincronizar();
        for (String v : new ArrayList<>(varAReg.keySet())) {
            String r = varAReg.get(v);
            if (r.startsWith("$t") || marco.esGlobal(v)) liberar(v);
        }
    }

    // ---------- consultas (pruebas / prólogo) ----------
    public Set<String> sUsados() { return Collections.unmodifiableSet(sUsados); }
    public int derrames() { return derrames; }
    public Optional<String> registroDe(String var) { return Optional.ofNullable(varAReg.get(var)); }
    public boolean estaSucio(String var) { return sucios.contains(var); }

    // ---------- internos ----------
    private boolean prefiereS(String clave) {
        return !clave.startsWith("#") && !esTemporal.test(clave) && !marco.esGlobal(clave);
    }

    private String reservar(String clave) {
        List<String> orden = new ArrayList<>();
        if (prefiereS(clave)) { orden.addAll(REG_S); orden.addAll(REG_T); }
        else                  { orden.addAll(REG_T); orden.addAll(REG_S); }
        for (String r : orden) {
            if (!regAVar.containsKey(r) && !bloqueados.contains(r)) {
                asignar(clave, r);
                return r;
            }
        }
        String r = derramar();
        asignar(clave, r);
        return r;
    }

    /** Desaloja el registro menos recientemente usado y no bloqueado. */
    private String derramar() {
        String mejor = null;
        long min = Long.MAX_VALUE;
        for (String r : regAVar.keySet()) {
            if (bloqueados.contains(r)) continue;
            long u = ultimoUso.getOrDefault(r, 0L);
            if (u < min) { min = u; mejor = r; }
        }
        if (mejor == null) {
            throw new GeneracionMIPSException("Sin registros disponibles: demasiados operandos simultáneos");
        }
        String v = regAVar.get(mejor);
        if (sucios.remove(v)) emisor.accept("sw " + mejor + ", " + marco.ubicacion(v));
        varAReg.remove(v);
        regAVar.remove(mejor);
        derrames++;
        return mejor;
    }

    private void asignar(String clave, String r) {
        varAReg.put(clave, r);
        regAVar.put(r, clave);
        if (r.startsWith("$s")) sUsados.add(r);
        toca(r);
    }

    private void toca(String r) { ultimoUso.put(r, ++reloj); }
}
