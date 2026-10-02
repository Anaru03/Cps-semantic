package tac;

import antlr.CompiscriptBaseVisitor;
import antlr.CompiscriptParser;
import java.util.Objects;

/** Traduce sentencias de un árbol previamente validado por las etapas anteriores. */
public final class GeneradorSentenciasTAC extends CompiscriptBaseVisitor<Void> {
    private final GeneradorTAC generador;
    private final GeneradorExpresionesTAC expresiones;
    public GeneradorSentenciasTAC() { this(new GeneradorTAC()); }

    public GeneradorSentenciasTAC(GeneradorTAC generador) {
        this.generador = Objects.requireNonNull(generador);
        expresiones = new GeneradorExpresionesTAC(generador);
    }

    public GeneradorTAC generador() { return generador; }
    public String codigo() { return generador.codigo(); }

    @Override
    public Void visitStatement(CompiscriptParser.StatementContext ctx) {
        if (ctx.ifStatement() != null) return visit(ctx.ifStatement());
        if (ctx.block() != null) return visit(ctx.block());
        if (ctx.variableDeclaration() != null || ctx.assignment() != null
                || ctx.expressionStatement() != null) {
            String valor = expresiones.visit(ctx.getChild(0));
            generador.liberarTemporal(valor);
            return null;
        }
        throw new UnsupportedOperationException("Sentencia TAC no implementada: "
                + ctx.getStart().getText() + " en línea " + ctx.getStart().getLine());
    }

    @Override
    public Void visitIfStatement(CompiscriptParser.IfStatementContext ctx) {
        String verdadero = generador.nuevaEtiqueta();
        String falso = generador.nuevaEtiqueta();
        boolean tieneElse = ctx.block().size() > 1;
        String fin = tieneElse ? generador.nuevaEtiqueta() : falso;
        String condicion = Objects.requireNonNull(expresiones.visit(ctx.expression()),
                "Condición TAC no soportada");
        generador.generarSaltoCondicional(condicion, verdadero);
        generador.generarSalto(falso);
        generador.liberarTemporal(condicion);
        generador.emitirEtiqueta(verdadero);
        visit(ctx.block(0));
        if (tieneElse) {
            generador.generarSalto(fin);
            generador.emitirEtiqueta(falso);
            visit(ctx.block(1));
        }
        generador.emitirEtiqueta(fin);
        return null;
    }
}
