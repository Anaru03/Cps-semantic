package mips;

import compiler.Compilador;
import compiler.ResultadoCompilacion;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Pipeline real: .cps -> ANTLR -> semántico -> TAC -> MIPS (funciones, pila, recursión). */
class FuncionesMIPSPipelineTest {

    private static String asm(String ruta) throws Exception {
        ResultadoCompilacion r = Compilador.compilar(Files.readString(Path.of(ruta)));
        assertTrue(r.esValido(), r.errores().toString());
        return TraductorTACProvisional.traducir(r);
    }

    private static String ejecutarEnMars(String asm) throws Exception {
        return MarsTestSupport.ejecutar(asm);
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

    @Test
    void ejecucionMarsConservaGlobalesConRetornoExplicito() throws Exception {
        ResultadoCompilacion r = Compilador.compilar("""
                let total: integer = 0;
                function incrementar(): integer {
                    total = total + 1;
                    return total;
                }
                function fijar() {
                    total = 40;
                    return;
                }
                print(incrementar());
                print(total);
                fijar();
                print(total);
                print(incrementar());
                print(total);
                """);
        assertTrue(r.esValido(), r.errores().toString());
        assertEquals("1\n1\n40\n41\n41\n",
                ejecutarEnMars(TraductorTACProvisional.traducir(r)));
    }
}
