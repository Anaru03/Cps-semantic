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

    @Override public String visitLeftHandSide(CompiscriptParser.LeftHandSideContext ctx) {
        if (ctx.suffixOp().isEmpty()) return visit(ctx.primaryAtom());
        if (!(ctx.primaryAtom() instanceof CompiscriptParser.IdentifierExprContext id)
                || ctx.suffixOp().size() != 1
                || !(ctx.suffixOp(0) instanceof CompiscriptParser.CallExprContext llamada))
            throw new UnsupportedOperationException("Acceso TAC no implementado: " + ctx.getText());
        var funcion = generador.funcion(id.Identifier().getText());
        var args = llamada.arguments() == null
                ? java.util.List.<CompiscriptParser.ExpressionContext>of() : llamada.arguments().expression();
        if (args.size() != funcion.parametros().size())
            throw new IllegalArgumentException("Cantidad de argumentos incompatible: " + funcion.nombre());
        var valores = new java.util.ArrayList<String>();
        try {
            // Congelar cada argumento antes de evaluar el siguiente; las llamadas
            // internas emiten sus propios arg/call antes de los argumentos externos.
            for (var arg : args) {
                String valor = visit(arg);
                if (valor == null) throw new IllegalArgumentException("Argumento sin valor");
                String copia = generador.temporales().nuevoTemporal();
                generador.generarAsignacion(copia, valor);
                liberarSiTemporal(valor);
                valores.add(copia);
            }
            for (String valor : valores) generador.emitir(InstruccionTAC.argumento(valor));
            String resultado = funcion.devuelveValor() ? generador.temporales().nuevoTemporal() : null;
            generador.emitir(InstruccionTAC.llamada(funcion.nombre(), valores.size(), resultado));
            return resultado;
        } finally { for (String valor : valores) generador.liberarTemporal(valor); }
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

            String derecho = visit(ctx.unaryExpr(i));

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

            String derecho = visit(ctx.multiplicativeExpr(i));

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

            String derecho = visit(ctx.additiveExpr(i));

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

            String derecho = visit(ctx.relationalExpr(i));

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

            String derecho = visit(ctx.equalityExpr(i));

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

            String derecho = visit(ctx.logicalAndExpr(i));

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

        // La semántica evalúa el inicializador antes de declarar el nombre nuevo.
        String valor = ctx.initializer() == null ? null : visit(ctx.initializer().expression());

        String identificador =
                ctx.Identifier().getText();

        if (ctx.initializer() == null) {
            return identificador;
        }

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

        liberarSiTemporal(valor);

        return destino;
    }

    private void liberarSiTemporal(String valor) {

        if (valor != null
                && valor.matches("t\\d+")) {

            generador.liberarTemporal(valor);
        }
    }
}
