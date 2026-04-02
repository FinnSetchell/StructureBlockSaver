package com.finndog.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ClearSelectionPayload() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ClearSelectionPayload> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("structure-block-saver", "clear_selection"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClearSelectionPayload> STREAM_CODEC =
        StreamCodec.unit(new ClearSelectionPayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
