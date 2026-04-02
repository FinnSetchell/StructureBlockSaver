package com.finndog;

import com.finndog.autosave.AutoSaveManager;
import com.finndog.commands.SbsCommand;
import com.finndog.network.ClearSelectionPayload;
import com.finndog.network.ExpandSelectionPayload;
import com.finndog.wand.StructureWand;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.slf4j.Logger;

import java.util.UUID;

public class StructureBlockSaver implements ModInitializer {
	public static final String MOD_ID = "structure-block-saver";
	public static final Logger LOGGER = LogUtils.getLogger();

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.playS2C().register(ClearSelectionPayload.TYPE, ClearSelectionPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(ExpandSelectionPayload.TYPE, ExpandSelectionPayload.STREAM_CODEC);

		ServerPlayNetworking.registerGlobalReceiver(ExpandSelectionPayload.TYPE, (payload, context) -> {
			context.player().server.execute(() -> {
				UUID uuid = context.player().getUUID();
				if (payload.isPos1()) {
					StructureWand.pos1Map.put(uuid, payload.newPos());
				}
				else {
					StructureWand.pos2Map.put(uuid, payload.newPos());
				}
			});
		});

		AutoSaveManager.register();
		SbsCommand.register();
		StructureWand.register();
	}
}
