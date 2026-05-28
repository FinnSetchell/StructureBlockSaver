package com.finndog.utils;

import com.finndog.network.ClientboundSaveStructurePayload;
import com.finndog.wand.WandSavedData;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class TargetedSaveTask implements TickProcessor.TickTask {
    private final ServerPlayer player;
    private final ServerLevel level;
    private final boolean localSave;
    private final Queue<BlockPos> positionsToProcess;
    private final Queue<ClientboundSaveStructurePayload> payloadsToSend = new LinkedList<>();
    private int processedCount = 0;
    private int failCount = 0;
    private final int total;
    private int ticks = 0;

    public TargetedSaveTask(ServerPlayer player, List<BlockPos> positions, boolean localSave) {
        this.player = player;
        this.level = (ServerLevel) player.level();
        this.localSave = localSave;
        this.positionsToProcess = new LinkedList<>(positions);
        this.total = positions.size();
        
        if (total > 1) {
            String action = localSave ? "locally saving" : "saving";
            player.sendSystemMessage(Component.literal("Started " + action + " " + total + " structures."));
        }
    }

    @Override
    public boolean tick() {
        ticks++;
        if (!payloadsToSend.isEmpty()) {
            if (ticks % 4 == 0) {
                ServerPlayNetworking.send(player, payloadsToSend.poll());
            }
        }

        int processedThisTick = 0;
        while (!positionsToProcess.isEmpty() && payloadsToSend.size() < 5 && processedThisTick < 16) {
            BlockPos pos = positionsToProcess.poll();
            processPosition(pos);
            processedThisTick++;
        }

        if (positionsToProcess.isEmpty() && payloadsToSend.isEmpty()) {
            finish();
            return true;
        }
        return false;
    }

    private void processPosition(BlockPos pos) {
        if (!level.hasChunkAt(pos)) return;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof StructureBlockEntity sbe)) {
            failCount++;
            return;
        }

        String name = sbe.getStructureName();
        if (localSave) {
            try {
                StructureTemplateManager manager = level.getStructureManager();
                ResourceLocation location = ResourceLocation.parse(name);
                StructureTemplate template = manager.getOrCreate(location);
                template.fillFromWorld(level, sbe.getBlockPos().offset(sbe.getStructurePos()), sbe.getStructureSize(), !sbe.isIgnoreEntities(), java.util.List.of(Blocks.STRUCTURE_VOID));
                CompoundTag tag = template.save(new CompoundTag());
                if (ServerPlayNetworking.canSend(player, ClientboundSaveStructurePayload.TYPE)) {
                    java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
                    net.minecraft.nbt.NbtIo.writeCompressed(tag, bos);
                    payloadsToSend.add(new ClientboundSaveStructurePayload(name, bos.toByteArray()));
                } else {
                    player.sendSystemMessage(Component.literal("You must have the client mod installed to save locally."));
                }
                processedCount++;
            } catch (Exception e) {
                com.finndog.StructureBlockSaver.LOGGER.error("Failed to process local structure save for {}", name, e);
                failCount++;
            }
        } else {
            if (sbe.saveStructure()) {
                processedCount++;
            } else {
                failCount++;
            }
        }
    }

    private void finish() {
        if (total == 0) return;
        String prefix = localSave ? "Saved locally: " : "Saved: ";
        player.sendSystemMessage(Component.literal(prefix + processedCount + " structures." + (failCount > 0 ? " Failed: " + failCount : "")));
    }
}
