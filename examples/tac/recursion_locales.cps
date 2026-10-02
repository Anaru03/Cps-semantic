let llamadas: integer = 0;
function suma(n: integer): integer {
    llamadas = llamadas + 1;
    let guardado: integer = n;
    if (n <= 0) { return 0; }
    let resto: integer = suma(n - 1);
    return guardado + resto;
}
let resultado: integer = suma(5);
// Cada invocación conserva guardado y resto propios.
// Resultado esperado: resultado = 15, llamadas = 6.
