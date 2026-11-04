package __PACKAGE__;

import __PACKAGE__.telas.TelaPrincipal;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class Main {
    private Main() { }
    public static void main(String[] args) {
        try{UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());}catch(Exception ignored){}
        SwingUtilities.invokeLater(()->new TelaPrincipal().setVisible(true));
    }
}
