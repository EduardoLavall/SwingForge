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
        root.put("schema", 2); root.put("generator", "SwingForge");
        root.put("name",s.name); root.put("pkg",s.pkg); root.put("database",s.database);
        Map<String,Object> forms=new LinkedHashMap<String,Object>();
        for(Spec.Entity e:s.entities){FormDesign.normalize(e);
            Map<String,Object> frm=new LinkedHashMap<String,Object>();frm.put("title",e.formTitle);
            List<Object> rows=new ArrayList<Object>();
            for(Spec.FormField u:e.formFields){Map<String,Object> row=new LinkedHashMap<String,Object>();
                row.put("field",u.field);row.put("label",u.label);row.put("widget",u.widget);
                row.put("options",u.options);row.put("visible",u.visible);rows.add(row);}
            frm.put("fields",rows);forms.put(e.name,frm);
        }
        root.put("forms",forms);
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
        if(!(m.get("schema") instanceof Number) || (((Number)m.get("schema")).intValue()!=1 && ((Number)m.get("schema")).intValue()!=2))throw new IllegalArgumentException("Versao do modelo nao suportada.");
        s.name=(String)m.get("name");s.pkg=(String)m.get("pkg");s.database=(String)m.get("database");
        for(Object item:(List<Object>)m.get("entities")){Map<String,Object> v=(Map<String,Object>)item;Spec.Entity e=new Spec.Entity((String)v.get("name"));
            for(Object f:(List<Object>)v.get("fields")){Map<String,Object> w=(Map<String,Object>)f;
                e.fields.add(new Spec.Field((String)w.get("name"),(String)w.get("type"),(String)w.get("reference"),Boolean.TRUE.equals(w.get("required"))));}
            s.entities.add(e);}
        Object rawForms=m.get("forms");
        if(rawForms instanceof Map){Map<String,Object> forms=(Map<String,Object>)rawForms;
            for(Spec.Entity e:s.entities){Object formValue=forms.get(e.name);
                if(!(formValue instanceof Map))continue;
                Map<String,Object> f=(Map<String,Object>)formValue;
                if(f.get("title") instanceof String)e.formTitle=(String)f.get("title");
                Object rawFields=f.get("fields");if(!(rawFields instanceof List))continue;
                for(Object rawField:(List<Object>)rawFields){if(!(rawField instanceof Map))continue;
                    Map<String,Object> u=(Map<String,Object>)rawField;
                    if(!(u.get("field") instanceof String))continue;
                    e.formFields.add(new Spec.FormField((String)u.get("field"),
                        u.get("label") instanceof String?(String)u.get("label"):"",
                        u.get("widget") instanceof String?(String)u.get("widget"):"Texto",
                        u.get("options") instanceof String?(String)u.get("options"):"",
                        !Boolean.FALSE.equals(u.get("visible"))));
                }
            }
        }
        for(Spec.Entity e:s.entities)FormDesign.normalize(e);
        Validator.validate(s);return s;
    }
    public static Spec read(Path p) throws IOException {return deserialize(new String(Files.readAllBytes(p), StandardCharsets.UTF_8));}
    public static void write(Path p, Spec spec) throws IOException {
        Validator.validate(spec);Files.write(p,serialize(spec).getBytes(StandardCharsets.UTF_8));
    }
}
