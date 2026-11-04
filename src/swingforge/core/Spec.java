package swingforge.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Modelo editavel que funciona sem NetBeans e sem conexao com banco. */
public final class Spec {
    public String name = "ControleEstoque";
    public String pkg = "controleestoque";
    public String database = "controle_estoque";
    public final List<Entity> entities = new ArrayList<Entity>();

    public static final class Entity {
        public String name;
        public final List<Field> fields = new ArrayList<Field>();
        public String formTitle = "";
        public final List<FormField> formFields = new ArrayList<FormField>();
        public Entity(String name) { this.name = name; }
        @Override public String toString() { return name; }
    }
    public static final class Field {
        public String name;
        public String type;
        public String reference;
        public boolean required;
        public Field(String name, String type, String reference, boolean required) {
            this.name = name; this.type = type; this.reference = reference; this.required = required;
        }
    }

    public static final class FormField {
        public String field, label, widget, options;
        public boolean visible;
        public FormField(String field,String label,String widget,String options,boolean visible) {
            this.field=field;this.label=label;this.widget=widget;this.options=options;this.visible=visible;
        }
    }
    /** Preset de Eventos com campos de formulario configuraveis. */
    public static Spec example() {
        Spec s=new Spec();s.name="CadastroEvento";s.pkg="cadastroevento";s.database="cadastro_evento";
        Entity e=new Entity("Evento");
        e.fields.add(new Field("nome","Texto","",true));
        e.fields.add(new Field("dataEvento","Texto","",true));
        e.fields.add(new Field("situacao","Texto","",true));
        e.fields.add(new Field("local","Texto","",true));
        e.fields.add(new Field("modalidade","Texto","",true));
        e.fields.add(new Field("tipo","Texto","",true));
        e.formTitle="Dados do Evento";
        e.formFields.add(new FormField("nome","Nome","Texto","",true));
        e.formFields.add(new FormField("dataEvento","DataEvento DD/MM/AAAA","Texto","",true));
        e.formFields.add(new FormField("situacao","Situacao","Radio","Agendado|Realizado|Cancelado",true));
        e.formFields.add(new FormField("local","Local","Texto","",true));
        e.formFields.add(new FormField("modalidade","Modalidade","Radio","Presencial|Online",true));
        e.formFields.add(new FormField("tipo","Tipo","Combo","Palestra|Workshop|Curso|Conferencia",true));
        s.entities.add(e);return s;
    }
    public static Spec estoqueExample() {
        Spec s=new Spec();
        Entity c=new Entity("Categoria");c.fields.add(new Field("nome","Texto","",true));
        Entity p=new Entity("Produto");
        p.fields.add(new Field("nome","Texto","",true));
        p.fields.add(new Field("preco","Decimal","",true));
        p.fields.add(new Field("quantidade","Inteiro","",true));
        p.fields.add(new Field("categoria_id","Relacao","Categoria",true));
        s.entities.add(c);s.entities.add(p);return s;
    }
    public static String table(String className) {
        String s = className.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
        return s.endsWith("s") ? s : s + "s";
    }
    public static String javaClass(String name) { return name; }
    public static String label(Entity e) {
        for (Field f : e.fields) if ("Texto".equals(f.type) && "nome".equalsIgnoreCase(f.name)) return f.name;
        for (Field f : e.fields) if ("Texto".equals(f.type)) return f.name;
        return "id";
    }
}
