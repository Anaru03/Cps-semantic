package tac;

import org.antlr.v4.runtime.ParserRuleContext;

/** Fallo de traducción con posición del nodo que lo produjo. */
public final class ErrorGeneracionTAC extends RuntimeException {
    private final int linea, columna;
    public ErrorGeneracionTAC(ParserRuleContext nodo, RuntimeException causa) {
        super(causa.getMessage() == null ? "Construcción sin representación TAC" : causa.getMessage(), causa);
        linea = nodo.getStart().getLine(); columna = nodo.getStart().getCharPositionInLine();
    }
    public int linea() { return linea; }
    public int columna() { return columna; }
}
