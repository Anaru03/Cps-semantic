package semantic;

import java.util.List;

public final class ValidadorArreglos {

    private ValidadorArreglos() {
    }

    public static boolean elementosCompatibles(
            TipoArreglo arreglo,
            List<Tipo> elementos
    ) {
        if (arreglo == null || elementos == null) {
            return false;
        }

        for (Tipo elemento : elementos) {
            if (!arreglo.acepta(elemento)) {
                return false;
            }
        }

        return true;
    }

    public static boolean indiceValido(Tipo tipoIndice) {
        return tipoIndice == Tipo.INTEGER;
    }
}