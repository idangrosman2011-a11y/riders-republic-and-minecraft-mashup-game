package gg.riderscraft.client;

import gg.riderscraft.gen.Gear;
import gg.riderscraft.gen.Trick;

import java.util.ArrayList;
import java.util.List;

/** Everything about the local rider that physics, tricks, HUD and rendering share. Solo only for now. */
public final class RiderState {
    public static final RiderState LOCAL = new RiderState();

    public Gear gear = Gear.ON_FOOT;

    // ride physics
    public double speed;          // blocks/tick along heading (ground sports)
    public double heading;        // radians, Minecraft yaw convention
    public boolean gliding;
    public double gvx, gvy, gvz;  // glide velocity
    public float fuel;
    public double lastY;
    public boolean wasOnGround = true;

    // tricks in the current jump
    public int airTicks;
    public float spin, flip, roll;            // degrees accumulated this jump
    public float prevSpin, prevFlip, prevRoll; // for render interpolation
    public Trick animTrick;                   // style/roll trick currently playing
    public int animTicksLeft;
    public Trick heldGrab;
    public final List<Trick> jumpTricks = new ArrayList<>();
    public int jumpHoldPoints;

    // combo across landings
    public int comboPoints;
    public int comboMultiplier = 1;
    public int comboTimeout;
    public int totalScore;
    public final List<Callout> callouts = new ArrayList<>();

    public record Callout(String text, int color, long bornTick) {}

    public void resetJump() {
        airTicks = 0; spin = flip = roll = prevSpin = prevFlip = prevRoll = 0;
        animTrick = null; animTicksLeft = 0; heldGrab = null; jumpTricks.clear(); jumpHoldPoints = 0;
    }

    public void callout(String text, int color, long tick) {
        callouts.add(new Callout(text, color, tick));
        if (callouts.size() > 5) callouts.remove(0);
    }
}
