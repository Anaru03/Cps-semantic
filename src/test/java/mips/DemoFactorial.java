package mips;

/** Genera factorial + llamadas anidadas "a mano" (como lo haría el traductor TAC) y escribe el .asm. */
public class DemoFactorial {
    public static String generar() {
        GeneradorFuncionesMIPS g = new GeneradorFuncionesMIPS();

        // function factorial(n) { if (n <= 1) return 1; return n * factorial(n - 1); }
        g.iniciarFuncion("factorial", java.util.List.of("n"), java.util.List.of());
        g.saltoSi("<=", "n", "1", "L_base");
        g.binaria("-", "t1", "n", "1");
        g.argumento("t1");
        g.llamada("t2", GeneradorFuncionesMIPS.etiquetaDe("factorial"), 1);
        g.binaria("*", "t3", "n", "t2");
        g.retorno("t3");
        g.etiqueta("L_base");
        g.retorno("1");
        g.finFuncion();

        // function suma6(a,b,c,d,e,f) { return a+b+c+d+e+f; }   (2 argumentos por pila)
        g.iniciarFuncion("suma6", java.util.List.of("a", "b", "c", "d", "e", "f"), java.util.List.of());
        g.binaria("+", "t1", "a", "b");
        g.binaria("+", "t1", "t1", "c");
        g.binaria("+", "t1", "t1", "d");
        g.binaria("+", "t1", "t1", "e");
        g.binaria("+", "t1", "t1", "f");
        g.retorno("t1");
        g.finFuncion();

        // let r = factorial(5); print(r); print(suma6(1,2,3,4,5,factorial(3)));
        g.iniciarMain(java.util.List.of("r"));
        g.argumento("5");
        g.llamada("t1", GeneradorFuncionesMIPS.etiquetaDe("factorial"), 1);
        g.copiar("r", "t1");
        g.imprimirEntero("r");
        g.argumento("3");
        g.llamada("t2", GeneradorFuncionesMIPS.etiquetaDe("factorial"), 1);
        g.argumento("1");
        g.argumento("2");
        g.argumento("3");
        g.argumento("4");
        g.argumento("5");
        g.argumento("t2");
        g.llamada("t3", GeneradorFuncionesMIPS.etiquetaDe("suma6"), 6);
        g.imprimirEntero("t3");
        g.finFuncion();
        return g.ensamblar();
    }

    public static void main(String[] args) throws Exception {
        String asm = generar();
        if (args.length > 0) java.nio.file.Files.writeString(java.nio.file.Path.of(args[0]), asm);
        else System.out.println(asm);
    }
}
