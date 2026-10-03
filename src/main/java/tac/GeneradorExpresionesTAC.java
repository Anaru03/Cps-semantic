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
                || nodo instanceof CompiscriptParser.NewExprContext
                || nodo instanceof CompiscriptParser.PropertyAssignExprContext
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


    // ------------------------------------------------------------------
    // Ternario y operadores lógicos con cortocircuito
    // ------------------------------------------------------------------

    @Override public String visitTernaryExpr(CompiscriptParser.TernaryExprContext ctx) {
        if (ctx.expression().isEmpty()) return visit(ctx.logicalOrExpr());
        String condicion = visit(ctx.logicalOrExpr());
        String siVerdadero = generador.nuevaEtiqueta(), siFalso = generador.nuevaEtiqueta(), fin = generador.nuevaEtiqueta();
        generador.generarSaltoCondicional(condicion, siVerdadero);
        generador.generarSalto(siFalso);
        liberarSiTemporal(condicion);
        String resultado = generador.temporales().nuevoTemporal();
        generador.emitirEtiqueta(siVerdadero);
        String a = visit(ctx.expression(0));
        generador.generarAsignacion(resultado, a); liberarSiTemporal(a);
        generador.generarSalto(fin);
        generador.emitirEtiqueta(siFalso);
        String b = visit(ctx.expression(1));
        generador.generarAsignacion(resultado, b); liberarSiTemporal(b);
        generador.emitirEtiqueta(fin);
        return resultado;
    }

    /** a && b: si a es falso se omite b. a || b: si a es verdadero se omite b. */
    private String cortocircuito(java.util.List<? extends org.antlr.v4.runtime.ParserRuleContext> operandos, boolean esAnd) {
        String primero = visit(operandos.get(0));
        if (operandos.size() == 1) return primero;
        String resultado = primero;
        if (primero == null || !primero.matches("t\\d+")) { // un temporal propio se reutiliza como resultado
            resultado = generador.temporales().nuevoTemporal();
            generador.generarAsignacion(resultado, primero);
        }
        String fin = generador.nuevaEtiqueta();
        for (int i = 1; i < operandos.size(); i++) {
            if (esAnd) {
                String continuar = generador.nuevaEtiqueta();
                generador.generarSaltoCondicional(resultado, continuar);
                generador.generarSalto(fin);
                generador.emitirEtiqueta(continuar);
            } else generador.generarSaltoCondicional(resultado, fin);
            String valor = visit(operandos.get(i));
            generador.generarAsignacion(resultado, valor);
            liberarSiTemporal(valor);
        }
        generador.emitirEtiqueta(fin);
        return resultado;
    }
    @Override public String visitLogicalAndExpr(CompiscriptParser.LogicalAndExprContext ctx) {
        return cortocircuito(ctx.equalityExpr(), true);
    }
    @Override public String visitLogicalOrExpr(CompiscriptParser.LogicalOrExprContext ctx) {
        return cortocircuito(ctx.logicalAndExpr(), false);
    }

    // ------------------------------------------------------------------
    // Literales, arreglos, objetos y accesos
    // ------------------------------------------------------------------

    @Override public String visitLiteralExpr(CompiscriptParser.LiteralExprContext ctx) {
        if (ctx.arrayLiteral() == null) return ctx.getText();
        var elementos = ctx.arrayLiteral().expression();
        String arreglo = generador.temporales().nuevoTemporal();
        generador.emitir(InstruccionTAC.nuevoArreglo(elementos.size(), arreglo));
        for (int i = 0; i < elementos.size(); i++) {
            String valor = visit(elementos.get(i));
            generador.emitir(InstruccionTAC.escrituraArreglo(arreglo, Integer.toString(i), valor));
            liberarSiTemporal(valor);
        }
        return arreglo;
    }

    @Override public String visitIdentifierExpr(CompiscriptParser.IdentifierExprContext ctx) {
        if (generador.esAtributo(ctx)) { // atributo usado sin 'this': acceso implícito al objeto actual
            String objeto = generador.resolverNombre("this");
            String resultado = generador.temporales().nuevoTemporal();
            generador.emitir(InstruccionTAC.lecturaCampo(objeto, ctx.getText(), resultado));
            return resultado;
        }
        return generador.resolverNombre(ctx, ctx.getText());
    }

    @Override public String visitThisExpr(CompiscriptParser.ThisExprContext ctx) {
        return generador.resolverNombre("this");
    }

    @Override public String visitNewExpr(CompiscriptParser.NewExprContext ctx) {
        String nombre = ctx.Identifier().getText();
        var clase = generador.clase(nombre);
        String objeto = generador.temporales().nuevoTemporal();
        generador.emitir(InstruccionTAC.nuevoObjeto(nombre, objeto));
        generador.tiparTemporal(objeto, nombre);
        if (clase.tieneInicializadores()) {
            generador.emitir(InstruccionTAC.argumento(objeto));
            generador.emitir(InstruccionTAC.llamada(nombre + ".$init", 1, null));
        }
        var args = ctx.arguments() == null
                ? java.util.List.<CompiscriptParser.ExpressionContext>of() : ctx.arguments().expression();
        String etiqueta = generador.etiquetaConstructor(nombre);
        if (etiqueta != null) emitirLlamada(generador.funcion(etiqueta), etiqueta, objeto, args, false);
        else if (!args.isEmpty()) throw new IllegalArgumentException("La clase " + nombre + " no tiene constructor");
        return objeto;
    }

    private java.util.List<CompiscriptParser.ExpressionContext> argumentos(CompiscriptParser.CallExprContext llamada) {
        return llamada.arguments() == null
                ? java.util.List.<CompiscriptParser.ExpressionContext>of() : llamada.arguments().expression();
    }

    /**
     * Emite la secuencia de una llamada. Cada argumento se evalúa de izquierda a derecha y se copia
     * a un temporal antes de evaluar el siguiente. El receptor (si existe) es el argumento 0.
     * No libera el receptor: pertenece a quien lo creó.
     */
    private String emitirLlamada(DescriptorFuncion funcion, String etiqueta, String receptor,
                                 java.util.List<CompiscriptParser.ExpressionContext> args, boolean virtual) {
        int extra = receptor == null ? 0 : 1;
        if (args.size() + extra != funcion.parametros().size())
            throw new IllegalArgumentException("Cantidad de argumentos incompatible: " + funcion.nombre());
        var enviar = new java.util.ArrayList<String>();
        var copias = new java.util.ArrayList<String>();
        try {
            boolean efectos = args.stream().anyMatch(this::tieneEfectos);
            if (receptor != null) {
                if (efectos && !receptor.matches("t\\d+")) {
                    String copia = generador.temporales().nuevoTemporal();
                    generador.generarAsignacion(copia, receptor);
                    copias.add(copia); enviar.add(copia);
                } else enviar.add(receptor);
            }
            for (var arg : args) {
                String valor = visit(arg);
                if (valor == null) throw new IllegalArgumentException("Argumento sin valor");
                String copia = generador.temporales().nuevoTemporal();
                generador.generarAsignacion(copia, valor);
                var tipo = generador.informacion().tipos().get(arg);
                if (tipo != null) generador.tiparTemporal(copia, tipo.toString());
                liberarSiTemporal(valor);
                copias.add(copia); enviar.add(copia);
            }
            for (String valor : enviar) generador.emitir(InstruccionTAC.argumento(valor));
            String resultado = funcion.devuelveValor() ? generador.temporales().nuevoTemporal() : null;
            generador.emitir(virtual ? InstruccionTAC.llamadaVirtual(etiqueta, enviar.size(), resultado)
                    : InstruccionTAC.llamada(etiqueta, enviar.size(), resultado));
            return resultado;
        } finally { for (String valor : copias) generador.liberarTemporal(valor); }
    }

    private String llamarMetodo(String claseEstatica, String nombre, String receptor,
                                CompiscriptParser.CallExprContext llamada) {
        var metodo = generador.clase(claseEstatica).metodo(nombre);
        if (metodo == null) throw new IllegalArgumentException("Método desconocido: " + claseEstatica + "." + nombre);
        return emitirLlamada(generador.funcion(metodo.etiqueta()), metodo.etiqueta(), receptor,
                argumentos(llamada), generador.requiereVirtual(claseEstatica, metodo));
    }

    private semantic.TipoDato tipoDe(org.antlr.v4.runtime.ParserRuleContext nodo) {
        var tipo = generador.informacion().tipos().get(nodo);
        if (tipo == null) throw new IllegalStateException("Se requiere información semántica para: " + nodo.getText());
        return tipo;
    }

    /** Evalúa el átomo y los primeros {@code n} sufijos; devuelve el operando resultante. */
    private String cadena(CompiscriptParser.LeftHandSideContext ctx, int n) {
        var atomo = ctx.primaryAtom();
        if (n == 0) return visit(atomo);
        String actual; int i = 0;
        if (atomo instanceof CompiscriptParser.IdentifierExprContext id
                && ctx.suffixOp(0) instanceof CompiscriptParser.CallExprContext llamada) {
            String nombre = id.Identifier().getText();
            if (generador.esMetodo(id)) // método de la clase actual invocado sin 'this'
                actual = llamarMetodo(generador.claseActual(), nombre, generador.resolverNombre("this"), llamada);
            else actual = emitirLlamada(generador.funcion(nombre), nombre, null, argumentos(llamada), false);
            i = 1;
        } else actual = visit(atomo);
        for (; i < n; i++) {
            var sufijo = ctx.suffixOp(i);
            if (sufijo instanceof CompiscriptParser.IndexExprContext indice) {
                String posicion = visit(indice.expression());
                String resultado = generador.temporales().nuevoTemporal();
                generador.emitir(InstruccionTAC.lecturaArreglo(actual, posicion, resultado));
                liberarSiTemporal(actual); liberarSiTemporal(posicion);
                actual = resultado;
            } else if (sufijo instanceof CompiscriptParser.PropertyAccessExprContext propiedad) {
                String nombre = propiedad.Identifier().getText();
                var previo = tipoDe(i == 0 ? atomo : ctx.suffixOp(i - 1));
                if (i + 1 < n && ctx.suffixOp(i + 1) instanceof CompiscriptParser.CallExprContext llamada) {
                    String receptor = actual;
                    actual = llamarMetodo(previo.nombreClase(), nombre, receptor, llamada);
                    liberarSiTemporal(receptor);
                    i++;
                } else {
                    String resultado = generador.temporales().nuevoTemporal();
                    if (previo.base() == semantic.Tipo.ARRAY && nombre.equals("length"))
                        generador.emitir(InstruccionTAC.longitudArreglo(actual, resultado));
                    else generador.emitir(InstruccionTAC.lecturaCampo(actual, nombre, resultado));
                    liberarSiTemporal(actual);
                    actual = resultado;
                }
            } else throw new UnsupportedOperationException("Llamada sobre un valor no soportada: " + ctx.getText());
            var tipo = generador.informacion().tipos().get(ctx.suffixOp(Math.min(i, n - 1)));
            if (tipo != null && tipo != semantic.TipoDato.VOID) generador.tiparTemporal(actual, tipo.toString());
        }
        return actual;
    }

    @Override public String visitLeftHandSide(CompiscriptParser.LeftHandSideContext ctx) {
        if (ctx.suffixOp().isEmpty()) return visit(ctx.primaryAtom());
        return cadena(ctx, ctx.suffixOp().size());
    }

    // ------------------------------------------------------------------
    // Asignaciones: variable, atributo implícito, propiedad y elemento de arreglo
    // ------------------------------------------------------------------

    private String escribirAtributoImplicito(org.antlr.v4.runtime.ParserRuleContext ctx, String nombre, String valor) {
        generador.emitir(InstruccionTAC.escrituraCampo(generador.resolverNombre("this"), nombre, valor));
        return valor;
    }

    @Override public String visitAssignment(CompiscriptParser.AssignmentContext ctx) {
        if (ctx.expression().size() == 2) { // expression '.' Identifier '=' expression
            String objeto = visit(ctx.expression(0));
            String valor = visit(ctx.expression(1));
            generador.emitir(InstruccionTAC.escrituraCampo(objeto, ctx.Identifier().getText(), valor));
            liberarSiTemporal(objeto);
            return valor;
        }
        String nombre = ctx.Identifier().getText();
        if (generador.esAtributo(ctx)) {
            return escribirAtributoImplicito(ctx, nombre, visit(ctx.expression(0)));
        }
        String identificador = generador.resolverNombre(ctx, nombre);
        String valor = visit(ctx.expression(0));
        generador.generarAsignacion(identificador, valor);
        liberarSiTemporal(valor);
        return identificador;
    }

    @Override public String visitAssignExpr(CompiscriptParser.AssignExprContext ctx) {
        var lhs = ctx.lhs;
        if (lhs.suffixOp().isEmpty()) {
            String nombre = lhs.getText();
            if (generador.esAtributo(ctx)) return escribirAtributoImplicito(ctx, nombre, visit(ctx.assignmentExpr()));
            String destino = generador.resolverNombre(ctx, nombre);
            String valor = visit(ctx.assignmentExpr());
            generador.generarAsignacion(destino, valor);
            liberarSiTemporal(valor);
            return destino;
        }
        int n = lhs.suffixOp().size();
        String contenedor = cadena(lhs, n - 1);
        var ultimo = lhs.suffixOp(n - 1);
        if (ultimo instanceof CompiscriptParser.IndexExprContext indice) {
            String posicion = visit(indice.expression());
            String valor = visit(ctx.assignmentExpr());
            generador.emitir(InstruccionTAC.escrituraArreglo(contenedor, posicion, valor));
            liberarSiTemporal(contenedor); liberarSiTemporal(posicion);
            return valor;
        }
        if (ultimo instanceof CompiscriptParser.PropertyAccessExprContext propiedad) {
            String valor = visit(ctx.assignmentExpr());
            generador.emitir(InstruccionTAC.escrituraCampo(contenedor, propiedad.Identifier().getText(), valor));
            liberarSiTemporal(contenedor);
            return valor;
        }
        throw new UnsupportedOperationException("Destino de asignación no soportado: " + lhs.getText());
    }

    @Override public String visitPropertyAssignExpr(CompiscriptParser.PropertyAssignExprContext ctx) {
        String objeto = visit(ctx.lhs);
        String valor = visit(ctx.assignmentExpr());
        generador.emitir(InstruccionTAC.escrituraCampo(objeto, ctx.Identifier().getText(), valor));
        liberarSiTemporal(objeto);
        return valor;
    }

    private void liberarSiTemporal(String valor) {

        if (valor != null
                && valor.matches("t\\d+")) {

            generador.liberarTemporal(valor);
        }
    }
}
