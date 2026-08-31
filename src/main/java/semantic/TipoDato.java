package semantic;

import java.util.Objects;

/** Tipo semantico que conserva el nombre de clases y el elemento de arreglos. */
public record TipoDato(Tipo base, String nombreClase, TipoDato elemento) {
    public static final TipoDato INTEGER = simple(Tipo.INTEGER);
    public static final TipoDato STRING = simple(Tipo.STRING);
    public static final TipoDato BOOLEAN = simple(Tipo.BOOLEAN);
    public static final TipoDato NULL = simple(Tipo.NULL);
    public static final TipoDato VOID = simple(Tipo.VOID);
    public static final TipoDato UNKNOWN = simple(Tipo.UNKNOWN);
    public static final TipoDato ERROR = simple(Tipo.ERROR);

    public TipoDato {
        Objects.requireNonNull(base);
        if (base != Tipo.CLASS) nombreClase = null;
        if (base != Tipo.ARRAY) elemento = null;
    }

    public static TipoDato simple(Tipo tipo) { return new TipoDato(tipo, null, null); }
    public static TipoDato clase(String nombre) { return new TipoDato(Tipo.CLASS, nombre, null); }
    public static TipoDato arreglo(TipoDato elemento) { return new TipoDato(Tipo.ARRAY, null, elemento); }

    public boolean compatibleCon(TipoDato otro) {
        if (otro == null || base == Tipo.ERROR || otro.base == Tipo.ERROR ||
                base == Tipo.UNKNOWN || otro.base == Tipo.UNKNOWN) return false;
        if (otro.base == Tipo.NULL) return base == Tipo.CLASS || base == Tipo.ARRAY || base == Tipo.NULL;
        if (base != otro.base) return false;
        if (base == Tipo.CLASS) return Objects.equals(nombreClase, otro.nombreClase);
        if (base == Tipo.ARRAY) return elemento.compatibleCon(otro.elemento);
        return true;
    }

    @Override public String toString() {
        if (base == Tipo.CLASS) return nombreClase;
        if (base == Tipo.ARRAY) return elemento + "[]";
        return base.name().toLowerCase();
    }
}
