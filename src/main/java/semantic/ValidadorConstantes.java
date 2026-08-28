package semantic;

public final class ValidadorConstantes {

    private ValidadorConstantes() {
    }

    public static boolean esInicializacionValida(
            Tipo tipoDeclarado,
            Tipo tipoValor
    ) {
        if (tipoDeclarado == null || tipoValor == null) {
            return false;
        }

        return tipoDeclarado.esCompatibleCon(tipoValor);
    }
}