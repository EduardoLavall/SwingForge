package swingforge.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Parser/serializador JSON local pequeno, sem bibliotecas e sem chamadas de rede. */
public final class Json {
    private Json() { }
    public static String write(Object v) {
        if (v == null) return "null";
        if (v instanceof Boolean || v instanceof Number) return v.toString();
        if (v instanceof String) {
            StringBuilder b = new StringBuilder("\"");
            for (char c : ((String)v).toCharArray()) {
                if (c == '"') b.append("\\\""); else if (c == '\\') b.append("\\\\");
                else if (c == '\n') b.append("\\n"); else if (c == '\r') b.append("\\r");
                else if (c == '\t') b.append("\\t"); else if (c < 32) b.append(String.format("\\u%04x",(int)c));
                else b.append(c);
            }
            return b.append('"').toString();
        }
        if (v instanceof Map) {
            StringBuilder b = new StringBuilder("{");
            for (Object key : ((Map<?, ?>)v).keySet()) {
                if (b.length() > 1) b.append(',');
                b.append(write(key.toString())).append(':').append(write(((Map<?, ?>)v).get(key)));
            }
            return b.append('}').toString();
        }
        if (v instanceof Iterable) {
            StringBuilder b = new StringBuilder("[");
            for (Object item : (Iterable<?>)v) {
                if (b.length() > 1) b.append(',');
                b.append(write(item));
            }
            return b.append(']').toString();
        }
        throw new IllegalArgumentException("Tipo JSON nao suportado: " + v.getClass());
    }
    public static Object parse(String json) { Parser p=new Parser(json); Object o=p.val();p.ws();if(p.i!=json.length())throw p.error();return o; }
    private static final class Parser {
        final String s; int i;
        Parser(String s){this.s=s;}
        RuntimeException error(){return new IllegalArgumentException("JSON invalido na posicao " + i);}
        void ws(){while(i<s.length() && Character.isWhitespace(s.charAt(i)))i++;}
        char next(){if(i>=s.length())throw error();return s.charAt(i++);}
        void expect(char c){ws();if(next()!=c)throw error();}
        Object val(){ws();if(i>=s.length())throw error();char c=s.charAt(i);
            if(c=='"')return string();if(c=='{')return object();if(c=='[')return array();
            if(s.startsWith("true",i)){i+=4;return Boolean.TRUE;}
            if(s.startsWith("false",i)){i+=5;return Boolean.FALSE;}
            if(s.startsWith("null",i)){i+=4;return null;}
            int start=i; while(i<s.length()&&"-+0123456789.eE".indexOf(s.charAt(i))>=0)i++;
            if(i==start)throw error();try{return Double.valueOf(s.substring(start,i));}catch(NumberFormatException ex){throw error();}
        }
        String string(){expect('"');StringBuilder b=new StringBuilder();while(true){char c=next();if(c=='"')return b.toString();
            if(c=='\\'){char x=next();switch(x){case '"':b.append('"');break;case '\\':b.append('\\');break;
                case '/':b.append('/');break;case 'n':b.append('\n');break;case 'r':b.append('\r');break;case 't':b.append('\t');break;
                case 'b':b.append('\b');break;case 'f':b.append('\f');break;case 'u':
                    if(i+4>s.length())throw error();try{b.append((char)Integer.parseInt(s.substring(i,i+4),16));}catch(Exception ex){throw error();}i+=4;break;
                default:throw error();}} else b.append(c);
        }}
        Map<String,Object> object(){expect('{');Map<String,Object> m=new LinkedHashMap<String,Object>();ws();if(i<s.length()&&s.charAt(i)=='}'){i++;return m;}
            while(true){String k=string();expect(':');m.put(k,val());ws();char end=next();if(end=='}')return m;if(end!=',')throw error();ws();}}
        List<Object> array(){expect('[');List<Object> out=new ArrayList<Object>();ws();if(i<s.length()&&s.charAt(i)==']'){i++;return out;}
            while(true){out.add(val());ws();char end=next();if(end==']')return out;if(end!=',')throw error();}}
    }
}
