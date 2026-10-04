# Diagrama del árbol sintáctico

La pestaña «Árbol sintáctico» utiliza `DiagramaArbol`, un lienzo vectorial Swing
con nodos y conexiones. Se conserva el parse tree completo de ANTLR, incluidos
reglas, tokens y EOF; plegar ramas solo cambia su visualización.

## Controles

- `+` / `−`: zoom, desde 2% hasta 300%. `100%` recupera el tamaño de lectura.
- `Panorama`: ajusta el árbol visible al área disponible; para árboles enormes
  sirve como orientación, no como sustituto del zoom para leer los nodos.
- `Inicio`: centra la raíz sin cambiar el zoom.
- `Plegar`: cierra ramas desde el primer nivel.
- Doble clic en un nodo: abre/cierra su rama inmediata. `+` en el nodo indica
  que hay hijos plegados; `−` indica una rama abierta.
- Arrastrar el lienzo: desplazar la vista. También hay barras de desplazamiento.
- Rueda: desplazamiento vertical; Shift + rueda: horizontal; Ctrl/⌘ + rueda: zoom.
- Buscar + Enter: recorre coincidencias en todo el árbol, abre la ruta al nodo,
  lo selecciona y centra; recupera zoom de lectura si estaba en panorama.

Los tokens se distinguen en verde; las reglas usan azul grisáceo y la selección
azul. Las etiquetas largas se acortan en las cajas, pero el texto completo y su
posición fuente se muestran al seleccionar y al pasar el cursor.

## Árboles grandes

La conversión de árbol a modelo se realiza en un `SwingWorker` cancelable: una
compilación nueva descarta resultados antiguos. No se usa recursión para construir
el modelo ni para distribuir nodos, evitando desbordar la pila en árboles profundos.
El layout calcula espacios por subárbol para evitar cajas superpuestas.

Las ramas desde profundidad 3 empiezan plegadas. El dibujo pinta únicamente nodos
y conexiones que intersectan el área visible; no se crea una imagen del tamaño
total del árbol. Los cálculos de layout y la comprobación del área visible siguen
siendo proporcionales a los nodos desplegados, y el modelo completo ocupa memoria
proporcional al árbol. Para árboles muy amplios, plegar y buscar permite explorar
una rama a tamaño legible en lugar de reducirlo todo a texto diminuto.

La preparación del diagrama ocurre en segundo plano; el análisis del compilador
mantiene el comportamiento de ejecución existente del IDE.

## Verificación

Los tests comprueban orden de tokens, modelo completo, layout con 15 000 niveles,
3 000 declaraciones, separación de ramas, plegado, búsqueda, zoom y renderizado
sin ventana nativa. Maven configura Swing en modo headless únicamente para tests.

```bash
mvn test
mvn exec:java
```

Para revisión visual, compilar en el IDE, abrir la pestaña del árbol y probar
zoom, búsqueda y doble clic sobre un programa grande.
