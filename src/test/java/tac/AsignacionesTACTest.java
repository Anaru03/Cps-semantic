package tac;

import antlr.CompiscriptLexer;
import antlr.CompiscriptParser;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AsignacionesTACTest {

    private String generarPrograma(String codigo) {
        CompiscriptLexer lexer =
                new CompiscriptLexer(
                        CharStreams.fromString(codigo)
                );

        CompiscriptParser parser =
                new CompiscriptParser(
                        new CommonTokenStream(lexer)
                );

        GeneradorExpresionesTAC visitor =
                new GeneradorExpresionesTAC();

        visitor.visit(parser.program());

        return visitor.codigo();
    }

    @Test
    void generaDeclaracionConLiteral() {
        String codigo =
                generarPrograma(
                        "let x: integer = 10;"
                );

        assertEquals(
                "x = 10"
                        + System.lineSeparator(),
                codigo
        );
    }

    @Test
    void generaDeclaracionConExpresion() {
        String codigo =
                generarPrograma(
                        "let x: integer = a + b * c;"
                );

        String esperado =
                "t0 = b * c"
                        + System.lineSeparator()
                + "t1 = a + t0"
                        + System.lineSeparator()
                + "x = t1"
                        + System.lineSeparator();

        assertEquals(
                esperado,
                codigo
        );
    }

    @Test
    void generaAsignacionSimple() {
        String codigo =
                generarPrograma(
                        "x = 10;"
                );

        assertEquals(
                "x = 10"
                        + System.lineSeparator(),
                codigo
        );
    }

    @Test
    void generaAsignacionConExpresion() {
        String codigo =
                generarPrograma(
                        "x = y - z;"
                );

        String esperado =
                "t0 = y - z"
                        + System.lineSeparator()
                + "x = t0"
                        + System.lineSeparator();

        assertEquals(
                esperado,
                codigo
        );
    }

    @Test
    void reutilizaTemporalEntreAsignaciones() {
        String codigo =
                generarPrograma(
                        "x = a + b; y = c + d;"
                );

        String esperado =
                "t0 = a + b"
                        + System.lineSeparator()
                + "x = t0"
                        + System.lineSeparator()
                + "t0 = c + d"
                        + System.lineSeparator()
                + "y = t0"
                        + System.lineSeparator();

        assertEquals(
                esperado,
                codigo
        );
    }

    @Test
    void generaProgramaConVariasExpresiones() {
        String codigo =
                generarPrograma(
                        "let x: integer = a + b; "
                        + "let y: integer = c * d; "
                        + "x = x - y;"
                );

        String esperado =
                "t0 = a + b"
                        + System.lineSeparator()
                + "x = t0"
                        + System.lineSeparator()
                + "t0 = c * d"
                        + System.lineSeparator()
                + "y = t0"
                        + System.lineSeparator()
                + "t0 = x - y"
                        + System.lineSeparator()
                + "x = t0"
                        + System.lineSeparator();

        assertEquals(
                esperado,
                codigo
        );
    }
}