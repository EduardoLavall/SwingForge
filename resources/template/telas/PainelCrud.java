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

/** Formulario Swing automaticamente gerado pelo SwingForge, seguindo o exemplo de eventos. */
public class PainelCrud extends JPanel {
    private final CrudDAO dao;
    private final JComponent[] inputs;
    private final JTextField busca=new JTextField(22);
    private final DefaultTableModel modelo;
    private final JTable tabela;
    private List<Registro> registros=new ArrayList<Registro>();
    private Integer editando=null;
    private Object[] valoresOriginais;

    protected PainelCrud(String titulo,CrudDAO dao,String[] rotulos,
            int[] ordem,String[] labels,String[] widgets,String[] options){
        super(new BorderLayout(10,10));this.dao=dao;inputs=new JComponent[dao.campos.length];
        setBorder(BorderFactory.createEmptyBorder(10,14,10,14));
        JPanel top=new JPanel(new BorderLayout(8,8));
        JPanel form=new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder(titulo));
        GridBagConstraints gc=new GridBagConstraints();
        gc.insets=new Insets(5,6,5,6);gc.anchor=GridBagConstraints.WEST;
        for(int row=0;row<ordem.length;row++){
            int i=ordem[row];gc.gridx=0;gc.gridy=row;gc.weightx=0;gc.fill=GridBagConstraints.NONE;
            form.add(new JLabel(labels[row]+(dao.obrigatorios[i]?" *":"")+":"),gc);
            gc.gridx=1;gc.weightx=1;gc.fill=GridBagConstraints.HORIZONTAL;
            JComponent input;
            if("Relacao".equals(dao.tipos[i]))input=new JComboBox<Opcao>();
            else if("Check".equals(widgets[row]))input=new JCheckBox("Sim");
            else if("Radio".equals(widgets[row]))input=new RadioOptions(
                "Booleano".equals(dao.tipos[i]) && options[row].trim().isEmpty()?"true|false":options[row]);
            else if("Combo".equals(widgets[row])){
                JComboBox<String> combo=new JComboBox<String>();
                if(!dao.obrigatorios[i])combo.addItem("");
                for(String text:("Booleano".equals(dao.tipos[i]) && options[row].trim().isEmpty()?"true|false":options[row]).split("\\|"))
                    if(!text.trim().isEmpty())combo.addItem(text.trim());
                input=combo;
            }else input=new JTextField(20);
            inputs[i]=input;form.add(input,gc);
        }
        top.add(form,BorderLayout.CENTER);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0));
        JButton clear=new JButton("Novo / Limpar"),edit=new JButton("Editar selecionado");
        JButton remove=new JButton("Excluir selecionado"),save=new JButton("Salvar");
        actions.add(clear);actions.add(edit);actions.add(remove);actions.add(save);
        top.add(actions,BorderLayout.SOUTH);add(top,BorderLayout.NORTH);
        String[] headers=new String[rotulos.length+1];headers[0]="ID";
        System.arraycopy(rotulos,0,headers,1,rotulos.length);
        modelo=new DefaultTableModel(headers,0){@Override public boolean isCellEditable(int row,int column){return false;}};
        tabela=new JTable(modelo);tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(tabela),BorderLayout.CENTER);
        JPanel searchbar=new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchbar.add(new JLabel("Buscar:"));searchbar.add(busca);
        JButton buscar=new JButton("Buscar"),refresh=new JButton("Recarregar");
        searchbar.add(buscar);searchbar.add(refresh);add(searchbar,BorderLayout.SOUTH);
        Tema.aplicar(this);
        clear.addActionListener(e->limpar());edit.addActionListener(e->selecionar());
        save.addActionListener(e->salvar());remove.addActionListener(e->excluir());
        buscar.addActionListener(e->carregar());refresh.addActionListener(e->{busca.setText("");carregar();});
        busca.addActionListener(e->carregar());
        tabela.getSelectionModel().addListSelectionListener(e->{if(!e.getValueIsAdjusting())selecionar();});
    }
    public void carregar(){
        try{
            List<Registro> atualizados=dao.listar(busca.getText());
            limpar();carregarOpcoes();registros=atualizados;modelo.setRowCount(0);
            for(Registro r:registros){Object[] row=new Object[r.exibicao.length+1];row[0]=r.id;
                System.arraycopy(r.exibicao,0,row,1,r.exibicao.length);modelo.addRow(row);}
        }catch(Exception ex){mostrarErro("Nao foi possivel consultar o banco",ex);}
    }
    private void carregarOpcoes()throws SQLException{
        for(int i=0;i<inputs.length;i++)if(inputs[i] instanceof JComboBox && "Relacao".equals(dao.tipos[i])){
            @SuppressWarnings("unchecked") JComboBox<Opcao> cb=(JComboBox<Opcao>)inputs[i];
            Integer previous=cb.getSelectedItem() instanceof Opcao?((Opcao)cb.getSelectedItem()).id:null;
            cb.removeAllItems();cb.addItem(new Opcao(0,dao.obrigatorios[i]?"Selecione uma opcao":"(Nao selecionado)"));
            for(Opcao op:dao.opcoes(dao.tabelasRelacionadas[i],dao.rotulosRelacionados[i]))cb.addItem(op);
            if(previous!=null)for(int x=0;x<cb.getItemCount();x++)if(cb.getItemAt(x).id==previous){cb.setSelectedIndex(x);break;}
        }
    }
    private Object[] lerCampos(){
        Object[] values=new Object[inputs.length];
        for(int i=0;i<inputs.length;i++){
            JComponent c=inputs[i];
            if(c==null){values[i]=valoresOriginais==null?"":valoresOriginais[i];continue;}
            if(c instanceof RadioOptions)values[i]=((RadioOptions)c).value();
            else if(c instanceof JCheckBox)values[i]=Boolean.toString(((JCheckBox)c).isSelected());
            else if(c instanceof JComboBox){
                Object selected=((JComboBox<?>)c).getSelectedItem();
                if(selected instanceof Opcao){
                    int id=((Opcao)selected).id;values[i]=id==0?"":String.valueOf(id);
                }else values[i]=selected==null?"":String.valueOf(selected);
            }else values[i]=((JTextField)c).getText().trim();
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
        Registro r=registros.get(index);editando=Integer.valueOf(r.id);valoresOriginais=r.valores.clone();
        for(int i=0;i<inputs.length;i++){
            JComponent c=inputs[i];if(c==null)continue;
            Object value=r.valores[i];String str=value==null?"":String.valueOf(value);
            if("Booleano".equals(dao.tipos[i]) && value!=null){
                if("1".equals(str))str="true";
                else if("0".equals(str))str="false";
            }
            if(c instanceof RadioOptions)((RadioOptions)c).select(str,dao.tipos[i]);
            else if(c instanceof JCheckBox)((JCheckBox)c).setSelected(Boolean.TRUE.equals(value)||"1".equals(str)||"true".equalsIgnoreCase(str));
            else if(c instanceof JComboBox){
                JComboBox<?> cb=(JComboBox<?>)c;
                if("Relacao".equals(dao.tipos[i])){
                    if(cb.getItemCount()>0)cb.setSelectedIndex(0);
                    for(int j=0;j<cb.getItemCount();j++)
                        if(cb.getItemAt(j) instanceof Opcao &&
                           str.equals(String.valueOf(((Opcao)cb.getItemAt(j)).id))){cb.setSelectedIndex(j);break;}
                }else {
                    cb.setSelectedIndex(-1);
                    for(int j=0;j<cb.getItemCount();j++)if(equivalente(str,String.valueOf(cb.getItemAt(j)),dao.tipos[i])){cb.setSelectedIndex(j);break;}
                }
            }else ((JTextField)c).setText(str);
        }
    }
    private void limpar(){editando=null;valoresOriginais=null;tabela.clearSelection();
        for(JComponent c:inputs){
            if(c instanceof RadioOptions)((RadioOptions)c).clear();
            else if(c instanceof JTextField)((JTextField)c).setText("");
            else if(c instanceof JCheckBox)((JCheckBox)c).setSelected(false);
            else if(c instanceof JComboBox && ((JComboBox<?>)c).getItemCount()>0)
                ((JComboBox<?>)c).setSelectedIndex(0);
        }
    }
    private void mostrarErro(String acao,Exception ex){
        JOptionPane.showMessageDialog(this,acao+":\n"+ex.getMessage(),"Problema",JOptionPane.ERROR_MESSAGE);
    }
    private static boolean equivalente(String stored,String option,String type) {
        if(stored.equals(option))return true;
        try {
            if("Inteiro".equals(type))return Integer.parseInt(stored)==Integer.parseInt(option);
            if("Decimal".equals(type))return new java.math.BigDecimal(stored.replace(',','.')).compareTo(new java.math.BigDecimal(option.replace(',','.')))==0;
            if("Data".equals(type)) {
                java.time.LocalDate date=java.time.LocalDate.parse(stored);
                return date.equals(option.contains("/")?java.time.LocalDate.parse(option,java.time.format.DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(java.time.format.ResolverStyle.STRICT)):java.time.LocalDate.parse(option));
            }
        } catch(RuntimeException ex) { return false; }
        return false;
    }
    private static final class RadioOptions extends JPanel {
        private final ButtonGroup group=new ButtonGroup();
        RadioOptions(String options){
            super(new FlowLayout(FlowLayout.LEFT,7,0));
            for(String raw:options.split("\\|")){
                String value=raw.trim();if(value.isEmpty())continue;
                JRadioButton button=new JRadioButton(value);group.add(button);add(button);
            }
        }
        String value(){
            for(java.util.Enumeration<AbstractButton> b=group.getElements();b.hasMoreElements();){
                AbstractButton button=b.nextElement();if(button.isSelected())return button.getText();
            }return "";
        }
        void select(String value,String type){group.clearSelection();
            for(java.util.Enumeration<AbstractButton> b=group.getElements();b.hasMoreElements();){
                AbstractButton button=b.nextElement();if(equivalente(value,button.getText(),type)){button.setSelected(true);break;}
            }
        }
        void clear(){group.clearSelection();}
    }
}
