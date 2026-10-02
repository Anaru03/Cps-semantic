package tac;

import antlr.CompiscriptLexer;
import antlr.CompiscriptParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;
import semantic.AnalizadorSemantico;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CiclosTACTest {
    private GeneradorSentenciasTAC traducir(String fuente, boolean validar) {
        if (validar) assertTrue(AnalizadorSemantico.analizar(fuente).resultado().errores().isEmpty());
        var parser = new CompiscriptParser(new CommonTokenStream(
                new CompiscriptLexer(CharStreams.fromString(fuente))));
        var arbol = parser.program();
        assertEquals(0, parser.getNumberOfSyntaxErrors());
        var visitor = new GeneradorSentenciasTAC();
        visitor.visit(arbol);
        return visitor;
    }

    // Intérprete de prueba limitado al subconjunto usado aquí. Comprueba efectos
    // observables: un salto equivocado puede producir otro resultado o no terminar.
    Map<String, Integer> ejecutar(String fuente) {
        var visitor = traducir(fuente, true);
        var instrucciones = visitor.generador().instrucciones();
        var etiquetas = new HashMap<String, Integer>();
        var memoria = new HashMap<String, Integer>();
        for (int i = 0; i < instrucciones.size(); i++) {
            var instruccion = instrucciones.get(i);
            if (instruccion.tipo() == InstruccionTAC.Tipo.ETIQUETA)
                assertNull(etiquetas.put(instruccion.resultado(), i), "Etiqueta duplicada");
        }
        for (var instruccion : instrucciones)
            if (instruccion.tipo() == InstruccionTAC.Tipo.SALTO
                    || instruccion.tipo() == InstruccionTAC.Tipo.SALTO_CONDICIONAL)
                assertTrue(etiquetas.containsKey(instruccion.resultado()), "Destino inexistente");
        int pasos = 0;
        for (int pc = 0; pc < instrucciones.size();) {
            assertTrue(++pasos < 10000, "El TAC no termina");
            var ins = instrucciones.get(pc++);
            switch (ins.tipo()) {
                case ETIQUETA -> { }
                case SALTO -> pc = etiquetas.get(ins.resultado());
                case SALTO_CONDICIONAL -> {
                    if (valor(ins.argumento1(), memoria) != 0) pc = etiquetas.get(ins.resultado());
                }
                case OPERACION -> {
                    int a = valor(ins.argumento1(), memoria);
                    int b = ins.argumento2() == null ? 0 : valor(ins.argumento2(), memoria);
                    int resultado = switch (ins.operador()) {
                        case "" -> a;
                        case "+" -> a + b;
                        case "-" -> ins.argumento2() == null ? -a : a - b;
                        case "<" -> a < b ? 1 : 0;
                        case "==" -> a == b ? 1 : 0;
                        default -> throw new AssertionError("Operador fuera del intérprete de prueba");
                    };
                    memoria.put(ins.resultado(), resultado);
                }
            }
        }
        assertEquals(0, visitor.generador().temporales().cantidadEnUso());
        return memoria;
    }

    private int valor(String operando, Map<String, Integer> memoria) {
        if ("true".equals(operando)) return 1;
        if ("false".equals(operando)) return 0;
        if (operando.matches("-?\\d+")) return Integer.parseInt(operando);
        assertTrue(memoria.containsKey(operando), "Lectura sin inicializar: " + operando);
        return memoria.get(operando);
    }

    @Test void whilePuedeEjecutarCeroOVariasIteraciones() {
        assertEquals(0, ejecutar("let x: integer = 0; while (false) { x = 1; }").get("x"));
        assertEquals(3, ejecutar("let x: integer = 0; while (x < 3) { x = x + 1; }").get("x"));
    }

    @Test void doWhileEjecutaUnaVezYContinueEvaluaCondicion() {
        assertEquals(1, ejecutar("let x: integer = 0; do { x = x + 1; } while (false);").get("x"));
        assertEquals(3, ejecutar("let x: integer = 0; do { x = x + 1; continue; } while (x < 3);").get("x"));
    }

    @Test void forContinuePasaPorActualizacionYBreakLaOmite() {
        var memoria = ejecutar("let suma: integer = 0; let i: integer = 0;"
                + " for (i = 0; i < 6; i = i + 1) {"
                + " if (i == 1) { continue; } if (i == 4) { break; } suma = suma + i; }");
        assertEquals(5, memoria.get("suma"));
        assertEquals(4, memoria.get("i"));
    }

    @Test void forAdmiteDeclaracionYCondicionFalsa() {
        assertEquals(0, ejecutar("let x: integer = 0; for (let i: integer = 0; false; i = i + 1) { x = 1; }").get("x"));
    }

    @Test void forAdmiteCondicionOActualizacionOmitidas() {
        assertEquals(3, ejecutar("let i: integer = 0; for (;; i = i + 1) { if (i == 3) { break; } }").get("i"));
        assertEquals(3, ejecutar("let i: integer = 0; for (; i < 3;) { i = i + 1; }").get("i"));
        assertEquals(1, ejecutar("let i: integer = 0; for (;;) { i = 1; break; }").get("i"));
    }

    @Test void saltosAfectanSoloAlCicloActivo() {
        var memoria = ejecutar("let i: integer = 0; let j: integer = 0; let total: integer = 0;"
                + " while (i < 3) { i = i + 1; j = 0; while (j < 4) {"
                + " j = j + 1; if (j == 1) { continue; } if (j == 3) { break; }"
                + " total = total + 1; } }");
        assertEquals(3, memoria.get("total"));
        assertEquals(3, memoria.get("i"));
    }

    @Test void whileContinueVuelveALaCondicion() {
        assertEquals(3, ejecutar("let i: integer = 0; while (i < 3) { i = i + 1; continue; }").get("i"));
    }

    @Test void rechazaSaltosSinContextoInclusoSinValidacionPrevia() {
        assertThrows(IllegalStateException.class, () -> traducir("break;", false));
        assertThrows(IllegalStateException.class, () -> traducir("continue;", false));
    }

    @Test void limpiaContextoCuandoFallaLaTraduccionDelCuerpo() {
        var visitor = new GeneradorSentenciasTAC();
        var parser = new CompiscriptParser(new CommonTokenStream(new CompiscriptLexer(
                CharStreams.fromString("while (true) { print(1); }"))));
        assertThrows(UnsupportedOperationException.class, () -> visitor.visit(parser.program()));
        var salto = new CompiscriptParser(new CommonTokenStream(new CompiscriptLexer(
                CharStreams.fromString("break;"))));
        assertThrows(IllegalStateException.class, () -> visitor.visit(salto.program()));
    }
}
