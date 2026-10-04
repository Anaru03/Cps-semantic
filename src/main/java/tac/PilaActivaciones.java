package tac;

import java.util.*;

/** Modelo runtime de llamadas. El intérprete/backend controla el contador de instrucciones. */
public final class PilaActivaciones {
    public record Regreso(int direccion, String destino, Object valor) { }
    private final Deque<RegistroActivacion.Instancia> pila = new ArrayDeque<>();
    public RegistroActivacion.Instancia actual() { return pila.peek(); }
    public int profundidad() { return pila.size(); }
    public RegistroActivacion.Instancia entrar(RegistroActivacion layout, List<?> argumentos,
                                               int regreso, String destino) {
        var marco = new RegistroActivacion.Instancia(layout, actual(), regreso, destino, argumentos);
        pila.push(marco);
        return marco;
    }
    public Regreso salir(Object valor) {
        if (pila.isEmpty()) throw new IllegalStateException("Retorno sin activación");
        var marco = pila.peek();
        if (marco.llamador() != null && marco.destinoRetorno() != null)
            marco.llamador().layout().posicion(marco.destinoRetorno());
        pila.pop();
        marco.retornar(valor);
        if (actual() != null && marco.destinoRetorno() != null)
            actual().escribir(marco.destinoRetorno(), valor);
        return new Regreso(marco.direccionRetorno(), marco.destinoRetorno(), valor);
    }
}
