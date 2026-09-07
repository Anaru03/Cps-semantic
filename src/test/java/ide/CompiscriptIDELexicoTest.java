package ide;

import antlr.CompiscriptLexer;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompiscriptIDELexicoTest {

    private List<String> erroresDe(String codigo) {
        CompiscriptLexer lexer = new CompiscriptLexer(CharStreams.fromString(codigo));
        List<String> errores = new ArrayList<>();
        CompiscriptIDE.registrarErroresLexicos(lexer, errores::add);
        newTokenStream(lexer).fill();
        return errores;
    }

    private CommonTokenStream newTokenStream(CompiscriptLexer lexer) {
        return new CommonTokenStream(lexer);
    }

    @Test void detectaArrobaComoCaracterInvalido() {
        List<String> errores = erroresDe("let x: integer = 10;\n@\nprint(x);");
        assertEquals(1, errores.size());
        assertTrue(errores.get(0).startsWith("Lexico 2:0"));
        assertTrue(errores.get(0).contains("@"));
    }

    @Test void detectaNumeralComoCaracterInvalido() {
        List<String> errores = erroresDe("#");
        assertEquals(1, errores.size());
        assertTrue(errores.get(0).startsWith("Lexico 1:0"));
    }
}
