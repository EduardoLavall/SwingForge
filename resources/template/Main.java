package __PACKAGE__;

import __PACKAGE__.conexao.PrepararBanco;
import __PACKAGE__.telas.PainelCrud;
__IMPORTS__
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.OceanTheme;

/** Janela do sistema, com uma aba para cada cadastro. */
public final class Main extends JFrame {
    private final JTabbedPane secoes = new JTabbedPane();
    private final List<PainelCrud> paineis = new ArrayList<PainelCrud>();

    public Main() {
        super("__APP_NAME__");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1020, 720);
        setLocationRelativeTo(null);
__TABS__
        add(secoes, BorderLayout.CENTER);
        for (PainelCrud painel : paineis) painel.atualizarSemLimpar();
    }

    private void adicionarSecao(String nome, PainelCrud painel) {
        paineis.add(painel);
        JScrollPane scroll = new JScrollPane(painel);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        secoes.addTab(nome, scroll);
        painel.aoAlterar(() -> {
            for (PainelCrud outro : paineis) if (outro != painel) outro.atualizarSemLimpar();
        });
    }

    public static void main(String[] args) {
        try {
            MetalLookAndFeel.setCurrentTheme(new OceanTheme());
            UIManager.setLookAndFeel(new MetalLookAndFeel());
            PrepararBanco.executar();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null,
                    "Nao foi possivel preparar o banco. Confira banco.properties, o MySQL e suas permissoes.\n" + ex.getMessage(),
                    "Erro de conexao", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
            return;
        }
        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }
}
