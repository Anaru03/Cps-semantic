# API e integración con Persona 3

## Punto de entrada

```java
ResultadoCompilacion resultado = Compilador.compilar(codigo);
if (!resultado.esValido()) {
    // Mostrar resultado.errores(): etapa, línea, columna y descripción.
} else {
    // Mostrar resultado.codigoTAC(), registros(), funciones() y enlaces().
}
```

Paquete `compiler`. Cada llamada crea un compilador independiente. Los resultados
de instrucciones, firmas, layouts y enlaces son snapshots inmutables; el árbol
y ámbitos semánticos se comparten para lectura y visualización.

El lexer y parser se ejecutan una vez. Su árbol se reutiliza en semántica y TAC.
Los errores de cualquier etapa bloquean la salida de instrucciones y metadatos
TAC; el árbol se conserva y el análisis semántico está disponible si se alcanzó
esa etapa. `analisis()` es null ante errores léxicos/sintácticos. Un programa
vacío válido puede tener TAC vacío: consultar `esValido()`, no la longitud del texto.

## Identidad de símbolos y posiciones

`AnalisisSemantico.informacion()` contiene mapas de nodo a tipo, ámbito y
referencia. `InformacionSemantica.Referencia` combina **instancia de ámbito +
nombre**, evitando confundir declaraciones homónimas. `referencia.simbolo()`
consulta el símbolo de esa declaración en su ámbito.

`ResultadoCompilacion.enlaces()` contiene `EnlaceSimboloTAC`:

| Campo | Uso |
|---|---|
| `referencia` | Declaración semántica exacta |
| `operando` | Nombre usado en las instrucciones TAC |
| `funcion` | Propietaria del marco; null en almacenamiento global |
| `offset` | Slot lógico del registro; null en almacenamiento global |

Los globales de raíz mantienen su nombre. Variables de bloques del programa
principal usan `%global.numero.nombre`; locales y parámetros usan
`%funcion.numero.nombre`. Para integrar metadatos en `Simbolo`/`Ambito`, usar
la referencia y el operando, nunca buscar solamente el nombre fuente.
Los temporales aparecen en los layouts, sin inventar declaraciones semánticas.

Los tipos de variables inferidas provienen del análisis semántico. Un temporal
reciclado que guarda tipos distintos tiene tipo `dynamic`: ocupa un slot genérico
de valor/referencia. Un backend deberá aceptar esa convención o separar slots
por tipo. Al usar los visitors directamente sin información semántica, algunos
tipos seguirán siendo `unknown`; para la entrega, usar la API `Compilador`.

## Responsabilidades que siguen pendientes

- Persona 3: incorporar estos datos en su tabla de símbolos y vistas del IDE;
  generación de arreglos, clases, atributos, constructores, `this` y métodos.
- Persona 3: respetar `LONGITUD_ARREGLO` y `LECTURA_ARREGLO` al integrar arreglos.
  `foreach` ya usa ese contrato: índices desde cero, longitud capturada al entrar
  y copia de referencia al asignar un operando de tipo arreglo. Ver `TAC.md`.
- Persona 2: diagrama del árbol implementado con zoom, búsqueda y plegado;
  instrucciones de uso en `DIAGRAMA_ARBOL.md`.
- Definir traducción de `print`, `try/catch` y ternario antes de ampliar el alcance;
  la API reporta diagnóstico TAC cuando todavía no están implementados.
- Los operadores `&&` y `||` mantienen la evaluación binaria de la base existente;
  si el lenguaje exige cortocircuito, debe acordarse e implementarse explícitamente.

La detección de soporte está en `Compilador.comprobarSoporte`. Al agregar una
construcción, implementar primero su traducción y tests, y después retirar su
diagnóstico de pendiente. No quitarlo para producir salida incompleta.

La interfaz Swing todavía usa el análisis anterior: la salida TAC se integra
consumiendo esta API. Este bloque deja el contrato listo y no agrega esa vista.

## Demostración y verificación

```bash
mvn compile
java -cp "target/classes:$HOME/.m2/repository/org/antlr/antlr4-runtime/4.13.2/antlr4-runtime-4.13.2.jar" compiler.CompilarArchivo examples/tac/recursion_locales.cps
mvn test
```

La CLI imprime TAC y devuelve código de salida 1 si hay errores. La ejecución
de las instrucciones se verifica mediante un intérprete limitado de tests, que
usa `PilaActivaciones`; no se ofrece todavía un intérprete de usuario en el IDE.
Los ejemplos de `examples/tac` se compilan mediante la API en los tests integrados.
