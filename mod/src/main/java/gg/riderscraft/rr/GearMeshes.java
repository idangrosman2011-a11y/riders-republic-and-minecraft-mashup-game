package gg.riderscraft.rr;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import gg.riderscraft.RidersCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Gear meshes converted from the player's Riders Republic install, stored per player under
 * riderscraft/imported/models/<key>.json: {"texture": "<key>.png", "vertices": [x,y,z,u,v,nx,ny,nz, ...]} in
 * block units, feet at the origin, Y up, triangles drawn as quads of 4 vertices.
 */
public final class GearMeshes {
    public record Mesh(ResourceLocation texture, float[] vertices) {}

    private static final Map<String, Mesh> LOADED = new HashMap<>();
    private static Path dir;

    static void setDirectory(Path modelsDir) { dir = modelsDir; LOADED.clear(); }

    /** Render thread only (creates GPU textures). Null until the import has produced this model. */
    public static Mesh get(String key) {
        if (dir == null || key.equals("none")) return null;
        return LOADED.computeIfAbsent(key, k -> {
            Path json = dir.resolve(k + ".json");
            if (!Files.exists(json)) return null;
            try {
                JsonObject o = JsonParser.parseString(Files.readString(json)).getAsJsonObject();
                JsonArray a = o.getAsJsonArray("vertices");
                float[] v = new float[a.size()];
                for (int i = 0; i < v.length; i++) v[i] = a.get(i).getAsFloat();
                NativeImage img;
                try (InputStream in = Files.newInputStream(dir.resolve(o.get("texture").getAsString()))) { img = NativeImage.read(in); }
                ResourceLocation tex = RidersCraft.id("imported/" + k);
                Minecraft.getInstance().getTextureManager().register(tex, new DynamicTexture(img));
                return new Mesh(tex, v);
            } catch (Exception e) {
                RidersCraft.LOG.warn("Could not load imported model {}", k, e);
                return null;
            }
        });
    }
}
