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
`t0`, `t1`, etc. y recicla valores liberados. Sigue pendiente distinguir estos
operandos de variables del usuario con el mismo nombre y enlazarlos con símbolos.

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

Este visitor todavía no es una API completa de compilación: no valida por sí
mismo tipos ni errores del parser. Por ahora soporta variables, asignaciones
y expresiones de la base existente, bloques y `if/else`. Otras sentencias,
incluidos ciclos, switches y funciones, provocan `UnsupportedOperationException` para
evitar traducir sus cuerpos como ejecución lineal. Las limitaciones de
expresiones heredadas se detallan en `CHECKLIST_PERSONA_2.md`; en particular,
ternario, llamadas y cortocircuito siguen pendientes.

## Verificación

```bash
mvn -Dtest=ControlCondicionalTACTest test
mvn test
```

Las pruebas nuevas verifican formato, orden de saltos y ramas, destinos existentes,
unicidad de etiquetas, reinicio, emisor compartido, temporales y rechazo de ciclos
aún no implementados. `CiclosTACTest` incorpora un intérprete limitado a las
operaciones utilizadas en sus casos para verificar los resultados del TAC.

