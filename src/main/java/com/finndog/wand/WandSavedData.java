package com.finndog.wand;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WandSavedData extends SavedData {
    public final Map<UUID, BlockPos> pos1Map = new HashMap<>();
    public final Map<UUID, BlockPos> pos2Map = new HashMap<>();

    public static final Codec<WandSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, BlockPos.CODEC).fieldOf("pos1Map").forGetter(d -> d.pos1Map),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, BlockPos.CODEC).fieldOf("pos2Map").forGetter(d -> d.pos2Map)
    ).apply(instance, (map1, map2) -> {
        WandSavedData data = new WandSavedData();
        data.pos1Map.putAll(map1);
        data.pos2Map.putAll(map2);
        return data;
    }));

    public static final SavedDataType<WandSavedData> TYPE = new SavedDataType<>(
            "sbs_wand_state",
            () -> new WandSavedData(),
            CODEC,
            null
    );

    public static WandSavedData getServerState(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }
}
