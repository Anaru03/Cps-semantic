package mips;

import tac.RegistroActivacion;

import java.util.*;
import java.util.function.Predicate;

/**
 * Secuencias de llamada/retorno, funciones y recursividad sobre MarcoPilaMIPS + AsignadorRegistros.
 *
 * Mapeo TAC -> API:
 *   FUNCION f(params)  -> iniciarFuncion(f, params, locales)      (iniciarMain para código global)
 *   PARAMETRO          -> ya va en la lista de params (copiados a slots en el prólogo)
 *   ARGUMENTO x        -> argumento(x)           (en orden de aparición: arg0, arg1, ...)
 *   LLAMADA d,f,n      -> llamada(d, etiquetaDe(f), n)
 *   RETORNO x          -> retorno(x)
 *   FIN_FUNCION        -> finFuncion()
 *
 * Convención de llamada:
 *   args 0-3 en $a0-$a3; args 4.. en pila (0($sp), 4($sp).. al hacer jal); retorno en $v0;
 *   $s0-$s7, $ra, $fp preservados por el callee; $t0-$t9 se sincronizan/liberan antes de jal.
 */
public class GeneradorFuncionesMIPS {
    private final Predicate<String> esTemporal;
    private final Set<String> globales = new TreeSet<>();
    private final List<String> datos = new ArrayList<>();
    private final List<String> textoMain = new ArrayList<>();
    private final List<String> textoFunciones = new ArrayList<>();
    private final List<String> argumentosPendientes = new ArrayList<>();
    private boolean argumentosInvertidos = false;
    private final Map<String, String> cadenas = new LinkedHashMap<>();

    private MarcoPilaMIPS marco;
    private AsignadorRegistros regs;
    private List<String> cuerpo;

    public GeneradorFuncionesMIPS() {
        this(n -> n.matches("t\\d+"));
    }

    public GeneradorFuncionesMIPS(Predicate<String> esTemporal) {
        this.esTemporal = esTemporal;
    }

    public static String etiquetaDe(String funcion) { return MarcoPilaMIPS.etiquetaDeFuncion(funcion); }

    /** Si el TAC emite los ARGUMENTO en orden inverso (último primero), activar. */
    public void setArgumentosInvertidos(boolean v) { this.argumentosInvertidos = v; }

    public void declararGlobales(Collection<String> nombres) { globales.addAll(nombres); }
    public void agregarDato(String lineaData) { datos.add(lineaData); }
    public AsignadorRegistros registros() { return regs; }

    // ---------- ciclo de vida de una función ----------
    public void iniciarMain(Collection<String> locales) {
        iniciar("main", true, List.of(), locales);
    }

    public void iniciarFuncion(String nombre, List<String> parametros, Collection<String> locales) {
        iniciar(nombre, false, parametros, locales);
    }

    /** Función del TAC: layout del Proyecto 2 y operandos de sus PARAMETRO en orden de índice. */
    public void iniciarFuncion(RegistroActivacion layout, List<String> parametros) {
        verificarSinAnidar(layout.funcion());
        activar(new MarcoPilaMIPS(layout, parametros, globales, esTemporal));
    }

    private void iniciar(String nombre, boolean esMain, List<String> params, Collection<String> locales) {
        verificarSinAnidar(nombre);
        activar(new MarcoPilaMIPS(nombre, esMain, params, locales, globales, esTemporal));
    }

    private void verificarSinAnidar(String nombre) {
        if (marco != null) {
            throw new GeneracionMIPSException("Funciones anidadas no soportadas: '" + nombre
                    + "' dentro de '" + marco.nombre() + "'");
        }
    }

    private void activar(MarcoPilaMIPS nuevo) {
        marco = nuevo;
        cuerpo = new ArrayList<>();
        regs = new AsignadorRegistros(marco, cuerpo::add, esTemporal);
        regs.usarCadenas(this::internar);
        argumentosPendientes.clear();
    }

    /** Registra un literal de cadena ("hola") en .data; devuelve su etiqueta. */
    public String internar(String literal) {
        return cadenas.computeIfAbsent(literal, l -> {
            String etiqueta = "str_" + cadenas.size();
            datos.add(etiqueta + ": .asciiz " + l);
            return etiqueta;
        });
    }

    public void finFuncion() {
        requerirFuncion();
        regs.invalidar();
        List<String> out = new ArrayList<>(marco.prologo(regs.sUsados()));
        out.addAll(cuerpo);
        out.addAll(marco.epilogo(regs.sUsados()));
        (marco.esMain() ? textoMain : textoFunciones).addAll(out);
        if (!argumentosPendientes.isEmpty()) {
            throw new GeneracionMIPSException("ARGUMENTO sin LLAMADA en '" + marco.nombre() + "'");
        }
        marco = null;
        regs = null;
        cuerpo = null;
    }

    // ---------- llamadas ----------
    public void argumento(String operando) {
        requerirFuncion();
        argumentosPendientes.add(operando);
    }

    public void llamada(String destino, String etiquetaFuncion, int numArgs) {
        requerirFuncion();
        if (argumentosPendientes.size() < numArgs) {
            throw new GeneracionMIPSException("LLAMADA a " + etiquetaFuncion + " espera " + numArgs
                    + " argumentos y solo hay " + argumentosPendientes.size());
        }
        int desde = argumentosPendientes.size() - numArgs;
        List<String> args = new ArrayList<>(argumentosPendientes.subList(desde, argumentosPendientes.size()));
        argumentosPendientes.subList(desde, argumentosPendientes.size()).clear();
        if (argumentosInvertidos) Collections.reverse(args);

        int extra = Math.max(0, numArgs - MarcoPilaMIPS.REGS_ARGUMENTO);
        if (extra > 0) emitir("addiu $sp, $sp, -" + extra * MarcoPilaMIPS.PALABRA);

        for (int k = 0; k < numArgs; k++) {
            String a = args.get(k);
            if (k < MarcoPilaMIPS.REGS_ARGUMENTO) {
                if (AsignadorRegistros.esLiteral(a)) {
                    emitir("li $a" + k + ", " + AsignadorRegistros.valorLiteral(a));
                } else {
                    emitir("move $a" + k + ", " + regs.getReg(a));
                }
            } else {
                String r = regs.getReg(a);
                emitir("sw " + r + ", " + (k - MarcoPilaMIPS.REGS_ARGUMENTO) * MarcoPilaMIPS.PALABRA + "($sp)");
            }
            regs.liberarBloqueos();
        }

        regs.preLlamada();
        emitir("jal " + etiquetaFuncion);
        if (extra > 0) emitir("addiu $sp, $sp, " + extra * MarcoPilaMIPS.PALABRA);

        if (destino != null) {
            String rd = regs.getRegDestino(destino);
            emitir("move " + rd + ", $v0");
            regs.liberarBloqueos();
        }
    }

    public void retorno(String operando) {
        requerirFuncion();
        if (operando != null) {
            if (AsignadorRegistros.esLiteral(operando)) {
                emitir("li $v0, " + AsignadorRegistros.valorLiteral(operando));
            } else {
                emitir("move $v0, " + regs.getReg(operando));
            }
            regs.liberarBloqueos();
        }
        emitir("j " + marco.etiquetaEpilogo());
        regs.descartar(); // lo que sigue es inalcanzable hasta la próxima etiqueta
    }

    // ---------- apoyo mínimo (Ruth es dueña del traductor completo) ----------
    public void etiqueta(String nombre) {
        requerirFuncion();
        regs.invalidar();
        emitir(nombre + ":");
    }

    public void saltoIncondicional(String etiqueta) {
        requerirFuncion();
        regs.sincronizar();
        emitir("j " + etiqueta);
        regs.descartar();
    }

    public void saltoSi(String cmp, String a, String b, String etiqueta) {
        requerirFuncion();
        String ra = regs.getReg(a), rb = regs.getReg(b);
        regs.sincronizar();
        String ins;
        switch (cmp) {
            case "==": ins = "beq"; break;
            case "!=": ins = "bne"; break;
            case "<":  ins = "blt"; break;
            case ">":  ins = "bgt"; break;
            case "<=": ins = "ble"; break;
            case ">=": ins = "bge"; break;
            default: throw new GeneracionMIPSException("Comparación no soportada: " + cmp);
        }
        emitir(ins + " " + ra + ", " + rb + ", " + etiqueta);
        regs.liberarBloqueos();
    }

    /** if cond goto etiqueta (cond es 0/1 o cualquier valor: distinto de cero es verdadero). */
    public void saltoSiVerdadero(String cond, String etiqueta) {
        requerirFuncion();
        String rc = regs.getReg(cond);
        regs.sincronizar();
        emitir("bne " + rc + ", $zero, " + etiqueta);
        regs.liberarBloqueos();
    }

    public void unaria(String op, String dst, String a) {
        requerirFuncion();
        String ra = regs.getReg(a), rd = regs.getRegDestino(dst);
        switch (op) {
            case "-": emitir("subu " + rd + ", $zero, " + ra); break;
            case "!": emitir("seq " + rd + ", " + ra + ", $zero"); break;
            default: throw new GeneracionMIPSException("Operador unario no soportado: " + op);
        }
        regs.liberarBloqueos();
    }

    public void copiar(String dst, String src) {
        requerirFuncion();
        if (AsignadorRegistros.esLiteral(src)) {
            emitir("li " + regs.getRegDestino(dst) + ", " + AsignadorRegistros.valorLiteral(src));
        } else {
            String rs = regs.getReg(src);
            emitir("move " + regs.getRegDestino(dst) + ", " + rs);
        }
        regs.liberarBloqueos();
    }

    public void binaria(String op, String dst, String a, String b) {
        requerirFuncion();
        String ra = regs.getReg(a), rb = regs.getReg(b), rd = regs.getRegDestino(dst);
        switch (op) {
            case "+":  emitir("addu " + rd + ", " + ra + ", " + rb); break;
            case "-":  emitir("subu " + rd + ", " + ra + ", " + rb); break;
            case "*":  emitir("mul " + rd + ", " + ra + ", " + rb); break;
            case "/":  emitir("div " + ra + ", " + rb); emitir("mflo " + rd); break;
            case "%":  emitir("div " + ra + ", " + rb); emitir("mfhi " + rd); break;
            case "<":  emitir("slt " + rd + ", " + ra + ", " + rb); break;
            case ">":  emitir("sgt " + rd + ", " + ra + ", " + rb); break;
            case "<=": emitir("sle " + rd + ", " + ra + ", " + rb); break;
            case ">=": emitir("sge " + rd + ", " + ra + ", " + rb); break;
            case "==": emitir("seq " + rd + ", " + ra + ", " + rb); break;
            case "!=": emitir("sne " + rd + ", " + ra + ", " + rb); break;
            case "&&": emitir("and " + rd + ", " + ra + ", " + rb); break;
            case "||": emitir("or " + rd + ", " + ra + ", " + rb); break;
            default: throw new GeneracionMIPSException("Operador no soportado: " + op);
        }
        regs.liberarBloqueos();
    }

    /** print: cadenas con syscall 4, resto como entero; siempre con salto de línea. */
    public void imprimir(String operando) {
        requerirFuncion();
        if (AsignadorRegistros.esCadena(operando)) {
            emitir("la $a0, " + internar(operando));
            emitir("li $v0, 4");
            emitir("syscall");
            emitir("li $a0, 10");
            emitir("li $v0, 11");
            emitir("syscall");
        } else {
            imprimirEntero(operando);
        }
    }

    /** print(entero) con salto de línea (syscalls 1 y 11 de MARS). */
    public void imprimirEntero(String operando) {
        requerirFuncion();
        if (AsignadorRegistros.esLiteral(operando)) {
            emitir("li $a0, " + AsignadorRegistros.valorLiteral(operando));
        } else {
            emitir("move $a0, " + regs.getReg(operando));
        }
        regs.liberarBloqueos();
        emitir("li $v0, 1");
        emitir("syscall");
        emitir("li $a0, 10");
        emitir("li $v0, 11");
        emitir("syscall");
    }

    public void emitir(String linea) {
        requerirFuncion();
        cuerpo.add(linea);
    }

    // ---------- salida ----------
    public String ensamblar() {
        if (marco != null) throw new GeneracionMIPSException("Función sin cerrar: " + marco.nombre());
        if (textoMain.isEmpty()) throw new GeneracionMIPSException("Falta el punto de entrada 'main'");
        StringBuilder sb = new StringBuilder();
        sb.append(".data\n");
        for (String g : globales) sb.append(MarcoPilaMIPS.etiquetaGlobal(g)).append(": .word 0\n");
        for (String d : datos) sb.append(d).append('\n');
        sb.append("\n.text\n.globl main\n");
        volcar(sb, textoMain);
        volcar(sb, textoFunciones);
        return sb.toString();
    }

    private static void volcar(StringBuilder sb, List<String> lineas) {
        for (String l : lineas) {
            boolean sinSangria = l.endsWith(":");
            if (sinSangria && !l.equals(lineas.get(0))) sb.append('\n');
            sb.append(sinSangria ? "" : "    ").append(l).append('\n');
        }
        sb.append('\n');
    }

    private void requerirFuncion() {
        if (marco == null) throw new GeneracionMIPSException("Instrucción fuera de una función");
    }
}
