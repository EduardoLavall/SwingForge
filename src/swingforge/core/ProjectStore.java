package swingforge.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ProjectStore {
    private ProjectStore() { }
    public static String serialize(Spec s) {
        Map<String,Object> root = new LinkedHashMap<String,Object>();
        root.put("schema", 1); root.put("generator", "SwingForge");
        root.put("name",s.name); root.put("pkg",s.pkg); root.put("database",s.database);
        // Reservado para a etapa 2: geometria dos formularios e .form do NetBeans.
        root.put("forms",new LinkedHashMap<String,Object>());
        List<Object> entities=new ArrayList<Object>();
        for(Spec.Entity e:s.entities){Map<String,Object> o=new LinkedHashMap<String,Object>();o.put("name",e.name);
            List<Object> fs=new ArrayList<Object>();for(Spec.Field f:e.fields){Map<String,Object> fm=new LinkedHashMap<String,Object>();
                fm.put("name",f.name);fm.put("type",f.type);fm.put("reference",f.reference==null?"":f.reference);fm.put("required",f.required);fs.add(fm);}
            o.put("fields",fs);entities.add(o);}
        root.put("entities",entities);return Json.write(root)+"\n";
    }
    @SuppressWarnings("unchecked")
    public static Spec deserialize(String content) {
        Object raw=Json.parse(content);if(!(raw instanceof Map))throw new IllegalArgumentException("Modelo JSON invalido.");
        Map<String,Object> m=(Map<String,Object>)raw;Spec s=new Spec();
        if(!"SwingForge".equals(m.get("generator")))throw new IllegalArgumentException("Projeto nao foi criado pelo SwingForge.");
        if(!(m.get("schema") instanceof Number) || ((Number)m.get("schema")).intValue()!=1)throw new IllegalArgumentException("Versao do modelo nao suportada.");
        s.name=(String)m.get("name");s.pkg=(String)m.get("pkg");s.database=(String)m.get("database");
        for(Object item:(List<Object>)m.get("entities")){Map<String,Object> v=(Map<String,Object>)item;Spec.Entity e=new Spec.Entity((String)v.get("name"));
            for(Object f:(List<Object>)v.get("fields")){Map<String,Object> w=(Map<String,Object>)f;
                e.fields.add(new Spec.Field((String)w.get("name"),(String)w.get("type"),(String)w.get("reference"),Boolean.TRUE.equals(w.get("required"))));}
            s.entities.add(e);}
        Validator.validate(s);return s;
    }
    public static Spec read(Path p) throws IOException {return deserialize(new String(Files.readAllBytes(p), StandardCharsets.UTF_8));}
    public static void write(Path p, Spec spec) throws IOException {
        Validator.validate(spec);Files.write(p,serialize(spec).getBytes(StandardCharsets.UTF_8));
    }
}
