package semantic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A diferencia de OperacionesAritmeticasTest / OperacionesLogicasTest / ComparacionesTest
 * (que prueban TipoVisitor de forma aislada, sin tabla de simbolos), estas pruebas pasan
 * por el punto de entrada real, AnalizadorSemantico.analizar, para confirmar que las
 * validaciones de operadores tambien se aplican dentro de programas completos.
 */
class OperadoresTest {
    private AnalisisSemantico analizar(String codigo) { return AnalizadorSemantico.analizar(codigo); }
    private void valido(String codigo) { assertTrue(analizar(codigo).resultado().esValido(), codigo); }
    private void invalido(String codigo, String fragmento) {
        var r = analizar(codigo).resultado();
        assertTrue(r.errores().stream().anyMatch(e -> e.descripcion().contains(fragmento)), r.errores().toString());
    }

    // ---- aritmeticas ----
    @Test void sumaDeVariablesEnterasEsValida() { valido("let a = 1; let b = 2; let c = a + b;"); }
    @Test void sumaDeBooleanConEnteroEsInvalida() { invalido("let a = true; let b = a + 1;", "'+'/'-'"); }
    @Test void multiplicacionDeStringsEsInvalida() { invalido("let a = \"x\" * \"y\";", "'*'/'/'/'%'"); }
    @Test void negacionAritmeticaDeUnBooleanoEsInvalida() { invalido("let a = -true;", "unario requiere"); }

    // ---- float ----
    @Test void sumaDeEnteroYFloatEsValidaYPromocionaAFloat() {
        var a = analizar("let a: float = 1 + 2.5;");
        assertTrue(a.resultado().esValido(), a.resultado().errores().toString());
    }
    @Test void comparacionRelacionalEntreEnteroYFloatEsValida() { valido("let a = 1 < 2.5;"); }
    @Test void sumaDeFloatConBooleanEsInvalida() { invalido("let a = 2.5 + true;", "'+'/'-'"); }

    // ---- logicas ----
    @Test void andEntreBooleanosEsValido() { valido("let a = true && false;"); }
    @Test void orConUnEnteroEsInvalido() { invalido("let a = true || 1;", "'||'"); }
    @Test void negacionLogicaDeUnEnteroEsInvalida() { invalido("let a = !5;", "'!' requiere"); }

    // ---- comparaciones ----
    @Test void comparacionRelacionalEntreEnterosEsValida() { valido("let a = 1 < 2;"); }
    @Test void comparacionRelacionalEntreBooleanosEsInvalida() { invalido("let a = true < false;", "comparacion relacional"); }
    @Test void igualdadEntreTiposIncompatiblesEsInvalida() { invalido("let a = 1 == \"x\";", "No se puede comparar"); }

    // ---- ternario ----
    @Test void ternarioConCondicionBooleanaYRamasCompatiblesEsValido() {
        valido("let a: integer = true ? 1 : 2;");
    }
    @Test void ternarioConCondicionNoBooleanaEsInvalido() {
        invalido("let a = 1 ? 1 : 2;", "operador ternario debe ser boolean");
    }
    @Test void ternarioConRamasDeTipoDistintoEsInvalido() {
        invalido("let a = true ? 1 : \"x\";", "mismo tipo");
    }

    // ---- expresiones sin sentido semantico (no multiplicar funciones) ----
    @Test void usarUnaFuncionSinInvocarlaEnUnaOperacionEsInvalido() {
        invalido("function f(): integer { return 1; } let a = f * 2;", "sin invocarlo");
    }
    @Test void usarUnaClaseSinInstanciarlaEsInvalido() {
        invalido("class A {} let a = A;", "sin invocarlo");
    }
    @Test void llamarUnaFuncionSiSeInvocaCorrectamenteEsValido() {
        valido("function f(): integer { return 1; } let a = f() + 1;");
    }
}
