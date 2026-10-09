package gg.riderscraft.client;

import gg.riderscraft.gen.Control;
import gg.riderscraft.gen.Gear;
import gg.riderscraft.net.GearPayload;
import gg.riderscraft.rr.RRImporter;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;

public class RidersCraftClient implements ClientModInitializer {
    private static int wheelKeyTicks;

    @Override
    public void onInitializeClient() {
        Controls.register();
        HudRenderCallback.EVENT.register(RiderHud::render);
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
            if (renderer instanceof PlayerRenderer pr) helper.register(new GearLayer(pr));
        });
        ClientLifecycleEvents.CLIENT_STARTED.register(mc -> RRImporter.startInBackground(mc.gameDirectory.toPath()));
        ClientPlayConnectionEvents.JOIN.register((h, s, mc) -> equip(RiderState.LOCAL.gear));
        ClientTickEvents.END_CLIENT_TICK.register(RidersCraftClient::tick);
        if (Boolean.getBoolean("riderscraft.devtest")) DevTest.install();
    }

    private static void tick(Minecraft mc) {
        ImportedPack.applyIfReady(mc);
        if (mc.player == null) return;
        // Hold 1 for the wheel; a quick tap stays a hotbar-slot-1 press.
        if (mc.screen == null) {
            if (Controls.isDown(Control.GEAR_WHEEL)) {
                if (++wheelKeyTicks == 4) mc.setScreen(new GearWheelScreen());
            } else {
                if (wheelKeyTicks > 0 && wheelKeyTicks < 4) mc.player.getInventory().selected = 0;
                wheelKeyTicks = 0;
            }
        } else if (!(mc.screen instanceof GearWheelScreen)) {
            wheelKeyTicks = 0;
        }
        if (mc.isPaused()) return;
        RidePhysics.tick(mc.player, RiderState.LOCAL);
        TrickSystem.tick(mc, mc.player, RiderState.LOCAL);
        RideSound.tick(mc, RiderState.LOCAL);
    }

    public static void equip(Gear gear) {
        RiderState s = RiderState.LOCAL;
        Minecraft mc = Minecraft.getInstance();
        if (gear != s.gear && mc.player != null) {
            s.speed = mc.player.getDeltaMovement().horizontalDistance();
            s.heading = Math.toRadians(mc.player.getYRot());
            s.lastGroundY = mc.player.getY();
            s.gliding = false;
            s.fuel = gear.boostFuelTicks;
        }
        s.gear = gear;
        if (ClientPlayNetworking.canSend(GearPayload.TYPE)) ClientPlayNetworking.send(new GearPayload(gear.ordinal()));
    }

    /** Called by the wheel once the key is released. */
    static void wheelClosed() { wheelKeyTicks = 0; }
}
