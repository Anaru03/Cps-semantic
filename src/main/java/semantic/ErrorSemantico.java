package semantic;

public record ErrorSemantico(
        int linea,
        int columna,
        String descripcion
) {
}