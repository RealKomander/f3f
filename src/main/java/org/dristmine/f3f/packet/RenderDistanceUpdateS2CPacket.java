package org.dristmine.f3f.packet;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record RenderDistanceUpdateS2CPacket(int renderDistance) implements CustomPayload {
    public static final Id<RenderDistanceUpdateS2CPacket> ID = new Id<>(Identifier.of("f3f", "render_distance_update"));

    public static final PacketCodec<PacketByteBuf, RenderDistanceUpdateS2CPacket> CODEC = PacketCodec.of(
            RenderDistanceUpdateS2CPacket::write,
            RenderDistanceUpdateS2CPacket::new
    );

    public RenderDistanceUpdateS2CPacket(PacketByteBuf buf) {
        this(buf.readInt());
    }

    public void write(PacketByteBuf buf) {
        buf.writeInt(renderDistance);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
