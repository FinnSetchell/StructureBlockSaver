package com.finndog.commands;

import com.finndog.wand.StructureWand;
import com.finndog.wand.WandSavedData;
import com.finndog.utils.StructureScanTask;
import com.finndog.utils.TickProcessor;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.properties.StructureMode;

import java.util.ArrayList;
import java.util.List;

public class SaveStructuresCommand {

	public static LiteralArgumentBuilder<CommandSourceStack> filteredsaveSubcommand() {
		return Commands.literal("filteredsave")
			.then(Commands.argument("filter", StringArgumentType.word())
				.executes(SaveStructuresCommand::fromWandFilter)
				.then(Commands.literal("dry-run")
					.executes(SaveStructuresCommand::fromWandDryRunFilter)
					.then(Commands.argument("pos1", BlockPosArgument.blockPos())
						.then(Commands.argument("pos2", BlockPosArgument.blockPos())
							.executes(SaveStructuresCommand::explicitDryRunFilter))))
				.then(Commands.argument("pos1", BlockPosArgument.blockPos())
					.then(Commands.argument("pos2", BlockPosArgument.blockPos())
						.executes(SaveStructuresCommand::explicitFilter))));
	}

	public static LiteralArgumentBuilder<CommandSourceStack> filteredlocalsaveSubcommand() {
		return Commands.literal("filteredlocalsave")
			.then(Commands.argument("filter", StringArgumentType.word())
				.executes(SaveStructuresCommand::fromWandFilterLocal)
				.then(Commands.argument("pos1", BlockPosArgument.blockPos())
					.then(Commands.argument("pos2", BlockPosArgument.blockPos())
						.executes(SaveStructuresCommand::explicitFilterLocal))));
	}

	public static LiteralArgumentBuilder<CommandSourceStack> saveSubcommand() {
		return Commands.literal("save")
			.executes(SaveStructuresCommand::fromWandSave)
			.then(Commands.literal("dry-run")
				.executes(SaveStructuresCommand::fromWandDryRun)
				.then(Commands.argument("pos1", BlockPosArgument.blockPos())
					.then(Commands.argument("pos2", BlockPosArgument.blockPos())
						.executes(SaveStructuresCommand::explicitDryRun)
						.then(Commands.argument("filter", StringArgumentType.word())
							.executes(SaveStructuresCommand::explicitDryRunFilter)))))
			.then(Commands.argument("pos1", BlockPosArgument.blockPos())
				.then(Commands.argument("pos2", BlockPosArgument.blockPos())
					.executes(SaveStructuresCommand::explicit)
					.then(Commands.argument("filter", StringArgumentType.word())
						.executes(SaveStructuresCommand::explicitFilter))));
	}

	public static LiteralArgumentBuilder<CommandSourceStack> localsaveSubcommand() {
		return Commands.literal("localsave")
			.executes(SaveStructuresCommand::fromWandSaveLocal)
			.then(Commands.argument("pos1", BlockPosArgument.blockPos())
				.then(Commands.argument("pos2", BlockPosArgument.blockPos())
					.executes(SaveStructuresCommand::explicitLocal)
					.then(Commands.argument("filter", StringArgumentType.word())
						.executes(SaveStructuresCommand::explicitFilterLocal))));
	}

	//////////////////////////////

	private static int fromWandSave(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		return fromWand(ctx, null, false, false);
	}

	private static int fromWandSaveLocal(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		return fromWand(ctx, null, false, true);
	}

	private static int fromWandDryRun(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		return fromWand(ctx, null, true, false);
	}

	private static int fromWandFilter(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		return fromWand(ctx, StringArgumentType.getString(ctx, "filter"), false, false);
	}

	private static int fromWandFilterLocal(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		return fromWand(ctx, StringArgumentType.getString(ctx, "filter"), false, true);
	}

	private static int fromWandDryRunFilter(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		return fromWand(ctx, StringArgumentType.getString(ctx, "filter"), true, false);
	}

	private static int explicit(CommandContext<CommandSourceStack> ctx) {
		return withArgs(ctx, null, false, false);
	}

	private static int explicitLocal(CommandContext<CommandSourceStack> ctx) {
		return withArgs(ctx, null, false, true);
	}

	private static int explicitDryRun(CommandContext<CommandSourceStack> ctx) {
		return withArgs(ctx, null, true, false);
	}

	private static int explicitFilter(CommandContext<CommandSourceStack> ctx) {
		return withArgs(ctx, StringArgumentType.getString(ctx, "filter"), false, false);
	}

	private static int explicitFilterLocal(CommandContext<CommandSourceStack> ctx) {
		return withArgs(ctx, StringArgumentType.getString(ctx, "filter"), false, true);
	}

	private static int explicitDryRunFilter(CommandContext<CommandSourceStack> ctx) {
		return withArgs(ctx, StringArgumentType.getString(ctx, "filter"), true, false);
	}

	//////////////////////////////

	private static int fromWand(CommandContext<CommandSourceStack> ctx, String filter, boolean dryRun, boolean localSave) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer player = source.getPlayerOrException();
		WandSavedData state = WandSavedData.getServerState(((net.minecraft.server.level.ServerLevel) player.level()).getServer());
		BlockPos pos1 = state.pos1Map.get(player.getUUID());
		BlockPos pos2 = state.pos2Map.get(player.getUUID());
		if (pos1 == null || pos2 == null) {
			source.sendFailure(Component.literal("Select both positions with the Structure Wand first."));
			return 0;
		}
		return executeSave(source, pos1, pos2, filter, dryRun, localSave);
	}

	private static int withArgs(CommandContext<CommandSourceStack> ctx, String filter, boolean dryRun, boolean localSave) {
		return executeSave(ctx.getSource(),
			BlockPosArgument.getBlockPos(ctx, "pos1"),
			BlockPosArgument.getBlockPos(ctx, "pos2"),
			filter, dryRun, localSave);
	}

	private static int executeSave(CommandSourceStack source, BlockPos pos1, BlockPos pos2, String filter, boolean dryRun, boolean localSave) {
		StructureScanTask task = new StructureScanTask(source, pos1, pos2, filter, dryRun, false, false, localSave, false);
		TickProcessor.submit(task);
		return 1;
	}

	//////////////////////////////

	public static LiteralArgumentBuilder<CommandSourceStack> listSubcommand() {
		return Commands.literal("list")
			.executes(SaveStructuresCommand::fromWandList)
			.then(Commands.argument("pos1", BlockPosArgument.blockPos())
				.then(Commands.argument("pos2", BlockPosArgument.blockPos())
					.executes(SaveStructuresCommand::explicitList)));
	}

	public static LiteralArgumentBuilder<CommandSourceStack> menuSubcommand() {
		return Commands.literal("menu")
			.executes(SaveStructuresCommand::fromWandMenu)
			.then(Commands.argument("pos1", BlockPosArgument.blockPos())
				.then(Commands.argument("pos2", BlockPosArgument.blockPos())
					.executes(SaveStructuresCommand::explicitMenu)));
	}

	private static int fromWandList(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		return executeListOrMenu(ctx, false);
	}

	private static int fromWandMenu(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		return executeListOrMenu(ctx, true);
	}

	private static int executeListOrMenu(CommandContext<CommandSourceStack> ctx, boolean menu) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer player = source.getPlayerOrException();
		WandSavedData state = WandSavedData.getServerState(((net.minecraft.server.level.ServerLevel) player.level()).getServer());
		BlockPos pos1 = state.pos1Map.get(player.getUUID());
		BlockPos pos2 = state.pos2Map.get(player.getUUID());
		if (pos1 == null || pos2 == null) {
			source.sendFailure(Component.literal("Select both positions with the Structure Wand first."));
			return 0;
		}
		StructureScanTask task = new StructureScanTask(source, pos1, pos2, null, false, !menu, false, false, menu);
		TickProcessor.submit(task);
		return 1;
	}

	private static int explicitList(CommandContext<CommandSourceStack> ctx) {
		return executeExplicitListOrMenu(ctx, false);
	}

	private static int explicitMenu(CommandContext<CommandSourceStack> ctx) {
		return executeExplicitListOrMenu(ctx, true);
	}

	private static int executeExplicitListOrMenu(CommandContext<CommandSourceStack> ctx, boolean menu) {
		CommandSourceStack source = ctx.getSource();
		BlockPos pos1 = BlockPosArgument.getBlockPos(ctx, "pos1");
		BlockPos pos2 = BlockPosArgument.getBlockPos(ctx, "pos2");
		StructureScanTask task = new StructureScanTask(source, pos1, pos2, null, false, !menu, false, false, menu);
		TickProcessor.submit(task);
		return 1;
	}

	//////////////////////////////

	private static boolean matchesFilter(String name, String filter) {
		if (!filter.contains("*")) return name.equals(filter);
		String[] parts = filter.split("\\*", -1);
		if (!name.startsWith(parts[0])) return false;
		int cursor = parts[0].length();
		for (int i = 1; i < parts.length; i++) {
			int idx = name.indexOf(parts[i], cursor);
			if (idx < 0) return false;
			cursor = idx + parts[i].length();
		}
		return true;
	}
}
