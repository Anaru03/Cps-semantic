package ide;

import compiler.Diagnostico;
import compiler.ResultadoCompilacion;
import tac.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.function.IntConsumer;

/** Vistas de resultados de la compilación: errores, TAC, registros de activación y clases. */
final class PanelesCompilacion {
    private PanelesCompilacion() { }

    static final Color FONDO = DiagramaArbol.FONDO, PANEL = DiagramaArbol.PANEL;
    static final Color TEXTO = new Color(226, 232, 240), SUAVE = new Color(148, 163, 184);

    static JTable tablaOscura(DefaultTableModel modelo) {
        var tabla = new JTable(modelo);
        tabla.setRowHeight(28); tabla.setFillsViewportHeight(true);
        tabla.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13)); tabla.setBackground(PANEL);
        tabla.setForeground(TEXTO); tabla.setGridColor(new Color(51, 65, 85));
        tabla.setSelectionBackground(new Color(30, 64, 175)); tabla.setSelectionForeground(Color.WHITE);
        tabla.setShowVerticalLines(false);
        tabla.getTableHeader().setBackground(FONDO); tabla.getTableHeader().setForeground(SUAVE);
        tabla.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        return tabla;
    }

    /** Lista de errores; al seleccionar uno se salta a su línea en el editor. */
    static final class Errores extends JPanel {
        private final DefaultTableModel modelo = new DefaultTableModel(
                new String[]{"Etapa", "Línea", "Columna", "Descripción"}, 0) {
            @Override public boolean isCellEditable(int f, int c) { return false; }
        };
        private final JTable tabla = tablaOscura(modelo);
        private final JLabel resumen = new JLabel(" ");
        private List<Diagnostico> actuales = List.of();

        Errores(IntConsumer irALinea) {
            super(new BorderLayout()); setBackground(PANEL);
            resumen.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
            resumen.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
            add(resumen, BorderLayout.NORTH);
            tabla.getColumnModel().getColumn(0).setPreferredWidth(90);
            tabla.getColumnModel().getColumn(1).setPreferredWidth(60);
            tabla.getColumnModel().getColumn(2).setPreferredWidth(70);
            tabla.getColumnModel().getColumn(3).setPreferredWidth(700);
            tabla.getSelectionModel().addListSelectionListener(e -> {
                int fila = tabla.getSelectedRow();
                if (!e.getValueIsAdjusting() && fila >= 0 && fila < actuales.size()) irALinea.accept(actuales.get(fila).linea());
            });
            var scroll = new JScrollPane(tabla); scroll.setBorder(null); scroll.getViewport().setBackground(PANEL);
            add(scroll, BorderLayout.CENTER);
        }
        void mostrar(ResultadoCompilacion resultado) {
            actuales = resultado.errores(); modelo.setRowCount(0);
            for (var e : actuales) modelo.addRow(new Object[]{etiqueta(e.etapa()), e.linea(), e.columna() + 1, e.descripcion()});
            if (actuales.isEmpty()) {
                resumen.setForeground(new Color(34, 197, 94));
                resumen.setText("✔ Sin errores léxicos, sintácticos ni semánticos. Se generó el código intermedio.");
            } else {
                resumen.setForeground(new Color(248, 113, 113));
                resumen.setText("✖ " + actuales.size() + " error(es). No se genera código intermedio.");
            }
        }
        void mensaje(String texto) { actuales = List.of(); modelo.setRowCount(0); resumen.setForeground(SUAVE); resumen.setText(texto); }
        JTable tabla() { return tabla; }
        private static String etiqueta(Diagnostico.Etapa etapa) {
            return switch (etapa) { case LEXICO -> "Léxico"; case SINTAXIS -> "Sintáctico";
                case SEMANTICA -> "Semántico"; case TAC -> "Código intermedio"; };
        }
    }

    /** Código TAC con numeración y resumen del reciclaje de temporales. */
    static final class CodigoIntermedio extends JPanel {
        private final JTextArea texto = new JTextArea();
        private final JLabel resumen = new JLabel(" ");
        CodigoIntermedio() {
            super(new BorderLayout()); setBackground(PANEL);
            texto.setEditable(false); texto.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
            texto.setBackground(PANEL); texto.setForeground(TEXTO); texto.setMargin(new Insets(12, 12, 12, 12));
            resumen.setForeground(SUAVE); resumen.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            resumen.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
            var scroll = new JScrollPane(texto); scroll.setBorder(null); scroll.getViewport().setBackground(PANEL);
            add(scroll, BorderLayout.CENTER); add(resumen, BorderLayout.SOUTH);
        }
        void mostrar(ResultadoCompilacion resultado) {
            var sb = new StringBuilder(); int n = 1;
            for (var ins : resultado.instrucciones()) {
                boolean sinSangria = ins.tipo() == InstruccionTAC.Tipo.ETIQUETA || ins.tipo() == InstruccionTAC.Tipo.FUNCION
                        || ins.tipo() == InstruccionTAC.Tipo.FIN_FUNCION || ins.tipo() == InstruccionTAC.Tipo.CLASE
                        || ins.tipo() == InstruccionTAC.Tipo.FIN_CLASE;
                sb.append(String.format("%4d  %s%s%n", n++, sinSangria ? "" : "    ", ins));
            }
            texto.setText(sb.toString()); texto.setCaretPosition(0);
            var t = resultado.temporales();
            resumen.setText(t == null ? " " : String.format(
                    "%d instrucciones · temporales: %d solicitudes, %d distintos, %d reutilizados, máximo simultáneo %d",
                    resultado.instrucciones().size(), t.solicitudes(), t.distintos(), t.reutilizados(), t.maximoSimultaneos()));
        }
        void limpiar(String mensaje) { texto.setText(mensaje); resumen.setText(" "); }
        String contenido() { return texto.getText(); }
    }

    /** Layouts de los registros de activación y de objetos. */
    static final class Estructuras extends JPanel {
        private final DefaultTableModel modelo = new DefaultTableModel(
                new String[]{"Unidad", "Elemento", "Operando / detalle", "Tipo", "Clase", "Offset"}, 0) {
            @Override public boolean isCellEditable(int f, int c) { return false; }
        };
        private final JTable tabla = tablaOscura(modelo);
        Estructuras() {
            super(new BorderLayout()); setBackground(PANEL);
            var scroll = new JScrollPane(tabla); scroll.setBorder(null); scroll.getViewport().setBackground(PANEL);
            add(scroll, BorderLayout.CENTER);
        }
        void mostrar(ResultadoCompilacion resultado) {
            modelo.setRowCount(0);
            for (var clase : resultado.clases().values()) {
                modelo.addRow(new Object[]{"clase " + clase.nombre(), "objeto",
                        clase.padre() == null ? "sin superclase" : "hereda de " + clase.padre(), "", "cabecera", 0});
                for (var c : clase.campos()) modelo.addRow(new Object[]{"clase " + clase.nombre(), c.nombre(),
                        "declarado en " + c.declarante() + (c.constante() ? " (const)" : ""), c.tipo(), "campo", c.offset()});
                for (var m : clase.tabla()) modelo.addRow(new Object[]{"clase " + clase.nombre(), m.nombre(),
                        m.etiqueta() + (m.sobrescribe() ? " (sobrescribe)" : ""), "", "método", m.ranura()});
            }
            for (var registro : resultado.registros().values()) {
                modelo.addRow(new Object[]{"registro " + registro.funcion(), "enlace dinámico", "", "", "cabecera", 0});
                modelo.addRow(new Object[]{"registro " + registro.funcion(), "dirección de retorno", "", "", "cabecera", 1});
                modelo.addRow(new Object[]{"registro " + registro.funcion(), "valor de retorno", "", "", "cabecera", 2});
                for (var p : registro.posiciones()) modelo.addRow(new Object[]{"registro " + registro.funcion(),
                        p.nombre(), p.operando(), p.tipo(), p.clase().toString().toLowerCase(), p.offset()});
            }
        }
        void limpiar() { modelo.setRowCount(0); }
        JTable tabla() { return tabla; }
    }
}
