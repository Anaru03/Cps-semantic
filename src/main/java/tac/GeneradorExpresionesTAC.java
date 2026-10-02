package tac;

import antlr.CompiscriptBaseVisitor;
import antlr.CompiscriptParser;

public final class GeneradorExpresionesTAC
        extends CompiscriptBaseVisitor<String> {

    private final GeneradorTAC generador;

    public GeneradorExpresionesTAC() {
        this(new GeneradorTAC());
    }

    public GeneradorExpresionesTAC(GeneradorTAC generador) {
        this.generador = java.util.Objects.requireNonNull(generador);
    }

    public GeneradorTAC generador() {
        return generador;
    }

    public String codigo() {
        return generador.codigo();
    }

    @Override
    public String visitLiteralExpr(
            CompiscriptParser.LiteralExprContext ctx) {

        if (ctx.arrayLiteral() != null) {
            return null;
        }

        return ctx.getText();
    }

    @Override
    public String visitIdentifierExpr(
            CompiscriptParser.IdentifierExprContext ctx) {

        return ctx.getText();
    }

    @Override
    public String visitPrimaryExpr(
            CompiscriptParser.PrimaryExprContext ctx) {

        if (ctx.expression() != null) {
            return visit(ctx.expression());
        }

        if (ctx.literalExpr() != null) {
            return visit(ctx.literalExpr());
        }

        if (ctx.leftHandSide() != null) {
            return visit(ctx.leftHandSide());
        }

        return null;
    }

    @Override
    public String visitUnaryExpr(
            CompiscriptParser.UnaryExprContext ctx) {

        if (ctx.getChildCount() == 2) {

            String operador =
                    ctx.getChild(0).getText();

            String operando =
                    visit(ctx.unaryExpr());

            String resultado =
                    generador.generarOperacionUnaria(
                            operador,
                            operando
                    );

            liberarSiTemporal(operando);

            return resultado;
        }

        return visit(ctx.primaryExpr());
    }

    @Override
    public String visitMultiplicativeExpr(
            CompiscriptParser.MultiplicativeExprContext ctx) {

        String izquierdo =
                visit(ctx.unaryExpr(0));

        for (int i = 1;
             i < ctx.unaryExpr().size();
             i++) {

            String operador =
                    ctx.getChild(2 * i - 1).getText();

            String derecho =
                    visit(ctx.unaryExpr(i));

            String resultado =
                    generador.generarOperacion(
                            operador,
                            izquierdo,
                            derecho
                    );

            liberarSiTemporal(izquierdo);
            liberarSiTemporal(derecho);

            izquierdo = resultado;
        }

        return izquierdo;
    }

    @Override
    public String visitAdditiveExpr(
            CompiscriptParser.AdditiveExprContext ctx) {

        String izquierdo =
                visit(ctx.multiplicativeExpr(0));

        for (int i = 1;
             i < ctx.multiplicativeExpr().size();
             i++) {

            String operador =
                    ctx.getChild(2 * i - 1).getText();

            String derecho =
                    visit(ctx.multiplicativeExpr(i));

            String resultado =
                    generador.generarOperacion(
                            operador,
                            izquierdo,
                            derecho
                    );

            liberarSiTemporal(izquierdo);
            liberarSiTemporal(derecho);

            izquierdo = resultado;
        }

        return izquierdo;
    }

    @Override
    public String visitRelationalExpr(
            CompiscriptParser.RelationalExprContext ctx) {

        String izquierdo =
                visit(ctx.additiveExpr(0));

        for (int i = 1;
             i < ctx.additiveExpr().size();
             i++) {

            String operador =
                    ctx.getChild(2 * i - 1).getText();

            String derecho =
                    visit(ctx.additiveExpr(i));

            String resultado =
                    generador.generarOperacion(
                            operador,
                            izquierdo,
                            derecho
                    );

            liberarSiTemporal(izquierdo);
            liberarSiTemporal(derecho);

            izquierdo = resultado;
        }

        return izquierdo;
    }

    @Override
    public String visitEqualityExpr(
            CompiscriptParser.EqualityExprContext ctx) {

        String izquierdo =
                visit(ctx.relationalExpr(0));

        for (int i = 1;
             i < ctx.relationalExpr().size();
             i++) {

            String operador =
                    ctx.getChild(2 * i - 1).getText();

            String derecho =
                    visit(ctx.relationalExpr(i));

            String resultado =
                    generador.generarOperacion(
                            operador,
                            izquierdo,
                            derecho
                    );

            liberarSiTemporal(izquierdo);
            liberarSiTemporal(derecho);

            izquierdo = resultado;
        }

        return izquierdo;
    }

    @Override
    public String visitLogicalAndExpr(
            CompiscriptParser.LogicalAndExprContext ctx) {

        String izquierdo =
                visit(ctx.equalityExpr(0));

        for (int i = 1;
             i < ctx.equalityExpr().size();
             i++) {

            String derecho =
                    visit(ctx.equalityExpr(i));

            String resultado =
                    generador.generarOperacion(
                            "&&",
                            izquierdo,
                            derecho
                    );

            liberarSiTemporal(izquierdo);
            liberarSiTemporal(derecho);

            izquierdo = resultado;
        }

        return izquierdo;
    }

    @Override
    public String visitLogicalOrExpr(
            CompiscriptParser.LogicalOrExprContext ctx) {

        String izquierdo =
                visit(ctx.logicalAndExpr(0));

        for (int i = 1;
             i < ctx.logicalAndExpr().size();
             i++) {

            String derecho =
                    visit(ctx.logicalAndExpr(i));

            String resultado =
                    generador.generarOperacion(
                            "||",
                            izquierdo,
                            derecho
                    );

            liberarSiTemporal(izquierdo);
            liberarSiTemporal(derecho);

            izquierdo = resultado;
        }

        return izquierdo;
    }

    @Override
    public String visitVariableDeclaration(
            CompiscriptParser.VariableDeclarationContext ctx) {

        String identificador =
                ctx.Identifier().getText();

        if (ctx.initializer() == null) {
            return identificador;
        }

        String valor =
                visit(ctx.initializer().expression());

        generador.generarAsignacion(
                identificador,
                valor
        );

        liberarSiTemporal(valor);

        return identificador;
    }

    @Override
    public String visitAssignment(
            CompiscriptParser.AssignmentContext ctx) {

        if (ctx.Identifier() == null
                || ctx.expression().size() != 1) {

            return null;
        }

        String identificador =
                ctx.Identifier().getText();

        String valor =
                visit(ctx.expression(0));

        generador.generarAsignacion(
                identificador,
                valor
        );

        liberarSiTemporal(valor);

        return identificador;
    }

    @Override
    public String visitAssignExpr(
            CompiscriptParser.AssignExprContext ctx) {

        String destino =
                ctx.lhs.getText();

        String valor =
                visit(ctx.assignmentExpr());

        generador.generarAsignacion(
                destino,
                valor
        );

        return destino;
    }

    private void liberarSiTemporal(String valor) {

        if (valor != null
                && valor.matches("t\\d+")) {

            generador.liberarTemporal(valor);
        }
    }
}
