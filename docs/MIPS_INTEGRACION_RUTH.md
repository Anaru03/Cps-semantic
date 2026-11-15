# Proyecto 3: entrega del runtime y la mirilla a Ruth

## Entrada pública para el IDE

```java
ResultadoCompilacion analisis = Compilador.compilar(fuente);
GeneradorMIPS.Resultado mips = new GeneradorMIPS().generar(analisis);
if (mips.esValido()) {
    // Mostrar mips.codigo() en la pestaña MIPS y permitir guardar/copiar .asm.
    // mips.sinOptimizar() y mips.optimizaciones() sirven para la demostración.
} else {
    // Mostrar mips.errores(); limpiar cualquier ensamblador de una compilación anterior.
}
```

`generar(analisis, false)` desactiva la mirilla. Cada invocación crea un backend
nuevo; no se comparten etiquetas, tipos, datos o registros entre compilaciones.
Si el análisis falla o el backend encuentra una operación no soportada, ambos
campos de código quedan vacíos. Los diagnósticos propios del backend son cadenas;
no se mezclan con los diagnósticos del lexer/parser/semántico.

El backend recibe únicamente instrucciones TAC estructuradas, firmas,
`RegistroActivacion`, `DescriptorClase` y enlaces semánticos. No traduce el AST,
no interpreta el texto de `codigoTAC()` y no introduce otro parser.

## División de archivos e integración

- `GeneradorFuncionesMIPS`, `AsignadorRegistros`, `MarcoPilaMIPS`: base de Alejandro,
  ampliada con llamadas virtuales y comprobación de división por cero.
- `GeneradorEstructurasMIPS`: arreglos, objetos, campos, vtables, llamadas de métodos,
  seguimiento de tipos y `try/catch` de Jorge.
- `RuntimeMIPS`: mensajes y propagación de excepciones.
- `OptimizadorMirilla`: nueve reglas y contadores por regla.
- `GeneradorMIPS`: fachada lista para consumir desde el IDE.
- `TraductorTACProvisional`: puente que permite probar todo el backend ahora.
  Ruth puede conservarlo o reemplazar su bucle por `TraductorTAC`.
- `CompilarMIPS`: CLI para generar `.asm` optimizado y una copia sin optimizar.

Para reemplazar el puente:

1. Crear **un solo** `GeneradorFuncionesMIPS` y `GeneradorEstructurasMIPS(g, resultado)`
   por compilación. El constructor instala el runtime y las vtables una sola vez.
2. Separar cuerpos de funciones del código global como hace el puente. Usar los
   layouts de `resultado.registros()` y los parámetros ordenados por índice TAC.
3. Después de iniciar cada función, llamar `estructuras.iniciarAmbito(nombre)`;
   después de iniciar main, llamar `estructuras.iniciarAmbito(null)`.
4. Por instrucción, intentar `estructuras.traducir(instruccion)`. Si devuelve
   `true`, ya quedó traducida y actualizó sus tipos. Si devuelve `false`, traducir
   la instrucción básica y después llamar `estructuras.observar(instruccion)`.
   El puente muestra el uso de `validarBasica()` y de `tipo()` para imprimir cadenas.
5. Para `/` y `%`, usar `g.binaria()` o emitir comprobaciones equivalentes de cero
   **después de sincronizar valores vivos**. Antes de cualquier operación que pueda
   lanzar, guardar valores sucios; el catch vuelve a cargar desde memoria.
6. Cerrar cada función; obtener `g.ensamblar()` y aplicar la mirilla al texto final.
   Mantener los datos y helpers de `RuntimeMIPS`, que `ensamblar()` agrega automáticamente.

`g.llamadaMetodo()` comprueba receptor nulo también en llamadas directas.
`g.llamadaVirtual()` recibe la ranura lógica y hace `jalr` sobre la vtable real.
No convertir un `vcall` en `jal` a la clase estática.

## Representaciones y comprobaciones

Destino probado: **MARS 4.5, MIPS32, pseudoinstrucciones habilitadas y delayed
branching desactivado**. Palabras y referencias de 4 bytes; enteros de 32 bits;
booleanos 0/1; `null` es 0; cadenas son punteros a `.asciiz`.
La impresión básica de booleanos sigue siendo 0/1.

- Arreglo: longitud en byte 0; elementos desde byte 4. Un elemento puede ser otra
  referencia, por lo que los arreglos multidimensionales son arreglos de punteros.
  Lectura y escritura comprueban `0 <= indice < longitud`; length comprueba null.
  La reserva inicializa las palabras a cero y los literales TAC escriben sus valores.
- Objeto: puntero a vtable en byte 0. Un campo con offset lógico `k` usa byte `4*k`.
  Los campos heredados conservan su posición. Las tablas respetan las ranuras de
  `DescriptorClase`; las sobrescrituras cambian el destino de esa ranura.
  Los campos se inicializan a cero; las llamadas TAC a `$init` y al constructor
  realizan los inicializadores explícitos, incluidos los heredados.
- Cadena: se preserva el puntero en asignaciones, campos, parámetros y retornos.
  La impresión utiliza syscall 4 según el tipo. Un puntero nulo produce una
  excepción del runtime. Se usa la syscall 9 para arreglos y objetos.

Los casos actuales de float se rechazan explícitamente: no se disfrazan como
enteros o nombres globales. Operaciones de cadenas que no implementa el traductor
básico también fallan explícitamente. La representación flotante y cualquier
ampliación de operaciones básicas quedan para el traductor de Ruth si el alcance
final del curso las exige. Las funciones léxicamente anidadas siguen rechazadas
por el módulo de funciones; la recursión y las llamadas anidadas sí se ejecutan.

## Excepciones y limpieza

Hay manejadores enlazados en la pila. Cada `INICIO_TRY` reserva 56 bytes y guarda:

| Byte | Valor |
| --- | --- |
| 0 | Manejador anterior |
| 4 | Dirección de la etiqueta catch |
| 8 | `$sp` antes de instalar el manejador |
| 12 | `$fp` de la función que contiene el try |
| 16 | `$ra` al instalar el manejador |
| 20–48 | `$s0`–`$s7` |

`FIN_TRY` restaura `$sp` y el enlace anterior. No se acumulan reservas de heap por
cada iteración de un ciclo. El TAC ya emite `FIN_TRY` al salir con return, break y
continue; el backend conserva esas instrucciones.

Ante división/módulo por cero, índice inválido o referencia nula, el runtime guarda
el mensaje en `__rt_exception`, retira el manejador y restaura los registros y la
pila del catch, incluso si atravesó varias llamadas. `CAPTURA` carga el mensaje
como cadena. Un error dentro del catch puede propagarse al manejador exterior.
Sin manejador, se imprime el mensaje y se termina con código 1 (syscall 17).
La gramática actual no incluye una sentencia `throw`; se propagan los errores de
runtime representados por estas comprobaciones.

## Mirilla: nueve reglas demostrables

Las reglas son locales; no se eliminan stores, loads, llamadas o etiquetas, ni
se supone que una variable esté muerta. No se eliminan operaciones `addi` que
puedan producir overflow. No se reemplazan multiplicaciones que puedan afectar
HI/LO. Se aplica hasta llegar a un punto fijo, con contadores por regla.

| Regla | Antes | Después |
| --- | --- | --- |
| move-identico | `move $t0, $t0` | eliminado |
| addiu-cero | `addiu $t0, $t1, 0` | `move $t0, $t1` |
| addu-cero | `addu $t0, $t1, $zero` | `move $t0, $t1` |
| subu-cero | `subu $t0, $t1, $zero` | `move $t0, $t1` |
| or-cero | `or $t0, $t1, $zero` | `move $t0, $t1` |
| shift-cero | `sll/srl/sra $t0, $t1, 0` | `move $t0, $t1` |
| salto-siguiente | `j L` seguido de `L:` (solo líneas vacías entre ambos) | solo `L:` |
| li-repetido | dos `li` idénticos consecutivos | un `li` |
| li-cero | `li $t0, 0` | `move $t0, $zero` |

`addu` y `or` también admiten cero a la izquierda. La regla de salto depende de
que delayed branching esté desactivado. Los datos `.asciiz` no se modifican.
Las nueve reglas tienen comparación de salida real en MARS; sus pares de ensamblador
y salidas se guardan en `target/mips/mirilla/`. La suite avanzada
también ejecuta cada programa con y sin mirilla. `examples/mips/mirilla_demo.cps`
activa al menos seis reglas distintas desde TAC real; una prueba exige ese mínimo.

## Verificación reproducible y evidencia

Requisitos: JDK 17+, Maven, bash, curl y shasum.

```sh
./scripts/verificar-mips.sh
```

El script descarga la distribución oficial de MARS en `.cache/mars/` si hace falta,
verifica su SHA-256, ejecuta una compilación limpia y todas las pruebas con `MARS_JAR` configurado, genera seis
pares de ensambladores y compara las salidas entre sí y con archivos `.expected`.
El JAR no se versiona. Los binarios de esta verificación usan `target/mips-build/`
para evitar interferencia con la compilación automática de VS Code en `target/classes/`.
También se puede proporcionar un MARS existente:

```sh
MARS_JAR=/ruta/Mars4_5.jar ./scripts/verificar-mips.sh
```

Los `.asm`, sus versiones sin optimizar y las salidas quedan en `target/mips/`.
Para compilar un archivo individual después de `mvn compile`:

```sh
mvn -q org.codehaus.mojo:exec-maven-plugin:3.2.0:java -Dexec.mainClass=mips.CompilarMIPS \
  -Dexec.args="examples/mips/objetos_herencia.cps target/objetos.asm"
```

La suite cubre arreglos multidimensionales/foreach, índices negativos y fuera de
rango, objetos con campos de referencia, inicializadores, constructores, herencia,
`vcall` con argumentos en pila, referencias nulas, try/catch anidados, excepciones
recursivas, return/break/continue, limpieza de manejadores en ciclos y errores sin
catch. Se mantienen las pruebas anteriores del compilador.

## Resultado de la validación

El 14 de noviembre de 2026, `./scripts/verificar-mips.sh` completó **327 pruebas**
sin fallos, errores ni omisiones. Los seis programas produjeron las salidas
esperadas, iguales con y sin mirilla. La demo desde TAC activó seis reglas distintas:
move idéntico, suma con cero, resta con cero, salto a la siguiente etiqueta,
carga inmediata repetida y carga de cero.

## Pendiente de Ruth

Conectar la API al IDE: pestaña MIPS, mostrar diagnósticos, limpiar resultados
anteriores al fallar, copiar/guardar `.asm` y verificar el flujo completo de la
interfaz. Revisar la cobertura de sus operaciones básicas y completar el traductor
definitivo si decide reemplazar el provisional. La parte de Jorge se puede probar
sin la interfaz y sin esperar cambios de esos archivos.
