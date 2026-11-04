package swingforge.core;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/** Prepara o esquema gerado sem internet, sem persistir credenciais e sem apagar dados. */
public final class DatabaseSetup {
    private DatabaseSetup() { }

    public static final class Credentials {
        public final String host, port, user;
        private final char[] password;
        public Credentials(String host, String port, String user, char[] password) {
            if (host == null || !host.matches("[a-zA-Z0-9._-]+"))
                throw new IllegalArgumentException("Host MySQL invalido.");
            if (port == null || !port.matches("[0-9]{1,5}"))
                throw new IllegalArgumentException("Porta MySQL invalida.");
            int numericPort = Integer.parseInt(port);
            if (numericPort < 1 || numericPort > 65535)
                throw new IllegalArgumentException("Porta fora do intervalo permitido.");
            if (user == null || user.trim().isEmpty())
                throw new IllegalArgumentException("Informe o usuario MySQL.");
            this.host = host; this.port = port; this.user = user;
            this.password = password == null ? new char[0] : password.clone();
        }
        public void clear() { Arrays.fill(password, '\0'); }
    }

    public static final class Inspection {
        public final boolean databaseExists;
        public final List<String> missingTables;
        public final List<String> missingRelations;
        public final List<String> conflicts;
        private Inspection(boolean exists, List<String> missingTables, List<String> missingRelations, List<String> conflicts) {
            this.databaseExists = exists;
            this.missingTables = missingTables;
            this.missingRelations = missingRelations;
            this.conflicts = conflicts;
        }
        public boolean ready() { return databaseExists && missingTables.isEmpty() && missingRelations.isEmpty() && conflicts.isEmpty(); }
        public boolean safeToPrepare() { return conflicts.isEmpty(); }
        public String describe(String database) {
            StringBuilder b = new StringBuilder("Servidor conectado.\nBanco: ").append(database).append('\n');
            b.append(databaseExists ? "Banco ja existe.\n" : "Banco ainda nao existe.\n");
            if (!missingTables.isEmpty()) b.append("Tabelas ausentes: ").append(String.join(", ", missingTables)).append("\n");
            if (!missingRelations.isEmpty()) b.append("Relacionamentos ausentes: ").append(String.join(", ", missingRelations)).append("\n");
            if (!conflicts.isEmpty()) b.append("Problemas que exigem revisao manual:\n - ")
                    .append(String.join("\n - ", conflicts)).append("\n");
            if (ready()) b.append("As tabelas e colunas previstas estao presentes.\n");
            return b.toString();
        }
    }

    private static final class Session implements AutoCloseable {
        private final URLClassLoader loader;
        final Connection connection;
        Session(Path driverJar, Credentials credentials) throws Exception {
            if (driverJar == null || !Files.isRegularFile(driverJar))
                throw new IllegalStateException("Connector/J 8.1.0 nao encontrado. Confira a pasta lib/ do projeto.");
            loader = new URLClassLoader(new URL[]{driverJar.toUri().toURL()},
                    DatabaseSetup.class.getClassLoader());
            Connection conn = null;
            try {
                Driver driver = (Driver) Class.forName("com.mysql.cj.jdbc.Driver", true, loader)
                        .getDeclaredConstructor().newInstance();
                Properties props = new Properties();
                props.setProperty("user", credentials.user);
                props.setProperty("password", new String(credentials.password));
                String url = "jdbc:mysql://" + credentials.host + ":" + credentials.port
                        + "/?connectTimeout=4000&connectionTimeZone=LOCAL";
                conn = driver.connect(url, props);
                if (conn == null) throw new SQLException("Connector/J nao aceitou a URL JDBC.");
                connection = conn;
            } catch (Exception ex) {
                if (conn != null) try { conn.close(); } catch (SQLException ignored) { }
                try { loader.close(); } catch (Exception ignored) { }
                throw ex;
            }
        }
        @Override public void close() throws Exception {
            try { connection.close(); } finally { loader.close(); }
        }
    }

    private static boolean exists(Connection connection, String query, String... args) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            for (int i = 0; i < args.length; i++) ps.setString(i + 1, args[i]);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public static Inspection inspect(Spec spec, Credentials credentials, Path jar) throws Exception {
        Validator.validate(spec);
        try (Session session = new Session(jar, credentials)) {
            return inspectUsing(session.connection, spec);
        }
    }

    private static Inspection inspectUsing(Connection connection, Spec spec) throws SQLException {
        boolean db = exists(connection, "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME=?", spec.database);
        List<String> missingTables = new ArrayList<String>();
        List<String> missingRelations = new ArrayList<String>();
        List<String> conflicts = new ArrayList<String>();
        if (!db) {
            for (Spec.Entity entity : spec.entities) missingTables.add(Spec.table(entity.name));
            return new Inspection(false, missingTables, missingRelations, conflicts);
        }
        for (Spec.Entity entity : spec.entities) {
            String table = Spec.table(entity.name);
            if (!exists(connection, "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES " +
                    "WHERE TABLE_SCHEMA=? AND TABLE_NAME=? AND TABLE_TYPE='BASE TABLE'", spec.database, table)) {
                missingTables.add(table);
                continue;
            }
            inspectColumn(connection, spec.database, table, "id", "Inteiro", true, conflicts);
            for (Spec.Field field : entity.fields) {
                inspectColumn(connection, spec.database, table, field.name, field.type, false, conflicts);
                if (!"Relacao".equals(field.type)) continue;
                String target = Spec.table(field.reference);
                try (PreparedStatement ps = connection.prepareStatement(
                        "SELECT REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE " +
                        "WHERE TABLE_SCHEMA=? AND TABLE_NAME=? AND COLUMN_NAME=? AND REFERENCED_TABLE_NAME IS NOT NULL")) {
                    ps.setString(1, spec.database); ps.setString(2, table); ps.setString(3, field.name);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) missingRelations.add(table + "." + field.name + " -> " + target + ".id");
                        else if (!target.equalsIgnoreCase(rs.getString(1)) || !"id".equalsIgnoreCase(rs.getString(2)))
                            conflicts.add("Relacionamento incompatível em " + table + "." + field.name + ".");
                    }
                }
            }
        }
        return new Inspection(true, missingTables, missingRelations, conflicts);
    }

    private static void inspectColumn(Connection connection, String db, String table, String column,
                                      String expected, boolean primaryKey, List<String> conflicts) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT DATA_TYPE, EXTRA FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA=? AND TABLE_NAME=? AND COLUMN_NAME=?")) {
            ps.setString(1, db); ps.setString(2, table); ps.setString(3, column);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) { conflicts.add("Coluna ausente: " + table + "." + column); return; }
                String actual = rs.getString(1);
                String extra = rs.getString(2);
                boolean matching = "Texto".equals(expected) ? "varchar".equals(actual) :
                        "Decimal".equals(expected) ? "decimal".equals(actual) :
                        "Data".equals(expected) ? "date".equals(actual) :
                        "Booleano".equals(expected) ? "tinyint".equals(actual) : "int".equals(actual);
                if (!matching) conflicts.add("Tipo incorreto em " + table + "." + column + ": esperado " + expected + ", atual " + actual);
                if (primaryKey && (extra == null || !extra.toLowerCase(java.util.Locale.ROOT).contains("auto_increment")))
                    conflicts.add("ID sem AUTO_INCREMENT na tabela " + table + ".");
            }
        }
    }

    /** Apenas SQL gerado/validado. Sem DROP, DELETE, TRUNCATE ou substituicao de dados. */
    public static Inspection prepare(Spec spec, Credentials credentials, Path jar) throws Exception {
        Validator.validate(spec);
        try (Session session = new Session(jar, credentials)) {
            Inspection before = inspectUsing(session.connection, spec);
            if (!before.safeToPrepare())
                throw new IllegalStateException("Estrutura existente incompatível. Corrija manualmente antes de preparar o banco.\n"
                        + before.describe(spec.database));
            if (!before.ready()) {
                for (String sql : splitGeneratedSql(Generator.sql(spec))) {
                    try (Statement statement = session.connection.createStatement()) {
                        statement.execute(sql);
                    }
                }
            }
            Inspection after = inspectUsing(session.connection, spec);
            if (!after.ready()) throw new SQLException("O SQL foi executado, mas ainda faltam tabelas ou colunas: \n" + after.describe(spec.database));
            return after;
        }
    }

    /** Divide apenas o SQL produzido por Generator.sql(), respeitando aspas e backticks. */
    static List<String> splitGeneratedSql(String rawSql) {
        List<String> sql = new ArrayList<String>();
        StringBuilder statement = new StringBuilder();
        boolean single = false, backtick = false, doubleQuoted = false;
        String[] lines = rawSql.replace("\r\n", "\n").split("\n");
        for (String line : lines) {
            if (line.trim().startsWith("--")) continue;
            for (int i = 0; i < line.length(); i++) {
                char ch = line.charAt(i);
                if (ch == '\'' && !backtick && !doubleQuoted) single = !single;
                else if (ch == '`' && !single && !doubleQuoted) backtick = !backtick;
                else if (ch == '"' && !single && !backtick) doubleQuoted = !doubleQuoted;
                if (ch == ';' && !single && !backtick && !doubleQuoted) {
                    String command = statement.toString().trim();
                    if (!command.isEmpty()) sql.add(command);
                    statement.setLength(0);
                } else statement.append(ch);
            }
            statement.append('\n');
        }
        if (!statement.toString().trim().isEmpty())
            throw new IllegalArgumentException("Script SQL gerado nao termina corretamente com ponto e virgula.");
        return sql;
    }
}