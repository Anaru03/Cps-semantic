# Compiscript Compiler

<p align="center">
  <img src="https://img.shields.io/badge/Java-17%2B-orange?style=for-the-badge&logo=openjdk&logoColor=white" />
  <img src="https://img.shields.io/badge/ANTLR-4.13.2-red?style=for-the-badge" />
  <img src="https://img.shields.io/badge/Maven-3.9%2B-blue?style=for-the-badge&logo=apachemaven&logoColor=white" />
  <img src="https://img.shields.io/badge/JUnit-5-green?style=for-the-badge&logo=junit5&logoColor=white" />
  <img src="https://img.shields.io/badge/TAC-Three--Address%20Code-blueviolet?style=for-the-badge" />
  <img src="https://img.shields.io/badge/status-finalizado-brightgreen?style=for-the-badge" />
</p>

<p align="center">
  <b>Compilador académico para el lenguaje Compiscript</b><br/>
  Análisis léxico, sintáctico y semántico con generación de código intermedio TAC.
</p>

---

## Descripción

**Compiscript Compiler** es un proyecto académico desarrollado para el curso de **Construcción de Compiladores** de la Universidad del Valle de Guatemala.

El proyecto implementa distintas etapas del proceso de compilación de programas escritos en Compiscript.

El **Proyecto 1** construyó la base del compilador mediante análisis léxico, sintáctico y semántico, sistema de tipos, tabla de símbolos y manejo de ámbitos.

El **Proyecto 2** extiende esta infraestructura con la generación de código intermedio utilizando **Three-Address Code (TAC)**, administración de temporales, control de flujo, funciones, registros de activación, arreglos, clases y estructuras necesarias para representar la ejecución del programa.

El flujo general es:

```text
Código Compiscript
        │
        ▼
      Lexer
        │
        ▼
      Parser
        │
        ▼
Árbol sintáctico
        │
        ▼
Análisis semántico
        │
        ▼
Tabla de símbolos
        │
        ▼
Generación de código intermedio
        │
        ▼
       TAC
```

---

## Características principales

El compilador actualmente incluye:

- Análisis léxico y sintáctico mediante ANTLR.
- Análisis semántico y sistema de tipos.
- Tabla de símbolos y manejo de ámbitos.
- Generación de código intermedio TAC.
- Creación, liberación y reutilización de variables temporales.
- Expresiones aritméticas, lógicas, relacionales y de igualdad.
- Precedencia de operadores y expresiones entre paréntesis.
- Declaraciones y asignaciones.
- Condicionales `if/else`.
- Ciclos `while`, `do-while`, `for` y `foreach`.
- `break` y `continue`.
- `switch`, `case` y `default`.
- Funciones, parámetros, llamadas y valores de retorno.
- Recursión y registros de activación.
- Arreglos y acceso por índice.
- Clases, objetos, atributos, constructores y `this`.
- Herencia y llamadas a métodos.
- Manejo de `try/catch`.
- Instrucción `print`.
- IDE gráfico desarrollado con Java Swing.
- Visualización de errores, TAC, árbol sintáctico, tabla de símbolos y estructuras de runtime.

---

## Código intermedio TAC

El proyecto utiliza **Three-Address Code** como representación intermedia.

Por ejemplo, el código Compiscript:

```cps
let x: integer = a + b * c;
```

genera:

```text
t0 = b * c
t1 = a + t0
x = t1
```

La precedencia se obtiene directamente del árbol sintáctico generado por ANTLR. El generador TAC recorre este árbol y produce las instrucciones en el orden correspondiente.

Otro ejemplo:

```cps
(a + b) * c
```

produce:

```text
t0 = a + b
t1 = t0 * c
```

### Temporales

Los resultados intermedios utilizan temporales:

```text
t0
t1
t2
...
```

`AdministradorTemporales` permite crear, liberar y reutilizar temporales cuando sus valores dejan de ser necesarios.

Por ejemplo:

```cps
x = a + b;
y = c + d;
```

puede producir:

```text
t0 = a + b
x = t0
t0 = c + d
y = t0
```

La reutilización evita crear temporales innecesarios durante la generación de código intermedio.

Las convenciones completas del lenguaje intermedio se encuentran en [`docs/TAC.md`](docs/TAC.md).

---

## Arquitectura general

El proyecto se organiza principalmente en los siguientes módulos:

```text
src/main/
├── antlr4/
│   └── Compiscript.g4
│
└── java/
    ├── compiler/
    ├── ide/
    ├── semantic/
    └── tac/
```

### `semantic`

Contiene el análisis semántico, sistema de tipos, tabla de símbolos, ámbitos y validaciones del lenguaje.

### `tac`

Contiene la representación y generación del código intermedio, administración de temporales y etiquetas, traducción de expresiones, sentencias, control de flujo, funciones y estructuras.

### `compiler`

Integra las diferentes etapas del compilador y expone el pipeline completo desde código Compiscript hasta los resultados de compilación.

### `ide`

Contiene la interfaz gráfica y las visualizaciones del compilador.

---

## IDE

El proyecto incluye un IDE desarrollado con **Java Swing**.

Permite:

- Escribir código Compiscript.
- Abrir archivos `.cps`.
- Guardar programas.
- Compilar con un botón o mediante `Ctrl + Enter`.
- Consultar errores léxicos, sintácticos y semánticos.
- Visualizar el código intermedio TAC.
- Visualizar la tabla de símbolos.
- Consultar registros de activación y estructuras de clases.
- Explorar el árbol sintáctico.

El árbol sintáctico dispone de una ventana independiente con:

- Zoom.
- Panorama.
- Búsqueda de nodos.
- Desplazamiento.
- Expansión y plegado de ramas.
- Visualización completa del árbol generado por ANTLR.

Para ejecutar el IDE:

```bash
mvn exec:java
```

También puede utilizarse:

```bash
mvn compile exec:java -Dexec.mainClass=ide.CompiscriptIDE
```

---

## Ejemplos

Los programas de demostración se encuentran en:

```text
examples/
├── tac/
└── errores/
```

`examples/tac` contiene programas válidos para probar características como:

- Expresiones.
- Condicionales.
- Ciclos.
- `foreach`.
- `switch`.
- Funciones.
- Recursión.
- Arreglos.
- Clases y herencia.
- `try/catch`.

`examples/errores` contiene programas diseñados para comprobar el manejo de errores léxicos, sintácticos y semánticos.

---

## Requisitos

- Java 17 o superior.
- Maven 3.9 o superior.

Verifica las instalaciones con:

```bash
java -version
mvn -version
```

---

## Instalación y ejecución

Clona el repositorio:

```bash
git clone https://github.com/Anaru03/Cps-semantic.git
cd Cps-semantic
```

Cambia a la rama del Proyecto 2:

```bash
git switch proyecto-2-tac
```

Compila:

```bash
mvn clean compile
```

Ejecuta las pruebas:

```bash
mvn test
```

Ejecuta el IDE:

```bash
mvn exec:java
```

---

## Pruebas

El proyecto utiliza **JUnit 5** para validar las diferentes etapas del compilador.

La batería de pruebas cubre, entre otros:

- Análisis semántico.
- Tabla de símbolos.
- Operadores y expresiones.
- Generación TAC.
- Temporales y etiquetas.
- Asignaciones.
- Condicionales.
- Ciclos.
- `foreach`.
- `switch`.
- Funciones.
- Recursión.
- Registros de activación.
- Arreglos y clases.
- Integración del compilador.
- Visualización del árbol sintáctico.

Para ejecutar toda la suite:

```bash
mvn clean test
```

La versión actual contiene **274 pruebas automatizadas**.

Antes de una entrega se espera:

```text
Failures: 0
Errors: 0
BUILD SUCCESS
```

Para ejecutar únicamente las pruebas TAC:

```bash
mvn -Dtest="tac.*Test" test
```

También puede ejecutarse una clase específica:

```bash
mvn -Dtest=GeneradorExpresionesTACTest test
```

---

## Documentación

La documentación técnica adicional se encuentra en:

- [`docs/TAC.md`](docs/TAC.md) — especificación y convenciones del código intermedio.
- [`docs/ARQUITECTURA.md`](docs/ARQUITECTURA.md) — arquitectura general del compilador.
- [`docs/DIAGRAMA_ARBOL.md`](docs/DIAGRAMA_ARBOL.md) — visualización del árbol sintáctico.

---

## Estado del proyecto

### Proyecto 1 — Análisis semántico

Completado.

Incluye análisis léxico, sintáctico y semántico, sistema de tipos, tabla de símbolos, ámbitos, funciones, clases, arreglos, control de flujo y manejo de errores.

### Proyecto 2 — Generación de código intermedio

Implementado.

Incluye generación TAC para expresiones y sentencias, temporales, etiquetas, control de flujo, funciones, recursión, registros de activación, arreglos, clases, objetos y estructuras de runtime.

El IDE integra las diferentes etapas y permite visualizar el código intermedio, árbol sintáctico, tabla de símbolos, errores y estructuras generadas durante la compilación.

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