# Compiscript Semantic Analyzer

<p align="center">
  <img src="https://img.shields.io/badge/Java-17%2B-orange?style=for-the-badge&logo=openjdk&logoColor=white" />
  <img src="https://img.shields.io/badge/ANTLR-4.13.2-red?style=for-the-badge" />
  <img src="https://img.shields.io/badge/Maven-3.9%2B-blue?style=for-the-badge&logo=apachemaven&logoColor=white" />
  <img src="https://img.shields.io/badge/JUnit-5-green?style=for-the-badge&logo=junit5&logoColor=white" />
  <img src="https://img.shields.io/badge/status-terminado-brightgreen?style=for-the-badge" />
</p>

<p align="center">
  <b>Analizador léxico, sintáctico y semántico para el lenguaje Compiscript.</b><br/>
  Construido con ANTLR, Java y Maven, con sistema de tipos, tabla de símbolos e interfaz gráfica.
</p>

---

## ¿Qué es?

**Compiscript Semantic Analyzer** es un proyecto académico desarrollado para el curso de **Construcción de Compiladores** de la Universidad del Valle de Guatemala.

El proyecto analiza programas escritos en Compiscript mediante tres fases principales: análisis léxico, análisis sintáctico y análisis semántico.

ANTLR genera el Lexer y Parser a partir de la gramática del lenguaje. A partir del árbol sintáctico generado, el analizador semántico valida tipos, expresiones, declaraciones, ámbitos, funciones, clases, estructuras de control y otras reglas del lenguaje.

Los resultados pueden visualizarse desde un IDE de escritorio que muestra los errores encontrados, el árbol sintáctico y la tabla de símbolos.

---

## Funcionalidades

El analizador implementa:

### Análisis léxico y sintáctico

- Lexer generado mediante ANTLR.
- Parser generado mediante ANTLR.
- Construcción del árbol sintáctico.
- Recorrido del árbol mediante Visitor.
- Integración del árbol sintáctico con el análisis semántico.

### Sistema de tipos

- Tipos `integer`, `float`, `string`, `boolean` y `null`.
- Tipos de arreglos.
- Tipos definidos mediante clases.
- Inferencia y determinación del tipo de expresiones.
- Promoción numérica entre `integer` y `float`.

### Expresiones y operadores

- Operaciones aritméticas: `+`, `-`, `*`, `/`.
- Operaciones lógicas: `&&`, `||`, `!`.
- Comparaciones: `==`, `!=`, `<`, `<=`, `>`, `>=`.
- Operador ternario.
- Validación de compatibilidad entre operandos.
- Determinación del tipo resultante de las expresiones.

### Variables y constantes

- Declaración de variables.
- Declaración de constantes.
- Compatibilidad entre el tipo declarado y el valor asignado.
- Validación de inicializaciones.
- Detección de declaraciones duplicadas.

### Arreglos

- Validación del tipo de los elementos.
- Arreglos tipados.
- Acceso a elementos.
- Validación del tipo utilizado como índice.

### Funciones

- Declaración de funciones.
- Parámetros.
- Tipos de retorno.
- Validación de llamadas.
- Validación de cantidad y tipos de argumentos.
- Recursión.
- Closures.
- Detección del uso de funciones como valores cuando corresponde una invocación.

### Clases y objetos

- Declaración de clases.
- Atributos.
- Métodos.
- Constructores.
- Instanciación de objetos.
- Acceso a miembros.
- Validación de llamadas a métodos.
- Uso contextual de `this`.

### Control de flujo

- `if` / `else`.
- `while`.
- `do-while`.
- `for`.
- `foreach`.
- `switch` / `case`.
- `break`.
- `continue`.
- `try` / `catch`.
- Validación de condiciones booleanas.
- Manejo de ámbitos dentro de estructuras de control.
- Detección de código muerto después de `return`, `break` o `continue`.

### Tabla de símbolos

La tabla de símbolos permite:

- Insertar símbolos.
- Recuperar información.
- Actualizar símbolos existentes.
- Resolver identificadores.
- Manejar ámbitos anidados.

Se registran símbolos correspondientes a:

- Variables.
- Constantes.
- Parámetros.
- Funciones.
- Clases.
- Atributos.
- Métodos.

El analizador maneja ámbitos globales, de función, de clase y de bloque.

### Manejo de errores

El análisis semántico continúa después de encontrar errores con el objetivo de reportar múltiples problemas en una misma ejecución.

Los errores muestran información como:

- Línea.
- Columna.
- Descripción del problema.

Por ejemplo:

```text
1:0 - No se puede inicializar integer con string
3:0 - No se puede inicializar boolean con integer
7:0 - La condición del if debe ser boolean, se obtuvo integer
11:0 - 'break' solo puede utilizarse dentro de un bucle
```

---

## Ejemplos

### Expresiones válidas

```cps
10 + 5
20 >= 10
true && false
"hola" == "mundo"
```

Resultados:

```text
10 + 5          -> INTEGER
20 >= 10        -> BOOLEAN
true && false   -> BOOLEAN
"hola" == "mundo" -> BOOLEAN
```

### Expresiones incompatibles

```cps
true + 5
10 * false
10 < "hola"
true || 10
```

Estas expresiones son reconocidas sintácticamente, pero producen errores durante el análisis semántico debido a incompatibilidades de tipos.

### Programa válido

```cps
class Persona {
    let edad: integer;

    function constructor(e: integer) {
        this.edad = e;
    }

    function esMayorDeEdad(): boolean {
        return this.edad >= 18;
    }
}

function sumar(a: integer, b: integer): integer {
    return a + b;
}

let p = new Persona(20);
let resultado: integer = sumar(2, 3);
let xs: integer[] = [1, 2, 3];
let promedio: float = 1 + 2.5;

if (p.esMayorDeEdad()) {
    print(resultado);
}

for (let i = 0; i < 3; i = i + 1) {
    print(xs[i]);
}
```

---

## Sistema de tipos

| Tipo | Uso |
|---|---|
| `INTEGER` | Valores enteros |
| `FLOAT` | Valores de punto flotante |
| `STRING` | Cadenas de texto |
| `BOOLEAN` | Valores lógicos |
| `NULL` | Valor nulo |
| `ARRAY` | Arreglos |
| `CLASS` | Tipos definidos mediante clases |
| `VOID` | Ausencia de valor |
| `UNKNOWN` | Tipo todavía no determinado |
| `ERROR` | Expresión semánticamente inválida |

---

## Cómo funciona

El proceso general de análisis es:

```text
Código Compiscript
        |
        v
   Lexer - ANTLR
        |
        v
   Parser - ANTLR
        |
        v
Árbol sintáctico
        |
        v
Analizador semántico
        |
   +----+----+
   |         |
   v         v
Sistema    Tabla de
de tipos   símbolos
   |         |
   +----+----+
        |
        v
Errores y resultados
        |
        v
       IDE
```

El Lexer transforma el código fuente en tokens y el Parser utiliza esos tokens para construir el árbol sintáctico.

El analizador semántico recorre el árbol y utiliza el sistema de tipos y la tabla de símbolos para comprobar las reglas semánticas del lenguaje.

El resultado del análisis contiene los errores encontrados y la información de los símbolos registrados durante el procesamiento.

El punto de entrada del análisis completo es:

```java
AnalisisSemantico analisis = AnalizadorSemantico.analizar(codigo);

ResultadoSemantico resultado = analisis.resultado();

Ambito tablaGlobal = analisis.ambitoGlobal();
```

---

## Requisitos

Para compilar y ejecutar el proyecto se necesita:

- Java 17 o superior.
- Maven 3.9 o superior.

Puedes verificar las instalaciones con:

```bash
java -version
mvn -version
```

---

## Instalación

Clona el repositorio:

```bash
git clone https://github.com/Anaru03/Cps-semantic.git
cd Cps-semantic
```

Compila el proyecto:

```bash
mvn clean compile
```

Maven genera automáticamente las clases necesarias de ANTLR y posteriormente compila el código Java.

Una compilación correcta debe finalizar con:

```text
BUILD SUCCESS
```

---

## IDE

El proyecto incluye una interfaz gráfica de escritorio desarrollada con **Java Swing**.

Para ejecutarla:

```bash
mvn compile exec:java
```

La interfaz contiene:

- Editor de código Compiscript.
- Botón **Compilar**.
- Atajo de compilación `Ctrl + Enter`.
- Pestaña **Errores**.
- Pestaña **Árbol sintáctico**.
- Pestaña **Tabla de símbolos**.

Al ejecutar el análisis, los resultados se muestran directamente dentro de la interfaz.

### Errores

La pestaña **Errores** muestra los problemas semánticos encontrados junto con su línea, columna y descripción.

Cuando el programa es correcto se muestra:

```text
Compilación exitosa: no se encontraron errores semánticos.
```

### Árbol sintáctico

La pestaña **Árbol sintáctico** muestra una representación navegable del árbol generado por el Parser de ANTLR.

Permite visualizar nodos correspondientes a declaraciones, expresiones, funciones, clases y estructuras de control.

### Tabla de símbolos

La pestaña **Tabla de símbolos** permite visualizar los símbolos registrados y sus respectivos ámbitos.

Por ejemplo:

```text
global
|
+-- CLASE Persona : Persona
|   |
|   +-- ATRIBUTO edad : integer
|   +-- METODO constructor : void
|   +-- METODO esMayorDeEdad : boolean
|
+-- FUNCION sumar : integer
|   |
|   +-- PARAMETRO a : integer
|   +-- PARAMETRO b : integer
|
+-- VARIABLE p : Persona
+-- VARIABLE resultado : integer
+-- VARIABLE xs : integer[]
+-- VARIABLE promedio : float
```

---

## Pruebas automatizadas

Para ejecutar toda la batería de pruebas:

```bash
mvn clean test
```

Actualmente el proyecto cuenta con:

```text
133 tests
0 fallos
0 errores
```

Una ejecución correcta finaliza con:

```text
Tests run: 133, Failures: 0, Errors: 0, Skipped: 0

BUILD SUCCESS
```

Las pruebas cubren:

- Sistema de tipos.
- Literales.
- Operaciones aritméticas.
- Operaciones lógicas.
- Comparaciones.
- Operadores.
- Asignaciones.
- Constantes.
- Arreglos e índices.
- Funciones y llamadas.
- Clases y objetos.
- Tabla de símbolos.
- Manejo de ámbitos.
- Control de flujo.
- Resultados y errores semánticos.
- Integración del analizador semántico.

### Ejecutar pruebas específicas

Sistema de tipos:

```bash
mvn -Dtest=TipoTest test
```

Inferencia de tipos:

```bash
mvn -Dtest=TipoVisitorTest test
```

Operaciones aritméticas:

```bash
mvn -Dtest=OperacionesAritmeticasTest test
```

Operaciones lógicas:

```bash
mvn -Dtest=OperacionesLogicasTest test
```

Comparaciones:

```bash
mvn -Dtest=ComparacionesTest test
```

Asignaciones:

```bash
mvn -Dtest=AsignacionesTest test
```

Constantes:

```bash
mvn -Dtest=ConstantesTest test
```

Arreglos:

```bash
mvn -Dtest=ArreglosTest test
```

Analizador semántico:

```bash
mvn -Dtest=AnalizadorSemanticoTest test
```

Operadores integrados:

```bash
mvn -Dtest=OperadoresTest test
```

Control de flujo:

```bash
mvn -Dtest=ControlFlujoTest test
```

Tabla de símbolos:

```bash
mvn -Dtest=TablaSimbolosTest test
```

---

## Documentación

La documentación de arquitectura y de los componentes principales del proyecto se encuentra en:

[`docs/ARQUITECTURA.md`](docs/ARQUITECTURA.md)

---

## Estado del proyecto

El proyecto se encuentra **terminado**.

Se implementaron los componentes necesarios para realizar el análisis léxico, sintáctico y semántico de programas escritos en Compiscript utilizando ANTLR.

El proyecto incluye sistema de tipos, validación de reglas semánticas, manejo de ámbitos, funciones, clases, estructuras de control, tabla de símbolos e interfaz gráfica.

La batería automatizada cuenta actualmente con **133 pruebas ejecutadas sin fallos ni errores**.

---

## Integrantes

- **Ruth de León** — [Anaru03](https://github.com/Anaru03)
- **Alejandro Antón** — [Anton17303](https://github.com/Anton17303)
- **Jorge López** — [Jorge162017](https://github.com/Jorge162017)

---

<p align="center">
  <b>Universidad del Valle de Guatemala</b><br/>
  Construcción de Compiladores
</p>
