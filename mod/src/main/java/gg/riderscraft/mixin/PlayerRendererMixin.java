package gg.riderscraft.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import gg.riderscraft.client.RiderState;
import gg.riderscraft.gen.Gear;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Turns the rider through flips, spins and rolls, and lays them flat in the wingsuit. */
@Mixin(PlayerRenderer.class)
public class PlayerRendererMixin {
    @Inject(method = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V", at = @At("TAIL"))
    private void riderscraft$trickPose(AbstractClientPlayer player, PoseStack pose, float bob, float yBodyRot, float partialTick, float scale, CallbackInfo ci) {
        if (player != Minecraft.getInstance().player) return; // solo: only the local rider has gear state
        RiderState s = RiderState.LOCAL;
        if (s.gear == Gear.ON_FOOT) return;
        float centre = 0.9f * scale;
        pose.translate(0, centre, 0);
        if (s.gliding) pose.mulPose(Axis.XP.rotationDegrees(-90f - player.getXRot()));
        float spin = Mth.lerp(partialTick, s.prevSpin, s.spin);
        float flip = Mth.lerp(partialTick, s.prevFlip, s.flip);
        float roll = Mth.lerp(partialTick, s.prevRoll, s.roll);
        if (spin != 0) pose.mulPose(Axis.YP.rotationDegrees(-spin));
        if (flip != 0) pose.mulPose(Axis.XP.rotationDegrees(flip));
        if (roll != 0) pose.mulPose(Axis.ZP.rotationDegrees(roll));
        pose.translate(0, -centre, 0);
    }
}
