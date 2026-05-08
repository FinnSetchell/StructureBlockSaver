package com.finndog.autosave;

import com.finndog.wand.StructureWand;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.properties.StructureMode;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;

public class AutoSaveManager {

    private static final int DEFAULT_INTERVAL = 30;

    private record Session(BlockPos pos1, BlockPos pos2, int intervalTicks, int elapsed) {}

    private static final Map<UUID, Session> sessions = new HashMap<>();

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(AutoSaveManager::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
            sessions.remove(handler.getPlayer().getUUID())
        );
    }

    public static LiteralArgumentBuilder<CommandSourceStack> subcommand() {
        return Commands.literal("autosave")
            .then(Commands.literal("on")
                .executes(ctx -> start(ctx, DEFAULT_INTERVAL))
                .then(Commands.argument("seconds", integer(5, 3600))
                    .executes(ctx -> start(ctx, getInteger(ctx, "seconds")))))
            .then(Commands.literal("off")
                .executes(AutoSaveManager::stop));
    }

    private static int start(CommandContext<CommandSourceStack> ctx, int seconds) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        BlockPos pos1 = StructureWand.pos1Map.get(player.getUUID());
        BlockPos pos2 = StructureWand.pos2Map.get(player.getUUID());
        if (pos1 == null || pos2 == null) {
            ctx.getSource().sendFailure(Component.literal("Select both positions with the Structure Wand first."));
            return 0;
        }
        sessions.put(player.getUUID(), new Session(pos1, pos2, seconds * 20, 0));
        ctx.getSource().sendSuccess(() -> Component.literal("Auto-save started. Saving every " + seconds + "s."), false);
        return 1;
    }

    private static int stop(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        if (sessions.remove(player.getUUID()) != null) {
            ctx.getSource().sendSuccess(() -> Component.literal("Auto-save stopped."), false);
        }
        else {
            ctx.getSource().sendFailure(Component.literal("Auto-save is not active."));
        }
        return 1;
    }

    private static void tick(MinecraftServer server) {
        if (sessions.isEmpty()) return;

        sessions.replaceAll((uuid, s) -> {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player == null) return s;
            int newElapsed = s.elapsed() + 1;
            if (newElapsed < s.intervalTicks()) {
                return new Session(s.pos1(), s.pos2(), s.intervalTicks(), newElapsed);
            }
            doSave(player, s);
            return new Session(s.pos1(), s.pos2(), s.intervalTicks(), 0);
        });

        sessions.entrySet().removeIf(e -> server.getPlayerList().getPlayer(e.getKey()) == null);
    }

    private static void doSave(ServerPlayer player, Session s) {
        ServerLevel level = (ServerLevel) player.level();
        int count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(s.pos1(), s.pos2())) {
            if (!level.isLoaded(pos)) continue;
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof StructureBlockEntity sbe && sbe.getMode() == StructureMode.SAVE) {
                if (sbe.saveStructure()) count++;
            }
        }
        int saved = count;
        player.sendSystemMessage(Component.literal("[AutoSave] Saved " + saved + " structure(s)."));
    }
}
