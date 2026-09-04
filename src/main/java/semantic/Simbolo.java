package semantic;

import java.util.List;
import java.util.Objects;

public final class Simbolo {
    private final String nombre;
    private final TipoDato tipo;
    private final CategoriaSimbolo categoria;
    private final Ambito ambito;
    private final List<TipoDato> parametros;
    private final Ambito miembros;

    public Simbolo(String nombre, TipoDato tipo, CategoriaSimbolo categoria, Ambito ambito) {
        this(nombre, tipo, categoria, ambito, List.of(), null);
    }

    public Simbolo(String nombre, TipoDato tipo, CategoriaSimbolo categoria, Ambito ambito,
                   List<TipoDato> parametros, Ambito miembros) {
        this.nombre = Objects.requireNonNull(nombre);
        this.tipo = Objects.requireNonNull(tipo);
        this.categoria = Objects.requireNonNull(categoria);
        this.ambito = Objects.requireNonNull(ambito);
        this.parametros = List.copyOf(parametros);
        this.miembros = miembros;
    }
    public String nombre() { return nombre; }
    public TipoDato tipo() { return tipo; }
    public CategoriaSimbolo categoria() { return categoria; }
    public Ambito ambito() { return ambito; }
    public List<TipoDato> parametros() { return parametros; }
    public Ambito miembros() { return miembros; }

    /** Crea una copia de este simbolo con un tipo distinto, preservando el resto de la informacion. */
    public Simbolo conTipo(TipoDato nuevoTipo) {
        return new Simbolo(nombre, nuevoTipo, categoria, ambito, parametros, miembros);
    }
}
