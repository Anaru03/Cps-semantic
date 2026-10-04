package ide;

import antlr.*;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import static org.junit.jupiter.api.Assertions.*;

class DiagramaArbolTest {
    private DiagramaArbol.Modelo modelo(String codigo) {
        var parser = new CompiscriptParser(new CommonTokenStream(new CompiscriptLexer(CharStreams.fromString(codigo))));
        return DiagramaArbol.construir(parser.program(), parser.getRuleNames());
    }
    @Test void conservaReglasTokensYOrdenDelParser() {
        var modelo = modelo("let x = 1;");
        assertEquals("program", modelo.raiz.texto);
        var terminales = modelo.nodos.stream().filter(n -> n.terminal).map(n -> n.texto).toList();
        assertEquals(java.util.List.of("let", "x", "=", "1", ";", "<EOF>"), terminales);
        assertTrue(modelo.nodos.stream().anyMatch(n -> n.detalle.contains("línea 1")));
    }
    @Test void construccionYLayoutNoDesbordanLaPilaConMilesDeNiveles() throws Exception {
        var raiz = new CompiscriptParser.BlockContext(null, 0);
        var ultimo = raiz;
        for (int i = 0; i < 15000; i++) {
            var hijo = new CompiscriptParser.BlockContext(ultimo, 0); ultimo.addChild(hijo); ultimo = hijo;
        }
        var modelo = DiagramaArbol.construir(raiz, CompiscriptParser.ruleNames);
        assertEquals(15001, modelo.nodos.size());
        SwingUtilities.invokeAndWait(() -> {
            var panel = new DiagramaArbol(); panel.establecer(modelo);
            for (var nodo : modelo.nodos) nodo.plegado = false;
            panel.establecer(modelo);
            assertTrue(modelo.nodos.get(15000).y > 1000000);
        });
    }
    @Test void ramasAmpliasNoSeSuperponenYSePuedenPlegar() throws Exception {
        var modelo = modelo("let x = 1;".repeat(3000));
        SwingUtilities.invokeAndWait(() -> {
            var panel = new DiagramaArbol(); panel.establecer(modelo);
            double fin = -1;
            for (var hijo : modelo.raiz.hijos) {
                assertTrue(hijo.x - hijo.ancho / 2 > fin);
                fin = hijo.x + hijo.ancho / 2;
            }
            modelo.raiz.plegado = true; panel.establecer(modelo);
            assertEquals(modelo.raiz.ancho, modelo.raiz.espacio);
        });
    }
    @Test void renderizaElDiagramaSinVentanaNativa() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var panel = new DiagramaArbol(); panel.setSize(1000, 650);
            var modelo = modelo("let x = 1; if (x < 2) { x = x + 1; }");
            panel.establecer(modelo); distribuir(panel);
            var imagen = new BufferedImage(1000, 650, BufferedImage.TYPE_INT_RGB);
            var g = imagen.createGraphics(); panel.paint(g); g.dispose();
            assertNotEquals(0, imagen.getRGB(500, 300));
            try { javax.imageio.ImageIO.write(imagen, "png", new java.io.File("/tmp/compiscript-diagrama.png")); }
            catch (java.io.IOException error) { throw new AssertionError(error); }
        });
    }
    @Test void busquedaAbreRamasPlegadasYZoomPermiteRecuperarLectura() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var panel = new DiagramaArbol(); panel.setSize(700, 500);
            var modelo = modelo("let needle = 12345;"); panel.establecer(modelo); distribuir(panel);
            var campo = encontrar(panel, JTextField.class, null);
            assertNotNull(campo); campo.setText("12345"); campo.postActionEvent();
            var token = modelo.nodos.stream().filter(n -> n.texto.equals("12345")).findFirst().orElseThrow();
            for (var padre = token.padre; padre != null; padre = padre.padre) assertFalse(padre.plegado);
            JButton menos = encontrar(panel, JButton.class, "−");
            JButton normal = encontrar(panel, JButton.class, "100%");
            assertNotNull(menos); assertNotNull(normal); menos.doClick(); normal.doClick();
            JLabel estado = encontrar(panel, JLabel.class, null);
            assertTrue(estado.getText().startsWith("100%"));
        });
    }
    private <T extends Component> T encontrar(Container contenedor, Class<T> clase, String texto) {
        for (var componente : contenedor.getComponents()) {
            if (clase.isInstance(componente) && (texto == null
                    || componente instanceof JButton boton && boton.getText().equals(texto))) return clase.cast(componente);
            if (componente instanceof Container hijo) {
                T encontrado = encontrar(hijo, clase, texto); if (encontrado != null) return encontrado;
            }
        }
        return null;
    }
    private void distribuir(Container contenedor) {
        contenedor.doLayout();
        for (var componente : contenedor.getComponents()) if (componente instanceof Container hijo) distribuir(hijo);
    }
}
