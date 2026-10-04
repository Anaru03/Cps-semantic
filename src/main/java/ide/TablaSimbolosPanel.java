package ide;

import semantic.*;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

/** Vista de símbolos en filas; conserva la identidad de los ámbitos homónimos. */
public final class TablaSimbolosPanel extends JPanel {
    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"Nombre", "Tipo", "Categoría", "Ámbito", "Parámetros",
                    "Almacenamiento", "Operando TAC", "Offset", "Etiqueta", "Tamaño (slots)"}, 0) {
        @Override public boolean isCellEditable(int fila, int columna) { return false; }
    };
    private final JTable tabla = new JTable(modelo);
    private final TableRowSorter<DefaultTableModel> orden = new TableRowSorter<>(modelo);
    private final JTextField filtro = new JTextField(20);
    private final JLabel estado = new JLabel("Sin compilar");
    public TablaSimbolosPanel() {
        super(new BorderLayout(0, 8)); setBackground(DiagramaArbol.PANEL);
        var barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8)); barra.setBackground(DiagramaArbol.PANEL);
        var etiqueta = new JLabel("Filtrar"); etiqueta.setForeground(new Color(226, 232, 240)); barra.add(etiqueta);
        filtro.setBackground(DiagramaArbol.FONDO); filtro.setForeground(Color.WHITE); filtro.setCaretColor(Color.WHITE);
        filtro.setToolTipText("Buscar por nombre, tipo, categoría o ámbito"); barra.add(filtro);
        estado.setForeground(new Color(148, 163, 184)); barra.add(estado); add(barra, BorderLayout.NORTH);
        filtro.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filtrar(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filtrar(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filtrar(); }
        });
        tabla.setRowSorter(orden); tabla.setRowHeight(30); tabla.setFillsViewportHeight(true);
        tabla.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13)); tabla.setBackground(DiagramaArbol.PANEL);
        tabla.setForeground(new Color(226, 232, 240)); tabla.setGridColor(new Color(51, 65, 85));
        tabla.setSelectionBackground(new Color(30, 64, 175)); tabla.setSelectionForeground(Color.WHITE);
        tabla.setShowVerticalLines(false); tabla.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] anchos = {140, 110, 110, 280, 170, 130, 170, 70, 150, 100};
        for (int i = 0; i < anchos.length; i++) tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        var renderer = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable table, Object valor,
                    boolean seleccionado, boolean foco, int fila, int columna) {
                super.getTableCellRendererComponent(table, valor, seleccionado, foco, fila, columna);
                if (!seleccionado) setBackground(fila % 2 == 0 ? DiagramaArbol.PANEL : new Color(25, 36, 53));
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                setToolTipText(valor == null ? null : valor.toString()); return this;
            }
        };
        tabla.setDefaultRenderer(Object.class, renderer);
        var cabecera = new DefaultTableCellRenderer(); cabecera.setBackground(DiagramaArbol.FONDO);
        cabecera.setForeground(new Color(148, 163, 184)); cabecera.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        tabla.getTableHeader().setDefaultRenderer(cabecera);
        tabla.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        var scroll = new JScrollPane(tabla); scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(DiagramaArbol.PANEL); add(scroll, BorderLayout.CENTER);
    }
    public void limpiar(String mensaje) { modelo.setRowCount(0); estado.setText(mensaje); }
    private void filtrar() {
        orden.setRowFilter(filtro.getText().isBlank() ? null
                : RowFilter.regexFilter("(?iu)" + Pattern.quote(filtro.getText())));
        actualizarConteo();
    }
    private void actualizarConteo() { estado.setText(tabla.getRowCount() + " / " + modelo.getRowCount() + " símbolos"); }
    public void mostrar(AnalisisSemantico analisis) {
        modelo.setRowCount(0);
        var vistos = Collections.newSetFromMap(new IdentityHashMap<Ambito, Boolean>());
        var ambitos = new ArrayList<Ambito>(); var pendientes = new ArrayDeque<Ambito>();
        pendientes.add(analisis.ambitoGlobal());
        var entradas = new ArrayList<>(analisis.informacion().ambitos().entrySet());
        entradas.sort(Comparator.comparingInt((Map.Entry<org.antlr.v4.runtime.ParserRuleContext, Ambito> e)
                -> e.getKey().getStart().getTokenIndex()).thenComparingInt(e -> e.getKey().getRuleIndex()));
        for (var entrada : entradas) pendientes.add(entrada.getValue());
        while (!pendientes.isEmpty()) {
            var ambito = pendientes.removeFirst(); if (!vistos.add(ambito)) continue;
            ambitos.add(ambito);
            for (var simbolo : ambito.simbolos()) if (simbolo.miembros() != null) pendientes.add(simbolo.miembros());
        }
        var rutas = new IdentityHashMap<Ambito, String>();
        for (int i = 0; i < ambitos.size(); i++) {
            var ambito = ambitos.get(i); var partes = new ArrayDeque<String>();
            for (var actual = ambito; actual != null; actual = actual.padre()) partes.addFirst(actual.nombre());
            rutas.put(ambito, String.join(" / ", partes) + " (#" + i + ")");
        }
        for (var ambito : ambitos) for (var simbolo : ambito.simbolos()) {
            String parametros = simbolo.parametros().stream().map(Object::toString)
                    .collect(java.util.stream.Collectors.joining(", "));
            var a = simbolo.almacenamiento();
            modelo.addRow(new Object[]{simbolo.nombre(), simbolo.tipo().toString(), simbolo.categoria().toString(),
                    rutas.get(ambito), parametros,
                    a == null ? "—" : a.clase().toString(), a == null || a.operando() == null ? "" : a.operando(),
                    a == null || a.offset() == null ? "" : a.offset().toString(),
                    a == null || a.etiqueta() == null ? "" : a.etiqueta(),
                    a == null || a.tamano() == null ? "" : a.tamano().toString()});
        }
        actualizarConteo();
    }
    JTable tabla() { return tabla; }
    JTextField filtro() { return filtro; }
}
