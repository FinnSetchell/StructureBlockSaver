package com.finndog.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ExpandSelectionPayload(boolean isPos1, BlockPos newPos) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ExpandSelectionPayload> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("structure-block-saver", "move_selection"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ExpandSelectionPayload> STREAM_CODEC = StreamCodec.of(
        (buf, p) -> {
            buf.writeBoolean(p.isPos1());
            buf.writeBlockPos(p.newPos());
        },
        buf -> new ExpandSelectionPayload(buf.readBoolean(), buf.readBlockPos())
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
