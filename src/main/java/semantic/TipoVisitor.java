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
}