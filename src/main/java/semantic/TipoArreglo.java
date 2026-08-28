package semantic;

import java.util.Objects;

public final class TipoArreglo {

    private final Tipo tipoElemento;

    public TipoArreglo(Tipo tipoElemento) {
        this.tipoElemento = Objects.requireNonNull(tipoElemento);
    }

    public Tipo tipoElemento() {
        return tipoElemento;
    }

    public boolean acepta(Tipo tipo) {
        return tipoElemento.esCompatibleCon(tipo);
    }
}