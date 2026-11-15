package mips;

import compiler.Compilador;
import java.nio.file.*;

/** Compila .cps a .asm optimizado y conserva una copia sin optimizar para comparación. */
public final class CompilarMIPS {
    private CompilarMIPS() { }
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Uso: CompilarMIPS entrada.cps salida.asm");
        var salida = new GeneradorMIPS().generar(Compilador.compilar(Files.readString(Path.of(args[0]))));
        if (!salida.esValido()) throw new GeneracionMIPSException(String.join("\n", salida.errores()));
        Path archivo = Path.of(args[1]);
        if (archivo.toAbsolutePath().getParent() != null) Files.createDirectories(archivo.toAbsolutePath().getParent());
        Files.writeString(archivo, salida.codigo());
        Files.writeString(Path.of(args[1] + ".sin-optimizar.asm"), salida.sinOptimizar());
        System.out.println("MIPS: " + archivo + " | mirilla: " + salida.optimizaciones());
    }
}
