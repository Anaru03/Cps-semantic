package tac;

import compiler.Compilador;
import compiler.ResultadoCompilacion;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ForeachTACTest {
    private ResultadoCompilacion compilar(String fuente) {
        var resultado = Compilador.compilar(fuente);
        assertTrue(resultado.esValido(), resultado.errores().toString());
        return resultado;
    }
    /** Runtime de prueba: referencias List y slots de la pila real de activaciones. */
    private static class Maquina {
        final ResultadoCompilacion resultado;
        final Map<String, Integer> etiquetas = new HashMap<>(), funciones = new HashMap<>();
        final Map<String, Object> globales = new HashMap<>();
        final PilaActivaciones pila = new PilaActivaciones();
        int pasos;
        Maquina(ResultadoCompilacion resultado) {
            this.resultado = resultado;
            for (int i = 0; i < resultado.instrucciones().size(); i++) {
                var ins = resultado.instrucciones().get(i);
                if (ins.tipo() == InstruccionTAC.Tipo.ETIQUETA) assertNull(etiquetas.put(ins.resultado(), i));
                if (ins.tipo() == InstruccionTAC.Tipo.FUNCION) funciones.put(ins.resultado(), i + 1);
            }
            ejecutar(0);
        }
        boolean local(String nombre) {
            return pila.actual() != null && pila.actual().layout().posiciones().stream()
                    .anyMatch(p -> p.operando().equals(nombre));
        }
        Object leer(String nombre) {
            if (nombre.equals("true")) return true;
            if (nombre.equals("false")) return false;
            if (nombre.matches("-?\\d+")) return Integer.parseInt(nombre);
            Object valor = local(nombre) ? pila.actual().leer(nombre) : globales.get(nombre);
            return Objects.requireNonNull(valor, "Lectura sin valor: " + nombre);
        }
        void escribir(String nombre, Object valor) {
            if (local(nombre)) pila.actual().escribir(nombre, valor); else globales.put(nombre, valor);
        }
        Object invocar(String nombre, List<?> argumentos) {
            pila.entrar(resultado.registros().get(nombre), argumentos, -1, null);
            Object valor = ejecutar(funciones.get(nombre));
            assertEquals(0, pila.profundidad());
            return valor;
        }
        Object ejecutar(int pc) {
            var argumentos = new ArrayList<Object>();
            while (pc < resultado.instrucciones().size()) {
                assertTrue(++pasos < 10000, "TAC no termina");
                var ins = resultado.instrucciones().get(pc++);
                switch (ins.tipo()) {
                    case ETIQUETA, FUNCION, PARAMETRO -> { }
                    case FIN_FUNCION -> throw new AssertionError("Falta retorno");
                    case SALTO -> pc = etiquetas.get(ins.resultado());
                    case SALTO_CONDICIONAL -> { if ((Boolean) leer(ins.argumento1())) pc = etiquetas.get(ins.resultado()); }
                    case LONGITUD_ARREGLO -> escribir(ins.resultado(), ((List<?>) leer(ins.argumento1())).size());
                    case LECTURA_ARREGLO -> escribir(ins.resultado(), ((List<?>) leer(ins.argumento1())).get((Integer) leer(ins.argumento2())));
                    case ARGUMENTO -> argumentos.add(leer(ins.argumento1()));
                    case LLAMADA -> {
                        assertEquals(Integer.parseInt(ins.argumento2()), argumentos.size());
                        pila.entrar(resultado.registros().get(ins.argumento1()), List.copyOf(argumentos), pc, ins.resultado());
                        argumentos.clear(); pc = funciones.get(ins.argumento1());
                    }
                    case RETORNO -> {
                        Object valor = ins.argumento1() == null ? null : leer(ins.argumento1());
                        var regreso = pila.salir(valor);
                        if (regreso.direccion() == -1) return valor;
                        if (pila.actual() == null && regreso.destino() != null) globales.put(regreso.destino(), valor);
                        pc = regreso.direccion();
                    }
                    case OPERACION -> {
                        Object a = leer(ins.argumento1());
                        Object b = ins.argumento2() == null ? null : leer(ins.argumento2());
                        Object valor = switch (ins.operador()) {
                            case "" -> a; case "+" -> (Integer) a + (Integer) b;
                            case "<" -> (Integer) a < (Integer) b; case "==" -> a.equals(b);
                            default -> throw new AssertionError(ins.operador());
                        };
                        escribir(ins.resultado(), valor);
                    }
                }
            }
            return null;
        }
    }
    @Test void recorreArregloVacioUnoYVariosElementos() {
        var resultado = compilar("function sumar(lista: integer[]): integer { let suma = 0;"
                + " foreach (x in lista) { suma = suma + x; } return suma; }");
        var maquina = new Maquina(resultado);
        assertEquals(0, maquina.invocar("sumar", List.of(List.of())));
        assertEquals(7, maquina.invocar("sumar", List.of(List.of(7))));
        assertEquals(6, maquina.invocar("sumar", List.of(List.of(1, 2, 3))));
    }
    @Test void continueAvanzaYBreakSaleSinRecorrerElResto() {
        var resultado = compilar("function sumar(lista: integer[]): integer { let suma = 0;"
                + " foreach (x in lista) { if (x == 2) { continue; } if (x == 4) { break; }"
                + " suma = suma + x; } return suma; }");
        assertEquals(4, new Maquina(resultado).invocar("sumar", List.of(List.of(1, 2, 3, 4, 9))));
    }
    @Test void anidamientoMantieneIndicesYReferenciasIndependientes() {
        var resultado = compilar("function combinar(lista: integer[]): integer { let suma = 0;"
                + " foreach (x in lista) { foreach (y in lista) { if (y == 2) { break; }"
                + " suma = suma + x + y; } } return suma; }");
        assertEquals(5, new Maquina(resultado).invocar("combinar", List.of(List.of(1, 2))));
    }
    @Test void evaluaIterableUnaVezYReasignacionNoCambiaElArregloCapturado() {
        var resultado = compilar("let llamadas = 0; function elegir(a: integer[]): integer[] {"
                + " llamadas = llamadas + 1; return a; }"
                + " function sumar(a: integer[], b: integer[]): integer { let suma = 0;"
                + " foreach (x in elegir(a)) { a = b; suma = suma + x; } return suma; }");
        var maquina = new Maquina(resultado);
        assertEquals(6, maquina.invocar("sumar", List.of(List.of(1, 2, 3), List.of(99))));
        assertEquals(1, maquina.globales.get("llamadas"));
    }
    @Test void variableDeIteracionTieneTipoOffsetYSombraIndependientes() {
        var resultado = compilar("function f(lista: integer[]): integer { let x = 10;"
                + " foreach (x in lista) { x = x + 1; } return x; }");
        assertEquals(10, new Maquina(resultado).invocar("f", List.of(List.of(1, 2))));
        var xs = resultado.enlaces().stream().filter(e -> e.referencia().nombre().equals("x")).toList();
        assertEquals(2, xs.size()); assertNotEquals(xs.get(0).offset(), xs.get(1).offset());
        assertEquals("integer", xs.get(1).referencia().simbolo().tipo().toString());
        assertFalse(Compilador.compilar("function f(a: integer[]) { foreach (x in a) {} x = 1; }").esValido());
    }
    @Test void switchDentroDelForeachConservaLosDestinosDeBreakYContinue() {
        var resultado = compilar("function f(lista: integer[]): integer { let suma = 0;"
                + " foreach (x in lista) { switch (x) { case 1: continue;"
                + " case 2: suma = suma + 2; break; default: suma = suma + x; }"
                + " suma = suma + 10; } return suma; }");
        assertEquals(25, new Maquina(resultado).invocar("f", List.of(List.of(1, 2, 3))));
    }
    @Test void rechazaIterablesInvalidosYCreacionDeArreglosSiguePendiente() {
        assertFalse(Compilador.compilar("foreach (x in 3) {}").esValido());
        assertFalse(Compilador.compilar("let a = [1, 2]; foreach (x in a) {}").esValido());
    }
    @Test void instruccionesTienenContratoExplicitoEIndicesLogicos() {
        assertEquals("t0 = length a", InstruccionTAC.longitudArreglo("a", "t0").toString());
        assertEquals("x = a[i]", InstruccionTAC.lecturaArreglo("a", "i", "x").toString());
        assertThrows(NullPointerException.class, () -> InstruccionTAC.lecturaArreglo("a", null, "x"));
    }
}
