package mips;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class GeneradorFuncionesMIPSTest {

    @Test
    void retornoGuardaGlobalModificadaAntesDeSaltarAlEpilogo() {
        for (String retorno : Arrays.asList(null, "total", "7")) {
            GeneradorFuncionesMIPS g = new GeneradorFuncionesMIPS();
            g.iniciarFuncion("actualizar", List.of(), List.of());
            g.copiar("total", "42");
            g.retorno(retorno);
            g.finFuncion();
            g.iniciarMain(List.of());
            g.finFuncion();
            String asm = g.ensamblar();
            int inicio = asm.indexOf("func_actualizar:");
            int guardado = asm.indexOf("sw $t0, g_total", inicio);
            int salto = asm.indexOf("j func_actualizar_epilogo", inicio);
            assertTrue(guardado > inicio && guardado < salto, asm);
        }
    }

    @Test
    void factorialGeneraSecuenciaDeLlamadaYRetorno() {
        String asm = DemoFactorial.generar();
        assertTrue(asm.contains("func_factorial:"));
        assertTrue(asm.contains("jal func_factorial"));
        assertTrue(asm.contains("jr $ra"));
        assertTrue(asm.contains("sw $ra,"));
        assertTrue(asm.contains("lw $ra,"));
        assertTrue(asm.contains("sw $s0,"), "n vive en $s0 y debe preservarse entre activaciones");
    }

    @Test
    void masDeCuatroArgumentosUsanLaPila() {
        String asm = DemoFactorial.generar();
        assertTrue(asm.contains("addiu $sp, $sp, -8"));
        assertTrue(asm.contains("sw $t1, 0($sp)"));
        assertTrue(asm.contains("addiu $sp, $sp, 8"), "la pila se restaura tras el retorno");
    }

    @Test
    void argumentoSinLlamadaEsError() {
        GeneradorFuncionesMIPS g = new GeneradorFuncionesMIPS();
        g.iniciarMain(List.of());
        g.argumento("1");
        assertThrows(GeneracionMIPSException.class, g::finFuncion);
    }

    @Test
    void llamadaConMenosArgumentosDeLosEsperadosEsError() {
        GeneradorFuncionesMIPS g = new GeneradorFuncionesMIPS();
        g.iniciarMain(List.of());
        g.argumento("1");
        assertThrows(GeneracionMIPSException.class, () -> g.llamada(null, "func_f", 2));
    }

    @Test
    void funcionesAnidadasNoSoportadasDanErrorExplicito() {
        GeneradorFuncionesMIPS g = new GeneradorFuncionesMIPS();
        g.iniciarFuncion("a", List.of(), List.of());
        assertThrows(GeneracionMIPSException.class, () -> g.iniciarFuncion("b", List.of(), List.of()));
    }

    @Test
    void sinMainNoSeEmiteEnsamblador() {
        GeneradorFuncionesMIPS g = new GeneradorFuncionesMIPS();
        g.iniciarFuncion("a", List.of(), List.of());
        g.finFuncion();
        assertThrows(GeneracionMIPSException.class, g::ensamblar);
    }

    /** Ejecuta en MARS si existe: MARS_JAR=/ruta/Mars4_5.jar mvn test */
    @Test
    void ejecucionEnMarsFactorialYSuma6() throws Exception {
        assertEquals("120\n21\n", MarsTestSupport.ejecutar(DemoFactorial.generar()));
    }
}
