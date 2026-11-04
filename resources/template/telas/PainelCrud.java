package __PACKAGE__.telas;

import __PACKAGE__.dao.CrudDAO;
import __PACKAGE__.dao.Opcao;
import __PACKAGE__.dao.Registro;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Formulario Swing gerado em Java puro. GUI Builder (.form) previsto para a etapa 2. */
public class PainelCrud extends JPanel {
    private final CrudDAO dao;
    private final JComponent[] inputs;
    private final JTextField busca=new JTextField(22);
    private final DefaultTableModel modelo;
    private final JTable tabela;
    private List<Registro> registros=new ArrayList<Registro>();
    private Integer editando=null;

    protected PainelCrud(String titulo,CrudDAO dao,String[] rotulos){
        super(new BorderLayout(12,12));this.dao=dao;inputs=new JComponent[dao.campos.length];
        setBorder(BorderFactory.createEmptyBorder(18,20,18,20));
        JPanel top=new JPanel(new BorderLayout(0,12));
        JLabel heading=new JLabel(titulo);heading.setFont(new Font("Dialog",Font.BOLD,22));top.add(heading,BorderLayout.NORTH);
        JPanel form=new JPanel(new GridBagLayout());GridBagConstraints gc=new GridBagConstraints();
        gc.insets=new Insets(5,5,5,10);gc.anchor=GridBagConstraints.WEST;gc.fill=GridBagConstraints.HORIZONTAL;
        for(int i=0;i<dao.campos.length;i++){
            gc.gridx=(i%2)*2;gc.gridy=i/2;gc.weightx=0;
            String caption=rotulos[i]+(dao.obrigatorios[i]?" *":"");form.add(new JLabel(caption),gc);
            gc.gridx++;gc.weightx=1;
            JComponent input;
            if("Relacao".equals(dao.tipos[i]))input=new JComboBox<Opcao>();
            else if("Booleano".equals(dao.tipos[i]))input=new JCheckBox("Ativo");
            else input=new JTextField(18);
            inputs[i]=input;form.add(input,gc);
        }
        top.add(form,BorderLayout.CENTER);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));
        JButton save=new JButton("Salvar"), clear=new JButton("Novo / Limpar"), remove=new JButton("Excluir selecionado");
        actions.add(save);actions.add(clear);actions.add(remove);top.add(actions,BorderLayout.SOUTH);
        add(top,BorderLayout.NORTH);
        String[] headers=new String[rotulos.length+1];headers[0]="ID";System.arraycopy(rotulos,0,headers,1,rotulos.length);
        modelo=new DefaultTableModel(headers,0){@Override public boolean isCellEditable(int row,int column){return false;}};
        tabela=new JTable(modelo);add(new JScrollPane(tabela),BorderLayout.CENTER);
        JPanel searchbar=new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchbar.add(new JLabel("Buscar por nome / categoria:"));searchbar.add(busca);
        JButton buscar=new JButton("Buscar"), refresh=new JButton("Recarregar");
        searchbar.add(buscar);searchbar.add(refresh);add(searchbar,BorderLayout.SOUTH);
        Tema.aplicar(this);heading.setForeground(Tema.TEXTO);heading.setFont(new Font("Dialog",Font.BOLD,22));
        save.addActionListener(e->salvar());clear.addActionListener(e->limpar());remove.addActionListener(e->excluir());
        buscar.addActionListener(e->carregar());refresh.addActionListener(e->{busca.setText("");carregar();});
        busca.addActionListener(e->carregar());
        tabela.getSelectionModel().addListSelectionListener(e->{if(!e.getValueIsAdjusting())selecionar();});
    }
    public void carregar(){
        try{
            carregarOpcoes();registros=dao.listar(busca.getText());modelo.setRowCount(0);
            for(Registro r:registros){Object[] row=new Object[r.exibicao.length+1];row[0]=r.id;
                System.arraycopy(r.exibicao,0,row,1,r.exibicao.length);modelo.addRow(row);}
        }catch(Exception ex){mostrarErro("Nao foi possivel consultar o banco",ex);}
    }
    private void carregarOpcoes()throws SQLException{
        for(int i=0;i<inputs.length;i++)if(inputs[i] instanceof JComboBox){
            @SuppressWarnings("unchecked") JComboBox<Opcao> cb=(JComboBox<Opcao>)inputs[i];
            Integer previous=cb.getSelectedItem() instanceof Opcao?((Opcao)cb.getSelectedItem()).id:null;
            cb.removeAllItems();if(!dao.obrigatorios[i])cb.addItem(new Opcao(0,"(Nao selecionado)"));
            for(Opcao op:dao.opcoes(dao.tabelasRelacionadas[i],dao.rotulosRelacionados[i]))cb.addItem(op);
            if(previous!=null)for(int x=0;x<cb.getItemCount();x++)if(cb.getItemAt(x).id==previous){cb.setSelectedIndex(x);break;}
        }
    }
    private Object[] lerCampos(){
        Object[] values=new Object[inputs.length];
        for(int i=0;i<inputs.length;i++){
            if(inputs[i] instanceof JCheckBox)values[i]=Boolean.toString(((JCheckBox)inputs[i]).isSelected());
            else if(inputs[i] instanceof JComboBox){Opcao op=(Opcao)((JComboBox<?>)inputs[i]).getSelectedItem();
                values[i]=op==null||op.id==0?"":Integer.toString(op.id);}
            else values[i]=((JTextField)inputs[i]).getText();
        }
        return values;
    }
    private void salvar(){
        try{dao.salvar(editando,lerCampos());limpar();carregar();
            JOptionPane.showMessageDialog(this,"Registro salvo com sucesso.");
        }catch(Exception ex){mostrarErro("Nao foi possivel salvar",ex);}
    }
    private void excluir(){
        if(editando==null){JOptionPane.showMessageDialog(this,"Selecione um registro na tabela.");return;}
        if(JOptionPane.showConfirmDialog(this,"Excluir registro #"+editando+"?","Confirmar",
                JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        try{dao.excluir(editando.intValue());limpar();carregar();}
        catch(Exception ex){mostrarErro("Nao foi possivel excluir (verifique relacionamentos)",ex);}
    }
    private void selecionar(){
        int view=tabela.getSelectedRow();if(view<0)return;
        int index=tabela.convertRowIndexToModel(view);if(index<0||index>=registros.size())return;
        Registro r=registros.get(index);editando=Integer.valueOf(r.id);
        for(int i=0;i<inputs.length;i++){
            Object v=r.valores[i];
            if(inputs[i] instanceof JCheckBox)((JCheckBox)inputs[i]).setSelected(Boolean.TRUE.equals(v)||"1".equals(String.valueOf(v)));
            else if(inputs[i] instanceof JComboBox){@SuppressWarnings("unchecked") JComboBox<Opcao> cb=(JComboBox<Opcao>)inputs[i];
                for(int k=0;k<cb.getItemCount();k++)if(v!=null&&String.valueOf(v).equals(String.valueOf(cb.getItemAt(k).id))){cb.setSelectedIndex(k);break;}}
            else ((JTextField)inputs[i]).setText(v==null?"":String.valueOf(v));
        }
    }
    private void limpar(){editando=null;tabela.clearSelection();
        for(JComponent component:inputs){
            if(component instanceof JTextField)((JTextField)component).setText("");
            else if(component instanceof JCheckBox)((JCheckBox)component).setSelected(false);
            else if(component instanceof JComboBox && ((JComboBox<?>)component).getItemCount()>0)
                ((JComboBox<?>)component).setSelectedIndex(0);
        }
    }
    private void mostrarErro(String acao,Exception ex){
        JOptionPane.showMessageDialog(this,acao+":\n"+ex.getMessage(),"Problema",JOptionPane.ERROR_MESSAGE);
    }
}
