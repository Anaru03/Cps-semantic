package tac;

import java.util.Objects;

public final class InstruccionTAC {

    private final String operador;
    private final String argumento1;
    private final String argumento2;
    private final String resultado;

    public InstruccionTAC(
            String operador,
            String argumento1,
            String argumento2,
            String resultado) {

        this.operador = Objects.requireNonNull(operador);
        this.argumento1 = argumento1;
        this.argumento2 = argumento2;
        this.resultado = Objects.requireNonNull(resultado);
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