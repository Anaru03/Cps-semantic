package tac;

import antlr.CompiscriptBaseVisitor;
import antlr.CompiscriptParser;
import java.util.Objects;
import java.util.ArrayDeque;
import java.util.Deque;

/** Traduce sentencias de un árbol previamente validado por las etapas anteriores. */
public final class GeneradorSentenciasTAC extends CompiscriptBaseVisitor<Void> {
    private final GeneradorTAC generador;
    private final GeneradorExpresionesTAC expresiones;
    // Un switch podrá agregar un contexto con continuación null: break usa el
    // contexto más cercano; continue busca el ciclo más cercano.
    private record ContextoControl(String salida, String continuacion) { }
    private final Deque<ContextoControl> contextos = new ArrayDeque<>();

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
        if (ctx.whileStatement() != null) return visit(ctx.whileStatement());
        if (ctx.doWhileStatement() != null) return visit(ctx.doWhileStatement());
        if (ctx.forStatement() != null) return visit(ctx.forStatement());
        if (ctx.breakStatement() != null) return visit(ctx.breakStatement());
        if (ctx.continueStatement() != null) return visit(ctx.continueStatement());
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

    private void emitirCondicion(CompiscriptParser.ExpressionContext ctx,
                                 String verdadero, String falso) {
        String valor = Objects.requireNonNull(expresiones.visit(ctx), "Condición TAC no soportada");
        generador.generarSaltoCondicional(valor, verdadero);
        generador.generarSalto(falso);
        generador.liberarTemporal(valor);
    }

    private void visitarCuerpo(CompiscriptParser.BlockContext cuerpo,
                              String salida, String continuacion) {
        contextos.push(new ContextoControl(salida, continuacion));
        try { visit(cuerpo); }
        finally { contextos.pop(); }
    }

    @Override public Void visitWhileStatement(CompiscriptParser.WhileStatementContext ctx) {
        String condicion = generador.nuevaEtiqueta();
        String cuerpo = generador.nuevaEtiqueta();
        String salida = generador.nuevaEtiqueta();
        generador.emitirEtiqueta(condicion);
        emitirCondicion(ctx.expression(), cuerpo, salida);
        generador.emitirEtiqueta(cuerpo);
        visitarCuerpo(ctx.block(), salida, condicion);
        generador.generarSalto(condicion);
        generador.emitirEtiqueta(salida);
        return null;
    }

    @Override public Void visitDoWhileStatement(CompiscriptParser.DoWhileStatementContext ctx) {
        String cuerpo = generador.nuevaEtiqueta();
        String condicion = generador.nuevaEtiqueta();
        String salida = generador.nuevaEtiqueta();
        generador.emitirEtiqueta(cuerpo);
        visitarCuerpo(ctx.block(), salida, condicion);
        generador.emitirEtiqueta(condicion);
        emitirCondicion(ctx.expression(), cuerpo, salida);
        generador.emitirEtiqueta(salida);
        return null;
    }

    @Override public Void visitForStatement(CompiscriptParser.ForStatementContext ctx) {
        if (ctx.variableDeclaration() != null) expresiones.visit(ctx.variableDeclaration());
        else if (ctx.assignment() != null) expresiones.visit(ctx.assignment());
        // Las dos expresiones son opcionales: su posición respecto al separador
        // distingue condición y actualización incluso cuando falta la primera.
        CompiscriptParser.ExpressionContext prueba = null;
        CompiscriptParser.ExpressionContext actualizacion = null;
        boolean despuesDelSeparador = false;
        for (int i = 3; i < ctx.getChildCount(); i++) {
            var hijo = ctx.getChild(i);
            if (hijo instanceof CompiscriptParser.ExpressionContext expr) {
                if (despuesDelSeparador) actualizacion = expr;
                else prueba = expr;
            } else if (";".equals(hijo.getText())) despuesDelSeparador = true;
        }
        String condicion = generador.nuevaEtiqueta();
        String cuerpo = generador.nuevaEtiqueta();
        String incremento = generador.nuevaEtiqueta();
        String salida = generador.nuevaEtiqueta();
        generador.emitirEtiqueta(condicion);
        if (prueba == null) generador.generarSalto(cuerpo);
        else emitirCondicion(prueba, cuerpo, salida);
        generador.emitirEtiqueta(cuerpo);
        visitarCuerpo(ctx.block(), salida, incremento);
        generador.emitirEtiqueta(incremento);
        if (actualizacion != null) {
            String valor = expresiones.visit(actualizacion);
            generador.liberarTemporal(valor);
        }
        generador.generarSalto(condicion);
        generador.emitirEtiqueta(salida);
        return null;
    }

    @Override public Void visitBreakStatement(CompiscriptParser.BreakStatementContext ctx) {
        if (contextos.isEmpty()) throw new IllegalStateException("break fuera de un ciclo o switch");
        generador.generarSalto(contextos.peek().salida());
        return null;
    }

    @Override public Void visitContinueStatement(CompiscriptParser.ContinueStatementContext ctx) {
        for (var contexto : contextos) {
            if (contexto.continuacion() != null) {
                generador.generarSalto(contexto.continuacion());
                return null;
            }
        }
        throw new IllegalStateException("continue fuera de un ciclo");
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
