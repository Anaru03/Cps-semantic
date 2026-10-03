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
    private record ContextoControl(String salida, String continuacion, int profundidadTry) { }
    private final Deque<ContextoControl> contextos = new ArrayDeque<>();
    private DescriptorFuncion funcionActual;
    private int tryActivos;
    private int profundidadBloque;

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
        for (var sentencia : ctx.statement()) if (sentencia.classDeclaration() != null)
            registrarClase(sentencia.classDeclaration());
        for (var sentencia : ctx.statement()) visit(sentencia);
        return null;
    }

    /** Calcula el layout del objeto, la tabla de métodos y las firmas (con 'this' implícito). */
    private void registrarClase(CompiscriptParser.ClassDeclarationContext ctx) {
        String nombre = ctx.Identifier(0).getText();
        String padre = ctx.Identifier().size() > 1 ? ctx.Identifier(1).getText() : null;
        DescriptorClase base = padre != null && generador.existeClase(padre) ? generador.clase(padre) : null;
        var campos = new java.util.ArrayList<DescriptorClase.Campo>(base == null ? java.util.List.of() : base.campos());
        var tabla = new java.util.ArrayList<DescriptorClase.Metodo>(base == null ? java.util.List.of() : base.tabla());
        boolean inicializadores = base != null && base.tieneInicializadores();
        for (var miembro : ctx.classMember()) {
            if (miembro.variableDeclaration() != null) {
                var v = miembro.variableDeclaration();
                if (v.initializer() != null) inicializadores = true;
                campos.add(new DescriptorClase.Campo(v.Identifier().getText(),
                        generador.tipoDeclarado(v, v.typeAnnotation() == null ? "unknown" : v.typeAnnotation().type().getText()),
                        DescriptorClase.CABECERA_OBJETO + campos.size(), nombre, false));
            } else if (miembro.constantDeclaration() != null) {
                var c = miembro.constantDeclaration();
                inicializadores = true;
                campos.add(new DescriptorClase.Campo(c.Identifier().getText(),
                        generador.tipoDeclarado(c, c.typeAnnotation() == null ? "unknown" : c.typeAnnotation().type().getText()),
                        DescriptorClase.CABECERA_OBJETO + campos.size(), nombre, true));
            } else {
                var f = miembro.functionDeclaration();
                String metodo = f.Identifier().getText();
                String etiqueta = nombre + "." + metodo;
                var parametros = new java.util.ArrayList<DescriptorFuncion.Parametro>();
                parametros.add(new DescriptorFuncion.Parametro("this", nombre));
                if (f.parameters() != null) for (var parametro : f.parameters().parameter())
                    parametros.add(new DescriptorFuncion.Parametro(parametro.Identifier().getText(),
                            generador.tipoDeclarado(parametro, parametro.type() == null ? "unknown" : parametro.type().getText())));
                generador.registrarFuncion(new DescriptorFuncion(etiqueta, parametros,
                        f.type() == null ? "void" : f.type().getText()));
                if (metodo.equals("constructor")) continue;
                int existente = -1;
                for (int i = 0; i < tabla.size(); i++) if (tabla.get(i).nombre().equals(metodo)) existente = i;
                if (existente >= 0) tabla.set(existente, new DescriptorClase.Metodo(metodo, etiqueta, nombre,
                        tabla.get(existente).ranura(), true));
                else tabla.add(new DescriptorClase.Metodo(metodo, etiqueta, nombre, tabla.size(), false));
            }
        }
        if (inicializadores) generador.registrarFuncion(new DescriptorFuncion(nombre + ".$init",
                java.util.List.of(new DescriptorFuncion.Parametro("this", nombre)), "void"));
        generador.registrarClase(new DescriptorClase(nombre, padre, campos, tabla, inicializadores));
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
        if (ctx.classDeclaration() != null) return visit(ctx.classDeclaration());
        if (ctx.tryCatchStatement() != null) return visit(ctx.tryCatchStatement());
        if (ctx.printStatement() != null) {
            String valor = Objects.requireNonNull(expresiones.visit(ctx.printStatement().expression()),
                    "Expresión de print sin valor TAC");
            generador.emitir(InstruccionTAC.imprimir(valor));
            generador.liberarTemporal(valor);
            return null;
        }
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

    @Override public Void visitBlock(CompiscriptParser.BlockContext ctx) {
        generador.entrarAmbito(); profundidadBloque++;
        try { for (var sentencia : ctx.statement()) visit(sentencia); }
        finally { generador.salirAmbito(); profundidadBloque--; }
        return null;
    }


    // ---------------- funciones, métodos y clases ----------------

    @Override public Void visitFunctionDeclaration(CompiscriptParser.FunctionDeclarationContext ctx) {
        if (funcionActual != null || !contextos.isEmpty() || profundidadBloque > 0)
            throw new UnsupportedOperationException("Funciones anidadas no soportadas en TAC");
        var parametros = ctx.parameters() == null ? java.util.List.<CompiscriptParser.ParameterContext>of()
                : ctx.parameters().parameter();
        emitirFuncion(ctx.Identifier().getText(), parametros, ctx.block(), null, true);
        return null;
    }

    /**
     * Emite el cuerpo de una función o método. {@code cuerpo} puede ser null cuando el contenido
     * lo produce {@code generarCuerpo} (inicializador de atributos); {@code saltar} omite el
     * código de la función durante la ejecución del programa principal.
     */
    private void emitirFuncion(String etiqueta, java.util.List<CompiscriptParser.ParameterContext> parametros,
                               CompiscriptParser.BlockContext cuerpo, Runnable generarCuerpo, boolean saltar) {
        var funcion = generador.funcion(etiqueta);
        if (cuerpo != null && funcion.devuelveValor() && !garantizaRetorno(cuerpo))
            throw new IllegalArgumentException("No se garantiza retorno en función: " + funcion.nombre());
        String despues = saltar ? generador.nuevaEtiqueta() : null;
        if (saltar) generador.generarSalto(despues);
        generador.emitir(InstruccionTAC.funcion(funcion.nombre()));
        generador.iniciarFuncion(funcion.nombre());
        for (var parametro : parametros)
            generador.vincular(parametro, generador.resolverNombre(parametro.Identifier().getText()));
        for (int i = 0; i < funcion.parametros().size(); i++)
            generador.emitir(InstruccionTAC.parametro(
                    generador.resolverNombre(funcion.parametros().get(i).nombre()), i));
        funcionActual = funcion; tryActivos = 0;
        try {
            if (cuerpo != null) for (var sentencia : cuerpo.statement()) visit(sentencia);
            else generarCuerpo.run();
        } finally { funcionActual = null; generador.finalizarFuncion(); }
        if (!funcion.devuelveValor()) generador.emitir(InstruccionTAC.retorno(null));
        generador.emitir(InstruccionTAC.finFuncion(funcion.nombre()));
        if (saltar) generador.emitirEtiqueta(despues);
    }

    @Override public Void visitClassDeclaration(CompiscriptParser.ClassDeclarationContext ctx) {
        if (funcionActual != null || !contextos.isEmpty() || profundidadBloque > 0)
            throw new UnsupportedOperationException("Clases anidadas no soportadas en TAC");
        String nombre = ctx.Identifier(0).getText();
        var descriptor = generador.clase(nombre);
        String despues = generador.nuevaEtiqueta();
        generador.generarSalto(despues);
        generador.emitir(InstruccionTAC.clase(nombre, descriptor.padre()));
        generador.claseActual(nombre);
        try {
            if (descriptor.tieneInicializadores()) emitirFuncion(nombre + ".$init", java.util.List.of(), null,
                    () -> generarInicializadores(ctx, descriptor), false);
            for (var miembro : ctx.classMember()) {
                var f = miembro.functionDeclaration();
                if (f == null) continue;
                var parametros = f.parameters() == null ? java.util.List.<CompiscriptParser.ParameterContext>of()
                        : f.parameters().parameter();
                emitirFuncion(nombre + "." + f.Identifier().getText(), parametros, f.block(), null, false);
            }
        } finally { generador.claseActual(null); }
        generador.emitir(InstruccionTAC.finClase(nombre));
        generador.emitirEtiqueta(despues);
        return null;
    }

    /** Inicializa los atributos propios (después de los de la superclase) con sus valores declarados. */
    private void generarInicializadores(CompiscriptParser.ClassDeclarationContext ctx, DescriptorClase descriptor) {
        String objeto = generador.resolverNombre("this");
        if (descriptor.padre() != null && generador.requiereInicializacion(descriptor.padre())) {
            generador.emitir(InstruccionTAC.argumento(objeto));
            generador.emitir(InstruccionTAC.llamada(descriptor.padre() + ".$init", 1, null));
        }
        for (var miembro : ctx.classMember()) {
            String campo; CompiscriptParser.ExpressionContext valor;
            if (miembro.variableDeclaration() != null && miembro.variableDeclaration().initializer() != null) {
                campo = miembro.variableDeclaration().Identifier().getText();
                valor = miembro.variableDeclaration().initializer().expression();
            } else if (miembro.constantDeclaration() != null) {
                campo = miembro.constantDeclaration().Identifier().getText();
                valor = miembro.constantDeclaration().expression();
            } else continue;
            String resultado = Objects.requireNonNull(expresiones.visit(valor), "Inicializador sin valor TAC");
            generador.emitir(InstruccionTAC.escrituraCampo(objeto, campo, resultado));
            generador.liberarTemporal(resultado);
        }
    }

    // ---------------- retornos, saltos y try/catch ----------------

    private void cerrarTry(int hasta) { for (int i = tryActivos; i > hasta; i--) generador.emitir(InstruccionTAC.finTry()); }

    @Override public Void visitReturnStatement(CompiscriptParser.ReturnStatementContext ctx) {
        if (funcionActual == null) throw new IllegalStateException("return fuera de una función");
        if (funcionActual.devuelveValor() != (ctx.expression() != null))
            throw new IllegalArgumentException("Retorno incompatible: " + funcionActual.nombre());
        String valor = ctx.expression() == null ? null : expresiones.visit(ctx.expression());
        if (ctx.expression() != null && valor == null) throw new IllegalArgumentException("Retorno sin valor");
        cerrarTry(0); // un return dentro de try desinstala todos los manejadores activos
        generador.emitir(InstruccionTAC.retorno(valor));
        generador.liberarTemporal(valor);
        return null;
    }

    @Override public Void visitBreakStatement(CompiscriptParser.BreakStatementContext ctx) {
        if (contextos.isEmpty()) throw new IllegalStateException("break fuera de un ciclo o switch");
        cerrarTry(contextos.peek().profundidadTry());
        generador.generarSalto(contextos.peek().salida());
        return null;
    }

    @Override public Void visitContinueStatement(CompiscriptParser.ContinueStatementContext ctx) {
        for (var contexto : contextos) {
            if (contexto.continuacion() != null) {
                cerrarTry(contexto.profundidadTry());
                generador.generarSalto(contexto.continuacion());
                return null;
            }
        }
        throw new IllegalStateException("continue fuera de un ciclo");
    }

    @Override public Void visitTryCatchStatement(CompiscriptParser.TryCatchStatementContext ctx) {
        String captura = generador.nuevaEtiqueta(), fin = generador.nuevaEtiqueta();
        generador.emitir(InstruccionTAC.inicioTry(captura));
        tryActivos++;
        try { visit(ctx.block(0)); } finally { tryActivos--; }
        generador.emitir(InstruccionTAC.finTry());
        generador.generarSalto(fin);
        generador.emitirEtiqueta(captura);
        generador.entrarAmbito();
        try {
            String variable = generador.declarar(ctx, ctx.Identifier().getText(), "string");
            generador.emitir(InstruccionTAC.captura(variable));
            visit(ctx.block(1));
        } finally { generador.salirAmbito(); }
        generador.emitirEtiqueta(fin);
        return null;
    }

    // Comprobación conservadora: retorno directo, bloque, if/else, try/catch o switch con default
    // cuyos cuerpos retornan; un ciclo nunca se considera garantía de retorno.
    private boolean garantizaRetorno(CompiscriptParser.BlockContext bloque) { return garantizaRetorno(bloque.statement()); }

    private boolean garantizaRetorno(java.util.List<CompiscriptParser.StatementContext> sentencias) {
        for (var sentencia : sentencias) {
            if (sentencia.returnStatement() != null) return true;
            if (sentencia.block() != null && garantizaRetorno(sentencia.block())) return true;
            var intento = sentencia.tryCatchStatement();
            if (intento != null && garantizaRetorno(intento.block(0)) && garantizaRetorno(intento.block(1))) return true;
            var condicional = sentencia.ifStatement();
            if (condicional != null && condicional.block().size() == 2
                    && garantizaRetorno(condicional.block(0)) && garantizaRetorno(condicional.block(1))) return true;
            var seleccion = sentencia.switchStatement();
            if (seleccion != null && seleccion.defaultCase() != null
                    && garantizaRetorno(seleccion.defaultCase().statement())
                    && seleccion.switchCase().stream().allMatch(c -> c.statement().isEmpty() || garantizaRetorno(c.statement())))
                return true;
        }
        return false;
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
        contextos.push(new ContextoControl(salida, continuacion, tryActivos));
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
        contextos.push(new ContextoControl(salida, null, tryActivos));
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
