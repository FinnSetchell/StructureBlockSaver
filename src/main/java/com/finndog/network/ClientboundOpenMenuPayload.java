package com.finndog.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record ClientboundOpenMenuPayload(List<StructureInfo> structures) implements CustomPacketPayload {
    public static final Type<ClientboundOpenMenuPayload> TYPE = new Type<>(ResourceLocation.parse("sbs:open_menu"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundOpenMenuPayload> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> payload.write(buf), ClientboundOpenMenuPayload::new
    );

    private ClientboundOpenMenuPayload(RegistryFriendlyByteBuf buf) {
        this(buf.readList(b -> new StructureInfo(b.readBlockPos(), b.readUtf(), new Vec3i(b.readInt(), b.readInt(), b.readInt()))));
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeCollection(structures, (b, s) -> {
            b.writeBlockPos(s.pos());
            b.writeUtf(s.name());
            b.writeInt(s.size().getX());
            b.writeInt(s.size().getY());
            b.writeInt(s.size().getZ());
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record StructureInfo(BlockPos pos, String name, Vec3i size) {}
}
