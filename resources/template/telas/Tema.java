package __PACKAGE__.telas;

import java.awt.*;

import javax.swing.*;
import javax.swing.table.JTableHeader;

/** Tema unico claro, como no cadastro de eventos da disciplina. */
public final class Tema {
    public static final Color FUNDO = new Color(249, 250, 252),
            PAINEL = Color.WHITE,
            TEXTO = new Color(36, 44, 54),
            SUAVE = new Color(91, 103, 117),
            BORDA = new Color(218, 224, 231);

    private Tema() {}

    public static void aplicar(Component c) {
        if (c instanceof JPanel) c.setBackground(FUNDO);
        if (c instanceof JLabel) c.setForeground(TEXTO);
        if (c instanceof JButton) {
            JButton b = (JButton) c;
            b.setBackground(new Color(239, 242, 247));
            b.setForeground(TEXTO);
            b.setFocusPainted(false);
        }
        if (c instanceof JTextField) {
            c.setBackground(PAINEL);
            c.setForeground(TEXTO);
        }
        if (c instanceof JComboBox) {
            c.setBackground(PAINEL);
            c.setForeground(TEXTO);
        }
        if (c instanceof JTable) {
            JTable t = (JTable) c;
            t.setBackground(PAINEL);
            t.setForeground(TEXTO);
            t.setRowHeight(27);
            t.setGridColor(BORDA);
            t.setSelectionBackground(new Color(213, 228, 248));
            t.setSelectionForeground(TEXTO);
            JTableHeader h = t.getTableHeader();
            h.setBackground(new Color(240, 243, 247));
            h.setForeground(TEXTO);
        }
        if (c instanceof JTabbedPane) {
            c.setBackground(FUNDO);
            c.setForeground(TEXTO);
        }
        if (c instanceof JCheckBox || c instanceof JRadioButton) {
            c.setBackground(FUNDO);
            c.setForeground(TEXTO);
        }
        if (c instanceof Container)
            for (Component child : ((Container) c).getComponents()) aplicar(child);
    }
}
