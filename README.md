# Compiscript Semantic Analyzer

<p align="center">
  <img src="https://img.shields.io/badge/Java-17%2B-orange?style=for-the-badge&logo=openjdk&logoColor=white" />
  <img src="https://img.shields.io/badge/ANTLR-4.13.2-red?style=for-the-badge" />
  <img src="https://img.shields.io/badge/Maven-3.9%2B-blue?style=for-the-badge&logo=apachemaven&logoColor=white" />
  <img src="https://img.shields.io/badge/JUnit-5-green?style=for-the-badge&logo=junit5&logoColor=white" />
  <img src="https://img.shields.io/badge/status-en%20desarrollo-yellow?style=for-the-badge" />
</p>

<p align="center">
  <b>Analizador semántico para el lenguaje Compiscript.</b><br/>
  Sistema de tipos, validación de expresiones y análisis semántico construido sobre ANTLR.
</p>

---

## ¿Qué es?

**Compiscript Semantic Analyzer** es un proyecto académico desarrollado para el curso de **Construcción de Compiladores** de la Universidad del Valle de Guatemala.

El proyecto extiende el análisis léxico y sintáctico de Compiscript incorporando una fase de **análisis semántico**, encargada de verificar que las instrucciones de un programa sean coherentes con las reglas y tipos definidos por el lenguaje.

ANTLR genera el Lexer y Parser a partir de la gramática de Compiscript. Posteriormente, un Visitor recorre el árbol sintáctico para determinar los tipos de las expresiones y aplicar las validaciones semánticas correspondientes.

---

## Funcionalidades actuales

Actualmente el sistema permite:

- Reconocer tipos `integer`, `string`, `boolean` y `null`
- Determinar el tipo resultante de expresiones
- Validar operaciones aritméticas `+`, `-`, `*` y `/`
- Validar operaciones lógicas `&&`, `||` y `!`
- Validar comparaciones `==`, `!=`, `<`, `<=`, `>` y `>=`
- Verificar compatibilidad de tipos en asignaciones
- Verificar tipos en la inicialización de constantes
- Validar tipos de elementos dentro de arreglos
- Validar el tipo utilizado como índice de un arreglo
- Representar errores y resultados semánticos
- Recorrer el árbol sintáctico mediante un Visitor de ANTLR
- Ejecutar pruebas automatizadas para casos válidos e inválidos

---

## Ejemplos

Expresiones válidas:

```cps
10 + 5
20 >= 10
true && false
"hola" == "mundo"
```

Expresiones incompatibles:

```cps
true + 5
10 * false
10 < "hola"
true || 10
```

El sistema determina el tipo resultante de cada expresión o identifica una incompatibilidad semántica.

Por ejemplo:

```text
10 + 5          -> INTEGER
10 < 20         -> BOOLEAN
true && false   -> BOOLEAN
true * 5        -> ERROR
```

---

## Sistema de tipos

Actualmente se contemplan los siguientes tipos dentro del analizador:

| Tipo | Uso |
|---|---|
| `INTEGER` | Valores enteros |
| `STRING` | Cadenas de texto |
| `BOOLEAN` | Valores lógicos |
| `NULL` | Valor nulo |
| `ARRAY` | Arreglos |
| `CLASS` | Tipos definidos mediante clases |
| `VOID` | Ausencia de valor |
| `UNKNOWN` | Tipo todavía no determinado |
| `ERROR` | Expresión semánticamente inválida |

---

## Cómo compilar

### Requisitos

Se necesita:

- Java 17 o superior
- Maven 3.9 o superior

Puedes comprobar las instalaciones con:

```bash
java -version
mvn -version
```

### Clonar el repositorio

```bash
git clone https://github.com/Anaru03/Cps-semantic.git
cd Cps-semantic
```

### Compilar

```bash
mvn clean compile
```

Maven genera automáticamente el Lexer, Parser, Listener y Visitor de ANTLR antes de compilar el código Java.

Una compilación correcta termina con:

```text
BUILD SUCCESS
```

---

## Cómo ejecutar las pruebas

Para ejecutar toda la batería de pruebas:

```bash
mvn clean test
```

Actualmente el proyecto cuenta con:

```text
65 tests
0 fallos
0 errores
```

Los tests cubren:

- Tipos básicos
- Literales
- Operaciones aritméticas
- Operaciones lógicas
- Comparaciones
- Asignaciones
- Constantes
- Arreglos
- Índices
- Resultados semánticos

### Ejecutar una prueba específica

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

---

## Cómo funciona

El análisis sigue actualmente este flujo:

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
   TipoVisitor
        |
        v
Sistema de tipos
        |
        v
Resultado semántico
```

El `TipoVisitor` utiliza el árbol generado por ANTLR para determinar el tipo de las expresiones.

Las diferentes reglas del sistema semántico utilizan esa información para comprobar si los operandos, asignaciones, constantes y estructuras utilizan tipos compatibles.

---

## Pruebas

La batería de pruebas incluye tanto **casos exitosos como casos fallidos**.

Por ejemplo:

```text
integer + integer     -> válido
boolean + integer     -> inválido

boolean && boolean    -> válido
integer && integer    -> inválido

integer < integer     -> válido
boolean >= boolean    -> inválido

integer <- integer    -> válido
integer <- string     -> inválido
```

Esto permite comprobar que el analizador no solamente acepta construcciones correctas, sino que también detecta incompatibilidades semánticas.

---

## Estado del proyecto

El proyecto se encuentra actualmente **en desarrollo**.

La base de ANTLR y el sistema de tipos ya se encuentran implementados y cuentan con pruebas automatizadas.

Las siguientes etapas integrarán los demás componentes del análisis semántico, incluyendo tabla de símbolos, ámbitos, funciones, clases, control de flujo y el entorno visual del compilador.

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