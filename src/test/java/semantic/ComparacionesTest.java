package semantic;

import antlr.CompiscriptLexer;
import antlr.CompiscriptParser;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ComparacionesTest {

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
    void igualdadEntreEnterosEsValida() {
        assertEquals(Tipo.BOOLEAN, analizar("10 == 20"));
    }

    @Test
    void desigualdadEntreBooleanosEsValida() {
        assertEquals(Tipo.BOOLEAN, analizar("true != false"));
    }

    @Test
    void igualdadEntreStringsEsValida() {
        assertEquals(
                Tipo.BOOLEAN,
                analizar("\"hola\" == \"adios\"")
        );
    }

    @Test
    void menorQueEntreEnterosEsValido() {
        assertEquals(Tipo.BOOLEAN, analizar("10 < 20"));
    }

    @Test
    void menorIgualEntreEnterosEsValido() {
        assertEquals(Tipo.BOOLEAN, analizar("10 <= 20"));
    }

    @Test
    void mayorQueEntreEnterosEsValido() {
        assertEquals(Tipo.BOOLEAN, analizar("20 > 10"));
    }

    @Test
    void mayorIgualEntreEnterosEsValido() {
        assertEquals(Tipo.BOOLEAN, analizar("20 >= 10"));
    }

    @Test
    void igualdadEntreTiposDiferentesEsInvalida() {
        assertEquals(
                Tipo.ERROR,
                analizar("10 == \"10\"")
        );
    }

    @Test
    void comparacionRelacionalConStringEsInvalida() {
        assertEquals(
                Tipo.ERROR,
                analizar("10 < \"hola\"")
        );
    }

    @Test
    void comparacionRelacionalConBooleanEsInvalida() {
        assertEquals(
                Tipo.ERROR,
                analizar("true >= false")
        );
    }
}