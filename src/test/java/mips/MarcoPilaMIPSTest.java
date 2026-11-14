package mips;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MarcoPilaMIPSTest {
    private MarcoPilaMIPS marco(List<String> params, List<String> locales) {
        return new MarcoPilaMIPS("f", false, params, locales, new TreeSet<>(), n -> n.matches("t\\d+"));
    }

    @Test
    void slotsLogicosSeConvierteAByteDesdeFp() {
        MarcoPilaMIPS m = marco(List.of("a", "b"), List.of("x"));
        assertEquals("-12($fp)", m.ubicacion("a"));
        assertEquals("-16($fp)", m.ubicacion("b"));
        assertEquals("-20($fp)", m.ubicacion("x"));
    }

    @Test
    void marcoAlineadoA8() {
        MarcoPilaMIPS m = marco(List.of("a"), List.of());
        assertEquals(16, m.tamanoMarco(0));   // 8 + 4 = 12 -> 16
        assertEquals(16, m.tamanoMarco(1));   // 8 + 4 + 4 = 16
        assertEquals(24, m.tamanoMarco(2));   // 20 -> 24
    }

    @Test
    void temporalesReservanSlotAlPrimerUso() {
        MarcoPilaMIPS m = marco(List.of(), List.of());
        assertEquals("-12($fp)", m.ubicacion("t1"));
        assertEquals("-16($fp)", m.ubicacion("t2"));
        assertEquals("-12($fp)", m.ubicacion("t1"));
    }

    @Test
    void parametrosExtraSeLeenDeLaPilaDelCaller() {
        MarcoPilaMIPS m = marco(List.of("a", "b", "c", "d", "e"), List.of());
        String p = String.join("\n", m.prologo(Set.of()));
        assertTrue(p.contains("lw $v1, 0($fp)"));
        assertTrue(p.contains("sw $a3,"));
    }

    @Test
    void variableNoDeclaradaEsErrorExplicito() {
        assertThrows(GeneracionMIPSException.class, () -> marco(List.of(), List.of()).ubicacion("%otra.0.zzz"));
    }

    @Test
    void mainUsaGlobalesParaVariablesDeUsuario() {
        Set<String> g = new TreeSet<>();
        MarcoPilaMIPS m = new MarcoPilaMIPS("main", true, List.of(), List.of("r"), g, n -> n.matches("t\\d+"));
        assertEquals("g_r", m.ubicacion("r"));
        assertTrue(g.contains("r"));
    }

    @Test
    void epilogoRestauraRaFpYSp() {
        String e = String.join("\n", marco(List.of(), List.of()).epilogo(Set.of("$s0")));
        assertTrue(e.contains("lw $s0,"));
        assertTrue(e.contains("move $sp, $fp"));
        assertTrue(e.contains("jr $ra"));
    }

    @Test
    void nombresDelTacSeConvierteEnEtiquetasValidas() {
        assertEquals("g__global_0_x", MarcoPilaMIPS.etiquetaGlobal("%global.0.x"));
        assertEquals("func_Animal_hablar", MarcoPilaMIPS.etiquetaDeFuncion("Animal.hablar"));
    }

    @Test
    void layoutDelProyecto2SeMapeaASlotsEnOrden() {
        var layout = new tac.RegistroActivacion("f", List.of(
                new tac.RegistroActivacion.Posicion("%f.0.n", "n", "integer", tac.RegistroActivacion.Clase.PARAMETRO, 3),
                new tac.RegistroActivacion.Posicion("t0", "t0", "integer", tac.RegistroActivacion.Clase.TEMPORAL, 4)));
        MarcoPilaMIPS m = new MarcoPilaMIPS(layout, List.of("%f.0.n"), new TreeSet<>(), n -> n.matches("t\\d+"));
        assertEquals("-12($fp)", m.ubicacion("%f.0.n"));   // offset lógico 3 -> slot 0
        assertEquals("-16($fp)", m.ubicacion("t0"));       // offset lógico 4 -> slot 1
        assertEquals("g_total", m.ubicacion("total"));      // sin '%': global del programa
    }
}
