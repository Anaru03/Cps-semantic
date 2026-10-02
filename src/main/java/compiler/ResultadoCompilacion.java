package compiler;

import antlr.CompiscriptParser;
import semantic.AnalisisSemantico;
import tac.*;
import java.util.*;

/** Snapshot de la salida; el árbol y ámbitos se comparten para su visualización. */
public record ResultadoCompilacion(CompiscriptParser.ProgramContext arbol,
                                  AnalisisSemantico analisis, List<Diagnostico> errores,
                                  List<InstruccionTAC> instrucciones,
                                  Map<String, DescriptorFuncion> funciones,
                                  Map<String, RegistroActivacion> registros,
                                  List<EnlaceSimboloTAC> enlaces) {
    public ResultadoCompilacion {
        errores = List.copyOf(errores); instrucciones = List.copyOf(instrucciones);
        funciones = Collections.unmodifiableMap(new LinkedHashMap<>(funciones));
        registros = Collections.unmodifiableMap(new LinkedHashMap<>(registros)); enlaces = List.copyOf(enlaces);
    }
    public boolean esValido() { return errores.isEmpty(); }
    public String codigoTAC() {
        var texto = new StringBuilder();
        for (var instruccion : instrucciones) texto.append(instruccion).append(System.lineSeparator());
        return texto.toString();
    }
}
