package swingforge.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Paleta unica clara, sem tema escuro nem dependencias externas. */
public final class Light {
    public static final Color BG=new Color(246,248,251), PANEL=Color.WHITE, PANEL2=new Color(241,244,248),
        TEXT=new Color(37,45,57), MUTED=new Color(94,107,121), BORDER=new Color(216,222,230),
        ACCENT=new Color(43,102,190), SUCCESS=new Color(36,131,81);
    private Light() { }
    public static void defaults(){
        try{UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());}catch(Exception ignored){}
        UIManager.put("Panel.background",BG);UIManager.put("Label.foreground",TEXT);
        UIManager.put("OptionPane.background",PANEL);UIManager.put("OptionPane.messageForeground",TEXT);
        UIManager.put("TextField.background",PANEL2);UIManager.put("TextField.foreground",TEXT);
        UIManager.put("TextField.caretForeground",TEXT);UIManager.put("TextArea.background",PANEL);
        UIManager.put("TextArea.foreground",TEXT);UIManager.put("Table.background",PANEL);
        UIManager.put("Table.foreground",TEXT);UIManager.put("Table.selectionBackground",new Color(213,228,248));
        UIManager.put("TableHeader.background",PANEL2);UIManager.put("TableHeader.foreground",TEXT);
        UIManager.put("List.background",PANEL);UIManager.put("List.foreground",TEXT);
        UIManager.put("List.selectionBackground",new Color(213,228,248));
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
            new EmptyBorder(10,14,10,14)));b.setBackground(primary?ACCENT:PANEL2);b.setForeground(primary?Color.WHITE:TEXT);b.setOpaque(true);
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
