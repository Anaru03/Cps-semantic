package ide;

import antlr.CompiscriptParser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

/** Diagrama vectorial del parse tree; no genera imágenes gigantes ni usa recursión. */
public final class DiagramaArbol extends JPanel {
    static final Color FONDO = new Color(15, 23, 42), PANEL = new Color(30, 41, 59);
    static final int ALTO = 40, PASO_Y = 88, SEPARACION = 20, MARGEN = 40;
    static final class Nodo {
        final String texto, detalle;
        final boolean terminal;
        final Nodo padre;
        final int profundidad;
        final List<Nodo> hijos = new ArrayList<>();
        boolean plegado;
        double ancho, espacio, x, y;
        Nodo(String texto, String detalle, boolean terminal, Nodo padre) {
            this.texto = texto; this.detalle = detalle; this.terminal = terminal; this.padre = padre;
            profundidad = padre == null ? 0 : padre.profundidad + 1;
            plegado = profundidad >= 3;
        }
    }
    static final class Modelo {
        final Nodo raiz;
        final List<Nodo> nodos;
        Modelo(Nodo raiz, List<Nodo> nodos) { this.raiz = raiz; this.nodos = nodos; }
    }
    static Modelo construir(ParseTree arbol, String[] reglas) {
        var nodos = new ArrayList<Nodo>();
        record Pendiente(ParseTree origen, Nodo padre) { }
        var pendientes = new ArrayDeque<Pendiente>(); pendientes.push(new Pendiente(arbol, null));
        Nodo raiz = null;
        while (!pendientes.isEmpty()) {
            if (Thread.currentThread().isInterrupted()) throw new java.util.concurrent.CancellationException();
            var pendiente = pendientes.pop(); var origen = pendiente.origen();
            boolean terminal = origen instanceof TerminalNode;
            String texto = terminal ? ((TerminalNode) origen).getSymbol().getText()
                    : reglas[((ParserRuleContext) origen).getRuleIndex()];
            String posicion = "";
            if (origen instanceof ParserRuleContext ctx && ctx.getStart() != null)
                posicion = " · línea " + ctx.getStart().getLine() + ", columna " + ctx.getStart().getCharPositionInLine();
            else if (origen instanceof TerminalNode token)
                posicion = " · línea " + token.getSymbol().getLine() + ", columna " + token.getSymbol().getCharPositionInLine();
            var nodo = new Nodo(texto, texto + posicion, terminal, pendiente.padre());
            if (nodo.padre == null) raiz = nodo; else nodo.padre.hijos.add(nodo);
            nodos.add(nodo);
            for (int i = origen.getChildCount() - 1; i >= 0; i--) pendientes.push(new Pendiente(origen.getChild(i), nodo));
        }
        return new Modelo(raiz, List.copyOf(nodos));
    }

    private final Lienzo lienzo = new Lienzo();
    private final JScrollPane scroll = new JScrollPane(lienzo);
    private final JLabel estado = new JLabel("Sin compilar");
    private final JTextArea detalle = new JTextArea(2, 20);
    private final JTextField buscar = new JTextField(14);
    private Modelo modelo;
    private List<Nodo> visibles = List.of();
    private Nodo seleccionado;
    private double zoom = 1, ancho = 400, alto = 250;
    private SwingWorker<Modelo, Void> carga;
    private boolean centrarAlMostrar;

    public DiagramaArbol() {
        super(new BorderLayout(0, 8)); setBackground(FONDO);
        var herramientas = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6)); herramientas.setBackground(PANEL);
        boton(herramientas, "−", () -> cambiarZoom(zoom / 1.25));
        boton(herramientas, "+", () -> cambiarZoom(zoom * 1.25));
        boton(herramientas, "100%", () -> cambiarZoom(1));
        boton(herramientas, "Panorama", this::ajustar);
        boton(herramientas, "Inicio", () -> { if (modelo != null) centrar(modelo.raiz); });
        boton(herramientas, "Plegar", () -> {
            if (modelo == null) return;
            for (var nodo : modelo.nodos) nodo.plegado = nodo.profundidad >= 1;
            seleccionado = modelo.raiz; distribuir(); centrar(seleccionado);
        });
        var busqueda = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4)); busqueda.setBackground(PANEL);
        buscar.setBackground(FONDO); buscar.setForeground(new Color(226, 232, 240)); buscar.setCaretColor(Color.WHITE);
        buscar.setToolTipText("Buscar nodos por regla o texto; Enter recorre coincidencias y abre su rama");
        buscar.addActionListener(e -> buscarSiguiente()); busqueda.add(buscar);
        boton(busqueda, "Buscar", this::buscarSiguiente);
        estado.setForeground(new Color(148, 163, 184)); busqueda.add(estado);
        var cabecera = new JPanel(new GridLayout(2, 1)); cabecera.add(herramientas); cabecera.add(busqueda);
        add(cabecera, BorderLayout.NORTH);
        scroll.setBorder(BorderFactory.createEmptyBorder()); scroll.getViewport().setBackground(FONDO);
        scroll.getHorizontalScrollBar().setUnitIncrement(40); scroll.getVerticalScrollBar().setUnitIncrement(40);
        scroll.getViewport().addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { centrarInicial(); }
        });
        scroll.setWheelScrollingEnabled(false);
        scroll.addMouseWheelListener(e -> {
            if (e.isControlDown() || e.isMetaDown()) cambiarZoom(zoom * Math.pow(1.15, -e.getPreciseWheelRotation()));
            else {
                var barra = e.isShiftDown() ? scroll.getHorizontalScrollBar() : scroll.getVerticalScrollBar();
                barra.setValue(barra.getValue() + (int) (e.getPreciseWheelRotation() * 60));
            }
            e.consume();
        });
        add(scroll, BorderLayout.CENTER);
        detalle.setEditable(false); detalle.setLineWrap(true); detalle.setWrapStyleWord(true);
        detalle.setBackground(PANEL); detalle.setForeground(new Color(226, 232, 240));
        detalle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        detalle.setText("Clic: seleccionar · Doble clic: plegar/abrir rama · Arrastrar: desplazar · Ctrl/⌘ + rueda: zoom");
        add(new JScrollPane(detalle), BorderLayout.SOUTH);
    }
    private void boton(JPanel panel, String texto, Runnable accion) {
        var boton = new JButton(texto); boton.setFocusable(false);
        boton.setBackground(FONDO); boton.setForeground(new Color(226, 232, 240));
        boton.setOpaque(true); boton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(71, 85, 105)), BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        boton.addActionListener(e -> accion.run()); panel.add(boton);
    }
    public void mostrar(ParseTree arbol, CompiscriptParser parser) {
        if (carga != null) carga.cancel(true);
        modelo = null; visibles = List.of(); seleccionado = null; lienzo.repaint(); estado.setText("Preparando diagrama…");
        String[] reglas = parser.getRuleNames().clone();
        carga = new SwingWorker<>() {
            @Override protected Modelo doInBackground() { return construir(arbol, reglas); }
            @Override protected void done() {
                if (isCancelled() || carga != this) return;
                try { establecer(get()); }
                catch (Exception error) { estado.setText("No se pudo construir el diagrama"); }
            }
        };
        carga.execute();
    }
    void establecer(Modelo modelo) {
        this.modelo = modelo; seleccionado = modelo.raiz; zoom = 1;
        centrarAlMostrar = true; distribuir(); SwingUtilities.invokeLater(this::centrarInicial);
    }
    private void centrarInicial() {
        var ext = scroll.getViewport().getExtentSize();
        if (centrarAlMostrar && modelo != null && ext.width > 0 && ext.height > 0) {
            centrarAlMostrar = false; centrar(modelo.raiz);
        }
    }
    private void distribuir() {
        if (modelo == null) return;
        var lista = new ArrayList<Nodo>(); var pendientes = new ArrayDeque<Nodo>(); pendientes.push(modelo.raiz);
        var fm = lienzo.getFontMetrics(lienzo.getFont());
        while (!pendientes.isEmpty()) {
            var nodo = pendientes.pop(); lista.add(nodo);
            nodo.ancho = Math.min(260, Math.max(96, fm.stringWidth(nodo.texto) + 40));
            if (!nodo.plegado) for (int i = nodo.hijos.size() - 1; i >= 0; i--) pendientes.push(nodo.hijos.get(i));
        }
        for (int i = lista.size() - 1; i >= 0; i--) {
            var nodo = lista.get(i); double hijos = 0;
            if (!nodo.plegado) for (var hijo : nodo.hijos) hijos += hijo.espacio + SEPARACION;
            nodo.espacio = Math.max(nodo.ancho, Math.max(0, hijos - SEPARACION));
        }
        modelo.raiz.x = MARGEN + modelo.raiz.espacio / 2; alto = 0;
        for (var nodo : lista) {
            nodo.y = MARGEN + nodo.profundidad * PASO_Y; alto = Math.max(alto, nodo.y + ALTO + MARGEN);
            double inicio = nodo.x - nodo.espacio / 2;
            if (!nodo.plegado) for (var hijo : nodo.hijos) {
                hijo.x = inicio + hijo.espacio / 2; inicio += hijo.espacio + SEPARACION;
            }
        }
        ancho = modelo.raiz.espacio + 2 * MARGEN; visibles = List.copyOf(lista);
        actualizarTamano();
    }
    private void actualizarTamano() {
        lienzo.setPreferredSize(new Dimension((int) Math.min(Integer.MAX_VALUE - 1, Math.ceil(ancho * zoom)),
                (int) Math.min(Integer.MAX_VALUE - 1, Math.ceil(alto * zoom))));
        lienzo.revalidate(); lienzo.repaint();
        if (modelo != null) estado.setText(Math.round(zoom * 100) + "% · " + visibles.size() + "/" + modelo.nodos.size() + " nodos");
    }
    private void cambiarZoom(double nuevo) {
        Point p = scroll.getViewport().getViewPosition(); Dimension ext = scroll.getViewport().getExtentSize();
        double cx = (p.x + ext.width / 2.0) / zoom, cy = (p.y + ext.height / 2.0) / zoom;
        zoom = Math.max(0.02, Math.min(3, nuevo)); actualizarTamano();
        posicionar(cx * zoom - ext.width / 2.0, cy * zoom - ext.height / 2.0);
    }
    private void posicionar(double x, double y) {
        Dimension ext = scroll.getViewport().getExtentSize(), size = lienzo.getPreferredSize();
        scroll.getViewport().setViewPosition(new Point((int) Math.max(0, Math.min(x, size.width - ext.width)),
                (int) Math.max(0, Math.min(y, size.height - ext.height))));
    }
    private void centrar(Nodo nodo) {
        var ext = scroll.getViewport().getExtentSize();
        posicionar(nodo.x * zoom - ext.width / 2.0, (nodo.y + ALTO / 2.0) * zoom - ext.height / 2.0);
    }
    private void ajustar() {
        var ext = scroll.getViewport().getExtentSize();
        if (ext.width > 0 && ext.height > 0) cambiarZoom(Math.min(1, Math.min(ext.width / ancho, ext.height / alto)));
    }
    private void buscarSiguiente() {
        if (modelo == null || buscar.getText().isBlank()) return;
        String texto = buscar.getText().toLowerCase(Locale.ROOT);
        int inicio = seleccionado == null ? -1 : modelo.nodos.indexOf(seleccionado);
        for (int paso = 1; paso <= modelo.nodos.size(); paso++) {
            var nodo = modelo.nodos.get((inicio + paso) % modelo.nodos.size());
            if (nodo.texto.toLowerCase(Locale.ROOT).contains(texto)) {
                for (var padre = nodo.padre; padre != null; padre = padre.padre) padre.plegado = false;
                seleccionado = nodo; detalle.setText(nodo.detalle); distribuir();
                if (zoom < 0.6) cambiarZoom(1); centrar(nodo); return;
            }
        }
        detalle.setText("Sin coincidencias para: " + buscar.getText());
    }
    private Nodo nodoEn(Point punto) {
        double x = punto.x / zoom, y = punto.y / zoom;
        for (var nodo : visibles) if (new Rectangle2D.Double(nodo.x - nodo.ancho / 2, nodo.y, nodo.ancho, ALTO).contains(x, y)) return nodo;
        return null;
    }
    private final class Lienzo extends JComponent {
        Point arrastre, origen;
        Lienzo() {
            setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13)); setToolTipText("");
            var mouse = new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    arrastre = SwingUtilities.convertPoint(Lienzo.this, e.getPoint(), scroll.getViewport());
                    origen = scroll.getViewport().getViewPosition(); setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                }
                @Override public void mouseReleased(MouseEvent e) { arrastre = null; setCursor(Cursor.getDefaultCursor()); }
                @Override public void mouseDragged(MouseEvent e) {
                    if (arrastre == null) return;
                    Point actual = SwingUtilities.convertPoint(Lienzo.this, e.getPoint(), scroll.getViewport());
                    posicionar(origen.x + arrastre.x - actual.x, origen.y + arrastre.y - actual.y);
                }
                @Override public void mouseClicked(MouseEvent e) {
                    var nodo = nodoEn(e.getPoint()); if (nodo == null) return;
                    seleccionado = nodo; detalle.setText(nodo.detalle);
                    if (e.getClickCount() == 2 && !nodo.hijos.isEmpty()) { nodo.plegado = !nodo.plegado; distribuir(); centrar(nodo); }
                    repaint();
                }
            };
            addMouseListener(mouse); addMouseMotionListener(mouse);
        }
        @Override public String getToolTipText(MouseEvent e) {
            var nodo = nodoEn(e.getPoint()); return nodo == null ? null : nodo.detalle;
        }
        @Override protected void paintComponent(Graphics graphics) {
            var g = (Graphics2D) graphics.create();
            try {
                g.setColor(FONDO); g.fillRect(0, 0, getWidth(), getHeight());
                g.setFont(getFont());
                g.scale(zoom, zoom); g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Rectangle2D clip = g.getClipBounds();
                if (clip == null) clip = new Rectangle2D.Double(0, 0, getWidth() / zoom, getHeight() / zoom);
                g.setStroke(new BasicStroke(1.3f)); g.setColor(new Color(71, 85, 105));
                for (var nodo : visibles) if (nodo.padre != null) {
                    var p = nodo.padre;
                    var bounds = new Rectangle2D.Double(Math.min(p.x, nodo.x) - 2, p.y,
                            Math.abs(p.x - nodo.x) + 4, nodo.y - p.y + ALTO);
                    if (!bounds.intersects(clip)) continue;
                    var linea = new Path2D.Double(); double medio = nodo.y - 24;
                    linea.moveTo(p.x, p.y + ALTO); linea.lineTo(p.x, medio); linea.lineTo(nodo.x, medio); linea.lineTo(nodo.x, nodo.y); g.draw(linea);
                }
                for (var nodo : visibles) {
                    var bounds = new Rectangle2D.Double(nodo.x - nodo.ancho / 2, nodo.y, nodo.ancho, ALTO);
                    if (!bounds.intersects(clip)) continue;
                    var caja = new RoundRectangle2D.Double(bounds.getX(), nodo.y, nodo.ancho, ALTO, 12, 12);
                    g.setColor(nodo == seleccionado ? new Color(30, 64, 175) : nodo.terminal ? new Color(6, 78, 59) : PANEL); g.fill(caja);
                    g.setColor(nodo == seleccionado ? new Color(96, 165, 250) : nodo.terminal ? new Color(52, 211, 153) : new Color(71, 85, 105)); g.draw(caja);
                    g.setColor(new Color(226, 232, 240)); var fm = g.getFontMetrics(); String texto = nodo.texto;
                    int max = (int) nodo.ancho - 32;
                    if (fm.stringWidth(texto) > max) {
                        int fin = Math.min(texto.length(), max / Math.max(1, fm.charWidth('m')));
                        texto = texto.substring(0, fin) + "…";
                    }
                    g.drawString(texto, (float) (nodo.x - fm.stringWidth(texto) / 2.0), (float) (nodo.y + 25));
                    if (!nodo.hijos.isEmpty()) g.drawString(nodo.plegado ? "+" : "−", (float) (bounds.getMaxX() - 16), (float) (nodo.y + 25));
                }
            } finally { g.dispose(); }
        }
    }
}
