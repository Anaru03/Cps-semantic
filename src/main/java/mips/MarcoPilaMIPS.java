package mips;

import tac.RegistroActivacion;

import java.util.*;
import java.util.function.Predicate;

/**
 * Marco de pila (registro de activación) de UNA función, traducido a bytes MIPS.
 *
 * Convención (palabra = 4 bytes, $sp alineado a 8):
 *
 *   alto  |  args extra (k >= 4)  |  <- caller los deja en 0($sp),4($sp).. justo antes de jal
 *         +-----------------------+  <- $fp (= $sp del caller al llamar)
 *         | $ra guardado          |  -4($fp)
 *         | $fp del caller        |  -8($fp)   (enlace dinámico)
 *         | slot 0 .. slot n-1    |  -12($fp) ... (parámetros, locales, temporales/spills)
 *         | $s usados guardados   |  debajo de los slots
 *   bajo  +-----------------------+  <- $sp
 *
 * Los parámetros se copian en el prólogo a su slot local ($a0-$a3 y pila), por lo que
 * todo acceso a variables es uniforme: desplazamiento($fp).
 * Los slots son LÓGICOS; aquí se convierten a bytes. Relación con RegistroActivacion:
 *   offset lógico = CABECERA (3) + i  ->  slot i  ->  -(8 + 4*(i+1))($fp)
 *   ENLACE_DINAMICO   (offset 0) -> -8($fp)  ($fp del caller)
 *   DIRECCION_RETORNO (offset 1) -> -4($fp)  ($ra)
 *   VALOR_RETORNO     (offset 2) -> registro $v0 (no ocupa palabra en el marco)
 */
public class MarcoPilaMIPS {
    public static final int PALABRA = 4;
    public static final int REGS_ARGUMENTO = 4;
    private static final int AREA_FIJA = 2 * PALABRA; // $ra + $fp

    private final String nombre;
    private final boolean esMain;
    private final List<String> parametros;
    private final Set<String> globales;           // compartido entre todos los marcos
    private final Predicate<String> esTemporal;
    private final Map<String, Integer> slots = new LinkedHashMap<>();

    /**
     * @param esMain   el código de nivel superior: sus variables no temporales son globales (.data)
     * @param globales conjunto compartido de globales (se completa solo)
     */
    public MarcoPilaMIPS(String nombre, boolean esMain, List<String> parametros,
                         Collection<String> locales, Set<String> globales,
                         Predicate<String> esTemporal) {
        this.nombre = nombre;
        this.esMain = esMain;
        this.parametros = new ArrayList<>(parametros);
        this.globales = globales;
        this.esTemporal = esTemporal;
        for (String p : this.parametros) reservarSlot(p);
        for (String l : locales) {
            if (esMain && !esTemporal.test(l)) globales.add(l);
            else reservarSlot(l);
        }
    }

    /** Marco de una función a partir del layout del Proyecto 2; los slots siguen el orden de sus posiciones. */
    public MarcoPilaMIPS(RegistroActivacion layout, List<String> parametros,
                         Set<String> globales, Predicate<String> esTemporal) {
        this.nombre = layout.funcion();
        this.esMain = false;
        this.parametros = new ArrayList<>(parametros);
        this.globales = globales;
        this.esTemporal = esTemporal;
        for (RegistroActivacion.Posicion p : layout.posiciones()) {
            reservarSlot(p.operando());
            if (slots.get(p.operando()) != p.offset() - RegistroActivacion.CABECERA) {
                throw new GeneracionMIPSException("Layout inconsistente en " + nombre);
            }
        }
        for (String p : this.parametros) {
            if (!slots.containsKey(p)) throw new GeneracionMIPSException("Parámetro sin posición: " + p);
        }
    }

    // ---------- etiquetas ----------
    /** Los nombres del TAC ("Animal.hablar", "%global.0.x") no son etiquetas MIPS válidas. */
    private static String limpiar(String n) { return n.replaceAll("[^A-Za-z0-9_]", "_"); }
    public static String etiquetaDeFuncion(String nombre) { return "func_" + limpiar(nombre); }
    public static String etiquetaGlobal(String nombre)    { return "g_" + limpiar(nombre); }
    public String etiquetaFuncion() { return esMain ? "main" : etiquetaDeFuncion(nombre); }
    public String etiquetaEpilogo() { return etiquetaFuncion() + "_epilogo"; }
    public boolean esMain() { return esMain; }
    public String nombre()  { return nombre; }

    // ---------- ubicación de variables ----------
    private void reservarSlot(String n) {
        if (!slots.containsKey(n)) slots.put(n, slots.size());
    }

    /**
     * true si la variable vive en .data (sin efectos colaterales). Según el TAC: los temporales ("tN")
     * y los operandos "%f.k.x" de un layout van al marco; "%global.k.x" y los nombres sin '%' son globales.
     */
    public boolean esGlobal(String n) {
        if (slots.containsKey(n) || esTemporal.test(n)) return false;
        return n.startsWith("%global.") || !n.startsWith("%");
    }

    /** Operando de memoria MIPS ("-12($fp)" o "g_x"). Reserva slot al primer uso de un temporal. */
    public String ubicacion(String n) {
        Integer s = slots.get(n);
        if (s != null) return desplazamiento(s) + "($fp)";
        if (esTemporal.test(n)) {
            reservarSlot(n);
            return ubicacion(n);
        }
        if (esGlobal(n)) {
            globales.add(n);
            return etiquetaGlobal(n);
        }
        throw new GeneracionMIPSException("Variable no declarada en la función '" + nombre + "': " + n);
    }

    /** Slot lógico -> byte relativo a $fp. */
    public int desplazamiento(int slot) {
        return -(AREA_FIJA + PALABRA * (slot + 1));
    }

    public int numSlots() { return slots.size(); }

    /** Tamaño total del marco, alineado a 8 bytes. */
    public int tamanoMarco(int cantidadSGuardados) {
        int bytes = AREA_FIJA + PALABRA * slots.size() + PALABRA * cantidadSGuardados;
        return (bytes + 7) & ~7;
    }

    private int desplazamientoS(int indice) {
        return -(AREA_FIJA + PALABRA * slots.size() + PALABRA * (indice + 1));
    }

    // ---------- prólogo / epílogo ----------
    /** Llamar al terminar la función, cuando ya se conocen slots y $s usados. */
    public List<String> prologo(Collection<String> sUsados) {
        List<String> s = new ArrayList<>(new TreeSet<>(sUsados));
        int marco = tamanoMarco(s.size());
        List<String> out = new ArrayList<>();
        out.add(etiquetaFuncion() + ":");
        out.add("addiu $sp, $sp, -" + marco);
        out.add("sw $ra, " + (marco - PALABRA) + "($sp)");
        out.add("sw $fp, " + (marco - 2 * PALABRA) + "($sp)");
        out.add("addiu $fp, $sp, " + marco);
        for (int i = 0; i < s.size(); i++) out.add("sw " + s.get(i) + ", " + desplazamientoS(i) + "($fp)");
        for (int i = 0; i < parametros.size(); i++) {
            int off = desplazamiento(slots.get(parametros.get(i)));
            if (i < REGS_ARGUMENTO) {
                out.add("sw $a" + i + ", " + off + "($fp)");
            } else {
                out.add("lw $v1, " + (i - REGS_ARGUMENTO) * PALABRA + "($fp)");
                out.add("sw $v1, " + off + "($fp)");
            }
        }
        return out;
    }

    public List<String> epilogo(Collection<String> sUsados) {
        List<String> s = new ArrayList<>(new TreeSet<>(sUsados));
        List<String> out = new ArrayList<>();
        out.add(etiquetaEpilogo() + ":");
        if (esMain) {
            out.add("li $v0, 10");
            out.add("syscall");
            return out;
        }
        for (int i = 0; i < s.size(); i++) out.add("lw " + s.get(i) + ", " + desplazamientoS(i) + "($fp)");
        out.add("lw $ra, -" + PALABRA + "($fp)");
        out.add("move $sp, $fp");
        out.add("lw $fp, -" + 2 * PALABRA + "($fp)");
        out.add("jr $ra");
        return out;
    }
}
