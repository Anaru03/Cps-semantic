package semantic;

public enum Tipo {

    INTEGER,
    STRING,
    BOOLEAN,
    NULL,
    ARRAY,
    CLASS,
    VOID,
    UNKNOWN,
    ERROR;

    public boolean esNumerico() {
        return this == INTEGER;
    }

    public boolean esBooleano() {
        return this == BOOLEAN;
    }

    public boolean esError() {
        return this == ERROR;
    }

    public boolean esCompatibleCon(Tipo otro) {
        if (otro == null) {
            return false;
        }

        if (this == ERROR || otro == ERROR) {
            return false;
        }

        if (this == UNKNOWN || otro == UNKNOWN) {
            return false;
        }

        return this == otro;
    }
}