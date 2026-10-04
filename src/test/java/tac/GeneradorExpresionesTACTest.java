package tac;

import antlr.CompiscriptLexer;
import antlr.CompiscriptParser;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeneradorExpresionesTACTest {

    private ResultadoTAC generar(String codigo) {
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

        String resultado =
                visitor.visit(parser.expression());

        return new ResultadoTAC(
                resultado,
                visitor.codigo()
        );
    }

    @Test
    void literalNoNecesitaTemporal() {
        ResultadoTAC resultado =
                generar("10");

        assertEquals("10", resultado.resultado());
        assertEquals("", resultado.codigo());
    }

    @Test
    void identificadorNoNecesitaTemporal() {
        ResultadoTAC resultado =
                generar("x");

        assertEquals("x", resultado.resultado());
        assertEquals("", resultado.codigo());
    }

    @Test
    void generaSuma() {
        ResultadoTAC resultado =
                generar("a + b");

        assertEquals("t0", resultado.resultado());

        assertEquals(
                "t0 = a + b"
                        + System.lineSeparator(),
                resultado.codigo()
        );
    }

    @Test
    void generaResta() {
        ResultadoTAC resultado =
                generar("a - b");

        assertEquals(
                "t0 = a - b"
                        + System.lineSeparator(),
                resultado.codigo()
        );
    }

    @Test
    void generaMultiplicacion() {
        ResultadoTAC resultado =
                generar("a * b");

        assertEquals(
                "t0 = a * b"
                        + System.lineSeparator(),
                resultado.codigo()
        );
    }

    @Test
    void generaDivision() {
        ResultadoTAC resultado =
                generar("a / b");

        assertEquals(
                "t0 = a / b"
                        + System.lineSeparator(),
                resultado.codigo()
        );
    }

    @Test
    void generaModulo() {
        ResultadoTAC resultado =
                generar("a % b");

        assertEquals(
                "t0 = a % b"
                        + System.lineSeparator(),
                resultado.codigo()
        );
    }

    @Test
    void respetaPrecedencia() {
        ResultadoTAC resultado =
                generar("a + b * c");

        String esperado =
                "t0 = b * c"
                        + System.lineSeparator()
                + "t1 = a + t0"
                        + System.lineSeparator();

        assertEquals(
                esperado,
                resultado.codigo()
        );
    }

    @Test
    void respetaParentesis() {
        ResultadoTAC resultado =
                generar("(a + b) * c");

        String esperado =
                "t0 = a + b"
                        + System.lineSeparator()
                + "t1 = t0 * c"
                        + System.lineSeparator();

        assertEquals(
                esperado,
                resultado.codigo()
        );
    }

    @Test
    void generaNegacionAritmetica() {
        ResultadoTAC resultado =
                generar("-a");

        assertEquals(
                "t0 = -a"
                        + System.lineSeparator(),
                resultado.codigo()
        );
    }

    @Test
    void generaNegacionLogica() {
        ResultadoTAC resultado =
                generar("!a");

        assertEquals(
                "t0 = !a"
                        + System.lineSeparator(),
                resultado.codigo()
        );
    }

    @Test
    void generaAnd() {
        ResultadoTAC resultado =
                generar("a && b");

        assertEquals(
                String.join(System.lineSeparator(), "t0 = a", "if t0 goto L1", "goto L0", "L1:", "t0 = b", "L0:")
                        + System.lineSeparator(),
                resultado.codigo()
        );
    }

    @Test
    void generaOr() {
        ResultadoTAC resultado =
                generar("a || b");

        assertEquals(
                String.join(System.lineSeparator(), "t0 = a", "if t0 goto L0", "t0 = b", "L0:")
                        + System.lineSeparator(),
                resultado.codigo()
        );
    }

    @Test
    void generaMenorQue() {
        ResultadoTAC resultado =
                generar("a < b");

        assertEquals(
                "t0 = a < b"
                        + System.lineSeparator(),
                resultado.codigo()
        );
    }

    @Test
    void generaMayorIgual() {
        ResultadoTAC resultado =
                generar("a >= b");

        assertEquals(
                "t0 = a >= b"
                        + System.lineSeparator(),
                resultado.codigo()
        );
    }

    @Test
    void generaIgualdad() {
        ResultadoTAC resultado =
                generar("a == b");

        assertEquals(
                "t0 = a == b"
                        + System.lineSeparator(),
                resultado.codigo()
        );
    }

    @Test
    void generaDesigualdad() {
        ResultadoTAC resultado =
                generar("a != b");

        assertEquals(
                "t0 = a != b"
                        + System.lineSeparator(),
                resultado.codigo()
        );
    }

    private record ResultadoTAC(
            String resultado,
            String codigo) {
    }
}