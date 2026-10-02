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
        comprobarSoporte(arbol, errores);
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
        return new ResultadoCompilacion(arbol, analisis, errores, generador.instrucciones(),
                generador.funciones(), generador.registrosActivacion(), generador.enlacesSimbolos());
    }
    private static BaseErrorListener listener(List<Diagnostico> errores, Diagnostico.Etapa etapa) {
        return new BaseErrorListener() {
            @Override public void syntaxError(Recognizer<?, ?> recognizer, Object simbolo, int linea,
                    int columna, String mensaje, RecognitionException error) {
                errores.add(new Diagnostico(etapa, linea, columna, mensaje));
            }
        };
    }
    private static ResultadoCompilacion fallo(CompiscriptParser.ProgramContext arbol,
                                               AnalisisSemantico analisis, List<Diagnostico> errores) {
        return new ResultadoCompilacion(arbol, analisis, errores, List.of(), Map.of(), Map.of(), List.of());
    }
    private static void comprobarSoporte(ParseTree nodo, List<Diagnostico> errores) {
        if (nodo instanceof ParserRuleContext ctx) {
            String problema = null;
            if (ctx instanceof CompiscriptParser.StatementContext sentencia
                    && (sentencia.classDeclaration() != null
                    || sentencia.tryCatchStatement() != null || sentencia.printStatement() != null))
                problema = "Sentencia TAC pendiente: " + ctx.getStart().getText();
            if (ctx instanceof CompiscriptParser.ArrayLiteralContext
                    || ctx instanceof CompiscriptParser.IndexExprContext
                    || ctx instanceof CompiscriptParser.PropertyAccessExprContext
                    || ctx instanceof CompiscriptParser.PropertyAssignExprContext
                    || ctx instanceof CompiscriptParser.NewExprContext
                    || ctx instanceof CompiscriptParser.ThisExprContext)
                problema = "Acceso o estructura TAC pendiente";
            if (ctx instanceof CompiscriptParser.TernaryExprContext ternario && !ternario.expression().isEmpty())
                problema = "Ternario TAC pendiente";
            if (ctx instanceof CompiscriptParser.AssignmentContext asignacion && asignacion.expression().size() != 1)
                problema = "Asignación de propiedad TAC pendiente";
            if (problema != null) {
                errores.add(new Diagnostico(Diagnostico.Etapa.TAC, ctx.getStart().getLine(),
                        ctx.getStart().getCharPositionInLine(), problema));
                return;
            }
        }
        for (int i = 0; i < nodo.getChildCount(); i++) comprobarSoporte(nodo.getChild(i), errores);
    }
}
