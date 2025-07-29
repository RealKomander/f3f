package org.dristmine.f3f.packet;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

//? if >=1.20.5 {
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record RenderDistanceChangeC2SPacket(boolean increase) implements CustomPayload {
    public static final Id<RenderDistanceChangeC2SPacket> ID = new Id<>(Identifier.of("f3f", "render_distance_change"));

    public static final PacketCodec<PacketByteBuf, RenderDistanceChangeC2SPacket> CODEC = PacketCodec.of(
            RenderDistanceChangeC2SPacket::write,
            RenderDistanceChangeC2SPacket::new
    );

    public RenderDistanceChangeC2SPacket(PacketByteBuf buf) {
        this(buf.readBoolean());
    }

    public void write(PacketByteBuf buf) {
        buf.writeBoolean(increase);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
//?} else {
/*public record RenderDistanceChangeC2SPacket(boolean increase) {
    public static final Identifier ID = new Identifier("f3f", "render_distance_change");

    public RenderDistanceChangeC2SPacket(PacketByteBuf buf) {
        this(buf.readBoolean());
    }

    public void write(PacketByteBuf buf) {
        buf.writeBoolean(increase);
    }

    public static RenderDistanceChangeC2SPacket read(PacketByteBuf buf) {
        return new RenderDistanceChangeC2SPacket(buf);
    }
}*/
//?}
