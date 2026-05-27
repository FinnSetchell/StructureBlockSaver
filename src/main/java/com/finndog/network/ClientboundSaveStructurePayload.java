package com.finndog.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ClientboundSaveStructurePayload(String name, CompoundTag nbt) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<ClientboundSaveStructurePayload> TYPE =
		new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("structure-block-saver", "save_structure_client"));

	public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundSaveStructurePayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.STRING_UTF8, ClientboundSaveStructurePayload::name,
		ByteBufCodecs.COMPOUND_TAG, ClientboundSaveStructurePayload::nbt,
		ClientboundSaveStructurePayload::new
	);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
