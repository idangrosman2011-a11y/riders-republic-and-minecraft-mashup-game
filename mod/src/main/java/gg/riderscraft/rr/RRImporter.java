package gg.riderscraft.rr;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import gg.riderscraft.RidersCraft;
import gg.riderscraft.gen.Gear;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reads the gear models, ride sounds and trick callout text out of the player's Riders Republic install once,
 * into <instance>/riderscraft/imported (models, callouts) and <instance>/resourcepacks/riderscraft-imported (sounds).
 * Nothing from Riders Republic ships with the mod.
 */
public final class RRImporter {
    public record Status(boolean ok, String message) {}

    private static volatile Status status = new Status(false, "Looking for Riders Republic...");
    private static final Map<String, String> CALLOUTS = new ConcurrentHashMap<>();
    private static volatile Path soundPack;

    public static Status status() { return status; }

    public static Path soundPack() { return soundPack; }

    public static String calloutText(String key, String fallback) { return CALLOUTS.getOrDefault(key, fallback); }

    public static void startInBackground(Path gameDir) {
        Thread t = new Thread(() -> run(gameDir), "Riders Craft import");
        t.setDaemon(true);
        t.start();
    }

    private static void run(Path gameDir) {
        Path out = gameDir.resolve("riderscraft/imported");
        try {
            Files.createDirectories(out.resolve("models"));
            GearMeshes.setDirectory(out.resolve("models"));
            RidersRepublicInstall rr = RidersRepublicInstall.find();
            if (rr == null) {
                status = new Status(false, "Riders Republic folder not found (" + RidersRepublicInstall.ENV + ")");
                RidersCraft.LOG.warn(status.message());
                return;
            }
            Path stamp = out.resolve("source.txt");
            String fp = rr.fingerprint();
            List<String> report = new ArrayList<>();
            if (!Files.exists(stamp) || !Files.readString(stamp).equals(fp)) {
                report.addAll(extract(rr, out, gameDir));
                Files.write(out.resolve("import-report.txt"), report);
            }
            loadCallouts(out.resolve("callouts.json"));
            Path pack = gameDir.resolve("resourcepacks/riderscraft-imported");
            if (Files.exists(pack.resolve("pack.mcmeta"))) soundPack = pack;
            boolean complete = report.stream().noneMatch(l -> l.startsWith("MISSING"));
            if (complete) Files.writeString(stamp, fp);
            status = new Status(complete, complete ? "Riders Republic content imported" : "Riders Republic import incomplete, see " + out.resolve("import-report.txt"));
            RidersCraft.LOG.info("{} ({} archives, {} sound banks at {})", status.message(), rr.forgeArchives().size(), rr.soundBanks().size(), rr.root());
        } catch (Exception e) {
            status = new Status(false, "Riders Republic import failed: " + e.getMessage());
            RidersCraft.LOG.error("Riders Republic import failed", e);
        }
    }

    /** One step per rr_content sheet row. Rows whose decoder isn't written yet are reported, never faked. */
    private static List<String> extract(RidersRepublicInstall rr, Path out, Path gameDir) throws Exception {
        List<String> report = new ArrayList<>();
        int forges = 0;
        for (Path f : rr.forgeArchives()) if (ForgeArchive.looksLikeForge(f)) forges++;
        report.add("FOUND " + forges + "/" + rr.forgeArchives().size() + " Anvil archives, " + rr.soundBanks().size() + " Wwise banks in " + rr.root());
        for (Gear g : Gear.values()) {
            if (!g.rrModel.equals("none")) report.add(step(g.rrModel, () -> ForgeArchive.extract(rr.forgeArchives().get(0), g.rrModel)));
        }
        for (String s : new String[]{"sound_bike_roll", "sound_snow_carve", "sound_wind", "sound_rocket", "sound_trick_land", "sound_trick_bail"}) {
            report.add(step(s, () -> { throw new UnsupportedOperationException("Wwise bank to .ogg conversion is not implemented yet"); }));
        }
        report.add(step("callout_text", () -> ForgeArchive.extract(rr.forgeArchives().get(0), "callout_text")));
        return report;
    }

    private interface Step { Object run() throws Exception; }

    private static String step(String id, Step s) {
        try { s.run(); return "OK " + id; }
        catch (Exception e) { return "MISSING " + id + ": " + e.getMessage(); }
    }

    private static void loadCallouts(Path file) throws Exception {
        if (!Files.exists(file)) return;
        Map<String, String> m = new Gson().fromJson(Files.readString(file), new TypeToken<Map<String, String>>() {}.getType());
        if (m != null) CALLOUTS.putAll(m);
    }
}
