package tac;

import compiler.*;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CompiladorIntegracionTest {
    private ResultadoCompilacion valido(String fuente) {
        var resultado = Compilador.compilar(fuente);
        assertTrue(resultado.esValido(), resultado.errores().toString());
        return resultado;
    }
    private Map<String, Integer> ejecutar(ResultadoCompilacion resultado) {
        var memoria = new HashMap<String, Integer>();
        var maquina = new FuncionesTACTest.Maquina(resultado);
        maquina.ejecutar(0, memoria, List.of());
        assertEquals(0, maquina.pila.profundidad());
        return memoria;
    }
    @Test void erroresLexicosSintacticosYSemanticosBloqueanTAC() {
        for (var caso : Map.of("@", Diagnostico.Etapa.LEXICO,
                "let x = ;", Diagnostico.Etapa.SINTAXIS,
                "let x: integer = true;", Diagnostico.Etapa.SEMANTICA).entrySet()) {
            var resultado = Compilador.compilar(caso.getKey());
            assertFalse(resultado.esValido());
            assertTrue(resultado.errores().stream().anyMatch(e -> e.etapa() == caso.getValue()));
            assertEquals("", resultado.codigoTAC()); assertTrue(resultado.registros().isEmpty());
        }
    }
    @Test void noPublicaCodigoParcialAnteConstruccionPendiente() {
        for (var fuente : List.of("let x = 1; print(x);", "let x = [1, 2];",
                "let x = true ? 1 : 2;", "function f(): integer {}")) {
            var resultado = Compilador.compilar(fuente);
            assertFalse(resultado.esValido()); assertEquals("", resultado.codigoTAC());
            assertNotNull(resultado.analisis()); assertTrue(resultado.enlaces().isEmpty());
            assertTrue(resultado.errores().stream().anyMatch(e -> e.etapa() == Diagnostico.Etapa.TAC));
        }
    }
    @Test void diagnosticoDeSoporteTieneLineaReal() {
        var resultado = Compilador.compilar("let x = 1;\n\nprint(x);");
        assertEquals(3, resultado.errores().get(0).linea());
        var retorno = Compilador.compilar("let x = 1;\n\nfunction f(): integer {}");
        assertEquals(3, retorno.errores().get(0).linea());
    }
    @Test void temporalesRecicladosConTiposDistintosUsanSlotDynamic() {
        var resultado = valido("function f(n: integer): integer { let x = n + 1;"
                + " if (n <= 0) { return x; } return n; } let y = f(2);");
        assertTrue(resultado.registros().get("f").posiciones().stream()
                .filter(p -> p.clase() == RegistroActivacion.Clase.TEMPORAL)
                .noneMatch(p -> p.tipo().equals("unknown")));
    }
    @Test void pipelineUsaElMismoArbolEnSemanticaYTACYCompletaTiposInferidos() {
        var resultado = valido("function f(n: integer): integer { let local = n + 1; return local; } let x = f(2);");
        assertEquals(3, ejecutar(resultado).get("x"));
        var local = resultado.registros().get("f").posiciones().stream()
                .filter(p -> p.nombre().equals("local")).findFirst().orElseThrow();
        assertEquals("integer", local.tipo());
        assertTrue(resultado.analisis().informacion().ambitos().containsKey(resultado.arbol()));
        var enlace = resultado.enlaces().stream().filter(e -> e.operando().equals(local.operando())).findFirst().orElseThrow();
        assertEquals("local", enlace.referencia().simbolo().nombre());
        assertEquals("f", enlace.funcion()); assertEquals(local.offset(), enlace.offset());
    }
    @Test void sombrasDelProgramaPrincipalYFuncionesSeResuelvenPorIdentidad() {
        var resultado = valido("let x = 1; { let x = 9; x = x + 1; }"
                + " function f(): integer { return x; } let y = f();");
        var memoria = ejecutar(resultado);
        assertEquals(1, memoria.get("x")); assertEquals(1, memoria.get("y"));
        var xs = resultado.enlaces().stream().filter(e -> e.referencia().nombre().equals("x")).toList();
        assertEquals(2, xs.size()); assertNotEquals(xs.get(0).operando(), xs.get(1).operando());
        assertNotSame(xs.get(0).referencia().ambito(), xs.get(1).referencia().ambito());
    }
    @Test void compilarDosVecesEsIndependienteYLosResultadosSonInmutables() {
        var primero = valido("function f(): integer { return 1; } let x = f();");
        var segundo = valido("let x = 2;");
        assertEquals("x = 2" + System.lineSeparator(), segundo.codigoTAC());
        assertEquals(1, primero.funciones().size()); assertTrue(segundo.funciones().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> primero.instrucciones().clear());
        assertThrows(UnsupportedOperationException.class, () -> primero.registros().clear());
    }
    @Test void constantesSeTraducenYReasignacionEsRechazada() {
        assertEquals(4, ejecutar(valido("const n = 3; let x = n + 1;")).get("x"));
        assertFalse(Compilador.compilar("const n = 3; n = 4;").esValido());
    }
    @Test void funcionesCondicionalesCiclosYSwitchSeCombinanEnUnaCompilacion() {
        var resultado = valido("function f(n: integer): integer { if (n <= 0) { return 1; } return n + 1; }"
                + " let x = 0; for (let i = 0; i < 3; i = i + 1) { switch (i) {"
                + " case 0: x = f(i); break; case 1: continue; default: x = x + f(i); } }");
        assertEquals(4, ejecutar(resultado).get("x"));
    }
    @Test void operandoIzquierdoConservaSuValorFrenteAEfectosDeLaLlamadaDerecha() {
        var resultado = valido("let g = 1; function f(): integer { g = g + 1; return g; } let x = g + f();");
        assertEquals(3, ejecutar(resultado).get("x"));
    }
    @Test void todosLosEjemplosPasanElPipelineYRecursionEjecutaConLosRegistros() throws Exception {
        try (var rutas = Files.list(Path.of("examples/tac"))) {
            for (var ruta : rutas.filter(p -> p.toString().endsWith(".cps")).toList()) valido(Files.readString(ruta));
        }
        var resultado = valido(Files.readString(Path.of("examples/tac/recursion_locales.cps")));
        var memoria = ejecutar(resultado);
        assertEquals(15, memoria.get("resultado")); assertEquals(6, memoria.get("llamadas"));
    }
}
