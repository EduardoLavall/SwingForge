package swingforge.ui;

import swingforge.core.DatabaseSetup;
import swingforge.core.Generator;
import swingforge.core.FormDesign;
import swingforge.core.MysqlDriver;
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
import java.util.Arrays;
import java.util.List;

/** Interface 100% offline com GUI Editor e builder automatico. */
public final class SwingForgeApp extends JFrame {
    private Spec spec=Spec.example();
    private Path root;
    private final JTextField nome=Light.field(spec.name), pacote=Light.field(spec.pkg), banco=Light.field(spec.database);
    private final DefaultListModel<Spec.Entity> entidades=new DefaultListModel<Spec.Entity>();
    private final JList<Spec.Entity> lista=new JList<Spec.Entity>(entidades);
    private final FieldTableModel campos=new FieldTableModel();
    private final JTable tabela=new JTable(campos);
    private final JLabel status=Light.label("Pronto para criar seu primeiro projeto.",12,true);
    private final JLabel title=Light.label("Campos",17,false);
    private final JTextArea log=new JTextArea(6,80);
    private final JPanel advancedPanel=new JPanel(new GridLayout(3,2,12,8));
    private boolean advancedShown=false, busy=false;
    private boolean atualizandoProjeto=false, bancoAutomatico=true;
    private final JButton gerar=Light.button("Gerar pasta do projeto",true);
    private final JButton compilar=Light.button("Compilar e testar",false);
    private final JButton executar=Light.button("Executar sistema",false);
    private final JButton prepararBanco=Light.button("Preparar MySQL",false);

    public SwingForgeApp(){
        super("SwingForge — Offline Java CRUD Studio");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);setSize(1080,780);setMinimumSize(new Dimension(850,650));
        setLocationRelativeTo(null);getContentPane().setBackground(Light.BG);
        JPanel body=new JPanel(new BorderLayout(0,18));body.setBackground(Light.BG);
        body.setBorder(new EmptyBorder(20,22,17,22));setContentPane(body);
        body.add(header(),BorderLayout.NORTH);
        JPanel content=new JPanel(new BorderLayout(18,0));content.setBackground(Light.BG);
        content.add(entityEditor(),BorderLayout.WEST);content.add(mainEditor(),BorderLayout.CENTER);
        body.add(content,BorderLayout.CENTER);body.add(footer(),BorderLayout.SOUTH);
        lista.addListSelectionListener(e->{if(!e.getValueIsAdjusting())selectedChanged();});
        gerar.addActionListener(e->generate());compilar.addActionListener(e->compile());executar.addActionListener(e->run());
        prepararBanco.addActionListener(e->prepareDatabase());
        refreshProject();
        nome.getDocument().addDocumentListener(new DocumentListener(){
            public void insertUpdate(DocumentEvent e){sugerirBanco();}
            public void removeUpdate(DocumentEvent e){sugerirBanco();}
            public void changedUpdate(DocumentEvent e){sugerirBanco();}
        });
        banco.getDocument().addDocumentListener(new DocumentListener(){
            public void insertUpdate(DocumentEvent e){mudouBanco();}
            public void removeUpdate(DocumentEvent e){mudouBanco();}
            public void changedUpdate(DocumentEvent e){mudouBanco();}
        });
    }
    private static String nomeBanco(String nome){
        return nome.trim().replaceAll("([a-z0-9])([A-Z])","$1_$2").toLowerCase(java.util.Locale.ROOT);
    }
    private void sugerirBanco(){
        if(atualizandoProjeto || !bancoAutomatico)return;
        atualizandoProjeto=true;try{banco.setText(nomeBanco(nome.getText()));}finally{atualizandoProjeto=false;}
    }
    private void mudouBanco(){if(!atualizandoProjeto)bancoAutomatico=banco.getText().equals(nomeBanco(nome.getText()));}
    private JPanel header(){
        JPanel top=new JPanel(new BorderLayout());top.setBackground(Light.BG);
        JPanel brand=new JPanel(new GridLayout(2,1,0,4));brand.setBackground(Light.BG);
        JLabel name=Light.label("▦  SwingForge",27,false);name.setFont(new Font("Dialog",Font.BOLD,27));brand.add(name);
        brand.add(Light.label("1. Escolha o modelo   →   2. Personalize os campos   →   3. Gere e teste",12,true));top.add(brand,BorderLayout.WEST);
        JLabel indicator=Light.label("●  OFFLINE",12,true);indicator.setForeground(Light.SUCCESS);top.add(indicator,BorderLayout.EAST);
        return top;
    }
    private JPanel entityEditor(){
        JPanel side=new JPanel(new BorderLayout(0,12));side.setBackground(Light.PANEL);
        side.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Light.BORDER),new EmptyBorder(15,12,15,12)));
        side.setPreferredSize(new Dimension(248,200));
        side.add(Light.label("Entidades",17,false),BorderLayout.NORTH);
        lista.setFont(new Font("Dialog",Font.PLAIN,15));lista.setBackground(Light.PANEL);lista.setForeground(Light.TEXT);
        lista.setFixedCellHeight(42);lista.setSelectionBackground(new Color(213,228,248));lista.setSelectionForeground(Light.TEXT);
        JScrollPane scroll=new JScrollPane(lista);scroll.setBorder(null);side.add(scroll,BorderLayout.CENTER);
        JPanel actions=new JPanel(new GridLayout(2,1,0,8));actions.setBackground(Light.PANEL);
        JButton plus=Light.button("+  Nova entidade",true),remove=Light.button("Remover entidade",false);
        actions.add(plus);actions.add(remove);side.add(actions,BorderLayout.SOUTH);
        plus.addActionListener(e->newEntity());remove.addActionListener(e->removeEntity());
        return side;
    }
    private JPanel mainEditor(){
        JPanel panel=new JPanel(new BorderLayout(0,14));panel.setBackground(Light.BG);
        JPanel config=new JPanel(new BorderLayout(0,12));config.setBackground(Light.PANEL);
        config.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Light.BORDER),new EmptyBorder(16,17,16,17)));
        JPanel row=new JPanel(new BorderLayout(0,5));row.setBackground(Light.PANEL);
        row.add(Light.label("Nome do projeto",12,true),BorderLayout.NORTH);row.add(nome,BorderLayout.CENTER);
        config.add(row,BorderLayout.NORTH);
        JButton toggle=Light.button("⚙  Opções avançadas  ▾",false);
        toggle.setHorizontalAlignment(SwingConstants.LEFT);
        config.add(toggle,BorderLayout.CENTER);
        advancedPanel.setBackground(Light.PANEL);
        advancedPanel.add(Light.label("Pacote Java",12,true));advancedPanel.add(Light.label("Banco MySQL (personalizavel)",12,true));
        advancedPanel.add(pacote);advancedPanel.add(banco);
        JButton importarJDBC=Light.button("Importar Connector/J 8.1.0",false);
        advancedPanel.add(Light.label("Driver JDBC (offline)",12,true));
        advancedPanel.add(importarJDBC);
        importarJDBC.addActionListener(e->importDriver());advancedPanel.setVisible(false);
        config.add(advancedPanel,BorderLayout.SOUTH);
        toggle.addActionListener(e->{advancedShown=!advancedShown;advancedPanel.setVisible(advancedShown);
            toggle.setText(advancedShown?"⚙  Opções avançadas  ▴":"⚙  Opções avançadas  ▾");config.revalidate();});
        panel.add(config,BorderLayout.NORTH);
        JPanel fields=new JPanel(new BorderLayout(0,12));fields.setBackground(Light.PANEL);
        fields.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Light.BORDER),new EmptyBorder(16,17,16,17)));
        fields.add(title,BorderLayout.NORTH);
        tabela.setRowHeight(34);tabela.setBackground(Light.PANEL);tabela.setForeground(Light.TEXT);
        tabela.setSelectionBackground(new Color(213,228,248));tabela.setGridColor(Light.BORDER);
        tabela.getTableHeader().setBackground(Light.PANEL2);tabela.getTableHeader().setForeground(Light.TEXT);
        tabela.getTableHeader().setPreferredSize(new Dimension(100,34));
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JComboBox<String> typeBox=new JComboBox<String>(new String[]{"Texto","Inteiro","Decimal","Data","Booleano","Relacao"});
        typeBox.setBackground(Light.PANEL2);typeBox.setForeground(Light.TEXT);
        TableColumn typeColumn=tabela.getColumnModel().getColumn(1);typeColumn.setCellEditor(new DefaultCellEditor(typeBox));
        JScrollPane pane=new JScrollPane(tabela);pane.getViewport().setBackground(Light.PANEL);pane.setBorder(BorderFactory.createLineBorder(Light.BORDER));
        fields.add(pane,BorderLayout.CENTER);
        JPanel actions=new JPanel();actions.setLayout(new BoxLayout(actions,BoxLayout.Y_AXIS));actions.setBackground(Light.PANEL);
        JPanel fieldActions=new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));fieldActions.setBackground(Light.PANEL);
        JPanel formActions=new JPanel(new FlowLayout(FlowLayout.LEFT,8,4));formActions.setBackground(Light.PANEL);
        JButton add=Light.button("+  Campo",true),del=Light.button("Remover campo",false),relation=Light.button("Editar relação",false);
        JButton designer=Light.button("Personalizar formulario / Previa",true);
        fieldActions.add(add);fieldActions.add(del);fieldActions.add(relation);formActions.add(designer);
        actions.add(fieldActions);actions.add(formActions);fields.add(actions,BorderLayout.SOUTH);
        add.addActionListener(e->newField());del.addActionListener(e->removeField());relation.addActionListener(e->editRelation());
        designer.addActionListener(e->editForm());
        panel.add(fields,BorderLayout.CENTER);
        return panel;
    }
    private JPanel footer(){
        JPanel bottom=new JPanel(new BorderLayout(0,9));bottom.setBackground(Light.BG);
        JPanel actionRows=new JPanel();actionRows.setLayout(new BoxLayout(actionRows,BoxLayout.Y_AXIS));actionRows.setBackground(Light.BG);
        JPanel primary=new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));primary.setBackground(Light.BG);
        JPanel secondary=new JPanel(new FlowLayout(FlowLayout.LEFT,8,4));secondary.setBackground(Light.BG);
        JButton novo=Light.button("Novo modelo",false),abrir=Light.button("Abrir projeto",false),salvar=Light.button("Salvar modelo",false);
        primary.add(novo);primary.add(abrir);primary.add(salvar);primary.add(gerar);
        secondary.add(compilar);secondary.add(prepararBanco);secondary.add(executar);
        JButton detalhes=Light.button("Mostrar detalhes ▾",false);secondary.add(detalhes);
        actionRows.add(primary);actionRows.add(secondary);
        novo.addActionListener(e->newProject());abrir.addActionListener(e->openProject());salvar.addActionListener(e->saveModel());
        bottom.add(actionRows,BorderLayout.NORTH);
        log.setEditable(false);log.setLineWrap(true);log.setWrapStyleWord(true);log.setForeground(Light.MUTED);log.setBackground(Light.PANEL);
        log.setFont(new Font(Font.MONOSPACED,Font.PLAIN,11));log.setBorder(new EmptyBorder(8,12,8,12));
        JScrollPane p=new JScrollPane(log);p.setBorder(BorderFactory.createLineBorder(Light.BORDER));
        p.setPreferredSize(new Dimension(500,105));p.setVisible(false);bottom.add(p,BorderLayout.CENTER);
        detalhes.addActionListener(e->{boolean visible=!p.isVisible();p.setVisible(visible);
            detalhes.setText(visible?"Ocultar detalhes ▴":"Mostrar detalhes ▾");bottom.revalidate();});
        bottom.add(status,BorderLayout.SOUTH);return bottom;
    }
    private void flushEditors(){if(tabela.isEditing())tabela.getCellEditor().stopCellEditing();
        spec.name=nome.getText().trim();spec.pkg=pacote.getText().trim();spec.database=banco.getText().trim();}
    private void refreshProject(){
        atualizandoProjeto=true;
        try{nome.setText(spec.name);pacote.setText(spec.pkg);banco.setText(spec.database);}
        finally{atualizandoProjeto=false;}
        bancoAutomatico=spec.database.equals(nomeBanco(spec.name));
        entidades.clear();for(Spec.Entity e:spec.entities)entidades.addElement(e);
        if(!spec.entities.isEmpty())lista.setSelectedIndex(0);
        campos.fireTableDataChanged();
    }
    private Spec.Entity selected(){return lista.getSelectedValue();}
    private void selectedChanged(){Spec.Entity e=selected();title.setText(e==null?"Campos":"Campos de "+e.name);
        campos.fireTableDataChanged();refreshRelationEditors();}
    private void refreshRelationEditors(){JComboBox<String> choices=new JComboBox<String>();choices.addItem("");
        for(Spec.Entity entity:spec.entities)choices.addItem(entity.name);
        choices.setBackground(Light.PANEL2);choices.setForeground(Light.TEXT);
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
    private void editForm(){
        if(spec.entities.isEmpty())return;
        flushEditors();
        if(FormEditor.edit(this,spec))
            message("Tela completa atualizada. Salve o modelo e gere o projeto para aplicar as mudancas.");
    }
    private void newProject(){if(JOptionPane.showConfirmDialog(this,"Criar um novo projeto? Alteracoes nao salvas serao perdidas.",
            "Novo projeto",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        Object selectedTemplate=JOptionPane.showInputDialog(this,"Modelo inicial do projeto:","Novo projeto",
            JOptionPane.QUESTION_MESSAGE,null,
            new String[]{"Cadastro de Eventos (exemplo UFN)","Produtos e Categorias","Projeto vazio"},
            "Cadastro de Eventos (exemplo UFN)");
        if(selectedTemplate==null)return;
        if("Produtos e Categorias".equals(selectedTemplate))spec=Spec.estoqueExample();
        else if("Projeto vazio".equals(selectedTemplate)){
            spec=new Spec();Spec.Entity e=new Spec.Entity("Item");
            e.fields.add(new Spec.Field("nome","Texto","",true));spec.entities.add(e);
        }else spec=Spec.example();
        root=null;refreshProject();}
    private JFileChooser chooser(){JFileChooser f=new JFileChooser();f.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        f.setDialogTitle("Selecione uma pasta local");return f;}
    private void openProject(){JFileChooser ch=chooser();if(ch.showOpenDialog(this)!=JFileChooser.APPROVE_OPTION)return;
        Path folder=ch.getSelectedFile().toPath();try{Spec other=ProjectStore.read(folder.resolve("swingforge.json"));
            root=folder;spec=other;refreshProject();message("Projeto reaberto: "+folder);
        }catch(Exception ex){error("Nao foi possivel abrir. A v1 edita projetos com swingforge.json. Importador de projetos legados: etapa 2.",ex);}}
    private Path chooseRoot(){
        if(root!=null)return root;
        return Paths.get(System.getProperty("user.dir"),"output",spec.name).toAbsolutePath().normalize();
    }
    private void importDriver(){
        JFileChooser ch=new JFileChooser();
        ch.setDialogTitle("Selecione o ZIP do professor ou mysql-connector-j-8.1.0.jar");
        ch.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if(ch.showOpenDialog(this)!=JFileChooser.APPROVE_OPTION)return;
        try{
            Path copied=MysqlDriver.importFile(ch.getSelectedFile().toPath());
            message("Connector/J 8.1.0 importado localmente: "+copied);
            if(root!=null && Files.isDirectory(root.resolve("lib"))){
                MysqlDriver.copyTo(root,root.resolve(".swingforge/backups/import-driver"));
                message("Driver atualizado no projeto: "+root.resolve("lib").resolve(MysqlDriver.NAME));
            }
        }catch(Exception ex){error("Nao foi possivel importar o Connector/J",ex);}
    }
    private void saveModel(){try{flushEditors();Validator.validate(spec);Path dest=chooseRoot();if(dest==null)return;
        if(root==null && Files.exists(dest.resolve("swingforge.json"))) {
            int answer=JOptionPane.showConfirmDialog(this,"Ja existe um modelo em "+dest+". Substituir o modelo?",
                "Confirmar salvamento",JOptionPane.YES_NO_OPTION);
            if(answer!=JOptionPane.YES_OPTION)return;
        }
        Files.createDirectories(dest);ProjectStore.write(dest.resolve("swingforge.json"),spec);root=dest;
        message("Modelo salvo: "+dest.resolve("swingforge.json"));
    }catch(Exception ex){error("Erro ao salvar",ex);}}
    private boolean generate(){try{flushEditors();Validator.validate(spec);Path dest=chooseRoot();if(dest==null)return false;
        if(Files.isDirectory(dest) && (root==null || Files.exists(dest.resolve("src")))){int conf=JOptionPane.showConfirmDialog(this,
            "A pasta ja existe: "+dest+"\nGerar codigo pode sobrescrever arquivos. Backups serao salvos em .swingforge/backups. Continuar?",
            "Confirmar geracao",JOptionPane.YES_NO_OPTION);if(conf!=JOptionPane.YES_OPTION)return false;}
        int count=Generator.generate(spec,dest);root=dest;message("Projeto gerado em "+dest+" ("+count+" arquivos).");
        if(MysqlDriver.find()==null)message("ATENCAO: Connector/J nao encontrado. Abra Opcoes avancadas > Importar Connector/J 8.1.0.");
        else message("Conector do professor disponivel em lib/"+MysqlDriver.NAME);
        return true;
    }catch(Exception ex){error("Erro ao gerar",ex);return false;}}
    /** Verifica o MySQL local; nao guarda a senha e nao modifica dados sem confirmacao. */
    private void prepareDatabase(){
        if(busy)return;
        try{
            flushEditors();Validator.validate(spec);
            if(root==null || !Files.isDirectory(root.resolve("src"))){
                JOptionPane.showMessageDialog(this,
                    "Gere a pasta do projeto antes de preparar o MySQL.",
                    "Projeto nao gerado",JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            Path model=root.resolve("swingforge.json");
            if(!Files.isRegularFile(model) || !ProjectStore.serialize(spec).equals(
                    new String(Files.readAllBytes(model),StandardCharsets.UTF_8))){
                int result=JOptionPane.showConfirmDialog(this,
                    "O modelo foi alterado desde a ultima geracao. Gerar o codigo e SQL antes de continuar?",
                    "Atualizar modelo",JOptionPane.YES_NO_OPTION);
                if(result!=JOptionPane.YES_OPTION||!generate())return;
            }
        }catch(Exception ex){error("Erro ao validar o modelo",ex);return;}
        Path driver=root.resolve("lib").resolve(MysqlDriver.NAME);
        if(!Files.isRegularFile(driver)){
            try{
                if(!MysqlDriver.copyTo(root,root.resolve(".swingforge/backups/driver-setup"))){
                    JOptionPane.showMessageDialog(this,
                        "Connector/J 8.1.0 ausente. Verifique tools/jdbc no SwingForge.",
                        "Dependencia ausente",JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }catch(Exception ex){error("Nao foi possivel preparar o Connector/J",ex);return;}
        }
        JTextField host=Light.field("localhost"),port=Light.field("3306"),user=Light.field("root");
        JPasswordField password=new JPasswordField(18);
        Object[] fields={"Conexao ao servidor MySQL local", "Banco: "+spec.database,
            "Host",host,"Porta",port,"Usuario",user,"Senha (nao sera salva)",password};
        int response=JOptionPane.showConfirmDialog(this,fields,"Preparar MySQL",
                JOptionPane.OK_CANCEL_OPTION,JOptionPane.QUESTION_MESSAGE);
        if(response!=JOptionPane.OK_OPTION){password.setText("");return;}
        char[] secret=password.getPassword();password.setText("");
        final DatabaseSetup.Credentials credentials;
        try{credentials=new DatabaseSetup.Credentials(host.getText().trim(),port.getText().trim(),
                user.getText().trim(),secret);}
        catch(Exception ex){Arrays.fill(secret,'\0');error("Dados de conexao invalidos",ex);return;}
        Arrays.fill(secret,'\0');
        final Spec snapshot=ProjectStore.deserialize(ProjectStore.serialize(spec));
        final Path jdbc=driver;
        busy=true;setActionsEnabled(false);
        message("Conectando ao servidor e verificando banco "+snapshot.database+"...");
        new SwingWorker<DatabaseSetup.Inspection,Void>(){
            @Override protected DatabaseSetup.Inspection doInBackground() throws Exception{
                return DatabaseSetup.inspect(snapshot,credentials,jdbc);
            }
            @Override protected void done(){
                busy=false;setActionsEnabled(true);
                try{
                    DatabaseSetup.Inspection result=get();
                    message(result.describe(snapshot.database).trim());
                    if(!result.safeToPrepare()){
                        JOptionPane.showMessageDialog(SwingForgeApp.this,
                            result.describe(snapshot.database)+"\nNenhuma alteracao foi feita.\nRevise as tabelas existentes manualmente.",
                            "Estrutura incompativel",JOptionPane.WARNING_MESSAGE);
                        credentials.clear();return;
                    }
                    if(result.ready()){
                        JOptionPane.showMessageDialog(SwingForgeApp.this,
                            "Banco e tabelas ja existem. Nada foi alterado.",
                            "MySQL pronto",JOptionPane.INFORMATION_MESSAGE);
                        credentials.clear();return;
                    }
                    String summary=result.describe(snapshot.database)+"\nDeseja criar somente as estruturas ausentes?\n"+
                        "O SwingForge executara o SQL gerado (CREATE DATABASE / CREATE TABLE e FKs).\n"+
                        "Nao executa DROP nem apaga registros. Tabelas existentes nao serao migradas.\n"+
                        "A operacao nao tem rollback automatico de DDL.";
                    int confirm=JOptionPane.showConfirmDialog(SwingForgeApp.this,summary,
                        "Confirmar criacao do banco",JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE);
                    if(confirm!=JOptionPane.YES_OPTION){credentials.clear();message("Preparacao do MySQL cancelada.");return;}
                    applyDatabase(snapshot,credentials,jdbc);
                }catch(Exception ex){credentials.clear();
                    Throwable cause=ex.getCause()==null?ex:ex.getCause();
                    error("Falha ao verificar MySQL",new Exception(cause.getMessage()));
                }
            }
        }.execute();
    }
    private void applyDatabase(final Spec snapshot,final DatabaseSetup.Credentials credentials,final Path jdbc){
        busy=true;setActionsEnabled(false);
        message("Criando somente banco, tabelas e relacionamentos ausentes...");
        new SwingWorker<DatabaseSetup.Inspection,Void>(){
            @Override protected DatabaseSetup.Inspection doInBackground() throws Exception{
                return DatabaseSetup.prepare(snapshot,credentials,jdbc);
            }
            @Override protected void done(){
                credentials.clear();busy=false;setActionsEnabled(true);
                try{
                    DatabaseSetup.Inspection result=get();
                    message(result.describe(snapshot.database).trim());
                    JOptionPane.showMessageDialog(SwingForgeApp.this,
                        "Banco "+snapshot.database+" preparado com sucesso.\n"+
                        "Agora execute o sistema e informe as credenciais quando solicitado.",
                        "MySQL pronto",JOptionPane.INFORMATION_MESSAGE);
                }catch(Exception ex){Throwable cause=ex.getCause()==null?ex:ex.getCause();
                    error("Falha ao criar banco/tabelas",new Exception(cause.getMessage()));}
            }
        }.execute();
    }
    private void setActionsEnabled(boolean enabled){
        gerar.setEnabled(enabled);compilar.setEnabled(enabled);
        prepararBanco.setEnabled(enabled);executar.setEnabled(enabled);
    }
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
        Path driver=folder.resolve("lib").resolve(MysqlDriver.NAME);
        if (!Files.isRegularFile(driver)) {
            try {
                if (!MysqlDriver.copyTo(folder, folder.resolve(".swingforge/backups/auto-driver"))) {
                    message("O projeto nao tem o Connector/J 8.1.0. Importe o ZIP do professor em Opcoes avancadas.");
                    JOptionPane.showMessageDialog(this,
                            "Driver MySQL ausente.\nAbra Opcoes avancadas > Importar Connector/J 8.1.0.",
                            "Dependencia ausente", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                message("Connector/J 8.1.0 restaurado automaticamente em lib/.");
            } catch (Exception ex) {
                error("Falha ao preparar o driver MySQL", ex);
                return;
            }
        }
        String antPath="ant";
        Path local=folder.resolve(System.getProperty("os.name").toLowerCase().contains("win")?
                "tools/ant/bin/ant.bat":"tools/ant/bin/ant");
        if(Files.isRegularFile(local))antPath=local.toAbsolutePath().toString();
        final List<String> command=new ArrayList<String>();command.add(antPath);
        if(!run){command.add("clean");command.add("jar");}else{command.add("run");}
        busy=true;setActionsEnabled(false);
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
                Path manifest = folder.resolve("build/manifest.mf");
                Files.write(manifest, java.util.Arrays.asList(
                        "Manifest-Version: 1.0",
                        "Main-Class: " + spec.pkg + ".Main",
                        "Class-Path: ../lib/mysql-connector-j-8.1.0.jar",
                        ""), StandardCharsets.UTF_8);
                try { return execute(java.util.Arrays.asList(jdkTool("jar"),"cfm",
                        "dist/"+spec.name+".jar", "build/manifest.mf", "-C","build/classes",".")); }
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
                busy=false;setActionsEnabled(true);
                try{
                    int exit=get();
                    if(exit==0){
                        if(run)message("Programa encerrado sem erros.");
                        else {
                            Path jar=folder.resolve("dist").resolve(spec.name+".jar");
                            Path sql=folder.resolve("sql/01_criar_banco.sql");
                            if(Files.isRegularFile(jar)&&Files.isRegularFile(sql))
                                message("Compilacao + artefatos: OK. Banco MySQL ainda precisa de verificacao.");
                            else message("Compilacao terminou, mas faltam artefatos previstos.");
                        int wantsDb=JOptionPane.showConfirmDialog(SwingForgeApp.this,
                            "Compilacao concluida. Deseja verificar e preparar o MySQL agora?",
                            "Verificar banco de dados",JOptionPane.YES_NO_OPTION);
                        if(wantsDb==JOptionPane.YES_OPTION)SwingUtilities.invokeLater(()->prepareDatabase());
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
            if(col==0){String previous=f.name;f.name=String.valueOf(v).trim();
                for(Spec.FormField ui:selected().formFields)
                    if(ui.field.equals(previous))ui.field=f.name;
            }
            if(col==1){String previous=f.type; f.type=String.valueOf(v);if(!"Relacao".equals(f.type))f.reference="";
                if(!previous.equals(f.type)) for(Spec.FormField ui:selected().formFields)
                    if(ui.field.equals(f.name)){ui.widget=FormDesign.defaultWidget(f);ui.options="";}
            }
            if(col==2)f.reference=v==null?"":String.valueOf(v);
            if(col==3)f.required=Boolean.TRUE.equals(v);
            fireTableRowsUpdated(row,row);
        }
    }
}