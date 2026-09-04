package semantic;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class Ambito {
    private final String nombre;
    private final Ambito padre;
    private final Map<String, Simbolo> simbolos = new LinkedHashMap<>();

    public Ambito(String nombre, Ambito padre) { this.nombre = nombre; this.padre = padre; }
    public String nombre() { return nombre; }
    public Ambito padre() { return padre; }
    public boolean declarar(Simbolo simbolo) {
        if (simbolos.containsKey(simbolo.nombre())) return false;
        simbolos.put(simbolo.nombre(), simbolo);
        return true;
    }
    public Optional<Simbolo> buscarLocal(String nombre) { return Optional.ofNullable(simbolos.get(nombre)); }
    public Optional<Simbolo> buscar(String nombre) {
        Simbolo local = simbolos.get(nombre);
        return local != null ? Optional.of(local) : padre == null ? Optional.empty() : padre.buscar(nombre);
    }
    /**
     * Actualiza el tipo de un simbolo ya declarado. Busca primero en este ambito y,
     * si no lo encuentra, en los ambitos padres (igual que {@link #buscar(String)}).
     * Devuelve false si el identificador no esta declarado en ningun ambito visible.
     */
    public boolean actualizar(String nombre, TipoDato nuevoTipo) {
        Simbolo existente = simbolos.get(nombre);
        if (existente != null) { simbolos.put(nombre, existente.conTipo(nuevoTipo)); return true; }
        return padre != null && padre.actualizar(nombre, nuevoTipo);
    }
    public Collection<Simbolo> simbolos() { return java.util.List.copyOf(simbolos.values()); }
}
