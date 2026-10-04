package tac;

import semantic.InformacionSemantica;

/** Contrato para Persona 3: identidad semántica exacta y almacenamiento TAC. */
public record EnlaceSimboloTAC(InformacionSemantica.Referencia referencia, String operando,
                              String funcion, Integer offset) { }
