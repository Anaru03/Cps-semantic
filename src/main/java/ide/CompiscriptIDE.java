package ide;

import antlr.CompiscriptLexer;
import antlr.CompiscriptParser;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.tree.ParseTree;
import compiler.Compilador;
import compiler.ResultadoCompilacion;

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
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
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
    private final PanelesCompilacion.Errores errores = new PanelesCompilacion.Errores(this::irALinea);
    private final PanelesCompilacion.CodigoIntermedio codigoIntermedio = new PanelesCompilacion.CodigoIntermedio();
    private final PanelesCompilacion.Estructuras estructuras = new PanelesCompilacion.Estructuras();
    private final JTabbedPane pestanas = new JTabbedPane();
    private java.io.File archivoActual;
    private final DiagramaArbol arbolSintactico = new DiagramaArbol();
    private final TablaSimbolosPanel tablaSimbolos = new TablaSimbolosPanel();
    private final JLabel estado = new JLabel(" Listo");

    public CompiscriptIDE() {
        super("Compiscript IDE");
        UIManager.put("TabbedPane.selected", new Color(37, 99, 235));
        UIManager.put("TabbedPane.contentAreaColor", PANEL);
        UIManager.put("TabbedPane.borderHightlightColor", BORDE);
        UIManager.put("TabbedPane.darkShadow", BORDE);
        UIManager.put("TabbedPane.light", PANEL);
        UIManager.put("TabbedPane.highlight", BORDE);
        UIManager.put("TabbedPane.shadow", BORDE);
        UIManager.put("TabbedPane.focus", new Color(37, 99, 235));
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

        JButton botonCompilar = new JButton("Compilar");
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

        pestanas.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        pestanas.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        pestanas.setBackground(PANEL);
        pestanas.setForeground(TEXTO);
        pestanas.setBorder(BorderFactory.createLineBorder(BORDE));
        pestanas.addTab("Errores", errores);
        pestanas.addTab("Código intermedio (TAC)", codigoIntermedio);
        pestanas.addTab("Árbol sintáctico", arbolSintactico);
        pestanas.addTab("Tabla de símbolos", tablaSimbolos);
        pestanas.addTab("Registros y clases", estructuras);

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
        JLabel subtitulo = new JLabel("Análisis léxico, sintáctico y semántico · Generación de código intermedio (TAC)");
        subtitulo.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        subtitulo.setForeground(TEXTO_SUAVE);
        marca.add(titulo, BorderLayout.NORTH);
        marca.add(subtitulo, BorderLayout.SOUTH);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        acciones.setOpaque(false);
        estado.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        estado.setForeground(TEXTO_SUAVE);
        acciones.add(estado);
        acciones.add(botonSecundario("Abrir…", this::abrirArchivo));
        acciones.add(botonSecundario("Guardar", this::guardarArchivo));
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

    private JButton botonSecundario(String texto, java.util.function.Consumer<ActionEvent> accion) {
        JButton boton = new JButton(texto);
        boton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        boton.setForeground(TEXTO); boton.setBackground(PANEL); boton.setOpaque(true);
        boton.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDE),
                BorderFactory.createEmptyBorder(9, 14, 9, 14)));
        boton.setFocusPainted(false);
        boton.addActionListener(accion::accept);
        return boton;
    }

    private void irALinea(int linea) {
        try {
            int inicio = editor.getLineStartOffset(Math.max(0, linea - 1));
            int fin = editor.getLineEndOffset(Math.max(0, linea - 1));
            editor.requestFocusInWindow();
            editor.select(inicio, Math.max(inicio, fin - 1));
        } catch (javax.swing.text.BadLocationException ignorada) { /* línea fuera de rango */ }
    }

    private void abrirArchivo(ActionEvent evento) {
        JFileChooser selector = new JFileChooser(archivoActual != null ? archivoActual.getParentFile()
                : new java.io.File(System.getProperty("user.dir")));
        selector.setDialogTitle("Seleccionar archivo Compiscript");
        selector.setFileFilter(new FileNameExtensionFilter("Archivos Compiscript (*.cps)", "cps"));
        if (selector.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        cargarArchivo(selector.getSelectedFile());
    }

    /** Carga un archivo en el editor y lo compila de inmediato. */
    void cargarArchivo(java.io.File archivo) {
        try {
            editor.setText(java.nio.file.Files.readString(archivo.toPath()));
            editor.setCaretPosition(0);
            archivoActual = archivo;
            setTitle("Compiscript IDE — " + archivo.getName());
            compilar(null);
        } catch (java.io.IOException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo leer el archivo:\n" + ex.getMessage(),
                    "Error de lectura", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void guardarArchivo(ActionEvent evento) {
        java.io.File destino = archivoActual;
        if (destino == null) {
            JFileChooser selector = new JFileChooser(new java.io.File(System.getProperty("user.dir")));
            selector.setFileFilter(new FileNameExtensionFilter("Archivos Compiscript (*.cps)", "cps"));
            if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            destino = selector.getSelectedFile();
            if (!destino.getName().contains(".")) destino = new java.io.File(destino.getPath() + ".cps");
        }
        try {
            java.nio.file.Files.writeString(destino.toPath(), editor.getText());
            archivoActual = destino; setTitle("Compiscript IDE — " + destino.getName());
            estado.setForeground(TEXTO_SUAVE); estado.setText(" Guardado");
        } catch (java.io.IOException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo guardar:\n" + ex.getMessage(),
                    "Error de escritura", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Ejecuta el pipeline completo y refresca todas las vistas. */
    private void compilar(ActionEvent evento) {
        try {
            ResultadoCompilacion resultado = Compilador.compilar(editor.getText());
            errores.mostrar(resultado);
            if (resultado.arbol() != null) arbolSintactico.mostrar(resultado.arbol());
            if (resultado.analisis() != null) tablaSimbolos.mostrar(resultado.analisis());
            else tablaSimbolos.limpiar("No disponible: hay errores léxicos o sintácticos");
            if (resultado.esValido()) {
                codigoIntermedio.mostrar(resultado);
                estructuras.mostrar(resultado);
                estado.setForeground(VERDE);
                estado.setText("● Compilación exitosa");
                pestanas.setSelectedComponent(codigoIntermedio);
            } else {
                codigoIntermedio.limpiar("No se genera código intermedio mientras existan errores.");
                estructuras.limpiar();
                estado.setForeground(ROJO);
                estado.setText("● " + resultado.errores().size() + " error(es)");
                pestanas.setSelectedComponent(errores);
            }
        } catch (Exception ex) {
            errores.mensaje("Error inesperado: " + ex);
            estado.setForeground(ROJO);
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
