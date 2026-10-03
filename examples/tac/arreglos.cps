let xs: integer[] = [10, 20, 30];
xs[1] = xs[0] + xs[2];
let m: integer[][] = [[1, 2], [3, 4]];
let suma = 0;
foreach (fila in m) {
  foreach (v in fila) { suma = suma + v; }
}
print(suma + xs.length);
