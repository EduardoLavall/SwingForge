package swingforge.ui;

import swingforge.core.FormDesign;
import swingforge.core.Spec;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Editor de todas as secoes da tela gerada, com alteracoes aplicadas em conjunto. */
public final class FormEditor extends JDialog {
    private final List<Section> sections=new ArrayList<Section>();
    private final JTabbedPane preview=new JTabbedPane();
    private final CardLayout editorLayout=new CardLayout();
    private final JPanel editors=new JPanel(editorLayout);
    private boolean accepted, rebuildingPreview;

    public static boolean edit(Frame owner,Spec spec){
        FormEditor editor=new FormEditor(owner,spec);
        editor.setVisible(true);return editor.accepted;
    }
    private FormEditor(Frame owner,Spec spec){
        super(owner,"Personalizar tela completa",true);
        setSize(1200,760);setMinimumSize(new Dimension(850,600));setLocationRelativeTo(owner);
        JPanel body=new JPanel(new BorderLayout(12,12));body.setBackground(Light.BG);
        body.setBorder(new EmptyBorder(16,16,16,16));setContentPane(body);
        JPanel headline=new JPanel(new GridLayout(2,1));headline.setBackground(Light.BG);
        headline.add(Light.label("Personalizar tela completa",20,false));
        headline.add(Light.label("Selecione uma aba na previa para editar os campos da entidade.",12,true));
        body.add(headline,BorderLayout.NORTH);
        editors.setBackground(Light.BG);
        for(Spec.Entity entity:spec.entities){
            Section section=new Section(entity);sections.add(section);
            editors.add(section.panel,String.valueOf(sections.size()-1));
        }
        preview.addChangeListener(e->{
            if(rebuildingPreview || preview.getSelectedIndex()<0)return;
            for(Section section:sections)section.stopEditing();
            editorLayout.show(editors,String.valueOf(preview.getSelectedIndex()));
        });
        JScrollPane editScroll=new JScrollPane(editors);editScroll.getVerticalScrollBar().setUnitIncrement(24);
        JPanel sample=new JPanel(new BorderLayout(6,6));sample.setBackground(Light.PANEL);
        sample.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Light.BORDER),new EmptyBorder(10,10,10,10)));
        sample.add(Light.label("Previa das abas",16,false),BorderLayout.NORTH);
        preview.setBackground(Color.WHITE);sample.add(preview,BorderLayout.CENTER);
        JSplitPane split=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,editScroll,sample);
        split.setBorder(null);split.setResizeWeight(.60);body.add(split,BorderLayout.CENTER);
        JPanel bottom=new JPanel(new FlowLayout(FlowLayout.RIGHT));bottom.setBackground(Light.BG);
        JButton cancel=Light.button("Cancelar",false),apply=Light.button("Aplicar ao projeto",true);
        bottom.add(cancel);bottom.add(apply);body.add(bottom,BorderLayout.SOUTH);
        cancel.addActionListener(e->dispose());apply.addActionListener(e->apply());
        refresh();
    }
    private final class Section {
        final Spec.Entity original, draft;
        final JTextField title=new JTextField(25);
        final JPanel panel=new JPanel(new BorderLayout(6,6));
        final EditorModel model;
        final JTable table;
        Section(Spec.Entity entity){
            original=entity;draft=new Spec.Entity(entity.name);draft.formTitle=entity.formTitle;
            draft.fields.addAll(entity.fields);
            for(Spec.FormField f:entity.formFields)draft.formFields.add(new Spec.FormField(f.field,f.label,f.widget,f.options,f.visible));
            FormDesign.normalize(draft);title.setText(draft.formTitle);
            model=new EditorModel(this);table=new JTable(model);
            panel.setBackground(Light.PANEL);panel.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(entity.name),new EmptyBorder(6,8,8,8)));
            JPanel head=new JPanel(new BorderLayout(8,0));head.setBackground(Light.PANEL);
            head.add(Light.label("Titulo da secao",12,true),BorderLayout.WEST);head.add(title,BorderLayout.CENTER);
            panel.add(head,BorderLayout.NORTH);
            table.setRowHeight(30);table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            JComboBox<String> choices=new JComboBox<String>(new String[]{"Texto","Combo","Radio","Check"});
            DefaultCellEditor editor=new DefaultCellEditor(choices);editor.setClickCountToStart(1);
            table.getColumnModel().getColumn(2).setCellEditor(editor);
            table.getColumnModel().getColumn(2).setCellRenderer((tbl,value,selected,focus,row,col)->{
                JComboBox<String> display=new JComboBox<String>(new String[]{String.valueOf(value)});
                display.setBackground(selected?tbl.getSelectionBackground():Color.WHITE);
                display.setForeground(selected?tbl.getSelectionForeground():tbl.getForeground());return display;
            });
            table.putClientProperty("terminateEditOnFocusLost",Boolean.TRUE);
            table.getColumnModel().getColumn(3).setPreferredWidth(150);
            panel.add(new JScrollPane(table),BorderLayout.CENTER);
            JPanel actions=new JPanel(new FlowLayout(FlowLayout.LEFT,6,0));actions.setBackground(Light.PANEL);
            JButton up=Light.button("Subir",false),down=Light.button("Descer",false),reset=Light.button("Restaurar rotulos",false);
            actions.add(up);actions.add(down);actions.add(reset);panel.add(actions,BorderLayout.SOUTH);
            up.addActionListener(e->move(-1));down.addActionListener(e->move(1));
            reset.addActionListener(e->{for(Spec.FormField f:draft.formFields)f.label=FormDesign.label(f.field);model.fireTableDataChanged();refresh();});
            title.getDocument().addDocumentListener(new DocumentListener(){
                public void insertUpdate(DocumentEvent e){refresh();}public void removeUpdate(DocumentEvent e){refresh();}
                public void changedUpdate(DocumentEvent e){refresh();}
            });
            int height=Math.max(210,Math.min(400,155+draft.formFields.size()*30));
            panel.setPreferredSize(new Dimension(560,height));panel.setMaximumSize(new Dimension(Integer.MAX_VALUE,height));
        }
        void stopEditing(){if(table.isEditing())table.getCellEditor().stopCellEditing();}
        void move(int delta){
            stopEditing();int row=table.getSelectedRow(),other=row+delta;
            if(row<0||other<0||other>=draft.formFields.size())return;
            java.util.Collections.swap(draft.formFields,row,other);model.fireTableDataChanged();
            table.setRowSelectionInterval(other,other);refresh();
        }
    }
    private void apply(){
        for(Section s:sections)s.stopEditing();
        try{
            for(Section s:sections){
                s.draft.formTitle=s.title.getText().trim().isEmpty()?"Dados de "+s.draft.name:s.title.getText().trim();
                FormDesign.validate(s.draft);
            }
        }catch(IllegalArgumentException ex){JOptionPane.showMessageDialog(this,ex.getMessage(),"Confira os campos",JOptionPane.WARNING_MESSAGE);return;}
        for(Section s:sections){s.original.formTitle=s.draft.formTitle;s.original.formFields.clear();s.original.formFields.addAll(s.draft.formFields);}
        accepted=true;dispose();
    }
    private void refresh(){
        int selected=preview.getSelectedIndex();
        rebuildingPreview=true;
        preview.removeAll();
        for(Section s:sections){
            JPanel section=new JPanel(new BorderLayout(4,4));section.setBackground(Color.WHITE);
            JPanel form=new JPanel(new GridBagLayout());form.setBackground(Color.WHITE);
            form.setBorder(BorderFactory.createTitledBorder(s.title.getText().trim().isEmpty()?"Dados":s.title.getText()));
            GridBagConstraints gc=new GridBagConstraints();gc.insets=new Insets(4,4,4,4);
            int row=0;for(Spec.FormField u:s.draft.formFields){
                if(!u.visible)continue;
                gc.gridx=0;gc.gridy=row;gc.weightx=0;gc.fill=GridBagConstraints.NONE;gc.anchor=GridBagConstraints.WEST;
                form.add(new JLabel(u.label+":"),gc);gc.gridx=1;gc.weightx=1;gc.fill=GridBagConstraints.HORIZONTAL;
                String options=u.options;Spec.Field f=FormDesign.find(s.draft,u.field);
                if(options.isEmpty() && "Booleano".equals(f.type))options="true|false";
                JComponent c;
                if("Radio".equals(u.widget)){
                    JPanel group=new JPanel(new FlowLayout(FlowLayout.LEFT,4,0));group.setBackground(Color.WHITE);
                    ButtonGroup buttons=new ButtonGroup();
                    for(String part:options.split("\\|"))if(!part.trim().isEmpty()){
                        JRadioButton b=new JRadioButton(part.trim());buttons.add(b);group.add(b);
                    }c=group;
                }else if("Combo".equals(u.widget))c=new JComboBox<String>(options.isEmpty()?new String[]{"Selecione"}:options.split("\\|"));
                else if("Check".equals(u.widget))c=new JCheckBox("Sim");else c=new JTextField(15);
                form.add(c,gc);row++;
            }
            JPanel buttons=new JPanel(new FlowLayout(FlowLayout.RIGHT,4,0));buttons.setBackground(Color.WHITE);
            for(String name:new String[]{"Novo / Limpar","Editar","Excluir","Salvar"})buttons.add(new JButton(name));
            JPanel top=new JPanel(new BorderLayout());top.add(form,BorderLayout.CENTER);top.add(buttons,BorderLayout.SOUTH);
            section.add(top,BorderLayout.NORTH);
            String[] headers=new String[s.draft.fields.size()+1];headers[0]="ID";
            for(int i=0;i<s.draft.fields.size();i++){
                String field=s.draft.fields.get(i).name;headers[i+1]=FormDesign.label(field);
                for(Spec.FormField u:s.draft.formFields)if(u.field.equals(field)){headers[i+1]=u.label;break;}
            }
            JTable fake=new JTable(new Object[0][headers.length],headers);fake.setPreferredScrollableViewportSize(new Dimension(350,90));
            section.add(new JScrollPane(fake),BorderLayout.CENTER);
            JScrollPane scroll=new JScrollPane(section);scroll.getVerticalScrollBar().setUnitIncrement(24);
            preview.addTab(s.draft.name,scroll);
        }
        if(selected>=0 && selected<preview.getTabCount())preview.setSelectedIndex(selected);
        rebuildingPreview=false;
        if(preview.getSelectedIndex()>=0)editorLayout.show(editors,String.valueOf(preview.getSelectedIndex()));
        preview.revalidate();preview.repaint();
    }
    private final class EditorModel extends AbstractTableModel {
        private final Section section;
        private final String[] columns={"Campo Java","Rotulo","Componente","Opcoes (separar por |)","Visivel"};
        EditorModel(Section section){this.section=section;}
        public int getRowCount(){return section.draft.formFields.size();}
        public int getColumnCount(){return columns.length;}
        public String getColumnName(int c){return columns[c];}
        public Class<?> getColumnClass(int c){return c==4?Boolean.class:String.class;}
        public boolean isCellEditable(int row,int col){return col!=0;}
        public Object getValueAt(int row,int col){Spec.FormField u=section.draft.formFields.get(row);
            return col==0?u.field:col==1?u.label:col==2?u.widget:col==3?u.options:u.visible;}
        public void setValueAt(Object value,int row,int col){Spec.FormField u=section.draft.formFields.get(row);
            if(col==1)u.label=String.valueOf(value).trim();
            if(col==2){String choice=String.valueOf(value);
                if(!FormDesign.compatible(FormDesign.find(section.draft,u.field),choice))
                    JOptionPane.showMessageDialog(FormEditor.this,"Componente incompativel com o tipo de "+u.field);
                else u.widget=choice;
            }
            if(col==3)u.options=String.valueOf(value).trim();
            if(col==4)u.visible=Boolean.TRUE.equals(value);
            fireTableRowsUpdated(row,row);SwingUtilities.invokeLater(()->refresh());
        }
    }
}
