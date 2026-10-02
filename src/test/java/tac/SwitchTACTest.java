package tac;

import org.junit.jupiter.api.Test;
import semantic.AnalizadorSemantico;
import static org.junit.jupiter.api.Assertions.*;

class SwitchTACTest {
    private int resultado(String fuente) {
        return new CiclosTACTest().ejecutar(fuente).get("x");
    }

    @Test void coincidenciaConBreakSeleccionaSoloUnaRama() {
        assertEquals(20, resultado("let x: integer = 0; switch (2) {"
                + " case 1: x = 10; break; case 2: x = 20; break; default: x = 30; }"));
    }

    @Test void sinCoincidenciaUsaDefaultOSale() {
        assertEquals(30, resultado("let x: integer = 0; switch (9) { case 1: x = 10; break; default: x = 30; }"));
        assertEquals(0, resultado("let x: integer = 0; switch (9) { case 1: x = 10; break; }"));
        assertEquals(0, resultado("let x: integer = 0; switch (9) {}"));
        assertEquals(5, resultado("let x: integer = 0; switch (9) { default: x = 5; }"));
    }

    @Test void sinBreakCaeALosCasosSiguientesYDefault() {
        assertEquals(6, resultado("let x: integer = 0; switch (1) {"
                + " case 1: x = x + 1; case 2: x = x + 2; default: x = x + 3; }"));
    }

    @Test void selectorSeEvaluaUnaVezAntesDeLosCase() {
        assertEquals(3, resultado("let x: integer = 1; switch (x = x + 1) {"
                + " case (x = 3): x = 10; break; case 2: break; default: x = 20; }"));
    }

    @Test void switchAnidadoBreakSaleSoloDelInterno() {
        assertEquals(3, resultado("let x: integer = 0; switch (1) { case 1:"
                + " switch (2) { case 2: x = 1; break; default: x = 10; }"
                + " x = x + 2; break; default: x = 20; }"));
    }

    @Test void breakDelSwitchNoSaleDelCicloYContinueLlegaALaActualizacion() {
        assertEquals(12, resultado("let x: integer = 0; for (let i: integer = 0; i < 3; i = i + 1) {"
                + " switch (i) { case 0: continue; case 1: x = x + 1; break; default: x = x + 1; }"
                + " x = x + 5; }"));
    }

    @Test void breakDeCicloDentroDeSwitchNoSaleDelSwitch() {
        assertEquals(2, resultado("let x: integer = 0; switch (1) { case 1:"
                + " while (true) { x = 1; break; } x = x + 1; break; }"));
    }

    @Test void semanticaRechazaContinueSinCicloYCaseIncompatible() {
        assertFalse(AnalizadorSemantico.analizar("switch (1) { case 1: continue; }").resultado().errores().isEmpty());
        assertFalse(AnalizadorSemantico.analizar("switch (1) { case true: break; }").resultado().errores().isEmpty());
        assertFalse(AnalizadorSemantico.analizar("break;").resultado().errores().isEmpty());
    }

    @Test void funcionesNoHeredanContextosDeControlExternos() {
        assertFalse(AnalizadorSemantico.analizar("switch (1) { case 1: function f() { break; } }")
                .resultado().errores().isEmpty());
        assertFalse(AnalizadorSemantico.analizar("while (true) { function f() { continue; } }")
                .resultado().errores().isEmpty());
    }
}
