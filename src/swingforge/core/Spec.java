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

    public static Spec example() {
        Spec s = new Spec();
        Entity c = new Entity("Categoria");
        c.fields.add(new Field("nome", "Texto", "", true));
        Entity p = new Entity("Produto");
        p.fields.add(new Field("nome", "Texto", "", true));
        p.fields.add(new Field("preco", "Decimal", "", true));
        p.fields.add(new Field("quantidade", "Inteiro", "", true));
        p.fields.add(new Field("categoria_id", "Relacao", "Categoria", true));
        s.entities.add(c); s.entities.add(p);
        return s;
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
