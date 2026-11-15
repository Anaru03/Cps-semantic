package mips;

import java.util.*;
import java.util.regex.*;

/** Mirilla local MIPS32. Requiere delayed branching desactivado, como el backend. */
public final class OptimizadorMirilla {
    public record Resultado(String codigo, Map<String, Integer> aplicaciones) {
        public Resultado { aplicaciones = Collections.unmodifiableMap(new LinkedHashMap<>(aplicaciones)); }
        public int total() { return aplicaciones.values().stream().mapToInt(Integer::intValue).sum(); }
    }
    public static final List<String> REGLAS = List.of("move-identico", "addiu-cero", "addu-cero",
            "subu-cero", "or-cero", "shift-cero", "salto-siguiente", "li-repetido", "li-cero");
    private static final String R = "(\\$[a-zA-Z0-9]+)";
    private static final Pattern MOVE = Pattern.compile("move\\s+" + R + "\\s*,\\s*" + R);
    private static final Pattern INMEDIATA = Pattern.compile("(addiu|sll|srl|sra)\\s+" + R + "\\s*,\\s*" + R + "\\s*,\\s*0");
    private static final Pattern BINARIA = Pattern.compile("(addu|subu|or)\\s+" + R + "\\s*,\\s*" + R + "\\s*,\\s*" + R);
    private static final Pattern SALTO = Pattern.compile("j\\s+([A-Za-z_][A-Za-z0-9_]*)");
    private static final Pattern LI = Pattern.compile("li\\s+" + R + "\\s*,\\s*(-?\\d+)");

    public Resultado optimizar(String codigo) {
        var lineas = new ArrayList<>(Arrays.asList(codigo.split("\\n", -1)));
        var cuenta = new LinkedHashMap<String, Integer>();
        REGLAS.forEach(r -> cuenta.put(r, 0));
        boolean cambio;
        do {
            cambio = false;
            boolean texto = false;
            for (int i = 0; i < lineas.size(); i++) {
                String l = lineas.get(i).strip();
                if (l.startsWith(".text")) texto = true;
                if (l.startsWith(".data")) texto = false;
                if (!texto) continue;
                String regla = null, reemplazo = null;
                Matcher m = MOVE.matcher(l);
                if (m.matches() && m.group(1).equals(m.group(2))) regla = "move-identico";
                m = INMEDIATA.matcher(l);
                if (regla == null && m.matches()) {
                    regla = m.group(1).equals("addiu") ? "addiu-cero" : "shift-cero";
                    reemplazo = "    move " + m.group(2) + ", " + m.group(3);
                }
                m = BINARIA.matcher(l);
                if (regla == null && m.matches()) {
                    String op = m.group(1), a = m.group(3), b = m.group(4);
                    if (b.equals("$zero") || (!op.equals("subu") && a.equals("$zero"))) {
                        regla = op + "-cero";
                        reemplazo = "    move " + m.group(2) + ", " + (b.equals("$zero") ? a : b);
                    }
                }
                m = SALTO.matcher(l);
                if (regla == null && m.matches()) {
                    int siguiente = i + 1;
                    while (siguiente < lineas.size() && lineas.get(siguiente).isBlank()) siguiente++;
                    if (siguiente < lineas.size() && lineas.get(siguiente).strip().equals(m.group(1) + ":"))
                        regla = "salto-siguiente";
                }
                m = LI.matcher(l);
                if (regla == null && m.matches() && i > 0 && lineas.get(i - 1).strip().equals(l)) regla = "li-repetido";
                if (regla == null && m.matches() && m.group(2).equals("0")) {
                    regla = "li-cero";
                    reemplazo = "    move " + m.group(1) + ", $zero";
                }
                if (regla != null) {
                    cuenta.merge(regla, 1, Integer::sum);
                    if (reemplazo == null) lineas.remove(i--); else lineas.set(i, reemplazo);
                    cambio = true;
                }
            }
        } while (cambio);
        return new Resultado(String.join("\n", lineas), cuenta);
    }
}
