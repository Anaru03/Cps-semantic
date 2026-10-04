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

<p align="center">
  <b>Estado del proyecto: Finalizado</b>
</p>

---

## Descripción

**Compiscript Compiler** es un proyecto académico desarrollado para el curso de **Construcción de Compiladores** de la Universidad del Valle de Guatemala.

El proyecto implementa las principales etapas de análisis y generación de código intermedio para programas escritos en Compiscript.

El **Proyecto 1** construyó la base del compilador mediante análisis léxico, sintáctico y semántico, sistema de tipos, tabla de símbolos, ámbitos y validaciones del lenguaje.

El **Proyecto 2** extiende esta infraestructura con generación de código intermedio utilizando **Three-Address Code (TAC)**, administración de temporales y etiquetas, control de flujo, funciones, registros de activación, arreglos, clases y estructuras necesarias para representar la ejecución del programa.

El flujo general del compilador es:

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

El compilador incluye:

- Análisis léxico y sintáctico mediante ANTLR.
- Análisis semántico y sistema de tipos.
- Tabla de símbolos y manejo de ámbitos.
- Generación de código intermedio TAC.
- Creación, liberación y reutilización de variables temporales.
- Generación y administración de etiquetas.
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

El proyecto utiliza **Three-Address Code (TAC)** como representación intermedia.

Por ejemplo, el código Compiscript:

```cps
let x: integer = a + b * c;
```

puede generar:

```text
t0 = b * c
t1 = a + t0
x = t1
```

La precedencia de operadores se obtiene del árbol sintáctico generado por ANTLR. El generador TAC recorre esta estructura y produce las instrucciones en el orden correspondiente.

Por ejemplo:

```cps
(a + b) * c
```

genera:

```text
t0 = a + b
t1 = t0 * c
```

### Temporales

Los resultados intermedios utilizan variables temporales:

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

puede generar:

```text
t0 = a + b
x = t0
t0 = c + d
y = t0
```

De esta manera, un temporal puede reutilizarse cuando su valor anterior ya no es necesario.

Las convenciones completas de la representación intermedia se encuentran en [`docs/TAC.md`](docs/TAC.md).

---

## Arquitectura

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

Integra las diferentes etapas del compilador y permite ejecutar el pipeline completo desde código Compiscript hasta los resultados de compilación.

### `ide`

Contiene la interfaz gráfica y las diferentes visualizaciones del compilador.

---

## IDE

El proyecto incluye un IDE desarrollado con **Java Swing** para trabajar directamente con programas Compiscript.

La interfaz permite:

- Escribir código Compiscript.
- Abrir archivos `.cps`.
- Guardar programas.
- Compilar mediante el botón `Compilar`.
- Compilar mediante `Ctrl + Enter`.
- Consultar errores léxicos, sintácticos y semánticos.
- Visualizar el código intermedio TAC.
- Consultar la tabla de símbolos.
- Visualizar registros de activación y estructuras de clases.
- Explorar el árbol sintáctico generado por ANTLR.

Las vistas principales del IDE son:

```text
Problemas | TAC | Árbol | Símbolos | Runtime
```

### Visualización del árbol sintáctico

El árbol sintáctico puede abrirse en una ventana independiente para facilitar la visualización de programas grandes.

La herramienta permite:

- Mostrar el árbol completo.
- Expandir y plegar ramas.
- Aplicar zoom.
- Mostrar una vista panorámica.
- Buscar nodos.
- Centrar el árbol.
- Desplazarse horizontal y verticalmente.
- Seleccionar nodos y consultar su ubicación en el código.

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

Los programas utilizados para probar el compilador se encuentran en:

```text
examples/
├── tac/
└── errores/
```

### Programas válidos

`examples/tac` contiene ejemplos para probar características como:

- Expresiones.
- Condicionales.
- Ciclos.
- `foreach`.
- `switch`.
- Funciones.
- Recursión.
- Arreglos.
- Clases y herencia.
- Manejo de excepciones con `try/catch`.

### Programas con errores

`examples/errores` contiene casos diseñados para comprobar la detección y presentación de errores léxicos, sintácticos y semánticos.

---

## Requisitos

Para ejecutar el proyecto se necesita:

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

Cambia a la rama del Proyecto 2:

```bash
git switch proyecto-2-tac
```

Compila el proyecto:

```bash
mvn clean compile
```

Una compilación correcta debe finalizar con:

```text
BUILD SUCCESS
```

---

## Ejecución

Para abrir el IDE:

```bash
mvn exec:java
```

El flujo normal de uso es:

```text
Escribir o abrir programa .cps
              │
              ▼
           Compilar
              │
              ▼
     Análisis del programa
              │
              ▼
     Generación de código TAC
              │
              ▼
 Visualización de resultados
```

Si existen errores, estos se muestran en la vista **Problemas**.

Si el programa es válido, el IDE permite consultar el TAC, árbol sintáctico, tabla de símbolos y estructuras de runtime.

---

## Pruebas

El proyecto utiliza **JUnit 5** para validar las diferentes etapas del compilador.

La batería de pruebas cubre:

- Análisis semántico.
- Sistema de tipos.
- Tabla de símbolos.
- Operadores y expresiones.
- Generación TAC.
- Administración de temporales.
- Administración de etiquetas.
- Asignaciones.
- Condicionales.
- Ciclos.
- `foreach`.
- `switch`.
- Funciones.
- Recursión.
- Registros de activación.
- Arreglos.
- Clases.
- Integración del compilador.
- IDE y visualización del árbol sintáctico.

Para ejecutar toda la suite:

```bash
mvn clean test
```

La versión actual contiene **274 pruebas automatizadas**.

El resultado esperado es:

```text
Tests run: 274
Failures: 0
Errors: 0
Skipped: 0

BUILD SUCCESS
```

Para ejecutar únicamente las pruebas relacionadas con TAC:

```bash
mvn -Dtest="tac.*Test" test
```

Para ejecutar una clase específica:

```bash
mvn -Dtest=GeneradorExpresionesTACTest test
```

Para ejecutar un caso individual:

```bash
mvn -Dtest=GeneradorExpresionesTACTest#respetaParentesis test
```

---

## Documentación

La documentación técnica adicional se encuentra en:

- [`docs/TAC.md`](docs/TAC.md) — especificación y convenciones del código intermedio.
- [`docs/ARQUITECTURA.md`](docs/ARQUITECTURA.md) — arquitectura general del compilador.
- [`docs/DIAGRAMA_ARBOL.md`](docs/DIAGRAMA_ARBOL.md) — visualización del árbol sintáctico.

---

## Estado del proyecto

> **Estado general: Finalizado**

El desarrollo correspondiente a los Proyectos 1 y 2 se encuentra completado.

### Proyecto 1 — Análisis semántico

**Estado: Finalizado**

Incluye:

- Análisis léxico.
- Análisis sintáctico.
- Análisis semántico.
- Sistema de tipos.
- Tabla de símbolos.
- Manejo de ámbitos.
- Funciones.
- Clases.
- Arreglos.
- Control de flujo.
- Manejo de errores.
- IDE.

### Proyecto 2 — Generación de código intermedio

**Estado: Finalizado**

Incluye:

- Representación intermedia TAC.
- Generación de TAC desde el árbol sintáctico.
- Expresiones y asignaciones.
- Administración y reutilización de temporales.
- Administración de etiquetas.
- Condicionales.
- Ciclos.
- `break` y `continue`.
- `switch`.
- Funciones, parámetros, llamadas y retornos.
- Recursión.
- Registros de activación.
- Arreglos.
- Clases y objetos.
- Atributos y constructores.
- `this`.
- Herencia y llamadas a métodos.
- Manejo de `try/catch`.
- Integración con la tabla de símbolos.
- Visualización de TAC en el IDE.
- Visualización interactiva del árbol sintáctico.
- Visualización de símbolos y estructuras de runtime.
- Programas de ejemplo.
- Pruebas automatizadas.

El compilador permite recorrer el flujo desde un programa escrito en Compiscript hasta la generación y visualización de su representación intermedia.

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