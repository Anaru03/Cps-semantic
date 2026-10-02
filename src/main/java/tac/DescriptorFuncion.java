package tac;

import java.util.List;

/** Firma pública; el layout asociado se consulta en GeneradorTAC.registrosActivacion(). */
public record DescriptorFuncion(String nombre, List<Parametro> parametros, String tipoRetorno) {
    public record Parametro(String nombre, String tipo) { }
    public DescriptorFuncion { parametros = List.copyOf(parametros); }
    public boolean devuelveValor() { return !tipoRetorno.equals("void"); }
}
