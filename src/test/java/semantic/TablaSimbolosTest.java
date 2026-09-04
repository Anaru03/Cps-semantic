package semantic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Prueba la tabla de simbolos (Ambito) de forma aislada, sin pasar por el analizador completo. */
class TablaSimbolosTest {

    @Test void insertarRegistraUnSimboloNuevo() {
        Ambito ambito = new Ambito("global", null);
        Simbolo x = new Simbolo("x", TipoDato.INTEGER, CategoriaSimbolo.VARIABLE, ambito);
        assertTrue(ambito.declarar(x));
    }

    @Test void insertarRechazaUnNombreDuplicadoEnElMismoAmbito() {
        Ambito ambito = new Ambito("global", null);
        ambito.declarar(new Simbolo("x", TipoDato.INTEGER, CategoriaSimbolo.VARIABLE, ambito));
        boolean segundaVez = ambito.declarar(new Simbolo("x", TipoDato.STRING, CategoriaSimbolo.VARIABLE, ambito));
        assertFalse(segundaVez);
    }

    @Test void recuperarLocalEncuentraUnSimboloDeclaradoEnEsteAmbito() {
        Ambito ambito = new Ambito("global", null);
        ambito.declarar(new Simbolo("x", TipoDato.INTEGER, CategoriaSimbolo.VARIABLE, ambito));
        assertEquals(TipoDato.INTEGER, ambito.buscarLocal("x").orElseThrow().tipo());
    }

    @Test void recuperarLocalNoEncuentraSimbolosDeAmbitosPadre() {
        Ambito global = new Ambito("global", null);
        global.declarar(new Simbolo("x", TipoDato.INTEGER, CategoriaSimbolo.VARIABLE, global));
        Ambito hijo = new Ambito("bloque", global);
        assertTrue(hijo.buscarLocal("x").isEmpty());
    }

    @Test void actualizarModificaElTipoDeUnSimboloExistente() {
        Ambito ambito = new Ambito("global", null);
        ambito.declarar(new Simbolo("x", TipoDato.UNKNOWN, CategoriaSimbolo.VARIABLE, ambito));
        boolean actualizado = ambito.actualizar("x", TipoDato.INTEGER);
        assertTrue(actualizado);
        assertEquals(TipoDato.INTEGER, ambito.buscarLocal("x").orElseThrow().tipo());
    }

    @Test void actualizarDevuelveFalsoSiElSimboloNoExiste() {
        Ambito ambito = new Ambito("global", null);
        assertFalse(ambito.actualizar("fantasma", TipoDato.INTEGER));
    }

    @Test void actualizarPreservaCategoriaYSoloCambiaElTipo() {
        Ambito ambito = new Ambito("global", null);
        ambito.declarar(new Simbolo("f", TipoDato.VOID, CategoriaSimbolo.FUNCION, ambito));
        ambito.actualizar("f", TipoDato.INTEGER);
        Simbolo actualizado = ambito.buscarLocal("f").orElseThrow();
        assertEquals(TipoDato.INTEGER, actualizado.tipo());
        assertEquals(CategoriaSimbolo.FUNCION, actualizado.categoria());
    }

    // ---- Manejo de alcances ----

    @Test void buscarResuelveUnSimboloDeclaradoEnUnAmbitoPadre() {
        Ambito global = new Ambito("global", null);
        global.declarar(new Simbolo("x", TipoDato.INTEGER, CategoriaSimbolo.VARIABLE, global));
        Ambito bloque = new Ambito("bloque", global);
        Ambito anidado = new Ambito("bloque", bloque);
        assertEquals(TipoDato.INTEGER, anidado.buscar("x").orElseThrow().tipo());
    }

    @Test void buscarNoEncuentraUnSimboloFueraDeAlcance() {
        Ambito global = new Ambito("global", null);
        Ambito bloque = new Ambito("bloque", global);
        bloque.declarar(new Simbolo("y", TipoDato.INTEGER, CategoriaSimbolo.VARIABLE, bloque));
        assertTrue(global.buscar("y").isEmpty());
    }

    @Test void unAmbitoHijoPuedeSombrearUnNombreDelPadre() {
        Ambito global = new Ambito("global", null);
        global.declarar(new Simbolo("x", TipoDato.INTEGER, CategoriaSimbolo.VARIABLE, global));
        Ambito bloque = new Ambito("bloque", global);
        bloque.declarar(new Simbolo("x", TipoDato.STRING, CategoriaSimbolo.VARIABLE, bloque));
        assertEquals(TipoDato.STRING, bloque.buscar("x").orElseThrow().tipo());
        assertEquals(TipoDato.INTEGER, global.buscar("x").orElseThrow().tipo());
    }

    @Test void actualizarBuscaEnAmbitosPadreCuandoNoEstaEnElLocal() {
        Ambito global = new Ambito("global", null);
        global.declarar(new Simbolo("x", TipoDato.UNKNOWN, CategoriaSimbolo.VARIABLE, global));
        Ambito bloque = new Ambito("bloque", global);
        boolean actualizado = bloque.actualizar("x", TipoDato.BOOLEAN);
        assertTrue(actualizado);
        assertEquals(TipoDato.BOOLEAN, global.buscarLocal("x").orElseThrow().tipo());
    }
}
