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
    private DescriptorFuncion funcionActual;

    @Override public Void visit(org.antlr.v4.runtime.tree.ParseTree nodo) {
        try { return super.visit(nodo); }
        catch (UnsupportedOperationException | IllegalArgumentException | IllegalStateException | NullPointerException error) {
            if (!generador.informacion().tipos().isEmpty() && nodo instanceof org.antlr.v4.runtime.ParserRuleContext ctx)
                throw new ErrorGeneracionTAC(ctx, error);
            throw error;
        }
    }

    @Override public Void visitProgram(CompiscriptParser.ProgramContext ctx) {
        reservarIdentificadores(ctx);
        for (var sentencia : ctx.statement()) {
            var funcion = sentencia.functionDeclaration();
            if (funcion != null) {
                var parametros = new java.util.ArrayList<DescriptorFuncion.Parametro>();
                if (funcion.parameters() != null) for (var parametro : funcion.parameters().parameter())
                    parametros.add(new DescriptorFuncion.Parametro(parametro.Identifier().getText(),
                            generador.tipoDeclarado(parametro, parametro.type() == null ? "unknown" : parametro.type().getText())));
                generador.registrarFuncion(new DescriptorFuncion(funcion.Identifier().getText(), parametros,
                        funcion.type() == null ? "void" : funcion.type().getText()));
            }
        }
        for (var sentencia : ctx.statement()) visit(sentencia);
        return null;
    }

    private void reservarIdentificadores(org.antlr.v4.runtime.tree.ParseTree nodo) {
        if (nodo instanceof org.antlr.v4.runtime.tree.TerminalNode terminal
                && terminal.getSymbol().getType() == CompiscriptParser.Identifier)
            generador.temporales().reservarNombre(terminal.getText());
        for (int i = 0; i < nodo.getChildCount(); i++) reservarIdentificadores(nodo.getChild(i));
    }

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
        if (ctx.foreachStatement() != null) return visit(ctx.foreachStatement());
        if (ctx.switchStatement() != null) return visit(ctx.switchStatement());
        if (ctx.functionDeclaration() != null) return visit(ctx.functionDeclaration());
        if (ctx.returnStatement() != null) return visit(ctx.returnStatement());
        if (ctx.breakStatement() != null) return visit(ctx.breakStatement());
        if (ctx.continueStatement() != null) return visit(ctx.continueStatement());
        if (ctx.block() != null) return visit(ctx.block());
        if (ctx.variableDeclaration() != null || ctx.constantDeclaration() != null || ctx.assignment() != null
                || ctx.expressionStatement() != null) {
            String valor = ctx.expressionStatement() == null ? expresiones.visit(ctx.getChild(0))
                    : expresiones.visit(ctx.expressionStatement().expression());
            generador.liberarTemporal(valor);
            return null;
        }
        throw new UnsupportedOperationException("Sentencia TAC no implementada: "
                + ctx.getStart().getText() + " en línea " + ctx.getStart().getLine());
    }

    @Override public Void visitFunctionDeclaration(CompiscriptParser.FunctionDeclarationContext ctx) {
        if (funcionActual != null || !contextos.isEmpty())
            throw new UnsupportedOperationException("Funciones anidadas no soportadas en TAC");
        var funcion = generador.funcion(ctx.Identifier().getText());
        if (funcion.devuelveValor() && !garantizaRetorno(ctx.block()))
            throw new IllegalArgumentException("No se garantiza retorno en función: " + funcion.nombre());
        String despues = generador.nuevaEtiqueta();
        generador.generarSalto(despues);
        generador.emitir(InstruccionTAC.funcion(funcion.nombre()));
        generador.iniciarFuncion(funcion.nombre());
        if (ctx.parameters() != null) for (var parametro : ctx.parameters().parameter())
            generador.vincular(parametro, generador.resolverNombre(parametro.Identifier().getText()));
        for (int i = 0; i < funcion.parametros().size(); i++)
            generador.emitir(InstruccionTAC.parametro(
                    generador.resolverNombre(funcion.parametros().get(i).nombre()), i));
        funcionActual = funcion;
        try { for (var sentencia : ctx.block().statement()) visit(sentencia); }
        finally { funcionActual = null; generador.finalizarFuncion(); }
        // Retorno implícito únicamente para funciones sin valor.
        if (!funcion.devuelveValor()) generador.emitir(InstruccionTAC.retorno(null));
        generador.emitir(InstruccionTAC.finFuncion(funcion.nombre()));
        generador.emitirEtiqueta(despues);
        return null;
    }

    @Override public Void visitBlock(CompiscriptParser.BlockContext ctx) {
        generador.entrarAmbito();
        try { for (var sentencia : ctx.statement()) visit(sentencia); }
        finally { generador.salirAmbito(); }
        return null;
    }

    // Comprobación conservadora: retorno directo, bloque o if con ambas ramas.
    private boolean garantizaRetorno(CompiscriptParser.BlockContext bloque) {
        for (var sentencia : bloque.statement()) {
            if (sentencia.returnStatement() != null) return true;
            if (sentencia.block() != null && garantizaRetorno(sentencia.block())) return true;
            var condicional = sentencia.ifStatement();
            if (condicional != null && condicional.block().size() == 2
                    && garantizaRetorno(condicional.block(0)) && garantizaRetorno(condicional.block(1))) return true;
        }
        return false;
    }

    @Override public Void visitReturnStatement(CompiscriptParser.ReturnStatementContext ctx) {
        if (funcionActual == null) throw new IllegalStateException("return fuera de una función");
        if (funcionActual.devuelveValor() != (ctx.expression() != null))
            throw new IllegalArgumentException("Retorno incompatible: " + funcionActual.nombre());
        String valor = ctx.expression() == null ? null : expresiones.visit(ctx.expression());
        if (ctx.expression() != null && valor == null) throw new IllegalArgumentException("Retorno sin valor");
        generador.emitir(InstruccionTAC.retorno(valor));
        generador.liberarTemporal(valor);
        return null;
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
        generador.entrarAmbito();
        try {
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
        } finally { generador.salirAmbito(); }
    }

    @Override public Void visitForeachStatement(CompiscriptParser.ForeachStatementContext ctx) {
        String valor = Objects.requireNonNull(expresiones.visit(ctx.expression()), "Iterable TAC no soportado");
        String arreglo = generador.temporales().nuevoTemporal();
        generador.generarAsignacion(arreglo, valor);
        generador.liberarTemporal(valor);
        var tipo = generador.informacion().tipos().get(ctx.expression());
        if (tipo != null) generador.tiparTemporal(arreglo, tipo.toString());
        String longitud = generador.temporales().nuevoTemporal();
        String indice = generador.temporales().nuevoTemporal();
        generador.emitir(InstruccionTAC.longitudArreglo(arreglo, longitud));
        generador.generarAsignacion(indice, "0");
        generador.tiparTemporal(longitud, "integer");
        generador.tiparTemporal(indice, "integer");
        String condicion = generador.nuevaEtiqueta();
        String cuerpo = generador.nuevaEtiqueta();
        String incremento = generador.nuevaEtiqueta();
        String salida = generador.nuevaEtiqueta();
        generador.entrarAmbito();
        try {
            String elemento = generador.declarar(ctx, ctx.Identifier().getText(), "unknown");
            generador.emitirEtiqueta(condicion);
            String prueba = generador.generarOperacion("<", indice, longitud);
            generador.tiparTemporal(prueba, "boolean");
            generador.generarSaltoCondicional(prueba, cuerpo);
            generador.generarSalto(salida);
            generador.liberarTemporal(prueba);
            generador.emitirEtiqueta(cuerpo);
            generador.emitir(InstruccionTAC.lecturaArreglo(arreglo, indice, elemento));
            visitarCuerpo(ctx.block(), salida, incremento);
            generador.emitirEtiqueta(incremento);
            String siguiente = generador.generarOperacion("+", indice, "1");
            generador.tiparTemporal(siguiente, "integer");
            generador.generarAsignacion(indice, siguiente);
            generador.liberarTemporal(siguiente);
            generador.generarSalto(condicion);
            generador.emitirEtiqueta(salida);
        } finally {
            generador.salirAmbito();
            generador.liberarTemporal(arreglo);
            generador.liberarTemporal(longitud);
            generador.liberarTemporal(indice);
        }
        return null;
    }

    @Override public Void visitSwitchStatement(CompiscriptParser.SwitchStatementContext ctx) {
        String salida = generador.nuevaEtiqueta();
        var destinos = new java.util.ArrayList<String>();
        for (var caso : ctx.switchCase()) destinos.add(generador.nuevaEtiqueta());
        String defecto = ctx.defaultCase() == null ? salida : generador.nuevaEtiqueta();
        String valor = Objects.requireNonNull(expresiones.visit(ctx.expression()),
                "Selector TAC no soportado");
        // Congelar el selector: un case puede modificar la variable original.
        String selector = generador.temporales().nuevoTemporal();
        generador.generarAsignacion(selector, valor);
        var tipoSelector = generador.informacion().tipos().get(ctx.expression());
        if (tipoSelector != null) generador.tiparTemporal(selector, tipoSelector.toString());
        generador.liberarTemporal(valor);
        try {
            for (int i = 0; i < ctx.switchCase().size(); i++) {
                String caso = Objects.requireNonNull(expresiones.visit(ctx.switchCase(i).expression()),
                        "Expresión case TAC no soportada");
                String comparacion = generador.generarOperacion("==", selector, caso);
                generador.tiparTemporal(comparacion, "boolean");
                generador.generarSaltoCondicional(comparacion, destinos.get(i));
                generador.liberarTemporal(comparacion);
                generador.liberarTemporal(caso);
            }
            generador.generarSalto(defecto);
        } finally { generador.liberarTemporal(selector); }
        contextos.push(new ContextoControl(salida, null));
        generador.entrarAmbito();
        try {
            for (int i = 0; i < ctx.switchCase().size(); i++) {
                generador.emitirEtiqueta(destinos.get(i));
                for (var sentencia : ctx.switchCase(i).statement()) visit(sentencia);
            }
            if (ctx.defaultCase() != null) {
                generador.emitirEtiqueta(defecto);
                for (var sentencia : ctx.defaultCase().statement()) visit(sentencia);
            }
        } finally { contextos.pop(); generador.salirAmbito(); }
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
