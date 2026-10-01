# Compiscript Compiler

<p align="center">
  <img src="https://img.shields.io/badge/Java-17%2B-orange?style=for-the-badge&logo=openjdk&logoColor=white" />
  <img src="https://img.shields.io/badge/ANTLR-4.13.2-red?style=for-the-badge" />
  <img src="https://img.shields.io/badge/Maven-3.9%2B-blue?style=for-the-badge&logo=apachemaven&logoColor=white" />
  <img src="https://img.shields.io/badge/JUnit-5-green?style=for-the-badge&logo=junit5&logoColor=white" />
  <img src="https://img.shields.io/badge/TAC-Three--Address%20Code-blueviolet?style=for-the-badge" />
  <img src="https://img.shields.io/badge/status-en%20desarrollo-yellow?style=for-the-badge" />
</p>

<p align="center">
  <b>Compilador académico para el lenguaje Compiscript.</b><br/>
  Análisis léxico, sintáctico y semántico con generación de código intermedio TAC.
</p>

---

## ¿Qué es?

**Compiscript Compiler** es un proyecto académico desarrollado para el curso de **Construcción de Compiladores** de la Universidad del Valle de Guatemala.

El proyecto implementa distintas etapas del proceso de compilación de programas escritos en Compiscript.

La primera etapa del proyecto desarrolló el análisis léxico, sintáctico y semántico del lenguaje, incluyendo sistema de tipos, tabla de símbolos, ámbitos, funciones, clases, arreglos, estructuras de control y manejo de errores.

La segunda etapa extiende esta infraestructura para generar una representación intermedia basada en **Three-Address Code (TAC)** a partir del árbol sintáctico generado por ANTLR.

El flujo general es:

```text
Código Compiscript
        |
        v
      Lexer
        |
        v
      Parser
        |
        v
Árbol sintáctico
        |
        v
Análisis semántico
        |
        v
Tabla de símbolos
        |
        v
Generación de código intermedio
        |
        v
       TAC
```

---

## Código intermedio TAC

El proyecto utiliza **Three-Address Code** como representación intermedia.

Una expresión Compiscript como:

```cps
let x: integer = a + b * c;
```

se transforma en:

```text
t0 = b * c
t1 = a + t0
x = t1
```

La precedencia de operadores no se calcula nuevamente durante esta fase. El Parser de ANTLR ya construye el árbol sintáctico de acuerdo con la precedencia definida en la gramática, y el generador TAC recorre esa estructura.

Por ejemplo:

```cps
(a + b) * c
```

produce:

```text
t0 = a + b
t1 = t0 * c
```

---

## Instrucciones TAC

Actualmente la representación intermedia soporta operaciones binarias, operaciones unarias y asignaciones.

### Operaciones binarias

```text
resultado = operando1 operador operando2
```

Ejemplo:

```text
t0 = a + b
```

### Operaciones unarias

```text
resultado = operador operando
```

Ejemplo:

```text
t0 = -a
```

### Asignaciones

```text
destino = valor
```

Ejemplo:

```text
x = t0
```

---

## Expresiones soportadas

### Aritméticas

```text
+
-
*
/
%
```

Ejemplos:

```text
t0 = a + b
t1 = x * y
t2 = n % 2
```

### Lógicas

```text
&&
||
!
```

Ejemplos:

```text
t0 = a && b
t1 = x || y
t2 = !activo
```

### Relacionales

```text
<
<=
>
>=
```

Ejemplo:

```text
t0 = edad >= 18
```

### Igualdad

```text
==
!=
```

Ejemplo:

```text
t0 = a == b
```

---

## Variables temporales

Los resultados intermedios utilizan variables temporales:

```text
t0
t1
t2
...
```

La clase `AdministradorTemporales` se encarga de crear, liberar y reutilizar estos temporales.

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

Después de almacenar el primer resultado en `x`, `t0` deja de ser necesario y puede reutilizarse para la siguiente expresión.

También se liberan resultados intermedios cuando dejan de utilizarse durante la evaluación de expresiones compuestas.

---

## Arquitectura

La implementación está dividida en dos áreas principales.

### Análisis del lenguaje

El paquete `semantic` contiene la infraestructura desarrollada durante la primera etapa:

```text
semantic/
├── AnalizadorSemantico
├── AnalisisSemantico
├── TipoVisitor
├── Tipo
├── Ambito
├── Simbolo
├── ResultadoSemantico
└── validadores semánticos
```

Esta etapa se encarga de validar el programa antes de utilizarlo en fases posteriores.

### Generación TAC

El paquete `tac` contiene la infraestructura de código intermedio:

```text
tac/
├── InstruccionTAC.java
├── AdministradorTemporales.java
├── GeneradorTAC.java
└── GeneradorExpresionesTAC.java
```

`InstruccionTAC` representa una instrucción de tres direcciones.

`AdministradorTemporales` administra la creación, liberación y reutilización de temporales.

`GeneradorTAC` almacena y produce las instrucciones intermedias.

`GeneradorExpresionesTAC` recorre el árbol sintáctico generado por ANTLR y traduce expresiones, declaraciones y asignaciones a TAC.

---

## Análisis semántico

Antes de la generación de código intermedio, el proyecto cuenta con análisis semántico para verificar la coherencia del programa.

Actualmente se manejan tipos como:

| Tipo | Uso |
|---|---|
| `INTEGER` | Valores enteros |
| `FLOAT` | Valores de punto flotante |
| `STRING` | Cadenas |
| `BOOLEAN` | Valores lógicos |
| `NULL` | Valor nulo |
| `ARRAY` | Arreglos |
| `CLASS` | Clases |
| `VOID` | Ausencia de valor |
| `UNKNOWN` | Tipo todavía no determinado |
| `ERROR` | Construcción semánticamente inválida |

El análisis también incluye:

- Variables y constantes
- Compatibilidad de tipos
- Funciones y parámetros
- Retornos
- Recursión
- Clases y objetos
- Arreglos
- Ámbitos
- Tabla de símbolos
- Control de flujo
- Detección de código muerto
- Recuperación y reporte de múltiples errores

---

## Ejemplo

Código Compiscript:

```cps
let a: integer = 10;
let b: integer = 20;
let c: integer = 5;

let resultado: integer = a + b * c;

resultado = resultado - 1;
```

Código TAC correspondiente:

```text
a = 10
b = 20
c = 5

t0 = b * c
t1 = a + t0
resultado = t1

t1 = resultado - 1
resultado = t1
```

Los temporales pueden reutilizarse cuando sus valores anteriores ya no son necesarios.

---

## IDE

El proyecto conserva el IDE de escritorio desarrollado con **Java Swing**.

Para ejecutarlo:

```bash
mvn exec:java
```

La interfaz permite trabajar con código Compiscript y visualizar información producida por las etapas del compilador.

Actualmente incluye visualización de:

- Errores
- Árbol sintáctico
- Tabla de símbolos

La integración de la representación intermedia TAC con la interfaz forma parte de la evolución del Proyecto 2.

---

## Requisitos

Se necesita:

- Java 17 o superior
- Maven 3.9 o superior

Verifica las instalaciones con:

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

Para trabajar con el Proyecto 2:

```bash
git switch proyecto-2-tac
```

Compila:

```bash
mvn clean compile
```

Una compilación correcta finaliza con:

```text
BUILD SUCCESS
```

---

## Pruebas

Para ejecutar toda la batería:

```bash
mvn test
```

Estado actual:

```text
Tests run: 171
Failures: 0
Errors: 0
Skipped: 0

BUILD SUCCESS
```

Las pruebas incluyen tanto la infraestructura heredada del análisis semántico como la generación de código intermedio.

### Pruebas TAC

Actualmente se incluyen:

```text
AdministradorTemporalesTest
AsignacionesTACTest
GeneradorExpresionesTACTest
GeneradorTACTest
InstruccionTACTest
```

En conjunto cubren:

- Representación de instrucciones TAC
- Operaciones binarias
- Operaciones unarias
- Literales
- Identificadores
- Aritmética
- Operaciones lógicas
- Comparaciones
- Igualdad
- Precedencia
- Paréntesis
- Declaraciones
- Asignaciones
- Creación de temporales
- Liberación de temporales
- Reutilización de temporales
- Expresiones compuestas

Para ejecutar únicamente las pruebas TAC:

```bash
mvn -Dtest="tac.*Test" test
```

También puede ejecutarse una clase individual:

```bash
mvn -Dtest=GeneradorExpresionesTACTest test
```

o un caso específico:

```bash
mvn -Dtest=GeneradorExpresionesTACTest#respetaParentesis test
```

---

## Documentación

La documentación técnica se encuentra en:

- [`docs/ARQUITECTURA.md`](docs/ARQUITECTURA.md) — arquitectura del analizador.
- [`docs/TAC.md`](docs/TAC.md) — diseño de la representación intermedia TAC.

---

## Estado del proyecto

### Proyecto 1 — Análisis semántico

Completado.

Incluye:

```text
Análisis léxico
Análisis sintáctico
Análisis semántico
Sistema de tipos
Tabla de símbolos
Ámbitos
Funciones
Clases
Arreglos
Control de flujo
Manejo de errores
IDE
```

### Proyecto 2 — Código intermedio

En desarrollo.

Actualmente se encuentra implementada la infraestructura base de TAC y la generación de código intermedio para expresiones, declaraciones y asignaciones, incluyendo administración y reciclaje de variables temporales.

Las siguientes etapas amplían esta infraestructura con control de flujo, funciones, estructuras, información adicional de símbolos e integración completa con el IDE.

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