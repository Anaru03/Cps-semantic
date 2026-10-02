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
    private String funcionActiva;
    private int siguienteLocal;
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
        registros.clear(); posiciones.clear(); ambitos.clear(); funcionActiva = null;
    }
}
