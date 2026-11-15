// Operaciones neutrales explícitas para demostrar mirilla desde TAC real.
function identidad(n: integer): integer { return n; }
let x: integer = 7;
x = 7;
x = x;
print(x + 0);
print(x - 0);
let cero: integer = 0;
print(cero);
print(identidad(x));
