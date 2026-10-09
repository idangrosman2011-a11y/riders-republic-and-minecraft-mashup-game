package gg.riderscraft.client;

import gg.riderscraft.gen.Control;
import gg.riderscraft.gen.Gear;
import gg.riderscraft.gen.Surface;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Riders Republic style handling, driven entirely by the gear sheet. Runs after the vanilla player tick and sets the
 * velocity the next vanilla move will use; we keep our own speed so vanilla ground friction doesn't eat it.
 */
public final class RidePhysics {
    private static final double GRAVITY = 0.08;

    public static void tick(LocalPlayer p, RiderState s) {
        Gear g = s.gear;
        if (g.physics == Gear.Physics.VANILLA || p.isPassenger() || p.getAbilities().flying || p.isInWater()) {
            s.gliding = false;
            s.lastY = p.getY();
            s.wasOnGround = p.onGround();
            return;
        }
        if (g.physics == Gear.Physics.GROUND) ground(p, s, g);
        else glide(p, s, g);
        p.resetFallDistance();
        s.lastY = p.getY();
        s.wasOnGround = p.onGround();
    }

    public static Surface surfaceUnder(LocalPlayer p) {
        BlockPos below = BlockPos.containing(p.getX(), p.getY() - 0.2, p.getZ());
        var state = p.level().getBlockState(below);
        if (state.isAir()) state = p.level().getBlockState(below.above()); // snow layers sit in the feet block
        return Surface.of(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
    }

    private static void ground(LocalPlayer p, RiderState s, Gear g) {
        float forward = p.input.forwardImpulse;
        double dy = p.getY() - s.lastY;
        double target = Math.toRadians(p.getYRot());

        if (p.onGround()) {
            // Steer the heading toward where the rider looks; tighter turns scrub speed like a carve.
            double diff = Mth.wrapDegrees(Math.toDegrees(target - s.heading));
            double turn = Mth.clamp(diff, -g.turnRate, g.turnRate);
            s.heading += Math.toRadians(turn);
            s.speed *= 1.0 - Math.abs(turn) / 900.0;

            if (forward > 0) s.speed += g.pedalAccel * (Controls.isDown(Control.BOOST) && g.isBike() ? 1.6 : 1.0);
            if (forward < 0) s.speed *= 0.86; // brake / snowplough
            if (dy < 0) s.speed += -dy * g.slopeGain;
            else if (dy > 0 && !g.canClimb) s.speed -= dy * g.slopeGain * 1.5;

            Surface surf = surfaceUnder(p);
            double mul = g.isSnow() ? surf.snowFrictionMul : surf.bikeFrictionMul;
            s.speed *= 1.0 - Math.min(0.5, g.friction * mul);
            if (s.speed < 0.004 && forward <= 0) s.speed = 0;

            if (p.horizontalCollision) s.speed *= 0.3; // ran into something
            // Take-off: jump keeps momentum and gets the gear's pop.
            if (!s.wasOnGround) s.speed = Math.max(s.speed, p.getDeltaMovement().horizontalDistance());
        } else {
            s.speed *= 0.995; // air drag
            if (s.wasOnGround && p.getDeltaMovement().y > 0) p.setDeltaMovement(p.getDeltaMovement().add(0, g.jumpBoost, 0));
        }
        s.speed = Mth.clamp(s.speed, 0, g.maxSpeed);
        Vec3 v = p.getDeltaMovement();
        p.setDeltaMovement(-Math.sin(s.heading) * s.speed, v.y, Math.cos(s.heading) * s.speed);
    }

    private static void glide(LocalPlayer p, RiderState s, Gear g) {
        if (p.onGround()) {
            s.gliding = false;
            s.fuel = Math.min(g.boostFuelTicks, s.fuel + g.fuelRegen * 4);
            return;
        }
        if (!s.gliding) {
            // Deploy with jump once airborne and falling, like Riders Republic's air deploy.
            if (Controls.isDown(Control.DEPLOY) && p.getDeltaMovement().y < 0) {
                s.gliding = true;
                Vec3 v = p.getDeltaMovement();
                s.gvx = v.x; s.gvy = v.y; s.gvz = v.z;
            } else return;
        }
        // Elytra-style flight model with the wingsuit's own lift, dive and drag from the sheet.
        Vec3 look = p.getLookAngle();
        float pitch = p.getXRot() * Mth.DEG_TO_RAD;
        double horiz = Math.sqrt(look.x * look.x + look.z * look.z);
        double hv = Math.sqrt(s.gvx * s.gvx + s.gvz * s.gvz);
        double cos2 = Math.cos(pitch); cos2 *= cos2;
        s.gvy += GRAVITY * (-1.0 + cos2 * g.glideLift);
        if (s.gvy < 0 && horiz > 0) {
            double d = s.gvy * -g.glideDive * cos2;
            s.gvx += look.x * d / horiz; s.gvy += d; s.gvz += look.z * d / horiz;
        }
        if (pitch < 0 && horiz > 0) {
            double d = hv * -Mth.sin(pitch) * g.glideClimb;
            s.gvx -= look.x * d / horiz; s.gvy += d * 3.2; s.gvz -= look.z * d / horiz;
        }
        if (horiz > 0) {
            s.gvx += (look.x / horiz * hv - s.gvx) * 0.1;
            s.gvz += (look.z / horiz * hv - s.gvz) * 0.1;
        }
        if (g.boostAccel > 0) {
            if (Controls.isDown(Control.BOOST) && s.fuel > 0) {
                s.gvx += look.x * g.boostAccel; s.gvy += look.y * g.boostAccel; s.gvz += look.z * g.boostAccel;
                s.fuel -= 1;
            } else {
                s.fuel = Math.min(g.boostFuelTicks, s.fuel + g.fuelRegen);
            }
        }
        s.gvx *= g.glideDrag; s.gvy *= 0.98; s.gvz *= g.glideDrag;
        double len = Math.sqrt(s.gvx * s.gvx + s.gvy * s.gvy + s.gvz * s.gvz);
        if (len > g.maxSpeed) { double k = g.maxSpeed / len; s.gvx *= k; s.gvy *= k; s.gvz *= k; }
        if (p.horizontalCollision) { s.gvx *= 0.2; s.gvz *= 0.2; }
        if (p.verticalCollision && s.gvy > 0) s.gvy = 0;
        s.speed = Math.sqrt(s.gvx * s.gvx + s.gvz * s.gvz);
        // Vanilla moves by this velocity first and only then applies its own gravity and drag, which we overwrite next tick.
        p.setDeltaMovement(s.gvx, s.gvy, s.gvz);
    }
}
