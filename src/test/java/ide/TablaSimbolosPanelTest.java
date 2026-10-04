package ide;

import org.junit.jupiter.api.Test;
import semantic.AnalizadorSemantico;
import javax.swing.*;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.*;

class TablaSimbolosPanelTest {
    @Test void incluyeLocalesParametrosYSombrasConAmbitosDistintos() throws Exception {
        var analisis = AnalizadorSemantico.analizar("let x = 1; { let x = 2; }"
                + " function f(p: integer): integer { let x = p; return x; }");
        SwingUtilities.invokeAndWait(() -> {
            var panel = new TablaSimbolosPanel(); panel.mostrar(analisis); var tabla = panel.tabla();
            assertEquals(5, tabla.getRowCount());
            var rutas = new HashSet<String>();
            for (int i = 0; i < tabla.getRowCount(); i++) if (tabla.getValueAt(i, 0).equals("x"))
                assertTrue(rutas.add((String) tabla.getValueAt(i, 3)));
            assertEquals(3, rutas.size()); assertFalse(tabla.isCellEditable(0, 0));
            panel.filtro().setText("x"); assertEquals(3, tabla.getRowCount());
            panel.filtro().setText(""); tabla.getRowSorter().toggleSortOrder(0);
            assertEquals("f", tabla.getValueAt(0, 0));
        });
    }
    @Test void filtroEsLiteralYLimpiarDescartaResultadosAnteriores() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var panel = new TablaSimbolosPanel();
            panel.mostrar(AnalizadorSemantico.analizar("let lista: integer[] = [1, 2]; let n = 3;"));
            panel.filtro().setText("[]"); assertEquals(1, panel.tabla().getRowCount());
            panel.limpiar("No disponible"); assertEquals(0, panel.tabla().getModel().getRowCount());
        });
    }
}
