package com.finndog.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class WandRenderer {

    public static void register() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(WandRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        BlockPos pos1 = ClientWandData.pos1;
        BlockPos pos2 = ClientWandData.pos2;
        if (pos1 == null || pos2 == null) return;

        MultiBufferSource consumers = context.consumers();
        if (consumers == null) return;

        PoseStack poseStack = context.matrixStack();
        if (poseStack == null) return;

        Vec3 camPos = context.camera().getPosition();

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        VertexConsumer lines = consumers.getBuffer(RenderType.lines());

        int minX = Math.min(pos1.getX(), pos2.getX());
        int minY = Math.min(pos1.getY(), pos2.getY());
        int minZ = Math.min(pos1.getZ(), pos2.getZ());
        int maxX = Math.max(pos1.getX(), pos2.getX()) + 1;
        int maxY = Math.max(pos1.getY(), pos2.getY()) + 1;
        int maxZ = Math.max(pos1.getZ(), pos2.getZ()) + 1;

        // Yellow: selection bounding box
        drawBox(poseStack, lines, minX, minY, minZ, maxX, maxY, maxZ, 255, 255, 0, 255);

        var level = Minecraft.getInstance().level;
        if (level == null) {
            poseStack.popPose();
            return;
        }

        BlockPos scanMin = new BlockPos(minX, minY, minZ);
        BlockPos scanMax = new BlockPos(maxX - 1, maxY - 1, maxZ - 1);

        for (BlockPos pos : BlockPos.betweenClosed(scanMin, scanMax)) {
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof StructureBlockEntity sbe)) continue;
            if (sbe.getMode() != StructureMode.SAVE) continue;

            int px = pos.getX();
            int py = pos.getY();
            int pz = pos.getZ();

            // Cyan: the structure block itself
            drawBox(poseStack, lines, px, py, pz, px + 1, py + 1, pz + 1, 0, 255, 255, 255);

            // Green: the save region defined by this structure block
            BlockPos offset = sbe.getStructurePos();
            Vec3i size = sbe.getStructureSize();
            int sx = px + offset.getX();
            int sy = py + offset.getY();
            int sz = pz + offset.getZ();
            drawBox(poseStack, lines, sx, sy, sz, sx + size.getX(), sy + size.getY(), sz + size.getZ(), 0, 255, 0, 255);
        }

        poseStack.popPose();
    }

    private static void drawBox(PoseStack poseStack, VertexConsumer consumer,
                                double x1, double y1, double z1,
                                double x2, double y2, double z2,
                                int r, int g, int b, int a) {
        PoseStack.Pose pose = poseStack.last();

        // Bottom face
        line(consumer, pose, x1,y1,z1, x2,y1,z1, r,g,b,a);
        line(consumer, pose, x2,y1,z1, x2,y1,z2, r,g,b,a);
        line(consumer, pose, x2,y1,z2, x1,y1,z2, r,g,b,a);
        line(consumer, pose, x1,y1,z2, x1,y1,z1, r,g,b,a);
        // Top face
        line(consumer, pose, x1,y2,z1, x2,y2,z1, r,g,b,a);
        line(consumer, pose, x2,y2,z1, x2,y2,z2, r,g,b,a);
        line(consumer, pose, x2,y2,z2, x1,y2,z2, r,g,b,a);
        line(consumer, pose, x1,y2,z2, x1,y2,z1, r,g,b,a);
        // Verticals
        line(consumer, pose, x1,y1,z1, x1,y2,z1, r,g,b,a);
        line(consumer, pose, x2,y1,z1, x2,y2,z1, r,g,b,a);
        line(consumer, pose, x2,y1,z2, x2,y2,z2, r,g,b,a);
        line(consumer, pose, x1,y1,z2, x1,y2,z2, r,g,b,a);
    }

    private static void line(VertexConsumer consumer, PoseStack.Pose pose,
                              double x1, double y1, double z1, double x2, double y2, double z2,
                              int r, int g, int b, int a) {
        float dx = (float)(x2 - x1);
        float dy = (float)(y2 - y1);
        float dz = (float)(z2 - z1);
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len == 0) return;

        consumer.addVertex(pose.pose(), (float) x1, (float) y1, (float) z1)
                .setColor(r, g, b, a)
                .setNormal(pose, dx / len, dy / len, dz / len);
        consumer.addVertex(pose.pose(), (float) x2, (float) y2, (float) z2)
                .setColor(r, g, b, a)
                .setNormal(pose, dx / len, dy / len, dz / len);
    }
}
