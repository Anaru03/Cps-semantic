package semantic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnalizadorSemanticoTest {
    private AnalisisSemantico analizar(String codigo) { return AnalizadorSemantico.analizar(codigo); }
    private void valido(String codigo) { assertTrue(analizar(codigo).resultado().esValido(), codigo); }
    private void invalido(String codigo, String fragmento) {
        var r = analizar(codigo).resultado();
        assertTrue(r.errores().stream().anyMatch(e -> e.descripcion().contains(fragmento)), r.errores().toString());
    }

    @Test void registraVariablesEnLaTabla() {
        var a = analizar("let edad: integer = 20;");
        assertTrue(a.resultado().esValido());
        assertEquals(TipoDato.INTEGER, a.ambitoGlobal().buscarLocal("edad").orElseThrow().tipo());
    }
    @Test void detectaVariableNoDeclarada() { invalido("print(fantasma);", "no esta declarado"); }
    @Test void detectaRedeclaracionLocal() { invalido("let x = 1; const x = 2;", "ya fue declarado"); }
    @Test void permiteSombreadoEnAmbitoAnidado() { valido("let x = 1; { let x = 2; print(x); }"); }
    @Test void resuelveVariableDelAmbitoPadre() { valido("let x = 1; { { print(x); } }"); }

    @Test void validaFuncionYArgumentos() {
        valido("function doble(x: integer): integer { return x; } let y = doble(2);");
        invalido("function f(x: integer) {} f();", "espera 1 argumentos");
        invalido("function f(x: integer) {} f(\"x\");", "argumento 1");
    }
    @Test void validaTipoDeRetorno() {
        invalido("function f(): integer { return \"no\"; }", "retorno debe ser integer");
        invalido("return 1;", "dentro de una funcion");
    }
    @Test void permiteRecursion() { valido("function cuenta(n: integer): integer { return cuenta(n); }"); }
    @Test void permiteFuncionesAnidadasYClosures() {
        valido("function externa(x: integer): integer { function interna(): integer { return x; } return interna(); }");
    }
    @Test void detectaFuncionesDuplicadas() { invalido("function f() {} function f() {}", "ya fue declarado"); }

    @Test void validaClasesAtributosMetodosYConstructor() {
        valido("class Persona { let edad: integer; function constructor(e: integer) { this.edad = e; } function leer(): integer { return this.edad; } } let p = new Persona(3); let e = p.leer();");
        invalido("class A {} let a = new A(); print(a.falta);", "no contiene 'falta'");
        invalido("class A { function constructor(x: integer) {} } let a = new A();", "espera 1 argumentos");
    }
    @Test void validaThis() { invalido("print(this);", "this solo"); }
    @Test void validaAsignacionesYConstantes() {
        invalido("let x: integer = 1; x = \"s\";", "No se puede asignar");
        invalido("const x = 1; x = 2;", "reasignar la constante");
    }
}
