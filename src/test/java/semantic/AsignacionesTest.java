package semantic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AsignacionesTest {

    @Test
    void integerPuedeRecibirInteger() {
        assertTrue(
                ValidadorAsignaciones.esAsignacionValida(
                        Tipo.INTEGER,
                        Tipo.INTEGER
                )
        );
    }

    @Test
    void stringPuedeRecibirString() {
        assertTrue(
                ValidadorAsignaciones.esAsignacionValida(
                        Tipo.STRING,
                        Tipo.STRING
                )
        );
    }

    @Test
    void booleanPuedeRecibirBoolean() {
        assertTrue(
                ValidadorAsignaciones.esAsignacionValida(
                        Tipo.BOOLEAN,
                        Tipo.BOOLEAN
                )
        );
    }

    @Test
    void integerNoPuedeRecibirString() {
        assertFalse(
                ValidadorAsignaciones.esAsignacionValida(
                        Tipo.INTEGER,
                        Tipo.STRING
                )
        );
    }

    @Test
    void stringNoPuedeRecibirBoolean() {
        assertFalse(
                ValidadorAsignaciones.esAsignacionValida(
                        Tipo.STRING,
                        Tipo.BOOLEAN
                )
        );
    }

    @Test
    void booleanNoPuedeRecibirInteger() {
        assertFalse(
                ValidadorAsignaciones.esAsignacionValida(
                        Tipo.BOOLEAN,
                        Tipo.INTEGER
                )
        );
    }

    @Test
    void unknownNoDebeSerCompatible() {
        assertFalse(
                ValidadorAsignaciones.esAsignacionValida(
                        Tipo.INTEGER,
                        Tipo.UNKNOWN
                )
        );
    }

    @Test
    void errorNoDebeSerCompatible() {
        assertFalse(
                ValidadorAsignaciones.esAsignacionValida(
                        Tipo.INTEGER,
                        Tipo.ERROR
                )
        );
    }
}