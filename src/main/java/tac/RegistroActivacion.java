package tac;

import java.util.*;

/** Layout inmutable. Los offsets son slots lógicos, no bytes de una arquitectura. */
public record RegistroActivacion(String funcion, List<Posicion> posiciones) {
    public enum Clase { PARAMETRO, LOCAL, TEMPORAL }
    public record Posicion(String operando, String nombre, String tipo, Clase clase, int offset) { }
    public static final int ENLACE_DINAMICO = 0, DIRECCION_RETORNO = 1, VALOR_RETORNO = 2;
    public static final int CABECERA = 3;

    public RegistroActivacion {
        Objects.requireNonNull(funcion);
        posiciones = List.copyOf(posiciones);
        var nombres = new HashSet<String>();
        for (int i = 0; i < posiciones.size(); i++) {
            var posicion = posiciones.get(i);
            if (posicion.offset() != CABECERA + i || !nombres.add(posicion.operando()))
                throw new IllegalArgumentException("Layout inválido: " + funcion);
        }
    }
    public int cantidadSlots() { return CABECERA + posiciones.size(); }
    public Posicion posicion(String operando) {
        return posiciones.stream().filter(p -> p.operando().equals(operando)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Operando sin posición: " + operando));
    }

    /** Cada invocación crea almacenamiento propio, incluso para el mismo layout. */
    public static final class Instancia {
        private final RegistroActivacion layout;
        private final Object[] slots;
        private final String destinoRetorno;
        public Instancia(RegistroActivacion layout, Instancia llamador, int regreso,
                         String destinoRetorno, List<?> argumentos) {
            this.layout = Objects.requireNonNull(layout);
            this.destinoRetorno = destinoRetorno;
            slots = new Object[layout.cantidadSlots()];
            slots[ENLACE_DINAMICO] = llamador;
            slots[DIRECCION_RETORNO] = regreso;
            var parametros = layout.posiciones().stream().filter(p -> p.clase() == Clase.PARAMETRO).toList();
            if (argumentos.size() != parametros.size()) throw new IllegalArgumentException("Argumentos incompatibles");
            for (int i = 0; i < parametros.size(); i++) slots[parametros.get(i).offset()] = argumentos.get(i);
        }
        public RegistroActivacion layout() { return layout; }
        public Instancia llamador() { return (Instancia) slots[ENLACE_DINAMICO]; }
        public int direccionRetorno() { return (Integer) slots[DIRECCION_RETORNO]; }
        public String destinoRetorno() { return destinoRetorno; }
        public Object leer(String operando) { return slots[layout.posicion(operando).offset()]; }
        public void escribir(String operando, Object valor) { slots[layout.posicion(operando).offset()] = valor; }
        public Object valorRetorno() { return slots[VALOR_RETORNO]; }
        public void retornar(Object valor) { slots[VALOR_RETORNO] = valor; }
    }
}
