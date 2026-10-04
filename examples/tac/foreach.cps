function sumar(lista: integer[]): integer {
    let suma: integer = 0;
    foreach (elemento in lista) {
        if (elemento == 2) { continue; }
        if (elemento == 4) { break; }
        suma = suma + elemento;
    }
    return suma;
}
// Con [1, 2, 3, 4, 9], devuelve 4.
// El recorrido está implementado. La creación del arreglo y su llamada
// desde el programa principal se integrarán con el módulo de Persona 3.
