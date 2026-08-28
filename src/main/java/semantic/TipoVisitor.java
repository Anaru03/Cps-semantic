package semantic;

import antlr.CompiscriptBaseVisitor;
import antlr.CompiscriptParser;

public class TipoVisitor extends CompiscriptBaseVisitor<Tipo> {

    @Override
    public Tipo visitLiteralExpr(CompiscriptParser.LiteralExprContext ctx) {
        String texto = ctx.getText();

        if (texto.equals("true") || texto.equals("false")) {
            return Tipo.BOOLEAN;
        }

        if (texto.equals("null")) {
            return Tipo.NULL;
        }

        if (texto.startsWith("\"") && texto.endsWith("\"")) {
            return Tipo.STRING;
        }

        if (texto.matches("[0-9]+")) {
            return Tipo.INTEGER;
        }

        return Tipo.UNKNOWN;
    }
    @Override
    public Tipo visitLogicalOrExpr(CompiscriptParser.LogicalOrExprContext ctx) {
        Tipo resultado = visit(ctx.logicalAndExpr(0));

        for (int i = 1; i < ctx.logicalAndExpr().size(); i++) {
            Tipo derecho = visit(ctx.logicalAndExpr(i));

            if (resultado != Tipo.BOOLEAN || derecho != Tipo.BOOLEAN) {
                return Tipo.ERROR;
            }

            resultado = Tipo.BOOLEAN;
        }

        return resultado;
    }

    @Override
    public Tipo visitLogicalAndExpr(CompiscriptParser.LogicalAndExprContext ctx) {
        Tipo resultado = visit(ctx.equalityExpr(0));

        for (int i = 1; i < ctx.equalityExpr().size(); i++) {
            Tipo derecho = visit(ctx.equalityExpr(i));

            if (resultado != Tipo.BOOLEAN || derecho != Tipo.BOOLEAN) {
                return Tipo.ERROR;
            }

            resultado = Tipo.BOOLEAN;
        }

        return resultado;
    }

    @Override
    public Tipo visitUnaryExpr(CompiscriptParser.UnaryExprContext ctx) {
        if (ctx.getChildCount() == 2) {
            String operador = ctx.getChild(0).getText();
            Tipo operando = visit(ctx.unaryExpr());

            if ("!".equals(operador)) {
                return operando == Tipo.BOOLEAN
                        ? Tipo.BOOLEAN
                        : Tipo.ERROR;
            }

            if ("-".equals(operador)) {
                return operando.esNumerico()
                        ? operando
                        : Tipo.ERROR;
            }
        }

        return visitChildren(ctx);
    }

    @Override
    public Tipo visitEqualityExpr(CompiscriptParser.EqualityExprContext ctx) {
        Tipo izquierdo = visit(ctx.relationalExpr(0));

        if (ctx.relationalExpr().size() == 1) {
            return izquierdo;
        }

        for (int i = 1; i < ctx.relationalExpr().size(); i++) {
            Tipo derecho = visit(ctx.relationalExpr(i));

            if (izquierdo == Tipo.ERROR || derecho == Tipo.ERROR) {
                return Tipo.ERROR;
            }

            if (izquierdo != derecho) {
                return Tipo.ERROR;
            }

            izquierdo = derecho;
        }

        return Tipo.BOOLEAN;
    }

    @Override
    public Tipo visitRelationalExpr(CompiscriptParser.RelationalExprContext ctx) {
        Tipo izquierdo = visit(ctx.additiveExpr(0));

        if (ctx.additiveExpr().size() == 1) {
            return izquierdo;
        }

        for (int i = 1; i < ctx.additiveExpr().size(); i++) {
            Tipo derecho = visit(ctx.additiveExpr(i));

            if (!izquierdo.esNumerico() || !derecho.esNumerico()) {
                return Tipo.ERROR;
            }

            izquierdo = derecho;
        }

        return Tipo.BOOLEAN;
    }

    @Override
    public Tipo visitAdditiveExpr(CompiscriptParser.AdditiveExprContext ctx) {
        Tipo izquierdo = visit(ctx.multiplicativeExpr(0));

        if (ctx.multiplicativeExpr().size() == 1) {
            return izquierdo;
        }

        for (int i = 1; i < ctx.multiplicativeExpr().size(); i++) {
            Tipo derecho = visit(ctx.multiplicativeExpr(i));

            if (izquierdo == Tipo.ERROR || derecho == Tipo.ERROR) {
                return Tipo.ERROR;
            }

            if (!izquierdo.esNumerico() || !derecho.esNumerico()) {
                return Tipo.ERROR;
            }

            izquierdo = Tipo.INTEGER;
        }

        return izquierdo;
    }

    @Override
    public Tipo visitMultiplicativeExpr(
            CompiscriptParser.MultiplicativeExprContext ctx
    ) {
        Tipo izquierdo = visit(ctx.unaryExpr(0));

        if (ctx.unaryExpr().size() == 1) {
            return izquierdo;
        }

        for (int i = 1; i < ctx.unaryExpr().size(); i++) {
            Tipo derecho = visit(ctx.unaryExpr(i));

            if (izquierdo == Tipo.ERROR || derecho == Tipo.ERROR) {
                return Tipo.ERROR;
            }

            if (!izquierdo.esNumerico() || !derecho.esNumerico()) {
                return Tipo.ERROR;
            }

            izquierdo = Tipo.INTEGER;
        }

        return izquierdo;
    }
}