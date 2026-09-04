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
    private boolean enBucle = false;

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
    /** Registra un error semantico y devuelve TipoDato.ERROR, para usar directamente en un return. */
    private TipoDato errorTipo(ParserRuleContext ctx, String mensaje) {
        error(ctx, mensaje);
        return TipoDato.ERROR;
    }
    private boolean esNumerico(TipoDato t) { return t.base() == Tipo.INTEGER || t.base() == Tipo.FLOAT; }
    private TipoDato promocionNumerica(TipoDato a, TipoDato b) {
        return (a.base() == Tipo.FLOAT || b.base() == Tipo.FLOAT) ? TipoDato.FLOAT : TipoDato.INTEGER;
    }
    private void declarar(ParserRuleContext ctx, Simbolo simbolo) {
        if (!actual.declarar(simbolo)) error(ctx, "El identificador '" + simbolo.nombre() + "' ya fue declarado en este ambito");
    }
    private TipoDato tipo(CompiscriptParser.TypeContext ctx) {
        if (ctx == null) return TipoDato.UNKNOWN;
        String base = ctx.baseType().getText();
        TipoDato t = switch (base) {
            case "integer" -> TipoDato.INTEGER; case "float" -> TipoDato.FLOAT; case "string" -> TipoDato.STRING;
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
        visitarSentencias(ctx.statement());
        actual = anterior; return TipoDato.VOID;
    }
    private void visitarBloqueSinNuevoAmbito(CompiscriptParser.BlockContext ctx) {
        visitarSentencias(ctx.statement());
    }
    /** Visita una lista de sentencias y marca como codigo muerto todo lo que venga
     *  despues de un return/break/continue dentro del mismo bloque. */
    private void visitarSentencias(List<CompiscriptParser.StatementContext> sentencias) {
        boolean inalcanzable = false;
        for (var s : sentencias) {
            if (inalcanzable) error(s, "Codigo muerto: esta instruccion nunca se ejecuta");
            visit(s);
            if (s.returnStatement() != null || s.breakStatement() != null || s.continueStatement() != null) inalcanzable = true;
        }
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

    // ---------------------------------------------------------------
    // Operadores: aritmeticos, logicos, comparaciones y ternario
    // ---------------------------------------------------------------

    @Override public TipoDato visitLogicalOrExpr(CompiscriptParser.LogicalOrExprContext ctx) {
        TipoDato resultado = visit(ctx.logicalAndExpr(0));
        for (int i = 1; i < ctx.logicalAndExpr().size(); i++) {
            TipoDato derecho = visit(ctx.logicalAndExpr(i));
            if (resultado.base() != Tipo.BOOLEAN || derecho.base() != Tipo.BOOLEAN)
                return errorTipo(ctx, "Los operandos de '||' deben ser boolean, se obtuvo " + resultado + " y " + derecho);
            resultado = TipoDato.BOOLEAN;
        }
        return resultado;
    }

    @Override public TipoDato visitLogicalAndExpr(CompiscriptParser.LogicalAndExprContext ctx) {
        TipoDato resultado = visit(ctx.equalityExpr(0));
        for (int i = 1; i < ctx.equalityExpr().size(); i++) {
            TipoDato derecho = visit(ctx.equalityExpr(i));
            if (resultado.base() != Tipo.BOOLEAN || derecho.base() != Tipo.BOOLEAN)
                return errorTipo(ctx, "Los operandos de '&&' deben ser boolean, se obtuvo " + resultado + " y " + derecho);
            resultado = TipoDato.BOOLEAN;
        }
        return resultado;
    }

    @Override public TipoDato visitEqualityExpr(CompiscriptParser.EqualityExprContext ctx) {
        TipoDato izquierdo = visit(ctx.relationalExpr(0));
        if (ctx.relationalExpr().size() == 1) return izquierdo;
        for (int i = 1; i < ctx.relationalExpr().size(); i++) {
            TipoDato derecho = visit(ctx.relationalExpr(i));
            if (!izquierdo.compatibleCon(derecho)) return errorTipo(ctx, "No se puede comparar " + izquierdo + " con " + derecho);
            izquierdo = derecho;
        }
        return TipoDato.BOOLEAN;
    }

    @Override public TipoDato visitRelationalExpr(CompiscriptParser.RelationalExprContext ctx) {
        TipoDato izquierdo = visit(ctx.additiveExpr(0));
        if (ctx.additiveExpr().size() == 1) return izquierdo;
        for (int i = 1; i < ctx.additiveExpr().size(); i++) {
            TipoDato derecho = visit(ctx.additiveExpr(i));
            if (!esNumerico(izquierdo) || !esNumerico(derecho))
                return errorTipo(ctx, "Los operandos de una comparacion relacional deben ser integer o float, se obtuvo " + izquierdo + " y " + derecho);
            izquierdo = derecho;
        }
        return TipoDato.BOOLEAN;
    }

    @Override public TipoDato visitAdditiveExpr(CompiscriptParser.AdditiveExprContext ctx) {
        TipoDato izquierdo = visit(ctx.multiplicativeExpr(0));
        for (int i = 1; i < ctx.multiplicativeExpr().size(); i++) {
            TipoDato derecho = visit(ctx.multiplicativeExpr(i));
            if (!esNumerico(izquierdo) || !esNumerico(derecho))
                return errorTipo(ctx, "Los operandos de '+'/'-' deben ser integer o float, se obtuvo " + izquierdo + " y " + derecho);
            izquierdo = promocionNumerica(izquierdo, derecho);
        }
        return izquierdo;
    }

    @Override public TipoDato visitMultiplicativeExpr(CompiscriptParser.MultiplicativeExprContext ctx) {
        TipoDato izquierdo = visit(ctx.unaryExpr(0));
        for (int i = 1; i < ctx.unaryExpr().size(); i++) {
            TipoDato derecho = visit(ctx.unaryExpr(i));
            if (!esNumerico(izquierdo) || !esNumerico(derecho))
                return errorTipo(ctx, "Los operandos de '*'/'/'/'%' deben ser integer o float, se obtuvo " + izquierdo + " y " + derecho);
            izquierdo = promocionNumerica(izquierdo, derecho);
        }
        return izquierdo;
    }

    @Override public TipoDato visitUnaryExpr(CompiscriptParser.UnaryExprContext ctx) {
        if (ctx.getChildCount() == 2) {
            String operador = ctx.getChild(0).getText();
            TipoDato operando = visit(ctx.unaryExpr());
            if ("!".equals(operador))
                return operando.base() == Tipo.BOOLEAN ? TipoDato.BOOLEAN : errorTipo(ctx, "El operador '!' requiere un operando boolean, se obtuvo " + operando);
            if ("-".equals(operador))
                return esNumerico(operando) ? operando : errorTipo(ctx, "El operador '-' unario requiere un operando integer o float, se obtuvo " + operando);
        }
        return visitChildren(ctx);
    }

    @Override public TipoDato visitTernaryExpr(CompiscriptParser.TernaryExprContext ctx) {
        TipoDato condicion = visit(ctx.logicalOrExpr());
        if (ctx.expression().isEmpty()) return condicion;
        if (condicion.base() != Tipo.BOOLEAN) error(ctx, "La condicion del operador ternario debe ser boolean, se obtuvo " + condicion);
        TipoDato siVerdadero = visit(ctx.expression(0));
        TipoDato siFalso = visit(ctx.expression(1));
        if (!siVerdadero.compatibleCon(siFalso))
            return errorTipo(ctx, "Las dos ramas del operador ternario deben ser del mismo tipo, se obtuvo " + siVerdadero + " y " + siFalso);
        return siVerdadero;
    }

    // ---------------------------------------------------------------
    // Control de flujo
    // ---------------------------------------------------------------

    private void validarCondicionBooleana(ParserRuleContext etiqueta, CompiscriptParser.ExpressionContext condExpr, String construccion) {
        TipoDato condicion = visit(condExpr);
        if (condicion.base() != Tipo.BOOLEAN) error(etiqueta, "La condicion del " + construccion + " debe ser boolean, se obtuvo " + condicion);
    }

    @Override public TipoDato visitIfStatement(CompiscriptParser.IfStatementContext ctx) {
        validarCondicionBooleana(ctx, ctx.expression(), "if");
        visit(ctx.block(0));
        if (ctx.block().size() > 1) visit(ctx.block(1));
        return TipoDato.VOID;
    }

    @Override public TipoDato visitWhileStatement(CompiscriptParser.WhileStatementContext ctx) {
        validarCondicionBooleana(ctx, ctx.expression(), "while");
        boolean bucleAnterior = enBucle; enBucle = true;
        visit(ctx.block());
        enBucle = bucleAnterior;
        return TipoDato.VOID;
    }

    @Override public TipoDato visitDoWhileStatement(CompiscriptParser.DoWhileStatementContext ctx) {
        boolean bucleAnterior = enBucle; enBucle = true;
        visit(ctx.block());
        enBucle = bucleAnterior;
        validarCondicionBooleana(ctx, ctx.expression(), "do-while");
        return TipoDato.VOID;
    }

    @Override public TipoDato visitForStatement(CompiscriptParser.ForStatementContext ctx) {
        Ambito anterior = actual; actual = new Ambito("for", anterior);
        if (ctx.variableDeclaration() != null) visit(ctx.variableDeclaration());
        else if (ctx.assignment() != null) visit(ctx.assignment());

        // El init (variableDeclaration | assignment | ';') es siempre el hijo 2 (indice 2),
        // ya que 'for' y '(' ocupan los indices 0 y 1. A partir de ahi, la primera expresion
        // encontrada antes del ';' propio del for es la condicion; la siguiente es el incremento.
        CompiscriptParser.ExpressionContext condicion = null;
        CompiscriptParser.ExpressionContext incremento = null;
        boolean pasoPuntoYComa = false;
        for (int i = 3; i < ctx.getChildCount(); i++) {
            var hijo = ctx.getChild(i);
            if (hijo instanceof CompiscriptParser.ExpressionContext expr) {
                if (!pasoPuntoYComa) condicion = expr; else incremento = expr;
            } else if (";".equals(hijo.getText())) {
                pasoPuntoYComa = true;
            }
        }
        if (condicion != null) validarCondicionBooleana(ctx, condicion, "for");

        boolean bucleAnterior = enBucle; enBucle = true;
        visit(ctx.block());
        enBucle = bucleAnterior;
        if (incremento != null) visit(incremento);

        actual = anterior;
        return TipoDato.VOID;
    }

    @Override public TipoDato visitForeachStatement(CompiscriptParser.ForeachStatementContext ctx) {
        TipoDato iterable = visit(ctx.expression());
        TipoDato elemento;
        if (iterable.base() == Tipo.ARRAY) {
            elemento = iterable.elemento();
        } else {
            error(ctx, "foreach requiere una expresion de tipo arreglo, se obtuvo " + iterable);
            elemento = TipoDato.ERROR;
        }
        Ambito anterior = actual; actual = new Ambito("foreach", anterior);
        declarar(ctx, new Simbolo(ctx.Identifier().getText(), elemento, CategoriaSimbolo.VARIABLE, actual));
        boolean bucleAnterior = enBucle; enBucle = true;
        visit(ctx.block());
        enBucle = bucleAnterior;
        actual = anterior;
        return TipoDato.VOID;
    }

    @Override public TipoDato visitSwitchStatement(CompiscriptParser.SwitchStatementContext ctx) {
        TipoDato tipoSwitch = visit(ctx.expression());
        Ambito anterior = actual; actual = new Ambito("switch", anterior);
        for (var c : ctx.switchCase()) {
            TipoDato tipoCase = visit(c.expression());
            if (!tipoSwitch.compatibleCon(tipoCase))
                error(c, "El tipo del case (" + tipoCase + ") no coincide con el tipo del switch (" + tipoSwitch + ")");
            visitarSentencias(c.statement());
        }
        if (ctx.defaultCase() != null) visitarSentencias(ctx.defaultCase().statement());
        actual = anterior;
        return TipoDato.VOID;
    }

    @Override public TipoDato visitBreakStatement(CompiscriptParser.BreakStatementContext ctx) {
        if (!enBucle) error(ctx, "'break' solo puede utilizarse dentro de un bucle");
        return TipoDato.VOID;
    }

    @Override public TipoDato visitContinueStatement(CompiscriptParser.ContinueStatementContext ctx) {
        if (!enBucle) error(ctx, "'continue' solo puede utilizarse dentro de un bucle");
        return TipoDato.VOID;
    }

    @Override public TipoDato visitTryCatchStatement(CompiscriptParser.TryCatchStatementContext ctx) {
        visit(ctx.block(0));
        Ambito anterior = actual; actual = new Ambito("catch", anterior);
        declarar(ctx, new Simbolo(ctx.Identifier().getText(), TipoDato.UNKNOWN, CategoriaSimbolo.VARIABLE, actual));
        visit(ctx.block(1));
        actual = anterior;
        return TipoDato.VOID;
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
        if (ctx.suffixOp().isEmpty() && invocable != null
                && (invocable.categoria() == CategoriaSimbolo.FUNCION
                    || invocable.categoria() == CategoriaSimbolo.METODO
                    || invocable.categoria() == CategoriaSimbolo.CLASE)) {
            error(ctx, "'" + invocable.nombre() + "' no se puede utilizar como valor sin invocarlo");
            corriente = TipoDato.ERROR;
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
        if (s.equals("null")) return TipoDato.NULL; if (s.startsWith("\"")) return TipoDato.STRING;
        if (s.indexOf('.') >= 0) return TipoDato.FLOAT; return TipoDato.INTEGER;
    }
    @Override public TipoDato visitChildren(org.antlr.v4.runtime.tree.RuleNode node) {
        TipoDato r = TipoDato.UNKNOWN;
        for (int i=0;i<node.getChildCount();i++) if (node.getChild(i) instanceof org.antlr.v4.runtime.tree.RuleNode n) {
            TipoDato x=n.accept(this); if (x!=null && x!=TipoDato.UNKNOWN) r=x;
        }
        return r;
    }
}
