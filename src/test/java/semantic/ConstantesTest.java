package semantic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConstantesTest {

    @Test
    void integerPuedeInicializarseConInteger() {
        assertTrue(
                ValidadorConstantes.esInicializacionValida(
                        Tipo.INTEGER,
                        Tipo.INTEGER
                )
        );
    }

    @Test
    void stringPuedeInicializarseConString() {
        assertTrue(
                ValidadorConstantes.esInicializacionValida(
                        Tipo.STRING,
                        Tipo.STRING
                )
        );
    }

    @Test
    void booleanPuedeInicializarseConBoolean() {
        assertTrue(
                ValidadorConstantes.esInicializacionValida(
                        Tipo.BOOLEAN,
                        Tipo.BOOLEAN
                )
        );
    }

    @Test
    void integerNoPuedeInicializarseConString() {
        assertFalse(
                ValidadorConstantes.esInicializacionValida(
                        Tipo.INTEGER,
                        Tipo.STRING
                )
        );
    }

    @Test
    void booleanNoPuedeInicializarseConInteger() {
        assertFalse(
                ValidadorConstantes.esInicializacionValida(
                        Tipo.BOOLEAN,
                        Tipo.INTEGER
                )
        );
    }

    @Test
    void unknownNoEsInicializacionValida() {
        assertFalse(
                ValidadorConstantes.esInicializacionValida(
                        Tipo.INTEGER,
                        Tipo.UNKNOWN
                )
        );
    }
}