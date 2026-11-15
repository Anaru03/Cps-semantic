package mips;

import compiler.ResultadoCompilacion;
import tac.InstruccionTAC;
import tac.RegistroActivacion;

import java.util.*;

/**
 * Puente de integración TAC -> MIPS para probar los módulos de Alejandro y Jorge.
 * GeneradorMIPS ofrece la API pública con diagnósticos y mirilla. Ruth puede sustituir
 * este bucle por TraductorTAC conservando la delegación a GeneradorFuncionesMIPS y
 * GeneradorEstructurasMIPS; ver docs/MIPS_INTEGRACION_RUTH.md.
 *
 * Particularidades del TAC del Proyecto 2 que se resuelven aquí:
 *  - Las funciones van incrustadas en el flujo, saltadas por "goto Lk ... Lk:" -> se extraen
 *    como bloques aparte y se elimina ese goto.
 *  - El código fuera de funciones pasa a ser "main".
 *  - "x = param k" no genera código: el prólogo copia $a0-$a3 / pila a los slots.
 */
public final class TraductorTACProvisional {
    private TraductorTACProvisional() { }

    public static String traducir(ResultadoCompilacion resultado) {
        if (!resultado.esValido()) {
            throw new IllegalArgumentException("Hay errores: no se genera código MIPS");
        }
        List<InstruccionTAC> ins = resultado.instrucciones();
        Map<String, List<InstruccionTAC>> funciones = new LinkedHashMap<>();
        List<InstruccionTAC> main = new ArrayList<>();
        String actual = null;
        for (int i = 0; i < ins.size(); i++) {
            InstruccionTAC x = ins.get(i);
            switch (x.tipo()) {
                case FUNCION:
                    if (actual != null) {
                        throw new GeneracionMIPSException("Funciones anidadas no soportadas: '"
                                + x.resultado() + "' dentro de '" + actual + "'");
                    }
                    actual = x.resultado();
                    funciones.put(actual, new ArrayList<>());
                    if (!main.isEmpty() && saltaSobreFuncion(ins, i, main.get(main.size() - 1))) {
                        main.remove(main.size() - 1);
                    }
                    break;
                case FIN_FUNCION:
                    actual = null;
                    break;
                case CLASE:
                case FIN_CLASE:
                    break; // layouts consumidos por GeneradorEstructurasMIPS
                default:
                    (actual == null ? main : funciones.get(actual)).add(x);
            }
        }

        GeneradorFuncionesMIPS g = new GeneradorFuncionesMIPS();
        GeneradorEstructurasMIPS estructuras = new GeneradorEstructurasMIPS(g, resultado);
        for (Map.Entry<String, List<InstruccionTAC>> e : funciones.entrySet()) {
            RegistroActivacion layout = resultado.registros().get(e.getKey());
            if (layout == null) throw new GeneracionMIPSException("Función sin registro de activación: " + e.getKey());
            g.iniciarFuncion(layout, parametros(e.getValue()));
            estructuras.iniciarAmbito(e.getKey());
            for (InstruccionTAC x : e.getValue()) traducir(g, estructuras, x);
            g.finFuncion();
        }
        g.iniciarMain(List.of());
        estructuras.iniciarAmbito(null);
        for (InstruccionTAC x : main) traducir(g, estructuras, x);
        g.finFuncion();
        return g.ensamblar();
    }

    /** ¿El 'goto L' anterior solo salta el cuerpo de la función que empieza en i (L: justo tras su fin)? */
    private static boolean saltaSobreFuncion(List<InstruccionTAC> ins, int i, InstruccionTAC previa) {
        if (previa.tipo() != InstruccionTAC.Tipo.SALTO) return false;
        for (int j = i + 1; j < ins.size(); j++) {
            if (ins.get(j).tipo() == InstruccionTAC.Tipo.FIN_FUNCION) {
                return j + 1 < ins.size() && ins.get(j + 1).tipo() == InstruccionTAC.Tipo.ETIQUETA
                        && ins.get(j + 1).resultado().equals(previa.resultado());
            }
        }
        return false;
    }

    private static List<String> parametros(List<InstruccionTAC> cuerpo) {
        TreeMap<Integer, String> porIndice = new TreeMap<>();
        for (InstruccionTAC x : cuerpo) {
            if (x.tipo() == InstruccionTAC.Tipo.PARAMETRO) {
                porIndice.put(Integer.parseInt(x.argumento1()), x.resultado());
            }
        }
        return new ArrayList<>(porIndice.values());
    }

    private static void traducir(GeneradorFuncionesMIPS g, GeneradorEstructurasMIPS estructuras, InstruccionTAC x) {
        if (estructuras.traducir(x)) return;
        estructuras.validarBasica(x);
        switch (x.tipo()) {
            case PARAMETRO:         break;
            case ETIQUETA:          g.etiqueta(x.resultado()); break;
            case SALTO:             g.saltoIncondicional(x.resultado()); break;
            case SALTO_CONDICIONAL: g.saltoSiVerdadero(x.argumento1(), x.resultado()); break;
            case ARGUMENTO:         g.argumento(x.argumento1()); break;
            case RETORNO:           g.retorno(x.argumento1()); break;
            case IMPRIMIR:
                if (estructuras.tipo(x.argumento1()).equals("string")) g.imprimirCadena(x.argumento1());
                else g.imprimir(x.argumento1());
                break;
            case LLAMADA:
                g.llamada(x.resultado(), GeneradorFuncionesMIPS.etiquetaDe(x.argumento1()),
                        Integer.parseInt(x.argumento2()));
                break;
            case OPERACION:
                if (x.operador().isEmpty())         g.copiar(x.resultado(), x.argumento1());
                else if (x.argumento2() == null)    g.unaria(x.operador(), x.resultado(), x.argumento1());
                else                                g.binaria(x.operador(), x.resultado(), x.argumento1(), x.argumento2());
                break;
            default:
                throw new GeneracionMIPSException("Instrucción TAC sin traducción MIPS todavía: " + x);
        }
        estructuras.observar(x);
    }
}
