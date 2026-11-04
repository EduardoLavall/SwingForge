package swingforge.ui;

import swingforge.core.Generator;
import swingforge.core.ProjectStore;
import swingforge.core.Spec;
import swingforge.core.Validator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/** Interface 100% offline. O editor visual de formularios (.form) entra na Etapa 2/3. */
public final class SwingForgeApp extends JFrame {
    private Spec spec=Spec.example();
    private Path root;
    private final JTextField nome=Dark.field(spec.name), pacote=Dark.field(spec.pkg), banco=Dark.field(spec.database);
    private final DefaultListModel<Spec.Entity> entidades=new DefaultListModel<Spec.Entity>();
    private final JList<Spec.Entity> lista=new JList<Spec.Entity>(entidades);
    private final FieldTableModel campos=new FieldTableModel();
    private final JTable tabela=new JTable(campos);
    private final JLabel status=Dark.label("Pronto para criar seu primeiro projeto.",12,true);
    private final JLabel title=Dark.label("Campos",17,false);
    private final JTextArea log=new JTextArea(6,80);
    private final JPanel advancedPanel=new JPanel(new GridLayout(2,2,12,8));
    private boolean advancedShown=false, busy=false;
    private final JButton gerar=Dark.button("Gerar pasta do projeto",true);
    private final JButton compilar=Dark.button("Compilar e testar",false);
    private final JButton executar=Dark.button("Executar sistema",false);

    public SwingForgeApp(){
        super("SwingForge — Offline Java CRUD Studio");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);setSize(1080,780);setMinimumSize(new Dimension(850,650));
        setLocationRelativeTo(null);getContentPane().setBackground(Dark.BG);
        JPanel body=new JPanel(new BorderLayout(0,18));body.setBackground(Dark.BG);
        body.setBorder(new EmptyBorder(20,22,17,22));setContentPane(body);
        body.add(header(),BorderLayout.NORTH);
        JPanel content=new JPanel(new BorderLayout(18,0));content.setBackground(Dark.BG);
        content.add(entityEditor(),BorderLayout.WEST);content.add(mainEditor(),BorderLayout.CENTER);
        body.add(content,BorderLayout.CENTER);body.add(footer(),BorderLayout.SOUTH);
        lista.addListSelectionListener(e->{if(!e.getValueIsAdjusting())selectedChanged();});
        gerar.addActionListener(e->generate());compilar.addActionListener(e->compile());executar.addActionListener(e->run());
        refreshProject();
    }
    private JPanel header(){
        JPanel top=new JPanel(new BorderLayout());top.setBackground(Dark.BG);
        JPanel brand=new JPanel(new GridLayout(2,1,0,4));brand.setBackground(Dark.BG);
        JLabel name=Dark.label("▦  SwingForge",27,false);name.setFont(new Font("Dialog",Font.BOLD,27));brand.add(name);
        brand.add(Dark.label("Projetos Java Swing + MySQL + NetBeans  •  100% offline",12,true));top.add(brand,BorderLayout.WEST);
        JLabel indicator=Dark.label("●  OFFLINE",12,true);indicator.setForeground(Dark.SUCCESS);top.add(indicator,BorderLayout.EAST);
        return top;
    }
    private JPanel entityEditor(){
        JPanel side=new JPanel(new BorderLayout(0,12));side.setBackground(Dark.PANEL);
        side.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Dark.BORDER),new EmptyBorder(15,12,15,12)));
        side.setPreferredSize(new Dimension(248,200));
        side.add(Dark.label("Entidades",17,false),BorderLayout.NORTH);
        lista.setFont(new Font("Dialog",Font.PLAIN,15));lista.setBackground(Dark.PANEL);lista.setForeground(Dark.TEXT);
        lista.setFixedCellHeight(42);lista.setSelectionBackground(new Color(65,59,101));lista.setSelectionForeground(Dark.TEXT);
        JScrollPane scroll=new JScrollPane(lista);scroll.setBorder(null);side.add(scroll,BorderLayout.CENTER);
        JPanel actions=new JPanel(new GridLayout(2,1,0,8));actions.setBackground(Dark.PANEL);
        JButton plus=Dark.button("+  Nova entidade",true),remove=Dark.button("Remover entidade",false);
        actions.add(plus);actions.add(remove);side.add(actions,BorderLayout.SOUTH);
        plus.addActionListener(e->newEntity());remove.addActionListener(e->removeEntity());
        return side;
    }
    private JPanel mainEditor(){
        JPanel panel=new JPanel(new BorderLayout(0,14));panel.setBackground(Dark.BG);
        JPanel config=new JPanel(new BorderLayout(0,12));config.setBackground(Dark.PANEL);
        config.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Dark.BORDER),new EmptyBorder(16,17,16,17)));
        JPanel row=new JPanel(new BorderLayout(0,5));row.setBackground(Dark.PANEL);
        row.add(Dark.label("Nome do projeto",12,true),BorderLayout.NORTH);row.add(nome,BorderLayout.CENTER);
        config.add(row,BorderLayout.NORTH);
        JButton toggle=Dark.button("⚙  Opções avançadas  ▾",false);
        toggle.setHorizontalAlignment(SwingConstants.LEFT);
        config.add(toggle,BorderLayout.CENTER);
        advancedPanel.setBackground(Dark.PANEL);
        advancedPanel.add(Dark.label("Pacote Java",12,true));advancedPanel.add(Dark.label("Banco MySQL",12,true));
        advancedPanel.add(pacote);advancedPanel.add(banco);advancedPanel.setVisible(false);
        config.add(advancedPanel,BorderLayout.SOUTH);
        toggle.addActionListener(e->{advancedShown=!advancedShown;advancedPanel.setVisible(advancedShown);
            toggle.setText(advancedShown?"⚙  Opções avançadas  ▴":"⚙  Opções avançadas  ▾");config.revalidate();});
        panel.add(config,BorderLayout.NORTH);
        JPanel fields=new JPanel(new BorderLayout(0,12));fields.setBackground(Dark.PANEL);
        fields.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Dark.BORDER),new EmptyBorder(16,17,16,17)));
        fields.add(title,BorderLayout.NORTH);
        tabela.setRowHeight(34);tabela.setBackground(Dark.PANEL);tabela.setForeground(Dark.TEXT);
        tabela.setSelectionBackground(new Color(65,59,101));tabela.setGridColor(Dark.BORDER);
        tabela.getTableHeader().setBackground(Dark.PANEL2);tabela.getTableHeader().setForeground(Dark.TEXT);
        tabela.getTableHeader().setPreferredSize(new Dimension(100,34));
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JComboBox<String> typeBox=new JComboBox<String>(new String[]{"Texto","Inteiro","Decimal","Data","Booleano","Relacao"});
        typeBox.setBackground(Dark.PANEL2);typeBox.setForeground(Dark.TEXT);
        TableColumn typeColumn=tabela.getColumnModel().getColumn(1);typeColumn.setCellEditor(new DefaultCellEditor(typeBox));
        JScrollPane pane=new JScrollPane(tabela);pane.getViewport().setBackground(Dark.PANEL);pane.setBorder(BorderFactory.createLineBorder(Dark.BORDER));
        fields.add(pane,BorderLayout.CENTER);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));actions.setBackground(Dark.PANEL);
        JButton add=Dark.button("+  Campo",true),del=Dark.button("Remover campo",false),relation=Dark.button("Editar relação",false);
        actions.add(add);actions.add(del);actions.add(relation);fields.add(actions,BorderLayout.SOUTH);
        add.addActionListener(e->newField());del.addActionListener(e->removeField());relation.addActionListener(e->editRelation());
        panel.add(fields,BorderLayout.CENTER);
        return panel;
    }
    private JPanel footer(){
        JPanel bottom=new JPanel(new BorderLayout(0,9));bottom.setBackground(Dark.BG);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));actions.setBackground(Dark.BG);
        JButton novo=Dark.button("Novo",false),abrir=Dark.button("Abrir projeto",false),salvar=Dark.button("Salvar modelo",false);
        actions.add(novo);actions.add(abrir);actions.add(salvar);actions.add(gerar);actions.add(compilar);actions.add(executar);
        novo.addActionListener(e->newProject());abrir.addActionListener(e->openProject());salvar.addActionListener(e->saveModel());
        bottom.add(actions,BorderLayout.NORTH);
        log.setEditable(false);log.setLineWrap(true);log.setWrapStyleWord(true);log.setForeground(Dark.MUTED);log.setBackground(Dark.PANEL);
        log.setFont(new Font(Font.MONOSPACED,Font.PLAIN,11));log.setBorder(new EmptyBorder(8,12,8,12));
        JScrollPane p=new JScrollPane(log);p.setBorder(BorderFactory.createLineBorder(Dark.BORDER));
        p.setPreferredSize(new Dimension(500,112));bottom.add(p,BorderLayout.CENTER);
        bottom.add(status,BorderLayout.SOUTH);return bottom;
    }
    private void flushEditors(){if(tabela.isEditing())tabela.getCellEditor().stopCellEditing();
        spec.name=nome.getText().trim();spec.pkg=pacote.getText().trim();spec.database=banco.getText().trim();}
    private void refreshProject(){
        nome.setText(spec.name);pacote.setText(spec.pkg);banco.setText(spec.database);
        entidades.clear();for(Spec.Entity e:spec.entities)entidades.addElement(e);
        if(!spec.entities.isEmpty())lista.setSelectedIndex(0);
        campos.fireTableDataChanged();
    }
    private Spec.Entity selected(){return lista.getSelectedValue();}
    private void selectedChanged(){Spec.Entity e=selected();title.setText(e==null?"Campos":"Campos de "+e.name);
        campos.fireTableDataChanged();refreshRelationEditors();}
    private void refreshRelationEditors(){JComboBox<String> choices=new JComboBox<String>();choices.addItem("");
        for(Spec.Entity entity:spec.entities)choices.addItem(entity.name);
        choices.setBackground(Dark.PANEL2);choices.setForeground(Dark.TEXT);
        tabela.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(choices));}
    private void newEntity(){String value=JOptionPane.showInputDialog(this,"Nome da entidade (ex.: Cliente):","Nova entidade",JOptionPane.QUESTION_MESSAGE);
        if(value==null)return;value=value.trim();if(value.isEmpty())return;
        Spec.Entity e=new Spec.Entity(value);e.fields.add(new Spec.Field("nome","Texto","",true));
        spec.entities.add(e);entidades.addElement(e);lista.setSelectedValue(e,true);}
    private void removeEntity(){Spec.Entity e=selected();if(e==null)return;
        if(JOptionPane.showConfirmDialog(this,"Excluir entidade "+e.name+" do modelo?","Confirmar",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        spec.entities.remove(e);entidades.removeElement(e);if(!entidades.isEmpty())lista.setSelectedIndex(0);}
    private void newField(){Spec.Entity e=selected();if(e==null)return;flushEditors();
        e.fields.add(new Spec.Field("novo_campo","Texto","",false));campos.fireTableDataChanged();
        int row=e.fields.size()-1;tabela.setRowSelectionInterval(row,row);tabela.editCellAt(row,0);tabela.requestFocusInWindow();}
    private void removeField(){Spec.Entity e=selected();int row=tabela.getSelectedRow();if(e==null||row<0)return;
        flushEditors();e.fields.remove(row);campos.fireTableDataChanged();}
    private void editRelation(){Spec.Entity e=selected();int row=tabela.getSelectedRow();if(e==null||row<0)return;
        flushEditors();Spec.Field f=e.fields.get(row);f.type="Relacao";String[] options=new String[spec.entities.size()];
        for(int i=0;i<options.length;i++)options[i]=spec.entities.get(i).name;
        Object answer=JOptionPane.showInputDialog(this,"Qual entidade sera referenciada?","Relacionamento",
            JOptionPane.QUESTION_MESSAGE,null,options,f.reference);
        if(answer!=null){f.reference=answer.toString();campos.fireTableDataChanged();}}
    private void newProject(){if(JOptionPane.showConfirmDialog(this,"Criar um novo projeto? Alteracoes nao salvas serao perdidas.",
            "Novo projeto",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        spec=new Spec();spec.entities.add(new Spec.Entity("Item"));spec.entities.get(0).fields.add(new Spec.Field("nome","Texto","",true));root=null;refreshProject();}
    private JFileChooser chooser(){JFileChooser f=new JFileChooser();f.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        f.setDialogTitle("Selecione uma pasta local");return f;}
    private void openProject(){JFileChooser ch=chooser();if(ch.showOpenDialog(this)!=JFileChooser.APPROVE_OPTION)return;
        Path folder=ch.getSelectedFile().toPath();try{Spec other=ProjectStore.read(folder.resolve("swingforge.json"));
            root=folder;spec=other;refreshProject();message("Projeto reaberto: "+folder);
        }catch(Exception ex){error("Nao foi possivel abrir. A v1 edita projetos com swingforge.json. Importador de projetos legados: etapa 2.",ex);}}
    private Path chooseRoot(){if(root!=null)return root;
        JFileChooser ch=chooser();ch.setDialogTitle("Pasta que recebera "+spec.name);
        if(ch.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION)return null;
        return ch.getSelectedFile().toPath().resolve(spec.name);
    }
    private void saveModel(){try{flushEditors();Validator.validate(spec);Path dest=chooseRoot();if(dest==null)return;
        Files.createDirectories(dest);ProjectStore.write(dest.resolve("swingforge.json"),spec);root=dest;
        message("Modelo salvo: "+dest.resolve("swingforge.json"));
    }catch(Exception ex){error("Erro ao salvar",ex);}}
    private boolean generate(){try{flushEditors();Validator.validate(spec);Path dest=chooseRoot();if(dest==null)return false;
        if(root!=null && Files.exists(dest.resolve("src"))){int conf=JOptionPane.showConfirmDialog(this,
            "Regenerar codigo pode sobrescrever mudancas manuais. Backups serao salvos em .swingforge/backups. Continuar?",
            "Confirmar geracao",JOptionPane.YES_NO_OPTION);if(conf!=JOptionPane.YES_OPTION)return false;}
        int count=Generator.generate(spec,dest);root=dest;message("Projeto gerado em "+dest+" ("+count+" arquivos).");return true;
    }catch(Exception ex){error("Erro ao gerar",ex);return false;}}
    private void compile(){
        if(root==null){if(!generate())return;}
        else {
            try {
                flushEditors();Validator.validate(spec);
                Path config=root.resolve("swingforge.json");
                boolean changes=!Files.exists(config)||!ProjectStore.serialize(spec).equals(
                    new String(Files.readAllBytes(config),StandardCharsets.UTF_8));
                if(changes || !Files.exists(root.resolve("build.xml"))) {
                    int confirm=JOptionPane.showConfirmDialog(this,
                        "O projeto possui alteracoes nao geradas. Gerar o codigo antes de compilar?",
                        "Atualizar projeto",JOptionPane.YES_NO_OPTION);
                    if(confirm!=JOptionPane.YES_OPTION)return;
                    if(!generate())return;
                }
            }catch(Exception ex){error("Falha ao validar modelo",ex);return;}
        }
        launchCommand(false);
    }
    private void run(){if(root==null){JOptionPane.showMessageDialog(this,"Gere o projeto primeiro.");return;}launchCommand(true);}
    private void launchCommand(final boolean run){
        if(busy)return;
        final Path folder=root;
        String antPath="ant";
        Path local=folder.resolve(System.getProperty("os.name").toLowerCase().contains("win")?
                "tools/ant/bin/ant.bat":"tools/ant/bin/ant");
        if(Files.isRegularFile(local))antPath=local.toAbsolutePath().toString();
        final List<String> command=new ArrayList<String>();command.add(antPath);
        if(!run){command.add("clean");command.add("jar");}else{command.add("run");}
        busy=true;gerar.setEnabled(false);compilar.setEnabled(false);executar.setEnabled(false);
        message(run?"Executando sistema...":"Compilando projeto...");
        new SwingWorker<Integer,String>(){
            private int execute(List<String> args) throws Exception {
                ProcessBuilder pb=new ProcessBuilder(args);
                pb.directory(folder.toFile());pb.redirectErrorStream(true);
                Process p=pb.start();
                try(BufferedReader reader=new BufferedReader(new InputStreamReader(p.getInputStream(),StandardCharsets.UTF_8))){
                    String line;while((line=reader.readLine())!=null)publish(line);
                }
                return p.waitFor();
            }
            private String jdkTool(String tool){
                String ext=System.getProperty("os.name").toLowerCase().contains("win")?".exe":"";
                Path home=Paths.get(System.getProperty("java.home"));
                Path candidate=home.resolve("bin").resolve(tool+ext);
                if(Files.isRegularFile(candidate))return candidate.toString();
                Path parent=home.getParent();
                if(parent!=null){candidate=parent.resolve("bin").resolve(tool+ext);
                    if(Files.isRegularFile(candidate))return candidate.toString();}
                return tool;
            }
            private int withoutAnt() throws Exception {
                publish("Apache Ant nao encontrado. Usando javac do JDK local...");
                Path main=folder.resolve("src").resolve(spec.pkg.replace('.', '/')).resolve("Main.java");
                if(!Files.isRegularFile(main))throw new java.io.IOException("Classe Main.java nao encontrada no projeto.");
                Files.createDirectories(folder.resolve("build/classes"));
                List<String> javacCommand=new ArrayList<String>();
                javacCommand.add(jdkTool("javac"));
                javacCommand.add("-encoding");javacCommand.add("UTF-8");
                javacCommand.add("-source");javacCommand.add("8");
                javacCommand.add("-target");javacCommand.add("8");
                javacCommand.add("-cp");javacCommand.add("lib/*");
                javacCommand.add("-sourcepath");javacCommand.add("src");
                javacCommand.add("-d");javacCommand.add("build/classes");
                javacCommand.add(main.toString());
                int exit;
                try { exit=execute(javacCommand); }
                catch(java.io.IOException ex){
                    throw new java.io.IOException("JDK/javac nao encontrado. Instale um JDK offline ou adicione Apache Ant.",ex);
                }
                if(exit!=0)return exit;
                if(run){
                    publish("Codigo compilado. Abrindo o sistema...");
                    return execute(java.util.Arrays.asList(jdkTool("java"),"-cp",
                            "build/classes"+java.io.File.pathSeparator+"lib/*",spec.pkg+".Main"));
                }
                Files.createDirectories(folder.resolve("dist"));
                try { return execute(java.util.Arrays.asList(jdkTool("jar"),"cfe",
                        "dist/"+spec.name+".jar",spec.pkg+".Main","-C","build/classes",".")); }
                catch(java.io.IOException ex){
                    throw new java.io.IOException("Comando jar do JDK nao encontrado. Instale um JDK completo offline.",ex);
                }
            }
            @Override protected Integer doInBackground() throws Exception {
                try { return execute(command); }
                catch(java.io.IOException missingAnt){return withoutAnt();}
            }
            @Override protected void process(List<String> chunks){
                for(String line:chunks){log.append(line+"\n");log.setCaretPosition(log.getDocument().getLength());}
            }
            @Override protected void done(){
                busy=false;gerar.setEnabled(true);compilar.setEnabled(true);executar.setEnabled(true);
                try{
                    int exit=get();
                    if(exit==0){
                        if(run)message("Programa encerrado sem erros.");
                        else {
                            Path jar=folder.resolve("dist").resolve(spec.name+".jar");
                            Path sql=folder.resolve("sql/01_criar_banco.sql");
                            if(Files.isRegularFile(jar)&&Files.isRegularFile(sql))
                                message("Compilacao + verificacao dos artefatos: OK. CRUD com MySQL real: nao testado.");
                            else message("Compilacao terminou, mas faltam artefatos previstos.");
                        }
                    } else message("Compilacao/execucao retornou codigo "+exit+". Veja os detalhes acima.");
                } catch(Exception ex){
                    Throwable cause=ex.getCause()==null?ex:ex.getCause();
                    error("Falha na compilacao ou execucao",new Exception(cause.getMessage(),cause));
                }
            }
        }.execute();
    }
    private void message(String message){status.setText(message);log.append(message+"\n");log.setCaretPosition(log.getDocument().getLength());}
    private void error(String text,Exception ex){message(text+" | "+ex.getMessage());JOptionPane.showMessageDialog(this,
        text+"\n"+ex.getMessage(),"SwingForge",JOptionPane.ERROR_MESSAGE);}
    private final class FieldTableModel extends AbstractTableModel {
        private final String[] titles={"Nome", "Tipo", "Relacionado a", "Obrigatorio"};
        @Override public int getRowCount(){Spec.Entity e=selected();return e==null?0:e.fields.size();}
        @Override public int getColumnCount(){return titles.length;}
        @Override public String getColumnName(int col){return titles[col];}
        @Override public Class<?> getColumnClass(int col){return col==3?Boolean.class:String.class;}
        @Override public boolean isCellEditable(int row,int col){return true;}
        @Override public Object getValueAt(int row,int col){Spec.Field f=selected().fields.get(row);
            return col==0?f.name:col==1?f.type:col==2?f.reference:f.required;}
        @Override public void setValueAt(Object v,int row,int col){Spec.Field f=selected().fields.get(row);
            if(col==0)f.name=String.valueOf(v).trim();
            if(col==1){f.type=String.valueOf(v);if(!"Relacao".equals(f.type))f.reference="";}
            if(col==2)f.reference=v==null?"":String.valueOf(v);
            if(col==3)f.required=Boolean.TRUE.equals(v);
            fireTableRowsUpdated(row,row);
        }
    }
}
