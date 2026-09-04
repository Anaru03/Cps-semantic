# Arquitectura

## Vision general

```
Codigo fuente (.cps)
        |
        v
  CompiscriptLexer   (generado por ANTLR, a partir de Compiscript.g4)
        |
        v
  CompiscriptParser   (genera el arbol sintactico / ParseTree)
        |
        v
  AnalizadorSemantico   (visitor sobre el ParseTree)
        |
        v
  AnalisisSemantico { ResultadoSemantico, Ambito global }
```

`AnalizadorSemantico` es el unico punto de entrada del analisis (`AnalizadorSemantico.analizar(codigo)`).
Recorre el arbol una sola vez, en preorden, construyendo la tabla de simbolos y validando
las reglas semanticas al mismo tiempo que resuelve el tipo de cada expresion.

## Paquetes

- **`antlr`** (generado, no versionado a mano): `CompiscriptLexer`, `CompiscriptParser`,
  `CompiscriptBaseVisitor`. Se regeneran en cada `mvn compile` a partir de `Compiscript.g4`.
- **`semantic`**: todo el analizador semantico.
- **`ide`**: la interfaz grafica (`CompiscriptIDE`), que solo consume la API publica de `semantic`.

## Clases principales de `semantic`

| Clase | Responsabilidad |
|---|---|
| `AnalizadorSemantico` | Visitor principal. Un metodo `visitX` por cada construccion del lenguaje que necesita validacion (declaraciones, operadores, control de flujo, funciones, clases). |
| `Ambito` | Tabla de simbolos con forma de arbol (una instancia por cada bloque, funcion o clase). Operaciones: `declarar` (insertar), `buscarLocal`/`buscar` (recuperar, con o sin subir por los ambitos padre), `actualizar` (modificar el tipo de un simbolo ya existente). |
| `Simbolo` | Un identificador declarado: nombre, tipo (`TipoDato`), categoria (`CategoriaSimbolo`), ambito donde vive, y para funciones/clases: parametros y su propio `Ambito` de miembros. |
| `TipoDato` | Tipo semantico (record inmutable). Envuelve `Tipo` (el enum base) mas informacion adicional para `CLASS` (nombre de la clase) y `ARRAY` (tipo del elemento). `compatibleCon` decide si dos tipos son intercambiables. |
| `ResultadoSemantico` / `ErrorSemantico` | Lista inmutable de errores con linea/columna/descripcion. |
| `TipoVisitor` | Visitor auxiliar y mas antiguo, usado unicamente por los tests que evaluan expresiones sueltas sin tabla de simbolos (`OperacionesAritmeticasTest`, `OperacionesLogicasTest`, `ComparacionesTest`, `TipoVisitorTest`). No forma parte del pipeline principal. |

## Manejo de ambitos

Cada `Ambito` tiene un puntero a su `padre`. `AnalizadorSemantico` mantiene un campo `actual`
que apunta al ambito activo; entrar a un bloque, funcion, clase, `for`, `foreach`, `switch` o
`catch` crea un `Ambito` hijo nuevo (`actual = new Ambito(..., actual)`) y, al salir, se
restaura el anterior. `buscar` sube por la cadena de padres hasta encontrar el identificador
o llegar al ambito global; `buscarLocal` no sube (usado para detectar redeclaraciones).

## Sistema de tipos

`Tipo` es el enum base (`INTEGER`, `FLOAT`, `STRING`, `BOOLEAN`, `NULL`, `ARRAY`, `CLASS`,
`VOID`, `UNKNOWN`, `ERROR`). Las operaciones aritmeticas y relacionales aceptan `INTEGER` o
`FLOAT` (con promocion: si cualquiera de los dos operandos es `FLOAT`, el resultado es
`FLOAT`); las logicas exigen `BOOLEAN` en ambos lados. `ERROR` se propaga hacia arriba en el
arbol de expresiones para evitar cascadas de errores redundantes.

## Deteccion de codigo muerto

`visitarSentencias` recorre cada lista de sentencias (cuerpo de bloque, `case`, `default`) y,
una vez que encuentra un `return`, `break` o `continue`, marca cualquier instruccion posterior
en esa misma lista como inalcanzable.

## Como ejecutar

Ver `README.md` para compilar, correr las pruebas y levantar el IDE
(`mvn compile exec:java -Dexec.mainClass="ide.CompiscriptIDE"`).
