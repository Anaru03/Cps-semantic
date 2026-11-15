package mips;

import org.junit.jupiter.api.Assumptions;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

final class MarsTestSupport {
    private MarsTestSupport() { }

    static String ejecutar(String asm) throws Exception {
        String jar = System.getenv("MARS_JAR");
        Assumptions.assumeTrue(jar != null && Files.isRegularFile(Path.of(jar)),
                "MARS_JAR no configurado");
        Path fuente = Files.createTempFile("cps-mips-", ".asm");
        Path salida = Files.createTempFile("cps-mars-", ".txt");
        Process proceso = null;
        try {
            Files.writeString(fuente, asm);
            String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
            proceso = new ProcessBuilder(java, "-Djava.awt.headless=true", "-jar", jar,
                    "nc", "ae1", "se2", "1000000", fuente.toString())
                    .redirectErrorStream(true).redirectOutput(salida.toFile()).start();
            assertTrue(proceso.waitFor(30, TimeUnit.SECONDS), "MARS excedió 30 segundos");
            String texto = Files.readString(salida).replace("\r", "");
            assertEquals(0, proceso.exitValue(), texto);
            // MARS agrega una línea vacía al terminar la simulación.
            return texto.replaceFirst("\\n\\n$", "\n");
        } finally {
            if (proceso != null && proceso.isAlive()) {
                proceso.destroyForcibly().waitFor();
            }
            Files.deleteIfExists(fuente);
            Files.deleteIfExists(salida);
        }
    }
}
