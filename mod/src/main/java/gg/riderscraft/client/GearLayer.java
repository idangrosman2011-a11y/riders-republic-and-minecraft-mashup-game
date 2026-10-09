package gg.riderscraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import gg.riderscraft.gen.Gear;
import gg.riderscraft.gen.Trick;
import gg.riderscraft.rr.GearMeshes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Draws the equipped gear under the rider. The mesh and texture are the ones converted from the player's
 * Riders Republic install (rr/GearMeshes); until that import has run, a plain outline box marks where it goes.
 */
public class GearLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public GearLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) { super(parent); }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player != Minecraft.getInstance().player) return;
        RiderState s = RiderState.LOCAL;
        Gear g = s.gear;
        if (g == Gear.ON_FOOT || g.physics == Gear.Physics.GLIDE && !s.gliding) return;

        pose.pushPose();
        // Model space is flipped with the origin ~1.5 blocks above the feet; return to feet-origin, Y-up block units.
        pose.translate(0, 1.5f, 0);
        pose.scale(-1, -1, 1);
        if (s.animTrick == Trick.TAILWHIP || s.animTrick == Trick.DOUBLE_TAILWHIP) {
            float t = 1f - (s.animTicksLeft - partialTick) / s.animTrick.animTicks;
            pose.mulPose(Axis.YP.rotationDegrees(t * 360f * (s.animTrick == Trick.DOUBLE_TAILWHIP ? 2 : 1)));
        }
        GearMeshes.Mesh mesh = GearMeshes.get(g.rrModel);
        if (mesh != null) {
            VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(mesh.texture()));
            PoseStack.Pose p = pose.last();
            float[] v = mesh.vertices();
            for (int i = 0; i + 7 < v.length; i += 8) {
                vc.addVertex(p, v[i], v[i + 1], v[i + 2]).setColor(0xFFFFFFFF).setUv(v[i + 3], v[i + 4])
                        .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, v[i + 5], v[i + 6], v[i + 7]);
            }
        } else {
            placeholderOutline(pose, buffers, g);
        }
        pose.popPose();
    }

    /** Not gear art: an outline of the gear's footprint so a rider can tell what is equipped before the import. */
    private static void placeholderOutline(PoseStack pose, MultiBufferSource buffers, Gear g) {
        VertexConsumer vc = buffers.getBuffer(RenderType.lines());
        float hw, len, top;
        switch (g.fallbackShape) {
            case "bike" -> { hw = 0.1f; len = 0.9f; top = 0.9f; }
            case "skis" -> { hw = 0.3f; len = 0.9f; top = 0.05f; }
            case "board" -> { hw = 0.15f; len = 0.75f; top = 0.05f; }
            default -> { // wings: a membrane from wrists to ankles, in the body's own plane
                net.minecraft.client.renderer.LevelRenderer.renderLineBox(pose, vc, -0.9, 0.2, -0.05, 0.9, 1.45, 0.05, 1f, 0.42f, 0f, 1f);
                return;
            }
        }
        net.minecraft.client.renderer.LevelRenderer.renderLineBox(pose, vc, -hw, 0, -len, hw, top, len, 1f, 0.42f, 0f, 1f);
    }
}
