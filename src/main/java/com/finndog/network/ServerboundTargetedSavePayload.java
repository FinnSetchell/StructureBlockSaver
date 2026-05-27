package com.finndog.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record ServerboundTargetedSavePayload(List<BlockPos> positions, boolean localSave) implements CustomPacketPayload {
    public static final Type<ServerboundTargetedSavePayload> TYPE = new Type<>(ResourceLocation.parse("sbs:targeted_save"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundTargetedSavePayload> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> payload.write(buf), ServerboundTargetedSavePayload::new
    );

    private ServerboundTargetedSavePayload(RegistryFriendlyByteBuf buf) {
        this(buf.readList(RegistryFriendlyByteBuf::readBlockPos), buf.readBoolean());
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeCollection(positions, RegistryFriendlyByteBuf::writeBlockPos);
        buf.writeBoolean(localSave);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
