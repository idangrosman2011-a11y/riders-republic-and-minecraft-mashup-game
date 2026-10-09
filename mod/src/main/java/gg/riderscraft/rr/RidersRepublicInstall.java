package gg.riderscraft.rr;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * The player's own Riders Republic folder. Melty checks the game is installed and passes its folder in
 * RIDERSCRAFT_RR_DIR; a dev run can set the same variable or the system property riderscraft.rrDir.
 */
public record RidersRepublicInstall(Path root, List<Path> forgeArchives, List<Path> soundBanks) {
    public static final String ENV = "RIDERSCRAFT_RR_DIR";

    public static RidersRepublicInstall find() throws IOException {
        String dir = System.getenv(ENV);
        if (dir == null || dir.isBlank()) dir = System.getProperty("riderscraft.rrDir");
        if (dir == null || dir.isBlank()) return null;
        Path root = Path.of(dir);
        if (!Files.isDirectory(root)) return null;
        List<Path> forges = new ArrayList<>(), banks = new ArrayList<>();
        try (Stream<Path> files = Files.walk(root, 4)) {
            files.filter(Files::isRegularFile).forEach(f -> {
                String n = f.getFileName().toString().toLowerCase();
                if (n.endsWith(".forge")) forges.add(f);
                else if (n.endsWith(".pck") || n.endsWith(".bnk")) banks.add(f);
            });
        }
        return new RidersRepublicInstall(root, forges, banks);
    }

    /** Changes whenever the game updates, so a patched install is re-imported. */
    public String fingerprint() throws IOException {
        long h = 17;
        for (Path p : forgeArchives) h = h * 31 + Files.size(p) * 7 + Files.getLastModifiedTime(p).toMillis();
        for (Path p : soundBanks) h = h * 31 + Files.size(p);
        return Long.toHexString(h);
    }
}
