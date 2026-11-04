package __PACKAGE__.dao;

import __PACKAGE__.conexao.Conexao;
import __PACKAGE__.beans.Entidade;


import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** SQL parametrizado para valores; identificadores provem apenas do gerador. */
public abstract class CrudDAO {
    public final String tabela;
    public final String[] campos, tipos, tabelasRelacionadas, rotulosRelacionados;
    public final boolean[] obrigatorios;

    protected CrudDAO(
            String tabela,
            String[] campos,
            String[] tipos,
            String[] refs,
            String[] rotulos,
            boolean[] obrigatorios) {
        this.tabela = tabela;
        this.campos = campos;
        this.tipos = tipos;
        this.tabelasRelacionadas = refs;
        this.rotulosRelacionados = rotulos;
        this.obrigatorios = obrigatorios;
    }

    private static String ident(String name) {
        if (name == null || !name.matches("[A-Za-z][A-Za-z0-9_]*"))
            throw new IllegalArgumentException("Identificador invalido.");
        return "`" + name + "`";
    }

    public abstract List<Registro> listar(String busca) throws SQLException;

    public List<Entidade> opcoes(String tabela, String rotulo) throws SQLException {

        List<Entidade> result = new ArrayList<Entidade>();
        String q =
                "SELECT id,"
                        + ident(rotulo)
                        + " FROM "
                        + ident(tabela)
                        + " ORDER BY "
                        + ident(rotulo);
        try (Connection cn = Conexao.abrir();
                PreparedStatement ps = cn.prepareStatement(q);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(new Opcao(rs.getInt(1), rs.getString(2)));
        }
        return result;
    }

    /** A tela utiliza este metodo para que cada DAO possa trabalhar com seus beans. */
    public List<Registro> listarRegistros(String busca) throws SQLException {
        return listar(busca);
    }

    protected Object[] validarEntrada(Object[] entrada) {
        if (entrada.length != campos.length) throw new IllegalArgumentException("Quantidade de campos invalida.");
        Object[] dados = new Object[entrada.length];
        for (int i = 0; i < entrada.length; i++) {
            String valor = entrada[i] == null ? "" : entrada[i].toString().trim();
            if (valor.isEmpty()) {
                if (obrigatorios[i]) throw new IllegalArgumentException("Campo obrigatorio nao informado: " + campos[i]);
                continue;
            }
            try {
                if ("Inteiro".equals(tipos[i]) || "Relacao".equals(tipos[i])) dados[i] = Integer.valueOf(valor);
                else if ("Decimal".equals(tipos[i])) dados[i] = new BigDecimal(valor.replace(',', '.'));
                else if ("Data".equals(tipos[i])) dados[i] = valor.contains("/")
                        ? LocalDate.parse(valor, java.time.format.DateTimeFormatter.ofPattern("dd/MM/uuuu")
                            .withResolverStyle(java.time.format.ResolverStyle.STRICT)) : LocalDate.parse(valor);
                else if ("Booleano".equals(tipos[i])) {
                    if (!"true".equalsIgnoreCase(valor) && !"false".equalsIgnoreCase(valor)
                            && !"1".equals(valor) && !"0".equals(valor)) throw new IllegalArgumentException();
                    dados[i] = "true".equalsIgnoreCase(valor) || "1".equals(valor);
                } else dados[i] = valor;
            } catch (RuntimeException ex) {
                throw new IllegalArgumentException("Valor invalido em " + campos[i] + ": " + valor);
            }
        }
        return dados;
    }

    protected static Integer numeroInteiro(Object valor) {
        return valor == null ? null : ((Number) valor).intValue();
    }

    protected static BigDecimal numeroDecimal(Object valor) {
        return valor == null ? null : new BigDecimal(valor.toString());
    }

    protected static LocalDate dataLocal(Object valor) {
        return valor == null ? null : valor instanceof Date
                ? ((Date) valor).toLocalDate() : LocalDate.parse(valor.toString());
    }

    protected static Boolean valorBooleano(Object valor) {
        return valor == null ? null : valor instanceof Boolean ? (Boolean) valor
                : valor instanceof Number ? ((Number) valor).intValue() != 0 : Boolean.valueOf(valor.toString());
    }

    public abstract void salvar(Integer id, Object[] values) throws SQLException;

    public abstract void excluir(int id) throws SQLException;
}
