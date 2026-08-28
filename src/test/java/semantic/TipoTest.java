package semantic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TipoTest {

    @Test
    void integerDebeSerNumerico() {
        assertTrue(Tipo.INTEGER.esNumerico());
    }

    @Test
    void stringNoDebeSerNumerico() {
        assertFalse(Tipo.STRING.esNumerico());
    }

    @Test
    void booleanDebeSerBooleano() {
        assertTrue(Tipo.BOOLEAN.esBooleano());
    }

    @Test
    void integerNoDebeSerBooleano() {
        assertFalse(Tipo.INTEGER.esBooleano());
    }

    @Test
    void errorDebeIdentificarseComoError() {
        assertTrue(Tipo.ERROR.esError());
    }

    @Test
    void unknownNoDebeIdentificarseComoError() {
        assertFalse(Tipo.UNKNOWN.esError());
    }
    @Test
    void tiposIgualesDebenSerCompatibles() {
        assertTrue(
                Tipo.INTEGER.esCompatibleCon(Tipo.INTEGER)
        );
    }

    @Test
    void tiposDiferentesNoDebenSerCompatibles() {
        assertFalse(
                Tipo.INTEGER.esCompatibleCon(Tipo.STRING)
        );
    }

    @Test
    void unknownNoDebeSerCompatible() {
        assertFalse(
                Tipo.INTEGER.esCompatibleCon(Tipo.UNKNOWN)
        );
    }

    @Test
    void errorNoDebeSerCompatible() {
        assertFalse(
                Tipo.INTEGER.esCompatibleCon(Tipo.ERROR)
        );
    }
}