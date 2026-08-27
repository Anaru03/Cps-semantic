package semantic;

import antlr.CompiscriptLexer;
import antlr.CompiscriptParser;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TipoVisitorTest {

    private Tipo analizarExpresion(String codigo) {
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
    void reconoceInteger() {
        assertEquals(Tipo.INTEGER, analizarExpresion("10"));
    }

    @Test
    void reconoceString() {
        assertEquals(Tipo.STRING, analizarExpresion("\"hola\""));
    }

    @Test
    void reconoceBooleanTrue() {
        assertEquals(Tipo.BOOLEAN, analizarExpresion("true"));
    }

    @Test
    void reconoceBooleanFalse() {
        assertEquals(Tipo.BOOLEAN, analizarExpresion("false"));
    }

    @Test
    void reconoceNull() {
        assertEquals(Tipo.NULL, analizarExpresion("null"));
    }
}