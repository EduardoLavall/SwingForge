package __PACKAGE__.conexao;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Executa o SQL gerado antes de abrir o cadastro. */
public final class PrepararBanco {
    private PrepararBanco() {}

    public static void executar() throws Exception {
        Conexao.carregarConfiguracao();
        String sql = new String(Files.readAllBytes(Paths.get("sql/01_criar_banco.sql")), StandardCharsets.UTF_8);
        try (Connection conexao = Conexao.abrirServidor(); Statement comando = conexao.createStatement()) {
            for (String instrucao : separar(sql)) comando.execute(instrucao);
        }
    }

    private static List<String> separar(String sql) {
        List<String> instrucoes = new ArrayList<String>();
        StringBuilder atual = new StringBuilder();
        boolean aspas = false, crase = false, dupla = false;
        for (String linha : sql.replace("\r\n", "\n").split("\n")) {
            if (linha.trim().startsWith("--")) continue;
            for (int i = 0; i < linha.length(); i++) {
                char c = linha.charAt(i);
                if (c == '\'' && !crase && !dupla) aspas = !aspas;
                else if (c == '`' && !aspas && !dupla) crase = !crase;
                else if (c == '"' && !aspas && !crase) dupla = !dupla;
                if (c == ';' && !aspas && !crase && !dupla) {
                    String instrucao = atual.toString().trim();
                    if (!instrucao.isEmpty()) instrucoes.add(instrucao);
                    atual.setLength(0);
                } else atual.append(c);
            }
            atual.append('\n');
        }
        if (!atual.toString().trim().isEmpty()) throw new IllegalArgumentException("SQL incompleto.");
        return instrucoes;
    }
}
