package compiler;

public record Diagnostico(Etapa etapa, int linea, int columna, String descripcion) {
    public enum Etapa { LEXICO, SINTAXIS, SEMANTICA, TAC }
    @Override public String toString() { return etapa + " " + linea + ":" + columna + " - " + descripcion; }
}
