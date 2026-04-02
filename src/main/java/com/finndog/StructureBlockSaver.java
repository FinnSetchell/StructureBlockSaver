package com.finndog;

import com.finndog.commands.SbsCommand;
import com.finndog.network.ClearSelectionPayload;
import com.finndog.wand.StructureWand;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.slf4j.Logger;

public class StructureBlockSaver implements ModInitializer {
	public static final String MOD_ID = "structure-block-saver";
	public static final Logger LOGGER = LogUtils.getLogger();

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.playS2C().register(ClearSelectionPayload.TYPE, ClearSelectionPayload.STREAM_CODEC);
		SbsCommand.register();
		StructureWand.register();
	}
}
