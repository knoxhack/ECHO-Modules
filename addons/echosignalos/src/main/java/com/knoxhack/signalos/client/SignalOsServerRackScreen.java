package com.knoxhack.signalos.client;

import com.knoxhack.echocore.client.ui.EchoCyberGlassUi;
import com.knoxhack.echonetcore.client.EchoNetClientActions;
import com.knoxhack.signalos.api.SignalOsDataRecord;
import com.knoxhack.signalos.api.SignalOsDriveData;
import com.knoxhack.signalos.content.SignalOsContentRegistry;
import com.knoxhack.signalos.item.SignalOsDataDriveItem;
import com.knoxhack.signalos.menu.SignalOsServerRackMenu;
import com.knoxhack.signalos.network.SignalOsRackActionPacket;
import com.knoxhack.signalos.service.SignalOsRackActions;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public class SignalOsServerRackScreen extends AbstractContainerScreen<SignalOsServerRackMenu> {
    private static final int PANEL = 0xEE071017;
    private static final int ROW = 0x94112430;
    private static final int ROW_HOVER = 0xD0152B38;
    private static final int TEXT = 0xFFE9FBFF;
    private static final int MUTED = 0xFF8CA7B5;
    private static final int CYAN = 0xFF66E8FF;
    private static final int GREEN = 0xFF91F7A5;
    private static final int WARN = 0xFFFFD166;
    private static final int RED = 0xFFFF8FA3;

    private final List<HitBox> hitBoxes = new ArrayList<>();
    private int selectedDriveSlot;
    private Identifier selectedNetworkRecordId;
    private Identifier selectedDriveRecordId;
    private Identifier selectedTemplateId;
    private String labelDraft = "";
    private boolean editingLabel;

    public SignalOsServerRackScreen(SignalOsServerRackMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, SignalOsServerRackMenu.GUI_WIDTH, SignalOsServerRackMenu.GUI_HEIGHT);
        this.inventoryLabelX = 97;
        this.inventoryLabelY = 162;
        this.titleLabelX = 14;
        this.titleLabelY = 11;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        hitBoxes.clear();
        syncLabelDraft();
        int x = leftPos;
        int y = topPos;
        EchoCyberGlassUi.panel(graphics, x, y, imageWidth, imageHeight, PANEL, CYAN);
        graphics.fill(x + 2, y + 2, x + imageWidth - 2, y + 28, 0xDD101D24);
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            drawSlot(graphics, x + slot.x, y + slot.y, i < SignalOsServerRackMenu.DRIVE_SLOT_COUNT);
        }
        drawDrivePanel(graphics, x, y, mouseX, mouseY);
        drawNetworkPanel(graphics, x, y, mouseX, mouseY);
        drawDriveRecordsPanel(graphics, x, y, mouseX, mouseY);
        drawTemplatesPanel(graphics, x, y, mouseX, mouseY);
        drawActions(graphics, x, y, mouseX, mouseY);
        super.extractContents(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, Component.literal("SIGNALOS SERVER RACK"), titleLabelX, titleLabelY, CYAN, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, MUTED, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        for (int i = 0; i < SignalOsServerRackMenu.DRIVE_SLOT_COUNT; i++) {
            Slot slot = menu.slots.get(i);
            if (inside(event.x(), event.y(), leftPos + slot.x, topPos + slot.y, 18, 18)) {
                selectedDriveSlot = i;
                syncLabelDraft();
                break;
            }
        }
        for (HitBox box : List.copyOf(hitBoxes)) {
            if (!box.inside(event.x(), event.y())) {
                continue;
            }
            switch (box.kind()) {
                case NETWORK_RECORD -> selectedNetworkRecordId = box.id();
                case DRIVE_RECORD -> selectedDriveRecordId = box.id();
                case TEMPLATE -> selectedTemplateId = box.id();
                case LABEL -> editingLabel = true;
                case ACTION -> send(box.action(), box.payload());
            }
            playClick();
            return true;
        }
        editingLabel = false;
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (editingLabel) {
            if (event.key() == GLFW.GLFW_KEY_BACKSPACE && !labelDraft.isEmpty()) {
                labelDraft = labelDraft.substring(0, labelDraft.length() - 1);
                return true;
            }
            if (event.key() == GLFW.GLFW_KEY_DELETE) {
                labelDraft = "";
                return true;
            }
            if (event.key() == GLFW.GLFW_KEY_ENTER) {
                send(SignalOsRackActions.RENAME_DRIVE, labelDraft);
                editingLabel = false;
                return true;
            }
        }
        return super.keyPressed(event);
    }

    public boolean handleCharTyped(CharacterEvent event) {
        if (!editingLabel || event == null || !event.isAllowedChatCharacter() || labelDraft.length() >= 80) {
            return false;
        }
        String typed = event.codepointAsString();
        if (typed == null || typed.isBlank()) {
            return false;
        }
        labelDraft += typed;
        return true;
    }

    private void drawDrivePanel(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
        ItemStack stack = selectedDrive();
        SignalOsDriveData data = SignalOsDataDriveItem.data(stack);
        graphics.text(font, "DRIVE BAYS", x + 18, y + 38, MUTED, false);
        for (int i = 0; i < SignalOsServerRackMenu.DRIVE_SLOT_COUNT; i++) {
            Slot slot = menu.slots.get(i);
            int sx = x + slot.x - 2;
            int sy = y + slot.y - 2;
            graphics.outline(sx, sy, 20, 20, i == selectedDriveSlot ? CYAN : 0x5538DFF4);
        }
        int panelX = x + 52;
        int panelY = y + 38;
        graphics.fill(panelX, panelY, panelX + 104, panelY + 75, 0xAA071017);
        graphics.outline(panelX, panelY, 104, 75, 0x5538DFF4);
        graphics.text(font, trim(stack.isEmpty() ? "NO DRIVE" : data.label().toUpperCase(Locale.ROOT), 92),
                panelX + 7, panelY + 7, stack.isEmpty() ? RED : TEXT, false);
        graphics.text(font, (data.isV2Supported() ? "V2 READY" : "LEGACY READ-ONLY")
                + " | " + data.records().size() + " record(s)", panelX + 7, panelY + 20,
                data.isV2Supported() ? MUTED : WARN, false);
        int inputY = panelY + 40;
        graphics.fill(panelX + 6, inputY, panelX + 98, inputY + 16, editingLabel ? 0xFF183743 : 0xFF112430);
        graphics.outline(panelX + 6, inputY, 92, 16, editingLabel ? CYAN : 0x5538DFF4);
        graphics.text(font, trim(labelDraft + (editingLabel ? "_" : ""), 84), panelX + 10, inputY + 5, TEXT, false);
        hitBoxes.add(new HitBox(HitKind.LABEL, null, null, "", panelX + 6, inputY, 92, 16));
    }

    private void drawNetworkPanel(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
        List<SignalOsDataRecord> records = SignalOsClientState.dataRecords().stream()
                .filter(record -> !"note".equals(record.type()))
                .toList();
        normalizeNetworkRecord(records);
        int panelX = x + 166;
        int panelY = y + 38;
        graphics.text(font, "NETWORK RECORDS", panelX, panelY - 12, CYAN, false);
        int rowY = panelY;
        for (SignalOsDataRecord record : records) {
            if (rowY + 18 > y + 103) {
                break;
            }
            drawRecordRow(graphics, mouseX, mouseY, record, selectedNetworkRecordId, panelX, rowY, 166, CYAN,
                    HitKind.NETWORK_RECORD);
            rowY += 19;
        }
        if (records.isEmpty()) {
            graphics.text(font, "No network records", panelX + 4, panelY + 6, MUTED, false);
        }
    }

    private void drawDriveRecordsPanel(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
        List<SignalOsDataRecord> records = SignalOsDataDriveItem.data(selectedDrive()).records();
        normalizeDriveRecord(records);
        int panelX = x + 166;
        int panelY = y + 112;
        graphics.text(font, "DRIVE RECORDS", panelX, panelY - 10, WARN, false);
        int rowY = panelY;
        for (SignalOsDataRecord record : records) {
            if (rowY + 18 > y + 162) {
                break;
            }
            drawRecordRow(graphics, mouseX, mouseY, record, selectedDriveRecordId, panelX, rowY, 166, WARN,
                    HitKind.DRIVE_RECORD);
            rowY += 19;
        }
        if (records.isEmpty()) {
            graphics.text(font, "Drive is empty", panelX + 4, panelY + 6, MUTED, false);
        }
    }

    private void drawTemplatesPanel(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
        List<Map.Entry<Identifier, SignalOsDriveData>> templates = templateEntries();
        if (!templates.isEmpty() && (selectedTemplateId == null
                || templates.stream().noneMatch(entry -> entry.getKey().equals(selectedTemplateId)))) {
            selectedTemplateId = templates.getFirst().getKey();
        }
        int panelX = x + 52;
        int panelY = y + 118;
        graphics.text(font, "TEMPLATES", panelX, panelY - 9, GREEN, false);
        int rowY = panelY;
        for (Map.Entry<Identifier, SignalOsDriveData> entry : templates) {
            Identifier id = entry.getKey();
            SignalOsDriveData template = entry.getValue();
            if (rowY + 16 > y + 162) {
                break;
            }
            boolean selected = id.equals(selectedTemplateId);
            boolean hovered = inside(mouseX, mouseY, panelX, rowY, 104, 15);
            graphics.fill(panelX, rowY, panelX + 104, rowY + 15, selected ? 0xD0152B38 : hovered ? ROW_HOVER : ROW);
            graphics.outline(panelX, rowY, 104, 15, selected ? GREEN : 0x5538DFF4);
            graphics.text(font, trim(template.label(), 96), panelX + 4, rowY + 4, selected ? TEXT : MUTED, false);
            hitBoxes.add(new HitBox(HitKind.TEMPLATE, id, null, "", panelX, rowY, 104, 15));
            rowY += 16;
        }
        if (templates.isEmpty()) {
            graphics.text(font, "No templates", panelX + 4, panelY + 4, MUTED, false);
        }
    }

    private void drawActions(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
        drawButton(graphics, mouseX, mouseY, "COPY", x + 52, y + 146, 48, CYAN,
                SignalOsRackActions.COPY_RECORD, selectedNetworkRecordId == null ? "" : selectedNetworkRecordId.toString());
        drawButton(graphics, mouseX, mouseY, "REMOVE", x + 104, y + 146, 52, RED,
                SignalOsRackActions.REMOVE_RECORD, selectedDriveRecordId == null ? "" : selectedDriveRecordId.toString());
        drawButton(graphics, mouseX, mouseY, "APPLY", x + 166, y + 146, 50, GREEN,
                SignalOsRackActions.APPLY_TEMPLATE, selectedTemplateId == null ? "" : selectedTemplateId.toString());
        drawButton(graphics, mouseX, mouseY, "RENAME", x + 220, y + 146, 58, WARN,
                SignalOsRackActions.RENAME_DRIVE, labelDraft);
        drawButton(graphics, mouseX, mouseY, "CLEAR", x + 282, y + 146, 48, RED,
                SignalOsRackActions.CLEAR_DRIVE, "");
    }

    private void drawRecordRow(GuiGraphicsExtractor graphics, int mouseX, int mouseY, SignalOsDataRecord record,
            Identifier selectedId, int x, int y, int w, int accent, HitKind kind) {
        boolean selected = record.id().equals(selectedId);
        boolean hovered = inside(mouseX, mouseY, x, y, w, 17);
        graphics.fill(x, y, x + w, y + 17, selected ? 0xD0152B38 : hovered ? ROW_HOVER : ROW);
        graphics.outline(x, y, w, 17, selected ? accent : 0x5538DFF4);
        graphics.text(font, trim(record.title(), w - 10), x + 5, y + 5, selected ? TEXT : MUTED, false);
        hitBoxes.add(new HitBox(kind, record.id(), null, "", x, y, w, 17));
    }

    private void drawSlot(GuiGraphicsExtractor graphics, int x, int y, boolean driveSlot) {
        EchoCyberGlassUi.slot(graphics, x, y, driveSlot ? 0xFF0B151C : 0xFF0A1118);
    }

    private void drawButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY, String label, int x, int y,
            int w, int accent, Identifier action, String payload) {
        boolean hovered = inside(mouseX, mouseY, x, y, w, 16);
        EchoCyberGlassUi.button(graphics, font, x, y, w, 16, trim(label, w - 8), hovered, true, accent);
        hitBoxes.add(new HitBox(HitKind.ACTION, null, action, payload, x, y, w, 16));
    }

    private void send(Identifier action, String payload) {
        EchoNetClientActions.sendServerboundAction(new SignalOsRackActionPacket(menu.blockPos(), selectedDriveSlot, action, payload));
    }

    private ItemStack selectedDrive() {
        if (selectedDriveSlot < 0 || selectedDriveSlot >= SignalOsServerRackMenu.DRIVE_SLOT_COUNT) {
            selectedDriveSlot = 0;
        }
        return menu.driveStack(selectedDriveSlot);
    }

    private void syncLabelDraft() {
        if (editingLabel) {
            return;
        }
        labelDraft = SignalOsDataDriveItem.data(selectedDrive()).label();
    }

    private void normalizeNetworkRecord(List<SignalOsDataRecord> records) {
        if (records.isEmpty()) {
            selectedNetworkRecordId = null;
        } else if (selectedNetworkRecordId == null
                || records.stream().noneMatch(record -> record.id().equals(selectedNetworkRecordId))) {
            selectedNetworkRecordId = records.getFirst().id();
        }
    }

    private void normalizeDriveRecord(List<SignalOsDataRecord> records) {
        if (records.isEmpty()) {
            selectedDriveRecordId = null;
        } else if (selectedDriveRecordId == null
                || records.stream().noneMatch(record -> record.id().equals(selectedDriveRecordId))) {
            selectedDriveRecordId = records.getFirst().id();
        }
    }

    private List<Map.Entry<Identifier, SignalOsDriveData>> templateEntries() {
        return SignalOsContentRegistry.driveTemplateEntries().entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .toList();
    }

    private void playClick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getSoundManager() != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SignalOsThemedSounds.click(), 1.0F));
        }
    }

    private String trim(String text, int maxWidth) {
        if (text == null || maxWidth <= 0) {
            return "";
        }
        if (font.width(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        return font.plainSubstrByWidth(text, Math.max(0, maxWidth - font.width(ellipsis))) + ellipsis;
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseY >= y && mouseX < x + w && mouseY < y + h;
    }

    private record HitBox(HitKind kind, Identifier id, Identifier action, String payload, int x, int y, int w, int h) {
        boolean inside(double mouseX, double mouseY) {
            return SignalOsServerRackScreen.inside(mouseX, mouseY, x, y, w, h);
        }
    }

    private enum HitKind {
        NETWORK_RECORD,
        DRIVE_RECORD,
        TEMPLATE,
        LABEL,
        ACTION
    }
}
