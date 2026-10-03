package tac;

import compiler.ResultadoCompilacion;
import java.util.*;

/**
 * Intérprete mínimo, solo para pruebas: ejecuta el TAC y permite verificar comportamiento
 * (no únicamente el texto). Soporta funciones, objetos con despacho virtual, arreglos y try/catch.
 */
final class MaquinaTAC {
    static final class Objeto { final String clase; final Map<String, Object> campos = new HashMap<>();
        Objeto(String clase) { this.clase = clase; } }
    static final class Excepcion extends RuntimeException { Excepcion(String m) { super(m, null, false, false); } }
    private static final class Marco { final Map<String, Object> vars = new HashMap<>(); int retorno; String destino;
        final Deque<Integer> manejadores = new ArrayDeque<>(); }

    private final List<InstruccionTAC> codigo;
    private final Map<String, Integer> etiquetas = new HashMap<>(), funciones = new HashMap<>();
    private final Map<String, DescriptorClase> clases;
    final Map<String, Object> globales = new HashMap<>();
    final List<String> salida = new ArrayList<>();
    private final Deque<Marco> pila = new ArrayDeque<>();
    private int pasos;

    MaquinaTAC(ResultadoCompilacion r) {
        codigo = r.instrucciones(); clases = r.clases();
        for (int i = 0; i < codigo.size(); i++) {
            var ins = codigo.get(i);
            if (ins.tipo() == InstruccionTAC.Tipo.ETIQUETA) etiquetas.put(ins.resultado(), i);
            if (ins.tipo() == InstruccionTAC.Tipo.FUNCION) funciones.put(ins.resultado(), i);
        }
    }

    private Marco marco() { return pila.peek(); }
    private boolean esLocal(String n) { return marco() != null && (n.startsWith("%" ) || n.matches("t\\d+")) && !n.startsWith("%global"); }
    private Object valor(String op) {
        if (op == null) return null;
        if (op.equals("true")) return true; if (op.equals("false")) return false; if (op.equals("null")) return null;
        if (op.startsWith("\"")) return op.substring(1, op.length() - 1);
        if (op.matches("-?\\d+")) return Long.parseLong(op);
        if (op.matches("-?\\d+\\.\\d+")) return Double.parseDouble(op);
        var m = esLocal(op) ? marco().vars : globales;
        return m.get(op);
    }
    private void guardar(String op, Object v) { (esLocal(op) ? marco().vars : globales).put(op, v); }

    private static Object aritmetica(String op, Object a, Object b) {
        if (a instanceof Double || b instanceof Double) {
            double x = ((Number) a).doubleValue(), y = ((Number) b).doubleValue();
            return switch (op) { case "+" -> x + y; case "-" -> x - y; case "*" -> x * y; case "/" -> x / y; case "%" -> x % y;
                case "<" -> x < y; case "<=" -> x <= y; case ">" -> x > y; case ">=" -> x >= y; case "==" -> x == y; case "!=" -> x != y;
                default -> throw new AssertionError(op); };
        }
        if (a instanceof Long x && b instanceof Long y) {
            if ((op.equals("/") || op.equals("%")) && y == 0) throw new Excepcion("division por cero");
            return switch (op) { case "+" -> x + y; case "-" -> x - y; case "*" -> x * y; case "/" -> x / y; case "%" -> x % y;
                case "<" -> x < y; case "<=" -> x <= y; case ">" -> x > y; case ">=" -> x >= y; case "==" -> x.equals(y); case "!=" -> !x.equals(y);
                default -> throw new AssertionError(op); };
        }
        return switch (op) { case "+" -> String.valueOf(a) + b; case "==" -> Objects.equals(a, b); case "!=" -> !Objects.equals(a, b);
            default -> throw new AssertionError(op + " sobre " + a + ", " + b); };
    }

    Object ejecutar() {
        int pc = 0; var args = new ArrayList<Object>(); var vistos = 0;
        while (pc < codigo.size()) {
            if (++pasos > 2_000_000) throw new AssertionError("posible ciclo infinito");
            var ins = codigo.get(pc);
            try {
                switch (ins.tipo()) {
                    case CLASE -> { pc = saltoFinClase(pc); continue; }
                    case FUNCION -> { // sólo se entra por llamada; en flujo normal el programa salta antes
                        pc++; continue; }
                    case ETIQUETA, FIN_CLASE -> pc++;
                    case SALTO -> pc = etiquetas.get(ins.resultado());
                    case SALTO_CONDICIONAL -> pc = Boolean.TRUE.equals(valor(ins.argumento1())) ? etiquetas.get(ins.resultado()) : pc + 1;
                    case PARAMETRO -> { guardar(ins.resultado(), marco().vars.get("$arg" + ins.argumento1())); pc++; }
                    case ARGUMENTO -> { args.add(valor(ins.argumento1())); pc++; }
                    case LLAMADA -> {
                        String destino = ins.argumento1();
                        if (ins.operador().equals("vcall")) {
                            var receptor = (Objeto) args.get(0);
                            String nombre = destino.substring(destino.indexOf('.') + 1);
                            destino = clases.get(receptor.clase).metodo(nombre).etiqueta();
                        }
                        var m = new Marco(); for (int i = 0; i < args.size(); i++) m.vars.put("$arg" + i, args.get(i));
                        args.clear(); m.retorno = pc + 1; m.destino = ins.resultado(); pila.push(m);
                        pc = funciones.get(destino) + 1;
                    }
                    case RETORNO -> {
                        Object v = valor(ins.argumento1()); var m = pila.pop();
                        pc = m.retorno; if (m.destino != null) guardar(m.destino, v);
                    }
                    case FIN_FUNCION -> throw new AssertionError("Falta retorno en " + ins.resultado());
                    case NUEVO_ARREGLO -> { var l = new ArrayList<Object>(); for (long i = 0; i < Long.parseLong(ins.argumento1()); i++) l.add(null); guardar(ins.resultado(), l); pc++; }
                    case ESCRITURA_ARREGLO -> { @SuppressWarnings("unchecked") var l = (List<Object>) valor(ins.resultado());
                        l.set(((Long) valor(ins.argumento1())).intValue(), valor(ins.argumento2())); pc++; }
                    case LECTURA_ARREGLO -> { @SuppressWarnings("unchecked") var l = (List<Object>) valor(ins.argumento1());
                        int i = ((Long) valor(ins.argumento2())).intValue();
                        if (i < 0 || i >= l.size()) throw new Excepcion("indice fuera de rango");
                        guardar(ins.resultado(), l.get(i)); pc++; }
                    case LONGITUD_ARREGLO -> { guardar(ins.resultado(), (long) ((List<?>) valor(ins.argumento1())).size()); pc++; }
                    case NUEVO_OBJETO -> { guardar(ins.resultado(), new Objeto(ins.argumento1())); pc++; }
                    case LECTURA_CAMPO -> { guardar(ins.resultado(), ((Objeto) valor(ins.argumento1())).campos.get(ins.argumento2())); pc++; }
                    case ESCRITURA_CAMPO -> { ((Objeto) valor(ins.resultado())).campos.put(ins.argumento1(), valor(ins.argumento2())); pc++; }
                    case IMPRIMIR -> { salida.add(String.valueOf(valor(ins.argumento1()))); pc++; }
                    case INICIO_TRY -> { marco0().manejadores.push(etiquetas.get(ins.resultado())); pc++; }
                    case FIN_TRY -> { marco0().manejadores.pop(); pc++; }
                    case CAPTURA -> { guardar(ins.resultado(), pendiente); pc++; }
                    case OPERACION -> { ejecutarOperacion(ins); pc++; }
                    default -> throw new AssertionError(ins.tipo());
                }
            } catch (Excepcion e) {
                // desenrollar marcos hasta encontrar un manejador
                while (marco() != null && marco().manejadores.isEmpty()) pila.pop();
                if (marco() == null && global.manejadores.isEmpty()) throw e;
                pendiente = e.getMessage();
                pc = marco0().manejadores.pop();
            }
        }
        return null;
    }
    private String pendiente;
    private final Marco global = new Marco();
    private Marco marco0() { return marco() != null ? marco() : global; }
    private int saltoFinClase(int pc) { int i = pc; while (codigo.get(i).tipo() != InstruccionTAC.Tipo.FIN_CLASE) i++; return i + 1; }

    private void ejecutarOperacion(InstruccionTAC ins) {
        if (ins.argumento2() == null) {
            Object v = valor(ins.argumento1());
            guardar(ins.resultado(), switch (ins.operador()) { case "" -> v; case "!" -> !(Boolean) v;
                case "-" -> v instanceof Long l ? (Object) (-l) : (Object) (-(Double) v); default -> throw new AssertionError(ins.operador()); });
        } else guardar(ins.resultado(), aritmetica(ins.operador(), valor(ins.argumento1()), valor(ins.argumento2())));
    }
}
