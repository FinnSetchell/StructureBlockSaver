package com.finndog.client.gui;

import com.finndog.network.ClientboundOpenMenuPayload.StructureInfo;
import com.finndog.network.ServerboundTargetedSavePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class StructureMenuScreen extends Screen {
    private List<StructureInfo> allStructures;
    private StructureListWidget listWidget;
    private EditBox searchBox;
    private boolean sortByName = true;

    public StructureMenuScreen(List<StructureInfo> structures) {
        super(Component.literal("Structure Block Saver Dashboard"));
        this.allStructures = structures;
    }

    @Override
    protected void init() {
        super.init();

        int topY = 10;
        
        // Command Buttons
        this.addRenderableWidget(Button.builder(Component.literal("Clear"), btn -> runCommand("sbs clear")).bounds(this.width / 2 - 160, topY, 75, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Save All"), btn -> runCommand("sbs save")).bounds(this.width / 2 - 80, topY, 75, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Local Save All"), btn -> runCommand("sbs localsave")).bounds(this.width / 2, topY, 80, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Refresh"), btn -> runCommand("sbs menu")).bounds(this.width / 2 + 85, topY, 75, 20).build());

        // Search Box
        this.searchBox = new EditBox(this.font, this.width / 2 - 160, topY + 25, 200, 20, Component.literal("Search..."));
        this.searchBox.setResponder(s -> refreshList());
        this.addRenderableWidget(this.searchBox);

        // Sort Button
        this.addRenderableWidget(Button.builder(Component.literal("Sort: Name"), btn -> {
            sortByName = !sortByName;
            btn.setMessage(Component.literal("Sort: " + (sortByName ? "Name" : "Size")));
            refreshList();
        }).bounds(this.width / 2 + 45, topY + 25, 115, 20).build());

        // List
        this.listWidget = new StructureListWidget(this.minecraft, this.width, this.height - 110, topY + 50, 24);
        this.addRenderableWidget(this.listWidget);

        // Bottom Actions
        this.addRenderableWidget(Button.builder(Component.literal("Save Displayed"), btn -> saveDisplayed(false)).bounds(this.width / 2 - 160, this.height - 30, 150, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Local Save Displayed"), btn -> saveDisplayed(true)).bounds(this.width / 2 + 10, this.height - 30, 150, 20).build());

        refreshList();
    }

    private void runCommand(String cmd) {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.connection.sendCommand(cmd);
            this.onClose();
        }
    }

    private void saveDisplayed(boolean localSave) {
        List<BlockPos> positions = this.listWidget.children().stream().map(e -> e.info.pos()).collect(Collectors.toList());
        if (!positions.isEmpty()) {
            ClientPlayNetworking.send(new ServerboundTargetedSavePayload(positions, localSave));
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.displayClientMessage(Component.literal("Requested batch " + (localSave ? "local " : "") + "save for " + positions.size() + " structures."), false);
            }
        }
    }

    public void updateStructures(List<StructureInfo> structures) {
        this.allStructures = structures;
        this.refreshList();
    }

    private void refreshList() {
        this.listWidget.clearStructures();
        String query = this.searchBox.getValue().toLowerCase();

        List<StructureInfo> filtered = allStructures.stream()
            .filter(info -> info.name().toLowerCase().contains(query))
            .sorted(sortByName ? Comparator.comparing(StructureInfo::name) : Comparator.comparingInt(i -> -(i.size().getX() * i.size().getY() * i.size().getZ())))
            .collect(Collectors.toList());

        for (StructureInfo info : filtered) {
            this.listWidget.addStructureEntry(info);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 5, -1);
    }
}
