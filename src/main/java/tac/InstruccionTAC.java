package tac;

import java.util.Objects;

public final class InstruccionTAC {
    public enum Tipo { OPERACION, ETIQUETA, SALTO, SALTO_CONDICIONAL,
        FUNCION, FIN_FUNCION, PARAMETRO, ARGUMENTO, LLAMADA, RETORNO,
        LONGITUD_ARREGLO, LECTURA_ARREGLO, ESCRITURA_ARREGLO, NUEVO_ARREGLO,
        NUEVO_OBJETO, LECTURA_CAMPO, ESCRITURA_CAMPO, IMPRIMIR,
        INICIO_TRY, FIN_TRY, CAPTURA, CLASE, FIN_CLASE }

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
        this.resultado = tipo == Tipo.LLAMADA || tipo == Tipo.RETORNO || tipo == Tipo.FIN_TRY
                || tipo == Tipo.IMPRIMIR || tipo == Tipo.ARGUMENTO ? resultado : Objects.requireNonNull(resultado);
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
    /** Llamada con despacho dinámico: el receptor es el primer argumento (índice 0). */
    public static InstruccionTAC llamadaVirtual(String nombre, int cantidad, String resultado) {
        return new InstruccionTAC(Tipo.LLAMADA, "vcall", Objects.requireNonNull(nombre),
                Integer.toString(cantidad), resultado);
    }
    public static InstruccionTAC escrituraArreglo(String arreglo, String indice, String valor) {
        return new InstruccionTAC(Tipo.ESCRITURA_ARREGLO, "store_index", Objects.requireNonNull(indice),
                Objects.requireNonNull(valor), Objects.requireNonNull(arreglo));
    }
    public static InstruccionTAC nuevoArreglo(int cantidad, String resultado) {
        return new InstruccionTAC(Tipo.NUEVO_ARREGLO, "newarray", Integer.toString(cantidad), null, resultado);
    }
    public static InstruccionTAC nuevoObjeto(String clase, String resultado) {
        return new InstruccionTAC(Tipo.NUEVO_OBJETO, "new", Objects.requireNonNull(clase), null, resultado);
    }
    public static InstruccionTAC lecturaCampo(String objeto, String campo, String resultado) {
        return new InstruccionTAC(Tipo.LECTURA_CAMPO, "load_field", Objects.requireNonNull(objeto),
                Objects.requireNonNull(campo), resultado);
    }
    public static InstruccionTAC escrituraCampo(String objeto, String campo, String valor) {
        return new InstruccionTAC(Tipo.ESCRITURA_CAMPO, "store_field", Objects.requireNonNull(campo),
                Objects.requireNonNull(valor), Objects.requireNonNull(objeto));
    }
    public static InstruccionTAC imprimir(String valor) {
        return new InstruccionTAC(Tipo.IMPRIMIR, "print", Objects.requireNonNull(valor), null, null);
    }
    /** Instala un manejador: ante una excepción de runtime el control pasa a {@code destino}. */
    public static InstruccionTAC inicioTry(String destino) {
        return new InstruccionTAC(Tipo.INICIO_TRY, "try", null, null, destino);
    }
    public static InstruccionTAC finTry() { return new InstruccionTAC(Tipo.FIN_TRY, "endtry", null, null, null); }
    public static InstruccionTAC captura(String variable) {
        return new InstruccionTAC(Tipo.CAPTURA, "catch", null, null, variable);
    }
    public static InstruccionTAC clase(String nombre, String padre) {
        return new InstruccionTAC(Tipo.CLASE, "class", padre, null, nombre);
    }
    public static InstruccionTAC finClase(String nombre) {
        return new InstruccionTAC(Tipo.FIN_CLASE, "endclass", null, null, nombre);
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
                    + operador + " " + argumento1 + ", " + argumento2;
            case ESCRITURA_ARREGLO: return resultado + "[" + argumento1 + "] = " + argumento2;
            case NUEVO_ARREGLO: return resultado + " = newarray " + argumento1;
            case NUEVO_OBJETO: return resultado + " = new " + argumento1;
            case LECTURA_CAMPO: return resultado + " = " + argumento1 + "." + argumento2;
            case ESCRITURA_CAMPO: return resultado + "." + argumento1 + " = " + argumento2;
            case IMPRIMIR: return "print " + argumento1;
            case INICIO_TRY: return "try " + resultado;
            case FIN_TRY: return "endtry";
            case CAPTURA: return resultado + " = catch";
            case CLASE: return "class " + resultado + (argumento1 == null ? "" : " : " + argumento1);
            case FIN_CLASE: return "end class " + resultado;
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
