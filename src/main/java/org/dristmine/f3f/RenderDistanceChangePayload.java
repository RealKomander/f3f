package org.dristmine.f3f;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record RenderDistanceChangePayload(boolean increase) implements CustomPayload {
    public static final Identifier RENDER_DISTANCE_CHANGE_PAYLOAD_ID = Identifier.of(F3f.MOD_ID, "render_distance_change");
    public static final CustomPayload.Id<RenderDistanceChangePayload> ID = new CustomPayload.Id<>(RENDER_DISTANCE_CHANGE_PAYLOAD_ID);
    public static final PacketCodec<RegistryByteBuf, RenderDistanceChangePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOLEAN,
            RenderDistanceChangePayload::increase,
            RenderDistanceChangePayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}