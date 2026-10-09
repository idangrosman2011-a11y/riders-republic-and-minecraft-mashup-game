package gg.riderscraft.client;

import gg.riderscraft.rr.RRImporter;
import net.minecraft.client.Minecraft;

import java.nio.file.Path;

/** Turns on the resource pack holding Riders Republic's ride sounds once the importer has written it. */
public final class ImportedPack {
    private static boolean applied;

    public static void applyIfReady(Minecraft mc) {
        if (applied) return;
        Path pack = RRImporter.soundPack();
        if (pack == null) return;
        applied = true;
        String id = "file/" + pack.getFileName();
        if (!mc.options.resourcePacks.contains(id)) {
            mc.options.resourcePacks.add(id);
            mc.getResourcePackRepository().reload();
            mc.options.loadSelectedResourcePacks(mc.getResourcePackRepository());
            mc.options.save();
            mc.reloadResourcePacks();
        }
    }
}
