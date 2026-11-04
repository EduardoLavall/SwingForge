package swingforge.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Connector/J local: copia o arquivo original sem baixar ou recompilar o driver. */
public final class MysqlDriver {
    public static final String NAME = "mysql-connector-j-8.1.0.jar";
    private static final long MAX_BYTES = 32L * 1024L * 1024L;
    private MysqlDriver() { }

    private static Path store() {
        return Paths.get(System.getProperty("user.home"), ".swingforge", "drivers", NAME);
    }

    /** Busca o pacote portatil ou o driver importado pelo usuario. */
    public static Path find() {
        Path cwd = Paths.get(System.getProperty("user.dir"));
        Path[] locations = new Path[] {
            cwd.resolve("tools/jdbc").resolve(NAME),
            store(),
            siblingToExecutable()
        };
        for (Path p : locations) {
            if (p != null && Files.isRegularFile(p) && Files.isReadable(p)) return p;
        }
        return null;
    }

    private static Path siblingToExecutable() {
        try {
            Path source = Paths.get(MysqlDriver.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            Path base = Files.isDirectory(source) ? source : source.getParent();
            if (base == null) return null;
            Path direct = base.resolve("tools/jdbc").resolve(NAME);
            if (Files.isRegularFile(direct)) return direct;
            return base.getParent() == null ? null : base.getParent().resolve("tools/jdbc").resolve(NAME);
        } catch (Exception ignored) { return null; }
    }

    private static void verify(Path jar) throws IOException {
        if (Files.size(jar) > MAX_BYTES) throw new IOException("Driver maior que o limite permitido.");
        try (JarFile file = new JarFile(jar.toFile())) {
            if (file.getManifest() == null || !"8.1.0".equals(
                    file.getManifest().getMainAttributes().getValue("Implementation-Version")))
                throw new IOException("Selecione o MySQL Connector/J versao 8.1.0.");
            if (file.getEntry("com/mysql/cj/jdbc/Driver.class") == null)
                throw new IOException("O JAR nao contem o driver JDBC do MySQL.");
        }
    }

    /** Aceita o .zip do professor ou o JAR extraido; guarda no perfil local. */
    public static Path importFile(Path input) throws IOException {
        if (!Files.isRegularFile(input)) throw new IOException("Arquivo nao encontrado.");
        Files.createDirectories(store().getParent());
        Path temp = Files.createTempFile(store().getParent(), "connector-j-", ".jar");
        boolean copied = false;
        try {
            if (input.getFileName().toString().toLowerCase().endsWith(".jar")) {
                if (Files.size(input) > MAX_BYTES) throw new IOException("Arquivo muito grande.");
                Files.copy(input, temp, StandardCopyOption.REPLACE_EXISTING);
                copied = true;
            } else if (input.getFileName().toString().toLowerCase().endsWith(".zip")) {
                try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(input))) {
                    ZipEntry entry;
                    while ((entry = zip.getNextEntry()) != null) {
                        String path = entry.getName().replace('\\', '/');
                        if (!entry.isDirectory() && (path.equals(NAME) || path.endsWith("/" + NAME))) {
                            try (java.io.OutputStream out = Files.newOutputStream(temp)) {
                                byte[] buf = new byte[8192]; int read; long total = 0;
                                while ((read = zip.read(buf)) != -1) {
                                    total += read;
                                    if (total > MAX_BYTES) throw new IOException("Driver muito grande.");
                                    out.write(buf, 0, read);
                                }
                            }
                            copied = true;
                            break;
                        }
                        zip.closeEntry();
                    }
                }
            } else throw new IOException("Escolha o ZIP do professor ou o arquivo JAR.");
            if (!copied) throw new IOException("Nao encontrei " + NAME + " dentro do ZIP.");
            verify(temp);
            Files.move(temp, store(), StandardCopyOption.REPLACE_EXISTING);
            return store();
        } finally { Files.deleteIfExists(temp); }
    }

    /** Copia os bytes originais para lib/ sem recriar a biblioteca. */
    public static boolean copyTo(Path project, Path backup) throws IOException {
        Path source = find();
        if (source == null) return false;
        verify(source);
        Path target = project.resolve("lib").resolve(NAME);
        Files.createDirectories(target.getParent());
        if (Files.isRegularFile(target) && Files.size(source) == Files.size(target)) {
            if (java.util.Arrays.equals(Files.readAllBytes(source), Files.readAllBytes(target))) return true;
        }
        if (Files.isRegularFile(target)) {
            Path old = backup.resolve("lib").resolve(NAME);
            Files.createDirectories(old.getParent());
            Files.copy(target, old, StandardCopyOption.REPLACE_EXISTING);
        }
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        return true;
    }
}
