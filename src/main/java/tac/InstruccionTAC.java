package tac;

import java.util.Objects;

public final class InstruccionTAC {
    public enum Tipo { OPERACION, ETIQUETA, SALTO, SALTO_CONDICIONAL,
        FUNCION, FIN_FUNCION, PARAMETRO, ARGUMENTO, LLAMADA, RETORNO,
        LONGITUD_ARREGLO, LECTURA_ARREGLO }

    private final Tipo tipo;

    private final String operador;
    private final String argumento1;
    private final String argumento2;
    private final String resultado;

    public InstruccionTAC(
            String operador,
            String argumento1,
            String argumento2,
            String resultado) {
        this(Tipo.OPERACION, operador, argumento1, argumento2, resultado);
    }

    private InstruccionTAC(Tipo tipo, String operador, String argumento1,
                           String argumento2, String resultado) {
        this.tipo = tipo;
        this.operador = Objects.requireNonNull(operador);
        this.argumento1 = argumento1;
        this.argumento2 = argumento2;
        this.resultado = tipo == Tipo.LLAMADA || tipo == Tipo.RETORNO
                || tipo == Tipo.ARGUMENTO ? resultado : Objects.requireNonNull(resultado);
    }

    public static InstruccionTAC etiqueta(String nombre) {
        return new InstruccionTAC(Tipo.ETIQUETA, "label", null, null, nombre);
    }

    public static InstruccionTAC salto(String destino) {
        return new InstruccionTAC(Tipo.SALTO, "goto", null, null, destino);
    }

    public static InstruccionTAC saltoCondicional(String condicion, String destino) {
        return new InstruccionTAC(Tipo.SALTO_CONDICIONAL, "if",
                Objects.requireNonNull(condicion), null, destino);
    }

    public Tipo tipo() { return tipo; }

    public static InstruccionTAC funcion(String nombre) {
        return new InstruccionTAC(Tipo.FUNCION, "function", null, null, nombre);
    }
    public static InstruccionTAC finFuncion(String nombre) {
        return new InstruccionTAC(Tipo.FIN_FUNCION, "end", null, null, nombre);
    }
    public static InstruccionTAC parametro(String nombre, int posicion) {
        return new InstruccionTAC(Tipo.PARAMETRO, "param", Integer.toString(posicion), null, nombre);
    }
    public static InstruccionTAC argumento(String valor) {
        return new InstruccionTAC(Tipo.ARGUMENTO, "arg", Objects.requireNonNull(valor), null, null);
    }
    public static InstruccionTAC llamada(String nombre, int cantidad, String resultado) {
        return new InstruccionTAC(Tipo.LLAMADA, "call", Objects.requireNonNull(nombre),
                Integer.toString(cantidad), resultado);
    }
    public static InstruccionTAC retorno(String valor) {
        return new InstruccionTAC(Tipo.RETORNO, "return", valor, null, null);
    }
    public static InstruccionTAC longitudArreglo(String arreglo, String resultado) {
        return new InstruccionTAC(Tipo.LONGITUD_ARREGLO, "length",
                Objects.requireNonNull(arreglo), null, resultado);
    }
    public static InstruccionTAC lecturaArreglo(String arreglo, String indice, String resultado) {
        return new InstruccionTAC(Tipo.LECTURA_ARREGLO, "load_index",
                Objects.requireNonNull(arreglo), Objects.requireNonNull(indice), resultado);
    }

    public String operador() {
        return operador;
    }

    public String argumento1() {
        return argumento1;
    }

    public String argumento2() {
        return argumento2;
    }

    public String resultado() {
        return resultado;
    }

    @Override
    public String toString() {
        switch (tipo) {
            case LONGITUD_ARREGLO: return resultado + " = length " + argumento1;
            case LECTURA_ARREGLO: return resultado + " = " + argumento1 + "[" + argumento2 + "]";
            case FUNCION: return "function " + resultado;
            case FIN_FUNCION: return "end function " + resultado;
            case PARAMETRO: return resultado + " = param " + argumento1;
            case ARGUMENTO: return "arg " + argumento1;
            case LLAMADA: return (resultado == null ? "" : resultado + " = ")
                    + "call " + argumento1 + ", " + argumento2;
            case RETORNO: return "return" + (argumento1 == null ? "" : " " + argumento1);
            case ETIQUETA: return resultado + ":";
            case SALTO: return "goto " + resultado;
            case SALTO_CONDICIONAL: return "if " + argumento1 + " goto " + resultado;
            default: break;
        }
        if (argumento2 == null && operador.isEmpty()) {
            return resultado + " = " + argumento1;
        }

        if (argumento2 == null) {
            return resultado + " = " + operador + argumento1;
        }

        return resultado
                + " = "
                + argumento1
                + " "
                + operador
                + " "
                + argumento2;
    }
}
