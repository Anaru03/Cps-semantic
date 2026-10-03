package tac;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GeneradorTAC {

    private final List<InstruccionTAC> instrucciones;
    private final AdministradorTemporales temporales;
    private final AdministradorEtiquetas etiquetas = new AdministradorEtiquetas();
    private final java.util.Map<String, DescriptorFuncion> funciones = new java.util.LinkedHashMap<>();
    private final java.util.Map<String, RegistroActivacion> registros = new java.util.LinkedHashMap<>();
    private final java.util.Deque<java.util.Map<String, String>> ambitos = new java.util.ArrayDeque<>();
    private final java.util.List<RegistroActivacion.Posicion> posiciones = new java.util.ArrayList<>();
    private final java.util.Map<String, DescriptorClase> clases = new java.util.LinkedHashMap<>();
    private String funcionActiva;
    private String claseActual;
    private int siguienteLocal;
    private int siguienteGlobal;
    private semantic.InformacionSemantica informacion = semantic.InformacionSemantica.vacia();
    private final java.util.Map<semantic.InformacionSemantica.Referencia, EnlaceSimboloTAC> enlaces = new java.util.LinkedHashMap<>();

    public void usarInformacion(semantic.InformacionSemantica informacion) { this.informacion = informacion; }
    public semantic.InformacionSemantica informacion() { return informacion; }
    public java.util.List<EnlaceSimboloTAC> enlacesSimbolos() { return java.util.List.copyOf(enlaces.values()); }
    public String tipoDeclarado(org.antlr.v4.runtime.ParserRuleContext ctx, String respaldo) {
        var referencia = informacion.referencias().get(ctx);
        return referencia == null ? respaldo : referencia.simbolo().tipo().toString();
    }
    public void vincular(org.antlr.v4.runtime.ParserRuleContext ctx, String operando) {
        var referencia = informacion.referencias().get(ctx);
        if (referencia != null) {
            Integer offset = funcionActiva == null ? null : posiciones.stream()
                    .filter(p -> p.operando().equals(operando)).findFirst().orElseThrow().offset();
            enlaces.put(referencia, new EnlaceSimboloTAC(referencia, operando, funcionActiva, offset));
        }
    }
    public String declarar(org.antlr.v4.runtime.ParserRuleContext ctx, String nombre, String tipo) {
        String operando;
        var referencia = informacion.referencias().get(ctx);
        if (funcionActiva == null && referencia != null && referencia.ambito().padre() != null)
            operando = "%global." + siguienteGlobal++ + "." + nombre;
        else operando = declararLocal(nombre, tipoDeclarado(ctx, tipo), RegistroActivacion.Clase.LOCAL);
        vincular(ctx, operando);
        return operando;
    }
    public String resolverNombre(org.antlr.v4.runtime.ParserRuleContext ctx, String respaldo) {
        var referencia = informacion.referencias().get(ctx);
        if (referencia != null) {
            var enlace = enlaces.get(referencia);
            if (enlace == null) throw new IllegalStateException("Símbolo sin almacenamiento: " + referencia.nombre());
            return enlace.operando();
        }
        return resolverNombre(respaldo);
    }
    public void tiparTemporal(String operando, String tipo) {
        if (funcionActiva == null || operando == null || !temporales.estaEnUso(operando)) return;
        for (int i = 0; i < posiciones.size(); i++) {
            var p = posiciones.get(i);
            if (p.operando().equals(operando) && p.clase() == RegistroActivacion.Clase.TEMPORAL) {
                String nuevo = p.tipo().equals("unknown") || p.tipo().equals(tipo) ? tipo : "dynamic";
                posiciones.set(i, new RegistroActivacion.Posicion(p.operando(), p.nombre(), nuevo, p.clase(), p.offset()));
            }
        }
    }

    public java.util.Map<String, RegistroActivacion> registrosActivacion() {
        return java.util.Collections.unmodifiableMap(registros);
    }
    public void iniciarFuncion(String nombre) {
        if (funcionActiva != null) throw new IllegalStateException("Función activa");
        funcionActiva = nombre; siguienteLocal = 0; posiciones.clear(); ambitos.clear(); entrarAmbito();
        for (var parametro : funcion(nombre).parametros())
            declararLocal(parametro.nombre(), parametro.tipo(), RegistroActivacion.Clase.PARAMETRO);
    }
    public void finalizarFuncion() {
        if (funcionActiva == null) throw new IllegalStateException("Sin función activa");
        registros.put(funcionActiva, new RegistroActivacion(funcionActiva, posiciones));
        funcionActiva = null; posiciones.clear(); ambitos.clear();
    }
    public void entrarAmbito() { ambitos.push(new java.util.LinkedHashMap<>()); }
    public void salirAmbito() { ambitos.pop(); }
    public String declararLocal(String nombre, String tipo, RegistroActivacion.Clase clase) {
        if (funcionActiva == null) return nombre;
        if (ambitos.peek().containsKey(nombre)) throw new IllegalArgumentException("Local duplicado: " + nombre);
        String operando = "%" + funcionActiva + "." + siguienteLocal++ + "." + nombre;
        ambitos.peek().put(nombre, operando);
        posiciones.add(new RegistroActivacion.Posicion(operando, nombre, tipo, clase,
                RegistroActivacion.CABECERA + posiciones.size()));
        return operando;
    }
    public String resolverNombre(String nombre) {
        for (var ambito : ambitos) if (ambito.containsKey(nombre)) return ambito.get(nombre);
        return nombre; // global: vive fuera de cualquier registro de activación
    }
    private void registrarTemporal(String operando) {
        if (funcionActiva != null && temporales.estaEnUso(operando)
                && posiciones.stream().noneMatch(p -> p.operando().equals(operando)))
            posiciones.add(new RegistroActivacion.Posicion(operando, operando, "unknown",
                    RegistroActivacion.Clase.TEMPORAL, RegistroActivacion.CABECERA + posiciones.size()));
    }

    // ---- clases y objetos ----
    public void registrarClase(DescriptorClase clase) {
        if (clases.putIfAbsent(clase.nombre(), clase) != null)
            throw new IllegalArgumentException("Clase duplicada: " + clase.nombre());
    }
    public DescriptorClase clase(String nombre) {
        var clase = clases.get(nombre);
        if (clase == null) throw new IllegalArgumentException("Clase desconocida: " + nombre);
        return clase;
    }
    public String claseActual() { return claseActual; }
    public void claseActual(String clase) { claseActual = clase; }
    /** Etiqueta del constructor propio o heredado más cercano; null si no existe. */
    public String etiquetaConstructor(String clase) {
        for (String c = clase; c != null; c = clases.containsKey(c) ? clases.get(c).padre() : null)
            if (funciones.containsKey(c + ".constructor")) return c + ".constructor";
        return null;
    }
    public boolean existeClase(String nombre) { return clases.containsKey(nombre); }
    public java.util.Map<String, DescriptorClase> clases() { return java.util.Collections.unmodifiableMap(clases); }
    public boolean esSubclase(String hija, String ancestro) {
        for (String c = hija; c != null; c = clases.containsKey(c) ? clases.get(c).padre() : null)
            if (c.equals(ancestro)) return true;
        return false;
    }
    /** Hay despacho dinámico si alguna subclase de {@code claseEstatica} redefine el método. */
    public boolean requiereVirtual(String claseEstatica, DescriptorClase.Metodo metodo) {
        for (var otra : clases.values()) {
            if (otra.nombre().equals(claseEstatica) || !esSubclase(otra.nombre(), claseEstatica)) continue;
            var redefinido = otra.metodo(metodo.nombre());
            if (redefinido != null && !redefinido.declarante().equals(metodo.declarante())) return true;
        }
        return false;
    }
    public boolean requiereInicializacion(String clase) { return clases.get(clase).tieneInicializadores(); }
    /** El símbolo referenciado es un atributo de clase: se accede mediante {@code this}. */
    public boolean esAtributo(org.antlr.v4.runtime.ParserRuleContext ctx) {
        var referencia = informacion.referencias().get(ctx);
        return referencia != null && referencia.simbolo().categoria() == semantic.CategoriaSimbolo.ATRIBUTO;
    }
    public boolean esMetodo(org.antlr.v4.runtime.ParserRuleContext ctx) {
        var referencia = informacion.referencias().get(ctx);
        return referencia != null && referencia.simbolo().categoria() == semantic.CategoriaSimbolo.METODO;
    }
    public boolean esFuncionGlobal(org.antlr.v4.runtime.ParserRuleContext ctx) {
        var referencia = informacion.referencias().get(ctx);
        return referencia != null && referencia.simbolo().categoria() == semantic.CategoriaSimbolo.FUNCION;
    }

    public void registrarFuncion(DescriptorFuncion funcion) {
        if (funciones.putIfAbsent(funcion.nombre(), funcion) != null)
            throw new IllegalArgumentException("Función duplicada: " + funcion.nombre());
    }
    public DescriptorFuncion funcion(String nombre) {
        var funcion = funciones.get(nombre);
        if (funcion == null) throw new IllegalArgumentException("Función desconocida: " + nombre);
        return funcion;
    }
    public java.util.Map<String, DescriptorFuncion> funciones() {
        return java.util.Collections.unmodifiableMap(funciones);
    }
    public void emitir(InstruccionTAC instruccion) {
        registrarTemporal(instruccion.argumento1());
        registrarTemporal(instruccion.argumento2());
        registrarTemporal(instruccion.resultado());
        instrucciones.add(java.util.Objects.requireNonNull(instruccion));
    }

    public GeneradorTAC() {
        instrucciones = new ArrayList<>();
        temporales = new AdministradorTemporales();
    }

    public String generarOperacion(
            String operador,
            String argumento1,
            String argumento2) {

        java.util.Objects.requireNonNull(argumento1, "Operando sin valor TAC");
        java.util.Objects.requireNonNull(argumento2, "Operando sin valor TAC");

        String temporal = temporales.nuevoTemporal();

        emitir(
                new InstruccionTAC(
                        operador,
                        argumento1,
                        argumento2,
                        temporal
                )
        );

        return temporal;
    }

    public String generarOperacionUnaria(
            String operador,
            String argumento) {

        java.util.Objects.requireNonNull(argumento, "Operando sin valor TAC");

        String temporal = temporales.nuevoTemporal();

        emitir(
                new InstruccionTAC(
                        operador,
                        argumento,
                        null,
                        temporal
                )
        );

        return temporal;
    }

    public void generarAsignacion(
            String destino,
            String valor) {

        java.util.Objects.requireNonNull(valor, "Asignación sin valor TAC");

        emitir(
                new InstruccionTAC(
                        "",
                        valor,
                        null,
                        destino
                )
        );
    }

    public void liberarTemporal(String temporal) {
        temporales.liberar(temporal);
    }

    public String nuevaEtiqueta() { return etiquetas.nuevaEtiqueta(); }

    public void emitirEtiqueta(String etiqueta) {
        instrucciones.add(InstruccionTAC.etiqueta(etiqueta));
    }

    public void generarSalto(String destino) {
        instrucciones.add(InstruccionTAC.salto(destino));
    }

    public void generarSaltoCondicional(String condicion, String destino) {
        instrucciones.add(InstruccionTAC.saltoCondicional(condicion, destino));
    }

    public List<InstruccionTAC> instrucciones() {
        return Collections.unmodifiableList(instrucciones);
    }

    public AdministradorTemporales temporales() {
        return temporales;
    }

    public String codigo() {
        StringBuilder codigo = new StringBuilder();

        for (InstruccionTAC instruccion : instrucciones) {
            codigo.append(instruccion)
                    .append(System.lineSeparator());
        }

        return codigo.toString();
    }

    public void limpiar() {
        instrucciones.clear();
        temporales.reiniciar();
        etiquetas.reiniciar();
        funciones.clear();
        registros.clear(); posiciones.clear(); ambitos.clear(); funcionActiva = null; clases.clear();
        enlaces.clear(); siguienteGlobal = 0; informacion = semantic.InformacionSemantica.vacia();
    }
}
