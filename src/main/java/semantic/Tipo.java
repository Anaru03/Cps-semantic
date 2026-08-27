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
}