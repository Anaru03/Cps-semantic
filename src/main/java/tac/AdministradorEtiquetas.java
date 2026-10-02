package tac;

/** Las etiquetas son únicas durante una compilación y no se reciclan. */
public final class AdministradorEtiquetas {
    private int siguiente;

    public String nuevaEtiqueta() { return "L" + siguiente++; }

    public void reiniciar() { siguiente = 0; }
}
