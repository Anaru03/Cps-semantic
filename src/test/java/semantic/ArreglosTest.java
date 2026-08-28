package semantic;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ArreglosTest {

    @Test
    void arregloIntegerAceptaIntegers() {
        TipoArreglo arreglo =
                new TipoArreglo(Tipo.INTEGER);

        assertTrue(
                ValidadorArreglos.elementosCompatibles(
                        arreglo,
                        List.of(
                                Tipo.INTEGER,
                                Tipo.INTEGER,
                                Tipo.INTEGER
                        )
                )
        );
    }

    @Test
    void arregloStringAceptaStrings() {
        TipoArreglo arreglo =
                new TipoArreglo(Tipo.STRING);

        assertTrue(
                ValidadorArreglos.elementosCompatibles(
                        arreglo,
                        List.of(
                                Tipo.STRING,
                                Tipo.STRING
                        )
                )
        );
    }

    @Test
    void arregloIntegerRechazaString() {
        TipoArreglo arreglo =
                new TipoArreglo(Tipo.INTEGER);

        assertFalse(
                ValidadorArreglos.elementosCompatibles(
                        arreglo,
                        List.of(
                                Tipo.INTEGER,
                                Tipo.STRING,
                                Tipo.INTEGER
                        )
                )
        );
    }

    @Test
    void arregloBooleanRechazaInteger() {
        TipoArreglo arreglo =
                new TipoArreglo(Tipo.BOOLEAN);

        assertFalse(
                ValidadorArreglos.elementosCompatibles(
                        arreglo,
                        List.of(
                                Tipo.BOOLEAN,
                                Tipo.INTEGER
                        )
                )
        );
    }

    @Test
    void indiceIntegerEsValido() {
        assertTrue(
                ValidadorArreglos.indiceValido(
                        Tipo.INTEGER
                )
        );
    }

    @Test
    void indiceStringEsInvalido() {
        assertFalse(
                ValidadorArreglos.indiceValido(
                        Tipo.STRING
                )
        );
    }

    @Test
    void indiceBooleanEsInvalido() {
        assertFalse(
                ValidadorArreglos.indiceValido(
                        Tipo.BOOLEAN
                )
        );
    }
}