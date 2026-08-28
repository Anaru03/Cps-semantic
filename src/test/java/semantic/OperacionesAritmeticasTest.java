package semantic;

import antlr.CompiscriptLexer;
import antlr.CompiscriptParser;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OperacionesAritmeticasTest {

    private Tipo analizar(String codigo) {
        CompiscriptLexer lexer =
                new CompiscriptLexer(CharStreams.fromString(codigo));

        CommonTokenStream tokens =
                new CommonTokenStream(lexer);

        CompiscriptParser parser =
                new CompiscriptParser(tokens);

        TipoVisitor visitor = new TipoVisitor();

        return visitor.visit(parser.expression());
    }

    @Test
    void sumaEntreEnterosEsValida() {
        assertEquals(
                Tipo.INTEGER,
                analizar("10 + 5")
        );
    }

    @Test
    void restaEntreEnterosEsValida() {
        assertEquals(
                Tipo.INTEGER,
                analizar("10 - 5")
        );
    }

    @Test
    void multiplicacionEntreEnterosEsValida() {
        assertEquals(
                Tipo.INTEGER,
                analizar("10 * 5")
        );
    }

    @Test
    void divisionEntreEnterosEsValida() {
        assertEquals(
                Tipo.INTEGER,
                analizar("10 / 5")
        );
    }

    @Test
    void sumaConBooleanEsInvalida() {
        assertEquals(
                Tipo.ERROR,
                analizar("true + 5")
        );
    }

    @Test
    void restaConStringEsInvalida() {
        assertEquals(
                Tipo.ERROR,
                analizar("\"hola\" - 5")
        );
    }

    @Test
    void multiplicacionConBooleanEsInvalida() {
        assertEquals(
                Tipo.ERROR,
                analizar("10 * false")
        );
    }

    @Test
    void divisionConStringEsInvalida() {
        assertEquals(
                Tipo.ERROR,
                analizar("10 / \"hola\"")
        );
    }

    @Test
    void negativoDeEnteroEsValido() {
        assertEquals(
                Tipo.INTEGER,
                analizar("-10")
        );
    }

    @Test
    void negativoDeBooleanEsInvalido() {
        assertEquals(
                Tipo.ERROR,
                analizar("-true")
        );
    }
}