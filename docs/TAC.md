# Representación TAC

## Expresiones y asignaciones

`InstruccionTAC` conserva el constructor de operaciones existente:
`(operador, argumento1, argumento2, resultado)`.

```text
t0 = a + b
t1 = -a
x = t0
```

Los operandos se representan como texto. El administrador de temporales crea
`t0`, `t1`, etc. y recicla valores liberados. La reserva de nombres para evitar colisiones se implementará en el bloque 5;
la asociación con símbolos semánticos se completará en el bloque 6.

## Control condicional

Las instrucciones tienen un `Tipo`: `OPERACION`, `ETIQUETA`, `SALTO` o
`SALTO_CONDICIONAL`. Para control, `resultado()` contiene la etiqueta definida
o el destino del salto; `argumento1()` contiene la condición si corresponde.
Las fábricas `etiqueta`, `salto` y `saltoCondicional` evitan imprimirlas como asignaciones.

```text
L0:
goto L1
if t0 goto L0
```

`if condición goto destino` salta cuando la condición booleana es verdadera.
Las etiquetas son únicas dentro de un `GeneradorTAC`, no se reciclan y se
reinician junto con instrucciones y temporales al llamar `limpiar()`.

Por ejemplo, `examples/tac/if_else.cps` produce:

```text
x = 0
t0 = x < 2
if t0 goto L0
goto L1
L0:
t0 = x + 1
x = t0
goto L2
L1:
x = 3
L2:
```

La condición se consume antes de liberar su temporal. El salto al final evita
ejecutar el `else` después de la rama verdadera. Sin `else`, el destino falso
es directamente la etiqueta final. Los bloques se recorren con el visitor de
sentencias, por lo que los condicionales anidados usan el mismo emisor.

## Uso e integración

Después de validar léxico, sintaxis y semántica, pasar `parser.program()` a:

```java
GeneradorSentenciasTAC visitor = new GeneradorSentenciasTAC();
visitor.visit(arbolValidado);
String texto = visitor.codigo();
```

También acepta un `GeneradorTAC` externo. Internamente lo comparte con
`GeneradorExpresionesTAC`; ambos mantienen el mismo orden de instrucciones
y administrador de temporales. El constructor sin argumentos de expresiones
continúa disponible para compatibilidad.

El visitor de bajo nivel no valida por sí
mismo tipos ni errores del parser. Por ahora soporta variables, asignaciones
y expresiones de la base existente, bloques, `if/else`, `while`, `do-while`,
`for`, `switch/case/default`, funciones globales, llamadas, retornos, `break`
y `continue`. Otras sentencias, incluidos `foreach`, provocan `UnsupportedOperationException` para
evitar traducir sus cuerpos como ejecución lineal. Las limitaciones de
expresiones heredadas se detallan en `CHECKLIST_PERSONA_2.md`; en particular,
ternario, llamadas a métodos y cortocircuito siguen pendientes.

## Verificación

```bash
mvn -Dtest=ControlCondicionalTACTest test
mvn test
```

Las pruebas nuevas verifican formato, orden de saltos y ramas, destinos existentes,
unicidad de etiquetas, reinicio, emisor compartido, temporales y rechazo de ciclos
aún no implementados. `CiclosTACTest` incorpora un intérprete limitado a las
operaciones utilizadas en sus casos para verificar los resultados del TAC.

## Ciclos y saltos (bloque 2)

| Construcción | Orden | Destino de `continue` |
|---|---|---|
| `while` | Condición, cuerpo, regreso a condición | Condición |
| `do-while` | Cuerpo, condición, repetición o salida | Condición después del cuerpo |
| `for` | Inicialización, condición, cuerpo, actualización, regreso | Actualización |

`break` salta a la salida del contexto más cercano. Una pila conserva los
contextos de estructuras anidadas y se restaura con `finally`, incluso si la
traducción del cuerpo falla. `continue` busca el contexto más cercano que tenga
destino de continuación. Esto permite agregar posteriormente un contexto de
`switch` con continuación nula, sin ocultar el ciclo exterior.

En `for`, la condición omitida se trata como verdadera; inicialización y
actualización también son opcionales. Las expresiones se identifican por su
posición respecto al separador, evitando confundir una actualización con una
condición omitida. La gramática requiere `i = i + 1`, no admite `i++`.
El resultado temporal de una asignación dentro de una expresión se libera
después de copiarlo al destino, también para la actualización del `for`.

El generador rechaza `break` y `continue` sin contexto mediante
`IllegalStateException`, aunque el consumidor haya omitido la validación previa.
Si falla una traducción, el TAC parcial debe descartarse o limpiarse antes de
volver a compilar. No se implementa rollback de instrucciones.

Ejemplo combinado: `examples/tac/ciclos.cps`. Pruebas del bloque:

```bash
mvn -Dtest=CiclosTACTest test
```

## Switch (bloque 3)

El selector se evalúa una sola vez y se copia a un temporal reservado. Los
`case` se comparan con `==` en orden de aparición; la primera coincidencia
salta al cuerpo correspondiente. Las expresiones de casos posteriores a una
coincidencia no se evalúan. La gramática permite expresiones en los `case`,
incluidas asignaciones; el temporal conserva el selector original frente a sus
efectos secundarios. Si ninguna comparación coincide, se salta a `default`
o directamente a la salida si no existe.

Los cuerpos se emiten consecutivamente: sin `break`, la ejecución cae al
siguiente caso y finalmente a `default`, sin repetir comparaciones. `break`
sale del switch más cercano; `continue` busca el ciclo exterior más cercano.
Un ciclo dentro de un switch mantiene sus propios destinos de salto.

Después del despacho se libera el temporal del selector, pues los cuerpos ya
no lo necesitan. Los contextos se restauran incluso si falla la traducción.
El análisis semántico admite `break` dentro de ciclos o switches y mantiene
`continue` exclusivo de ciclos. Las funciones anidadas no heredan permisos
de salto de estructuras externas.

Se conserva la comprobación semántica de tipos compatibles entre selector y
casos. No se agrega validación de casos duplicados: la primera coincidencia
determina el punto de entrada y puede haber caída a los siguientes cuerpos.

Ejemplo: `examples/tac/switch.cps`. Verificación:

```bash
mvn -Dtest=SwitchTACTest test
```

Los nueve tests cubren selección, ausencia de coincidencia, default, switch
vacío, caída entre casos, selector con efectos secundarios, anidamiento,
combinación con ciclos y errores semánticos. Suite completa: 196 pruebas pasan.

El bloque 4 agrega funciones, argumentos, llamadas y retornos; el bloque 5
completará los registros de activación.

`foreach` sigue pendiente del contrato de longitud y acceso a arreglos con Persona 3.

## Funciones y llamadas (bloque 4)

El visitor registra descriptores de las funciones declaradas directamente en
el programa: nombre, nombres/tipos de parámetros y tipo de retorno. Pueden
consultarse mediante `generador.funciones()`. Estos datos son la interfaz inicial
para los layouts de activación del bloque 5. Los tipos se conservan como texto
de la gramática; las validaciones de compatibilidad corresponden al analizador
semántico previo, no a este registro.

```text
goto L0
function sumar
a = param 0
b = param 1
t0 = a + b
return t0
end function sumar
L0:
t0 = 2
t1 = 3
arg t0
arg t1
t2 = call sumar, 2
x = t2
```

- `function nombre` identifica la entrada; un salto permite que el programa
  principal omita el cuerpo de la declaración.
- `nombre = param posición` recibe un argumento por valor; posiciones desde cero.
- Los argumentos se evalúan de izquierda a derecha y se copian a temporales
  antes de evaluar el siguiente. Esto preserva sus valores frente a asignaciones.
- Todos los `arg` de una llamada se emiten después de evaluar sus argumentos,
  incluidas las llamadas internas. `call nombre, cantidad` consume exactamente
  esos argumentos; no deja argumentos pendientes del llamador en una llamada interna.
- Una llamada con valor guarda su retorno en un temporal; una función sin tipo
  de retorno es `void` y su llamada no crea temporal de resultado.
- `return valor` devuelve y termina la invocación; `return` termina sin valor.
  Se agrega retorno implícito únicamente para funciones `void`.
- `end function` delimita el cuerpo; no representa un retorno válido de una función
  con valor. Estas funciones deben garantizar un `return`.

La comprobación de retorno es conservadora: reconoce retornos directos,
bloques y `if/else` cuyas dos ramas retornan. No prueba que un ciclo o switch
retorne en todos los caminos; usar un retorno final después de esas estructuras.

En estas instrucciones, `resultado()` contiene el nombre para `FUNCION` y
`FIN_FUNCION`, el parámetro para `PARAMETRO` y el temporal (o null) para `LLAMADA`.
`argumento1()` contiene posición, valor de argumento, nombre invocado o valor
retornado, según el tipo. `argumento2()` contiene la cantidad en `LLAMADA`.

Los temporales y locales deben vivir en el marco de cada invocación, incluso
si tienen nombres iguales en distintas funciones. El bloque 4 define el contrato de llamadas. Los layouts y la pila de activaciones
se implementarán en el bloque 5. Las pruebas verifican llamadas anidadas y
factorial con memorias independientes; no modelan aún acceso a globals.

Se admiten llamadas directas a funciones globales y recursión directa. Se
rechazan funciones anidadas, métodos, encadenamiento de llamadas y accesos
compuestos. El registro TAC no autoriza referencias adelantadas: el programa
debe pasar la semántica existente, que resuelve declaraciones en orden.

Ejemplo en `examples/tac/funciones.cps`. Pruebas:

```bash
mvn -Dtest=FuncionesTACTest test
mvn test
```
