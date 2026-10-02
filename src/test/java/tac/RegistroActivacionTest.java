package tac;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RegistroActivacionTest {
    private RegistroActivacion layout() {
        return new RegistroActivacion("f", List.of(
                new RegistroActivacion.Posicion("%f.0.n", "n", "integer", RegistroActivacion.Clase.PARAMETRO, 3),
                new RegistroActivacion.Posicion("%f.1.x", "x", "integer", RegistroActivacion.Clase.LOCAL, 4),
                new RegistroActivacion.Posicion("t0", "t0", "unknown", RegistroActivacion.Clase.TEMPORAL, 5)));
    }
    @Test void layoutTieneCabeceraYOffsetsLogicosValidos() {
        var layout = layout();
        assertEquals(6, layout.cantidadSlots());
        assertEquals(4, layout.posicion("%f.1.x").offset());
        assertThrows(UnsupportedOperationException.class, () -> layout.posiciones().clear());
        assertThrows(IllegalArgumentException.class, () -> layout.posicion("ausente"));
        assertThrows(IllegalArgumentException.class, () -> new RegistroActivacion("f", List.of(
                new RegistroActivacion.Posicion("x", "x", "integer", RegistroActivacion.Clase.LOCAL, 99))));
    }
    @Test void recursionMantieneSlotsEnlaceYDireccionDeRegresoIndependientes() {
        var pila = new PilaActivaciones();
        var padre = pila.entrar(layout(), List.of(5), 10, "resultadoGlobal");
        padre.escribir("%f.1.x", 12); padre.escribir("t0", 99);
        var hijo = pila.entrar(layout(), List.of(4), 25, "t0");
        hijo.escribir("%f.1.x", 8);
        assertSame(padre, hijo.llamador());
        assertEquals(5, padre.leer("%f.0.n"));
        assertEquals(4, hijo.leer("%f.0.n"));
        assertEquals(12, padre.leer("%f.1.x"));
        var regreso = pila.salir(24);
        assertEquals(25, regreso.direccion()); assertEquals(24, padre.leer("t0"));
        assertEquals(24, hijo.valorRetorno()); assertSame(padre, pila.actual());
        assertEquals("resultadoGlobal", pila.salir(120).destino());
        assertEquals(0, pila.profundidad()); assertNull(pila.actual());
    }
    @Test void rechazaArgumentosInvalidosYRetornosSinMarco() {
        var pila = new PilaActivaciones();
        assertThrows(IllegalArgumentException.class, () -> pila.entrar(layout(), List.of(), 0, null));
        assertEquals(0, pila.profundidad());
        assertThrows(IllegalStateException.class, () -> pila.salir(null));
    }
}
