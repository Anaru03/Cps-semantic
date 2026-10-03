package compiler;

import antlr.*;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import semantic.*;
import tac.*;
import java.util.*;

/** Punto de entrada compartido para tests, CLI e integración futura del IDE. */
public final class Compilador {
    private Compilador() { }
    public static ResultadoCompilacion compilar(String fuente) {
        Objects.requireNonNull(fuente);
        var errores = new ArrayList<Diagnostico>();
        var lexer = new CompiscriptLexer(CharStreams.fromString(fuente));
        lexer.removeErrorListeners(); lexer.addErrorListener(listener(errores, Diagnostico.Etapa.LEXICO));
        var tokens = new CommonTokenStream(lexer); tokens.fill();
        var parser = new CompiscriptParser(tokens);
        parser.removeErrorListeners(); parser.addErrorListener(listener(errores, Diagnostico.Etapa.SINTAXIS));
        var arbol = parser.program();
        if (!errores.isEmpty()) return fallo(arbol, null, errores);
        var analisis = AnalizadorSemantico.analizar(arbol);
        for (var error : analisis.resultado().errores()) errores.add(new Diagnostico(
                Diagnostico.Etapa.SEMANTICA, error.linea(), error.columna(), error.descripcion()));
        if (!errores.isEmpty()) return fallo(arbol, analisis, errores);
        var generador = new GeneradorTAC(); generador.usarInformacion(analisis.informacion());
        try {
            new GeneradorSentenciasTAC(generador).visit(arbol);
        } catch (ErrorGeneracionTAC error) {
            errores.add(new Diagnostico(Diagnostico.Etapa.TAC, error.linea(), error.columna(), error.getMessage()));
            return fallo(arbol, analisis, errores);
        } catch (UnsupportedOperationException | IllegalArgumentException | IllegalStateException | NullPointerException error) {
            // No publicar código parcial después de un fallo de generación.
            errores.add(new Diagnostico(Diagnostico.Etapa.TAC, arbol.getStart().getLine(),
                    arbol.getStart().getCharPositionInLine(), error.getMessage() == null
                    ? "Construcción sin representación TAC" : error.getMessage()));
            return fallo(arbol, analisis, errores);
        }
        AnotadorSimbolos.anotar(analisis, generador);
        return new ResultadoCompilacion(arbol, analisis, errores, generador.instrucciones(),
                generador.funciones(), generador.registrosActivacion(), generador.enlacesSimbolos(),
                generador.clases(), generador.temporales().estadisticas());
    }
    private static BaseErrorListener listener(List<Diagnostico> errores, Diagnostico.Etapa etapa) {
        return new BaseErrorListener() {
            @Override public void syntaxError(Recognizer<?, ?> recognizer, Object simbolo, int linea,
                    int columna, String mensaje, RecognitionException error) {
                errores.add(new Diagnostico(etapa, linea, columna, traducir(mensaje)));
            }
        };
    }
    /** Mensajes de ANTLR en español y sin detalles internos de la gramática. */
    static String traducir(String mensaje) {
        var m = java.util.regex.Pattern.compile("token recognition error at: '(.*)'").matcher(mensaje);
        if (m.matches()) return "Carácter o símbolo no reconocido: '" + m.group(1) + "'";
        m = java.util.regex.Pattern.compile("extraneous input '(.*)' expecting .*", java.util.regex.Pattern.DOTALL).matcher(mensaje);
        if (m.matches()) return "Símbolo inesperado '" + m.group(1) + "'";
        m = java.util.regex.Pattern.compile("missing (.*) at '(.*)'").matcher(mensaje);
        if (m.matches()) return "Falta " + m.group(1) + " antes de '" + m.group(2) + "'";
        m = java.util.regex.Pattern.compile("mismatched input '(.*)' expecting (.*)", java.util.regex.Pattern.DOTALL).matcher(mensaje);
        if (m.matches()) return "Se encontró '" + m.group(1) + "' pero se esperaba " + m.group(2);
        m = java.util.regex.Pattern.compile("no viable alternative at input '(.*)'", java.util.regex.Pattern.DOTALL).matcher(mensaje);
        if (m.matches()) return "Construcción no válida cerca de '" + m.group(1).strip() + "'";
        return mensaje;
    }
    /** Elimina mensajes repetidos y ordena por posición para una lectura estable. */
    private static List<Diagnostico> depurar(List<Diagnostico> errores) {
        var lineasLexicas = new HashSet<Integer>();
        for (var e : errores) if (e.etapa() == Diagnostico.Etapa.LEXICO) lineasLexicas.add(e.linea());
        var unicos = new LinkedHashSet<Diagnostico>();
        // Un símbolo ilegal provoca errores sintácticos derivados en la misma línea: no aportan información.
        for (var e : errores) if (!(e.etapa() == Diagnostico.Etapa.SINTAXIS && lineasLexicas.contains(e.linea()))) unicos.add(e);
        var lista = new ArrayList<>(unicos);
        lista.sort(Comparator.comparingInt(Diagnostico::linea).thenComparingInt(Diagnostico::columna));
        return lista;
    }
    private static ResultadoCompilacion fallo(CompiscriptParser.ProgramContext arbol,
                                               AnalisisSemantico analisis, List<Diagnostico> errores) {
        return new ResultadoCompilacion(arbol, analisis, depurar(errores), List.of(), Map.of(), Map.of(), List.of(), Map.of(), null);
    }
}
