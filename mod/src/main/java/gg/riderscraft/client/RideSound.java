package gg.riderscraft.client;

import gg.riderscraft.RidersCraft;
import gg.riderscraft.gen.Gear;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

/** Looping ride audio from Riders Republic (tyres, snow, wind, rocket) whose volume and pitch follow speed. */
public class RideSound extends AbstractTickableSoundInstance {
    private static RideSound playing;
    private final Gear gear;

    private RideSound(Gear gear) {
        super(RidersCraft.SOUNDS.get(gear.rrSound), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.gear = gear;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.01f;
        this.relative = true;
    }

    public static void tick(Minecraft mc, RiderState s) {
        boolean want = s.gear != Gear.ON_FOOT && RidersCraft.SOUNDS.containsKey(s.gear.rrSound);
        if (playing != null && (playing.isStopped() || playing.gear != s.gear)) playing = null;
        if (want && playing == null) {
            playing = new RideSound(s.gear);
            mc.getSoundManager().play(playing);
        }
    }

    @Override
    public void tick() {
        RiderState s = RiderState.LOCAL;
        var p = Minecraft.getInstance().player;
        if (p == null || s.gear != gear) { stop(); return; }
        boolean rolling = gear.physics == Gear.Physics.GROUND ? p.onGround() : s.gliding;
        float target = rolling ? (float) Math.min(1.0, s.speed / gear.maxSpeed * 1.4) : 0f;
        volume += (target - volume) * 0.2f;
        pitch = 0.8f + (float) Math.min(0.6, s.speed / gear.maxSpeed * 0.6);
    }
}
