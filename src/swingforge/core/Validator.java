package swingforge.core;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.Arrays;

public final class Validator {
    private Validator() { }
    private static final Set<String> RESERVED = new HashSet<String>(Arrays.asList(
        "class","interface","enum","public","private","protected","static","final","return","for","while",
        "switch","case","default","package","import","null","true","false","new","extends","implements",
        "void","int","double","float","short","byte","boolean","char","long","this","super","abstract",
        "if","else","try","catch","throw","throws","do","break","continue","instanceof","transient",
        "volatile","native","synchronized","strictfp","assert","goto","const","_"
    ));
    private static void identifier(String s, String where) {
        if (s == null || !s.matches("[A-Za-z][A-Za-z0-9_]*") || RESERVED.contains(s.toLowerCase(Locale.ROOT)))
            throw new IllegalArgumentException(where + ": use letras ASCII, numeros e _; comece com letra. Valor: " + s);
    }
    public static void validate(Spec s) {
        identifier(s.name, "Nome do projeto");
        if (s.pkg == null || !s.pkg.matches("[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*"))
            throw new IllegalArgumentException("Pacote deve ser lowercase (ex.: meuapp ou br.exemplo.loja).");
        for (String part : s.pkg.split("\\.")) identifier(part, "Pacote");
        identifier(s.database, "Banco MySQL");
        if (s.entities.isEmpty()) throw new IllegalArgumentException("Adicione ao menos uma entidade.");
        Set<String> classes = new HashSet<String>();
        Set<String> generatedClasses = new HashSet<String>(Arrays.asList(
            "main", "conexao", "registro", "opcao", "cruddao", "tema", "painelcrud", "telaprincipal"));
        Set<String> tables = new HashSet<String>();
        for (Spec.Entity e : s.entities) {
            identifier(e.name, "Entidade");
            if (!Character.isUpperCase(e.name.charAt(0))) throw new IllegalArgumentException("Classe deve iniciar com maiuscula: " + e.name);
            for (String generated : Arrays.asList(e.name, e.name + "DAO", "Tela" + e.name)) {
                if (!generatedClasses.add(generated.toLowerCase(Locale.ROOT)))
                    throw new IllegalArgumentException("Nome de entidade gera classe duplicada: " + e.name);
            }
            if (!classes.add(e.name.toLowerCase(Locale.ROOT)) || !tables.add(Spec.table(e.name)))
                throw new IllegalArgumentException("Entidade duplicada: " + e.name);
            if (e.fields.isEmpty()) throw new IllegalArgumentException("A entidade " + e.name + " precisa de campos alem do ID.");
            Set<String> fields = new HashSet<String>();
            for (Spec.Field f : e.fields) {
                identifier(f.name, "Campo de " + e.name);
                if ("id".equalsIgnoreCase(f.name) || !fields.add(f.name.toLowerCase(Locale.ROOT)))
                    throw new IllegalArgumentException("Campo reservado ou duplicado em " + e.name + ": " + f.name);
                if (!Arrays.asList("Texto","Inteiro","Decimal","Data","Booleano","Relacao").contains(f.type))
                    throw new IllegalArgumentException("Tipo desconhecido em " + f.name);
                if ("Relacao".equals(f.type) && (f.reference == null || f.reference.trim().isEmpty()))
                    throw new IllegalArgumentException("Defina a entidade vinculada em " + e.name + "." + f.name);
            }
        }
        for (Spec.Entity e : s.entities) FormDesign.validate(e);
        for (Spec.Entity e : s.entities) for (Spec.Field f : e.fields) if ("Relacao".equals(f.type)) {
            boolean exists = false;
            for (Spec.Entity target : s.entities) if (target.name.equals(f.reference)) exists = true;
            if (!exists) throw new IllegalArgumentException("Entidade relacionada nao encontrada: " + f.reference);
        }
    }
}
