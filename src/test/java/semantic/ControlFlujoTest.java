package semantic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ControlFlujoTest {
    private AnalisisSemantico analizar(String codigo) { return AnalizadorSemantico.analizar(codigo); }
    private void valido(String codigo) { assertTrue(analizar(codigo).resultado().esValido(), codigo); }
    private void invalido(String codigo, String fragmento) {
        var r = analizar(codigo).resultado();
        assertTrue(r.errores().stream().anyMatch(e -> e.descripcion().contains(fragmento)), r.errores().toString());
    }

    // ---- if ----
    @Test void ifConCondicionBooleanaEsValido() { valido("if (true) { print(1); }"); }
    @Test void ifConCondicionNoBooleanaEsInvalido() { invalido("if (1) { print(1); }", "condicion del if"); }
    @Test void ifElseSeVisitanAmbasRamas() { invalido("if (true) { print(1); } else { print(fantasma); }", "no esta declarado"); }

    // ---- while ----
    @Test void whileConCondicionBooleanaEsValido() { valido("while (false) { print(1); }"); }
    @Test void whileConCondicionNoBooleanaEsInvalido() { invalido("while (1) { print(1); }", "condicion del while"); }

    // ---- do-while ----
    @Test void doWhileConCondicionBooleanaEsValido() { valido("do { print(1); } while (true);"); }
    @Test void doWhileConCondicionNoBooleanaEsInvalido() { invalido("do { print(1); } while (1);", "condicion del do-while"); }

    // ---- for ----
    @Test void forConCondicionBooleanaEsValido() { valido("for (let i = 0; i < 10; i = i + 1) { print(i); }"); }
    @Test void forConCondicionNoBooleanaEsInvalido() { invalido("for (let i = 0; i; i = i + 1) { print(i); }", "condicion del for"); }
    @Test void forSinInicializacionNiIncrementoEsValido() { valido("for (; true; ) { print(1); }"); }
    @Test void variableDelForEstaLimitadaAlBucle() { invalido("for (let i = 0; i < 1; i = i + 1) {} print(i);", "no esta declarado"); }

    // ---- foreach ----
    @Test void foreachSobreArregloInfiereElTipoDeElemento() {
        valido("let xs: integer[] = [1, 2, 3]; foreach (x in xs) { let y: integer = x; }");
    }
    @Test void foreachSobreNoArregloEsInvalido() { invalido("foreach (x in 5) { print(x); }", "foreach requiere"); }

    // ---- break / continue ----
    @Test void breakDentroDeUnBucleEsValido() { valido("while (true) { break; }"); }
    @Test void breakFueraDeUnBucleEsInvalido() { invalido("break;", "'break' solo puede utilizarse"); }
    @Test void continueDentroDeUnBucleEsValido() { valido("while (true) { continue; }"); }
    @Test void continueFueraDeUnBucleEsInvalido() { invalido("continue;", "'continue' solo puede utilizarse"); }
    @Test void breakDentroDeUnaFuncionFueraDeBucleEsInvalido() {
        invalido("function f() { break; }", "'break' solo puede utilizarse");
    }

    // ---- switch ----
    @Test void switchConCasosDelMismoTipoEsValido() {
        valido("let x = 1; switch (x) { case 1: print(1); case 2: print(2); default: print(0); }");
    }
    @Test void switchConCaseDeTipoDistintoEsInvalido() {
        invalido("let x = 1; switch (x) { case \"a\": print(1); }", "no coincide con el tipo del switch");
    }

    // ---- try/catch ----
    @Test void tryCatchDeclaraLaVariableDeCatchEnSuAmbito() {
        valido("try { print(1); } catch (e) { print(e); }");
    }

    // ---- codigo muerto ----
    @Test void instruccionDespuesDeReturnEsCodigoMuerto() {
        invalido("function f(): integer { return 1; print(2); }", "Codigo muerto");
    }
    @Test void instruccionDespuesDeBreakEsCodigoMuerto() {
        invalido("while (true) { break; print(1); }", "Codigo muerto");
    }
    @Test void instruccionDespuesDeContinueEsCodigoMuerto() {
        invalido("while (true) { continue; print(1); }", "Codigo muerto");
    }
    @Test void codigoSinReturnPrevioNoSeMarcaComoMuerto() {
        valido("function f(): integer { print(1); return 1; }");
    }
}
