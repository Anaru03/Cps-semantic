package mips;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class OptimizadorMirillaTest {
    private static String programa(String cuerpo, int valor) {
        return ".text\n.globl main\nmain:\nli $t0, " + valor + "\n" + cuerpo
                + "\nmove $a0, $t1\nli $v0, 1\nsyscall\nli $a0, 10\nli $v0, 11\nsyscall\nli $v0, 10\nsyscall\n";
    }
    @Test void nueveReglasConservanLaSalidaEnMars() throws Exception {
        String[][] casos = {
                {"move-identico", "move $t1, $t0\nmove $t1, $t1"},
                {"addiu-cero", "addiu $t1, $t0, 0"},
                {"addu-cero", "addu $t1, $zero, $t0"},
                {"subu-cero", "subu $t1, $t0, $zero"},
                {"or-cero", "or $t1, $t0, $zero"},
                {"shift-cero", "sll $t1, $t0, 0"},
                {"salto-siguiente", "move $t1, $t0\nj siguiente\nsiguiente:"},
                {"li-repetido", "li $t1, 37\nli $t1, 37"},
                {"li-cero", "li $t1, 0"}
        };
        for (String[] caso : casos) {
            String original = programa(caso[1], -2147483648);
            var optimizado = new OptimizadorMirilla().optimizar(original);
            assertTrue(optimizado.aplicaciones().get(caso[0]) > 0, caso[0]);
            String esperada = MarsTestSupport.ejecutar(original);
            assertEquals(esperada, MarsTestSupport.ejecutar(optimizado.codigo()), caso[0]);
            Path evidencia = Path.of("target", "mips", "mirilla");
            Files.createDirectories(evidencia);
            Files.writeString(evidencia.resolve(caso[0] + ".antes.asm"), original);
            Files.writeString(evidencia.resolve(caso[0] + ".despues.asm"), optimizado.codigo());
            Files.writeString(evidencia.resolve(caso[0] + ".out"), esperada);
        }
    }
    @Test void conservaDatosComentariosEtiquetasYOperacionesConEfectos() {
        String s = """
                .data
                mensaje: .asciiz "move $t0, $t0"
                .text
                main:
                addi $t0, $t0, 0
                sw $t0, 0($sp)
                lw $t0, 0($sp)
                jal otra
                li $t0, 1
                etiqueta:
                li $t0, 1
                subu $t0, $zero, $t1
                # comentario
                jr $ra
                """;
        var r = new OptimizadorMirilla().optimizar(s);
        assertEquals(s, r.codigo());
        assertEquals(0, r.total());
    }
    @Test void esIdempotenteYNoConfundeRegistros() {
        String s = programa("addu $t1, $t1, $zero\nmove $t1, $t10", 10);
        var o = new OptimizadorMirilla();
        var r = o.optimizar(s);
        assertFalse(r.codigo().contains("move $t1, $t1\n"));
        assertTrue(r.codigo().contains("move $t1, $t10"));
        assertEquals(r.codigo(), o.optimizar(r.codigo()).codigo());
        assertEquals(0, o.optimizar(r.codigo()).total());
    }
    @Test void demoDesdeTacActivaAlMenosSeisTransformaciones() throws Exception {
        var fuente = Files.readString(Path.of("examples/mips/mirilla_demo.cps"));
        var r = new GeneradorMIPS().generar(compiler.Compilador.compilar(fuente));
        assertTrue(r.esValido(), r.errores().toString());
        assertTrue(r.optimizaciones().values().stream().filter(n -> n > 0).count() >= 6,
                r.optimizaciones().toString());
        assertEquals(MarsTestSupport.ejecutar(r.sinOptimizar()), MarsTestSupport.ejecutar(r.codigo()));
    }

}
