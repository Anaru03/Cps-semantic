package tac;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeneradorTACTest {

    @Test
    void generaOperacionBinaria() {
        GeneradorTAC generador = new GeneradorTAC();

        String temporal =
                generador.generarOperacion("+", "a", "b");

        assertEquals("t0", temporal);
        assertEquals(
                "t0 = a + b" + System.lineSeparator(),
                generador.codigo()
        );
    }

    @Test
    void generaExpresionCompuesta() {
        GeneradorTAC generador = new GeneradorTAC();

        String t0 =
                generador.generarOperacion("*", "b", "c");

        String t1 =
                generador.generarOperacion("+", "a", t0);

        generador.generarAsignacion("x", t1);

        String esperado =
                "t0 = b * c" + System.lineSeparator()
                + "t1 = a + t0" + System.lineSeparator()
                + "x = t1" + System.lineSeparator();

        assertEquals(esperado, generador.codigo());
    }

    @Test
    void generaAsignacionSimple() {
        GeneradorTAC generador = new GeneradorTAC();

        generador.generarAsignacion("x", "10");

        assertEquals(
                "x = 10" + System.lineSeparator(),
                generador.codigo()
        );
    }

    @Test
    void generaOperacionUnaria() {
        GeneradorTAC generador = new GeneradorTAC();

        generador.generarOperacionUnaria("-", "x");

        assertEquals(
                "t0 = -x" + System.lineSeparator(),
                generador.codigo()
        );
    }
}