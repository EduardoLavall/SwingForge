package __PACKAGE__.telas;

import __PACKAGE__.conexao.Conexao;
import javax.swing.*;
import java.awt.*;

public final class TelaPrincipal extends JFrame {
    private final JTabbedPane tabs=new JTabbedPane();
    public TelaPrincipal(){
        super("__APP_NAME__ — Java Swing + MySQL");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);setSize(1020,720);setLocationRelativeTo(null);
        JPanel content=new JPanel(new BorderLayout());
        JPanel header=new JPanel(new FlowLayout(FlowLayout.RIGHT,10,8));
        JButton configurar=new JButton("Configurar MySQL"), reload=new JButton("Atualizar listas");
        header.add(configurar);header.add(reload);content.add(header,BorderLayout.NORTH);
__TABS__
        content.add(tabs,BorderLayout.CENTER);setContentPane(content);Tema.aplicar(content);
        configurar.addActionListener(e->configurar());
        reload.addActionListener(e->atualizar());
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
            atualizar();
        }catch(Exception ex){JOptionPane.showMessageDialog(this,"Nao foi possivel conectar: "+ex.getMessage());}
    }
    private void atualizar(){for(int i=0;i<tabs.getTabCount();i++)((PainelCrud)tabs.getComponentAt(i)).carregar();}
}
