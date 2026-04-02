package com.finndog;

import com.finndog.commands.SaveStructuresCommand;
import com.finndog.wand.StructureWand;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;

public class StructureBlockSaver implements ModInitializer {
	public static final String MOD_ID = "structure-block-saver";
	public static final Logger LOGGER = LogUtils.getLogger();

	@Override
	public void onInitialize() {
		SaveStructuresCommand.register();
		StructureWand.register();
	}
}
