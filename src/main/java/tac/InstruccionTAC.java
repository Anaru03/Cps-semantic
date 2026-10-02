package tac;

import java.util.Objects;

public final class InstruccionTAC {
    public enum Tipo { OPERACION, ETIQUETA, SALTO, SALTO_CONDICIONAL }

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
        this.resultado = Objects.requireNonNull(resultado);
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
