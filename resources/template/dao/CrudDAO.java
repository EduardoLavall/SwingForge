package __PACKAGE__.dao;

import __PACKAGE__.conexao.Conexao;
import java.sql.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** SQL parametrizado para valores; identificadores provem apenas do gerador. */
public class CrudDAO {
    public final String tabela;
    public final String[] campos, tipos, tabelasRelacionadas, rotulosRelacionados;
    public final boolean[] obrigatorios;

    protected CrudDAO(String tabela,String[] campos,String[] tipos,String[] refs,String[] rotulos,boolean[] obrigatorios){
        this.tabela=tabela;this.campos=campos;this.tipos=tipos;this.tabelasRelacionadas=refs;
        this.rotulosRelacionados=rotulos;this.obrigatorios=obrigatorios;
    }
    public List<Registro> listar(String busca) throws SQLException {
        List<Registro> out=new ArrayList<Registro>();
        StringBuilder sql=new StringBuilder("SELECT t.id");
        StringBuilder joins=new StringBuilder();StringBuilder filtros=new StringBuilder();
        for(int i=0;i<campos.length;i++){
            sql.append(",t.").append(campos[i]);
            if("Relacao".equals(tipos[i])){
                sql.append(",r").append(i).append('.').append(rotulosRelacionados[i]).append(" AS __rotulo_").append(i);
                joins.append(" LEFT JOIN ").append(tabelasRelacionadas[i]).append(" r").append(i)
                     .append(" ON r").append(i).append(".id=t.").append(campos[i]);
                if(filtros.length()>0)filtros.append(" OR ");
                filtros.append("LOWER(COALESCE(r").append(i).append('.').append(rotulosRelacionados[i])
                    .append(",'')) LIKE ?");
            } else if("Texto".equals(tipos[i])){
                if(filtros.length()>0)filtros.append(" OR ");
                filtros.append("LOWER(t.").append(campos[i]).append(") LIKE ?");
            }
        }
        sql.append(" FROM ").append(tabela).append(" t").append(joins);
        boolean search=busca!=null&&!busca.trim().isEmpty()&&filtros.length()>0;
        if(search)sql.append(" WHERE ").append(filtros);
        sql.append(" ORDER BY t.id DESC");
        try(Connection cn=Conexao.abrir();PreparedStatement ps=cn.prepareStatement(sql.toString())){
            if(search){int count=1;for(int i=0;i<campos.length;i++)if("Texto".equals(tipos[i])||"Relacao".equals(tipos[i]))
                ps.setString(count++,"%"+busca.trim().toLowerCase(java.util.Locale.ROOT)+"%");}
            try(ResultSet rs=ps.executeQuery()){while(rs.next()){
                Object[] values=new Object[campos.length];String[] shown=new String[campos.length];
                for(int i=0;i<campos.length;i++){
                    values[i]=rs.getObject(campos[i]);
                    shown[i]="Relacao".equals(tipos[i])?rs.getString("__rotulo_"+i):rs.getString(campos[i]);
                    if(shown[i]==null)shown[i]="";
                }
                out.add(new Registro(rs.getInt("id"),values,shown));
            }}
        }
        return out;
    }
    public List<Opcao> opcoes(String tabela,String rotulo) throws SQLException {
        if(!tabela.matches("[a-z][a-z0-9_]*")||!rotulo.matches("[a-z][a-z0-9_]*"))
            throw new IllegalArgumentException("Identificador invalido.");
        List<Opcao> result=new ArrayList<Opcao>();
        String q="SELECT id,"+rotulo+" FROM "+tabela+" ORDER BY "+rotulo;
        try(Connection cn=Conexao.abrir();PreparedStatement ps=cn.prepareStatement(q);ResultSet rs=ps.executeQuery()){
            while(rs.next())result.add(new Opcao(rs.getInt(1),rs.getString(2)));
        }
        return result;
    }
    public void salvar(Integer id,Object[] values) throws SQLException {
        if(values.length!=campos.length)throw new IllegalArgumentException("Quantidade de campos invalida.");
        StringBuilder columns=new StringBuilder(),placeholders=new StringBuilder(),updates=new StringBuilder();
        for(String name:campos){if(columns.length()>0){columns.append(',');placeholders.append(',');updates.append(',');}
            columns.append(name);placeholders.append('?');updates.append(name).append("=?");}
        boolean editing=id!=null;
        String sql=editing?"UPDATE "+tabela+" SET "+updates+" WHERE id=?":
            "INSERT INTO "+tabela+" ("+columns+") VALUES ("+placeholders+")";
        try(Connection cn=Conexao.abrir();PreparedStatement ps=cn.prepareStatement(sql)){
            for(int i=0;i<values.length;i++)bind(ps,i+1,values[i],tipos[i],obrigatorios[i]);
            if(editing)ps.setInt(values.length+1,id.intValue());
            ps.executeUpdate();
        }
    }
    private void bind(PreparedStatement ps,int pos,Object input,String type,boolean required)throws SQLException{
        String value=input==null?"":input.toString().trim();
        if(value.isEmpty()){
            if(required)throw new IllegalArgumentException("Campo obrigatorio nao informado.");
            ps.setNull(pos,"Relacao".equals(type)||"Inteiro".equals(type)?Types.INTEGER:
                "Decimal".equals(type)?Types.DECIMAL:"Booleano".equals(type)?Types.BOOLEAN:
                "Data".equals(type)?Types.DATE:Types.VARCHAR);return;
        }
        try{
            if("Inteiro".equals(type)||"Relacao".equals(type))ps.setInt(pos,Integer.parseInt(value));
            else if("Decimal".equals(type))ps.setBigDecimal(pos,new BigDecimal(value.replace(',','.')));
            else if("Data".equals(type))ps.setDate(pos,Date.valueOf(LocalDate.parse(value)));
            else if("Booleano".equals(type))ps.setBoolean(pos,Boolean.parseBoolean(value));
            else ps.setString(pos,value);
        }catch(RuntimeException ex){throw new IllegalArgumentException("Valor invalido em "+campos[pos-1]+": "+value);}
    }
    public void excluir(int id)throws SQLException{
        try(Connection cn=Conexao.abrir();PreparedStatement ps=cn.prepareStatement("DELETE FROM "+tabela+" WHERE id=?")){
            ps.setInt(1,id);ps.executeUpdate();}
    }
}
