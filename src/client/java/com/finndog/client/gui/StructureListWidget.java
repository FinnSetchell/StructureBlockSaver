package com.finndog.client.gui;

import com.finndog.network.ClientboundOpenMenuPayload.StructureInfo;
import com.finndog.network.ServerboundTargetedSavePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;

import net.minecraft.client.input.MouseButtonEvent;

import java.util.List;

public class StructureListWidget extends ObjectSelectionList<StructureListWidget.Entry> {

    public StructureListWidget(Minecraft minecraft, int width, int height, int y0, int itemHeight) {
        super(minecraft, width, height, y0, itemHeight);
    }

    @Override
    public int getRowWidth() {
        return 280;
    }

    public void addStructureEntry(StructureInfo info) {
        this.addEntry(new Entry(info));
    }

    public void clearStructures() {
        this.clearEntries();
    }

    public static class Entry extends ObjectSelectionList.Entry<Entry> {
        public final StructureInfo info;
        private final Button saveButton;
        private final Button localSaveButton;
        private final Button tpButton;

        public Entry(StructureInfo info) {
            this.info = info;
            this.tpButton = Button.builder(Component.literal("TP"), btn -> {
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.connection.sendCommand("tp " + info.pos().getX() + " " + info.pos().getY() + " " + info.pos().getZ());
                }
            }).bounds(0, 0, 30, 20).build();

            this.saveButton = Button.builder(Component.literal("Save"), btn -> {
                ClientPlayNetworking.send(new ServerboundTargetedSavePayload(List.of(info.pos()), false));
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.displayClientMessage(Component.literal("Requested save for " + info.name()), false);
                }
            }).bounds(0, 0, 40, 20).build();

            this.localSaveButton = Button.builder(Component.literal("Local Save"), btn -> {
                ClientPlayNetworking.send(new ServerboundTargetedSavePayload(List.of(info.pos()), true));
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.displayClientMessage(Component.literal("Requested local save for " + info.name()), false);
                }
            }).bounds(0, 0, 70, 20).build();
        }

        @Override
        public Component getNarration() {
            return Component.literal(info.name());
        }

        @Override
        public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            int left = this.getX();
            int top = this.getY();
            int width = this.getWidth();
            String text = info.name() + " (" + info.size().getX() + "x" + info.size().getY() + "x" + info.size().getZ() + ")";
            guiGraphics.drawString(Minecraft.getInstance().font, text, left + 5, top + 6, -1);

            this.tpButton.setX(left + width - 150);
            this.tpButton.setY(top);
            this.tpButton.render(guiGraphics, mouseX, mouseY, partialTick);

            this.saveButton.setX(left + width - 115);
            this.saveButton.setY(top);
            this.saveButton.render(guiGraphics, mouseX, mouseY, partialTick);

            this.localSaveButton.setX(left + width - 70);
            this.localSaveButton.setY(top);
            this.localSaveButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
            if (this.tpButton.mouseClicked(event, bl)) return true;
            if (this.saveButton.mouseClicked(event, bl)) return true;
            if (this.localSaveButton.mouseClicked(event, bl)) return true;
            return super.mouseClicked(event, bl);
        }
    }
}
