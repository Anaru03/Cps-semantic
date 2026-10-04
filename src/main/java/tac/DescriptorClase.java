package tac;

import java.util.List;

/**
 * Layout de objetos y tabla de métodos de una clase.
 * Slot 0 del objeto: identificador de clase / puntero a la tabla virtual.
 * Los campos heredados conservan el offset que tienen en la superclase, por lo que
 * un objeto de la subclase puede tratarse como uno de la superclase.
 */
public record DescriptorClase(String nombre, String padre, List<Campo> campos, List<Metodo> tabla,
                              boolean tieneInicializadores) {
    public static final int CABECERA_OBJETO = 1;

    /** @param offset slot lógico dentro del objeto (la cabecera ocupa el slot 0) */
    public record Campo(String nombre, String tipo, int offset, String declarante, boolean constante) { }

    /** @param ranura índice en la tabla virtual; una sobrescritura conserva la ranura del padre */
    public record Metodo(String nombre, String etiqueta, String declarante, int ranura, boolean sobrescribe) { }

    public DescriptorClase {
        campos = List.copyOf(campos);
        tabla = List.copyOf(tabla);
    }
    public int tamano() { return CABECERA_OBJETO + campos.size(); }
    public Campo campo(String nombre) {
        return campos.stream().filter(c -> c.nombre().equals(nombre)).findFirst().orElse(null);
    }
    public Metodo metodo(String nombre) {
        return tabla.stream().filter(m -> m.nombre().equals(nombre)).findFirst().orElse(null);
    }
}
