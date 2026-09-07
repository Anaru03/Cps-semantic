package ide;

import antlr.CompiscriptLexer;
import antlr.CompiscriptParser;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.Trees;
import semantic.AnalisisSemantico;
import semantic.AnalizadorSemantico;
import semantic.Ambito;
import semantic.ResultadoSemantico;
import semantic.Simbolo;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * IDE minimo para Compiscript: permite escribir codigo, compilarlo (analisis lexico,
 * sintactico y semantico) y ver los resultados en tres vistas: errores, arbol
 * sintactico (representacion visual) y tabla de simbolos.
 *
 * Para ejecutarlo: mvn compile exec:java -Dexec.mainClass="ide.CompiscriptIDE"
 * (o directamente el metodo main desde el IDE de Java).
 */
public final class CompiscriptIDE extends JFrame {

    private static final Color FONDO = new Color(15, 23, 42);
    private static final Color PANEL = new Color(30, 41, 59);
    private static final Color BORDE = new Color(51, 65, 85);
    private static final Color TEXTO = new Color(226, 232, 240);
    private static final Color TEXTO_SUAVE = new Color(148, 163, 184);
    private static final Color AZUL = new Color(37, 99, 235);
    private static final Color VERDE = new Color(34, 197, 94);
    private static final Color ROJO = new Color(248, 113, 113);

    private final JTextArea editor = new JTextArea();
    private final JTextArea salida = new JTextArea();
    private final JTree arbolSintactico = new JTree(new DefaultMutableTreeNode("(sin compilar)"));
    private final JTree tablaSimbolos = new JTree(new DefaultMutableTreeNode("(sin compilar)"));
    private final JLabel estado = new JLabel(" Listo");

    public CompiscriptIDE() {
        super("Compiscript IDE");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 800);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
        construirInterfaz();
    }

    private void construirInterfaz() {
        JPanel contenido = new JPanel(new BorderLayout(0, 12));
        contenido.setBackground(FONDO);
        contenido.setBorder(BorderFactory.createEmptyBorder(14, 16, 16, 16));

        editor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 15));
        editor.setTabSize(2);
        editor.setText(EJEMPLO);
        editor.setBackground(FONDO);
        editor.setForeground(TEXTO);
        editor.setCaretColor(TEXTO);
        editor.setSelectionColor(AZUL);
        editor.setSelectedTextColor(Color.WHITE);
        editor.setMargin(new Insets(10, 10, 10, 10));
        JScrollPane panelEditor = new JScrollPane(editor);
        panelEditor.setBorder(BorderFactory.createLineBorder(BORDE));
        panelEditor.getViewport().setBackground(FONDO);
        panelEditor.setRowHeaderView(crearNumerosLinea());

        JButton botonCompilar = new JButton("▶  Compilar");
        botonCompilar.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        botonCompilar.setForeground(Color.WHITE);
        botonCompilar.setBackground(AZUL);
        botonCompilar.setOpaque(true);
        botonCompilar.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        botonCompilar.setFocusPainted(false);
        botonCompilar.setToolTipText("Compilar (Ctrl+Enter)");
        botonCompilar.addActionListener(this::compilar);
        editor.getInputMap().put(KeyStroke.getKeyStroke("control ENTER"), "compilar");
        editor.getActionMap().put("compilar", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { compilar(e); }
        });

        salida.setEditable(false);
        salida.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        salida.setBackground(PANEL);
        salida.setForeground(TEXTO);
        salida.setMargin(new Insets(12, 12, 12, 12));
        JScrollPane panelSalida = new JScrollPane(salida);
        estilizarScroll(panelSalida);

        JScrollPane panelArbol = new JScrollPane(arbolSintactico);
        JScrollPane panelTabla = new JScrollPane(tablaSimbolos);
        estilizarArbol(arbolSintactico);
        estilizarArbol(tablaSimbolos);
        estilizarScroll(panelArbol);
        estilizarScroll(panelTabla);

        JTabbedPane pestanas = new JTabbedPane();
        pestanas.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        pestanas.setBackground(PANEL);
        pestanas.setForeground(TEXTO);
        pestanas.setBorder(BorderFactory.createLineBorder(BORDE));
        pestanas.addTab("Errores", panelSalida);
        pestanas.addTab("Árbol sintáctico", panelArbol);
        pestanas.addTab("Tabla de símbolos", panelTabla);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelEditor, pestanas);
        split.setResizeWeight(0.56);
        split.setDividerSize(8);
        split.setBorder(null);
        split.setBackground(FONDO);

        JPanel superior = new JPanel(new BorderLayout(16, 0));
        superior.setOpaque(false);
        JPanel marca = new JPanel(new BorderLayout());
        marca.setOpaque(false);
        JLabel titulo = new JLabel("Compiscript IDE");
        titulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        titulo.setForeground(Color.WHITE);
        JLabel subtitulo = new JLabel("Analizador léxico, sintáctico y semántico");
        subtitulo.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        subtitulo.setForeground(TEXTO_SUAVE);
        marca.add(titulo, BorderLayout.NORTH);
        marca.add(subtitulo, BorderLayout.SOUTH);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        acciones.setOpaque(false);
        estado.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        estado.setForeground(TEXTO_SUAVE);
        acciones.add(estado);
        acciones.add(botonCompilar);
        superior.add(marca, BorderLayout.WEST);
        superior.add(acciones, BorderLayout.EAST);

        setLayout(new BorderLayout());
        getContentPane().setBackground(FONDO);
        contenido.add(superior, BorderLayout.NORTH);
        contenido.add(split, BorderLayout.CENTER);
        add(contenido, BorderLayout.CENTER);
    }

    private JTextArea crearNumerosLinea() {
        JTextArea numeros = new JTextArea("1");
        numeros.setEditable(false);
        numeros.setFocusable(false);
        numeros.setFont(editor.getFont());
        numeros.setBackground(PANEL);
        numeros.setForeground(TEXTO_SUAVE);
        numeros.setMargin(new Insets(10, 8, 10, 8));
        numeros.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BORDE));
        Runnable actualizar = () -> {
            int lineas = editor.getLineCount();
            StringBuilder texto = new StringBuilder();
            for (int i = 1; i <= lineas; i++) texto.append(i).append('\n');
            numeros.setText(texto.toString());
        };
        editor.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { actualizar.run(); }
            @Override public void removeUpdate(DocumentEvent e) { actualizar.run(); }
            @Override public void changedUpdate(DocumentEvent e) { actualizar.run(); }
        });
        actualizar.run();
        return numeros;
    }

    private void estilizarScroll(JScrollPane scroll) {
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(PANEL);
    }

    private void estilizarArbol(JTree arbol) {
        arbol.setBackground(PANEL);
        arbol.setForeground(TEXTO);
        arbol.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        arbol.setRowHeight(24);
        arbol.setShowsRootHandles(true);
        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
        renderer.setBackgroundNonSelectionColor(PANEL);
        renderer.setBackgroundSelectionColor(AZUL);
        renderer.setTextNonSelectionColor(TEXTO);
        renderer.setTextSelectionColor(Color.WHITE);
        renderer.setBorderSelectionColor(AZUL);
        arbol.setCellRenderer(renderer);
    }

    private void compilar(ActionEvent evento) {
        String codigo = editor.getText();
        try {
            CompiscriptLexer lexer = new CompiscriptLexer(CharStreams.fromString(codigo));
            List<String> erroresLexicos = new ArrayList<>();
            registrarErroresLexicos(lexer, erroresLexicos::add);
            CompiscriptParser parser = new CompiscriptParser(new CommonTokenStream(lexer));
            parser.removeErrorListeners();
            List<String> erroresSintaxis = new ArrayList<>();
            parser.addErrorListener(new BaseErrorListener() {
                @Override public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                                    int linea, int columna, String mensaje, RecognitionException ex) {
                    erroresSintaxis.add("Sintaxis " + linea + ":" + columna + " - " + mensaje);
                }
            });
            ParseTree arbol = parser.program();
            arbolSintactico.setModel(new DefaultTreeModel(nodoDelArbol(arbol, parser)));
            expandirHasta(arbolSintactico, 2);

            if (!erroresLexicos.isEmpty() || !erroresSintaxis.isEmpty()) {
                List<String> erroresAnalisis = new ArrayList<>(erroresLexicos);
                erroresAnalisis.addAll(erroresSintaxis);
                salida.setForeground(ROJO);
                salida.setText(String.join("\n", erroresAnalisis));
                estado.setForeground(ROJO);
                estado.setText("● Error de análisis (" + erroresAnalisis.size() + ")");
                tablaSimbolos.setModel(new DefaultTreeModel(new DefaultMutableTreeNode(
                        "(no disponible: hay errores lexicos o sintacticos)")));
                return;
            }

            AnalisisSemantico analisis = AnalizadorSemantico.analizar(codigo);
            ResultadoSemantico resultado = analisis.resultado();
            tablaSimbolos.setModel(new DefaultTreeModel(nodoDelAmbito(analisis.ambitoGlobal())));
            expandirHasta(tablaSimbolos, 3);

            if (resultado.esValido()) {
                salida.setForeground(VERDE);
                salida.setText("Compilacion exitosa: no se encontraron errores semanticos.");
                estado.setForeground(VERDE);
                estado.setText("● Compilación exitosa");
            } else {
                salida.setForeground(ROJO);
                StringBuilder texto = new StringBuilder();
                for (var error : resultado.errores())
                    texto.append(error.linea()).append(':').append(error.columna())
                         .append(" - ").append(error.descripcion()).append('\n');
                salida.setText(texto.toString());
                estado.setForeground(ROJO);
                estado.setText("● " + resultado.cantidadErrores() + " error(es)");
            }
        } catch (Exception ex) {
            salida.setForeground(ROJO);
            salida.setText("Error inesperado: " + ex);
            estado.setText(" Error inesperado");
        }
    }

    /** Conecta los errores del lexer con la salida visible del IDE. */
    static void registrarErroresLexicos(CompiscriptLexer lexer, Consumer<String> receptor) {
        lexer.removeErrorListeners();
        lexer.addErrorListener(new BaseErrorListener() {
            @Override public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                              int linea, int columna, String mensaje,
                                              RecognitionException ex) {
                receptor.accept("Lexico " + linea + ":" + columna + " - " + mensaje);
            }
        });
    }

    /** Construye la representacion visual (JTree) del arbol sintactico generado por ANTLR. */
    private DefaultMutableTreeNode nodoDelArbol(ParseTree nodo, CompiscriptParser parser) {
        String etiqueta = nodo instanceof TerminalNode
                ? "'" + nodo.getText() + "'"
                : Trees.getNodeText(nodo, Arrays.asList(parser.getRuleNames()));
        DefaultMutableTreeNode raiz = new DefaultMutableTreeNode(etiqueta);
        for (int i = 0; i < nodo.getChildCount(); i++) raiz.add(nodoDelArbol(nodo.getChild(i), parser));
        return raiz;
    }

    /** Construye la representacion visual (JTree) de la tabla de simbolos, ambito por ambito. */
    private DefaultMutableTreeNode nodoDelAmbito(Ambito ambito) {
        DefaultMutableTreeNode raiz = new DefaultMutableTreeNode(ambito.nombre());
        for (Simbolo simbolo : ambito.simbolos()) {
            DefaultMutableTreeNode nodoSimbolo = new DefaultMutableTreeNode(
                    simbolo.categoria() + " " + simbolo.nombre() + " : " + simbolo.tipo());
            if (simbolo.miembros() != null) nodoSimbolo.add(nodoDelAmbito(simbolo.miembros()));
            raiz.add(nodoSimbolo);
        }
        return raiz;
    }

    /**
     * Expande solo unos niveles. Expandir el arbol completo puede contener miles de
     * nodos y provocar recursion en la capa de accesibilidad nativa de Swing en macOS.
     */
    private void expandirHasta(JTree arbol, int profundidadMaxima) {
        Object raiz = arbol.getModel().getRoot();
        if (raiz instanceof TreeNode nodo) {
            expandirHasta(arbol, new TreePath(nodo), 0, profundidadMaxima);
        }
    }

    private void expandirHasta(JTree arbol, TreePath ruta, int profundidad, int maxima) {
        arbol.expandPath(ruta);
        if (profundidad >= maxima) return;
        TreeNode nodo = (TreeNode) ruta.getLastPathComponent();
        for (int i = 0; i < nodo.getChildCount(); i++) {
            expandirHasta(arbol, ruta.pathByAddingChild(nodo.getChildAt(i)), profundidad + 1, maxima);
        }
    }

    private static final String EJEMPLO = """
            class Persona {
              let edad: integer;
              function constructor(e: integer) { this.edad = e; }
              function esMayorDeEdad(): boolean { return this.edad >= 18; }
            }

            function sumar(a: integer, b: integer): integer {
              return a + b;
            }

            let p = new Persona(20);
            let resultado: integer = sumar(2, 3);
            let xs: integer[] = [1, 2, 3];
            let promedio: float = 1 + 2.5;

            if (p.esMayorDeEdad()) {
              print(resultado);
            }

            for (let i = 0; i < 3; i = i + 1) {
              print(xs[i]);
            }
            """;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            UIManager.put("ToolTip.background", PANEL);
            UIManager.put("ToolTip.foreground", TEXTO);
            new CompiscriptIDE().setVisible(true);
        });
    }
}
