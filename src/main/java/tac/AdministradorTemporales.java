package tac;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

public final class AdministradorTemporales {

    private int siguiente;
    private int solicitudes, reutilizados, maximoSimultaneos;

    private final Deque<String> disponibles;
    private final Set<String> enUso;
    private final Set<String> reservados = new HashSet<>();

    public void reservarNombre(String nombre) { reservados.add(nombre); }

    public AdministradorTemporales() {
        siguiente = 0;
        disponibles = new ArrayDeque<>();
        enUso = new HashSet<>();
    }

    public String nuevoTemporal() {
        String temporal;

        solicitudes++;
        if (!disponibles.isEmpty()) {
            temporal = disponibles.removeFirst();
            reutilizados++;
        } else {
            do { temporal = "t" + siguiente++; } while (reservados.contains(temporal));
        }

        enUso.add(temporal);
        maximoSimultaneos = Math.max(maximoSimultaneos, enUso.size());

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

    /** Métricas del reciclaje: solicitudes totales, temporales distintos creados y reutilizaciones. */
    public record Estadisticas(int solicitudes, int distintos, int reutilizados, int maximoSimultaneos) { }
    public Estadisticas estadisticas() {
        return new Estadisticas(solicitudes, solicitudes - reutilizados, reutilizados, maximoSimultaneos);
    }

    public void reiniciar() {
        solicitudes = reutilizados = maximoSimultaneos = 0;
        siguiente = 0;
        disponibles.clear();
        enUso.clear();
        reservados.clear();
    }
}
