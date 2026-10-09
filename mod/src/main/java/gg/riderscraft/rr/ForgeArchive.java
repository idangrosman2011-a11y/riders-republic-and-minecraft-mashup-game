package gg.riderscraft.rr;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Riders Republic stores its data in Ubisoft Anvil ".forge" archives. Recognising an archive is implemented;
 * decoding its entries (index layout, compression, mesh and localisation formats) is NOT implemented yet: it has
 * to be worked out against a real install, which this build was not made with. See sheets/rr_content.json.
 */
public final class ForgeArchive {
    private static final byte[] MAGIC = "scimitar".getBytes(StandardCharsets.US_ASCII);

    public static boolean looksLikeForge(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            return Arrays.equals(in.readNBytes(MAGIC.length), MAGIC);
        }
    }

    public static byte[] extract(Path archive, String assetName) {
        throw new UnsupportedOperationException("Anvil .forge entry decoding is not implemented yet (" + assetName + ")");
    }
}
