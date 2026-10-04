package ide;

import antlr.CompiscriptParser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CancellationException;

public final class DiagramaArbol extends JPanel {

    static final Color FONDO = new Color(15, 23, 42);
    static final Color PANEL = new Color(30, 41, 59);

    private static final Color BORDE = new Color(71, 85, 105);
    private static final Color TEXTO = new Color(226, 232, 240);
    private static final Color TEXTO_SUAVE = new Color(148, 163, 184);
    private static final Color AZUL = new Color(30, 64, 175);
    private static final Color AZUL_BORDE = new Color(96, 165, 250);
    private static final Color VERDE = new Color(6, 78, 59);
    private static final Color VERDE_BORDE = new Color(52, 211, 153);

    private static final int ALTO = 42;
    private static final int PASO_Y = 100;
    private static final int SEPARACION = 30;
    private static final int MARGEN = 70;

    static final class Nodo {

        final String texto;
        final String detalle;
        final boolean terminal;
        final Nodo padre;
        final int profundidad;
        final List<Nodo> hijos = new ArrayList<>();

        boolean plegado;
        double ancho;
        double espacio;
        double x;
        double y;

        Nodo(
                String texto,
                String detalle,
                boolean terminal,
                Nodo padre) {

            this.texto = texto;
            this.detalle = detalle;
            this.terminal = terminal;
            this.padre = padre;

            this.profundidad =
                    padre == null
                            ? 0
                            : padre.profundidad + 1;

            this.plegado = false;
        }
    }

    static final class Modelo {

        final Nodo raiz;
        final List<Nodo> nodos;

        Modelo(
                Nodo raiz,
                List<Nodo> nodos) {

            this.raiz = raiz;
            this.nodos = nodos;
        }
    }

    static Modelo construir(
            ParseTree arbol,
            String[] reglas) {

        ArrayList<Nodo> nodos =
                new ArrayList<>();

        record Pendiente(
                ParseTree origen,
                Nodo padre) {
        }

        ArrayDeque<Pendiente> pendientes =
                new ArrayDeque<>();

        pendientes.push(
                new Pendiente(
                        arbol,
                        null
                )
        );

        Nodo raiz = null;

        while (!pendientes.isEmpty()) {

            if (Thread.currentThread().isInterrupted()) {
                throw new CancellationException();
            }

            Pendiente pendiente =
                    pendientes.pop();

            ParseTree origen =
                    pendiente.origen();

            boolean terminal =
                    origen instanceof TerminalNode;

            String texto;

            if (terminal) {

                texto =
                        ((TerminalNode) origen)
                                .getSymbol()
                                .getText();

            } else {

                ParserRuleContext contexto =
                        (ParserRuleContext) origen;

                texto =
                        reglas[
                                contexto.getRuleIndex()
                        ];
            }

            String posicion = "";

            if (
                    origen instanceof ParserRuleContext contexto
                            && contexto.getStart() != null
            ) {

                posicion =
                        " · línea "
                                + contexto.getStart().getLine()
                                + ", columna "
                                + contexto.getStart().getCharPositionInLine();

            } else if (
                    origen instanceof TerminalNode token
            ) {

                posicion =
                        " · línea "
                                + token.getSymbol().getLine()
                                + ", columna "
                                + token.getSymbol().getCharPositionInLine();
            }

            Nodo nodo =
                    new Nodo(
                            texto,
                            texto + posicion,
                            terminal,
                            pendiente.padre()
                    );

            if (nodo.padre == null) {
                raiz = nodo;
            } else {
                nodo.padre.hijos.add(nodo);
            }

            nodos.add(nodo);

            for (
                    int i = origen.getChildCount() - 1;
                    i >= 0;
                    i--
            ) {

                pendientes.push(
                        new Pendiente(
                                origen.getChild(i),
                                nodo
                        )
                );
            }
        }

        return new Modelo(
                raiz,
                List.copyOf(nodos)
        );
    }

    private final Lienzo lienzo =
            new Lienzo();

    private final JScrollPane scroll =
            new JScrollPane(lienzo);

    private final JLabel estado =
            new JLabel("Sin compilar");

    private final JTextArea detalle =
            new JTextArea(2, 20);

    private final JTextField buscar =
            new JTextField(14);

    private Modelo modelo;

    private List<Nodo> visibles =
            List.of();

    private Nodo seleccionado;

    private double zoom = 1.0;
    private double ancho = 400;
    private double alto = 250;

    private SwingWorker<Modelo, Void> carga;

    private boolean centrarAlMostrar;

    public DiagramaArbol() {

        super(
                new BorderLayout(
                        0,
                        8
                )
        );

        setBackground(FONDO);

        JPanel herramientas =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.LEFT,
                                6,
                                6
                        )
                );

        herramientas.setBackground(PANEL);

        boton(
                herramientas,
                "−",
                () -> cambiarZoom(
                        zoom / 1.25
                )
        );

        boton(
                herramientas,
                "+",
                () -> cambiarZoom(
                        zoom * 1.25
                )
        );

        boton(
                herramientas,
                "100%",
                () -> cambiarZoom(1.0)
        );

        boton(
                herramientas,
                "Panorama",
                this::ajustar
        );

        boton(
                herramientas,
                "Inicio",
                () -> {
                    if (modelo != null) {
                        centrar(modelo.raiz);
                    }
                }
        );

        boton(
                herramientas,
                "Expandir",
                this::expandirTodo
        );

        boton(
                herramientas,
                "Plegar",
                this::plegarTodo
        );

        JPanel busqueda =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.LEFT,
                                6,
                                4
                        )
                );

        busqueda.setBackground(PANEL);

        buscar.setBackground(FONDO);
        buscar.setForeground(TEXTO);
        buscar.setCaretColor(Color.WHITE);

        buscar.setToolTipText(
                "Buscar nodos por regla o texto"
        );

        buscar.addActionListener(
                e -> buscarSiguiente()
        );

        busqueda.add(buscar);

        boton(
                busqueda,
                "Buscar",
                this::buscarSiguiente
        );

        estado.setForeground(TEXTO_SUAVE);

        busqueda.add(estado);

        JPanel cabecera =
                new JPanel(
                        new GridLayout(
                                2,
                                1
                        )
                );

        cabecera.add(herramientas);
        cabecera.add(busqueda);

        add(
                cabecera,
                BorderLayout.NORTH
        );

        scroll.setBorder(
                BorderFactory.createEmptyBorder()
        );

        scroll.getViewport()
                .setBackground(FONDO);

        scroll.getHorizontalScrollBar()
                .setUnitIncrement(50);

        scroll.getVerticalScrollBar()
                .setUnitIncrement(50);

        scroll.getViewport()
                .addComponentListener(
                        new ComponentAdapter() {

                            @Override
                            public void componentResized(
                                    ComponentEvent e) {

                                centrarInicial();
                            }
                        }
                );

        scroll.setWheelScrollingEnabled(false);

        scroll.addMouseWheelListener(
                e -> {

                    if (
                            e.isControlDown()
                                    || e.isMetaDown()
                    ) {

                        cambiarZoom(
                                zoom
                                        * Math.pow(
                                        1.15,
                                        -e.getPreciseWheelRotation()
                                )
                        );

                    } else {

                        var barra =
                                e.isShiftDown()
                                        ? scroll.getHorizontalScrollBar()
                                        : scroll.getVerticalScrollBar();

                        barra.setValue(
                                barra.getValue()
                                        + (int) (
                                        e.getPreciseWheelRotation()
                                                * 60
                                )
                        );
                    }

                    e.consume();
                }
        );

        add(
                scroll,
                BorderLayout.CENTER
        );

        detalle.setEditable(false);
        detalle.setLineWrap(true);
        detalle.setWrapStyleWord(true);
        detalle.setBackground(PANEL);
        detalle.setForeground(TEXTO);

        detalle.setFont(
                new Font(
                        Font.SANS_SERIF,
                        Font.PLAIN,
                        12
                )
        );

        detalle.setText(
                "Clic: seleccionar · Doble clic: plegar/abrir rama · Arrastrar: desplazar · Ctrl/⌘ + rueda: zoom"
        );

        JScrollPane panelDetalle =
                new JScrollPane(detalle);

        panelDetalle.setBorder(
                BorderFactory.createLineBorder(
                        BORDE
                )
        );

        add(
                panelDetalle,
                BorderLayout.SOUTH
        );
    }

    private void boton(
            JPanel panel,
            String texto,
            Runnable accion) {

        JButton boton =
                new JButton(texto);

        boton.setFocusable(false);
        boton.setBackground(FONDO);
        boton.setForeground(TEXTO);
        boton.setOpaque(true);

        boton.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                BORDE
                        ),
                        BorderFactory.createEmptyBorder(
                                5,
                                9,
                                5,
                                9
                        )
                )
        );

        boton.addActionListener(
                e -> accion.run()
        );

        panel.add(boton);
    }

    public void mostrar(
            ParseTree arbol,
            CompiscriptParser parser) {

        mostrar(
                arbol,
                parser.getRuleNames()
        );
    }

    public void mostrar(
            ParseTree arbol) {

        mostrar(
                arbol,
                CompiscriptParser.ruleNames
        );
    }

    public void mostrar(
            ParseTree arbol,
            String[] nombresReglas) {

        if (carga != null) {
            carga.cancel(true);
        }

        modelo = null;
        visibles = List.of();
        seleccionado = null;

        lienzo.repaint();

        estado.setText(
                "Preparando diagrama…"
        );

        String[] reglas =
                nombresReglas.clone();

        carga =
                new SwingWorker<>() {

                    @Override
                    protected Modelo doInBackground() {

                        return construir(
                                arbol,
                                reglas
                        );
                    }

                    @Override
                    protected void done() {

                        if (
                                isCancelled()
                                        || carga != this
                        ) {
                            return;
                        }

                        try {

                            Modelo nuevoModelo =
                                    get();

                            for (
                                    Nodo nodo :
                                    nuevoModelo.nodos
                            ) {
                                nodo.plegado = false;
                            }

                            establecer(
                                    nuevoModelo
                            );

                        } catch (
                                Exception error
                        ) {

                            estado.setText(
                                    "No se pudo construir el diagrama"
                            );
                        }
                    }
                };

        carga.execute();
    }

    void establecer(
            Modelo modelo) {

        this.modelo = modelo;

        seleccionado =
                modelo.raiz;

        zoom = 1.0;

        centrarAlMostrar = true;

        distribuir();

        SwingUtilities.invokeLater(
                () -> {
                    ajustarZoomInicial();
                    centrarInicial();
                }
        );
    }

    private void expandirTodo() {

        if (modelo == null) {
            return;
        }

        for (
                Nodo nodo :
                modelo.nodos
        ) {
            nodo.plegado = false;
        }

        seleccionado =
                modelo.raiz;

        distribuir();

        SwingUtilities.invokeLater(
                () -> {

                    if (zoom < 0.20) {
                        cambiarZoom(0.20);
                    }

                    centrar(
                            modelo.raiz
                    );
                }
        );
    }

    private void plegarTodo() {

        if (modelo == null) {
            return;
        }

        for (
                Nodo nodo :
                modelo.nodos
        ) {

            nodo.plegado =
                    nodo.profundidad >= 1;
        }

        modelo.raiz.plegado =
                false;

        seleccionado =
                modelo.raiz;

        distribuir();

        SwingUtilities.invokeLater(
                () -> centrar(
                        modelo.raiz
                )
        );
    }

    private void ajustarZoomInicial() {

        if (modelo == null) {
            return;
        }

        Dimension ext =
                scroll.getViewport()
                        .getExtentSize();

        if (
                ext.width <= 0
                        || ext.height <= 0
        ) {
            return;
        }

        double zoomVertical =
                ext.height
                        / Math.max(
                        1.0,
                        alto
                );

        double inicial =
                Math.min(
                        0.70,
                        Math.max(
                                0.20,
                                zoomVertical
                        )
                );

        cambiarZoom(
                inicial
        );
    }

    private void centrarInicial() {

        Dimension ext =
                scroll.getViewport()
                        .getExtentSize();

        if (
                centrarAlMostrar
                        && modelo != null
                        && ext.width > 0
                        && ext.height > 0
        ) {

            centrarAlMostrar = false;

            centrar(
                    modelo.raiz
            );
        }
    }

    private void distribuir() {

        if (modelo == null) {
            return;
        }

        ArrayList<Nodo> lista =
                new ArrayList<>();

        ArrayDeque<Nodo> pendientes =
                new ArrayDeque<>();

        pendientes.push(
                modelo.raiz
        );

        FontMetrics fm =
                lienzo.getFontMetrics(
                        lienzo.getFont()
                );

        while (!pendientes.isEmpty()) {

            Nodo nodo =
                    pendientes.pop();

            lista.add(nodo);

            nodo.ancho =
                    Math.min(
                            280,
                            Math.max(
                                    105,
                                    fm.stringWidth(
                                            nodo.texto
                                    ) + 42
                            )
                    );

            if (!nodo.plegado) {

                for (
                        int i =
                        nodo.hijos.size() - 1;
                        i >= 0;
                        i--
                ) {

                    pendientes.push(
                            nodo.hijos.get(i)
                    );
                }
            }
        }

        for (
                int i =
                lista.size() - 1;
                i >= 0;
                i--
        ) {

            Nodo nodo =
                    lista.get(i);

            double espacioHijos = 0;

            if (!nodo.plegado) {

                for (
                        Nodo hijo :
                        nodo.hijos
                ) {

                    espacioHijos +=
                            hijo.espacio
                                    + SEPARACION;
                }
            }

            if (espacioHijos > 0) {
                espacioHijos -=
                        SEPARACION;
            }

            nodo.espacio =
                    Math.max(
                            nodo.ancho,
                            espacioHijos
                    );
        }

        modelo.raiz.x =
                MARGEN
                        + modelo.raiz.espacio
                        / 2.0;

        alto = 0;

        for (
                Nodo nodo :
                lista
        ) {

            nodo.y =
                    MARGEN
                            + nodo.profundidad
                            * PASO_Y;

            alto =
                    Math.max(
                            alto,
                            nodo.y
                                    + ALTO
                                    + MARGEN
                    );

            if (
                    nodo.plegado
                            || nodo.hijos.isEmpty()
            ) {
                continue;
            }

            double anchoHijos = 0;

            for (
                    Nodo hijo :
                    nodo.hijos
            ) {

                anchoHijos +=
                        hijo.espacio
                                + SEPARACION;
            }

            anchoHijos -=
                    SEPARACION;

            double inicio =
                    nodo.x
                            - anchoHijos
                            / 2.0;

            for (
                    Nodo hijo :
                    nodo.hijos
            ) {

                hijo.x =
                        inicio
                                + hijo.espacio
                                / 2.0;

                inicio +=
                        hijo.espacio
                                + SEPARACION;
            }
        }

        ancho =
                modelo.raiz.espacio
                        + 2.0 * MARGEN;

        visibles =
                List.copyOf(lista);

        actualizarTamano();
    }

    private void actualizarTamano() {

        lienzo.setPreferredSize(
                new Dimension(
                        (int) Math.min(
                                Integer.MAX_VALUE - 1,
                                Math.ceil(
                                        ancho * zoom
                                )
                        ),
                        (int) Math.min(
                                Integer.MAX_VALUE - 1,
                                Math.ceil(
                                        alto * zoom
                                )
                        )
                )
        );

        lienzo.revalidate();
        lienzo.repaint();

        if (modelo != null) {

            estado.setText(
                    Math.round(
                            zoom * 100
                    )
                            + "% · "
                            + visibles.size()
                            + "/"
                            + modelo.nodos.size()
                            + " nodos"
            );
        }
    }

    private void cambiarZoom(
            double nuevo) {

        Point posicion =
                scroll.getViewport()
                        .getViewPosition();

        Dimension ext =
                scroll.getViewport()
                        .getExtentSize();

        double centroX =
                (
                        posicion.x
                                + ext.width / 2.0
                ) / zoom;

        double centroY =
                (
                        posicion.y
                                + ext.height / 2.0
                ) / zoom;

        zoom =
                Math.max(
                        0.05,
                        Math.min(
                                3.0,
                                nuevo
                        )
                );

        actualizarTamano();

        posicionar(
                centroX * zoom
                        - ext.width / 2.0,
                centroY * zoom
                        - ext.height / 2.0
        );
    }

    private void posicionar(
            double x,
            double y) {

        Dimension ext =
                scroll.getViewport()
                        .getExtentSize();

        Dimension size =
                lienzo.getPreferredSize();

        double maxX =
                Math.max(
                        0,
                        size.width
                                - ext.width
                );

        double maxY =
                Math.max(
                        0,
                        size.height
                                - ext.height
                );

        scroll.getViewport()
                .setViewPosition(
                        new Point(
                                (int) Math.max(
                                        0,
                                        Math.min(
                                                x,
                                                maxX
                                        )
                                ),
                                (int) Math.max(
                                        0,
                                        Math.min(
                                                y,
                                                maxY
                                        )
                                )
                        )
                );
    }

    private void centrar(
            Nodo nodo) {

        Dimension ext =
                scroll.getViewport()
                        .getExtentSize();

        posicionar(
                nodo.x * zoom
                        - ext.width / 2.0,
                (
                        nodo.y
                                + ALTO / 2.0
                ) * zoom
                        - ext.height / 2.0
        );
    }

    private void ajustar() {

        Dimension ext =
                scroll.getViewport()
                        .getExtentSize();

        if (
                ext.width <= 0
                        || ext.height <= 0
        ) {
            return;
        }

        double nuevo =
                Math.min(
                        1.0,
                        Math.min(
                                ext.width / ancho,
                                ext.height / alto
                        )
                );

        cambiarZoom(nuevo);

        if (modelo != null) {

            SwingUtilities.invokeLater(
                    () -> centrar(
                            modelo.raiz
                    )
            );
        }
    }

    private void buscarSiguiente() {

        if (
                modelo == null
                        || buscar.getText().isBlank()
        ) {
            return;
        }

        String texto =
                buscar.getText()
                        .toLowerCase(
                                Locale.ROOT
                        );

        int inicio =
                seleccionado == null
                        ? -1
                        : modelo.nodos
                        .indexOf(
                                seleccionado
                        );

        for (
                int paso = 1;
                paso <= modelo.nodos.size();
                paso++
        ) {

            Nodo nodo =
                    modelo.nodos.get(
                            (
                                    inicio
                                            + paso
                            )
                                    % modelo.nodos.size()
                    );

            if (
                    nodo.texto
                            .toLowerCase(
                                    Locale.ROOT
                            )
                            .contains(
                                    texto
                            )
            ) {

                for (
                        Nodo padre =
                        nodo.padre;
                        padre != null;
                        padre =
                                padre.padre
                ) {

                    padre.plegado =
                            false;
                }

                seleccionado = nodo;

                detalle.setText(
                        nodo.detalle
                );

                distribuir();

                if (zoom < 0.6) {
                    cambiarZoom(1.0);
                }

                centrar(nodo);

                return;
            }
        }

        detalle.setText(
                "Sin coincidencias para: "
                        + buscar.getText()
        );
    }

    private Nodo nodoEn(
            Point punto) {

        double x =
                punto.x / zoom;

        double y =
                punto.y / zoom;

        for (
                Nodo nodo :
                visibles
        ) {

            Rectangle2D caja =
                    new Rectangle2D.Double(
                            nodo.x
                                    - nodo.ancho / 2.0,
                            nodo.y,
                            nodo.ancho,
                            ALTO
                    );

            if (
                    caja.contains(
                            x,
                            y
                    )
            ) {

                return nodo;
            }
        }

        return null;
    }

    private final class Lienzo
            extends JComponent {

        private Point arrastre;
        private Point origen;

        Lienzo() {

            setFont(
                    new Font(
                            Font.SANS_SERIF,
                            Font.PLAIN,
                            13
                    )
            );

            setToolTipText("");

            MouseAdapter mouse =
                    new MouseAdapter() {

                        @Override
                        public void mousePressed(
                                MouseEvent e) {

                            arrastre =
                                    SwingUtilities.convertPoint(
                                            Lienzo.this,
                                            e.getPoint(),
                                            scroll.getViewport()
                                    );

                            origen =
                                    scroll.getViewport()
                                            .getViewPosition();

                            setCursor(
                                    Cursor.getPredefinedCursor(
                                            Cursor.MOVE_CURSOR
                                    )
                            );
                        }

                        @Override
                        public void mouseReleased(
                                MouseEvent e) {

                            arrastre = null;

                            setCursor(
                                    Cursor.getDefaultCursor()
                            );
                        }

                        @Override
                        public void mouseDragged(
                                MouseEvent e) {

                            if (arrastre == null) {
                                return;
                            }

                            Point actual =
                                    SwingUtilities.convertPoint(
                                            Lienzo.this,
                                            e.getPoint(),
                                            scroll.getViewport()
                                    );

                            posicionar(
                                    origen.x
                                            + arrastre.x
                                            - actual.x,
                                    origen.y
                                            + arrastre.y
                                            - actual.y
                            );
                        }

                        @Override
                        public void mouseClicked(
                                MouseEvent e) {

                            Nodo nodo =
                                    nodoEn(
                                            e.getPoint()
                                    );

                            if (nodo == null) {
                                return;
                            }

                            seleccionado =
                                    nodo;

                            detalle.setText(
                                    nodo.detalle
                            );

                            if (
                                    e.getClickCount() == 2
                                            && !nodo.hijos.isEmpty()
                            ) {

                                nodo.plegado =
                                        !nodo.plegado;

                                distribuir();

                                centrar(nodo);
                            }

                            repaint();
                        }
                    };

            addMouseListener(mouse);
            addMouseMotionListener(mouse);
        }

        @Override
        public String getToolTipText(
                MouseEvent e) {

            Nodo nodo =
                    nodoEn(
                            e.getPoint()
                    );

            return nodo == null
                    ? null
                    : nodo.detalle;
        }

        @Override
        protected void paintComponent(
                Graphics graphics) {

            Graphics2D g =
                    (Graphics2D) graphics.create();

            try {

                g.setColor(FONDO);

                g.fillRect(
                        0,
                        0,
                        getWidth(),
                        getHeight()
                );

                g.setFont(
                        getFont()
                );

                g.scale(
                        zoom,
                        zoom
                );

                g.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON
                );

                Rectangle clipOriginal =
                        g.getClipBounds();

                Rectangle2D clip;

                if (clipOriginal == null) {

                    clip =
                            new Rectangle2D.Double(
                                    0,
                                    0,
                                    getWidth() / zoom,
                                    getHeight() / zoom
                            );

                } else {

                    clip = clipOriginal;
                }

                g.setStroke(
                        new BasicStroke(
                                1.3f
                        )
                );

                g.setColor(BORDE);

                for (
                        Nodo nodo :
                        visibles
                ) {

                    if (nodo.padre == null) {
                        continue;
                    }

                    Nodo padre =
                            nodo.padre;

                    Rectangle2D bounds =
                            new Rectangle2D.Double(
                                    Math.min(
                                            padre.x,
                                            nodo.x
                                    ) - 2,
                                    padre.y,
                                    Math.abs(
                                            padre.x
                                                    - nodo.x
                                    ) + 4,
                                    nodo.y
                                            - padre.y
                                            + ALTO
                            );

                    if (
                            !bounds.intersects(
                                    clip
                            )
                    ) {
                        continue;
                    }

                    Path2D linea =
                            new Path2D.Double();

                    double medio =
                            nodo.y
                                    - (
                                    PASO_Y
                                            - ALTO
                            ) / 2.0;

                    linea.moveTo(
                            padre.x,
                            padre.y + ALTO
                    );

                    linea.lineTo(
                            padre.x,
                            medio
                    );

                    linea.lineTo(
                            nodo.x,
                            medio
                    );

                    linea.lineTo(
                            nodo.x,
                            nodo.y
                    );

                    g.draw(linea);
                }

                for (
                        Nodo nodo :
                        visibles
                ) {

                    Rectangle2D bounds =
                            new Rectangle2D.Double(
                                    nodo.x
                                            - nodo.ancho / 2.0,
                                    nodo.y,
                                    nodo.ancho,
                                    ALTO
                            );

                    if (
                            !bounds.intersects(
                                    clip
                            )
                    ) {
                        continue;
                    }

                    RoundRectangle2D caja =
                            new RoundRectangle2D.Double(
                                    bounds.getX(),
                                    nodo.y,
                                    nodo.ancho,
                                    ALTO,
                                    12,
                                    12
                            );

                    if (
                            nodo == seleccionado
                    ) {

                        g.setColor(AZUL);

                    } else if (
                            nodo.terminal
                    ) {

                        g.setColor(VERDE);

                    } else {

                        g.setColor(PANEL);
                    }

                    g.fill(caja);

                    if (
                            nodo == seleccionado
                    ) {

                        g.setColor(
                                AZUL_BORDE
                        );

                    } else if (
                            nodo.terminal
                    ) {

                        g.setColor(
                                VERDE_BORDE
                        );

                    } else {

                        g.setColor(BORDE);
                    }

                    g.draw(caja);

                    g.setColor(TEXTO);

                    FontMetrics fm =
                            g.getFontMetrics();

                    String texto =
                            nodo.texto;

                    int max =
                            (int) nodo.ancho
                                    - 32;

                    if (
                            fm.stringWidth(
                                    texto
                            ) > max
                    ) {

                        int fin =
                                Math.min(
                                        texto.length(),
                                        Math.max(
                                                1,
                                                max
                                                        / Math.max(
                                                        1,
                                                        fm.charWidth('m')
                                                )
                                        )
                                );

                        texto =
                                texto.substring(
                                        0,
                                        fin
                                ) + "…";
                    }

                    g.drawString(
                            texto,
                            (float) (
                                    nodo.x
                                            - fm.stringWidth(
                                            texto
                                    ) / 2.0
                            ),
                            (float) (
                                    nodo.y
                                            + 26
                            )
                    );

                    if (
                            !nodo.hijos.isEmpty()
                    ) {

                        g.drawString(
                                nodo.plegado
                                        ? "+"
                                        : "−",
                                (float) (
                                        bounds.getMaxX()
                                                - 17
                                ),
                                (float) (
                                        nodo.y
                                                + 26
                                )
                        );
                    }
                }

            } finally {

                g.dispose();
            }
        }
    }
}