# Funciones, registros y pila MIPS

El módulo de Alejandro recibe TAC estructurado mediante
`TraductorTACProvisional.traducir(ResultadoCompilacion)`. Rechaza compilaciones
inválidas y operaciones todavía no soportadas. El traductor definitivo de Ruth
debe delegar las llamadas y funciones en `GeneradorFuncionesMIPS`.

## Convención

- Palabra de 4 bytes; marcos redondeados a múltiplos de 8 bytes.
- Argumentos en orden de aparición del TAC: los primeros cuatro usan `$a0`–`$a3`;
  los demás se colocan desde `0($sp)` al llamar. El llamador libera esa área.
- Retorno en `$v0`. El llamado preserva `$ra`, `$fp` y los registros `$s` utilizados.
- `$fp` apunta al `$sp` de entrada: `$ra` en `-4($fp)`, enlace dinámico en
  `-8($fp)`, parámetros/locales/temporales desde `-12($fp)`.
- Los offsets lógicos del TAC se convierten a bytes en `MarcoPilaMIPS`.
- El asignador usa registros `$t` y `$s`, derrames LRU y sincronización con
  memoria antes de llamadas y transferencias de control. El retorno explícito
  también sincroniza valores sucios para conservar las globales modificadas.

## Ejecutar las pruebas con MARS

Descargar `Mars4_5.jar` desde la distribución oficial:
https://github.com/dpetersanderson/MARS/releases/tag/v.4.5.1

Con JDK 17 o superior y Maven disponibles:

```sh
MARS_JAR=/ruta/absoluta/Mars4_5.jar mvn clean test
```

Sin `MARS_JAR`, las pruebas de simulador se omiten explícitamente. Con la variable,
se comprueban factorial, Fibonacci, llamadas anidadas, seis argumentos,
recursión con locales y globales modificadas con retornos explícitos.
El proceso usa modo sin ventana, códigos de error de ensamblado/ejecución,
límite de instrucciones y timeout. La salida se compara con valores esperados.

Validación del 14 de noviembre de 2026: MARS 4.5, sin delayed branching.
Arreglos, objetos, excepciones y mirilla están implementados en el módulo de Jorge.
La API pública, las pruebas reproducibles y lo pendiente del IDE se documentan en
[MIPS_INTEGRACION_RUTH.md](MIPS_INTEGRACION_RUTH.md).
