package semantic;

import org.antlr.v4.runtime.ParserRuleContext;
import java.util.Map;

/** Identidad por ámbito y nombre, conservada aunque el símbolo cambie de tipo. */
public record InformacionSemantica(Map<ParserRuleContext, TipoDato> tipos,
                                  Map<ParserRuleContext, Ambito> ambitos,
                                  Map<ParserRuleContext, Referencia> referencias) {
    public record Referencia(Ambito ambito, String nombre) {
        public Simbolo simbolo() { return ambito.buscarLocal(nombre).orElseThrow(); }
    }
    public InformacionSemantica {
        tipos = Map.copyOf(tipos); ambitos = Map.copyOf(ambitos); referencias = Map.copyOf(referencias);
    }
    public static InformacionSemantica vacia() { return new InformacionSemantica(Map.of(), Map.of(), Map.of()); }
}
