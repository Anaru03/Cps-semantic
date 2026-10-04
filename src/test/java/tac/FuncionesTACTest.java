package tac;

import antlr.CompiscriptLexer;
import antlr.CompiscriptParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;
import semantic.AnalizadorSemantico;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class FuncionesTACTest {
    private GeneradorSentenciasTAC traducir(String fuente, boolean validar) {
        if (validar) assertTrue(AnalizadorSemantico.analizar(fuente).resultado().errores().isEmpty());
        var parser = new CompiscriptParser(new CommonTokenStream(new CompiscriptLexer(CharStreams.fromString(fuente))));
        var arbol = parser.program();
        assertEquals(0, parser.getNumberOfSyntaxErrors());
        var visitor = new GeneradorSentenciasTAC();
        visitor.visit(arbol);
        return visitor;
    }

    // Modelo de llamadas para verificar el contrato, no un runtime de producción.
    static class Maquina {
        final List<InstruccionTAC> codigo;
        final Map<String, RegistroActivacion> registros;
        final PilaActivaciones pila = new PilaActivaciones();
        Map<String, Integer> globales = new HashMap<>();
        final Map<String, Integer> etiquetas = new HashMap<>(), funciones = new HashMap<>();
        int pasos;
        Maquina(GeneradorSentenciasTAC visitor) {
            this(visitor.generador().instrucciones(), visitor.generador().registrosActivacion());
        }
        Maquina(compiler.ResultadoCompilacion resultado) {
            this(resultado.instrucciones(), resultado.registros());
        }
        private Maquina(List<InstruccionTAC> instrucciones, Map<String, RegistroActivacion> layouts) {
            codigo = instrucciones;
            registros = layouts;
            for (int i = 0; i < codigo.size(); i++) {
                var ins = codigo.get(i);
                if (ins.tipo() == InstruccionTAC.Tipo.ETIQUETA) assertNull(etiquetas.put(ins.resultado(), i));
                if (ins.tipo() == InstruccionTAC.Tipo.FUNCION) funciones.put(ins.resultado(), i + 1);
            }
        }
        int valor(String texto, Map<String, Integer> memoria) {
            if (texto.equals("true")) return 1;
            if (texto.equals("false")) return 0;
            if (texto.matches("-?\\d+")) return Integer.parseInt(texto);
            assertTrue(memoria.containsKey(texto), "Operando sin valor: " + texto);
            return memoria.get(texto);
        }
        Integer ejecutar(int pc, Map<String, Integer> memoria, List<Integer> parametros) {
            var pendientes = new ArrayList<Integer>();
            while (pc < codigo.size()) {
                assertTrue(++pasos < 20000, "TAC no termina");
                var ins = codigo.get(pc++);
                switch (ins.tipo()) {
                    case ETIQUETA, FUNCION -> { }
                    case FIN_FUNCION -> throw new AssertionError("Función sin retorno");
                    case SALTO -> pc = etiquetas.get(ins.resultado());
                    case SALTO_CONDICIONAL -> {
                        if (valor(ins.argumento1(), memoria) != 0) pc = etiquetas.get(ins.resultado());
                    }
                    case PARAMETRO -> memoria.put(ins.resultado(), parametros.get(Integer.parseInt(ins.argumento1())));
                    case ARGUMENTO -> pendientes.add(valor(ins.argumento1(), memoria));
                    case LLAMADA -> {
                        assertEquals(Integer.parseInt(ins.argumento2()), pendientes.size());
                        var args = List.copyOf(pendientes); pendientes.clear();
                        var marco = pila.entrar(registros.get(ins.argumento1()), args, pc, ins.resultado());
                        Integer retorno = ejecutar(funciones.get(ins.argumento1()), memoriaMarco(marco, memoria), args);
                        var regreso = pila.salir(retorno);
                        assertEquals(pc, regreso.direccion());
                        if (ins.resultado() != null) memoria.put(ins.resultado(), Objects.requireNonNull(retorno));
                    }
                    case RETORNO -> { return ins.argumento1() == null ? null : valor(ins.argumento1(), memoria); }
                    case OPERACION -> {
                        int a = valor(ins.argumento1(), memoria);
                        int b = ins.argumento2() == null ? 0 : valor(ins.argumento2(), memoria);
                        int r = switch (ins.operador()) {
                            case "" -> a; case "+" -> a + b; case "-" -> a - b;
                            case "*" -> a * b; case "<=" -> a <= b ? 1 : 0;
                            case "<" -> a < b ? 1 : 0; case "==" -> a == b ? 1 : 0;
                            default -> throw new AssertionError(ins.operador());
                        };
                        memoria.put(ins.resultado(), r);
                    }
                }
            }
            return null;
        }
        Map<String, Integer> memoriaMarco(RegistroActivacion.Instancia marco, Map<String, Integer> llamador) {
            // La memoria principal se mantiene compartida para globals; los slots
            // de cualquier activación se consultan exclusivamente en su marco.
            if (pila.profundidad() == 1) globales = llamador;
            return new AbstractMap<>() {
                private boolean local(Object key) {
                    return marco.layout().posiciones().stream().anyMatch(p -> p.operando().equals(key));
                }
                @Override public Integer get(Object key) {
                    return local(key) ? (Integer) marco.leer((String) key) : globales.get(key);
                }
                @Override public boolean containsKey(Object key) { return get(key) != null; }
                @Override public Integer put(String key, Integer value) {
                    Integer previo = get(key);
                    if (local(key)) marco.escribir(key, value); else globales.put(key, value);
                    return previo;
                }
                @Override public Set<Entry<String, Integer>> entrySet() { throw new UnsupportedOperationException(); }
            };
        }
    }
    private Map<String, Integer> ejecutar(String fuente) {
        var visitor = traducir(fuente, true);
        var memoria = new HashMap<String, Integer>();
        new Maquina(visitor).ejecutar(0, memoria, List.of());
        assertEquals(0, visitor.generador().temporales().cantidadEnUso());
        return memoria;
    }

    @Test void declaraSinEjecutarElCuerpoYRecibeParametrosEnOrden() {
        assertEquals(23, ejecutar("function combinar(a: integer, b: integer): integer { return a * 10 + b; }"
                + " let x: integer = combinar(2, 3);").get("x"));
    }
    @Test void llamadasAnidadasPreservanArgumentosExternos() {
        assertEquals(15, ejecutar("function doble(a: integer): integer { return a * 2; }"
                + " function sumar(a: integer, b: integer): integer { return a + b; }"
                + " let x: integer = sumar(doble(3), sumar(4, 5));").get("x"));
    }
    @Test void argumentosSeCongelanDeIzquierdaADerecha() {
        assertEquals(12, ejecutar("function combinar(a: integer, b: integer): integer { return a * 10 + b; }"
                + " let a: integer = 1; let x: integer = combinar(a, a = 2);").get("x"));
    }
    @Test void retornoEnRamasYRecursionUsanMarcosIndependientesEnElContrato() {
        assertEquals(120, ejecutar("function fact(n: integer): integer { if (n <= 1) { return 1; }"
                + " return n * fact(n - 1); } let x: integer = fact(5);").get("x"));
    }
    @Test void ceroArgumentosYLlamadasComoSentenciaLiberanTemporales() {
        assertEquals(7, ejecutar("function siete(): integer { return 7; } siete(); let x: integer = siete();").get("x"));
    }
    @Test void voidNoCreaTemporalYAdmiteRetornoImplicitoOExplicito() {
        var visitor = traducir("function f() { return; } function g() {} f(); g();", true);
        assertEquals(2, visitor.generador().funciones().size());
        assertEquals(0, visitor.generador().temporales().cantidadEnUso());
        for (var ins : visitor.generador().instrucciones())
            if (ins.tipo() == InstruccionTAC.Tipo.LLAMADA) assertNull(ins.resultado());
        new Maquina(visitor).ejecutar(0, new HashMap<>(), List.of());
    }
    @Test void erroresDeArgumentosYRetornosSonSemanticos() {
        for (String fuente : List.of("function f(a: integer): integer { return a; } f();",
                "function f(a: integer): integer { return a; } f(true);",
                "function f(): integer { return true; }", "return 1;"))
            assertFalse(AnalizadorSemantico.analizar(fuente).resultado().errores().isEmpty());
    }
    @Test void generadorRechazaRetornoFueraDeFuncionYLlamadaDesconocida() {
        assertThrows(IllegalStateException.class, () -> traducir("return 1;", false));
        assertThrows(IllegalArgumentException.class, () -> traducir("desconocida();", false));
        assertThrows(IllegalArgumentException.class,
                () -> traducir("function f(a: integer) {} f();", false));
    }
    @Test void funcionesAnidadasSeRechazanExplicitamente() {
        assertThrows(UnsupportedOperationException.class,
                () -> traducir("function f() { function g() {} }", true));
    }
    @Test void rechazaFuncionesConCaminosSinRetornoYVoidComoValor() {
        assertThrows(IllegalArgumentException.class,
                () -> traducir("function f(): integer { if (true) { return 1; } }", false));
        assertThrows(NullPointerException.class,
                () -> traducir("function f() {} let x = f();", false));
        assertEquals(2, ejecutar("function f(n: integer): integer { if (n <= 0) { return 1; }"
                + " else { return 2; } } let x: integer = f(1);").get("x"));
    }
    @Test void limpiarBorraDescriptoresYTiposDeInstruccionesTienenFormatoPropio() {
        var visitor = traducir("function f(a: integer): integer { return a; }", true);
        assertEquals("integer", visitor.generador().funcion("f").tipoRetorno());
        assertEquals("a = param 0", InstruccionTAC.parametro("a", 0).toString());
        assertEquals("arg t0", InstruccionTAC.argumento("t0").toString());
        assertEquals("t1 = call f, 1", InstruccionTAC.llamada("f", 1, "t1").toString());
        assertEquals("return", InstruccionTAC.retorno(null).toString());
        visitor.generador().limpiar();
        assertTrue(visitor.generador().funciones().isEmpty());
    }
    @Test void recursionPreservaLocalesYResultadosPreviosDelLlamador() {
        assertEquals(15, ejecutar("function suma(n: integer): integer { let guardado: integer = n;"
                + " if (n <= 0) { return 0; } let resto: integer = suma(n - 1);"
                + " return guardado + resto; } let x: integer = suma(5);").get("x"));
    }
    @Test void parametrosYSombrasDeBloqueTienenAlmacenamientoDistinto() {
        assertEquals(3, ejecutar("function f(n: integer): integer { { let n: integer = 9; n = n + 1; }"
                + " return n; } let x: integer = f(3);").get("x"));
    }
    @Test void globalesSonCompartidasPeroLocalesNoLasSobrescriben() {
        assertEquals(5, ejecutar("let g: integer = 1; function f(): integer { g = g + 1;"
                + " { let g: integer = 99; } return g; } let a: integer = f();"
                + " let x: integer = a + f();").get("x"));
    }
    @Test void nombresDeUsuarioNoColisionanConTemporales() {
        assertEquals(12, ejecutar("function f(t0: integer): integer { let t1: integer = t0 + 1;"
                + " return t1 + t0; } let t0: integer = 5; let x: integer = f(t0 + 0) + 1;").get("x"));
    }
    @Test void inicializadorDeSombraLeeElNombreExterior() {
        assertEquals(4, ejecutar("function f(n: integer): integer { let x: integer = 0;"
                + " { let n: integer = n + 1; x = n; } return x; } let x: integer = f(3);").get("x"));
    }
    @Test void layoutsGeneradosIncluyenParametrosLocalesYTemporalesDeCadaFuncion() {
        var visitor = traducir("function f(n: integer): integer { let x: integer = n + 1;"
                + " { let x: integer = 9; } return x; }"
                + " function g(n: integer): integer { return n + 2; }", true);
        var registros = visitor.generador().registrosActivacion();
        assertEquals(2, registros.size());
        var f = registros.get("f");
        assertEquals(RegistroActivacion.Clase.PARAMETRO, f.posiciones().get(0).clase());
        assertEquals("integer", f.posiciones().get(0).tipo());
        var sombras = f.posiciones().stream().filter(p -> p.nombre().equals("x")).toList();
        assertEquals(2, sombras.size());
        assertNotEquals(sombras.get(0).operando(), sombras.get(1).operando());
        assertNotEquals(sombras.get(0).offset(), sombras.get(1).offset());
        assertTrue(f.posiciones().stream().anyMatch(p -> p.clase() == RegistroActivacion.Clase.TEMPORAL));
        for (int i = 0; i < f.posiciones().size(); i++) assertEquals(i + 3, f.posiciones().get(i).offset());
        assertThrows(UnsupportedOperationException.class, () -> registros.clear());
        visitor.generador().limpiar();
        assertTrue(registros.isEmpty());
    }
}
