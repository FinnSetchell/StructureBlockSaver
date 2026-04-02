package com.finndog.commands;

import com.finndog.wand.StructureWand;
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

	//////////////////////////////

	private static int fromWandSave(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		return fromWand(ctx, null, false);
	}

	private static int fromWandDryRun(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		return fromWand(ctx, null, true);
	}

	private static int fromWandFilter(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		return fromWand(ctx, StringArgumentType.getString(ctx, "filter"), false);
	}

	private static int fromWandDryRunFilter(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		return fromWand(ctx, StringArgumentType.getString(ctx, "filter"), true);
	}

	private static int explicit(CommandContext<CommandSourceStack> ctx) {
		return withArgs(ctx, null, false);
	}

	private static int explicitDryRun(CommandContext<CommandSourceStack> ctx) {
		return withArgs(ctx, null, true);
	}

	private static int explicitFilter(CommandContext<CommandSourceStack> ctx) {
		return withArgs(ctx, StringArgumentType.getString(ctx, "filter"), false);
	}

	private static int explicitDryRunFilter(CommandContext<CommandSourceStack> ctx) {
		return withArgs(ctx, StringArgumentType.getString(ctx, "filter"), true);
	}

	//////////////////////////////

	private static int fromWand(CommandContext<CommandSourceStack> ctx, String filter, boolean dryRun) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer player = source.getPlayerOrException();
		BlockPos pos1 = StructureWand.pos1Map.get(player.getUUID());
		BlockPos pos2 = StructureWand.pos2Map.get(player.getUUID());
		if (pos1 == null || pos2 == null) {
			source.sendFailure(Component.literal("Select both positions with the Structure Wand first."));
			return 0;
		}
		return executeSave(source, pos1, pos2, filter, dryRun);
	}

	private static int withArgs(CommandContext<CommandSourceStack> ctx, String filter, boolean dryRun) {
		return executeSave(ctx.getSource(),
			BlockPosArgument.getBlockPos(ctx, "pos1"),
			BlockPosArgument.getBlockPos(ctx, "pos2"),
			filter, dryRun);
	}

	private static int executeSave(CommandSourceStack source, BlockPos pos1, BlockPos pos2, String filter, boolean dryRun) {
		ServerLevel level = source.getLevel();
		List<String> names = new ArrayList<>();

		for (BlockPos pos : BlockPos.betweenClosed(pos1, pos2)) {
			if (!level.isLoaded(pos)) continue;
			BlockEntity be = level.getBlockEntity(pos);
			if (!(be instanceof StructureBlockEntity sbe)) continue;
			if (sbe.getMode() != StructureMode.SAVE) continue;
			String name = sbe.getStructureName();
			if (filter != null && !matchesFilter(name, filter)) continue;
			if (dryRun || sbe.saveStructure()) {
				names.add(name);
			}
		}

		String prefix = dryRun ? "[Dry Run] Found " : "Saved ";
		StringBuilder sb = new StringBuilder(prefix).append(names.size()).append(" structure(s)");
		if (!names.isEmpty()) {
			sb.append(":");
			for (String name : names) sb.append("\n  - ").append(name);
		}
		else {
			sb.append(".");
		}

		String msg = sb.toString();
		source.sendSuccess(() -> Component.literal(msg), !dryRun);
		return names.size();
	}

	//////////////////////////////

	public static LiteralArgumentBuilder<CommandSourceStack> listSubcommand() {
		return Commands.literal("list")
			.executes(SaveStructuresCommand::fromWandList)
			.then(Commands.argument("pos1", BlockPosArgument.blockPos())
				.then(Commands.argument("pos2", BlockPosArgument.blockPos())
					.executes(SaveStructuresCommand::explicitList)));
	}

	private static int fromWandList(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer player = source.getPlayerOrException();
		BlockPos pos1 = StructureWand.pos1Map.get(player.getUUID());
		BlockPos pos2 = StructureWand.pos2Map.get(player.getUUID());
		if (pos1 == null || pos2 == null) {
			source.sendFailure(Component.literal("Select both positions with the Structure Wand first."));
			return 0;
		}
		return executeList(source, pos1, pos2);
	}

	private static int explicitList(CommandContext<CommandSourceStack> ctx) {
		return executeList(ctx.getSource(),
			BlockPosArgument.getBlockPos(ctx, "pos1"),
			BlockPosArgument.getBlockPos(ctx, "pos2"));
	}

	private static int executeList(CommandSourceStack source, BlockPos pos1, BlockPos pos2) {
		ServerLevel level = source.getLevel();
		List<String> lines = new ArrayList<>();

		for (BlockPos pos : BlockPos.betweenClosed(pos1, pos2)) {
			if (!level.isLoaded(pos)) continue;
			BlockEntity be = level.getBlockEntity(pos);
			if (!(be instanceof StructureBlockEntity sbe)) continue;
			Vec3i size = sbe.getStructureSize();
			lines.add("[" + sbe.getMode().name() + "] " + sbe.getStructureName()
				+ " (" + size.getX() + "x" + size.getY() + "x" + size.getZ() + ")");
		}

		StringBuilder sb = new StringBuilder("Found ").append(lines.size()).append(" structure block(s)");
		if (!lines.isEmpty()) {
			sb.append(":");
			for (String line : lines) sb.append("\n  - ").append(line);
		}
		else {
			sb.append(".");
		}

		String msg = sb.toString();
		source.sendSuccess(() -> Component.literal(msg), false);
		return lines.size();
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
