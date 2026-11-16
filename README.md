
# Compiscript Compiler — Proyecto 03: Generación de ensamblador MIPS

<p align="center">
  <img src="https://img.shields.io/badge/Java-17%2B-orange?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 17+" />
  <img src="https://img.shields.io/badge/ANTLR-4.13.2-red?style=for-the-badge" alt="ANTLR 4.13.2" />
  <img src="https://img.shields.io/badge/Maven-3.9%2B-blue?style=for-the-badge&logo=apachemaven&logoColor=white" alt="Maven 3.9+" />
  <img src="https://img.shields.io/badge/MIPS-MARS%204.5-purple?style=for-the-badge" alt="MIPS MARS 4.5" />
  <img src="https://img.shields.io/badge/Tests-327%20aprobados-success?style=for-the-badge" alt="327 pruebas aprobadas" />
  <img src="https://img.shields.io/badge/STATUS-TERMINADO-brightgreen?style=for-the-badge" alt="Estado: Terminado" />
</p>

<p align="center">
  <b>Universidad del Valle de Guatemala</b><br/>
  Construcción de Compiladores · Segundo semestre de 2026
</p>

<p align="center">
  Compilador académico para el lenguaje <b>Compiscript</b>, con análisis léxico, sintáctico y semántico, generación de código intermedio de tres direcciones (TAC), traducción a ensamblador MIPS, optimización por mirilla e interfaz gráfica desarrollada en Java Swing.
</p>

---

## Estado del proyecto

**ESTADO: FINALIZADO**

Los tres proyectos desarrollados durante el curso se encuentran finalizados dentro del alcance implementado:

| Proyecto | Descripción | Estado |
|---|---|---|
| Proyecto 1 | Análisis léxico, sintáctico y semántico | Finalizado |
| Proyecto 2 | Generación de código intermedio TAC | Finalizado |
| Proyecto 3 | Generación de código ensamblador MIPS | Finalizado |

### Validación final

**Fecha de validación:** 16 de noviembre de 2026.

- 327 pruebas automatizadas aprobadas.
- 0 fallos, 0 errores y 0 pruebas omitidas.
- 6 programas de ejemplo MIPS verificados mediante MARS 4.5.
- Generación de ensamblador desde el código TAC.
- Optimización por mirilla con nueve reglas implementadas.
- Seis transformaciones diferentes demostradas con `mirilla_demo.cps`.
- Integración del generador MIPS en la interfaz gráfica.
- Exportación de ensamblador a archivos `.asm`.
- Ejecución de ensamblador mediante MARS.
- Recuperación de múltiples errores sintácticos.
- Bloqueo de generación TAC y MIPS cuando existen errores de análisis.

**Nota:** El proyecto está finalizado dentro de su alcance implementado y probado. Esto no implica soporte completo de todas las construcciones del lenguaje Compiscript.

---

## 1. Descripción general

**Compiscript Compiler** es un compilador académico desarrollado para el curso de Construcción de Compiladores de la Universidad del Valle de Guatemala.

El objetivo del Proyecto 3 es transformar programas escritos en Compiscript a código ensamblador MIPS utilizando como entrada la representación intermedia TAC desarrollada en el Proyecto 2.

El compilador implementa las siguientes etapas:

1. Análisis léxico mediante ANTLR.
2. Análisis sintáctico y construcción del árbol.
3. Análisis semántico y validación de tipos.
4. Generación de código intermedio TAC.
5. Traducción del TAC a ensamblador MIPS.
6. Optimización del ensamblador mediante mirilla.
7. Visualización y exportación desde el IDE.
8. Ejecución del ensamblador mediante MARS 4.5.

### Flujo del compilador

```text
              CÓDIGO COMPISCRIPT (.cps)
                         |
                         v
                  ANÁLISIS LÉXICO
                      ANTLR
                         |
                         v
                 ANÁLISIS SINTÁCTICO
                         |
                         v
                  ÁRBOL SINTÁCTICO
                         |
                         v
                  ANÁLISIS SEMÁNTICO
                         |
                  +------+------+
                  |             |
               ERRORES        VÁLIDO
                  |             |
                  v             v
           MOSTRAR ERRORES   GENERACIÓN TAC
                                |
                                v
                       CÓDIGO INTERMEDIO
                                |
                                v
                       GENERADOR MIPS
                                |
                                v
                      ENSAMBLADOR ORIGINAL
                                |
                                v
                      OPTIMIZADOR MIRILLA
                                |
                                v
                      ENSAMBLADOR MIPS
                                |
                                v
                       ARCHIVO .asm
                                |
                                v
                           MARS 4.5
                                |
                                v
                       SALIDA DEL PROGRAMA
```

Si existen errores léxicos, sintácticos o semánticos, el compilador no genera TAC ni ensamblador MIPS.

---

## 2. Tecnologías utilizadas

| Tecnología | Utilización |
|---|---|
| Java 17 | Implementación del compilador |
| ANTLR 4.13.2 | Generación del lexer y parser |
| Maven | Gestión de dependencias y compilación |
| Java Swing | Interfaz gráfica |
| TAC | Representación intermedia |
| MIPS | Arquitectura ensamblador destino |
| MARS 4.5 | Ensamblador y simulador |
| JUnit 5 | Pruebas automatizadas |
| Git y GitHub | Control de versiones |

---

## 3. Arquitectura del proyecto

El código se organiza en módulos que representan las etapas del compilador.

```text
Cps-semantic/
|
|-- src/
|   |-- main/
|   |   |-- antlr4/
|   |   |   `-- Compiscript.g4
|   |   |
|   |   `-- java/
|   |       |-- compiler/
|   |       |-- semantic/
|   |       |-- tac/
|   |       |-- mips/
|   |       `-- ide/
|   |
|   `-- test/
|       `-- java/
|           |-- semantic/
|           |-- tac/
|           |-- mips/
|           `-- ide/
|
|-- examples/
|   `-- mips/
|
|-- scripts/
|   |-- preparar-mars.sh
|   `-- verificar-mips.sh
|
|-- docs/
|
|-- salidas/
|   `-- mips/
|
|-- pom.xml
`-- README.md
```

### Módulo `compiler`

Integra las etapas del compilador y proporciona el resultado del análisis de un programa Compiscript.

La clase principal de integración es:

`compiler.Compilador`

### Módulo `semantic`

Implementa el análisis semántico, validación de tipos, manejo de ámbitos y tabla de símbolos.

### Módulo `tac`

Implementa la representación intermedia de tres direcciones.

Incluye:

- Instrucciones TAC.
- Expresiones y asignaciones.
- Control de flujo.
- Funciones y parámetros.
- Administración de temporales.
- Etiquetas.
- Registros de activación.
- Arreglos.
- Clases y herencia.
- Manejo de excepciones.

### Módulo `mips`

Contiene el backend de generación de ensamblador MIPS.

Entre sus componentes principales se encuentran:

| Archivo | Responsabilidad |
|---|---|
| `GeneradorMIPS.java` | API principal de generación |
| `TraductorTACProvisional.java` | Traducción desde la representación TAC |
| `AsignadorRegistros.java` | Administración y asignación de registros |
| `MarcoPilaMIPS.java` | Organización de los marcos de pila |
| `GeneradorFuncionesMIPS.java` | Funciones, parámetros y retornos |
| `GeneradorEstructurasMIPS.java` | Estructuras y operaciones del runtime |
| `RuntimeMIPS.java` | Soporte de ejecución |
| `OptimizadorMirilla.java` | Optimización del ensamblador |
| `CompilarMIPS.java` | Punto de entrada para generación desde consola |

### Módulo `ide`

Contiene la interfaz gráfica del compilador.

Los componentes principales son:

- `CompiscriptIDE.java`
- `PanelesCompilacion.java`
- `PanelMIPS.java`
- `DiagramaArbol.java`

La interfaz permite visualizar las diferentes fases de compilación en un mismo entorno.

---

## 4. Generación de código ensamblador MIPS

El Proyecto 3 utiliza la representación intermedia TAC del Proyecto 2 como entrada para la generación de ensamblador.

La fachada pública del backend es:

```java
GeneradorMIPS generador = new GeneradorMIPS();

GeneradorMIPS.Resultado resultadoMIPS =
        generador.generar(resultadoCompilacion);
```

El resultado proporciona:

- `codigo()`: ensamblador optimizado.
- `sinOptimizar()`: ensamblador original.
- `errores()`: problemas detectados durante la generación.
- `optimizaciones()`: cantidad de aplicaciones por regla.
- `esValido()`: indica si la generación terminó sin errores.

Cuando el análisis falla, no se publica código ensamblador parcial.

### Asignación de registros

El backend incorpora un administrador de registros MIPS para traducir operaciones intermedias a instrucciones de bajo nivel.

Se consideran los registros disponibles y las necesidades de almacenamiento de los operandos, utilizando registros y memoria según corresponda.

### Registros de activación

Se implementa el manejo de marcos de pila para permitir:

- Llamadas a funciones.
- Paso de parámetros.
- Almacenamiento de variables locales.
- Conservación del estado necesario.
- Retorno de funciones.
- Llamadas recursivas.

La implementación se encuentra principalmente en `MarcoPilaMIPS.java` y `GeneradorFuncionesMIPS.java`.

### Estructuras de datos

El backend incluye soporte probado mediante ejemplos y pruebas automatizadas para:

- Variables y constantes.
- Operaciones aritméticas.
- Arreglos.
- Funciones.
- Recursividad.
- Clases y objetos.
- Herencia.
- Manejo de excepciones.

El alcance exacto de cada construcción depende de las operaciones implementadas en el backend MIPS.

---

## 5. Optimización por mirilla

El compilador implementa optimización por mirilla sobre el código ensamblador generado.

Esta técnica examina pequeñas secuencias de instrucciones para identificar transformaciones que preservan el comportamiento del programa.

El optimizador implementa nueve reglas:

| Regla | Descripción |
|---|---|
| `move-identico` | Elimina movimientos de un registro hacia sí mismo |
| `addiu-cero` | Simplifica sumas inmediatas con cero |
| `addu-cero` | Simplifica sumas con el registro cero |
| `subu-cero` | Simplifica restas de cero |
| `or-cero` | Simplifica operaciones OR con cero |
| `shift-cero` | Simplifica desplazamientos de cero posiciones |
| `salto-siguiente` | Elimina saltos hacia la instrucción siguiente |
| `li-repetido` | Elimina cargas inmediatas redundantes |
| `li-cero` | Simplifica cargas inmediatas de cero |

### Demostración de optimización

El archivo:

`examples/mips/mirilla_demo.cps`

permite demostrar seis transformaciones diferentes.

Para comprobarlas:

1. Abrir el archivo desde Compiscript IDE.
2. Presionar **Compilar**.
3. Seleccionar la pestaña **MIPS**.
4. Revisar el resumen de optimizaciones.
5. Presionar **Ver sin optimizar**.
6. Presionar **Ver optimizado**.

La última validación confirmó seis reglas activadas.

El resultado esperado al ejecutar el ensamblador en MARS es:

```text
7
7
0
7
```

---

## 6. Interfaz gráfica

El proyecto incluye un IDE desarrollado con Java Swing.

La interfaz permite:

- Escribir programas Compiscript.
- Seleccionar archivos `.cps`.
- Guardar código fuente.
- Compilar programas.
- Consultar errores.
- Visualizar código TAC.
- Explorar el árbol sintáctico.
- Consultar símbolos.
- Revisar estructuras de runtime.
- Visualizar código ensamblador MIPS.
- Comparar ensamblador optimizado y original.
- Copiar ensamblador.
- Exportar archivos `.asm`.

### Pestañas del IDE

| Pestaña | Funcionalidad |
|---|---|
| Problemas | Errores léxicos, sintácticos y semánticos |
| TAC | Código intermedio de tres direcciones |
| Árbol | Árbol sintáctico generado por ANTLR |
| Símbolos | Tabla de símbolos |
| Runtime | Registros de activación y estructuras |
| MIPS | Código ensamblador y herramientas de exportación |

### Controles principales

**Abrir…**

Permite seleccionar un archivo Compiscript con extensión `.cps`.

**Guardar**

Guarda el código fuente del editor.

**Compilar**

Ejecuta el análisis y las fases de generación disponibles.

**Ver sin optimizar**

Muestra el ensamblador antes de aplicar las transformaciones de mirilla.

**Ver optimizado**

Muestra el ensamblador después de aplicar las optimizaciones.

**Copiar**

Copia el ensamblador mostrado al portapapeles.

**Guardar .asm**

Exporta el ensamblador mostrado a un archivo compatible con MARS.

### Manejo de errores

Cuando se detectan errores léxicos, sintácticos o semánticos:

- Se presentan los diagnósticos en la pestaña Problemas.
- No se genera código TAC.
- No se genera ensamblador MIPS.
- Se eliminan los resultados de compilaciones anteriores.
- Se deshabilitan las acciones de exportación de MIPS.

La recuperación de errores permite reportar múltiples problemas durante una misma ejecución cuando el análisis puede continuar.

---

## 7. Requisitos

Para ejecutar el proyecto se necesita:

- JDK 17 o superior.
- Maven.
- Entorno gráfico compatible con Java Swing.
- Acceso a MARS 4.5 para simular el ensamblador.

Verificar las herramientas instaladas:

```bash
java -version
mvn -version
```

---

## 8. Instalación

Clonar el repositorio:

```bash
git clone https://github.com/Anaru03/Cps-semantic.git

cd Cps-semantic
```

Seleccionar la rama del Proyecto 3:

```bash
git switch proyecto-3-mips
```

Compilar:

```bash
mvn clean compile
```

El resultado esperado es:

```text
BUILD SUCCESS
```

---

## 9. Ejecutar Compiscript IDE

Desde la raíz del proyecto:

```bash
mvn exec:java
```

Se abrirá la interfaz gráfica del compilador.

### Ejemplo de uso

1. Presionar **Abrir…**.
2. Seleccionar `examples/mips/arreglos.cps`.
3. Presionar **Compilar**.
4. Revisar las pestañas Problemas y TAC.
5. Seleccionar la pestaña MIPS.
6. Presionar **Guardar .asm**.
7. Guardar el archivo como `salidas/mips/arreglos.asm`.

Crear previamente la carpeta de salida si es necesario:

```bash
mkdir -p salidas/mips
```

Se recomienda utilizar `salidas/mips` para conservar los ensambladores.

La carpeta `target/` es administrada por Maven y puede eliminarse al ejecutar `mvn clean`.

---

## 10. Ejecutar ensamblador en MARS

Preparar MARS:

```bash
bash scripts/preparar-mars.sh
```

Ejecutar el ensamblador exportado:

```bash
java -jar .cache/mars/Mars4_5.jar \
  nc ae1 se2 1000000 \
  salidas/mips/arreglos.asm
```

Salida esperada:

```text
13
```

Este resultado fue verificado ejecutando el archivo generado desde la interfaz gráfica.

### Ejecutar el ejemplo de mirilla

Guardar el ensamblador optimizado como:

`salidas/mips/mirilla_demo.asm`

Ejecutar:

```bash
java -jar .cache/mars/Mars4_5.jar \
  nc ae1 se2 1000000 \
  salidas/mips/mirilla_demo.asm
```

Salida esperada:

```text
7
7
0
7
```

MARS es una herramienta externa. El IDE no necesita ejecutar directamente el ensamblador para generar código válido.

---

## 11. Generación MIPS desde consola

También es posible utilizar el generador sin abrir el IDE.

Crear la carpeta de salida:

```bash
mkdir -p salidas/mips
```

Ejecutar:

```bash
mvn -q exec:java \
  -Dexec.mainClass=mips.CompilarMIPS \
  -Dexec.args="examples/mips/arreglos.cps salidas/mips/arreglos.asm"
```

Después ejecutar el archivo generado con MARS:

```bash
java -jar .cache/mars/Mars4_5.jar \
  nc ae1 se2 1000000 \
  salidas/mips/arreglos.asm
```

---

## 12. Programas de prueba

Los programas de ejemplo del Proyecto 3 se encuentran en:

`examples/mips/`

| Programa | Objetivo |
|---|---|
| `arreglos.cps` | Arreglos, matrices y recorridos |
| `funciones_recursion.cps` | Funciones, parámetros y recursividad |
| `recursion_locales.cps` | Recursión y variables locales |
| `objetos_herencia.cps` | Clases, objetos y herencia |
| `excepciones.cps` | Manejo de excepciones |
| `mirilla_demo.cps` | Optimizaciones por mirilla |

Cada ejemplo dispone de un archivo `.expected` con su salida esperada.

### Prueba de errores sintácticos

Para comprobar la recuperación de errores, introducir en el editor:

```cps
let a: integer = ;
let b: integer = ;
let c: integer = 10;
```

Presionar **Compilar**.

El compilador debe reportar los errores de las primeras dos líneas sin generar TAC ni MIPS.

---

## 13. Pruebas automatizadas

El proyecto utiliza JUnit 5.

La última ejecución completa, realizada el 16 de noviembre de 2026, produjo:

```text
Tests run: 327
Failures: 0
Errors: 0
Skipped: 0

BUILD SUCCESS
```

### Ejecutar todas las pruebas MIPS

Desde la raíz del repositorio:

```bash
bash scripts/verificar-mips.sh
```

Este script:

1. Comprueba o prepara MARS.
2. Compila el proyecto.
3. Ejecuta las pruebas automatizadas.
4. Genera ensambladores de los ejemplos.
5. Ejecuta los programas mediante MARS.
6. Compara las salidas con los archivos `.expected`.

El resultado final esperado es:

```text
Pruebas y equivalencia MIPS verificadas.
```

La evidencia generada por el script se almacena en:

`target/mips/`

### Pruebas del backend MIPS

Entre las clases de prueba se encuentran:

- `AsignadorRegistrosTest`
- `MarcoPilaMIPSTest`
- `GeneradorFuncionesMIPSTest`
- `FuncionesMIPSPipelineTest`
- `EstructurasMIPSTest`
- `GeneradorMIPSTest`
- `OptimizadorMirillaTest`

También se ejecutan las pruebas de los proyectos anteriores, correspondientes al análisis semántico, TAC e IDE.

---

## 14. Limitaciones conocidas

El backend MIPS está orientado principalmente a operaciones y representaciones enteras.

Algunas construcciones del lenguaje Compiscript pueden ser aceptadas por el análisis y la generación TAC, pero todavía no disponer de traducción MIPS.

Entre las limitaciones conocidas se encuentra:

- Representación y operaciones `float` en el backend entero MIPS.
- Determinadas operaciones de cadenas y construcciones no cubiertas por las pruebas MIPS disponibles.

Cuando una construcción no es soportada, el generador debe informar el problema y evitar publicar ensamblador parcial como resultado válido.

El compilador se considera finalizado dentro del alcance implementado y validado.

---

## 15. Documentación técnica

Los documentos complementarios se encuentran en `docs/`.

| Documento | Contenido |
|---|---|
| `ARQUITECTURA.md` | Arquitectura general del compilador |
| `TAC.md` | Representación intermedia y convenciones |
| `MIPS_FUNCIONES.md` | Generación de funciones y manejo de pila |
| `MIPS_INTEGRACION_RUTH.md` | Contrato de integración con el IDE |
| `DIAGRAMA_ARBOL.md` | Visualización del árbol sintáctico |

Estos documentos complementan la explicación de los componentes y las decisiones de implementación.

---

## 16. Organización del repositorio

El desarrollo se mantiene en ramas separadas para conservar la evolución de los proyectos.

| Rama | Contenido |
|---|---|
| `main` | Proyecto 1: análisis semántico |
| `proyecto-2-tac` | Proyecto 2: generación de TAC |
| `proyecto-3-mips` | Proyecto 3: generación de MIPS e integración gráfica |

La rama principal para la entrega del Proyecto 3 es:

**`proyecto-3-mips`**

---

## 17. Integrantes

Proyecto desarrollado por:

- **Ruth de León** — [Anaru03](https://github.com/Anaru03)
- **Alejandro Antón** — [Anton17303](https://github.com/Anton17303)
- **Jorge López** — [Jorge162017](https://github.com/Jorge162017)

Las contribuciones individuales se encuentran registradas en el historial de commits de GitHub.

---

<p align="center">
  <b>Universidad del Valle de Guatemala</b><br/>
  Facultad de Ingeniería<br/>
  Construcción de Compiladores<br/>
  Segundo semestre de 2026
</p>

<p align="center">
  <b>Proyecto 03 — Generación de código ensamblador MIPS</b><br/>
  <b>ESTADO: FINALIZADO</b>
</p>


**Universidad del Valle de Guatemala · Construcción de Compiladores · Segundo semestre de 2026**

Compilador académico de **Compiscript** con análisis léxico, sintáctico y semántico, generación de código de tres direcciones (**TAC**) y traducción a ensamblador **MIPS**, con optimización por mirilla e interfaz gráfica Java Swing.

> **Estado verificado (16 de noviembre de 2026):** el IDE genera TAC y MIPS; los ejemplos probados se ejecutan en MARS 4.5. La última ejecución de `scripts/verificar-mips.sh` reportó **327 pruebas, 0 fallos, 0 errores y 0 omitidas**, además de equivalencia de los seis ejemplos MIPS. Esto no implica soporte total de todas las construcciones del lenguaje.

## Inicio rápido

**Requisitos:** JDK 17 o superior, Maven y entorno gráfico para Swing. MARS 4.5 se prepara mediante el script del repositorio.

```bash
git clone https://github.com/Anaru03/Cps-semantic.git
cd Cps-semantic
git switch proyecto-3-mips
java -version
mvn -version
mvn clean compile
mvn exec:java
```

En el IDE: **Abrir…** → `examples/mips/arreglos.cps` → **Compilar** → pestaña **MIPS** → **Guardar .asm**. Se recomienda guardar en `salidas/mips/arreglos.asm`.

```bash
mkdir -p salidas/mips
bash scripts/preparar-mars.sh
java -jar .cache/mars/Mars4_5.jar nc ae1 se2 1000000 salidas/mips/arreglos.asm
```

**Salida esperada:** `13`.

> El archivo `.asm` no es un ejecutable nativo de Linux: MARS lo ensambla y simula. `mvn clean` elimina `target/`, por lo que los ensambladores que se quieran conservar deben guardarse en `salidas/mips/` u otra carpeta fuera de `target/`.

## Flujo del compilador

```text
Archivo .cps seleccionado desde el IDE
              |
              v
ANTLR: lexer y parser -> árbol sintáctico
              |
              v
Análisis semántico y tabla de símbolos
              |
         ¿Hay errores? ---- Sí ---> Diagnósticos; sin TAC ni MIPS
              |
              No
              v
Generación de código de tres direcciones (TAC)
              |
              v
Backend MIPS: registros, pila, funciones y estructuras
              |
              v
Optimización por mirilla
              |
              v
Pestaña MIPS -> copiar / comparar / exportar .asm
              |
              v
MARS 4.5 (ejecución externa)
```

La generación MIPS **consume el TAC producido por el Proyecto 2**, no traduce directamente desde el texto fuente. El análisis léxico y sintáctico utiliza **ANTLR**, no un parser manual.

## Interfaz gráfica

El IDE Swing permite escribir o abrir archivos `.cps`, guardarlos y compilarlos. Incluye seis pestañas:

| Pestaña | Contenido |
| --- | --- |
| **Problemas** | Diagnósticos léxicos, sintácticos y semánticos, con líneas y columnas. |
| **TAC** | Instrucciones de tres direcciones generadas. |
| **Árbol** | Visualización navegable del árbol sintáctico. |
| **Símbolos** | Tabla de símbolos, tipos y ámbitos. |
| **Runtime** | Registros de activación y estructuras de clases. |
| **MIPS** | Ensamblador optimizado, versión sin optimizar y opciones **Copiar** y **Guardar .asm**. |

Si existen errores léxicos, sintácticos o semánticos, no se publica TAC ni MIPS. Si el TAC es válido pero el backend MIPS no soporta una construcción, se informa de la limitación sin mostrar ensamblador parcial como válido.

## Backend MIPS y arquitectura

| Paquete / archivo | Responsabilidad |
| --- | --- |
| `compiler/Compilador.java` | Pipeline de análisis y generación TAC. |
| `tac/` | Representación intermedia, temporales, etiquetas y registros de activación. |
| `mips/GeneradorMIPS.java` | Fachada para traducir un `ResultadoCompilacion` a MIPS. |
| `mips/TraductorTACProvisional.java` | Traducción del TAC mediante los componentes del backend. |
| `mips/AsignadorRegistros.java` | Administración y asignación de registros. |
| `mips/MarcoPilaMIPS.java` | Organización de pila y marcos de activación. |
| `mips/GeneradorFuncionesMIPS.java` | Llamadas, parámetros, retornos y recursión. |
| `mips/GeneradorEstructurasMIPS.java` | Arreglos, objetos, herencia y excepciones. |
| `mips/RuntimeMIPS.java` | Soporte de ejecución en ensamblador. |
| `mips/OptimizadorMirilla.java` | Transformaciones locales de código MIPS. |
| `ide/CompiscriptIDE.java` | Ventana principal y coordinación de la compilación. |
| `ide/PanelMIPS.java` | Visualización y exportación de ensamblador. |

Consultar también `docs/ARQUITECTURA.md`, `docs/MIPS_FUNCIONES.md` y `docs/MIPS_INTEGRACION_RUTH.md` para detalles técnicos. Los layouts de TAC son posiciones lógicas y el backend los adapta a su representación MIPS.

## Optimizaciones por mirilla

El optimizador incluye nueve reglas: `move-identico`, `addiu-cero`, `addu-cero`, `subu-cero`, `or-cero`, `shift-cero`, `salto-siguiente`, `li-repetido` y `li-cero`.

En `examples/mips/mirilla_demo.cps`, la validación realizada activó **seis reglas diferentes**: `move-identico`, `addu-cero`, `subu-cero`, `salto-siguiente`, `li-repetido` y `li-cero`. Desde el IDE es posible comparar el código antes y después de optimizar.

La ejecución de `mirilla_demo.asm` en MARS produjo:

```text
7
7
0
7
```

## Pruebas y validación

El repositorio incluye pruebas JUnit 5 para análisis, TAC, registros, pila, funciones, estructuras y optimización MIPS, además de programas `.cps` con archivos `.expected`.

**Validación integral (recomendada):**

```bash
bash scripts/verificar-mips.sh
```

El script prepara MARS, ejecuta las pruebas, genera ensambladores y contrasta las salidas de los ejemplos. En la ejecución verificada del 16/11/2026: **327 pruebas aprobadas** y mensaje `Pruebas y equivalencia MIPS verificadas`.

**Ejemplos MIPS:**

| Programa | Objetivo |
| --- | --- |
| `examples/mips/arreglos.cps` | Arreglos y `foreach`; salida comprobada: `13`. |
| `examples/mips/objetos_herencia.cps` | Objetos y herencia. |
| `examples/mips/excepciones.cps` | Manejo de excepciones. |
| `examples/mips/funciones_recursion.cps` | Funciones y recursividad. |
| `examples/mips/recursion_locales.cps` | Recursión con variables locales. |
| `examples/mips/mirilla_demo.cps` | Demostración de seis transformaciones por mirilla. |

Los resultados esperados exactos de los demás ejemplos se encuentran en sus respectivos archivos `.expected`.

**Recuperación de errores:** se probó desde el IDE un programa con dos expresiones incompletas; el compilador reportó dos errores sintácticos y limpió las vistas TAC y MIPS. Se recomienda ampliar las pruebas con errores léxicos y semánticos múltiples y casos de recuperación más complejos.

## Limitaciones conocidas

El backend MIPS actual no implementa todas las construcciones del lenguaje con la misma cobertura que el análisis y TAC. En particular, se ha observado el rechazo de la representación `float` y existen restricciones en determinadas operaciones con cadenas y funciones anidadas. Estas limitaciones deben comunicarse durante la demostración; no equivalen a errores del parser o de TAC.

## Estructura de ramas

- `main`: base del Proyecto 1 (análisis semántico).
- `proyecto-2-tac`: Proyecto 2 (código intermedio TAC).
- **`proyecto-3-mips`**: Proyecto 3 (backend MIPS e integración gráfica).

Para la entrega del Proyecto 3, utilizar la rama **`proyecto-3-mips`**. El historial de Git identifica las contribuciones individuales.

## Integrantes

- **Ruth de León** — [Anaru03](https://github.com/Anaru03)
- **Alejandro Antón** — [Anton17303](https://github.com/Anton17303)
- **Jorge López** — [Jorge162017](https://github.com/Jorge162017)

---

**Universidad del Valle de Guatemala — Construcción de Compiladores — Proyecto 03 (2026)**
