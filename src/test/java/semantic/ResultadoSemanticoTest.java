package semantic;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResultadoSemanticoTest {

    @Test
    void resultadoSinErroresDebeSerValido() {
        ResultadoSemantico resultado =
                new ResultadoSemantico(List.of());

        assertTrue(resultado.esValido());
        assertEquals(0, resultado.cantidadErrores());
    }

    @Test
    void resultadoConErroresNoDebeSerValido() {
        ErrorSemantico error = new ErrorSemantico(
                3,
                10,
                "Tipos incompatibles."
        );

        ResultadoSemantico resultado =
                new ResultadoSemantico(List.of(error));

        assertFalse(resultado.esValido());
        assertEquals(1, resultado.cantidadErrores());
    }

    @Test
    void debeConservarInformacionDelError() {
        ErrorSemantico error = new ErrorSemantico(
                5,
                8,
                "El operador requiere operandos numéricos."
        );

        ResultadoSemantico resultado =
                new ResultadoSemantico(List.of(error));

        ErrorSemantico obtenido = resultado.errores().get(0);

        assertEquals(5, obtenido.linea());
        assertEquals(8, obtenido.columna());
        assertEquals(
                "El operador requiere operandos numéricos.",
                obtenido.descripcion()
        );
    }
}