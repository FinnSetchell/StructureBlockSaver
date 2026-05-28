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

	public static void register() {
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (world.isClientSide()) return InteractionResult.PASS;
			if (!isWand(player.getItemInHand(hand))) return InteractionResult.PASS;

			BlockPos pos = hitResult.getBlockPos();
			WandSavedData state = WandSavedData.getServerState(world.getServer());
			if (pos.equals(state.pos1Map.get(player.getUUID()))) return InteractionResult.SUCCESS;
			state.pos1Map.put(player.getUUID(), pos);
			state.setDirty();
			((ServerPlayer) player).sendSystemMessage(Component.literal("Position 1 set to " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()));
			return InteractionResult.SUCCESS;
		});

		AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
			if (world.isClientSide()) return InteractionResult.PASS;
			if (!isWand(player.getItemInHand(hand))) return InteractionResult.PASS;

			WandSavedData state = WandSavedData.getServerState(world.getServer());
			if (pos.equals(state.pos2Map.get(player.getUUID()))) return InteractionResult.SUCCESS;
			state.pos2Map.put(player.getUUID(), pos);
			state.setDirty();
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
		WandSavedData state = WandSavedData.getServerState(((net.minecraft.server.level.ServerLevel) player.level()).getServer());
		state.pos1Map.remove(player.getUUID());
		state.pos2Map.remove(player.getUUID());
		state.setDirty();
		if (ServerPlayNetworking.canSend(player, ClearSelectionPayload.TYPE)) {
			ServerPlayNetworking.send(player, new ClearSelectionPayload());
		}
		ctx.getSource().sendSuccess(() -> Component.literal("Selection cleared."), false);
		return 1;
	}
}
