package mips;

import compiler.ResultadoCompilacion;
import java.util.*;

/** API del backend para el IDE: ante errores no devuelve ensamblador parcial. */
public final class GeneradorMIPS {
    public record Resultado(String codigo, String sinOptimizar, List<String> errores,
                            Map<String, Integer> optimizaciones) {
        public Resultado {
            errores = List.copyOf(errores);
            optimizaciones = Collections.unmodifiableMap(new LinkedHashMap<>(optimizaciones));
        }
        public boolean esValido() { return errores.isEmpty(); }
    }
    public Resultado generar(ResultadoCompilacion entrada) { return generar(entrada, true); }

    public Resultado generar(ResultadoCompilacion entrada, boolean optimizar) {
        if (!entrada.esValido()) return new Resultado("", "",
                entrada.errores().stream().map(Object::toString).toList(), Map.of());
        try {
            String original = TraductorTACProvisional.traducir(entrada);
            if (!optimizar) return new Resultado(original, original, List.of(), Map.of());
            var salida = new OptimizadorMirilla().optimizar(original);
            return new Resultado(salida.codigo(), original, List.of(), salida.aplicaciones());
        } catch (GeneracionMIPSException | IllegalArgumentException e) {
            return new Resultado("", "", List.of(e.getMessage()), Map.of());
        }
    }
}
