package com.finndog;

import com.finndog.client.ClientWandData;
import com.finndog.client.WandRenderer;
import com.finndog.network.ClearSelectionPayload;
import com.finndog.wand.StructureWand;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;

public class StructureBlockSaverClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        WandRenderer.register();

        ClientPlayNetworking.registerGlobalReceiver(ClearSelectionPayload.TYPE, (payload, context) -> {
            ClientWandData.pos1 = null;
            ClientWandData.pos2 = null;
        });

        ClientPlayNetworking.registerGlobalReceiver(com.finndog.network.ClientboundSaveStructurePayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                try {
                    java.nio.file.Path dir = net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir().resolve("structures");
                    String safeName = payload.name().replace(":", "/");
                    java.nio.file.Path targetFile = dir.resolve(safeName + ".nbt");
                    if (!java.nio.file.Files.exists(targetFile.getParent())) {
                        java.nio.file.Files.createDirectories(targetFile.getParent());
                    }
                    net.minecraft.nbt.NbtIo.writeCompressed(payload.nbt(), targetFile);
                    context.player().displayClientMessage(net.minecraft.network.chat.Component.literal("Saved local structure: " + payload.name()), false);
                } catch (Exception e) {
                    StructureBlockSaver.LOGGER.error("Failed to save local structure", e);
                    context.player().displayClientMessage(net.minecraft.network.chat.Component.literal("Failed to save local structure " + payload.name()), false);
                }
            });
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClientSide()) return InteractionResult.PASS;
            if (!StructureWand.isWand(player.getItemInHand(hand))) return InteractionResult.PASS;
            BlockPos pos1 = hitResult.getBlockPos();
            if (pos1.equals(ClientWandData.pos1)) return InteractionResult.SUCCESS;
            ClientWandData.pos1 = pos1;
            ClientWandData.lastSetWasPos1 = true;
            return InteractionResult.SUCCESS;
        });

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (!world.isClientSide()) return InteractionResult.PASS;
            if (!StructureWand.isWand(player.getItemInHand(hand))) return InteractionResult.PASS;
            BlockPos pos2 = pos.immutable();
            if (pos2.equals(ClientWandData.pos2)) return InteractionResult.SUCCESS;
            ClientWandData.pos2 = pos2;
            ClientWandData.lastSetWasPos1 = false;
            return InteractionResult.SUCCESS;
        });

        HudRenderCallback.EVENT.register((guiGraphics, deltaTicker) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            if (!StructureWand.isWand(mc.player.getMainHandItem())) return;
            BlockPos p1 = ClientWandData.pos1;
            BlockPos p2 = ClientWandData.pos2;
            if (p1 == null || p2 == null) return;
            int w = Math.abs(p2.getX() - p1.getX()) + 1;
            int h = Math.abs(p2.getY() - p1.getY()) + 1;
            int d = Math.abs(p2.getZ() - p1.getZ()) + 1;
            String text = w + " x " + h + " x " + d;
            int screenW = mc.getWindow().getGuiScaledWidth();
            int screenH = mc.getWindow().getGuiScaledHeight();
            int textW = mc.font.width(text);
            guiGraphics.drawString(mc.font, text, (screenW - textW) / 2, screenH - 59, 0xFFFFFF);
        });
    }
}
