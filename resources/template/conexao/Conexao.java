package __PACKAGE__.conexao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class Conexao {
    private static String host, porta, usuario, senha;
    private static final String BANCO = "__DB_NAME__";

    private Conexao() {}

    public static void carregarConfiguracao() throws IOException {
        Properties config = new Properties();
        for (String linha : Files.readAllLines(Paths.get("banco.properties"), StandardCharsets.UTF_8)) {
            String texto = linha.trim();
            if (texto.isEmpty() || texto.startsWith("#")) continue;
            int separador = linha.indexOf('=');
            if (separador < 0) throw new IllegalArgumentException("Linha invalida em banco.properties.");
            config.setProperty(linha.substring(0, separador).trim(), linha.substring(separador + 1));
        }
        host = config.getProperty("host", "localhost").trim();
        porta = config.getProperty("porta", "3306").trim();
        usuario = config.getProperty("usuario", "root").trim();
        senha = config.getProperty("senha", "");
        if (!host.matches("[A-Za-z0-9._-]+") || !porta.matches("[0-9]{1,5}")
                || Integer.parseInt(porta) < 1 || Integer.parseInt(porta) > 65535 || usuario.isEmpty()) {
            throw new IllegalArgumentException("Confira host, porta e usuario em banco.properties.");
        }
    }

    private static Connection conectar(String banco) throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            throw new SQLException("Driver MySQL ausente em lib/.", ex);
        }
        String url = "jdbc:mysql://" + host + ":" + porta + "/" + banco
                + "?characterEncoding=UTF-8&connectionTimeZone=LOCAL&connectTimeout=4000";
        return DriverManager.getConnection(url, usuario, senha);
    }

    public static Connection abrirServidor() throws SQLException { return conectar(""); }
    public static Connection abrir() throws SQLException { return conectar(BANCO); }
}
