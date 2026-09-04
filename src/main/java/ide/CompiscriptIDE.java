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
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * IDE minimo para Compiscript: permite escribir codigo, compilarlo (analisis lexico,
 * sintactico y semantico) y ver los resultados en tres vistas: errores, arbol
 * sintactico (representacion visual) y tabla de simbolos.
 *
 * Para ejecutarlo: mvn compile exec:java -Dexec.mainClass="ide.CompiscriptIDE"
 * (o directamente el metodo main desde el IDE de Java).
 */
public final class CompiscriptIDE extends JFrame {

    private final JTextArea editor = new JTextArea();
    private final JTextArea salida = new JTextArea();
    private final JTree arbolSintactico = new JTree(new DefaultMutableTreeNode("(sin compilar)"));
    private final JTree tablaSimbolos = new JTree(new DefaultMutableTreeNode("(sin compilar)"));
    private final JLabel estado = new JLabel(" Listo");

    public CompiscriptIDE() {
        super("Compiscript IDE");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1150, 720);
        setLocationRelativeTo(null);
        construirInterfaz();
    }

    private void construirInterfaz() {
        editor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        editor.setTabSize(2);
        editor.setText(EJEMPLO);
        JScrollPane panelEditor = new JScrollPane(editor);
        panelEditor.setBorder(BorderFactory.createTitledBorder("Codigo Compiscript"));

        JButton botonCompilar = new JButton("Compilar  (Ctrl+Enter)");
        botonCompilar.addActionListener(this::compilar);
        editor.getInputMap().put(KeyStroke.getKeyStroke("control ENTER"), "compilar");
        editor.getActionMap().put("compilar", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { compilar(e); }
        });

        salida.setEditable(false);
        salida.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JScrollPane panelSalida = new JScrollPane(salida);

        JScrollPane panelArbol = new JScrollPane(arbolSintactico);
        JScrollPane panelTabla = new JScrollPane(tablaSimbolos);

        JTabbedPane pestanas = new JTabbedPane();
        pestanas.addTab("Errores", panelSalida);
        pestanas.addTab("Arbol sintactico", panelArbol);
        pestanas.addTab("Tabla de simbolos", panelTabla);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelEditor, pestanas);
        split.setResizeWeight(0.5);

        JPanel superior = new JPanel(new BorderLayout());
        superior.add(botonCompilar, BorderLayout.WEST);
        superior.add(estado, BorderLayout.CENTER);

        setLayout(new BorderLayout());
        add(superior, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
    }

    private void compilar(ActionEvent evento) {
        String codigo = editor.getText();
        try {
            CompiscriptLexer lexer = new CompiscriptLexer(CharStreams.fromString(codigo));
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
            expandirTodo(arbolSintactico);

            if (!erroresSintaxis.isEmpty()) {
                salida.setForeground(Color.RED.darker());
                salida.setText(String.join("\n", erroresSintaxis));
                estado.setText(" Error de sintaxis (" + erroresSintaxis.size() + ")");
                tablaSimbolos.setModel(new DefaultTreeModel(new DefaultMutableTreeNode("(no disponible: hay errores de sintaxis)")));
                return;
            }

            AnalisisSemantico analisis = AnalizadorSemantico.analizar(codigo);
            ResultadoSemantico resultado = analisis.resultado();
            tablaSimbolos.setModel(new DefaultTreeModel(nodoDelAmbito(analisis.ambitoGlobal())));
            expandirTodo(tablaSimbolos);

            if (resultado.esValido()) {
                salida.setForeground(new Color(0, 128, 0));
                salida.setText("Compilacion exitosa: no se encontraron errores semanticos.");
                estado.setText(" Compilacion exitosa");
            } else {
                salida.setForeground(Color.RED.darker());
                StringBuilder texto = new StringBuilder();
                for (var error : resultado.errores())
                    texto.append(error.linea()).append(':').append(error.columna())
                         .append(" - ").append(error.descripcion()).append('\n');
                salida.setText(texto.toString());
                estado.setText(" " + resultado.cantidadErrores() + " error(es) semantico(s)");
            }
        } catch (Exception ex) {
            salida.setForeground(Color.RED.darker());
            salida.setText("Error inesperado: " + ex);
            estado.setText(" Error inesperado");
        }
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

    private void expandirTodo(JTree arbol) {
        for (int i = 0; i < arbol.getRowCount(); i++) arbol.expandRow(i);
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
        SwingUtilities.invokeLater(() -> new CompiscriptIDE().setVisible(true));
    }
}
