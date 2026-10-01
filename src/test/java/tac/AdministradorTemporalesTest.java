package tac;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdministradorTemporalesTest {

    @Test
    void generaTemporalesConsecutivos() {
        AdministradorTemporales temporales =
                new AdministradorTemporales();

        assertEquals("t0", temporales.nuevoTemporal());
        assertEquals("t1", temporales.nuevoTemporal());
        assertEquals("t2", temporales.nuevoTemporal());
    }

    @Test
    void reutilizaTemporalLiberado() {
        AdministradorTemporales temporales =
                new AdministradorTemporales();

        String primero =
                temporales.nuevoTemporal();

        temporales.nuevoTemporal();

        temporales.liberar(primero);

        assertEquals(
                "t0",
                temporales.nuevoTemporal()
        );
    }

    @Test
    void controlaTemporalesEnUso() {
        AdministradorTemporales temporales =
                new AdministradorTemporales();

        String temporal =
                temporales.nuevoTemporal();

        assertTrue(
                temporales.estaEnUso(temporal)
        );

        assertEquals(
                1,
                temporales.cantidadEnUso()
        );

        temporales.liberar(temporal);

        assertFalse(
                temporales.estaEnUso(temporal)
        );

        assertEquals(
                0,
                temporales.cantidadEnUso()
        );
    }

    @Test
    void noLiberaDosVecesElMismoTemporal() {
        AdministradorTemporales temporales =
                new AdministradorTemporales();

        String temporal =
                temporales.nuevoTemporal();

        temporales.liberar(temporal);
        temporales.liberar(temporal);

        assertEquals(
                "t0",
                temporales.nuevoTemporal()
        );

        assertEquals(
                "t1",
                temporales.nuevoTemporal()
        );
    }

    @Test
    void reiniciaAdministrador() {
        AdministradorTemporales temporales =
                new AdministradorTemporales();

        temporales.nuevoTemporal();
        temporales.nuevoTemporal();

        temporales.reiniciar();

        assertEquals(
                0,
                temporales.cantidadEnUso()
        );

        assertEquals(
                "t0",
                temporales.nuevoTemporal()
        );
    }
}