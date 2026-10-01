package tac;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Construye y almacena instrucciones de codigo de tres direcciones.
 */
public final class GeneradorTAC {

    private final List<InstruccionTAC> instrucciones;
    private final AdministradorTemporales temporales;

    public GeneradorTAC() {
        instrucciones = new ArrayList<>();
        temporales = new AdministradorTemporales();
    }

    public String generarOperacion(
            String operador,
            String argumento1,
            String argumento2) {

        String temporal = temporales.nuevoTemporal();

        instrucciones.add(
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

        String temporal = temporales.nuevoTemporal();

        instrucciones.add(
                new InstruccionTAC(
                        operador,
                        argumento,
                        null,
                        temporal
                )
        );

        return temporal;
    }

    public void generarAsignacion(String destino, String valor) {
        instrucciones.add(
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
    }
}