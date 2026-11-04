package __PACKAGE__;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Conexao {
    private static String host = "localhost",
            port = "3306",
            banco = "__DB_NAME__",
            user = "root",
            password = "";

    private Conexao() {}

    public static void configurar(String h, String p, String b, String u, String pass) {
        if (!h.matches("[A-Za-z0-9._-]+")
                || !p.matches("[0-9]{1,5}")
                || !b.matches("[A-Za-z0-9_]+")
                || u.trim().isEmpty())
            throw new IllegalArgumentException("Confira os dados de conexao.");
        int number = Integer.parseInt(p);
        if (number < 1 || number > 65535) throw new IllegalArgumentException("Porta invalida.");
        host = h;
        port = p;
        banco = b;
        user = u;
        password = pass;
    }

    public static Connection abrir() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            throw new SQLException(
                    "Driver MySQL ausente. Confira lib/mysql-connector-j-8.1.0.jar e "
                            + "recompile o projeto. O JAR em dist/ depende da pasta lib/.",
                    ex);
        }
        String url =
                "jdbc:mysql://"
                        + host
                        + ":"
                        + port
                        + "/"
                        + banco
                        + "?characterEncoding=UTF-8&connectionTimeZone=LOCAL&connectTimeout=4000";
        return DriverManager.getConnection(url, user, password);
    }

    public static String bancoPadrao() {
        return banco;
    }
}
