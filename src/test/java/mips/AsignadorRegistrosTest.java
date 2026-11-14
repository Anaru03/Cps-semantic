package mips;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AsignadorRegistrosTest {
    private final List<String> salida = new ArrayList<>();

    private AsignadorRegistros crear(List<String> locales) {
        MarcoPilaMIPS m = new MarcoPilaMIPS("f", false, List.of(), locales, new TreeSet<>(), n -> n.matches("t\\d+"));
        return new AsignadorRegistros(m, salida::add, n -> n.matches("t\\d+"));
    }

    @Test
    void reutilizaRegistroSinRecargar() {
        AsignadorRegistros a = crear(List.of("x"));
        String r1 = a.getReg("x");
        a.liberarBloqueos();
        String r2 = a.getReg("x");
        assertEquals(r1, r2);
        assertEquals(1, salida.stream().filter(s -> s.startsWith("lw")).count());
    }

    @Test
    void localesPrefierenSYTemporalesPrefierenT() {
        AsignadorRegistros a = crear(List.of("x"));
        assertTrue(a.getReg("x").startsWith("$s"));
        assertTrue(a.getRegDestino("t1").startsWith("$t"));
    }

    @Test
    void literalCeroUsaZeroYOtrosUsanLi() {
        AsignadorRegistros a = crear(List.of());
        assertEquals("$zero", a.getReg("0"));
        String r = a.getReg("7");
        assertTrue(salida.contains("li " + r + ", 7"));
        a.liberarBloqueos();
        assertTrue(a.registroDe("#lit0").isEmpty());
    }

    @Test
    void presionDeRegistrosDerramaValoresSucios() {
        List<String> locales = new ArrayList<>();
        for (int i = 0; i < 25; i++) locales.add("v" + i);
        AsignadorRegistros a = crear(locales);
        for (String v : locales) { a.getRegDestino(v); a.liberarBloqueos(); }
        assertTrue(a.derrames() >= 7);                       // 25 variables - 18 registros
        assertTrue(salida.stream().anyMatch(s -> s.startsWith("sw")));
        assertEquals(8, a.sUsados().size());
    }

    @Test
    void masDeUnaInstruccionNoDesalojaOperandosBloqueados() {
        List<String> locales = new ArrayList<>();
        for (int i = 0; i < 20; i++) locales.add("v" + i);
        AsignadorRegistros a = crear(locales);
        Set<String> usados = new HashSet<>();
        for (int i = 0; i < 18; i++) assertTrue(usados.add(a.getReg("v" + i)));  // sin liberar: 18 distintos
        assertThrows(GeneracionMIPSException.class, () -> a.getReg("v19"));       // 19 simultáneos: error explícito
    }

    @Test
    void preLlamadaLiberaTPeroConservaS() {
        AsignadorRegistros a = crear(List.of("x"));
        a.getReg("x");
        a.getRegDestino("t1");
        a.liberarBloqueos();
        a.preLlamada();
        assertTrue(a.registroDe("x").isPresent());
        assertTrue(a.registroDe("t1").isEmpty());
        assertTrue(salida.stream().anyMatch(s -> s.startsWith("sw") && s.contains("($fp)"))); // t1 escrito antes de jal
    }

    @Test
    void invalidarEscribeSuciosYOlvida() {
        AsignadorRegistros a = crear(List.of("x"));
        a.getRegDestino("x");
        a.invalidar();
        assertTrue(salida.stream().anyMatch(s -> s.startsWith("sw")));
        assertTrue(a.registroDe("x").isEmpty());
    }

    @Test
    void liberarTemporalNoGeneraStore() {
        AsignadorRegistros a = crear(List.of());
        a.getRegDestino("t1");
        a.liberarBloqueos();
        a.liberar("t1");
        a.invalidar();
        assertTrue(salida.stream().noneMatch(s -> s.startsWith("sw")));
    }
}
