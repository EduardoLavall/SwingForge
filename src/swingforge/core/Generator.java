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

/** Gera pasta Ant/NetBeans offline: src, beans, dao, conexao, telas e SQL. */
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
    private static String sqlType(String type){
        if("Inteiro".equals(type)||"Relacao".equals(type))return "INT";
        if("Decimal".equals(type))return "DECIMAL(12,2)";
        if("Data".equals(type))return "DATE";
        if("Booleano".equals(type))return "BOOLEAN";
        return "VARCHAR(255)";
    }
    private static String title(String field) {String x=field.replace('_',' ');return Character.toUpperCase(x.charAt(0))+x.substring(1);}
    private static String arr(String... xs){StringBuilder b=new StringBuilder("new String[]{");for(String x:xs){if(b.length()>13)b.append(',');b.append(q(x));}return b.append('}').toString();}
    private static String arrFields(Spec.Entity e,int variant,Spec spec){
        StringBuilder b=new StringBuilder("new String[]{");
        for(Spec.Field f:e.fields){if(b.charAt(b.length()-1)!='{')b.append(',');
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
    private static String bools(Spec.Entity e){StringBuilder b=new StringBuilder("new boolean[]{");for(Spec.Field f:e.fields){if(b.charAt(b.length()-1)!='{')b.append(',');b.append(f.required);}return b.append('}').toString();}
    public static String bean(Spec spec,Spec.Entity e){
        StringBuilder b=new StringBuilder("package ").append(spec.pkg).append(".beans;\n\n");
        b.append("/** Modelo de dados da entidade ").append(e.name).append(". */\npublic class ").append(e.name).append(" {\n");
        b.append("    private Integer id;\n");
        for(Spec.Field f:e.fields)b.append("    private ").append(javaType(f.type)).append(' ').append(f.name).append(";\n");
        b.append("\n    public ").append(e.name).append("() {}\n");
        b.append("    public Integer getId(){return id;}\n    public void setId(Integer id){this.id=id;}\n");
        for(Spec.Field f:e.fields){String title=upper(f.name);String t=javaType(f.type);
            b.append("    public ").append(t).append(" get").append(title).append("(){return ").append(f.name).append(";}\n");
            b.append("    public void set").append(title).append('(').append(t).append(' ').append(f.name).append("){this.")
             .append(f.name).append('=').append(f.name).append(";}\n");
        }
        return b.append("}\n").toString();
    }
    public static String dao(Spec spec,Spec.Entity e){
        return "package "+spec.pkg+".dao;\n\n/** CRUD SQL da entidade "+e.name+". */\n"+
                "public final class "+e.name+"DAO extends CrudDAO {\n"+
                "    public "+e.name+"DAO(){\n        super("+q(Spec.table(e.name))+",\n            "+
                arrFields(e,0,spec)+",\n            "+arrFields(e,1,spec)+",\n            "+
                arrFields(e,2,spec)+",\n            "+arrFields(e,3,spec)+",\n            "+bools(e)+");\n    }\n}\n";
    }
    /** Ordenacao e rotulos independentes dos campos SQL, configurados no GUI Editor. */
    private static String arrayOf(List<String> values){
        StringBuilder b=new StringBuilder("new String[]{");
        for(String x:values){if(b.charAt(b.length()-1)!='{')b.append(',');b.append(q(x));}
        return b.append('}').toString();
    }
    private static String formIndices(Spec.Entity e){
        FormDesign.normalize(e);StringBuilder b=new StringBuilder("new int[]{");
        for(Spec.FormField u:e.formFields)if(u.visible){
            if(b.charAt(b.length()-1)!='{')b.append(',');
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
    public static String tela(Spec spec,Spec.Entity e){
        return "package "+spec.pkg+".telas;\n\nimport "+spec.pkg+".dao."+e.name+"DAO;\n\n"+
            "/** Formulario Swing gerado pelo GUI Builder do SwingForge. */\n"+
            "public final class Tela"+e.name+" extends PainelCrud {\n"+
            "    public Tela"+e.name+"(){super("+q(e.formTitle)+",new "+e.name+"DAO(),"+
               tableLabels(e)+","+formIndices(e)+","+formArray(e,0)+","+
               formArray(e,1)+","+formArray(e,2)+");}\n}\n";
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
        addTemplate(files,spec,"Main.java",src+"Main.java");
        for(String name:new String[]{"conexao/Conexao.java","dao/Registro.java","dao/Opcao.java","dao/CrudDAO.java",
            "telas/Tema.java","telas/PainelCrud.java"})addTemplate(files,spec,name,src+name);
        StringBuilder tabs=new StringBuilder();
        for(Spec.Entity e:spec.entities){
            files.put(src+"beans/"+e.name+".java",bean(spec,e));
            files.put(src+"dao/"+e.name+"DAO.java",dao(spec,e));
            files.put(src+"telas/Tela"+e.name+".java",tela(spec,e));
            tabs.append("        adicionarSecao(").append(q(e.name)).append(",new Tela").append(e.name).append("());\n");
        }
        files.put(src+"telas/TelaPrincipal.java",interpolar(loadTemplate("telas/TelaPrincipal.java"),spec)
            .replace("__TABS__",tabs.toString()));
        files.put("sql/01_criar_banco.sql",sql(spec));
        files.put("nbproject/project.properties","application.title="+spec.name+"\napplication.vendor=SwingForge\n"+
            "src.dir=src\nbuild.dir=build\ndist.dir=dist\nmain.class="+spec.pkg+".Main\n"+
            "javac.source=8\njavac.target=8\nsource.encoding=UTF-8\n");
        files.put("nbproject/project.xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
            "<project xmlns=\"http://www.netbeans.org/ns/project/1\"><type>org.netbeans.modules.java.j2seproject</type>"+
            "<configuration><data xmlns=\"http://www.netbeans.org/ns/j2se-project/3\"><name>"+spec.name+"</name>"+
            "<source-roots><root id=\"src.dir\"/></source-roots><test-roots/></data></configuration></project>\n");
        files.put("build.xml",buildXml(spec));
        files.put(".gitignore","build/\ndist/\nnbproject/private/\n.swinforge/\n.swingforge/\n"+
           "test/\ntests/\n*.log\n.env\n.env.*\n.idea/\n.vscode/\n");
        files.put("lib/DRIVER_MYSQL.txt","O projeto usa MySQL Connector/J 8.1.0 (arquivo fornecido pelo professor).\n"+
            "O SwingForge copia o JAR original para esta pasta quando ele estiver disponivel.\n"+
            "Se lib/mysql-connector-j-8.1.0.jar estiver ausente, importe o ZIP no SwingForge.\n");
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
        files.put("swingforge.json",ProjectStore.serialize(spec));
        return files;
    }
    public static int generate(Spec s,Path root)throws IOException{
        Map<String,String> generated=files(s);
        root=root.toAbsolutePath().normalize();
        if(!Files.exists(root))Files.createDirectories(root);
        if(!Files.isDirectory(root))throw new IOException("Destino precisa ser uma pasta.");
        Path backups=root.resolve(".swingforge/backups/"+LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS")));
        Path previousList=root.resolve(".swingforge/generated-files.txt");
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
        Path licenseFile = root.resolve("lib/LICENSE-MySQL-Connector-J.txt");
        Path driverLocation = MysqlDriver.find();
        if (jdbc && driverLocation != null) {
            Path sourceLicense = driverLocation.getParent().resolve("LICENSE-MySQL-Connector-J.txt");
            if (Files.isRegularFile(sourceLicense))
                Files.copy(sourceLicense,licenseFile,StandardCopyOption.REPLACE_EXISTING);
        }
        java.util.List<String> tracked = new java.util.ArrayList<String>(generated.keySet());
        if (jdbc) tracked.add("lib/" + MysqlDriver.NAME);
        if (Files.isRegularFile(licenseFile)) tracked.add("lib/LICENSE-MySQL-Connector-J.txt");
        Files.createDirectories(previousList.getParent());
        Files.write(previousList,tracked,StandardCharsets.UTF_8);
        return count + (jdbc ? 1 : 0);
    }
}