package tac;

import antlr.CompiscriptLexer;
import antlr.CompiscriptParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;
import semantic.AnalizadorSemantico;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.*;

class ControlCondicionalTACTest {
    private GeneradorSentenciasTAC generar(String fuente) {
        assertTrue(AnalizadorSemantico.analizar(fuente).resultado().errores().isEmpty());
        var parser = new CompiscriptParser(new CommonTokenStream(
                new CompiscriptLexer(CharStreams.fromString(fuente))));
        var arbol = parser.program();
        assertEquals(0, parser.getNumberOfSyntaxErrors());
        var visitor = new GeneradorSentenciasTAC();
        visitor.visit(arbol);
        return visitor;
    }

    private String lineas(String... lineas) {
        return String.join(System.lineSeparator(), lineas) + System.lineSeparator();
    }

    @Test void ifSinElseSaltaAlFinalCuandoEsFalso() {
        var visitor = generar("let x: integer = 0; if (true) { x = 1; } x = 2;");
        assertEquals(lineas("x = 0", "if true goto L0", "goto L1", "L0:",
                "x = 1", "L1:", "x = 2"), visitor.codigo());
    }

    @Test void ifElseEvitaEjecutarAmbasRamasYLiberaCondicion() {
        var visitor = generar("let x: integer = 0; if (x < 2) { x = x + 1; } else { x = 3; }");
        assertEquals(lineas("x = 0", "t0 = x < 2", "if t0 goto L0", "goto L1",
                "L0:", "t0 = x + 1", "x = t0", "goto L2", "L1:", "x = 3", "L2:"),
                visitor.codigo());
        assertEquals(0, visitor.generador().temporales().cantidadEnUso());
    }

    @Test void anidamientoYCondicionalesConsecutivosTienenDestinosUnicos() {
        var visitor = generar("let x: integer = 0; if (true) { if (false) { x = 1; }"
                + " else { x = 2; } } else { x = 3; } if (false) { x = 4; }");
        var etiquetas = new HashSet<String>();
        for (var instruccion : visitor.generador().instrucciones()) {
            if (instruccion.tipo() == InstruccionTAC.Tipo.ETIQUETA)
                assertTrue(etiquetas.add(instruccion.resultado()), "Etiqueta duplicada");
        }
        assertEquals(8, etiquetas.size());
        for (var instruccion : visitor.generador().instrucciones()) {
            if (instruccion.tipo() == InstruccionTAC.Tipo.SALTO
                    || instruccion.tipo() == InstruccionTAC.Tipo.SALTO_CONDICIONAL)
                assertTrue(etiquetas.contains(instruccion.resultado()), "Destino inexistente");
        }
    }

    @Test void rechazaSentenciasTodaviaNoImplementadas() {
        assertThrows(UnsupportedOperationException.class,
                () -> generar("print(1);"));
    }

    @Test void comparteEmisorDeExpresionesYSentencias() {
        var emisor = new GeneradorTAC();
        assertSame(emisor, new GeneradorExpresionesTAC(emisor).generador());
        assertSame(emisor, new GeneradorSentenciasTAC(emisor).generador());
    }

    @Test void etiquetasSeReinicianAlLimpiar() {
        var emisor = new GeneradorTAC();
        assertEquals("L0", emisor.nuevaEtiqueta());
        assertEquals("L1", emisor.nuevaEtiqueta());
        emisor.emitirEtiqueta("L0");
        emisor.limpiar();
        assertEquals("", emisor.codigo());
        assertEquals("L0", emisor.nuevaEtiqueta());
    }

    @Test void instruccionesDeControlTienenRepresentacionPropia() {
        assertEquals("L0:", InstruccionTAC.etiqueta("L0").toString());
        assertEquals("goto L0", InstruccionTAC.salto("L0").toString());
        assertEquals("if true goto L0", InstruccionTAC.saltoCondicional("true", "L0").toString());
        assertThrows(NullPointerException.class, () -> InstruccionTAC.salto(null));
        assertThrows(NullPointerException.class, () -> InstruccionTAC.saltoCondicional(null, "L0"));
    }
}
