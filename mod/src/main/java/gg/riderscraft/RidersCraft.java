package gg.riderscraft;

import gg.riderscraft.gen.Gear;
import gg.riderscraft.net.GearPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RidersCraft implements ModInitializer {
    public static final String MOD_ID = "riderscraft";
    public static final Logger LOG = LoggerFactory.getLogger("Riders Craft");
    private static final ResourceLocation STEP_MODIFIER = id("gear_step_height");

    /** Server-side record of what each player is riding. */
    private static final Map<UUID, Gear> RIDING = new HashMap<>();

    public static final Map<String, SoundEvent> SOUNDS = new HashMap<>();

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        for (String key : new String[]{"sound_bike_roll", "sound_snow_carve", "sound_wind", "sound_rocket", "sound_trick_land", "sound_trick_bail"}) {
            ResourceLocation rl = id(key);
            SOUNDS.put(key, Registry.register(BuiltInRegistries.SOUND_EVENT, rl, SoundEvent.createVariableRangeEvent(rl)));
        }

        PayloadTypeRegistry.playC2S().register(GearPayload.TYPE, GearPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(GearPayload.TYPE, (payload, context) ->
                context.server().execute(() -> setGear(context.player(), Gear.byId(payload.gear()))));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> RIDING.remove(handler.getPlayer().getUUID()));

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                Gear gear = RIDING.getOrDefault(player.getUUID(), Gear.ON_FOOT);
                // Riders Republic has no fall damage while riding: landings are judged by the trick system instead.
                if (gear != Gear.ON_FOOT) player.resetFallDistance();
            }
        });
        LOG.info("Riders Craft loaded: {} sports", Gear.values().length - 1);
    }

    private static void setGear(ServerPlayer player, Gear gear) {
        RIDING.put(player.getUUID(), gear);
        AttributeInstance step = player.getAttribute(Attributes.STEP_HEIGHT);
        if (step == null) return;
        step.removeModifier(STEP_MODIFIER);
        double extra = gear.stepHeight - step.getBaseValue();
        if (extra > 0) step.addTransientModifier(new AttributeModifier(STEP_MODIFIER, extra, AttributeModifier.Operation.ADD_VALUE));
    }
}
