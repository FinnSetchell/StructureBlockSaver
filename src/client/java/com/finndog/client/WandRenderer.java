package com.finndog.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class WandRenderer {

    private static final long MAX_SCAN_VOLUME = 50_000;

    private record StructureBlockData(BlockPos pos, BlockPos offset, Vec3i size) {}

    private static BlockPos lastPos1 = null;
    private static BlockPos lastPos2 = null;
    private static List<StructureBlockData> cachedBlocks = List.of();
    private static boolean tooLargeToScan = false;

    public static void register() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(WandRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        BlockPos pos1 = ClientWandData.pos1;
        BlockPos pos2 = ClientWandData.pos2;
        if (pos1 == null && pos2 == null) return;

        MultiBufferSource consumers = context.consumers();
        if (consumers == null) return;

        PoseStack poseStack = context.matrixStack();
        if (poseStack == null) return;

        Vec3 camPos = context.camera().getPosition();

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        VertexConsumer lines = consumers.getBuffer(RenderType.lines());

        if (pos1 != null && pos2 == null) {
            drawBox(poseStack, lines, pos1.getX(), pos1.getY(), pos1.getZ(), pos1.getX() + 1, pos1.getY() + 1, pos1.getZ() + 1, 255, 255, 0, 255);
            poseStack.popPose();
            return;
        }
        if (pos2 != null && pos1 == null) {
            drawBox(poseStack, lines, pos2.getX(), pos2.getY(), pos2.getZ(), pos2.getX() + 1, pos2.getY() + 1, pos2.getZ() + 1, 255, 255, 0, 255);
            poseStack.popPose();
            return;
        }

        int minX = Math.min(pos1.getX(), pos2.getX());
        int minY = Math.min(pos1.getY(), pos2.getY());
        int minZ = Math.min(pos1.getZ(), pos2.getZ());
        int maxX = Math.max(pos1.getX(), pos2.getX()) + 1;
        int maxY = Math.max(pos1.getY(), pos2.getY()) + 1;
        int maxZ = Math.max(pos1.getZ(), pos2.getZ()) + 1;

        // Yellow: selection bounding box
        drawBox(poseStack, lines, minX, minY, minZ, maxX, maxY, maxZ, 255, 255, 0, 255);

        if (!pos1.equals(lastPos1) || !pos2.equals(lastPos2)) {
            lastPos1 = pos1;
            lastPos2 = pos2;
            long volume = (long)(maxX - minX) * (maxY - minY) * (maxZ - minZ);
            if (volume > MAX_SCAN_VOLUME) {
                tooLargeToScan = true;
                cachedBlocks = List.of();
            }
            else {
                tooLargeToScan = false;
                cachedBlocks = scanBlocks(minX, minY, minZ, maxX - 1, maxY - 1, maxZ - 1);
            }
        }

        for (StructureBlockData data : cachedBlocks) {
            int px = data.pos().getX();
            int py = data.pos().getY();
            int pz = data.pos().getZ();

            // Cyan: the structure block itself
            drawBox(poseStack, lines, px, py, pz, px + 1, py + 1, pz + 1, 0, 255, 255, 255);

            // Green: the save region defined by this structure block
            int sx = px + data.offset().getX();
            int sy = py + data.offset().getY();
            int sz = pz + data.offset().getZ();
            drawBox(poseStack, lines, sx, sy, sz, sx + data.size().getX(), sy + data.size().getY(), sz + data.size().getZ(), 0, 255, 0, 255);
        }

        poseStack.popPose();
    }

    private static List<StructureBlockData> scanBlocks(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        var level = Minecraft.getInstance().level;
        if (level == null) return List.of();

        List<StructureBlockData> found = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof StructureBlockEntity sbe)) continue;
            if (sbe.getMode() != StructureMode.SAVE) continue;
            found.add(new StructureBlockData(pos.immutable(), sbe.getStructurePos(), sbe.getStructureSize()));
        }
        return found;
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
