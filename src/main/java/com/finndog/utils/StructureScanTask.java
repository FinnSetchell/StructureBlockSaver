package com.finndog.utils;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class StructureScanTask implements TickProcessor.TickTask {
	private final ServerLevel level;
	private final CommandSourceStack source;
	private final BlockPos pos1;
	private final BlockPos pos2;
	private final String filter;
	private final boolean dryRun;
	private final boolean listMode;
	private final boolean autoSave;
	private final boolean localSave;
	private final boolean menuMode;

	private final Queue<ChunkPos> chunksToProcess = new LinkedList<>();
	private final List<String> results = new ArrayList<>();
	private final List<com.finndog.network.ClientboundOpenMenuPayload.StructureInfo> menuResults = new ArrayList<>();

	private final int totalChunks;
	private final int minX, maxX, minY, maxY, minZ, maxZ;

	public StructureScanTask(CommandSourceStack source, BlockPos pos1, BlockPos pos2, String filter, boolean dryRun, boolean listMode, boolean autoSave, boolean localSave, boolean menuMode) {
		this.level = source.getLevel();
		this.source = source;
		this.pos1 = pos1;
		this.pos2 = pos2;
		this.filter = filter;
		this.dryRun = dryRun;
		this.listMode = listMode;
		this.autoSave = autoSave;
		this.localSave = localSave;
		this.menuMode = menuMode;

		this.minX = Math.min(pos1.getX(), pos2.getX());
		this.maxX = Math.max(pos1.getX(), pos2.getX());
		this.minY = Math.min(pos1.getY(), pos2.getY());
		this.maxY = Math.max(pos1.getY(), pos2.getY());
		this.minZ = Math.min(pos1.getZ(), pos2.getZ());
		this.maxZ = Math.max(pos1.getZ(), pos2.getZ());

		for (int cx = minX >> 4; cx <= maxX >> 4; cx++) {
			for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++) {
				chunksToProcess.add(new ChunkPos(cx, cz));
			}
		}

		this.totalChunks = chunksToProcess.size();
		if (totalChunks > 1 && !autoSave) {
			String action = menuMode ? "opening menu for" : (listMode ? "listing" : (localSave ? "locally saving" : "saving"));
			source.sendSuccess(() -> Component.literal("Started " + action + " structures in " + totalChunks + " chunks. This may take a moment..."), false);
		}
	}

	@Override
	public boolean tick() {
		int chunksThisTick = 0;
		while (!chunksToProcess.isEmpty() && chunksThisTick < 16) {
			ChunkPos cp = chunksToProcess.poll();
			processChunk(cp);
			chunksThisTick++;
		}

		if (chunksToProcess.isEmpty()) {
			finish();
			return true;
		}
		return false;
	}

	private void processChunk(ChunkPos cp) {
		LevelChunk chunk = level.getChunk(cp.x, cp.z);
		for (BlockEntity be : chunk.getBlockEntities().values()) {
			if (!(be instanceof StructureBlockEntity sbe)) continue;

			BlockPos pos = sbe.getBlockPos();
			if (pos.getX() < minX || pos.getX() > maxX ||
				pos.getY() < minY || pos.getY() > maxY ||
				pos.getZ() < minZ || pos.getZ() > maxZ) {
				continue;
			}

			if (menuMode) {
				if (sbe.getMode() != StructureMode.SAVE) continue;
				menuResults.add(new com.finndog.network.ClientboundOpenMenuPayload.StructureInfo(pos.immutable(), sbe.getStructureName(), sbe.getStructureSize()));
			} else if (listMode) {
				Vec3i size = sbe.getStructureSize();
				results.add("[" + sbe.getMode().name() + "] " + sbe.getStructureName()
					+ " (" + size.getX() + "x" + size.getY() + "x" + size.getZ() + ")");
			} else {
				if (sbe.getMode() != StructureMode.SAVE) continue;
				String name = sbe.getStructureName();
				if (filter != null && !matchesFilter(name, filter)) continue;
				
				if (localSave) {
					if (!dryRun) {
						try {
							net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager manager = level.getStructureManager();
							net.minecraft.resources.ResourceLocation location = net.minecraft.resources.ResourceLocation.parse(name);
							net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate template = manager.getOrCreate(location);
							template.fillFromWorld(level, sbe.getBlockPos().offset(sbe.getStructurePos()), sbe.getStructureSize(), !sbe.isIgnoreEntities(), java.util.List.of(net.minecraft.world.level.block.Blocks.STRUCTURE_VOID));
							net.minecraft.nbt.CompoundTag tag = template.save(new net.minecraft.nbt.CompoundTag());
							net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send((net.minecraft.server.level.ServerPlayer) source.getPlayer(), new com.finndog.network.ClientboundSaveStructurePayload(name, tag));
						} catch (Exception e) {
							com.finndog.StructureBlockSaver.LOGGER.error("Failed to process local structure save for {}", name, e);
							continue;
						}
					}
					results.add(name);
				} else {
					if (dryRun || sbe.saveStructure()) {
						results.add(name);
					}
				}
			}
		}
	}

	private void finish() {
		if (menuMode) {
			if (source.getPlayer() instanceof net.minecraft.server.level.ServerPlayer sp) {
				net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(sp, new com.finndog.network.ClientboundOpenMenuPayload(menuResults));
			}
		} else if (listMode) {
			StringBuilder sb = new StringBuilder("Found ").append(results.size()).append(" structure block(s)");
			if (!results.isEmpty()) {
				sb.append(":");
				for (String line : results) sb.append("\n  - ").append(line);
			} else {
				sb.append(".");
			}
			source.sendSuccess(() -> Component.literal(sb.toString()), false);
		} else {
			String prefix;
			if (autoSave) prefix = "[AutoSave] Saved ";
			else prefix = dryRun ? "[Dry Run] Found " : "Saved ";
			
			StringBuilder sb = new StringBuilder(prefix).append(results.size()).append(" structure(s)");
			if (!results.isEmpty()) {
				sb.append(":");
				for (String name : results) sb.append("\n  - ").append(name);
			} else {
				sb.append(".");
			}
			source.sendSuccess(() -> Component.literal(sb.toString()), !dryRun);
		}
	}

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
