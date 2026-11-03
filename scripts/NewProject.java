import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.SourceVersion;

/** Run from the repository root with Java 25; no extra scripting runtime is required. */
public class NewProject {
    private static final String OLD_PACKAGE = "com.company.qa";
    private static final String OLD_ARTIFACT = "qa-enterprise-template";

    public static void main(String[] args) throws IOException {
        if (args.length < 2 || args.length > 3) {
            throw new IllegalArgumentException(
                    "Usage: java scripts/NewProject.java <package> <artifact-id> [display-name]");
        }
        String pkg = args[0];
        String artifact = args[1];
        String name = args.length == 3 ? args[2] : artifact;
        if (!SourceVersion.isName(pkg, SourceVersion.RELEASE_25) || !pkg.contains(".")) {
            throw new IllegalArgumentException(
                    "Package must be a qualified Java name, for example com.acme.qa");
        }
        if (!artifact.matches("[a-z][a-z0-9]*(?:-[a-z0-9]+)*")) {
            throw new IllegalArgumentException("Artifact ID must be lowercase kebab-case");
        }
        if (pkg.equals(OLD_PACKAGE) && artifact.equals(OLD_ARTIFACT)) {
            throw new IllegalArgumentException("Choose your own package or artifact ID");
        }
        if (name.isBlank() || name.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(
                    "Display name must be non-empty and contain no control characters");
        }
        Path root = Path.of("").toAbsolutePath().normalize();
        Path pom = root.resolve("pom.xml");
        String pomText = Files.readString(pom);
        if (!pomText.contains("<artifactId>" + OLD_ARTIFACT + "</artifactId>")
                || !pomText.contains("<groupId>" + OLD_PACKAGE + "</groupId>")) {
            throw new IllegalStateException(
                    "Run once from an uninitialized template root. This project is already renamed.");
        }
        Map<Path, Path> moves = new LinkedHashMap<>();
        for (String source : List.of("main", "test")) {
            Path base = root.resolve("src").resolve(source).resolve("java");
            Path oldPath = base.resolve(OLD_PACKAGE.replace('.', java.io.File.separatorChar));
            Path newPath = base.resolve(pkg.replace('.', java.io.File.separatorChar));
            if (!Files.isDirectory(oldPath))
                throw new IllegalStateException("Missing template source: " + oldPath);
            if (!oldPath.equals(newPath)
                    && (Files.exists(newPath)
                            || newPath.startsWith(oldPath)
                            || oldPath.startsWith(newPath))) {
                throw new IllegalArgumentException(
                        "Package destination collides with existing sources: " + newPath);
            }
            moves.put(oldPath, newPath);
        }
        Map<Path, String> originals = new LinkedHashMap<>();
        Map<Path, String> updates = new LinkedHashMap<>();
        for (String directory : List.of("src", "docs")) {
            try (var files = Files.walk(root.resolve(directory))) {
                for (Path file :
                        files.filter(Files::isRegularFile)
                                .filter(p -> !Files.isSymbolicLink(p))
                                .filter(
                                        p ->
                                                p.toString()
                                                                .matches(
                                                                        ".*\\.(java|properties|xml|json|html|md)$")
                                                        || p.toString().contains("META-INF"))
                                .toList()) {
                    String text = Files.readString(file, StandardCharsets.UTF_8);
                    if (text.contains(OLD_PACKAGE)) {
                        originals.put(file, text);
                        updates.put(file, text.replace(OLD_PACKAGE, pkg));
                    }
                }
            }
        }
        originals.put(pom, pomText);
        updates.put(
                pom,
                pomText.replace(OLD_PACKAGE, pkg)
                        .replace(
                                "<artifactId>" + OLD_ARTIFACT + "</artifactId>",
                                "<artifactId>" + artifact + "</artifactId>")
                        .replace(
                                "<name>QA Enterprise Template</name>",
                                "<name>" + xml(name) + "</name>"));
        Path readme = root.resolve("README.md");
        String readmeText = Files.readString(readme);
        originals.put(readme, readmeText);
        updates.put(
                readme,
                readmeText
                        .replaceFirst(
                                "# QA Enterprise Template",
                                java.util.regex.Matcher.quoteReplacement("# " + name))
                        .replace(OLD_PACKAGE, pkg));
        List<Map.Entry<Path, Path>> moved = new java.util.ArrayList<>();
        try {
            for (var update : updates.entrySet())
                Files.writeString(update.getKey(), update.getValue(), StandardCharsets.UTF_8);
            for (var move : moves.entrySet()) {
                if (move.getKey().equals(move.getValue())) continue;
                Files.createDirectories(move.getValue().getParent());
                Files.move(move.getKey(), move.getValue());
                moved.add(move);
            }
        } catch (IOException e) {
            for (int i = moved.size() - 1; i >= 0; i--) {
                var move = moved.get(i);
                try {
                    Files.move(move.getValue(), move.getKey());
                } catch (IOException rollback) {
                    e.addSuppressed(rollback);
                }
            }
            for (var original : originals.entrySet()) {
                try {
                    Files.writeString(
                            original.getKey(), original.getValue(), StandardCharsets.UTF_8);
                } catch (IOException rollback) {
                    e.addSuppressed(rollback);
                }
            }
            throw e;
        }
        System.out.println("Initialized " + name + " (" + pkg + ":" + artifact + ")");
        System.out.println("Next: ./mvnw clean verify (Windows: .\\mvnw.cmd clean verify)");
    }

    private static String xml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
