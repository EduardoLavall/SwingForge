package __PACKAGE__.telas;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import javax.swing.*;
import javax.swing.table.JTableHeader;

public final class Tema {
    public static final Color FUNDO=new Color(12,12,15), PAINEL=new Color(24,24,29), TEXTO=new Color(239,239,244),
        SUAVE=new Color(162,162,172), ACENTO=new Color(121,129,255);
    private Tema() { }
    public static void aplicar(Component c){
        c.setFont(new Font("Dialog",Font.PLAIN,13));
        if(c instanceof JButton){JButton b=(JButton)c;b.setBackground(new Color(49,49,59));b.setForeground(TEXTO);
            b.setFocusPainted(false);b.setOpaque(true);b.setBorder(BorderFactory.createEmptyBorder(9,12,9,12));}
        else if(c instanceof JTextField){c.setBackground(PAINEL);c.setForeground(TEXTO);
            ((JTextField)c).setCaretColor(TEXTO);((JTextField)c).setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60,60,68)),BorderFactory.createEmptyBorder(7,8,7,8)));}
        else if(c instanceof JTable){JTable t=(JTable)c;t.setBackground(PAINEL);t.setForeground(TEXTO);
            t.setGridColor(new Color(48,48,55));t.setSelectionBackground(new Color(61,61,98));t.setRowHeight(29);
            JTableHeader h=t.getTableHeader();h.setBackground(new Color(37,37,43));h.setForeground(TEXTO);}
        else if(c instanceof JComboBox){c.setBackground(PAINEL);c.setForeground(TEXTO);}
        else if(c instanceof JLabel){c.setForeground(SUAVE);}
        else if(c instanceof JCheckBox){c.setBackground(PAINEL);c.setForeground(TEXTO);}
        else if(c instanceof JTabbedPane){c.setBackground(FUNDO);c.setForeground(TEXTO);}
        else if(c instanceof JPanel){c.setBackground(FUNDO);}
        if(c instanceof Container)for(Component child:((Container)c).getComponents())aplicar(child);
    }
}
