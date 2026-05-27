package com.finndog.wand;

import com.finndog.network.ClearSelectionPayload;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StructureWand {

	public static final Map<UUID, BlockPos> pos1Map = new HashMap<>();
	public static final Map<UUID, BlockPos> pos2Map = new HashMap<>();

	public static void register() {
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (world.isClientSide()) return InteractionResult.PASS;
			if (!isWand(player.getItemInHand(hand))) return InteractionResult.PASS;

			BlockPos pos = hitResult.getBlockPos();
			if (pos.equals(pos1Map.get(player.getUUID()))) return InteractionResult.SUCCESS;
			pos1Map.put(player.getUUID(), pos);
			((ServerPlayer) player).sendSystemMessage(Component.literal("Position 1 set to " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()));
			return InteractionResult.SUCCESS;
		});

		AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
			if (world.isClientSide()) return InteractionResult.PASS;
			if (!isWand(player.getItemInHand(hand))) return InteractionResult.PASS;

			if (pos.equals(pos2Map.get(player.getUUID()))) return InteractionResult.SUCCESS;
			pos2Map.put(player.getUUID(), pos);
			((ServerPlayer) player).sendSystemMessage(Component.literal("Position 2 set to " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()));
			return InteractionResult.SUCCESS;
		});
	}

	public static LiteralArgumentBuilder<CommandSourceStack> wandSubcommand() {
		return Commands.literal("wand").executes(StructureWand::giveWand);
	}

	public static LiteralArgumentBuilder<CommandSourceStack> clearSubcommand() {
		return Commands.literal("clear").executes(StructureWand::clearSelection);
	}

	public static boolean isWand(ItemStack stack) {
		if (stack.getItem() != Items.WOODEN_AXE) return false;
		Component name = stack.get(DataComponents.CUSTOM_NAME);
		return name != null && name.getString().equals("Structure Wand");
	}

	private static ItemStack createWand() {
		ItemStack stack = new ItemStack(Items.WOODEN_AXE);
		stack.set(DataComponents.CUSTOM_NAME, Component.literal("Structure Wand"));
		return stack;
	}

	private static int giveWand(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		var player = ctx.getSource().getPlayerOrException();
		player.getInventory().add(createWand());
		ctx.getSource().sendSuccess(() -> Component.literal("Given Structure Wand."), false);
		return 1;
	}

	private static int clearSelection(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		pos1Map.remove(player.getUUID());
		pos2Map.remove(player.getUUID());
		if (ServerPlayNetworking.canSend(player, ClearSelectionPayload.TYPE)) {
			ServerPlayNetworking.send(player, new ClearSelectionPayload());
		}
		ctx.getSource().sendSuccess(() -> Component.literal("Selection cleared."), false);
		return 1;
	}
}
