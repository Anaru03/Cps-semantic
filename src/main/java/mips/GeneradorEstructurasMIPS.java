package mips;

import compiler.ResultadoCompilacion;
import tac.*;
import java.util.*;

/** Traducción de estructuras desde TAC; no consulta ni recorre el AST. */
public final class GeneradorEstructurasMIPS {
    private final GeneradorFuncionesMIPS g;
    private final ResultadoCompilacion resultado;
    private final Map<String, String> declarados = new HashMap<>();
    private final Map<String, String> tipos = new HashMap<>();
    private int secuencia;

    public GeneradorEstructurasMIPS(GeneradorFuncionesMIPS g, ResultadoCompilacion resultado) {
        this.g = g;
        this.resultado = resultado;
        g.habilitarRuntime();
        for (var e : resultado.enlaces()) declarados.put(e.operando(), e.referencia().simbolo().tipo().toString());
        var representaciones = new ArrayList<>(declarados.values());
        resultado.registros().values().forEach(r -> r.posiciones().forEach(p -> representaciones.add(p.tipo())));
        resultado.funciones().values().forEach(f -> representaciones.add(f.tipoRetorno()));
        resultado.clases().values().forEach(c -> c.campos().forEach(f -> representaciones.add(f.tipo())));
        if (representaciones.stream().anyMatch(t -> t.equals("float") || t.startsWith("float[")))
            throw new GeneracionMIPSException("Representación float pendiente en el backend entero MIPS");
        for (var c : resultado.clases().values()) {
            g.agregarDato(".align 2");
            var metodos = c.tabla().stream().sorted(Comparator.comparingInt(DescriptorClase.Metodo::ranura))
                    .map(m -> GeneradorFuncionesMIPS.etiquetaDe(m.etiqueta())).toList();
            g.agregarDato(vtable(c.nombre()) + ": .word " + (metodos.isEmpty() ? "0" : String.join(", ", metodos)));
        }
    }

    /** Reinicia tipos de temporales (sus nombres se reutilizan entre funciones). */
    public void iniciarAmbito(String funcion) {
        tipos.clear();
        tipos.putAll(declarados);
        var layout = resultado.registros().get(funcion);
        if (layout != null) for (var p : layout.posiciones()) {
            if (!p.tipo().equals("unknown") && !p.tipo().equals("dynamic")) tipos.put(p.operando(), p.tipo());
        }
    }

    public String tipo(String operando) {
        if (operando == null) return "void";
        if (AsignadorRegistros.esCadena(operando)) return "string";
        if (operando.equals("true") || operando.equals("false")) return "boolean";
        if (operando.matches("-?\\d+")) return "integer";
        return tipos.getOrDefault(operando, "unknown");
    }

    /** Fallar explícitamente ante representaciones que el backend básico aún no implementa. */
    public void validarBasica(InstruccionTAC x) {
        if (x.tipo() != InstruccionTAC.Tipo.OPERACION && x.tipo() != InstruccionTAC.Tipo.IMPRIMIR) return;
        for (String op : Arrays.asList(x.argumento1(), x.argumento2(), x.resultado())) {
            if (tipo(op).equals("float")) throw new GeneracionMIPSException("Representación float pendiente: " + x);
        }
        if (x.tipo() == InstruccionTAC.Tipo.OPERACION && !x.operador().isEmpty()
                && (tipo(x.argumento1()).equals("string") || tipo(x.argumento2()).equals("string")))
            throw new GeneracionMIPSException("Operación de cadenas pendiente: " + x);
    }

    /** Se llama también para instrucciones básicas, para seguir copias y retornos tipados. */
    public void observar(InstruccionTAC x) {
        if (x.resultado() == null) return;
        String t = switch (x.tipo()) {
            case OPERACION -> x.operador().isEmpty() ? tipo(x.argumento1())
                    : Set.of("<", ">", "<=", ">=", "==", "!=", "!", "&&", "||").contains(x.operador()) ? "boolean" : "integer";
            case NUEVO_OBJETO -> x.argumento1();
            case NUEVO_ARREGLO -> "unknown[]";
            case LONGITUD_ARREGLO -> "integer";
            case LECTURA_ARREGLO -> {
                String a = tipo(x.argumento1());
                yield a.endsWith("[]") ? a.substring(0, a.length() - 2) : "unknown";
            }
            case LECTURA_CAMPO -> campo(x.argumento1(), x.argumento2()).tipo();
            case CAPTURA -> "string";
            case LLAMADA -> {
                var f = resultado.funciones().get(x.argumento1());
                yield f == null ? "unknown" : f.tipoRetorno();
            }
            default -> null;
        };
        if (t != null) tipos.put(x.resultado(), declarados.getOrDefault(x.resultado(), t));
    }

    private DescriptorClase.Campo campo(String objeto, String nombre) {
        var clase = resultado.clases().get(tipo(objeto));
        if (clase != null && clase.campo(nombre) != null) return clase.campo(nombre);
        // TAC puede reciclar un temporal con tipo dinámico. Solo inferir cuando
        // todos los layouts candidatos coinciden en offset y representación.
        var campos = resultado.clases().values().stream().map(c -> c.campo(nombre)).filter(Objects::nonNull).toList();
        if (!campos.isEmpty() && campos.stream().allMatch(c -> c.offset() == campos.get(0).offset()
                && c.tipo().equals(campos.get(0).tipo()))) return campos.get(0);
        throw new GeneracionMIPSException("Campo sin layout inequívoco: " + objeto + "." + nombre);
    }

    private static String vtable(String clase) { return "__rt_vtable_" + clase; }
    private void e(String s) { g.emitir(s); }
    private String fuente(String s) { return g.registros().getReg(s); }
    private String destino(String s) { return g.registros().getRegDestino(s); }
    private void comprobarNulo(String r) { e("beq " + r + ", $zero, __rt_null"); }

    /** Devuelve false si la instrucción pertenece al traductor básico de Ruth. */
    public boolean traducir(InstruccionTAC x) {
        switch (x.tipo()) {
            case NUEVO_ARREGLO, LONGITUD_ARREGLO, LECTURA_ARREGLO, ESCRITURA_ARREGLO,
                 NUEVO_OBJETO, LECTURA_CAMPO, ESCRITURA_CAMPO, INICIO_TRY, FIN_TRY, CAPTURA -> { }
            case LLAMADA -> {
                if (!x.operador().equals("vcall")) {
                    var f = resultado.funciones().get(x.argumento1());
                    if (f == null || f.parametros().isEmpty() || !f.parametros().get(0).nombre().equals("this")) return false;
                    g.llamadaMetodo(x.resultado(), GeneradorFuncionesMIPS.etiquetaDe(x.argumento1()),
                            Integer.parseInt(x.argumento2()));
                    observar(x);
                    return true;
                }
                String etiqueta = x.argumento1();
                int punto = etiqueta.lastIndexOf('.');
                var c = resultado.clases().get(etiqueta.substring(0, punto));
                var m = c == null ? null : c.metodo(etiqueta.substring(punto + 1));
                if (m == null) throw new GeneracionMIPSException("Método virtual desconocido: " + etiqueta);
                g.llamadaVirtual(x.resultado(), Integer.parseInt(x.argumento2()), m.ranura());
                observar(x);
                return true;
            }
            default -> { return false; }
        }
        // Cualquier comprobación puede abandonar el bloque: guardar primero los
        // valores vivos, incluidas globales modificadas y locales del catch.
        g.registros().invalidar();
        switch (x.tipo()) {
            case NUEVO_ARREGLO -> {
                int cantidad = Integer.parseInt(x.argumento1());
                if (cantidad < 0 || cantidad > (Integer.MAX_VALUE / 4) - 1)
                    throw new GeneracionMIPSException("Tamaño de arreglo inválido: " + cantidad);
                e("li $a0, " + ((cantidad + 1) * 4));
                e("li $v0, 9"); e("syscall");
                e("li $v1, " + cantidad); e("sw $v1, 0($v0)");
                inicializar(4, (cantidad + 1) * 4);
                e("move " + destino(x.resultado()) + ", $v0");
            }
            case NUEVO_OBJETO -> {
                var c = resultado.clases().get(x.argumento1());
                if (c == null) throw new GeneracionMIPSException("Clase desconocida: " + x.argumento1());
                e("li $a0, " + c.tamano() * 4); e("li $v0, 9"); e("syscall");
                e("la $v1, " + vtable(c.nombre())); e("sw $v1, 0($v0)");
                inicializar(4, c.tamano() * 4);
                e("move " + destino(x.resultado()) + ", $v0");
            }
            case LONGITUD_ARREGLO -> {
                String r = fuente(x.argumento1()); comprobarNulo(r);
                e("lw " + destino(x.resultado()) + ", 0(" + r + ")");
            }
            case LECTURA_ARREGLO, ESCRITURA_ARREGLO -> {
                boolean lectura = x.tipo() == InstruccionTAC.Tipo.LECTURA_ARREGLO;
                String r = fuente(lectura ? x.argumento1() : x.resultado());
                String i = fuente(lectura ? x.argumento2() : x.argumento1());
                comprobarNulo(r); e("bltz " + i + ", __rt_index");
                e("lw $v1, 0(" + r + ")"); e("bge " + i + ", $v1, __rt_index");
                e("sll $v1, " + i + ", 2"); e("addu $v1, " + r + ", $v1");
                if (lectura) e("lw " + destino(x.resultado()) + ", 4($v1)");
                else e("sw " + fuente(x.argumento2()) + ", 4($v1)");
            }
            case LECTURA_CAMPO, ESCRITURA_CAMPO -> {
                boolean lectura = x.tipo() == InstruccionTAC.Tipo.LECTURA_CAMPO;
                String objeto = lectura ? x.argumento1() : x.resultado();
                var c = campo(objeto, lectura ? x.argumento2() : x.argumento1());
                String r = fuente(objeto); comprobarNulo(r);
                if (lectura) e("lw " + destino(x.resultado()) + ", " + c.offset() * 4 + "(" + r + ")");
                else e("sw " + fuente(x.argumento2()) + ", " + c.offset() * 4 + "(" + r + ")");
            }
            case INICIO_TRY -> {
                e("addiu $sp, $sp, -56");
                e("move $v0, $sp");
                e("lw $v1, __rt_handler"); e("sw $v1, 0($v0)");
                e("la $v1, " + x.resultado()); e("sw $v1, 4($v0)");
                e("addiu $v1, $sp, 56"); e("sw $v1, 8($v0)");
                e("sw $fp, 12($v0)"); e("sw $ra, 16($v0)");
                for (int i = 0; i < 8; i++) e("sw $s" + i + ", " + (20 + i * 4) + "($v0)");
                e("sw $v0, __rt_handler");
            }
            case FIN_TRY -> {
                e("lw $v1, __rt_handler"); e("lw $sp, 8($v1)");
                e("lw $v1, 0($v1)"); e("sw $v1, __rt_handler");
            }
            case CAPTURA -> e("lw " + destino(x.resultado()) + ", __rt_exception");
            default -> throw new AssertionError(x.tipo());
        }
        g.registros().liberarBloqueos();
        observar(x);
        return true;
    }

    private void inicializar(int desde, int hasta) {
        if (desde == hasta) return;
        String bucle = "__rt_zero_" + secuencia++, fin = bucle + "_fin";
        e("addiu $a1, $v0, " + desde); e("li $a2, " + (hasta - desde));
        e(bucle + ":"); e("beq $a2, $zero, " + fin);
        e("sw $zero, 0($a1)"); e("addiu $a1, $a1, 4"); e("addiu $a2, $a2, -4"); e("j " + bucle);
        e(fin + ":");
    }
}
