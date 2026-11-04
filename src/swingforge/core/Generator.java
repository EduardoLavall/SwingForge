package swingforge.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;

/** Gera projeto Ant/NetBeans offline com classes no mesmo pacote e SQL. */
public final class Generator {
    private Generator() { }
    private static String q(String raw) {return "\""+raw.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r")+"\"";}
    private static String upper(String raw){return Character.toUpperCase(raw.charAt(0))+raw.substring(1);}
    private static String javaType(String type){
        if("Inteiro".equals(type)||"Relacao".equals(type))return "Integer";
        if("Decimal".equals(type))return "java.math.BigDecimal";
        if("Data".equals(type))return "java.time.LocalDate";
        if("Booleano".equals(type))return "Boolean";
        return "String";
    }
    private static String tipoCampo(Spec.Field f) {
        return "Relacao".equals(f.type) ? f.reference : javaType(f.type);
    }
    private static String sqlType(String type){
        if("Inteiro".equals(type)||"Relacao".equals(type))return "INT";
        if("Decimal".equals(type))return "DECIMAL(12,2)";
        if("Data".equals(type))return "DATE";
        if("Booleano".equals(type))return "BOOLEAN";
        return "VARCHAR(255)";
    }
    private static String title(String field) {String x=field.replace('_',' ');return Character.toUpperCase(x.charAt(0))+x.substring(1);}
    private static String arrFields(Spec.Entity e,int variant,Spec spec){
        StringBuilder b=new StringBuilder("new String[] {");
        for(Spec.Field f:e.fields){if(b.charAt(b.length()-1)!='{')b.append(", ");
            switch(variant){
                case 0:b.append(q(f.name));break;
                case 1:b.append(q(f.type));break;
                case 2:b.append(q("Relacao".equals(f.type)?Spec.table(f.reference):""));break;
                case 3:b.append(q("Relacao".equals(f.type)?Spec.label(find(spec,f.reference)):""));break;
                case 4:b.append(q(title(f.name.replace("_id",""))));break;
            }
        }return b.append('}').toString();
    }
    private static Spec.Entity find(Spec s,String name){for(Spec.Entity e:s.entities)if(e.name.equals(name))return e;throw new IllegalArgumentException(name);}
    private static String bools(Spec.Entity e){StringBuilder b=new StringBuilder("new boolean[] {");for(Spec.Field f:e.fields){if(b.charAt(b.length()-1)!='{')b.append(", ");b.append(f.required);}return b.append('}').toString();}
    public static String bean(Spec spec, Spec.Entity e) {
        StringBuilder b = new StringBuilder("package ").append(spec.pkg).append(".beans;\n\n");
        b.append("/** Modelo de dados da entidade ").append(e.name).append(". */\n")
            .append("public class ").append(e.name).append(" implements Entidade {\n")
            .append("    private Integer id;\n");
        for (Spec.Field f : e.fields) {
            b.append("    private ").append(tipoCampo(f)).append(' ').append(f.name).append(";\n");
        }
        b.append("\n    public ").append(e.name).append("() {\n    }\n\n");
        b.append("    public ").append(e.name).append("(");
        for (int i = 0; i < e.fields.size(); i++) {
            Spec.Field f = e.fields.get(i);
            if (i > 0) b.append(", ");
            b.append(tipoCampo(f)).append(' ').append(f.name);
        }
        b.append(") {\n");
        for (Spec.Field f : e.fields) {
            b.append("        this.").append(f.name).append(" = ").append(f.name).append(";\n");
        }
        b.append("    }\n\n")
            .append("    public Integer getId() {\n        return id;\n    }\n\n")
            .append("    public void setId(Integer id) {\n        this.id = id;\n    }\n");
        for (Spec.Field f : e.fields) {
            String label = upper(f.name);
            String type = tipoCampo(f);
            b.append("\n    public ").append(type).append(" get").append(label)
                .append("() {\n        return ").append(f.name).append(";\n    }\n\n")
                .append("    public void set").append(label).append('(').append(type).append(' ')
                .append(f.name).append(") {\n        this.").append(f.name).append(" = ")
                .append(f.name).append(";\n    }\n");
        }
        String display = Spec.label(e);
        b.append("\n    @Override\n    public String toString() {\n        return ")
            .append("id".equals(display) ? "id == null ? \"\" : String.valueOf(id)" : display + " == null ? \"\" : String.valueOf(" + display + ")")
            .append(";\n    }\n\n")
            .append("    @Override\n    public boolean equals(Object outro) {\n")
            .append("        if (this == outro) return true;\n")
            .append("        if (!(outro instanceof ").append(e.name).append(")) return false;\n")
            .append("        ").append(e.name).append(" entidade = (").append(e.name).append(") outro;\n")
            .append("        return id != null && id.equals(entidade.id);\n    }\n\n")
            .append("    @Override\n    public int hashCode() {\n        return id == null ? 0 : id.hashCode();\n    }\n");
        b.append("\n    public Object[] obterDados() {\n        return new Object[] {");
        for (int i = 0; i < e.fields.size(); i++) {
            if (i > 0) b.append(", ");
            b.append(e.fields.get(i).name);
        }
        return b.append("};\n    }\n}\n").toString();
    }
    private static String conversao(Spec.Field f, String valor) {
        if ("Inteiro".equals(f.type)) return "numeroInteiro(" + valor + ")";
        if ("Decimal".equals(f.type)) return "numeroDecimal(" + valor + ")";
        if ("Data".equals(f.type)) return "dataLocal(" + valor + ")";
        if ("Booleano".equals(f.type)) return "valorBooleano(" + valor + ")";
        return valor + " == null ? null : String.valueOf(" + valor + ")";
    }
    private static String consultaDAO(Spec spec, Spec.Entity e) {
        StringBuilder select=new StringBuilder("SELECT t.id"), joins=new StringBuilder();
        List<String> filtros=new java.util.ArrayList<String>();
        for(int i=0;i<e.fields.size();i++){
            Spec.Field f=e.fields.get(i);select.append(", t.`").append(f.name).append("`");
            if("Relacao".equals(f.type)){
                String label=Spec.label(find(spec,f.reference));
                select.append(", r").append(i).append(".`").append(label).append("` AS __rotulo_").append(i);
                joins.append(" LEFT JOIN `").append(Spec.table(f.reference)).append("` r").append(i)
                    .append(" ON r").append(i).append(".id = t.`").append(f.name).append("`");
                filtros.add("LOWER(COALESCE(r"+i+".`"+label+"`, '')) LIKE ?");
            }else if("Texto".equals(f.type)) filtros.add("LOWER(t.`"+f.name+"`) LIKE ?");
        }
        String base=select.append(" FROM `").append(Spec.table(e.name)).append("` t").append(joins).toString();
        String filter=String.join(" OR ",filtros);
        StringBuilder b=new StringBuilder("    @Override\n    public List<Registro> listar(String busca) throws SQLException {\n")
            .append("        List<Registro> resultados = new ArrayList<Registro>();\n")
            .append("        String sql = ").append(q(base)).append(";\n")
            .append("        boolean pesquisar = ").append(filtros.isEmpty()?"false":"busca != null && !busca.trim().isEmpty()").append(";\n");
        if(!filtros.isEmpty())b.append("        if (pesquisar) sql += ").append(q(" WHERE "+filter)).append(";\n");
        b.append("        sql += \" ORDER BY t.id DESC\";\n")
            .append("        try (Connection conexao = Conexao.abrir();\n")
            .append("                PreparedStatement comando = conexao.prepareStatement(sql)) {\n");
        if(!filtros.isEmpty()) {
            b.append("            if (pesquisar) {\n");
            for(int i=0;i<filtros.size();i++)b.append("                comando.setString(").append(i+1).append(", \"%\" + busca.trim().toLowerCase(java.util.Locale.ROOT) + \"%\");\n");
            b.append("            }\n");
        }
        b.append("            try (ResultSet rs = comando.executeQuery()) {\n")
            .append("                while (rs.next()) {\n")
            .append("                    Object[] dados = new Object[").append(e.fields.size()).append("];\n")
            .append("                    String[] exibicao = new String[").append(e.fields.size()).append("];\n");
        for(int i=0;i<e.fields.size();i++){
            Spec.Field f=e.fields.get(i);b.append("                    dados[").append(i).append("] = rs.getObject(").append(q(f.name)).append(");\n")
                .append("                    exibicao[").append(i).append("] = rs.getString(").append(q("Relacao".equals(f.type)?"__rotulo_"+i:f.name)).append(");\n")
                .append("                    if (exibicao[").append(i).append("] == null) exibicao[").append(i).append("] = \"\";\n");
        }
        return b.append("                    resultados.add(new Registro(rs.getInt(\"id\"), dados, exibicao));\n")
            .append("                }\n            }\n        }\n        return resultados;\n    }\n\n").toString();
    }
    private static String gravarDAO(Spec.Entity e, boolean editar) {
        StringBuilder colunas=new StringBuilder(), marcas=new StringBuilder(), updates=new StringBuilder();
        for(Spec.Field f:e.fields){
            if(colunas.length()>0){colunas.append(", ");marcas.append(", ");updates.append(", ");}
            colunas.append('`').append(f.name).append('`');marcas.append('?');updates.append('`').append(f.name).append("` = ?");
        }
        String sql=editar?"UPDATE `"+Spec.table(e.name)+"` SET "+updates+" WHERE id = ?":
            "INSERT INTO `"+Spec.table(e.name)+"` ("+colunas+") VALUES ("+marcas+")";
        StringBuilder b=new StringBuilder("    public void ").append(editar?"atualizar":"inserir").append('(').append(e.name).append(" item) throws SQLException {\n");
        if(editar)b.append("        if (item.getId() == null) throw new IllegalArgumentException(\"Informe o ID para atualizar.\");\n");
        b.append("        Object[] dados = validarEntrada(valores(item));\n")
            .append("        String sql = ").append(q(sql)).append(";\n")
            .append("        try (Connection conexao = Conexao.abrir();\n")
            .append("                PreparedStatement comando = conexao.prepareStatement(sql)) {\n");
        for(int i=0;i<e.fields.size();i++){
            Spec.Field f=e.fields.get(i);String type=f.type;String sqlType="Texto".equals(type)?"VARCHAR":"Decimal".equals(type)?"DECIMAL":"Data".equals(type)?"DATE":"Booleano".equals(type)?"BOOLEAN":"INTEGER";
            String setter="Texto".equals(type)?"setString":"Decimal".equals(type)?"setBigDecimal":"Data".equals(type)?"setDate":"Booleano".equals(type)?"setBoolean":"setInt";
            String cast="Texto".equals(type)?"(String) ":"Decimal".equals(type)?"(java.math.BigDecimal) ":"Data".equals(type)?"java.sql.Date.valueOf((java.time.LocalDate) ":"Booleano".equals(type)?"(Boolean) ":"(Integer) ";
            b.append("            if (dados[").append(i).append("] == null) comando.setNull(").append(i+1).append(", Types.").append(sqlType).append(");\n")
                .append("            else comando.").append(setter).append('(').append(i+1).append(", ").append(cast).append("dados[").append(i).append(']');
            if("Data".equals(type))b.append(')');
            b.append(");\n");
        }
        if(editar)b.append("            comando.setInt(").append(e.fields.size()+1).append(", item.getId());\n");
        return b.append("            comando.executeUpdate();\n        }\n    }\n\n").toString();
    }
    public static String dao(Spec spec, Spec.Entity e) {
        StringBuilder b = new StringBuilder("package ").append(spec.pkg).append(".dao;\n\n")
            .append("import ").append(spec.pkg).append(".beans.*;\n")
            .append("import ").append(spec.pkg).append(".conexao.Conexao;\n")
            .append("import java.sql.*;\nimport java.util.ArrayList;\nimport java.util.List;\n\n")
            .append("/** Acesso aos dados de ").append(e.name).append(". */\n")
            .append("public final class ").append(e.name).append("DAO extends CrudDAO {\n")
            .append("    public ").append(e.name).append("DAO() {\n        super(\n")
            .append("            ").append(q(Spec.table(e.name))).append(",\n")
            .append("            ").append(arrFields(e,0,spec)).append(",\n")
            .append("            ").append(arrFields(e,1,spec)).append(",\n")
            .append("            ").append(arrFields(e,2,spec)).append(",\n")
            .append("            ").append(arrFields(e,3,spec)).append(",\n")
            .append("            ").append(bools(e)).append(");\n    }\n\n")
            .append(consultaDAO(spec,e))
            .append("    /** Consulta e transforma os resultados em objetos da entidade. */\n")
            .append("    public List<").append(e.name).append("> listarEntidades(String busca) throws SQLException {\n")
            .append("        List<").append(e.name).append("> lista = new ArrayList<").append(e.name).append(">();\n")
            .append("        for (Registro registro : listar(busca)) {\n")
            .append("            ").append(e.name).append(" item = new ").append(e.name).append("();\n")
            .append("            item.setId(registro.id);\n");
        for (int i = 0; i < e.fields.size(); i++) {
            Spec.Field f=e.fields.get(i);String setter="set"+upper(f.name);String value="registro.valores["+i+"]";
            if ("Relacao".equals(f.type)) {
                Spec.Entity target=find(spec,f.reference);String label=Spec.label(target);
                b.append("            if (").append(value).append(" != null) {\n")
                    .append("                ").append(f.reference).append(" relacionado = new ").append(f.reference).append("();\n")
                    .append("                relacionado.setId(numeroInteiro(").append(value).append("));\n");
                if (!"id".equals(label)) b.append("                relacionado.set").append(upper(label)).append("(registro.exibicao[").append(i).append("]);\n");
                b.append("                item.").append(setter).append("(relacionado);\n            }\n");
            } else b.append("            item.").append(setter).append("(").append(conversao(f,value)).append(");\n");
        }
        b.append("            lista.add(item);\n        }\n        return lista;\n    }\n\n")
            .append("    @Override\n    public List<Registro> listarRegistros(String busca) throws SQLException {\n")
            .append("        List<Registro> resultado = new ArrayList<Registro>();\n")
            .append("        for (").append(e.name).append(" item : listarEntidades(busca)) {\n")
            .append("            Object[] dados = valores(item);\n")
            .append("            String[] exibicao = new String[] {");
        for (int i=0;i<e.fields.size();i++) {
            Spec.Field f=e.fields.get(i);String getter="item.get"+upper(f.name)+"()";
            if(i>0)b.append(", ");
            b.append(getter).append(" == null ? \"\" : String.valueOf(").append(getter).append(")");
        }
        b.append("};\n            resultado.add(new Registro(item.getId(), dados, exibicao));\n")
            .append("        }\n        return resultado;\n    }\n\n")
            .append("    @Override\n    public void salvar(Integer id, Object[] entrada) throws SQLException {\n")
            .append("        Object[] dados = validarEntrada(entrada);\n")
            .append("        ").append(e.name).append(" item = new ").append(e.name).append("();\n")
            .append("        item.setId(id);\n");
        for (int i=0;i<e.fields.size();i++) {
            Spec.Field f=e.fields.get(i);String setter="set"+upper(f.name);
            if("Relacao".equals(f.type)) {
                b.append("        if (dados[").append(i).append("] != null) {\n")
                    .append("            ").append(f.reference).append(" relacionado = new ").append(f.reference).append("();\n")
                    .append("            relacionado.setId((Integer) dados[").append(i).append("]);\n")
                    .append("            item.").append(setter).append("(relacionado);\n        }\n");
            } else b.append("        item.").append(setter).append("((").append(tipoCampo(f)).append(") dados[").append(i).append("]);\n");
        }
        b.append("        if (id == null) inserir(item); else atualizar(item);\n    }\n\n")
            .append(gravarDAO(e,false))
            .append(gravarDAO(e,true))
            .append("    @Override\n    public void excluir(int id) throws SQLException {\n")
            .append("        String sql = ").append(q("DELETE FROM `"+Spec.table(e.name)+"` WHERE id = ?")).append(";\n")
            .append("        try (Connection conexao = Conexao.abrir();\n")
            .append("                PreparedStatement comando = conexao.prepareStatement(sql)) {\n")
            .append("            comando.setInt(1, id);\n            comando.executeUpdate();\n        }\n    }\n\n")
            .append("    private Object[] valores(").append(e.name).append(" item) {\n")
            .append("        return new Object[] {");
        for (int i=0;i<e.fields.size();i++) {
            Spec.Field f=e.fields.get(i);String getter="item.get"+upper(f.name)+"()";
            if(i>0)b.append(", ");
            b.append("Relacao".equals(f.type)?getter+" == null ? null : "+getter+".getId()":getter);
        }
        b.append("};\n    }\n");
        java.util.Set<String> refs=new java.util.LinkedHashSet<String>();
        for(Spec.Field f:e.fields)if("Relacao".equals(f.type))refs.add(f.reference);
        if(!refs.isEmpty()) {
            b.append("\n    @Override\n    public List<Entidade> opcoes(String tabela, String rotulo) throws SQLException {\n");
            for(String ref:refs)b.append("        if (").append(q(Spec.table(ref))).append(".equals(tabela)) return new ArrayList<Entidade>(new ")
                .append(ref).append("DAO().listarEntidades(\"\"));\n");
            b.append("        return super.opcoes(tabela, rotulo);\n    }\n");
        }
        return b.append("}\n").toString();
    }
    /** Ordenacao e rotulos independentes dos campos SQL, configurados no GUI Editor. */
    private static String arrayOf(List<String> values){
        StringBuilder b=new StringBuilder("new String[] {");
        for(String x:values){if(b.charAt(b.length()-1)!='{')b.append(", ");b.append(q(x));}
        return b.append('}').toString();
    }
    private static String formIndices(Spec.Entity e){
        FormDesign.normalize(e);StringBuilder b=new StringBuilder("new int[] {");
        for(Spec.FormField u:e.formFields)if(u.visible){
            if(b.charAt(b.length()-1)!='{')b.append(", ");
            for(int i=0;i<e.fields.size();i++)if(e.fields.get(i).name.equals(u.field)){b.append(i);break;}
        }
        return b.append('}').toString();
    }
    private static String formArray(Spec.Entity e,int kind){
        FormDesign.normalize(e);List<String> values=new java.util.ArrayList<String>();
        for(Spec.FormField u:e.formFields)if(u.visible)
            values.add(kind==0?u.label:kind==1?u.widget:u.options);
        return arrayOf(values);
    }
    private static String tableLabels(Spec.Entity e){
        FormDesign.normalize(e);List<String> values=new java.util.ArrayList<String>();
        for(Spec.Field f:e.fields){
            String label=FormDesign.label(f.name);
            for(Spec.FormField u:e.formFields)if(u.field.equals(f.name)){label=u.label;break;}
            values.add(label);
        }
        return arrayOf(values);
    }
    public static String tela(Spec spec, Spec.Entity e) {
        FormDesign.normalize(e);
        StringBuilder b = new StringBuilder("package ").append(spec.pkg).append(".telas;\n\n")
            .append("import ").append(spec.pkg).append(".dao.").append(e.name).append("DAO;\n")
            .append("import ").append(spec.pkg).append(".beans.").append("Entidade;\n")
            .append("import javax.swing.*;\n\n")
            .append("/** Cadastro de ").append(e.name).append(" com formulario e tabela. */\n")
            .append("public final class Tela").append(e.name).append(" extends PainelCrud {\n")
            .append("    public Tela").append(e.name).append("() {\n")
            .append("        super(\n            ").append(q(e.formTitle)).append(",\n")
            .append("            new ").append(e.name).append("DAO(),\n")
            .append("            ").append(tableLabels(e)).append(",\n")
            .append("            ").append(formIndices(e)).append(",\n")
            .append("            ").append(formArray(e, 0)).append(",\n")
            .append("            criarCampos());\n    }\n\n")
            .append("    /** Componentes do formulario, na ordem dos campos da entidade. */\n")
            .append("    private static JComponent[] criarCampos() {\n")
            .append("        JComponent[] campos = new JComponent[").append(e.fields.size()).append("];\n");
        for (Spec.FormField u : e.formFields) {
            if (!u.visible) continue;
            Spec.Field f = FormDesign.find(e, u.field);
            int index = e.fields.indexOf(f);
            String suffix = upper(f.name);
            String var;
            if ("Relacao".equals(f.type)) {
                var = "cmb" + suffix;
                b.append("        JComboBox<Entidade> ").append(var).append(" = new JComboBox<Entidade>();\n");
            } else if ("Check".equals(u.widget)) {
                var = "chk" + suffix;
                b.append("        JCheckBox ").append(var).append(" = new JCheckBox(\"Sim\");\n");
            } else if ("Radio".equals(u.widget)) {
                var = "grp" + suffix;
                b.append("        RadioOptions ").append(var).append(" = new RadioOptions();\n");
                String options = "Booleano".equals(f.type) && u.options.trim().isEmpty() ? "true|false" : u.options;
                int n = 0;
                for (String option : options.split("\\|")) {
                    String radio = "rdo" + suffix + "_op" + (++n);
                    b.append("        JRadioButton ").append(radio).append(" = new JRadioButton(")
                        .append(q(option.trim())).append(");\n");
                    b.append("        ").append(var).append(".adicionar(").append(radio).append(");\n");
                }
            } else if ("Combo".equals(u.widget)) {
                var = "cmb" + suffix;
                b.append("        JComboBox<String> ").append(var).append(" = new JComboBox<String>();\n");
                if (!f.required) b.append("        ").append(var).append(".addItem(\"\");\n");
                String options = "Booleano".equals(f.type) && u.options.trim().isEmpty() ? "true|false" : u.options;
                for (String option : options.split("\\|"))
                    b.append("        ").append(var).append(".addItem(").append(q(option.trim())).append(");\n");
            } else {
                var = "txt" + suffix;
                b.append("        JTextField ").append(var).append(" = new JTextField(20);\n");
            }
            b.append("        campos[").append(index).append("] = ").append(var).append(";\n");
            b.append("\n");
        }
        return b.append("        return campos;\n    }\n}\n").toString();
    }
    public static String sql(Spec spec){
        StringBuilder b=new StringBuilder("-- Gerado offline pelo SwingForge. Execute no MySQL local.\nCREATE DATABASE IF NOT EXISTS `")
                .append(spec.database).append("` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;\nUSE `")
                .append(spec.database).append("`;\n\n");
        for(Spec.Entity e:spec.entities){
            b.append("CREATE TABLE IF NOT EXISTS `").append(Spec.table(e.name)).append("` (\n")
              .append("    `id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY");
            for(Spec.Field f:e.fields){b.append(",\n    `").append(f.name).append("` ")
                .append(sqlType(f.type)).append(f.required?" NOT NULL":" NULL");}
            b.append("\n) ENGINE=InnoDB;\n\n");
        }
        // CREATE TABLE first; relation constraints added afterwards so the order of entities does not matter.
        for(Spec.Entity e:spec.entities)for(Spec.Field f:e.fields)if("Relacao".equals(f.type)){
            String foreign=Spec.table(e.name)+"_"+f.name+"_fk";
            // Idempotent: check information_schema before adding a foreign key.
            b.append("SET @sf_fk_exists = (SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE ")
                .append("WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = '")
                .append(Spec.table(e.name)).append("' AND COLUMN_NAME = '").append(f.name)
                .append("' AND REFERENCED_TABLE_NAME = '").append(Spec.table(f.reference))
                .append("' AND REFERENCED_COLUMN_NAME = 'id');\n")
                .append("SET @sf_statement = IF(@sf_fk_exists = 0, 'ALTER TABLE `")
                .append(Spec.table(e.name)).append("` ADD CONSTRAINT `").append(foreign)
                .append("` FOREIGN KEY (`").append(f.name).append("`) REFERENCES `")
                .append(Spec.table(f.reference)).append("` (`id`) ON DELETE RESTRICT', 'SELECT 1');\n")
                .append("PREPARE sf_statement FROM @sf_statement;\nEXECUTE sf_statement;\nDEALLOCATE PREPARE sf_statement;\n\n");
        }
        return b.toString();
    }
    public static String buildXml(Spec spec){return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
        "<project name=\""+spec.name+"\" default=\"jar\" basedir=\".\">\n"+
        "  <property file=\"nbproject/project.properties\"/>\n"+
        "  <path id=\"jdbc\"><fileset dir=\"lib\" includes=\"*.jar\"/></path>\n"+
        "  <target name=\"clean\"><delete dir=\"build\"/><delete dir=\"dist\"/></target>\n"+
        "  <target name=\"compile\">\n    <mkdir dir=\"build/classes\"/>\n"+
        "    <javac srcdir=\"src\" destdir=\"build/classes\" encoding=\"UTF-8\" includeantruntime=\"false\" source=\"8\" target=\"8\">\n"+
        "      <classpath refid=\"jdbc\"/>\n    </javac>\n  </target>\n"+
        "  <target name=\"jar\" depends=\"compile\">\n    <mkdir dir=\"dist\"/>\n"+
        "    <jar destfile=\"dist/"+spec.name+".jar\" basedir=\"build/classes\">\n"+
        "      <manifest>\n"+
        "        <attribute name=\"Main-Class\" value=\"${main.class}\"/>\n"+
        "        <attribute name=\"Class-Path\" value=\"../lib/mysql-connector-j-8.1.0.jar\"/>\n"+
        "      </manifest>\n    </jar>\n  </target>\n"+
        "  <target name=\"run\" depends=\"compile\">\n"+
        "    <java classname=\"${main.class}\" fork=\"true\" failonerror=\"true\">\n"+
        "      <classpath><pathelement location=\"build/classes\"/><path refid=\"jdbc\"/></classpath>\n"+
        "    </java>\n  </target>\n</project>\n";
    }
    private static String loadTemplate(String name)throws IOException{
        String resource="/template/"+name;
        InputStream stream=Generator.class.getResourceAsStream(resource);
        if(stream==null)throw new IOException("Template indisponivel: "+resource);
        try(InputStream in=stream;ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);
            return new String(out.toByteArray(),StandardCharsets.UTF_8);
        }
    }
    private static String interpolar(String code,Spec s){return code.replace("__PACKAGE__",s.pkg)
        .replace("__APP_NAME__",s.name).replace("__DB_NAME__",s.database);}
    private static void addTemplate(Map<String,String> files,Spec s,String name,String output)throws IOException{
        files.put(output,interpolar(loadTemplate(name),s));
    }
    public static Map<String,String> files(Spec spec)throws IOException{
        Validator.validate(spec);
        Map<String,String> files=new LinkedHashMap<String,String>();
        String src="src/"+spec.pkg.replace('.','/')+"/";
        StringBuilder imports=new StringBuilder();
        for(Spec.Entity e:spec.entities)imports.append("import ").append(spec.pkg).append(".telas.Tela").append(e.name).append(";\n");
        files.put(src+"Main.java",interpolar(loadTemplate("Main.java"),spec).replace("__IMPORTS__",imports.toString()));
        for(String name:new String[] {"conexao/Conexao.java","dao/Registro.java","dao/Opcao.java","dao/CrudDAO.java",
            "telas/PainelCrud.java","conexao/PrepararBanco.java"})addTemplate(files,spec,name,src+name);
        StringBuilder tabs=new StringBuilder();
        files.put(src+"beans/Entidade.java", "package "+spec.pkg+".beans;\n\npublic interface Entidade {\n    Integer getId();\n}\n");
        for(Spec.Entity e:spec.entities){
            files.put(src+"beans/"+e.name+".java",bean(spec,e));
            files.put(src+"dao/"+e.name+"DAO.java",dao(spec,e));
            files.put(src+"telas/Tela"+e.name+".java",tela(spec,e));
            tabs.append("        adicionarSecao(").append(q(e.name)).append(",new Tela").append(e.name).append("());\n");
        }
        files.put(src+"Main.java",files.get(src+"Main.java").replace("__TABS__",tabs.toString()));
        files.put("sql/01_criar_banco.sql",sql(spec));
        files.put("criar_banco.bat", "@echo off\r\nsetlocal EnableExtensions EnableDelayedExpansion\r\n"+
            "cd /d \"%~dp0\"\r\n"+
            "set \"PAUSAR=1\"\r\nif /i \"%~1\"==\"--automatico\" set \"PAUSAR=0\"\r\n"+
            "if not exist \"sql\\01_criar_banco.sql\" (\r\n  echo [ERRO] Script SQL nao encontrado.\r\n  if \"!PAUSAR!\"==\"1\" pause\r\n  exit /b 1\r\n)\r\n"+
            "set \"MYSQL_CLIENT=\"\r\n"+
            "where mysql >nul 2>&1\r\nif not errorlevel 1 set \"MYSQL_CLIENT=mysql\"\r\n"+
            "if not defined MYSQL_CLIENT for /d %%D in (\"%ProgramFiles%\\MySQL\\MySQL Server *\") do if exist \"%%~D\\bin\\mysql.exe\" set \"MYSQL_CLIENT=%%~D\\bin\\mysql.exe\"\r\n"+
            "if not defined MYSQL_CLIENT (\r\n  echo [ERRO] Cliente mysql.exe nao encontrado. Instale o cliente MySQL ou adicione-o ao PATH.\r\n  if \"!PAUSAR!\"==\"1\" pause\r\n  exit /b 1\r\n)\r\n"+
            "echo Banco a preparar: "+spec.database+"\r\n"+
            "echo Informe os dados do servidor MySQL. Deixe em branco para usar os valores padrao.\r\n"+
            "set \"MYSQL_HOST=localhost\"\r\nset \"MYSQL_PORT=3306\"\r\nset \"MYSQL_USER=root\"\r\n"+
            "set /p \"MYSQL_HOST=Host [localhost]: \"\r\nset /p \"MYSQL_PORT=Porta [3306]: \"\r\nset /p \"MYSQL_USER=Usuario [root]: \"\r\n"+
            "echo A senha sera solicitada pelo cliente MySQL.\r\n"+
            "call \"!MYSQL_CLIENT!\" --default-character-set=utf8mb4 --host=\"!MYSQL_HOST!\" --port=\"!MYSQL_PORT!\" --user=\"!MYSQL_USER!\" --password < \"sql\\01_criar_banco.sql\"\r\n"+
            "if errorlevel 1 (\r\n  echo [ERRO] Nao foi possivel executar o SQL. Confira a conexao, permissoes e mensagens acima.\r\n  if \"!PAUSAR!\"==\"1\" pause\r\n  exit /b 1\r\n)\r\n"+
            "echo SQL executado. Estruturas que ja existiam nao foram validadas.\r\nif \"!PAUSAR!\"==\"1\" pause\r\nexit /b 0\r\n");
        files.put("nbproject/project.properties","application.title="+spec.name+"\napplication.vendor=SwingForge\n"+
            "src.dir=src\nbuild.dir=build\ndist.dir=dist\nmain.class="+spec.pkg+".Main\n"+
            "javac.source=8\njavac.target=8\nsource.encoding=UTF-8\n");
        files.put("nbproject/project.xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
            "<project xmlns=\"http://www.netbeans.org/ns/project/1\"><type>org.netbeans.modules.java.j2seproject</type>"+
            "<configuration><data xmlns=\"http://www.netbeans.org/ns/j2se-project/3\"><name>"+spec.name+"</name>"+
            "<source-roots><root id=\"src.dir\"/></source-roots><test-roots/></data></configuration></project>\n");
        files.put("build.xml",buildXml(spec));
        files.put(".gitignore","build/\ndist/\nnbproject/private/\n.swinforge/\n.swingforge/\n"+
           "test/\ntests/\n*.log\n.env\n.env.*\nbanco.properties\n.idea/\n.vscode/\n");
        String mainSource="src/"+spec.pkg.replace('.', '/')+"/Main.java";
        files.put("run.bat", "@echo off\r\nsetlocal EnableExtensions\r\ncd /d \"%~dp0\"\r\n"+
            "if not exist \"lib\\mysql-connector-j-8.1.0.jar\" (echo [ERRO] Driver MySQL ausente em lib\\. & pause & exit /b 1)\r\n"+
            "if exist \"tools\\ant\\bin\\ant.bat\" (call \"tools\\ant\\bin\\ant.bat\" run & goto :end)\r\n"+
            "where ant >nul 2>&1\r\nif not errorlevel 1 (call ant run & goto :end)\r\n"+
            "where javac >nul 2>&1\r\nif errorlevel 1 (echo [ERRO] JDK/javac nao encontrado. Instale um JDK offline. & pause & exit /b 1)\r\n"+
            "if not exist \"build\\classes\" mkdir \"build\\classes\"\r\n"+
            "javac -encoding UTF-8 -source 8 -target 8 -cp \"lib\\*\" -sourcepath \"src\" -d \"build\\classes\" \""+mainSource.replace('/', '\\')+"\"\r\n"+
            "if errorlevel 1 goto :end\r\n"+
            "java -cp \"build\\classes;lib\\*\" "+spec.pkg+".Main\r\n"+
            ":end\r\nif errorlevel 1 pause\r\n");
        files.put("run.sh", "#!/bin/sh\nset -eu\ncd \"$(dirname \"$0\")\"\n"+
            "if [ ! -f lib/mysql-connector-j-8.1.0.jar ]; then echo '[ERRO] Driver MySQL ausente em lib/' >&2; exit 1; fi\n"+
            "if [ -x tools/ant/bin/ant ]; then exec tools/ant/bin/ant run; fi\n"+
            "if command -v ant >/dev/null 2>&1; then exec ant run; fi\n"+
            "if ! command -v javac >/dev/null 2>&1; then echo '[ERRO] JDK/javac ausente.' >&2; exit 1; fi\n"+
            "mkdir -p build/classes\n"+
            "javac -encoding UTF-8 -source 8 -target 8 -cp 'lib/*' -sourcepath src -d build/classes '"+mainSource+"'\n"+
            "exec java -cp 'build/classes:lib/*' "+spec.pkg+".Main\n");
        return files;
    }
    public static int generate(Spec s,Path root)throws IOException{
        Map<String,String> generated=files(s);
        root=root.toAbsolutePath().normalize();
        if(!Files.exists(root))Files.createDirectories(root);
        if(!Files.isDirectory(root))throw new IOException("Destino precisa ser uma pasta.");
        Path metadata=ProjectStore.metadataDir(root);
        Files.createDirectories(metadata);
        String stamp=LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
        Path backups=metadata.resolve("backups").resolve(stamp);
        Path previousList=metadata.resolve("generated-files.txt");
        Path oldMetadata=root.resolve(".swingforge");
        if(Files.isDirectory(oldMetadata)){
            Path migrated=metadata.resolve("legado-"+stamp);
            Files.move(oldMetadata,migrated);
            if(!Files.exists(previousList) && Files.isRegularFile(migrated.resolve("generated-files.txt")))
                Files.copy(migrated.resolve("generated-files.txt"),previousList);
        }
        Path oldModel=root.resolve("swingforge.json");
        if(Files.isRegularFile(oldModel)){
            ProjectStore.read(oldModel);
            Files.createDirectories(backups);
            Files.move(oldModel,backups.resolve("swingforge.json"));
        }
        if(Files.isRegularFile(previousList)){
            for(String line:Files.readAllLines(previousList,StandardCharsets.UTF_8)){
                String pathName=line.trim();
                if(pathName.isEmpty()||generated.containsKey(pathName)||pathName.equals("lib/"+MysqlDriver.NAME))continue;
                Path old=root.resolve(pathName).normalize();
                if(!old.startsWith(root)||!Files.isRegularFile(old))continue;
                Path backup=backups.resolve(pathName);Files.createDirectories(backup.getParent());
                Files.copy(old,backup,StandardCopyOption.REPLACE_EXISTING);
                Files.delete(old);
            }
        }
        Path configLocal=root.resolve("banco.properties");
        if(!Files.exists(configLocal)){
            Files.write(configLocal,("# Configuracao local. Informe a senha do MySQL; nao compartilhe este arquivo.\n"+
                "host=localhost\nporta=3306\nusuario=root\nsenha=\n").getBytes(StandardCharsets.UTF_8));
        }
        int count=0;
        for(Map.Entry<String,String> entry:generated.entrySet()){
            Path path=root.resolve(entry.getKey()).normalize();
            if(!path.startsWith(root))throw new IOException("Caminho fora do projeto.");
            Files.createDirectories(path.getParent());
            if(Files.isRegularFile(path)){
                byte[] existing=Files.readAllBytes(path);
                byte[] replacement=entry.getValue().getBytes(StandardCharsets.UTF_8);
                if(java.util.Arrays.equals(existing,replacement))continue;
                Path previous=backups.resolve(entry.getKey());Files.createDirectories(previous.getParent());
                Files.copy(path,previous,StandardCopyOption.REPLACE_EXISTING);
            }
            Files.write(path,entry.getValue().getBytes(StandardCharsets.UTF_8));count++;
        }
        // O JDBC e um binario: copiar o arquivo oficial em vez de gerar um JAR vazio.
        boolean jdbc = MysqlDriver.copyTo(root, backups);
        java.util.List<String> tracked = new java.util.ArrayList<String>(generated.keySet());
        if (jdbc) tracked.add("lib/" + MysqlDriver.NAME);
        Files.createDirectories(previousList.getParent());
        Files.write(previousList,tracked,StandardCharsets.UTF_8);
        ProjectStore.writeProject(root,s);
        ProjectStore.write(ProjectStore.generatedModelPath(root),s);
        return count + (jdbc ? 1 : 0);
    }
}