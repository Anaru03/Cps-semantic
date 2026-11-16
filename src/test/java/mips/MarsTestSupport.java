
package mips;

import org.junit.jupiter.api.Assumptions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

final class MarsTestSupport {

    private MarsTestSupport() {
    }

    record Ejecucion(int codigo, String salida) {
    }

    static String ejecutar(String asm) throws Exception {
        var resultado = ejecutarResultado(asm);

        assertEquals(
                0,
                resultado.codigo(),
                resultado.salida()
        );

        return resultado.salida();
    }

    static Ejecucion ejecutarResultado(String asm) throws Exception {
        String jar = System.getenv("MARS_JAR");

        Assumptions.assumeTrue(
                jar != null && Files.isRegularFile(Path.of(jar)),
                "MARS_JAR no configurado"
        );

        Path fuente = Files.createTempFile("cps-mips-", ".asm");
        Path salida = Files.createTempFile("cps-mars-", ".txt");
        Path errores = Files.createTempFile("cps-mars-errores-", ".txt");

        Process proceso = null;

        try {
            Files.writeString(fuente, asm);

            String java = Path.of(
                    System.getProperty("java.home"),
                    "bin",
                    "java"
            ).toString();

            proceso = new ProcessBuilder(
                    java,
                    "-Djava.awt.headless=true",
                    "-jar",
                    jar,
                    "nc",
                    "ae1",
                    "se2",
                    "1000000",
                    fuente.toString()
            )
                    .redirectOutput(salida.toFile())
                    .redirectError(errores.toFile())
                    .start();

            boolean terminado = proceso.waitFor(
                    30,
                    TimeUnit.SECONDS
            );

            assertTrue(
                    terminado,
                    "MARS excedió 30 segundos"
            );

            String stdout = Files.readString(salida)
                    .replace("\r", "");

            String stderr = Files.readString(errores)
                    .replace("\r", "");

            // Normalizar los saltos de línea finales.
            stdout = stdout.replaceFirst("\\n+$", "\n");

            int codigo = proceso.exitValue();

            // Conservar los errores sin agregar líneas vacías.
            if (codigo != 0) {
                String detalle = stderr.isBlank()
                        ? stdout
                        : stdout
                            + (stdout.endsWith("\n") || stdout.isEmpty()
                                ? ""
                                : "\n")
                            + stderr;

                return new Ejecucion(codigo, detalle);
            }

            return new Ejecucion(codigo, stdout);

        } finally {

            if (proceso != null && proceso.isAlive()) {
                proceso.destroyForcibly().waitFor();
            }

            Files.deleteIfExists(fuente);
            Files.deleteIfExists(salida);
            Files.deleteIfExists(errores);
        }
    }
}
