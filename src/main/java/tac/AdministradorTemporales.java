package tac;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

public final class AdministradorTemporales {

    private int siguiente;

    private final Deque<String> disponibles;
    private final Set<String> enUso;

    public AdministradorTemporales() {
        siguiente = 0;
        disponibles = new ArrayDeque<>();
        enUso = new HashSet<>();
    }

    public String nuevoTemporal() {
        String temporal;

        if (!disponibles.isEmpty()) {
            temporal = disponibles.removeFirst();
        } else {
            temporal = "t" + siguiente;
            siguiente++;
        }

        enUso.add(temporal);

        return temporal;
    }

    public void liberar(String temporal) {
        if (temporal == null) {
            return;
        }

        if (enUso.remove(temporal)) {
            disponibles.addLast(temporal);
        }
    }

    public boolean estaEnUso(String temporal) {
        return enUso.contains(temporal);
    }

    public int cantidadEnUso() {
        return enUso.size();
    }

    public void reiniciar() {
        siguiente = 0;
        disponibles.clear();
        enUso.clear();
    }
}