package gg.riderscraft.client;

import gg.riderscraft.RidersCraft;
import gg.riderscraft.gen.Gear;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

/**
 * Developer play-test, only active with -Driderscraft.devtest=true: makes a world, rides each sport down a snowy
 * mountain with scripted inputs, logs "RRTEST" lines and takes screenshots, then quits. Never runs for players.
 */
public final class DevTest {
    private static int t = -1, phaseStart, trickNo;
    private static boolean started, slopeReady;
    private static String phase = "wait";
    private static double startX, startZ, peakSpeed;
    private static final java.util.Set<String> shots = new java.util.HashSet<>();

    static void install() { ClientTickEvents.END_CLIENT_TICK.register(DevTest::tick); }

    private static void log(String fmt, Object... a) { RidersCraft.LOG.info("RRTEST " + fmt, a); }

    private static void shot(Minecraft mc, String name) {
        if (!shots.add(name)) return;
        Screenshot.grab(mc.gameDirectory, "rrtest-" + name + ".png", mc.getMainRenderTarget(), c -> {});
        log("screenshot {}", name);
    }

    private static void hold(KeyMapping k, boolean down) { k.setDown(down); }

    private static void begin(Minecraft mc, String next) {
        log("{} result: travelled {} blocks, peak {} km/h, banked {} + combo {}", phase,
                Math.round(Math.hypot(mc.player.getX() - startX, mc.player.getZ() - startZ)), Math.round(peakSpeed * 72),
                RiderState.LOCAL.totalScore, RiderState.LOCAL.comboPoints);
        for (KeyMapping k : mc.options.keyMappings) k.setDown(false);
        phase = next; phaseStart = t; peakSpeed = 0; startX = mc.player.getX(); startZ = mc.player.getZ();
    }

    private static void tick(Minecraft mc) {
        if (!started && mc.screen instanceof TitleScreen && mc.level == null) {
            started = true;
            mc.createWorldOpenFlows().createFreshLevel("rr-test",
                    new LevelSettings("rr-test", GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
                    new WorldOptions(Long.getLong("riderscraft.seed", 4242L), false, false), WorldPresets::createNormalWorldDimensions, mc.screen);
            return;
        }
        if (mc.player == null || mc.getSingleplayerServer() == null) return;
        t++;
        var o = mc.options;
        RiderState s = RiderState.LOCAL;
        int pt = t - phaseStart;
        peakSpeed = Math.max(peakSpeed, s.speed);
        if (t == 40) mc.getSingleplayerServer().execute(() -> { toSlope(mc, 0); slopeReady = true; });

        switch (phase) {
            case "wait" -> {
                if (slopeReady && t > 60) {
                    o.setCameraType(CameraType.THIRD_PERSON_BACK);
                    RidersCraftClient.equip(Gear.SNOWBOARD);
                    begin(mc, "snowboard");
                }
            }
            case "snowboard", "skis", "mountain_bike" -> {
                ride(mc, s, pt);
                if (pt == 60) shot(mc, phase);
                if (pt >= 300) {
                    String next = phase.equals("snowboard") ? "skis" : phase.equals("skis") ? "mountain_bike" : "wheel";
                    if (!next.equals("wheel")) {
                        slopeReady = false;
                        mc.getSingleplayerServer().execute(() -> { toSlope(mc, 0); slopeReady = true; });
                        RidersCraftClient.equip(next.equals("skis") ? Gear.SKIS : Gear.MOUNTAIN_BIKE);
                    }
                    begin(mc, next);
                }
            }
            case "wheel" -> {
                hold(Controls.mapping(gg.riderscraft.gen.Control.GEAR_WHEEL), pt < 30);
                if (pt == 2) mc.setScreen(new GearWheelScreen());
                if (pt == 20) shot(mc, "wheel");
                if (pt == 40) {
                    mc.getSingleplayerServer().execute(() -> {
                        ServerPlayer sp = mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
                        sp.teleportTo(sp.getX(), sp.getY() + 90, sp.getZ());
                    });
                    RidersCraftClient.equip(Gear.ROCKET_WINGSUIT);
                    begin(mc, "rocket_wingsuit");
                }
            }
            case "rocket_wingsuit" -> {
                hold(o.keyJump, pt >= 15 && pt < 18);
                mc.player.setXRot(pt < 90 ? 25f : -10f);
                hold(o.keySprint, pt > 90 && pt < 140);
                if (pt % 15 == 0) log("wingsuit pt={} gliding={} speed={} kmh y={} fuel={} callouts={}", pt, s.gliding, Math.round(s.speed * 72),
                        Math.round(mc.player.getY()), Math.round(s.fuel), s.callouts.stream().map(RiderState.Callout::text).toList());
                if (pt == 70) shot(mc, "wingsuit");
                if (pt == 110) shot(mc, "rocket");
                if (pt == 170) {
                    begin(mc, "done");
                    log("health {}", mc.player.getHealth());
                    log("done");
                    mc.stop();
                }
            }
            default -> {}
        }
    }

    /** Plays like a rider: steer down the fall line, pop off bumps, throw one trick per jump and stop rotating to land. */
    private static void ride(Minecraft mc, RiderState s, int pt) {
        var o = mc.options;
        var p = mc.player;
        if (!slopeReady) return;
        hold(o.keyUp, true);
        hold(o.keySprint, s.gear.isBike());
        if (pt % 5 == 0 && p.onGround()) {
            float best = p.getYRot(); int lowest = Integer.MAX_VALUE;
            for (int d = -60; d <= 60; d += 15) {
                double r = Math.toRadians(p.getYRot() + d);
                int y = mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(p.getX() - Math.sin(r) * 10), (int) Math.floor(p.getZ() + Math.cos(r) * 10));
                if (y < lowest || y == lowest && Math.abs(d) < Math.abs(best - p.getYRot())) { lowest = y; best = p.getYRot() + d; }
            }
            p.setYRot(p.getYRot() + (best - p.getYRot()) * 0.5f);
        }
        boolean air = !p.onGround();
        hold(o.keyJump, !air && pt % 30 == 0 && pt > 20);
        if (air && s.airTicks == 1) { jumped = p.getDeltaMovement().y > 0.3; if (jumped) trickNo++; }
        int kind = jumped ? trickNo % 3 : -1;
        KeyMapping grab = Controls.mapping(gg.riderscraft.gen.Control.GRAB_LEFT);
        hold(o.keyDown, air && s.airTicks >= 2 && kind == 0 && s.flip > -345);
        hold(o.keyRight, air && s.airTicks >= 2 && kind == 1 && s.spin < 345);
        hold(grab, air && s.airTicks >= 2 && s.airTicks < (s.gear.isBike() ? 4 : 9) && kind == 2);
        if (air && Math.abs(s.flip) > 120 && Math.abs(s.flip) < 240) shot(mc, phase + "-flip");
        if (air && kind == 2 && s.heldGrab != null) shot(mc, phase + "-grab");
        if (pt % 20 == 0) log("{} pt={} speed={} kmh ground={} surface={} callouts={}", phase, pt, Math.round(s.speed * 72), p.onGround(),
                RidePhysics.surfaceUnder(p).label, s.callouts.stream().map(RiderState.Callout::text).toList());
    }

    /** Teleports the player to the top of a long snowy run (not ending in water), facing down it. */
    private static void toSlope(Minecraft mc, int attempt) {
        ServerLevel level = mc.getSingleplayerServer().overworld();
        ServerPlayer sp = mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
        level.setDayTime(6000);
        level.setWeatherParameters(6000, 0, false, false);
        int bestDrop = -1; double bx = 0, by = 0, bz = 0; float byaw = 0;
        for (int k = 0; k < 6; k++) {
            BlockPos origin = new BlockPos(k * 500, 100, (k % 2) * 500 + slopesUsed * 900);
            var found = level.findClosestBiome3d(h -> h.is(Biomes.SNOWY_SLOPES), origin, 3000, 32, 64);
            if (found == null) continue;
            int x = found.getFirst().getX(), z = found.getFirst().getZ(), y = height(level, x, z);
            for (int i = 0; i < 40; i++) {
                int nx = x, nz = z, ny = y;
                for (int dx = -6; dx <= 6; dx += 6) for (int dz = -6; dz <= 6; dz += 6) {
                    int hy = height(level, x + dx, z + dz);
                    if (hy > ny) { nx = x + dx; nz = z + dz; ny = hy; }
                }
                if (ny == y) break;
                x = nx; z = nz; y = ny;
            }
            for (int a = 0; a < 360; a += 15) {
                double r = Math.toRadians(a);
                int tx = x + (int) (-Math.sin(r) * 40), tz = z + (int) (Math.cos(r) * 40);
                int ty = height(level, tx, tz);
                if (!level.getFluidState(new BlockPos(tx, ty - 1, tz)).isEmpty()) continue;
                if (y - ty > bestDrop) { bestDrop = y - ty; bx = x + 0.5; by = y + 0.5; bz = z + 0.5; byaw = a; }
            }
            if (bestDrop >= 18) break;
        }
        slopesUsed++;
        sp.teleportTo(level, bx, by, bz, byaw, 10f);
        log("slope at {} {} {} facing {} (drop {} over 40 blocks)", Math.round(bx), Math.round(by), Math.round(bz), byaw, bestDrop);
    }

    private static int slopesUsed;
    private static boolean jumped;

    private static int height(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
    }
}
