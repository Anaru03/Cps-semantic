package semantic;

public final class ValidadorAsignaciones {

    private ValidadorAsignaciones() {
    }

    public static boolean esAsignacionValida(
            Tipo tipoDeclarado,
            Tipo tipoValor
    ) {
        if (tipoDeclarado == null || tipoValor == null) {
            return false;
        }

        return tipoDeclarado.esCompatibleCon(tipoValor);
    }
}