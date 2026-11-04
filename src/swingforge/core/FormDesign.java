package swingforge.core;
import java.util.*;
/** Configuracao do formulario mantida em swingforge.json. */
public final class FormDesign {
    private FormDesign() { }
    public static String label(String name) {
        String x=name.replaceAll("([a-z])([A-Z])","$1 $2").replace('_',' ');
        return x.length()==0?"Campo":Character.toUpperCase(x.charAt(0))+x.substring(1);
    }
    public static String defaultWidget(Spec.Field f) {
        return "Relacao".equals(f.type)?"Combo":"Booleano".equals(f.type)?"Check":"Texto";
    }
    public static Spec.Field find(Spec.Entity e,String name) {
        for(Spec.Field f:e.fields)if(f.name.equals(name))return f;
        throw new IllegalArgumentException("Campo nao encontrado: "+name);
    }
    public static void normalize(Spec.Entity e) {
        if(e.formTitle==null||e.formTitle.trim().isEmpty())e.formTitle="Dados de "+e.name;
        Set<String> allowed=new HashSet<String>();for(Spec.Field f:e.fields)allowed.add(f.name);
        Set<String> used=new HashSet<String>();List<Spec.FormField> out=new ArrayList<Spec.FormField>();
        for(Spec.FormField u:e.formFields)if(u!=null && allowed.contains(u.field) && used.add(u.field)){
            if(u.label==null||u.label.trim().isEmpty())u.label=label(u.field);
            if(u.widget==null)u.widget="Texto";
            if(u.options==null)u.options="";
            out.add(u);
        }
        for(Spec.Field f:e.fields)if(used.add(f.name))
            out.add(new Spec.FormField(f.name,label(f.name),defaultWidget(f),"",true));
        e.formFields.clear();e.formFields.addAll(out);
        for(Spec.FormField u:e.formFields){
            Spec.Field f=find(e,u.field);
            if("Relacao".equals(f.type))u.widget="Combo";
            else if("Booleano".equals(f.type))u.widget="Check";
            else if(!"Texto".equals(f.type) || "Check".equals(u.widget))u.widget="Texto";
            if(!Arrays.asList("Texto","Radio","Combo","Check").contains(u.widget))u.widget=defaultWidget(f);
        }
    }
    public static void validate(Spec.Entity e){normalize(e);
        for(Spec.FormField u:e.formFields){
            if(!u.visible && find(e,u.field).required)
                throw new IllegalArgumentException("Campo obrigatorio nao pode ficar oculto: "+u.field);
            if(u.label.length()>120)throw new IllegalArgumentException("Rotulo muito longo: "+u.field);
            if(("Radio".equals(u.widget)||"Combo".equals(u.widget))
                && "Texto".equals(find(e,u.field).type)&&u.options.trim().isEmpty())
                throw new IllegalArgumentException("Configure opcoes separadas por | para "+u.field);
        }
    }
}
