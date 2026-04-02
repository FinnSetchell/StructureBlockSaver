package com.finndog.commands;

import com.finndog.wand.StructureWand;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.properties.StructureMode;

public class SaveStructuresCommand {

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			dispatcher.register(
				Commands.literal("savestructures")
					.requires(source -> source.hasPermission(2))
					.executes(SaveStructuresCommand::executeFromWand)
					.then(Commands.argument("pos1", BlockPosArgument.blockPos())
						.then(Commands.argument("pos2", BlockPosArgument.blockPos())
							.executes(SaveStructuresCommand::execute)))
			)
		);
	}

	private static int execute(CommandContext<CommandSourceStack> ctx) {
		BlockPos pos1 = BlockPosArgument.getBlockPos(ctx, "pos1");
		BlockPos pos2 = BlockPosArgument.getBlockPos(ctx, "pos2");
		return executeSave(ctx.getSource(), pos1, pos2);
	}

	private static int executeFromWand(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer player = source.getPlayerOrException();

		BlockPos pos1 = StructureWand.pos1Map.get(player.getUUID());
		BlockPos pos2 = StructureWand.pos2Map.get(player.getUUID());

		if (pos1 == null || pos2 == null) {
			source.sendFailure(Component.literal("Select both positions with the Structure Wand first."));
			return 0;
		}

		return executeSave(source, pos1, pos2);
	}

	private static int executeSave(CommandSourceStack source, BlockPos pos1, BlockPos pos2) {
		ServerLevel level = source.getLevel();
		int saved = 0;
		for (BlockPos pos : BlockPos.betweenClosed(pos1, pos2)) {
			BlockEntity be = level.getBlockEntity(pos);
			if (be instanceof StructureBlockEntity sbe && sbe.getMode() == StructureMode.SAVE) {
				if (sbe.saveStructure()) {
					saved++;
				}
			}
		}
		int count = saved;
		source.sendSuccess(() -> Component.literal("Saved " + count + " structure(s)."), true);
		return count;
	}
}
