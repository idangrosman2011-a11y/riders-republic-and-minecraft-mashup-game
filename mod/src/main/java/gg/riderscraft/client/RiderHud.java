package gg.riderscraft.client;

import gg.riderscraft.gen.Gear;
import gg.riderscraft.rr.RRImporter;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Speed in km/h, the live combo, trick callouts and rocket fuel, laid out like Riders Republic's HUD. */
public final class RiderHud {
    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        RiderState s = RiderState.LOCAL;
        if (mc.player == null || mc.options.hideGui || s.gear == Gear.ON_FOOT) return;
        var font = mc.font;
        int w = g.guiWidth(), h = g.guiHeight();

        int kmh = (int) Math.round(s.speed * 20 * 3.6);
        g.drawString(font, kmh + " km/h", w - 70, h - 40, 0xFFFFFF);
        g.drawString(font, Component.translatable("gear.riderscraft." + s.gear.name().toLowerCase()), w - 70, h - 28, 0xFFD24A);

        if (s.gear.boostFuelTicks > 0) {
            int bw = 60, filled = (int) (bw * s.fuel / s.gear.boostFuelTicks);
            g.fill(w - 70, h - 16, w - 70 + bw, h - 12, 0x80000000);
            g.fill(w - 70, h - 16, w - 70 + filled, h - 12, 0xFFFF6A00);
        }
        if (s.comboPoints > 0) {
            g.drawCenteredString(font, s.comboPoints + "  x" + s.comboMultiplier, w / 2, 30, 0xFFD24A);
        }
        if (s.totalScore > 0) g.drawString(font, "Score " + s.totalScore, 6, 6, 0xFFFFFF);

        int y = h / 2 - 50;
        long now = mc.player.level().getGameTime();
        for (RiderState.Callout c : s.callouts) {
            int alpha = (int) Math.max(30, 255 - (now - c.bornTick()) * 3);
            g.drawCenteredString(font, c.text(), w / 2, y, (alpha << 24) | c.color());
            y += 11;
        }
        if (!RRImporter.status().ok()) {
            g.drawString(font, Component.translatable("hud.riderscraft.import_missing"), 6, h - 60, 0xFF8080);
        }
    }
}
