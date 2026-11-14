function factorial(n: integer): integer {
  if (n <= 1) { return 1; }
  return n * factorial(n - 1);
}
function fib(n: integer): integer {
  if (n < 2) { return n; }
  return fib(n - 1) + fib(n - 2);
}
function sumar(a: integer, b: integer): integer {
  return a + b;
}
function suma6(a: integer, b: integer, c: integer, d: integer, e: integer, f: integer): integer {
  return a + b + c + d + e + f;
}
function saludar() {
  print("hola desde una funcion");
}
print(factorial(5));
print(fib(10));
print(sumar(factorial(5), 3));
print(suma6(1, 2, 3, 4, 5, factorial(3)));
saludar();
