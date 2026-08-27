package semantic;

import java.util.List;

public record ResultadoSemantico(
        List<ErrorSemantico> errores
) {

    public ResultadoSemantico {
        errores = List.copyOf(errores);
    }

    public boolean esValido() {
        return errores.isEmpty();
    }

    public int cantidadErrores() {
        return errores.size();
    }
}