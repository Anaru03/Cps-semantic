package mips;

import compiler.Compilador;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GeneradorMIPSTest {
    @Test void erroresDeAnalisisNoProducenEnsamblador() {
        var r = new GeneradorMIPS().generar(Compilador.compilar("let x: integer = \"error\";"));
        assertFalse(r.esValido());
        assertFalse(r.errores().isEmpty());
        assertEquals("", r.codigo());
        assertEquals("", r.sinOptimizar());
    }
    @Test void floatPendienteProduceErrorExplicito() {
        for (String s : new String[]{"let x: float = 1.5; print(x);", "print(1.5);"}) {
            var entrada = Compilador.compilar(s);
            assertTrue(entrada.esValido(), entrada.errores().toString());
            var r = new GeneradorMIPS().generar(entrada);
            assertFalse(r.esValido());
            assertEquals("", r.codigo());
            assertEquals("", r.sinOptimizar());
        }
    }
    @Test void sePuedeDesactivarLaMirilla() throws Exception {
        var entrada = Compilador.compilar("let xs: integer[] = [1,2]; print(xs[1]);");
        var backend = new GeneradorMIPS();
        var original = backend.generar(entrada, false);
        var optimizado = backend.generar(entrada);
        assertTrue(original.esValido(), original.errores().toString());
        assertTrue(optimizado.esValido(), optimizado.errores().toString());
        assertEquals(original.codigo(), optimizado.sinOptimizar());
        assertEquals(original.codigo(), original.sinOptimizar());
        assertEquals("2\n", MarsTestSupport.ejecutar(optimizado.codigo()));
    }
}
