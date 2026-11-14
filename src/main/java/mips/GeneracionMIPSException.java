package mips;

/** Error explícito de generación: nunca se emite ensamblador "aparentemente válido pero incompleto". */
public class GeneracionMIPSException extends RuntimeException {
    public GeneracionMIPSException(String mensaje) {
        super(mensaje);
    }
}
