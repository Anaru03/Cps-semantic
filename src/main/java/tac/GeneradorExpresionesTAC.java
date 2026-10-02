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

    @Override public String visit(org.antlr.v4.runtime.tree.ParseTree nodo) {
        String resultado;
        try { resultado = super.visit(nodo); }
        catch (UnsupportedOperationException | IllegalArgumentException | IllegalStateException | NullPointerException error) {
            if (!generador.informacion().tipos().isEmpty() && nodo instanceof org.antlr.v4.runtime.ParserRuleContext ctx)
                throw new ErrorGeneracionTAC(ctx, error);
            throw error;
        }
        if (nodo instanceof org.antlr.v4.runtime.ParserRuleContext ctx) {
            var tipo = generador.informacion().tipos().get(ctx);
            if (tipo != null && tipo != semantic.TipoDato.VOID)
                generador.tiparTemporal(resultado, tipo.toString());
        }
        return resultado;
    }

    private String operacion(org.antlr.v4.runtime.ParserRuleContext ctx,
                             String operador, String izquierdo, String derecho) {
        String resultado = generador.generarOperacion(operador, izquierdo, derecho);
        var tipo = generador.informacion().tipos().get(ctx);
        if (tipo != null) generador.tiparTemporal(resultado, tipo.toString());
        return resultado;
    }

    private boolean tieneEfectos(org.antlr.v4.runtime.tree.ParseTree nodo) {
        if (nodo instanceof CompiscriptParser.CallExprContext
                || nodo instanceof CompiscriptParser.AssignExprContext) return true;
        for (int i = 0; i < nodo.getChildCount(); i++) if (tieneEfectos(nodo.getChild(i))) return true;
        return false;
    }
    private String congelar(String valor, org.antlr.v4.runtime.ParserRuleContext ctx) {
        String copia = generador.temporales().nuevoTemporal();
        generador.generarAsignacion(copia, valor);
        var tipo = generador.informacion().tipos().get(ctx);
        if (tipo != null) generador.tiparTemporal(copia, tipo.toString());
        liberarSiTemporal(valor);
        return copia;
    }

    @Override public String visitTernaryExpr(CompiscriptParser.TernaryExprContext ctx) {
        if (!ctx.expression().isEmpty()) throw new UnsupportedOperationException("Ternario TAC no implementado");
        return visit(ctx.logicalOrExpr());
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
                var tipo = generador.informacion().tipos().get(arg);
                if (tipo != null) generador.tiparTemporal(copia, tipo.toString());
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

        return generador.resolverNombre(ctx, ctx.getText());
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

            if (tieneEfectos(ctx.unaryExpr(i))) izquierdo = congelar(izquierdo, ctx);

            String derecho = visit(ctx.unaryExpr(i));

            String resultado =
                    operacion(ctx,
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

            if (tieneEfectos(ctx.multiplicativeExpr(i))) izquierdo = congelar(izquierdo, ctx);

            String derecho = visit(ctx.multiplicativeExpr(i));

            String resultado =
                    operacion(ctx,
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

            if (tieneEfectos(ctx.additiveExpr(i))) izquierdo = congelar(izquierdo, ctx);

            String derecho = visit(ctx.additiveExpr(i));

            String resultado =
                    operacion(ctx,
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

            if (tieneEfectos(ctx.relationalExpr(i))) izquierdo = congelar(izquierdo, ctx);

            String derecho = visit(ctx.relationalExpr(i));

            String resultado =
                    operacion(ctx,
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

            if (tieneEfectos(ctx.equalityExpr(i))) izquierdo = congelar(izquierdo, ctx);

            String derecho = visit(ctx.equalityExpr(i));

            String resultado =
                    operacion(ctx,
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

            if (tieneEfectos(ctx.logicalAndExpr(i))) izquierdo = congelar(izquierdo, ctx);

            String derecho = visit(ctx.logicalAndExpr(i));

            String resultado =
                    operacion(ctx,
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
                generador.declarar(ctx, ctx.Identifier().getText(),
                        ctx.typeAnnotation() == null ? "unknown" : ctx.typeAnnotation().type().getText());

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

    @Override public String visitConstantDeclaration(CompiscriptParser.ConstantDeclarationContext ctx) {
        String valor = visit(ctx.expression());
        String destino = generador.declarar(ctx, ctx.Identifier().getText(),
                ctx.typeAnnotation() == null ? "unknown" : ctx.typeAnnotation().type().getText());
        generador.generarAsignacion(destino, valor);
        liberarSiTemporal(valor);
        return destino;
    }

    @Override
    public String visitAssignment(
            CompiscriptParser.AssignmentContext ctx) {

        if (ctx.Identifier() == null
                || ctx.expression().size() != 1) {

            return null;
        }

        String identificador =
                generador.resolverNombre(ctx, ctx.Identifier().getText());

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
                generador.resolverNombre(ctx, ctx.lhs.getText());

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
