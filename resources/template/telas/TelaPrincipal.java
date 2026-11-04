package __PACKAGE__.telas;

import __PACKAGE__.conexao.Conexao;
import javax.swing.*;
import java.awt.*;

public final class TelaPrincipal extends JFrame {
    private final JTabbedPane secoes=new JTabbedPane();
    private final java.util.List<PainelCrud> paineis=new java.util.ArrayList<PainelCrud>();
    private boolean conectado;
    public TelaPrincipal(){
        super("__APP_NAME__ — Java Swing + MySQL");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);setSize(1020,720);setLocationRelativeTo(null);
        JPanel content=new JPanel(new BorderLayout());
__TABS__
        content.add(secoes,BorderLayout.CENTER);setContentPane(content);Tema.aplicar(content);
        SwingUtilities.invokeLater(()->configurar());
    }
    private void configurar(){
        JTextField host=new JTextField("localhost"),porta=new JTextField("3306"),
           banco=new JTextField(Conexao.bancoPadrao()),usuario=new JTextField("root");
        JPasswordField senha=new JPasswordField();
        Object[] fields={"Host",host,"Porta",porta,"Banco",banco,"Usuario",usuario,"Senha",senha};
        if(JOptionPane.showConfirmDialog(this,fields,"Conexao MySQL",JOptionPane.OK_CANCEL_OPTION)
                !=JOptionPane.OK_OPTION)return;
        try{Conexao.configurar(host.getText(),porta.getText(),banco.getText(),usuario.getText(),new String(senha.getPassword()));
            try(java.sql.Connection cn=Conexao.abrir()){if(!cn.isValid(4))throw new java.sql.SQLException("Conexao invalida.");}
            conectado=true;atualizar();
        }catch(Exception ex){JOptionPane.showMessageDialog(this,"Nao foi possivel conectar: "+ex.getMessage());}
    }
    private void adicionarSecao(String nome,PainelCrud painel){
        paineis.add(painel);
        JScrollPane scroll=new JScrollPane(painel);scroll.getVerticalScrollBar().setUnitIncrement(24);
        secoes.addTab(nome,scroll);
        painel.aoAlterar(()->atualizarOutras(painel));
    }
    private void atualizar(){for(PainelCrud painel:paineis)painel.atualizarSemLimpar();}
    private void atualizarOutras(PainelCrud origem){
        if(conectado)for(PainelCrud painel:paineis)if(painel!=origem)painel.atualizarSemLimpar();
    }
}
