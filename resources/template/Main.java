package __PACKAGE__;


import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.OceanTheme;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        try {
            MetalLookAndFeel.setCurrentTheme(new OceanTheme());
            UIManager.setLookAndFeel(new MetalLookAndFeel());
        } catch (Exception ignored) {
        }
        SwingUtilities.invokeLater(() -> new TelaPrincipal().setVisible(true));
    }
}
