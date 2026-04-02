package com.finndog.mixin.client;

import com.finndog.client.ClientWandData;
import com.finndog.network.ExpandSelectionPayload;
import com.finndog.wand.StructureWand;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class ScrollMixin {

    @Inject(at = @At("HEAD"), method = "onScroll", cancellable = true)
    private void onScroll(long window, double xOffset, double yOffset, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        if (!StructureWand.isWand(mc.player.getMainHandItem())) return;

        BlockPos p1 = ClientWandData.pos1;
        BlockPos p2 = ClientWandData.pos2;
        if (p1 == null || p2 == null) return;

        Vec3 look = mc.player.getLookAngle();
        Direction facing;
        if (Math.abs(look.y) > Math.abs(look.x) && Math.abs(look.y) > Math.abs(look.z)) {
            facing = look.y > 0 ? Direction.UP : Direction.DOWN;
        }
        else {
            facing = mc.player.getDirection();
        }

        Direction moveDir = yOffset > 0 ? facing : facing.getOpposite();

        boolean isPos1 = ClientWandData.lastSetWasPos1;
        BlockPos target = isPos1 ? p1 : p2;
        BlockPos newPos = target.relative(moveDir);

        if (isPos1) {
            ClientWandData.pos1 = newPos;
        }
        else {
            ClientWandData.pos2 = newPos;
        }

        ClientPlayNetworking.send(new ExpandSelectionPayload(isPos1, newPos));
        ci.cancel();
    }
}
