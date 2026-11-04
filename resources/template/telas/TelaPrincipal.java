package __PACKAGE__.telas;

import __PACKAGE__.conexao.Conexao;
import javax.swing.*;
import java.awt.*;

public final class TelaPrincipal extends JFrame {
    private final JPanel secoes=new JPanel();
    private final java.util.List<PainelCrud> paineis=new java.util.ArrayList<PainelCrud>();
    private boolean conectado;
    public TelaPrincipal(){
        super("__APP_NAME__ — Java Swing + MySQL");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);setSize(1020,720);setLocationRelativeTo(null);
        JPanel content=new JPanel(new BorderLayout());
        JPanel header=new JPanel(new FlowLayout(FlowLayout.RIGHT,10,8));
        JButton configurar=new JButton("Configurar MySQL"), reload=new JButton("Atualizar listas");
        header.add(configurar);header.add(reload);content.add(header,BorderLayout.NORTH);
        secoes.setLayout(new BoxLayout(secoes,BoxLayout.Y_AXIS));
__TABS__
        JScrollPane rolagem=new JScrollPane(secoes);
        rolagem.getVerticalScrollBar().setUnitIncrement(24);
        content.add(rolagem,BorderLayout.CENTER);setContentPane(content);Tema.aplicar(content);
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
            conectado=true;atualizar();
        }catch(Exception ex){JOptionPane.showMessageDialog(this,"Nao foi possivel conectar: "+ex.getMessage());}
    }
    private void adicionarSecao(String nome,PainelCrud painel){
        JPanel secao=new JPanel(new BorderLayout());
        secao.setBorder(BorderFactory.createTitledBorder(nome));secao.add(painel,BorderLayout.CENTER);
        Dimension preferred=painel.getPreferredSize();
        secao.setPreferredSize(new Dimension(900,Math.max(420,preferred.height+30)));
        secao.setMaximumSize(new Dimension(Integer.MAX_VALUE,secao.getPreferredSize().height));
        secao.setAlignmentX(Component.LEFT_ALIGNMENT);
        paineis.add(painel);secoes.add(secao);
        painel.aoAlterar(()->atualizarOutras(painel));
    }
    private void atualizar(){for(PainelCrud painel:paineis)painel.carregar();}
    private void atualizarOutras(PainelCrud origem){
        if(conectado)for(PainelCrud painel:paineis)if(painel!=origem)painel.atualizarSemLimpar();
    }
}
