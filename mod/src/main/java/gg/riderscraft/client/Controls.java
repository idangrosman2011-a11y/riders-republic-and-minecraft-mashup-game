package gg.riderscraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import gg.riderscraft.gen.Control;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.EnumMap;
import java.util.Map;

/** Key mappings built from sheets/controls.json. Vanilla controls reuse the player's own bindings. */
public final class Controls {
    private static final Map<Control, KeyMapping> OWN = new EnumMap<>(Control.class);

    public static void register() {
        for (Control c : Control.values()) {
            if (c.isVanilla()) continue;
            int code;
            try { code = GLFW.class.getField(c.key).getInt(null); }
            catch (ReflectiveOperationException e) { throw new IllegalStateException("controls sheet: unknown key " + c.key, e); }
            OWN.put(c, KeyBindingHelper.registerKeyBinding(new KeyMapping(
                    "key.riderscraft." + c.name().toLowerCase(), InputConstants.Type.KEYSYM, code, "key.categories.riderscraft")));
        }
    }

    public static KeyMapping mapping(Control c) {
        if (!c.isVanilla()) return OWN.get(c);
        var o = Minecraft.getInstance().options;
        return switch (c.key.substring("vanilla:".length())) {
            case "keyUp" -> o.keyUp;
            case "keyDown" -> o.keyDown;
            case "keyLeft" -> o.keyLeft;
            case "keyRight" -> o.keyRight;
            case "keySprint" -> o.keySprint;
            case "keyJump" -> o.keyJump;
            default -> throw new IllegalStateException("controls sheet: unknown vanilla key " + c.key);
        };
    }

    /**
     * Physical key state. The gear wheel shares key 1 with hotbar slot 1, and Minecraft only routes a key's
     * presses to one mapping, so we read the key itself instead of the mapping's click queue.
     */
    public static boolean isDown(Control c) {
        KeyMapping km = mapping(c);
        InputConstants.Key key = KeyBindingHelper.getBoundKeyOf(km);
        if (key.getType() != InputConstants.Type.KEYSYM || key.getValue() == InputConstants.UNKNOWN.getValue()) return km.isDown();
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), key.getValue());
    }

    public static boolean isDown(String controlId) { return isDown(Control.byId(controlId)); }
}
