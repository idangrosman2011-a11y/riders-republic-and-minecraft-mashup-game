package gg.riderscraft.net;

import gg.riderscraft.RidersCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: the gear the player just picked on the wheel (Gear ordinal). */
public record GearPayload(int gear) implements CustomPacketPayload {
    public static final Type<GearPayload> TYPE = new Type<>(RidersCraft.id("gear"));
    public static final StreamCodec<ByteBuf, GearPayload> CODEC =
            ByteBufCodecs.VAR_INT.map(GearPayload::new, GearPayload::gear);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
