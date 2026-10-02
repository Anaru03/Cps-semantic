package semantic;

public record AnalisisSemantico(ResultadoSemantico resultado, Ambito ambitoGlobal,
                               InformacionSemantica informacion) {
    public AnalisisSemantico(ResultadoSemantico resultado, Ambito ambitoGlobal) {
        this(resultado, ambitoGlobal, InformacionSemantica.vacia());
    }
}
