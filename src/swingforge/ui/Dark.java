package swingforge.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Paleta black-style leve; nenhum tema externo necessario. */
public final class Dark {
    public static final Color BG=new Color(10,10,12), PANEL=new Color(22,22,26), PANEL2=new Color(31,31,37),
        TEXT=new Color(239,239,242), MUTED=new Color(159,159,169), BORDER=new Color(47,47,56),
        ACCENT=new Color(125,124,242), SUCCESS=new Color(130,212,163);
    private Dark() { }
    public static void defaults(){
        try{UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());}catch(Exception ignored){}
        UIManager.put("Panel.background",BG);UIManager.put("Label.foreground",TEXT);
        UIManager.put("OptionPane.background",PANEL);UIManager.put("OptionPane.messageForeground",TEXT);
        UIManager.put("TextField.background",PANEL2);UIManager.put("TextField.foreground",TEXT);
        UIManager.put("TextField.caretForeground",TEXT);UIManager.put("TextArea.background",PANEL);
        UIManager.put("TextArea.foreground",TEXT);UIManager.put("Table.background",PANEL);
        UIManager.put("Table.foreground",TEXT);UIManager.put("Table.selectionBackground",new Color(65,59,101));
        UIManager.put("TableHeader.background",PANEL2);UIManager.put("TableHeader.foreground",TEXT);
        UIManager.put("List.background",PANEL);UIManager.put("List.foreground",TEXT);
        UIManager.put("List.selectionBackground",new Color(65,59,101));
        UIManager.put("List.selectionForeground",TEXT);
        UIManager.put("ScrollPane.background",BG);
        UIManager.put("ComboBox.background",PANEL2);UIManager.put("ComboBox.foreground",TEXT);
        UIManager.put("CheckBox.background",BG);UIManager.put("CheckBox.foreground",TEXT);
        UIManager.put("Button.background",PANEL2);UIManager.put("Button.foreground",TEXT);
        UIManager.put("control",PANEL2);
    }
    public static JButton button(String label,boolean primary){
        JButton b=new JButton(label);b.setFont(new Font("Dialog",Font.BOLD,12));b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(primary?ACCENT:BORDER),
            new EmptyBorder(10,14,10,14)));b.setBackground(primary?ACCENT:PANEL2);b.setForeground(TEXT);b.setOpaque(true);
        return b;
    }
    public static JLabel label(String text, int size,boolean muted){
        JLabel l=new JLabel(text);l.setFont(new Font("Dialog",Font.PLAIN,size));l.setForeground(muted?MUTED:TEXT);return l;
    }
    public static JTextField field(String text){
        JTextField f=new JTextField(text);f.setFont(new Font("Dialog",Font.PLAIN,13));f.setBackground(PANEL2);
        f.setForeground(TEXT);f.setCaretColor(TEXT);f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),new EmptyBorder(10,10,10,10)));return f;
    }
}
