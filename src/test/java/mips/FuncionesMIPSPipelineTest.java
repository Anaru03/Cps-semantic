package mips;

import compiler.Compilador;
import compiler.ResultadoCompilacion;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** Pipeline real: .cps -> ANTLR -> semántico -> TAC -> MIPS (funciones, pila, recursión). */
class FuncionesMIPSPipelineTest {

    private static String asm(String ruta) throws Exception {
        ResultadoCompilacion r = Compilador.compilar(Files.readString(Path.of(ruta)));
        assertTrue(r.esValido(), r.errores().toString());
        return TraductorTACProvisional.traducir(r);
    }

    private static String ejecutarEnMars(String asm) throws Exception {
        String jar = System.getenv("MARS_JAR");
        Assumptions.assumeTrue(jar != null && new File(jar).exists(), "MARS_JAR no configurado");
        Path f = Files.createTempFile("p3", ".asm");
        Files.writeString(f, asm);
        Process p = new ProcessBuilder("java", "-jar", jar, "nc", f.toString()).redirectErrorStream(true).start();
        assertTrue(p.waitFor(30, TimeUnit.SECONDS));
        return new String(p.getInputStream().readAllBytes()).replace("\r", "");
    }

    @Test
    void factorialYFibonacciGeneranLlamadasRecursivas() throws Exception {
        String s = asm("examples/mips/funciones_recursion.cps");
        assertTrue(s.contains("func_factorial:"));
        assertTrue(s.contains("func_fib:"));
        assertTrue(s.contains("jal func_factorial"));
        assertEquals(3, s.split("jal func_fib", -1).length - 1, "dos llamadas recursivas + una desde main");
        assertTrue(s.contains("jr $ra"));
    }

    @Test
    void masDeCuatroArgumentosSeEnvianPorPilaYSeRestaura() throws Exception {
        String s = asm("examples/mips/funciones_recursion.cps");
        assertTrue(s.contains("addiu $sp, $sp, -8"));
        assertTrue(s.contains("addiu $sp, $sp, 8"));
        assertTrue(s.contains("lw $v1, 0($fp)"));
    }

    @Test
    void elGotoQueSaltaLaFuncionNoSeEmite() throws Exception {
        String s = asm("examples/mips/funciones_recursion.cps");
        assertFalse(s.contains("j L0\n"), "el 'goto L0' sobre factorial es innecesario");
    }

    @Test
    void localesYGlobalesSeSeparan() throws Exception {
        String s = asm("examples/mips/recursion_locales.cps");
        assertTrue(s.contains("g_llamadas: .word 0"));
        assertTrue(s.contains("g_resultado: .word 0"));
        assertTrue(s.contains("sw $s0,"), "locales en $s se preservan en el prólogo");
    }

    @Test
    void conErroresNoSeGeneraMips() {
        ResultadoCompilacion r = Compilador.compilar("let x: integer = \"texto\";");
        assertFalse(r.esValido());
        assertThrows(IllegalArgumentException.class, () -> TraductorTACProvisional.traducir(r));
    }

    @Test
    void construccionesPendientesDanErrorExplicito() throws Exception {
        ResultadoCompilacion r = Compilador.compilar(Files.readString(Path.of("examples/tac/arreglos.cps")));
        assertThrows(GeneracionMIPSException.class, () -> TraductorTACProvisional.traducir(r));
    }

    @Test
    void ejecucionMarsFunciones() throws Exception {
        assertEquals("120\n55\n123\n21\nhola desde una funcion\n",
                ejecutarEnMars(asm("examples/mips/funciones_recursion.cps")));
    }

    @Test
    void ejecucionMarsRecursionConLocales() throws Exception {
        assertEquals("15\n6\n", ejecutarEnMars(asm("examples/mips/recursion_locales.cps")));
    }
}
