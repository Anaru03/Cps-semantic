package mips;

import compiler.Compilador;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class EstructurasMIPSTest {
    private static String asm(String fuente) {
        var r = Compilador.compilar(fuente);
        assertTrue(r.esValido(), r.errores().toString());
        return TraductorTACProvisional.traducir(r);
    }
    private static void salida(String fuente, String esperada) throws Exception {
        String original = asm(fuente);
        assertEquals(esperada, MarsTestSupport.ejecutar(original));
        assertEquals(esperada, MarsTestSupport.ejecutar(new OptimizadorMirilla().optimizar(original).codigo()));
    }

    @Test void arreglosMultidimensionalesYForeach() throws Exception {
        salida(Files.readString(Path.of("examples/tac/arreglos.cps")), "13\n");
    }
    @Test void herenciaInicializadoresConstructorYDespachoVirtual() throws Exception {
        salida(Files.readString(Path.of("examples/tac/clases_herencia.cps")) + "\nprint(s); print(a.nombre);\n",
                "division por cero\n5\nguau\nrex\n");
    }
    @Test void tryCatchConReturnYContinue() throws Exception {
        salida(Files.readString(Path.of("examples/tac/try_catch.cps")), "division por cero\n0\n2\n");
    }
    @Test void indicesInvalidosEnLecturaYEscritura() throws Exception {
        salida("""
                let xs: integer[] = [4,5];
                try { print(xs[-1]); } catch (e) { print(e); }
                try { xs[2] = 9; } catch (e) { print(e); }
                print(xs[1]);
                """, "indice fuera de rango\nindice fuera de rango\n5\n");
    }
    @Test void propagacionEntreFuncionesRecursivasYLocalesDelCatch() throws Exception {
        salida("""
                let visitas: integer = 0;
                function fallar(n: integer): integer {
                    visitas = visitas + 1;
                    if (n == 0) { return 10 / n; }
                    return fallar(n-1);
                }
                function probar(): integer {
                    let local: integer = 37;
                    try { print(fallar(4)); } catch (e) { print(e); local = local + 5; }
                    return local;
                }
                print(probar()); print(visitas); print(probar());
                """, "division por cero\n42\n5\ndivision por cero\n42\n");
    }
    @Test void catchAnidadoPropagaAlManejadorExterior() throws Exception {
        salida("""
                try {
                    try { print(4 / 0); }
                    catch (interno) { print(interno); print(5 % 0); }
                } catch (externo) { print(externo); }
                print(9);
                """, "division por cero\ndivision por cero\n9\n");
    }
    @Test void breakYReturnDesinstalanManejadores() throws Exception {
        salida("""
                function temprano(): integer {
                    try { return 7; } catch (e) { return 0; }
                }
                print(temprano());
                while (true) {
                    try { break; } catch (e) { print(99); }
                }
                try { print(1 / 0); } catch (e) { print(e); }
                """, "7\ndivision por cero\n");
    }
    @Test void arregloVacioYCamposSinInicializador() throws Exception {
        salida("""
                class Caja { let valor: integer; }
                let c: Caja = new Caja();
                print(c.valor); c.valor = 12; print(c.valor);
                let xs: integer[] = [];
                print(xs.length);
                try { print(xs[0]); } catch (e) { print(e); }
                """, "0\n12\n0\nindice fuera de rango\n");
    }
    @Test void referenciasNulasSeAtrapan() throws Exception {
        salida("""
                class Caja {
                    let valor: integer;
                    function obtener(): integer { return this.valor; }
                }
                let c: Caja = null;
                let xs: integer[] = null;
                try { print(c.valor); } catch (e) { print(e); }
                try { c.valor = 4; } catch (e) { print(e); }
                try { print(c.obtener()); } catch (e) { print(e); }
                try { print(xs.length); } catch (e) { print(e); }
                try { print(xs[0]); } catch (e) { print(e); }
                """, "referencia nula\nreferencia nula\nreferencia nula\nreferencia nula\nreferencia nula\n");
    }
    @Test void muchosTryEnCiclosNoAcumulanMarcos() throws Exception {
        salida("""
                let i: integer = 0;
                let contador: integer = 0;
                while (i < 1500) {
                    try { contador = contador + 1; }
                    catch (e) { print(e); }
                    i = i + 1;
                }
                print(contador);
                """, "1500\n");
    }

    @Test void excepcionSinCatchTerminaConErrorExplicito() throws Exception {
        String original = asm("print(9 / 0); print(99);");
        for (String codigo : new String[]{original, new OptimizadorMirilla().optimizar(original).codigo()}) {
            var r = MarsTestSupport.ejecutarResultado(codigo);
            assertEquals(1, r.codigo());
            assertEquals("division por cero\n", r.salida());
        }
    }
    @Test void vcallConArgumentosEnPilaYCampoDeReferencia() throws Exception {
        salida("""
                class Base {
                    function sumar(a: integer, b: integer, c: integer, d: integer): integer { return 0; }
                }
                class Hija : Base {
                    let extra: integer = 10;
                    function sumar(a: integer, b: integer, c: integer, d: integer): integer {
                        return a + b + c + d + this.extra;
                    }
                }
                class Contenedor { let objeto: Base; }
                let cont: Contenedor = new Contenedor();
                cont.objeto = new Hija();
                print(cont.objeto.sumar(1,2,3,4));
                """, "20\n");
    }

}
