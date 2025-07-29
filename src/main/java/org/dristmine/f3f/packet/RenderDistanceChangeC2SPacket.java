package org.dristmine.f3f.packet;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

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
