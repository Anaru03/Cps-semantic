let suma: integer = 0;
let i: integer = 0;
for (i = 0; i < 6; i = i + 1) {
    if (i == 1) { continue; }
    if (i == 4) { break; }
    suma = suma + i;
}
// Al salir: suma = 5, i = 4.
while (i < 6) {
    i = i + 1;
}
do {
    i = i - 1;
} while (i < 0);
// Valores finales: suma = 5, i = 5.
