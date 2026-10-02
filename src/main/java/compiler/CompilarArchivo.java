package compiler;

import java.nio.file.*;

/** Demostración de extremo a extremo sin depender de Swing. */
public final class CompilarArchivo {
    public static void main(String[] args) throws java.io.IOException {
        if (args.length != 1) throw new IllegalArgumentException("Uso: CompilarArchivo programa.cps");
        var resultado = Compilador.compilar(Files.readString(Path.of(args[0])));
        if (!resultado.esValido()) {
            resultado.errores().forEach(System.err::println);
            System.exit(1);
        }
        System.out.print(resultado.codigoTAC());
    }
}
