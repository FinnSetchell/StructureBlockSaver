package com.finndog.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record ClientboundSyncSelectionPayload(Optional<BlockPos> pos1, Optional<BlockPos> pos2) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientboundSyncSelectionPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("structure_block_saver", "sync_selection"));
    public static final StreamCodec<FriendlyByteBuf, ClientboundSyncSelectionPayload> CODEC = StreamCodec.ofMember(ClientboundSyncSelectionPayload::write, ClientboundSyncSelectionPayload::new);

    public ClientboundSyncSelectionPayload(FriendlyByteBuf buf) {
        this(buf.readBoolean() ? Optional.of(buf.readBlockPos()) : Optional.empty(), buf.readBoolean() ? Optional.of(buf.readBlockPos()) : Optional.empty());
    }

    public void write(FriendlyByteBuf buf) {
        if (pos1.isPresent()) { buf.writeBoolean(true); buf.writeBlockPos(pos1.get()); } else { buf.writeBoolean(false); }
        if (pos2.isPresent()) { buf.writeBoolean(true); buf.writeBlockPos(pos2.get()); } else { buf.writeBoolean(false); }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
