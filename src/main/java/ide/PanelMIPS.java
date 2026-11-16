
package ide;

import mips.GeneradorMIPS;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

final class PanelMIPS extends JPanel {

    private final JTextArea codigo = new JTextArea();
    private final JLabel resumen = new JLabel(
            "Compile un programa para generar ensamblador MIPS."
    );

    private final JButton copiar = new JButton("Copiar");
    private final JButton guardar = new JButton("Guardar .asm");
    private final JButton alternar = new JButton("Ver sin optimizar");

    private String optimizado = "";
    private String original = "";
    private boolean mostrarOptimizado = true;
    private boolean disponible = false;

    PanelMIPS() {
        super(new BorderLayout(0, 10));

        setBackground(PanelesCompilacion.PANEL);
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        codigo.setEditable(false);
        codigo.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        codigo.setBackground(PanelesCompilacion.PANEL);
        codigo.setForeground(PanelesCompilacion.TEXTO);
        codigo.setCaretColor(Color.WHITE);
        codigo.setMargin(new Insets(12, 12, 12, 12));

        JScrollPane scroll = new JScrollPane(codigo);
        scroll.setBorder(BorderFactory.createLineBorder(
                new Color(51, 65, 85)
        ));

        JPanel superior = new JPanel(new BorderLayout(10, 0));
        superior.setOpaque(false);

        resumen.setForeground(PanelesCompilacion.SUAVE);
        resumen.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);

        configurarBoton(copiar);
        configurarBoton(guardar);
        configurarBoton(alternar);

        botones.add(alternar);
        botones.add(copiar);
        botones.add(guardar);

        superior.add(resumen, BorderLayout.CENTER);
        superior.add(botones, BorderLayout.EAST);

        add(superior, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);

        copiar.addActionListener(e -> copiarCodigo());
        guardar.addActionListener(e -> guardarCodigo());
        alternar.addActionListener(e -> cambiarVersion());

        limpiar("Compile un programa para generar ensamblador MIPS.");
    }

    private void configurarBoton(JButton boton) {
        boton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        boton.setForeground(Color.WHITE);
        boton.setBackground(new Color(30, 64, 175));
        boton.setFocusPainted(false);
        boton.setBorder(BorderFactory.createEmptyBorder(9, 12, 9, 12));
    }

    void mostrar(GeneradorMIPS.Resultado resultado) {
        if (!resultado.esValido()) {
            limpiar("No se pudo generar ensamblador MIPS:\n"
                    + String.join("\n", resultado.errores()));
            return;
        }

        optimizado = resultado.codigo();
        original = resultado.sinOptimizar();
        disponible = !optimizado.isBlank();
        mostrarOptimizado = true;

        actualizarCodigo();
        actualizarResumen(resultado.optimizaciones());
        actualizarBotones();
    }

    void limpiar(String mensaje) {
        optimizado = "";
        original = "";
        disponible = false;
        mostrarOptimizado = true;

        codigo.setText(mensaje);
        codigo.setCaretPosition(0);
        resumen.setText("Código ensamblador MIPS");

        actualizarBotones();
    }

    private void actualizarCodigo() {
        codigo.setText(mostrarOptimizado ? optimizado : original);
        codigo.setCaretPosition(0);

        alternar.setText(
                mostrarOptimizado
                        ? "Ver sin optimizar"
                        : "Ver optimizado"
        );
    }

    private void actualizarResumen(Map<String, Integer> optimizaciones) {
        int total = optimizaciones.values().stream()
                .mapToInt(Integer::intValue)
                .sum();

        long reglas = optimizaciones.values().stream()
                .filter(valor -> valor > 0)
                .count();

        resumen.setText(
                "MIPS generado | "
                        + reglas + " reglas activadas | "
                        + total + " optimizaciones aplicadas"
        );
    }

    private void actualizarBotones() {
        copiar.setEnabled(disponible);
        guardar.setEnabled(disponible);
        alternar.setEnabled(disponible && !optimizado.equals(original));
    }

    private void cambiarVersion() {
        if (!disponible) {
            return;
        }

        mostrarOptimizado = !mostrarOptimizado;
        actualizarCodigo();
    }

    private void copiarCodigo() {
        if (!disponible) {
            return;
        }

        Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(
                        new StringSelection(codigo.getText()),
                        null
                );

        JOptionPane.showMessageDialog(
                this,
                "Código MIPS copiado al portapapeles.",
                "FibLang / Compiscript MIPS",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void guardarCodigo() {
        if (!disponible) {
            return;
        }

        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Guardar código ensamblador MIPS");
        selector.setFileFilter(
                new FileNameExtensionFilter(
                        "Ensamblador MIPS (*.asm)", "asm"
                )
        );

        selector.setSelectedFile(new File("programa.asm"));

        if (selector.showSaveDialog(this)
                != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File destino = selector.getSelectedFile();

        if (!destino.getName().toLowerCase().endsWith(".asm")) {
            destino = new File(destino.getPath() + ".asm");
        }

        if (destino.exists()) {
            int confirmar = JOptionPane.showConfirmDialog(
                    this,
                    "El archivo ya existe. ¿Desea reemplazarlo?",
                    "Confirmar",
                    JOptionPane.YES_NO_OPTION
            );

            if (confirmar != JOptionPane.YES_OPTION) {
                return;
            }
        }

        try {
            Files.writeString(destino.toPath(), codigo.getText());

            JOptionPane.showMessageDialog(
                    this,
                    "Ensamblador guardado en:\n" + destino.getAbsolutePath(),
                    "Archivo guardado",
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "No se pudo guardar el archivo:\n" + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    String contenido() {
        return codigo.getText();
    }
}
