package com.finndog.commands;

import com.finndog.autosave.AutoSaveManager;
import com.finndog.wand.StructureWand;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;

public class SbsCommand {

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			dispatcher.register(
				Commands.literal("sbs")
					.requires(source -> source.hasPermission(2))
					.then(StructureWand.wandSubcommand())
					.then(StructureWand.clearSubcommand())
					.then(SaveStructuresCommand.saveSubcommand())
					.then(SaveStructuresCommand.filteredsaveSubcommand())
					.then(SaveStructuresCommand.listSubcommand())
					.then(AutoSaveManager.subcommand())
			)
		);
	}
}
