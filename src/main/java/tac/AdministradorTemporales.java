package tac;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * Administra la creacion y reutilizacion de variables temporales
 * utilizadas durante la generacion de codigo TAC.
 */
public final class AdministradorTemporales {

    private int siguiente;
    private final Deque<String> disponibles;
    private final Set<String> enUso;

    public AdministradorTemporales() {
        this.siguiente = 0;
        this.disponibles = new ArrayDeque<>();
        this.enUso = new HashSet<>();
    }

    /**
     * Obtiene un temporal disponible.
     * Si existe uno previamente liberado, lo reutiliza.
     */
    public String nuevoTemporal() {
        String temporal;

        if (!disponibles.isEmpty()) {
            temporal = disponibles.removeFirst();
        } else {
            temporal = "t" + siguiente++;
        }

        enUso.add(temporal);
        return temporal;
    }

    /**
     * Libera un temporal para que pueda reutilizarse.
     */
    public void liberar(String temporal) {
        if (temporal == null) {
            return;
        }

        if (enUso.remove(temporal)) {
            disponibles.addLast(temporal);
        }
    }

    /**
     * Indica si un temporal se encuentra actualmente en uso.
     */
    public boolean estaEnUso(String temporal) {
        return enUso.contains(temporal);
    }

    /**
     * Cantidad de temporales actualmente utilizados.
     */
    public int cantidadEnUso() {
        return enUso.size();
    }

    /**
     * Reinicia completamente el administrador.
     */
    public void reiniciar() {
        siguiente = 0;
        disponibles.clear();
        enUso.clear();
    }
}