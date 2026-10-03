package tac;

import compiler.*;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Casos exitosos y fallidos de cada componente de la rúbrica, verificando comportamiento. */
class CompletoTACTest {
    static ResultadoCompilacion ok(String fuente) {
        var r = Compilador.compilar(fuente);
        assertTrue(r.esValido(), r.errores().toString());
        return r;
    }
    static List<String> correr(String fuente) { var m = new MaquinaTAC(ok(fuente)); m.ejecutar(); return m.salida; }
    static ResultadoCompilacion falla(String fuente) {
        var r = Compilador.compilar(fuente);
        assertFalse(r.esValido(), "Debía fallar: " + fuente);
        assertEquals("", r.codigoTAC(), "No debe generarse TAC con errores");
        return r;
    }
    static String archivo(String ruta) throws Exception { return Files.readString(Path.of(ruta)); }

    // ---------- variables, constantes y aritmética ----------
    @Test void variablesConstantesYAritmetica() {
        assertEquals(List.of("14", "2", "7.5"), correr(
            "let a = 2 + 3 * 4; const b: integer = 2; let c: float = 7.5; print(a); print(b); print(c);"));
        assertEquals(List.of("-3", "1"), correr("let x = -(1 + 2); let y = 7 % 3; print(x); print(y);"));
    }
    @Test void constanteNoSePuedeReasignar() { falla("const k = 1; k = 2;"); }
    @Test void tipoIncompatibleEnDeclaracion() { falla("let x: integer = \"a\";"); }

    // ---------- expresiones lógicas con cortocircuito ----------
    @Test void logicasYTernario() {
        assertEquals(List.of("true", "false", "10", "20"), correr(
            "let a = 3; let b = 0; print(a > 1 && b == 0 || false); print(!(a > 1));"
            + "let r = a > 1 ? 10 : 20; print(r); let s = a < 1 ? 10 : 20; print(s);"));
    }
    @Test void cortocircuitoNoEvaluaOperandoDerecho() {
        var tac = ok("let n = 0; function f(): boolean { n = n + 1; return true; }"
                + " let a = false && f(); let b = true || f(); print(n);");
        var m = new MaquinaTAC(tac); m.ejecutar();
        assertEquals(List.of("0"), m.salida);
        var m2 = new MaquinaTAC(ok("let n = 0; function f(): boolean { n = n + 1; return true; }"
                + " let a = true && f(); let b = false || f(); print(n);")); m2.ejecutar();
        assertEquals(List.of("2"), m2.salida);
    }
    @Test void operandoLogicoNoBooleanoEsError() { falla("let x = 1 && true;"); falla("let x = !5;"); }

    // ---------- arreglos ----------
    @Test void arreglosLecturaEscrituraLongitudYAnidados() {
        assertEquals(List.of("40", "3", "4"), correr(
            "let xs: integer[] = [10, 20, 30]; xs[1] = xs[0] + xs[2]; print(xs[1]); print(xs.length);"
            + "let m: integer[][] = [[1,2],[3,4]]; print(m[1][1]);"));
    }
    @Test void foreachSumaElementos() {
        assertEquals(List.of("10"), correr("let s = 0; foreach (v in [1,2,3,4]) { s = s + v; } print(s);"));
    }
    @Test void arreglosErrores() {
        falla("let xs = [1, 2]; let y = xs[\"a\"];");
        falla("let xs: integer[] = [1, \"a\"];");
        falla("let n = 5; let y = n[0];");
        falla("let xs: integer[] = [1]; xs[0] = true;");
    }

    // ---------- control de flujo ----------
    @Test void ifWhileDoWhileFor() {
        assertEquals(List.of("a", "6", "3", "3"), correr(
            "let x = 1; if (x > 0) { print(\"a\"); } else { print(\"b\"); }"
            + "let s = 0; let i = 0; while (i < 4) { i = i + 1; s = s + i; if (s > 5) { break; } } print(s);"
            + "let k = 0; do { k = k + 1; } while (k < 3); print(k);"
            + "let c = 0; for (let j = 0; j < 5; j = j + 1) { if (j == 3) { break; } c = c + 1; } print(c);"));
    }
    @Test void continueYSwitch() {
        assertEquals(List.of("9", "dos", "otro"), correr(
            "let s = 0; for (let i = 0; i < 5; i = i + 1) { if (i == 2) { continue; } s = s + i; } print(s + 1);"
            + "function f(n: integer): string { switch (n) { case 2: return \"dos\"; default: return \"otro\"; } }"
            + "print(f(2)); print(f(9));"));
    }
    @Test void condicionNoBooleanaEsError() { falla("if (1) { }"); falla("while (\"a\") { }"); }
    @Test void breakContinueFueraDeCiclo() { falla("break;"); falla("continue;"); }

    // ---------- funciones, parámetros y recursividad ----------
    @Test void funcionesYParametros() {
        assertEquals(List.of("5", "9"), correr(
            "function sumar(a: integer, b: integer): integer { return a + b; } print(sumar(2, 3)); print(sumar(sumar(1, 2), 6));"));
    }
    @Test void recursividadFactorialYFibonacci() throws Exception {
        assertEquals(List.of("120", "55"), correr(archivo("examples/tac/factorial.cps")));
    }
    @Test void funcionesErrores() {
        falla("function f(a: integer): integer { return a; } let x = f(1, 2);");
        falla("function f(a: integer): integer { return a; } let x = f(\"s\");");
        falla("function f(): integer { return \"s\"; }");
        falla("let x = g(1);");
    }

    // ---------- clases, objetos, herencia ----------
    @Test void clasesYObjetos() {
        assertEquals(List.of("21", "5"), correr(
            "class P { let edad: integer = 20; function constructor(e: integer) { this.edad = e + 1; }"
            + " function dame(): integer { return edad; } function suma(n: integer): integer { return this.edad + n; } }"
            + "let p = new P(0); p.edad = 21; print(p.edad); print(p.suma(0) - 16);"));
    }
    @Test void herenciaPolimorfismoYCamposHeredados() throws Exception {
        var r = ok(archivo("examples/tac/clases_herencia.cps"));
        var m = new MaquinaTAC(r); m.ejecutar();
        assertEquals(List.of("5"), m.salida.subList(m.salida.size() - 1, m.salida.size())); // patas heredadas + 1
        assertTrue(r.codigoTAC().contains("vcall Animal.hablar"), "Debe despachar dinámicamente");
        var perro = r.clases().get("Perro");
        assertEquals("Animal", perro.padre());
        assertEquals(r.clases().get("Animal").campo("nombre").offset(), perro.campo("nombre").offset(),
                "Los campos heredados conservan su offset");
        assertTrue(perro.metodo("hablar").sobrescribe());
        assertEquals(r.clases().get("Animal").metodo("hablar").ranura(), perro.metodo("hablar").ranura());
    }
    @Test void despachoVirtualEligeElMetodoDeLaSubclase() {
        var m = new MaquinaTAC(ok("class A { function h(): string { return \"grr\"; } }"
            + "class B : A { function h(): string { return \"guau\"; } }"
            + "let x: A = new B(); print(x.h()); let y: A = new A(); print(y.h());"));
        m.ejecutar();
        assertEquals(List.of("guau", "grr"), m.salida);
    }
    @Test void constructorHeredado() {
        assertEquals(List.of("7"), correr("class A { let v: integer; function constructor(n: integer) { this.v = n; } }"
                + "class B : A { } let b = new B(7); print(b.v);"));
    }
    @Test void clasesErrores() {
        falla("let o = new Inexistente();");
        falla("class A { let n: integer; } let o = new A(); o.m = 3;");
        falla("class A { let n: integer; } let o = new A(); o.n = \"x\";");
        falla("class B : NoExiste { }");
        falla("class A { function f(): integer { return 1; } } class B : A { function f(): string { return \"\"; } }");
        falla("class A { let n: integer; } let x: integer = new A();");
        falla("class A { } class B : A { } let b: B = new A();");
    }

    // ---------- try / catch ----------
    @Test void tryCatchCapturaYContinua() {
        assertEquals(List.of("division por cero", "fin"), correr(
            "try { let z = 10 / 0; print(z); } catch (e) { print(e); } print(\"fin\");"));
        assertEquals(List.of("0", "2"), correr(
            "for (let i = 0; i < 3; i = i + 1) { try { if (i == 1) { continue; } print(i); } catch (e) { break; } }"));
    }
    @Test void tryCatchDentroDeFuncionConRetorno() throws Exception {
        var r = ok(archivo("examples/tac/try_catch.cps"));
        var m = new MaquinaTAC(r); m.ejecutar();
        assertEquals("division por cero", m.salida.get(0));
        assertEquals(List.of("0", "2"), m.salida.subList(1, 3));
    }
    @Test void excepcionDesdeFuncionLlamadaLaCapturaElLlamador() {
        assertEquals(List.of("division por cero", "ok"), correr(
            "function f(a: integer): integer { return 1 / a; } try { let r = f(0); print(r); } catch (e) { print(e); } print(\"ok\");"));
    }

    // ---------- reciclaje de temporales ----------
    @Test void reciclaTemporalesEnExpresionesLargas() {
        var r = ok("let a = 1; let b = 2; let c = 3; let d = (a + b) * (c - a) + (b * c) / (a + 1) - (c + b);");
        assertTrue(r.temporales().reutilizados() > 0);
        assertTrue(r.temporales().distintos() <= 4, "Se esperaban pocos temporales: " + r.temporales());
        assertTrue(r.temporales().distintos() < r.temporales().solicitudes());
    }
    @Test void temporalNoSeReutilizaMientrasEstaVivo() {
        var tac = ok("let a = 1; let b = (a + 1) * (a + 2);").codigoTAC();
        assertEquals("t0 = a + 1\nt1 = a + 2\nt2 = t0 * t1\nb = t2\n".replace("\n", System.lineSeparator()), tac.substring(tac.indexOf("t0 =")));
    }

    // ---------- tabla de símbolos con direcciones ----------
    @Test void simbolosRecibenAlmacenamiento() {
        var r = ok("let g = 1; function f(p: integer): integer { let l = p + g; return l; }"
                + " class C { let x: integer; function m(): integer { return this.x; } } let o = new C();");
        var global = r.analisis().ambitoGlobal();
        var g = global.buscarLocal("g").orElseThrow().almacenamiento();
        assertEquals(semantic.Simbolo.Almacenamiento.Clase.GLOBAL, g.clase());
        var f = global.buscarLocal("f").orElseThrow().almacenamiento();
        assertEquals("f", f.etiqueta()); assertEquals(r.registros().get("f").cantidadSlots(), f.tamano());
        var c = global.buscarLocal("C").orElseThrow();
        assertEquals(2, c.almacenamiento().tamano()); // cabecera + un campo
        assertEquals(1, c.miembros().buscarLocal("x").orElseThrow().almacenamiento().offset());
        assertEquals("C.m", c.miembros().buscarLocal("m").orElseThrow().almacenamiento().etiqueta());
        assertTrue(r.enlaces().stream().anyMatch(e -> e.operando().startsWith("%f.") && e.offset() != null));
    }

    // ---------- recuperación de errores ----------
    @Test void reportaVariosErroresEnUnaEjecucionSinRepetirlos() throws Exception {
        var r = Compilador.compilar(archivo("examples/errores/varios_errores.cps"));
        assertTrue(r.errores().size() >= 5, r.errores().toString());
        assertEquals(r.errores().size(), new HashSet<>(r.errores()).size());
        var lineas = r.errores().stream().map(Diagnostico::linea).toList();
        assertEquals(lineas.stream().sorted().toList(), lineas);
        assertEquals("", r.codigoTAC());
    }
    @Test void erroresSintacticosMultiplesYLexicoSinDerivados() throws Exception {
        var r = Compilador.compilar(archivo("examples/errores/sintaxis.cps"));
        assertTrue(r.errores().size() >= 3);
        assertEquals(1, r.errores().stream().filter(e -> e.linea() == 3).count(), "Sin errores derivados del símbolo ilegal");
        assertEquals("", r.codigoTAC());
    }
    @Test void unErrorNoProvocaCascadaDeMensajes() {
        assertEquals(1, Compilador.compilar("let y = z + 1 * 2;").errores().size());
        assertEquals(1, Compilador.compilar("let y: integer = (z + 1) * 3;").errores().size());
    }

    // ---------- ejemplos del repositorio ----------
    @Test void todosLosEjemplosDeTacCompilan() throws Exception {
        try (var rutas = Files.list(Path.of("examples/tac"))) {
            for (var ruta : rutas.filter(p -> p.toString().endsWith(".cps")).toList()) {
                var r = Compilador.compilar(Files.readString(ruta));
                assertTrue(r.esValido(), ruta + " " + r.errores());
                assertFalse(r.instrucciones().isEmpty(), ruta.toString());
            }
        }
    }
    @Test void todosLosEjemplosDeErroresFallan() throws Exception {
        try (var rutas = Files.list(Path.of("examples/errores"))) {
            for (var ruta : rutas.filter(p -> p.toString().endsWith(".cps")).toList())
                assertFalse(Compilador.compilar(Files.readString(ruta)).esValido(), ruta.toString());
        }
    }
}
