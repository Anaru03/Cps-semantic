package tac;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InstruccionTACTest {

    @Test
    void representaOperacionBinaria() {
        InstruccionTAC instruccion =
                new InstruccionTAC(
                        "+",
                        "a",
                        "b",
                        "t0"
                );

        assertEquals("+", instruccion.operador());
        assertEquals("a", instruccion.argumento1());
        assertEquals("b", instruccion.argumento2());
        assertEquals("t0", instruccion.resultado());
        assertEquals("t0 = a + b", instruccion.toString());
    }

    @Test
    void representaOperacionUnaria() {
        InstruccionTAC instruccion =
                new InstruccionTAC(
                        "-",
                        "x",
                        null,
                        "t0"
                );

        assertEquals(
                "t0 = -x",
                instruccion.toString()
        );
    }

    @Test
    void representaAsignacion() {
        InstruccionTAC instruccion =
                new InstruccionTAC(
                        "",
                        "t0",
                        null,
                        "x"
                );

        assertEquals(
                "x = t0",
                instruccion.toString()
        );
    }
}