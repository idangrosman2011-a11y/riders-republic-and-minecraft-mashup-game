package gg.riderscraft.client;

import gg.riderscraft.gen.Control;
import gg.riderscraft.gen.Gear;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** Riders Republic's gear wheel: hold 1, point at a sport, let go. */
public class GearWheelScreen extends Screen {
    private static final Gear[] SLOTS = sorted();
    private int hovered = -1;

    public GearWheelScreen() { super(Component.literal("Gear")); }

    private static Gear[] sorted() {
        Gear[] g = Gear.values().clone();
        java.util.Arrays.sort(g, java.util.Comparator.comparingInt(x -> x.wheelSlot));
        return g;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void tick() {
        if (!Controls.isDown(Control.GEAR_WHEEL)) {
            if (hovered >= 0) RidersCraftClient.equip(SLOTS[hovered]);
            RidersCraftClient.wheelClosed();
            onClose();
        }
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (hovered >= 0) RidersCraftClient.equip(SLOTS[hovered]);
        RidersCraftClient.wheelClosed();
        onClose();
        return true;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // keep the world visible behind the wheel
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int cx = width / 2, cy = height / 2, radius = Math.min(width, height) / 3;
        double dx = mouseX - cx, dy = mouseY - cy;
        int n = SLOTS.length;
        hovered = -1;
        if (dx * dx + dy * dy > 20 * 20) {
            double ang = (Math.atan2(dx, -dy) + Math.PI * 2 + Math.PI / n) % (Math.PI * 2);
            hovered = (int) (ang / (Math.PI * 2 / n));
        }
        g.fill(cx - 2, cy - 2, cx + 2, cy + 2, 0xFFFFFFFF);
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n;
            int x = cx + (int) (Mth.sin((float) a) * radius), y = cy - (int) (Mth.cos((float) a) * radius);
            boolean on = i == hovered, current = SLOTS[i] == RiderState.LOCAL.gear;
            int w = 46, h = 16;
            g.fill(x - w, y - h, x + w, y + h, on ? 0xE0FF6A00 : 0xA0101010);
            if (current) g.renderOutline(x - w, y - h, w * 2, h * 2, 0xFFFFD24A);
            g.drawCenteredString(font, Component.translatable("gear.riderscraft." + SLOTS[i].name().toLowerCase()), x, y - 4, 0xFFFFFF);
        }
        g.drawCenteredString(font, hovered >= 0 ? Component.translatable("gear.riderscraft." + SLOTS[hovered].name().toLowerCase())
                : Component.literal("Riders Republic gear"), cx, cy + radius + 26, 0xFFD24A);
    }
}
