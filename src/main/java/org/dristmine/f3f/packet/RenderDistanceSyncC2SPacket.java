package org.dristmine.f3f.packet;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

//? if >=1.20.5 {
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

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
//?} else {
/*public record RenderDistanceSyncC2SPacket(int renderDistance) {
    public static final Identifier ID = new Identifier("f3f", "render_distance_sync");

    public RenderDistanceSyncC2SPacket(PacketByteBuf buf) {
        this(buf.readInt());
    }

    public void write(PacketByteBuf buf) {
        buf.writeInt(renderDistance);
    }

    public static RenderDistanceSyncC2SPacket read(PacketByteBuf buf) {
        return new RenderDistanceSyncC2SPacket(buf);
    }
}*/
//?}
