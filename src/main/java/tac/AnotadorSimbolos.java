package tac;

import semantic.*;
import java.util.*;

/** Completa la tabla de símbolos con direcciones lógicas, etiquetas y tamaños del backend. */
public final class AnotadorSimbolos {
    private AnotadorSimbolos() { }

    public static void anotar(AnalisisSemantico analisis, GeneradorTAC generador) {
        int globales = 0;
        var vistos = Collections.newSetFromMap(new IdentityHashMap<Simbolo, Boolean>());
        for (var enlace : generador.enlacesSimbolos()) {
            Simbolo simbolo = enlace.referencia().simbolo();
            if (!vistos.add(simbolo)) continue;
            if (enlace.funcion() == null) {
                simbolo.asignarAlmacenamiento(new Simbolo.Almacenamiento(Simbolo.Almacenamiento.Clase.GLOBAL,
                        enlace.operando(), null, globales++, null, 1, null));
            } else {
                var clase = simbolo.categoria() == CategoriaSimbolo.PARAMETRO
                        ? Simbolo.Almacenamiento.Clase.PARAMETRO : Simbolo.Almacenamiento.Clase.LOCAL;
                simbolo.asignarAlmacenamiento(new Simbolo.Almacenamiento(clase, enlace.operando(),
                        enlace.funcion(), enlace.offset(), null, 1, null));
            }
        }
        for (var referencia : new LinkedHashSet<>(analisis.informacion().referencias().values())) {
            Simbolo simbolo = referencia.simbolo();
            if (!vistos.add(simbolo)) continue;
            switch (simbolo.categoria()) {
                case FUNCION -> {
                    var registro = generador.registrosActivacion().get(simbolo.nombre());
                    simbolo.asignarAlmacenamiento(new Simbolo.Almacenamiento(Simbolo.Almacenamiento.Clase.FUNCION,
                            null, null, null, simbolo.nombre(), registro == null ? null : registro.cantidadSlots(), null));
                }
                case METODO -> {
                    String clase = nombreClase(referencia.ambito());
                    String etiqueta = clase + "." + simbolo.nombre();
                    var registro = generador.registrosActivacion().get(etiqueta);
                    var descriptor = generador.existeClase(clase) ? generador.clase(clase) : null;
                    var metodo = descriptor == null ? null : descriptor.metodo(simbolo.nombre());
                    simbolo.asignarAlmacenamiento(new Simbolo.Almacenamiento(Simbolo.Almacenamiento.Clase.METODO,
                            null, null, null, etiqueta, registro == null ? null : registro.cantidadSlots(),
                            metodo == null ? null : metodo.ranura()));
                }
                case CLASE -> {
                    var descriptor = generador.existeClase(simbolo.nombre()) ? generador.clase(simbolo.nombre()) : null;
                    simbolo.asignarAlmacenamiento(new Simbolo.Almacenamiento(Simbolo.Almacenamiento.Clase.CLASE,
                            null, null, null, simbolo.nombre(), descriptor == null ? null : descriptor.tamano(), null));
                }
                case ATRIBUTO -> {
                    String clase = nombreClase(referencia.ambito());
                    var descriptor = generador.existeClase(clase) ? generador.clase(clase) : null;
                    var campo = descriptor == null ? null : descriptor.campo(simbolo.nombre());
                    if (campo != null) simbolo.asignarAlmacenamiento(new Simbolo.Almacenamiento(
                            Simbolo.Almacenamiento.Clase.CAMPO, null, clase, campo.offset(), null, 1, null));
                }
                default -> { }
            }
        }
    }

    private static String nombreClase(Ambito ambito) {
        return ambito.nombre().startsWith("clase ") ? ambito.nombre().substring(6) : ambito.nombre();
    }
}
