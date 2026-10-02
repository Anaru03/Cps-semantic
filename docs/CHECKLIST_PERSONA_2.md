# Proyecto 2: revisión y checklist de Persona 2

Revisión del checkout local de `proyecto-2-tac`, sobre el commit `11cf981`.
No se comparó con el estado remoto actualizado. Suite ejecutada: `mvn -o test`;
171 pruebas, 0 fallos, 0 errores, 0 omitidas. De ellas, 36 pertenecen a TAC.

## Estructura actual

- `src/main/antlr4/Compiscript.g4`: gramática; Maven genera lexer, parser y visitors.
- `src/main/java/semantic/`: validación de tipos, nombres, ámbitos, funciones,
  clases, arreglos y control de flujo del Proyecto 1.
- `src/main/java/tac/`: instrucciones, administrador de temporales, emisor y
  visitor de expresiones del Proyecto 2.
- `src/main/java/ide/CompiscriptIDE.java`: IDE Swing; muestra errores, árbol y símbolos.
- `src/test/java/`: pruebas semánticas, TAC e IDE.
- `docs/ARQUITECTURA.md`: arquitectura del análisis semántico; falta actualizar el pipeline TAC.
- `target/`: salida generada; no editar parser ni clases compiladas a mano.

Stack configurado: Java 17, Maven, ANTLR 4.13.2 y JUnit 5.11.4.

## Persona 1: base implementada, cierre pendiente

Implementado y cubierto por pruebas:

- [x] Instrucciones binarias, unarias y asignaciones; representación textual.
- [x] Literales escalares, identificadores, aritmética, lógica, relaciones e igualdad.
- [x] Precedencia y paréntesis mediante el árbol de ANTLR.
- [x] Declaraciones de variables con inicializador y asignaciones simples.
- [x] Creación, liberación y reciclaje de temporales.
- [x] Commits de implementación atribuibles a Anaru03.

Pendientes observados por inspección del código:

- [x] Conectar TAC con tipos y referencias de símbolos a través de `Compilador.compilar`
  y `AnalisisSemantico.informacion()`. Los visitors aislados mantienen compatibilidad.
- [x] Agregar pruebas negativas y diagnósticos para construcciones pendientes;
  la API no publica TAC parcial ante fallos.
- [x] Crear `docs/TAC.md`: agregado durante el primer bloque de Persona 2. El commit
  `60f8279`, llamado documentación TAC, agregó pruebas de asignaciones.
- [x] Evitar colisiones entre temporales `t0`, `t1`, etc. e identificadores legales
  al generar programas completos: el visitor reserva los nombres antes de emitir TAC.
- [x] Liberar el resultado intermedio de `visitAssignExpr` después de copiarlo al destino.
- [x] Liberar resultados de expresiones usadas como sentencias independientes.
- [x] Traducir constantes escalares y validar sus asignaciones.
- [ ] Implementar ternario: por ahora se rechaza explícitamente mediante diagnóstico TAC.
- [ ] Acordar si `&&` y `||` requieren cortocircuito: hoy se emiten como operaciones
  binarias con evaluación de ambos operandos.

Estos pendientes no impiden comenzar Persona 2, pero deben resolverse o asignarse
antes de considerar completo el compilador. Las pruebas actuales no verifican estos casos.

## Checklist de Persona 2, en orden de implementación

### 1. Contrato compartido y punto de entrada

- [ ] Documentar instrucciones y operandos para etiquetas, saltos, funciones,
  argumentos, retornos y operaciones runtime.
- [x] Extender `InstruccionTAC` para etiquetas y saltos con formato propio.
- [x] Compartir un único `GeneradorTAC` y administrador de temporales entre visitors
  mediante constructor y composición.
- [x] Crear una API de compilación reutilizable por tests e IDE que reporte errores,
  instrucciones, texto TAC y metadatos de funciones.
- [x] Bloquear generación si hay errores léxicos, sintácticos o semánticos.
- [x] Definir manejo explícito de construcciones aún no implementadas, incluido
  `try/catch`, presente en la gramática pero fuera del reparto principal.
- [x] Preparar mapas nodo → ámbito/tipo/referencia y documentar su consumo para Persona 3.

### 2. Etiquetas y condicionales

- [x] Administrador de etiquetas únicas por compilación, con reinicio determinista.
- [x] Emisión de etiqueta, salto incondicional y salto condicional.
- [x] `if` sin `else`, `if/else` y condicionales anidados.
- [x] Mantener vivos los resultados hasta su último uso; liberar condiciones después
  de emitir el salto que las consume.
- [ ] Resolver cortocircuito y ternario junto con el módulo de expresiones.
- [x] Pruebas de destinos existentes, etiquetas únicas y saltos que separan ambas ramas.

### 3. Ciclos y contexto de saltos

- [x] `while`: condición → cuerpo → regreso a condición → salida.
- [x] `do-while`: cuerpo → condición → repetición/salida.
- [x] `for`: inicialización → condición → cuerpo → actualización → condición.
  Cubrir condición y actualización omitidas; la gramática no admite `i++`, usar `i = i + 1`.
- [x] Pila de contextos para anidamiento; `break` sale del contexto adecuado.
- [x] `continue`: condición en `while`/`do-while`, actualización en `for`.
- [ ] `foreach`: evaluar iterable una vez, recorrer elementos y avanzar en `continue`.
  Acordar con Persona 3 instrucciones de longitud/acceso y representación del arreglo.
- [x] Pruebas de cero/una/varias iteraciones, anidamiento, actualización y saltos fuera de contexto.
- [ ] Pruebas de temporales persistentes del iterable/índice para `foreach`.

Bloque 2 verificado con 187 pruebas pasando. El bloque 3 es `switch/case/default`;
la pila ya distingue contextos con y sin continuación. `foreach` permanece pendiente
del contrato de arreglos con Persona 3.

### 4. Switch

- [x] Evaluar selector una vez; emitir comparaciones y destinos `case`/`default`/salida.
- [x] Documentar caída al siguiente `case` sin `break` y salida del switch con `break`.
- [x] Ajustar la validación semántica para admitir `break` en ciclos o switches.
- [x] Separar contextos de `break` y `continue`: un `switch` dentro de un ciclo
  no debe desviar `continue` fuera del ciclo.
- [x] Probar coincidencia, sin coincidencia, sin `default`, anidamiento y tipos inválidos.

Bloque 3 verificado con 196 pruebas pasando; ejemplo en `examples/tac/switch.cps`.
El siguiente bloque es funciones, argumentos, llamadas y retornos.

### 5. Funciones, llamadas y retornos

- [x] Delimitar funciones para que sus cuerpos no se ejecuten como código principal.
- [x] Registrar nombre/entrada, firma, parámetros y tipo de retorno para funciones globales.
- [x] Traducir llamadas directas en `leftHandSide`; `CallExpr` contiene argumentos,
  mientras que el receptor está en el nodo padre. Dejar extensión para métodos de Persona 3.
- [x] Definir orden de evaluación/paso de argumentos y conservar valores frente a
  llamadas anidadas o argumentos con efectos secundarios.
- [x] Emitir llamada con resultado y llamada sin valor; retorno con y sin expresión.
- [x] Implementar salida de función y coordinación con el registro de activación.
- [x] Probar múltiples parámetros, llamadas anidadas, llamadas como expresión,
  múltiples retornos, recursión directa y preservación de valores del llamador.
- [x] Definir soporte o rechazo de funciones anidadas y referencias adelantadas;
  no asumir que la semántica existente acepta recursión mutua.

Bloque 4 implementado con convención de llamadas y descriptores; la prueba de
recursión usa la pila y marcos del bloque 5 en un intérprete limitado de tests.
La asociación de posiciones con símbolos y la integración del pipeline quedan
para el bloque 6. El chequeo de retorno es conservador y está documentado en `TAC.md`.

### 6. Registros de activación y ámbitos runtime

- [x] Modelo de registro por invocación: parámetros, locales, temporales,
  retorno, enlace al llamador y dirección de regreso según la convención elegida.
- [x] Definir posiciones, unidades de offsets, tamaños/alineación si aplican y
  quién calcula cada dato. Una posición lógica es válida si se documenta.
- [x] Asegurar almacenamiento independiente en llamadas recursivas; no modelar
  los locales y temporales de todas las invocaciones como variables globales.
- [x] Distinguir parámetros y locales homónimos de funciones/bloques mediante operandos únicos.
- [x] Completar identidad de símbolos de bloques del programa principal y enlace semántico (bloque 6).
- [x] Preparar descriptor de cada función y una interfaz documentada para enlazar
  símbolos con sus posiciones; Persona 3 integra esos metadatos en `Simbolo`/`Ambito`.
- [x] Probar layouts, parámetros/locales homónimos en distintos ámbitos y recursión.

Bloque 5 verificado con 216 pruebas pasando. `GeneradorTAC.registrosActivacion()`
expone los layouts; `PilaActivaciones` crea marcos independientes y coordina
retornos. El intérprete de pruebas usa esas clases, incluidos accesos a globals.
El contrato y la lista de integración para el bloque 6 están en `docs/TAC.md`.

### 7. Pruebas, documentación y entrega

- [ ] Tests propios de control, funciones y registros con casos exitosos y fallidos.
- [ ] Casos negativos: saltos sin destino, `break`/`continue` fuera de contexto,
  llamadas con argumentos incompatibles y retornos inválidos.
- [ ] Verificar comportamiento, además de texto TAC: ejecución con un intérprete de
  prueba o comprobaciones estructurales que detecten orden y destinos incorrectos.
- [ ] Programas `.cps` de demostración: condicional, ciclos anidados, switch y factorial recursivo.
- [ ] Ejemplo integrado con `foreach`; completar arreglos/clases con Persona 3 cuando esté su módulo.
- [ ] Documentar convenciones, arquitectura, ejecución, supuestos y límites en `docs`.
- [ ] Cambiar la visualización del árbol sintáctico en el IDE: reemplazar la vista
  jerárquica tipo explorador de archivos por una imagen/diagrama con nodos y conexiones.
  A cargo de Persona 2, como último bloque; comprobar que representa el árbol
  generado por el parser y que los programas grandes se pueden recorrer con scroll o zoom.
- [ ] Ejecutar suite completa y mantener los 171 tests existentes pasando.
- [ ] Commits por feature con pruebas y autoría propia; sin mezclar toda la integración en un commit.

## Qué debe recibir Persona 3

1. API estable de generación con errores, instrucciones, TAC textual y descriptores de funciones.
2. Catálogo de instrucciones y convenciones de etiquetas, temporales, llamadas y retornos.
3. Modelo de registros de activación y contrato para posiciones/referencias de símbolos.
4. Puntos de extensión para arreglos, objetos, atributos, `this`, constructores y métodos.
5. Contrato de longitud/acceso requerido por `foreach`, sin duplicar la traducción de arreglos.
6. Tests y ejemplos ejecutables de Persona 2, con suite completa pasando.
7. Lista explícita de pendientes propios, heredados de Persona 1 y compartidos.
8. Coordinar con el cambio de árbol sintáctico a imagen/diagrama que implementará
   Persona 2 al final, en lugar de la vista tipo explorador de archivos.

Persona 3 conserva la responsabilidad de extender símbolos, generar TAC de arreglos
y clases e integrar la visualización del TAC en Swing. La integración final y las
pruebas combinadas siguen siendo trabajo compartido.

## Cierre del bloque 6

API `compiler.Compilador.compilar` implementada con snapshots de resultados y
fallos por etapa. Ejemplos compilados desde archivos y pruebas integradas de
funciones, ciclos, switch, recursión, tipos inferidos y símbolos homónimos.
Contrato para Persona 3: `docs/INTEGRACION_PERSONA_3.md`. La visualización TAC
y las estructuras siguen a su cargo; `foreach` requiere el acuerdo de arreglos.
Suite completa: 227 pruebas pasando. Bloques 4–6 implementados y verificados.
