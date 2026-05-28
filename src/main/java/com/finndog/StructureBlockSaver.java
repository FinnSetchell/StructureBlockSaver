package com.finndog;

import com.finndog.autosave.AutoSaveManager;
import com.finndog.commands.SbsCommand;
import com.finndog.network.ClearSelectionPayload;
import com.finndog.network.ClientboundSaveStructurePayload;
import com.finndog.network.ExpandSelectionPayload;
import com.finndog.utils.TickProcessor;
import com.finndog.wand.StructureWand;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.UUID;

public class StructureBlockSaver implements ModInitializer {
	public static final String MOD_ID = "structure-block-saver";
	public static final Logger LOGGER = LogUtils.getLogger();

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.playS2C().register(ClearSelectionPayload.TYPE, ClearSelectionPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(ClientboundSaveStructurePayload.TYPE, ClientboundSaveStructurePayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(com.finndog.network.ClientboundOpenMenuPayload.TYPE, com.finndog.network.ClientboundOpenMenuPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(com.finndog.network.ClientboundSyncSelectionPayload.TYPE, com.finndog.network.ClientboundSyncSelectionPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(ExpandSelectionPayload.TYPE, ExpandSelectionPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(com.finndog.network.ServerboundTargetedSavePayload.TYPE, com.finndog.network.ServerboundTargetedSavePayload.STREAM_CODEC);

		ServerPlayNetworking.registerGlobalReceiver(com.finndog.network.ServerboundTargetedSavePayload.TYPE, (payload, context) -> {
			com.finndog.utils.TickProcessor.submit(new com.finndog.utils.TargetedSaveTask(context.player(), payload.positions(), payload.localSave()));
		});

		ServerPlayNetworking.registerGlobalReceiver(ExpandSelectionPayload.TYPE, (payload, context) -> {
			context.server().execute(() -> {
				UUID uuid = context.player().getUUID();
				com.finndog.wand.WandSavedData state = com.finndog.wand.WandSavedData.getServerState(context.server());
				if (payload.isPos1()) {
					state.pos1Map.put(uuid, payload.newPos());
				}
				else {
					state.pos2Map.put(uuid, payload.newPos());
				}
				state.setDirty();
			});
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			UUID uuid = handler.player.getUUID();
			com.finndog.wand.WandSavedData state = com.finndog.wand.WandSavedData.getServerState(server);
			if (ServerPlayNetworking.canSend(handler.player, com.finndog.network.ClientboundSyncSelectionPayload.TYPE)) {
				ServerPlayNetworking.send(handler.player, new com.finndog.network.ClientboundSyncSelectionPayload(
					Optional.ofNullable(state.pos1Map.get(uuid)),
					Optional.ofNullable(state.pos2Map.get(uuid))
				));
			}
		});

		AutoSaveManager.register();
		SbsCommand.register();
		TickProcessor.register();
		StructureWand.register();
	}
}
