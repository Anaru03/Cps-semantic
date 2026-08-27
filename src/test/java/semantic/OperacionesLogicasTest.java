package semantic;

import antlr.CompiscriptLexer;
import antlr.CompiscriptParser;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OperacionesLogicasTest {

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
    void andEntreBooleanosEsValido() {
        assertEquals(
                Tipo.BOOLEAN,
                analizar("true && false")
        );
    }

    @Test
    void orEntreBooleanosEsValido() {
        assertEquals(
                Tipo.BOOLEAN,
                analizar("true || false")
        );
    }

    @Test
    void negacionBooleanaEsValida() {
        assertEquals(
                Tipo.BOOLEAN,
                analizar("!true")
        );
    }

    @Test
    void andConEnterosEsInvalido() {
        assertEquals(
                Tipo.ERROR,
                analizar("10 && 20")
        );
    }

    @Test
    void orConTiposIncompatiblesEsInvalido() {
        assertEquals(
                Tipo.ERROR,
                analizar("true || 10")
        );
    }

    @Test
    void negacionDeEnteroEsInvalida() {
        assertEquals(
                Tipo.ERROR,
                analizar("!5")
        );
    }
}