package gg.riderscraft.client;

import gg.riderscraft.RidersCraft;
import gg.riderscraft.gen.Control;
import gg.riderscraft.gen.Gear;
import gg.riderscraft.gen.Trick;
import gg.riderscraft.rr.RRImporter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/** Riders Republic's trick loop: rotate and grab in the air, land clean to bank points into a combo. */
public final class TrickSystem {
    private static final float FLIP_RATE = 30f, SPIN_RATE = 36f, LAND_TOLERANCE = 100f;
    private static final int COMBO_WINDOW = 60, SHORT_AIR = 8;
    private static final String[] TRICK_KEYS = {"grab_left", "grab_right", "style", "flip_forward", "flip_back", "spin_left", "spin_right"};
    private static final boolean DEV = Boolean.getBoolean("riderscraft.devtest");
    private static boolean proximityActive;
    /** Keys already held at take-off (W to ride, Sprint to pedal) only count once released and pressed again. */
    private static final boolean[] latched = new boolean[TRICK_KEYS.length];

    public static void tick(Minecraft mc, LocalPlayer p, RiderState s) {
        s.prevSpin = s.spin; s.prevFlip = s.flip; s.prevRoll = s.roll;
        Gear g = s.gear;
        long now = p.level().getGameTime();
        s.callouts.removeIf(c -> now - c.bornTick() > 70);
        if (g == Gear.ON_FOOT) { s.resetJump(); return; }

        boolean inAir = g.physics == Gear.Physics.GLIDE ? s.gliding : !p.onGround() && !p.isInWater();
        if (inAir) {
            s.airTicks++;
            air(p, s, g);
        } else if (s.airTicks > 0) {
            land(mc, p, s, g, now);
        } else if (s.comboPoints > 0 && --s.comboTimeout <= 0) {
            s.totalScore += s.comboPoints;
            s.callout("Combo +" + s.comboPoints, 0xFFD24A, now);
            s.comboPoints = 0; s.comboMultiplier = 1;
        }
    }

    private static void air(LocalPlayer p, RiderState s, Gear g) {
        boolean[] held = new boolean[TRICK_KEYS.length];
        for (int i = 0; i < held.length; i++) {
            boolean down = Controls.isDown(TRICK_KEYS[i]);
            if (s.airTicks == 1) latched[i] = down;
            else if (!down) latched[i] = false;
            held[i] = down && !latched[i];
        }
        boolean modifier = held[0] || held[1] || held[2];

        if (s.airTicks > 2 && (!modifier || g.isSnow() || g.isBike())) {
            boolean flips = g.physics == Gear.Physics.GROUND || !modifier;
            if (flips && held[3]) s.flip += FLIP_RATE;
            if (flips && held[4]) s.flip -= FLIP_RATE;
            if (g.physics == Gear.Physics.GROUND && !held[2]) {
                if (held[5]) s.spin -= SPIN_RATE;
                if (held[6]) s.spin += SPIN_RATE;
            }
        }

        Trick match = modifier ? bestMatch(g.sport, held) : null;
        s.heldGrab = null;
        if (match != null && match.kind == Trick.Kind.GRAB) {
            s.heldGrab = match;
            s.jumpHoldPoints += match.holdPointsPerTick;
            if (!s.jumpTricks.contains(match)) s.jumpTricks.add(match);
        } else if (match != null && s.animTicksLeft == 0 && (match.kind == Trick.Kind.STYLE || match.kind == Trick.Kind.ROLL)) {
            s.animTrick = match;
            s.animTicksLeft = match.animTicks;
        }
        if (s.animTicksLeft > 0) {
            if (s.animTrick.kind == Trick.Kind.ROLL) s.roll += (held[5] ? -360f : 360f) / s.animTrick.animTicks;
            if (--s.animTicksLeft == 0) {
                s.jumpTricks.add(s.animTrick);
                s.roll = 0; s.prevRoll = 0;
                s.animTrick = null;
            }
        }
        if (g.physics == Gear.Physics.GLIDE) proximity(p, s);
    }

    /** The trick whose input is fully held with the most keys, so "grab_left+style" beats "grab_left". */
    private static Trick bestMatch(String sport, boolean[] held) {
        Trick best = null; int bestKeys = 0;
        for (Trick t : Trick.values()) {
            if (!t.forSport(sport) || t.kind == Trick.Kind.FLIP || t.kind == Trick.Kind.SPIN) continue;
            for (String[] alt : t.inputs) {
                boolean all = true;
                for (String k : alt) { int i = indexOf(k); if (i < 0 || !held[i]) { all = false; break; } }
                if (all && alt.length > bestKeys) { best = t; bestKeys = alt.length; }
            }
        }
        return best;
    }

    private static int indexOf(String k) { for (int i = 0; i < TRICK_KEYS.length; i++) if (TRICK_KEYS[i].equals(k)) return i; return -1; }

    private static void proximity(LocalPlayer p, RiderState s) {
        BlockPos base = p.blockPosition();
        boolean near = false;
        for (int dx = -2; dx <= 2 && !near; dx += 2)
            for (int dz = -2; dz <= 2 && !near; dz += 2)
                for (int dy = -3; dy <= 1 && !near; dy++)
                    if (!(dx == 0 && dz == 0 && dy >= 0) && !p.level().getBlockState(base.offset(dx, dy, dz)).isAir()) near = true;
        if (near && s.speed > 0.6) {
            s.jumpHoldPoints += Trick.PROXIMITY.holdPointsPerTick;
            if (!proximityActive) s.callout(name(Trick.PROXIMITY), 0x7CE0FF, p.level().getGameTime());
            if (!s.jumpTricks.contains(Trick.PROXIMITY)) s.jumpTricks.add(Trick.PROXIMITY);
        }
        proximityActive = near;
    }

    private static void land(Minecraft mc, LocalPlayer p, RiderState s, Gear g, long now) {
        float flipRem = Mth.wrapDegrees(s.flip), spinRem = Mth.wrapDegrees(s.spin);
        boolean upright = Math.abs(flipRem) < LAND_TOLERANCE;
        // Within LAND_TOLERANCE of upright the rider straightens out, like Riders Republic's landing assist.
        boolean switchLanding = g.isSnow() && Math.abs(Math.abs(spinRem) - 180) < 50f;
        boolean facing = Math.abs(spinRem) < LAND_TOLERANCE || switchLanding;
        boolean clean = upright && facing && s.animTicksLeft == 0 && s.heldGrab == null || g.physics == Gear.Physics.GLIDE && s.airTicks > 0 && upright;

        if (!clean && s.airTicks < SHORT_AIR) {
            // A tiny hop off a step: drop the unfinished trick rather than punish the rider.
            s.resetJump();
            return;
        }
        if (!clean) {
            if (DEV) RidersCraft.LOG.info("RRTEST bail flip={} spin={} anim={} grab={} air={}", s.flip, s.spin, s.animTrick, s.heldGrab, s.airTicks);
            s.callout(RRImporter.calloutText("bail", "Bail!"), 0xFF5A4A, now);
            s.comboPoints = 0; s.comboMultiplier = 1;
            s.speed *= 0.2; s.gvx = s.gvy = s.gvz = 0;
            p.playSound(RidersCraft.SOUNDS.get("sound_trick_bail"), 1f, 1f);
            s.resetJump();
            return;
        }
        List<String> names = new ArrayList<>();
        int points = s.jumpHoldPoints;
        int flips = Math.round(Math.abs(s.flip) / 360f);
        if (flips > 0) {
            Trick t = s.flip > 0 ? Trick.FRONTFLIP : Trick.BACKFLIP;
            names.add(multiple(flips) + name(t));
            points += t.points * flips;
        }
        int halfSpins = Math.round(Math.abs(s.spin) / 180f);
        if (halfSpins > 0 && g.physics == Gear.Physics.GROUND) {
            names.add(String.valueOf(halfSpins * 180));
            points += Trick.SPIN.points * halfSpins;
        }
        for (Trick t : s.jumpTricks) { names.add(name(t)); points += t.points; }
        if (switchLanding) names.add("Switch");

        if (points > 0) {
            int gained = points * s.comboMultiplier;
            s.comboPoints += gained;
            s.comboMultiplier = Math.min(10, s.comboMultiplier + 1);
            s.comboTimeout = COMBO_WINDOW;
            s.callout(String.join(" + ", names) + "  +" + gained, 0xFFFFFF, now);
            p.playSound(RidersCraft.SOUNDS.get("sound_trick_land"), 1f, 1f);
        }
        s.resetJump();
    }

    private static String multiple(int n) {
        return switch (n) { case 1 -> ""; case 2 -> "Double "; case 3 -> "Triple "; case 4 -> "Quad "; default -> n + "x "; };
    }

    /** Riders Republic's own callout text from the player's install, or the sheet name until it is imported. */
    public static String name(Trick t) { return RRImporter.calloutText(t.calloutKey, t.displayName); }
}
