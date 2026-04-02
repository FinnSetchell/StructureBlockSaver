package com.finndog;

import com.finndog.client.ClientWandData;
import com.finndog.client.WandRenderer;
import com.finndog.wand.StructureWand;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;

public class StructureBlockSaverClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        WandRenderer.register();

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClientSide) return InteractionResult.PASS;
            if (!StructureWand.isWand(player.getItemInHand(hand))) return InteractionResult.PASS;
            BlockPos pos1 = hitResult.getBlockPos();
            if (pos1.equals(ClientWandData.pos1)) return InteractionResult.SUCCESS;
            ClientWandData.pos1 = pos1;
            return InteractionResult.SUCCESS;
        });

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (!world.isClientSide) return InteractionResult.PASS;
            if (!StructureWand.isWand(player.getItemInHand(hand))) return InteractionResult.PASS;
            BlockPos pos2 = pos.immutable();
            if (pos2.equals(ClientWandData.pos2)) return InteractionResult.SUCCESS;
            ClientWandData.pos2 = pos2;
            return InteractionResult.SUCCESS;
        });
    }
}
