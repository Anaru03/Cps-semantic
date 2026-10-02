function sumar(a: integer, b: integer): integer {
    return a + b;
}

function factorial(n: integer): integer {
    if (n <= 1) { return 1; }
    return n * factorial(n - 1);
}

function sinValor() { return; }

let resultado: integer = sumar(factorial(5), 3);
sinValor();
// Resultado esperado según la convención de llamadas: 123.
