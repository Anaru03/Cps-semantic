package semantic;

import antlr.CompiscriptBaseVisitor;
import antlr.CompiscriptLexer;
import antlr.CompiscriptParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Analizador de nombres, ambitos, funciones y clases de Compiscript. */
public final class AnalizadorSemantico extends CompiscriptBaseVisitor<TipoDato> {
    private final List<ErrorSemantico> errores = new ArrayList<>();
    private final Ambito global = new Ambito("global", null);
    private Ambito actual = global;
    private Simbolo funcionActual;
    private Simbolo claseActual;

    public static AnalisisSemantico analizar(String fuente) {
        CompiscriptParser parser = new CompiscriptParser(new CommonTokenStream(
                new CompiscriptLexer(CharStreams.fromString(fuente))));
        AnalizadorSemantico analizador = new AnalizadorSemantico();
        analizador.visit(parser.program());
        return new AnalisisSemantico(new ResultadoSemantico(analizador.errores), analizador.global);
    }

    private void error(ParserRuleContext ctx, String mensaje) {
        errores.add(new ErrorSemantico(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), mensaje));
    }
    private void declarar(ParserRuleContext ctx, Simbolo simbolo) {
        if (!actual.declarar(simbolo)) error(ctx, "El identificador '" + simbolo.nombre() + "' ya fue declarado en este ambito");
    }
    private TipoDato tipo(CompiscriptParser.TypeContext ctx) {
        if (ctx == null) return TipoDato.UNKNOWN;
        String base = ctx.baseType().getText();
        TipoDato t = switch (base) {
            case "integer" -> TipoDato.INTEGER; case "string" -> TipoDato.STRING;
            case "boolean" -> TipoDato.BOOLEAN; default -> TipoDato.clase(base);
        };
        for (int i = 1; i < ctx.getChildCount(); i += 2) t = TipoDato.arreglo(t);
        return t;
    }
    private TipoDato tipoAnotado(CompiscriptParser.TypeAnnotationContext ctx) {
        return ctx == null ? TipoDato.UNKNOWN : tipo(ctx.type());
    }

    @Override public TipoDato visitBlock(CompiscriptParser.BlockContext ctx) {
        Ambito anterior = actual; actual = new Ambito("bloque", anterior);
        for (var s : ctx.statement()) visit(s);
        actual = anterior; return TipoDato.VOID;
    }
    private void visitarBloqueSinNuevoAmbito(CompiscriptParser.BlockContext ctx) {
        for (var s : ctx.statement()) visit(s);
    }
    @Override public TipoDato visitVariableDeclaration(CompiscriptParser.VariableDeclarationContext ctx) {
        TipoDato declarado = tipoAnotado(ctx.typeAnnotation());
        TipoDato valor = ctx.initializer() == null ? TipoDato.UNKNOWN : visit(ctx.initializer().expression());
        if (declarado != TipoDato.UNKNOWN && valor != TipoDato.UNKNOWN && !declarado.compatibleCon(valor))
            error(ctx, "No se puede inicializar " + declarado + " con " + valor);
        if (declarado == TipoDato.UNKNOWN) declarado = valor;
        declarar(ctx, new Simbolo(ctx.Identifier().getText(), declarado,
                claseActual != null && funcionActual == null ? CategoriaSimbolo.ATRIBUTO : CategoriaSimbolo.VARIABLE, actual));
        return TipoDato.VOID;
    }
    @Override public TipoDato visitConstantDeclaration(CompiscriptParser.ConstantDeclarationContext ctx) {
        TipoDato declarado = tipoAnotado(ctx.typeAnnotation()); TipoDato valor = visit(ctx.expression());
        if (declarado != TipoDato.UNKNOWN && !declarado.compatibleCon(valor))
            error(ctx, "No se puede inicializar " + declarado + " con " + valor);
        if (declarado == TipoDato.UNKNOWN) declarado = valor;
        declarar(ctx, new Simbolo(ctx.Identifier().getText(), declarado,
                claseActual != null && funcionActual == null ? CategoriaSimbolo.ATRIBUTO : CategoriaSimbolo.CONSTANTE, actual));
        return TipoDato.VOID;
    }

    @Override public TipoDato visitAssignment(CompiscriptParser.AssignmentContext ctx) {
        if (ctx.expression().size() == 1)
            return asignar(ctx, ctx.Identifier().getText(), visit(ctx.expression(0)));
        // La alternativa de propiedad se valida al resolver la expresion receptora.
        TipoDato receptor = visit(ctx.expression(0));
        if (receptor.base() != Tipo.CLASS) error(ctx, "Solo se pueden asignar propiedades de objetos");
        return visit(ctx.expression(ctx.expression().size() - 1));
    }

    @Override public TipoDato visitAssignExpr(CompiscriptParser.AssignExprContext ctx) {
        String nombre = ctx.lhs.getText();
        if (ctx.lhs.suffixOp().isEmpty() && ctx.lhs.primaryAtom() instanceof CompiscriptParser.IdentifierExprContext)
            return asignar(ctx, nombre, visit(ctx.assignmentExpr()));
        visit(ctx.lhs); return visit(ctx.assignmentExpr());
    }

    private TipoDato asignar(ParserRuleContext ctx, String nombre, TipoDato valor) {
        Optional<Simbolo> encontrado = actual.buscar(nombre);
        if (encontrado.isEmpty()) { error(ctx, "El identificador '" + nombre + "' no esta declarado"); return TipoDato.ERROR; }
        Simbolo simbolo = encontrado.get();
        if (simbolo.categoria() == CategoriaSimbolo.CONSTANTE) error(ctx, "No se puede reasignar la constante '" + nombre + "'");
        else if (!simbolo.tipo().compatibleCon(valor)) error(ctx, "No se puede asignar " + valor + " a " + simbolo.tipo());
        return simbolo.tipo();
    }

    @Override public TipoDato visitFunctionDeclaration(CompiscriptParser.FunctionDeclarationContext ctx) {
        List<TipoDato> params = new ArrayList<>();
        if (ctx.parameters() != null) for (var p : ctx.parameters().parameter())
            params.add(p.type() == null ? TipoDato.UNKNOWN : tipo(p.type()));
        TipoDato retorno = ctx.type() == null ? TipoDato.VOID : tipo(ctx.type());
        CategoriaSimbolo cat = claseActual == null ? CategoriaSimbolo.FUNCION : CategoriaSimbolo.METODO;
        Ambito cuerpo = new Ambito("funcion " + ctx.Identifier().getText(), actual);
        Simbolo funcion = new Simbolo(ctx.Identifier().getText(), retorno, cat, actual, params, cuerpo);
        declarar(ctx, funcion); // antes del cuerpo: habilita recursion
        Ambito previo = actual; Simbolo previaFuncion = funcionActual;
        actual = cuerpo; funcionActual = funcion;
        if (ctx.parameters() != null) for (int i = 0; i < ctx.parameters().parameter().size(); i++) {
            var p = ctx.parameters().parameter(i);
            declarar(p, new Simbolo(p.Identifier().getText(), params.get(i), CategoriaSimbolo.PARAMETRO, actual));
        }
        visitarBloqueSinNuevoAmbito(ctx.block()); actual = previo; funcionActual = previaFuncion;
        return TipoDato.VOID;
    }

    @Override public TipoDato visitReturnStatement(CompiscriptParser.ReturnStatementContext ctx) {
        if (funcionActual == null) { error(ctx, "return solo puede utilizarse dentro de una funcion"); return TipoDato.ERROR; }
        TipoDato valor = ctx.expression() == null ? TipoDato.VOID : visit(ctx.expression());
        if (!funcionActual.tipo().compatibleCon(valor)) error(ctx, "El retorno debe ser " + funcionActual.tipo() + " pero se obtuvo " + valor);
        return valor;
    }

    @Override public TipoDato visitClassDeclaration(CompiscriptParser.ClassDeclarationContext ctx) {
        String nombre = ctx.Identifier(0).getText(); Ambito miembros = new Ambito("clase " + nombre, actual);
        Simbolo clase = new Simbolo(nombre, TipoDato.clase(nombre), CategoriaSimbolo.CLASE, actual, List.of(), miembros);
        declarar(ctx, clase);
        Ambito previo = actual; Simbolo previaClase = claseActual; actual = miembros; claseActual = clase;
        for (var m : ctx.classMember()) visit(m); actual = previo; claseActual = previaClase;
        return TipoDato.VOID;
    }

    @Override public TipoDato visitIdentifierExpr(CompiscriptParser.IdentifierExprContext ctx) {
        return actual.buscar(ctx.Identifier().getText()).map(Simbolo::tipo).orElseGet(() -> {
            error(ctx, "El identificador '" + ctx.Identifier().getText() + "' no esta declarado"); return TipoDato.ERROR;
        });
    }
    @Override public TipoDato visitThisExpr(CompiscriptParser.ThisExprContext ctx) {
        if (claseActual == null) { error(ctx, "this solo puede utilizarse dentro de una clase"); return TipoDato.ERROR; }
        return claseActual.tipo();
    }
    @Override public TipoDato visitNewExpr(CompiscriptParser.NewExprContext ctx) {
        String nombre = ctx.Identifier().getText(); Optional<Simbolo> encontrado = actual.buscar(nombre);
        if (encontrado.isEmpty() || encontrado.get().categoria() != CategoriaSimbolo.CLASE) {
            error(ctx, "La clase '" + nombre + "' no esta declarada"); return TipoDato.ERROR;
        }
        Simbolo clase = encontrado.get(); Optional<Simbolo> ctor = clase.miembros().buscarLocal("constructor");
        List<CompiscriptParser.ExpressionContext> args = ctx.arguments() == null ? List.of() : ctx.arguments().expression();
        if (ctor.isPresent()) validarArgumentos(ctx, ctor.get(), args);
        else if (!args.isEmpty()) error(ctx, "La clase '" + nombre + "' no tiene constructor");
        return clase.tipo();
    }

    @Override public TipoDato visitLeftHandSide(CompiscriptParser.LeftHandSideContext ctx) {
        TipoDato corriente = visit(ctx.primaryAtom());
        Simbolo invocable = ctx.primaryAtom() instanceof CompiscriptParser.IdentifierExprContext id
                ? actual.buscar(id.Identifier().getText()).orElse(null) : null;
        for (var sufijo : ctx.suffixOp()) {
            if (sufijo instanceof CompiscriptParser.PropertyAccessExprContext propiedad) {
                if (corriente.base() != Tipo.CLASS) { error(propiedad, "Solo los objetos tienen miembros"); corriente = TipoDato.ERROR; continue; }
                Simbolo clase = global.buscar(corriente.nombreClase()).orElse(null);
                invocable = clase == null ? null : clase.miembros().buscarLocal(propiedad.Identifier().getText()).orElse(null);
                if (invocable == null) { error(propiedad, "La clase '" + corriente.nombreClase() + "' no contiene '" + propiedad.Identifier().getText() + "'"); corriente = TipoDato.ERROR; }
                else corriente = invocable.tipo();
            } else if (sufijo instanceof CompiscriptParser.CallExprContext llamada) {
                if (invocable == null || (invocable.categoria() != CategoriaSimbolo.FUNCION && invocable.categoria() != CategoriaSimbolo.METODO)) {
                    error(llamada, "La expresion no es una funcion o metodo"); corriente = TipoDato.ERROR;
                } else {
                    var args = llamada.arguments() == null ? List.<CompiscriptParser.ExpressionContext>of() : llamada.arguments().expression();
                    validarArgumentos(llamada, invocable, args); corriente = invocable.tipo();
                }
                invocable = null;
            } else if (sufijo instanceof CompiscriptParser.IndexExprContext indice) {
                TipoDato it = visit(indice.expression());
                if (corriente.base() != Tipo.ARRAY || it.base() != Tipo.INTEGER) { error(indice, "Acceso de arreglo invalido"); corriente = TipoDato.ERROR; }
                else corriente = corriente.elemento();
                invocable = null;
            }
        }
        return corriente;
    }
    private void validarArgumentos(ParserRuleContext ctx, Simbolo funcion, List<CompiscriptParser.ExpressionContext> args) {
        if (args.size() != funcion.parametros().size()) {
            error(ctx, "'" + funcion.nombre() + "' espera " + funcion.parametros().size() + " argumentos, se recibieron " + args.size());
        }
        for (int i = 0; i < args.size(); i++) {
            TipoDato recibido = visit(args.get(i));
            if (i < funcion.parametros().size() && !funcion.parametros().get(i).compatibleCon(recibido))
                error(args.get(i), "El argumento " + (i + 1) + " debe ser " + funcion.parametros().get(i) + " pero se obtuvo " + recibido);
        }
    }

    @Override public TipoDato visitLiteralExpr(CompiscriptParser.LiteralExprContext ctx) {
        if (ctx.arrayLiteral() != null) {
            var xs = ctx.arrayLiteral().expression(); if (xs.isEmpty()) return TipoDato.arreglo(TipoDato.UNKNOWN);
            TipoDato e = visit(xs.get(0)); for (int i=1;i<xs.size();i++) if (!e.compatibleCon(visit(xs.get(i)))) return TipoDato.ERROR;
            return TipoDato.arreglo(e);
        }
        String s=ctx.getText(); if (s.equals("true")||s.equals("false")) return TipoDato.BOOLEAN;
        if (s.equals("null")) return TipoDato.NULL; if (s.startsWith("\"")) return TipoDato.STRING; return TipoDato.INTEGER;
    }
    @Override public TipoDato visitChildren(org.antlr.v4.runtime.tree.RuleNode node) {
        TipoDato r = TipoDato.UNKNOWN;
        for (int i=0;i<node.getChildCount();i++) if (node.getChild(i) instanceof org.antlr.v4.runtime.tree.RuleNode n) {
            TipoDato x=n.accept(this); if (x!=null && x!=TipoDato.UNKNOWN) r=x;
        }
        return r;
    }
}
