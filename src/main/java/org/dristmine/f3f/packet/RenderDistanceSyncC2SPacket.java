package org.dristmine.f3f.packet;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record RenderDistanceSyncC2SPacket(int renderDistance) implements CustomPayload {
    public static final Id<RenderDistanceSyncC2SPacket> ID = new Id<>(Identifier.of("f3f", "render_distance_sync"));

    public static final PacketCodec<PacketByteBuf, RenderDistanceSyncC2SPacket> CODEC = PacketCodec.of(
            RenderDistanceSyncC2SPacket::write,
            RenderDistanceSyncC2SPacket::new
    );

    public RenderDistanceSyncC2SPacket(PacketByteBuf buf) {
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
