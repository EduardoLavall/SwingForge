package swingforge.ui;

import swingforge.core.FormDesign;
import swingforge.core.Spec;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** GUI Editor offline: edita propriedades e mostra a previa dos controles. */
public final class FormEditor extends JDialog {
    private final Spec.Entity entity;
    private final List<Spec.FormField> draft=new ArrayList<Spec.FormField>();
    private final JTextField titulo=new JTextField(30);
    private final JPanel preview=new JPanel(new BorderLayout());
    private final EditorModel model=new EditorModel();
    private final JTable table=new JTable(model);
    private boolean accepted;

    public static boolean edit(Frame owner,Spec.Entity entity){
        FormEditor editor=new FormEditor(owner,entity);
        editor.setVisible(true);return editor.accepted;
    }
    private FormEditor(Frame owner,Spec.Entity entity) {
        super(owner,"GUI Editor — "+entity.name,true);
        this.entity=entity;FormDesign.normalize(entity);
        for(Spec.FormField u:entity.formFields)
            draft.add(new Spec.FormField(u.field,u.label,u.widget,u.options,u.visible));
        titulo.setText(entity.formTitle);
        setSize(1040,680);setMinimumSize(new Dimension(790,520));setLocationRelativeTo(owner);
        JPanel body=new JPanel(new BorderLayout(12,12));body.setBackground(Light.BG);
        body.setBorder(new EmptyBorder(16,16,16,16));setContentPane(body);
        JPanel headline=new JPanel(new GridLayout(2,1));headline.setBackground(Light.BG);
        headline.add(Light.label("GUI Editor — "+entity.name,20,false));
        headline.add(Light.label("Altere rotulos, componentes, opcoes e ordem. Tudo funciona offline.",12,true));
        body.add(headline,BorderLayout.NORTH);

        JPanel editorPanel=new JPanel(new BorderLayout(8,8));editorPanel.setBackground(Light.PANEL);
        editorPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Light.BORDER),new EmptyBorder(12,12,12,12)));
        JPanel titlePanel=new JPanel(new BorderLayout(0,5));titlePanel.setBackground(Light.PANEL);
        titlePanel.add(Light.label("Titulo do formulario",12,true),BorderLayout.NORTH);
        titlePanel.add(titulo,BorderLayout.CENTER);editorPanel.add(titlePanel,BorderLayout.NORTH);
        table.setRowHeight(32);table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(
            new JComboBox<String>(new String[]{"Texto","Combo","Radio","Check"})));
        table.getColumnModel().getColumn(3).setPreferredWidth(180);
        editorPanel.add(new JScrollPane(table),BorderLayout.CENTER);
        JPanel reorder=new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));reorder.setBackground(Light.PANEL);
        JButton up=Light.button("Subir",false),down=Light.button("Descer",false),
                reset=Light.button("Restaurar rotulos",false);
        reorder.add(up);reorder.add(down);reorder.add(reset);
        editorPanel.add(reorder,BorderLayout.SOUTH);
        up.addActionListener(e->move(-1));down.addActionListener(e->move(1));
        reset.addActionListener(e->{for(Spec.FormField f:draft)f.label=FormDesign.label(f.field);
            model.fireTableDataChanged();refresh();});

        JPanel sample=new JPanel(new BorderLayout(6,6));sample.setBackground(Light.PANEL);
        sample.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Light.BORDER),new EmptyBorder(12,12,12,12)));
        sample.add(Light.label("Prévia em tema claro",16,false),BorderLayout.NORTH);
        sample.add(new JScrollPane(preview),BorderLayout.CENTER);
        JSplitPane split=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,editorPanel,sample);
        split.setBorder(null);split.setResizeWeight(.58);body.add(split,BorderLayout.CENTER);

        JPanel bottom=new JPanel(new FlowLayout(FlowLayout.RIGHT));bottom.setBackground(Light.BG);
        JButton cancel=Light.button("Cancelar",false),apply=Light.button("Aplicar ao projeto",true);
        bottom.add(cancel);bottom.add(apply);body.add(bottom,BorderLayout.SOUTH);
        cancel.addActionListener(e->dispose());apply.addActionListener(e->apply());
        titulo.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){
            public void insertUpdate(javax.swing.event.DocumentEvent e){refresh();}
            public void removeUpdate(javax.swing.event.DocumentEvent e){refresh();}
            public void changedUpdate(javax.swing.event.DocumentEvent e){refresh();}
        });
        refresh();
    }
    private void move(int delta) {
        if(table.isEditing())table.getCellEditor().stopCellEditing();
        int index=table.getSelectedRow(),other=index+delta;
        if(index<0||other<0||other>=draft.size())return;
        java.util.Collections.swap(draft,index,other);
        model.fireTableDataChanged();table.setRowSelectionInterval(other,other);refresh();
    }
    private void apply() {
        if(table.isEditing())table.getCellEditor().stopCellEditing();
        for(Spec.FormField u:draft) {
            Spec.Field field=FormDesign.find(entity,u.field);
            if("Relacao".equals(field.type))u.widget="Combo";
            else if("Booleano".equals(field.type))u.widget="Check";
            else if(!"Texto".equals(field.type)||"Check".equals(u.widget))u.widget="Texto";
            if(!u.visible&&field.required) {
                JOptionPane.showMessageDialog(this,"Campo obrigatorio deve ficar visivel: "+u.field);return;
            }
            if("Texto".equals(field.type)&&("Combo".equals(u.widget)||"Radio".equals(u.widget))
                    &&u.options.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this,"Informe as opcoes separadas por | para "+u.field);return;
            }
        }
        entity.formTitle=titulo.getText().trim().isEmpty()?"Dados de "+entity.name:titulo.getText().trim();
        entity.formFields.clear();entity.formFields.addAll(draft);
        accepted=true;dispose();
    }
    private void refresh() {
        preview.removeAll();preview.setBackground(Color.WHITE);
        JPanel form=new JPanel(new GridBagLayout());form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createTitledBorder(titulo.getText().isEmpty()?"Dados":titulo.getText()));
        GridBagConstraints gc=new GridBagConstraints();gc.insets=new Insets(5,5,5,5);
        int row=0;for(Spec.FormField u:draft) {
            if(!u.visible)continue;
            gc.gridx=0;gc.gridy=row;gc.weightx=0;gc.fill=GridBagConstraints.NONE;gc.anchor=GridBagConstraints.WEST;
            form.add(new JLabel(u.label+":"),gc);
            gc.gridx=1;gc.weightx=1;gc.fill=GridBagConstraints.HORIZONTAL;
            JComponent c;
            if("Radio".equals(u.widget)) {
                JPanel group=new JPanel(new FlowLayout(FlowLayout.LEFT,4,0));group.setBackground(Color.WHITE);
                ButtonGroup buttons=new ButtonGroup();
                for(String part:u.options.split("\\|"))if(!part.trim().isEmpty()){
                    JRadioButton b=new JRadioButton(part.trim());buttons.add(b);group.add(b);
                }
                c=group;
            }else if("Combo".equals(u.widget))c=new JComboBox<String>(
                u.options.isEmpty()?new String[]{"Selecione","Exemplo"}:u.options.split("\\|"));
            else if("Check".equals(u.widget))c=new JCheckBox("Sim");
            else c=new JTextField(15);
            form.add(c,gc);row++;
        }
        JPanel buttons=new JPanel(new FlowLayout(FlowLayout.RIGHT));buttons.setBackground(Color.WHITE);
        for(String name:new String[]{"Novo / Limpar","Editar selecionado","Excluir selecionado","Salvar"})
            buttons.add(new JButton(name));
        JPanel top=new JPanel(new BorderLayout());top.setBackground(Color.WHITE);
        top.add(form,BorderLayout.CENTER);top.add(buttons,BorderLayout.SOUTH);
        preview.add(top,BorderLayout.NORTH);
        JTable fake=new JTable(new Object[][]{{1,"Exemplo"}},new String[]{"ID","Nome"});
        fake.setPreferredScrollableViewportSize(new Dimension(350,180));
        preview.add(new JScrollPane(fake),BorderLayout.CENTER);
        preview.revalidate();preview.repaint();
    }
    private final class EditorModel extends AbstractTableModel {
        private final String[] columns={"Campo Java","Rotulo","Componente","Opcoes (separar por |)","Visivel"};
        public int getRowCount(){return draft.size();}
        public int getColumnCount(){return columns.length;}
        public String getColumnName(int c){return columns[c];}
        public Class<?> getColumnClass(int c){return c==4?Boolean.class:String.class;}
        public boolean isCellEditable(int row,int col){return col!=0;}
        public Object getValueAt(int row,int col){Spec.FormField u=draft.get(row);
            return col==0?u.field:col==1?u.label:col==2?u.widget:col==3?u.options:u.visible;}
        public void setValueAt(Object value,int row,int col){Spec.FormField u=draft.get(row);
            if(col==1)u.label=String.valueOf(value).trim();
            if(col==2)u.widget=String.valueOf(value);
            if(col==3)u.options=String.valueOf(value).trim();
            if(col==4)u.visible=Boolean.TRUE.equals(value);
            fireTableRowsUpdated(row,row);SwingUtilities.invokeLater(()->refresh());
        }
    }
}
