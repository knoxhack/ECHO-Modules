package com.knoxhack.signalos.client;

import com.google.gson.JsonObject;
import com.knoxhack.signalos.api.SignalOsDataRecord;
import com.knoxhack.signalos.api.SignalOsNetLink;
import com.knoxhack.signalos.client.api.SignalOsAppRenderContext;
import com.knoxhack.signalos.client.api.SignalOsAppRenderer;
import com.knoxhack.signalos.service.SignalOsBuiltinActions;
import com.knoxhack.signalos.service.SignalOsNetService;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

public class SignalNetNativeRenderer implements SignalOsAppRenderer {
    private static final int TEXT = 0xFFEAF7FF;
    private static final int MUTED = 0xFF8CA7B5;
    private static final int CYAN = 0xFF66E8FF;
    private static final int GREEN = 0xFF91F7A5;
    private static final int WARN = 0xFFFFD166;
    private static final int RED = 0xFFFF8FA3;
    private static final int PANEL = 0xC8122432;
    private static final int ROW = 0x80132635;
    private static final int ROW_HOVER = 0xA01A3648;

    private String addressDraft = "";
    private String query = "";
    private String selectedAddress = "";
    private Focus focus = Focus.NONE;
    private List<SignalOsDataRecord> lastResults = List.of();
    private SignalOsDataRecord selectedRecord;

    @Override
    public void render(SignalOsAppRenderContext context, GuiGraphicsExtractor graphics, int mouseX, int mouseY,
            float partialTick) {
        Font font = context.minecraft().font;
        int x = context.x();
        int y = context.y();
        int w = context.width();
        int h = context.height();
        List<SignalOsDataRecord> netRecords = SignalOsNetService.searchRecords(SignalOsClientState.dataRecords(), query);
        lastResults = netRecords;
        normalizeSelection(netRecords);
        graphics.fill(x, y, x + w, y + h, 0xB8081720);
        header(graphics, font, x, y, w, context);
        int controlsY = y + 28;
        input(graphics, font, x + 8, controlsY, Math.max(90, w / 2 - 14), "ADDRESS", addressDraft, focus == Focus.ADDRESS);
        input(graphics, font, x + Math.max(98, w / 2 + 2), controlsY, Math.max(70, w / 2 - 10), "SEARCH", query, focus == Focus.SEARCH);
        int bodyY = controlsY + 28;
        int listW = w >= 300 ? Math.min(154, w / 2) : w - 16;
        drawResults(graphics, font, x + 8, bodyY, listW, h - 70, mouseX, mouseY);
        if (w >= 300) {
            drawDetail(graphics, font, x + 16 + listW, bodyY, w - listW - 24, h - 70, mouseX, mouseY, context);
        }
        if (!SignalOsClientState.lastActionStatus().isBlank()) {
            graphics.text(font, trim(SignalOsClientState.lastActionStatus(), font, w - 16), x + 8,
                    y + h - 12, CYAN, false);
        }
    }

    @Override
    public boolean mouseClicked(SignalOsAppRenderContext context, double mouseX, double mouseY, int button) {
        int x = context.x();
        int y = context.y();
        int w = context.width();
        int controlsY = y + 28;
        int addressW = Math.max(90, w / 2 - 14);
        int searchX = x + Math.max(98, w / 2 + 2);
        int searchW = Math.max(70, w / 2 - 10);
        if (inside(mouseX, mouseY, x + 8, controlsY, addressW, 20)) {
            focus = Focus.ADDRESS;
            return true;
        }
        if (inside(mouseX, mouseY, searchX, controlsY, searchW, 20)) {
            focus = Focus.SEARCH;
            return true;
        }
        int bodyY = controlsY + 28;
        int listW = w >= 300 ? Math.min(154, w / 2) : w - 16;
        int rowY = bodyY + 20;
        for (SignalOsDataRecord record : lastResults) {
            if (rowY + 27 > bodyY + context.height() - 76) {
                break;
            }
            if (inside(mouseX, mouseY, x + 8, rowY, listW, 25)) {
                openAddress(context, record.metadataValue(SignalOsNetService.META_ADDRESS, ""));
                return true;
            }
            rowY += 27;
        }
        if (w >= 300 && selectedRecord != null) {
            int detailX = x + 16 + listW;
            int detailW = w - listW - 24;
            int buttonY = y + context.height() - 44;
            if (button(context, mouseX, mouseY, detailX + 8, buttonY, 70, SignalOsBuiltinActions.BOOKMARK_NET_PAGE)) {
                return true;
            }
            if (button(context, mouseX, mouseY, detailX + 84, buttonY, 58, SignalOsBuiltinActions.SAVE_NET_PAGE)) {
                return true;
            }
            int linkY = bodyY + 94;
            for (SignalOsNetLink link : SignalOsNetService.decodedLinks(selectedRecord)) {
                if (inside(mouseX, mouseY, detailX + 8, linkY - 2, detailW - 16, 16)) {
                    openAddress(context, link.address());
                    return true;
                }
                linkY += 18;
            }
        }
        focus = Focus.NONE;
        return false;
    }

    @Override
    public boolean keyPressed(SignalOsAppRenderContext context, KeyEvent event) {
        if (focus == Focus.NONE) {
            return false;
        }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            focus = Focus.NONE;
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_BACKSPACE) {
            if (focus == Focus.ADDRESS && !addressDraft.isEmpty()) {
                addressDraft = addressDraft.substring(0, addressDraft.length() - 1);
            } else if (focus == Focus.SEARCH && !query.isEmpty()) {
                query = query.substring(0, query.length() - 1);
            }
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ENTER) {
            if (focus == Focus.ADDRESS) {
                openAddress(context, addressDraft);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(SignalOsAppRenderContext context, CharacterEvent event) {
        if (focus == Focus.NONE || !event.isAllowedChatCharacter()) {
            return false;
        }
        String typed = event.codepointAsString();
        if (focus == Focus.ADDRESS) {
            addressDraft = clamp(addressDraft + typed, 96);
        } else {
            query = clamp(query + typed, 60);
        }
        return true;
    }

    protected List<SignalOsDataRecord> currentResults() {
        return lastResults;
    }

    protected SignalOsDataRecord selectedRecord() {
        return selectedRecord;
    }

    protected String selectedAddress() {
        return selectedAddress;
    }

    protected String query() {
        return query;
    }

    protected void openAddress(SignalOsAppRenderContext context, String address) {
        String normalized = SignalOsNetService.normalizeAddress(address);
        if (normalized.isBlank()) {
            return;
        }
        selectedAddress = normalized;
        addressDraft = normalized;
        selectedRecord = lastResults.stream()
                .filter(record -> normalized.equals(record.metadataValue(SignalOsNetService.META_ADDRESS, "")))
                .findFirst()
                .orElse(null);
        send(context, SignalOsBuiltinActions.RECORD_NET_RECENT);
    }

    protected void send(SignalOsAppRenderContext context, net.minecraft.resources.Identifier action) {
        if (context == null || selectedRecord == null) {
            return;
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("address", selectedRecord.metadataValue(SignalOsNetService.META_ADDRESS, selectedAddress));
        context.sendAction(SignalOsBuiltinActions.PAGE_SIGNALNET, action, payload.toString());
    }

    private void normalizeSelection(List<SignalOsDataRecord> records) {
        if (records.isEmpty()) {
            selectedRecord = null;
            return;
        }
        selectedRecord = records.stream()
                .filter(record -> selectedAddress.equals(record.metadataValue(SignalOsNetService.META_ADDRESS, "")))
                .findFirst()
                .orElse(records.getFirst());
        selectedAddress = selectedRecord.metadataValue(SignalOsNetService.META_ADDRESS, "");
        if (addressDraft.isBlank()) {
            addressDraft = selectedAddress;
        }
    }

    private void header(GuiGraphicsExtractor graphics, Font font, int x, int y, int w, SignalOsAppRenderContext context) {
        graphics.text(font, "SIGNALNET", x + 8, y + 5, CYAN, false);
        String status = context.accessTier() + " tier | " + SignalOsClientState.networkId();
        graphics.text(font, trim(status, font, w - 86), x + 82, y + 5, MUTED, false);
    }

    private void input(GuiGraphicsExtractor graphics, Font font, int x, int y, int w, String label, String value,
            boolean focused) {
        graphics.fill(x, y, x + w, y + 20, focused ? 0xD0143142 : PANEL);
        graphics.outline(x, y, w, 20, focused ? CYAN : 0x4438DFF4);
        graphics.text(font, label, x + 5, y + 2, MUTED, false);
        graphics.text(font, trim(value.isBlank() ? "-" : value, font, w - 46), x + 43, y + 10, TEXT, false);
    }

    private void drawResults(GuiGraphicsExtractor graphics, Font font, int x, int y, int w, int h,
            int mouseX, int mouseY) {
        graphics.text(font, "RESULTS " + lastResults.size(), x + 2, y + 5, MUTED, false);
        int rowY = y + 20;
        for (SignalOsDataRecord record : lastResults) {
            if (rowY + 27 > y + h) {
                break;
            }
            boolean selected = record == selectedRecord;
            boolean hovered = inside(mouseX, mouseY, x, rowY, w, 25);
            graphics.fill(x, rowY, x + w, rowY + 25, selected ? 0xD0152B38 : hovered ? ROW_HOVER : ROW);
            graphics.outline(x, rowY, w, 25, selected ? CYAN : 0x3338DFF4);
            graphics.text(font, trim(record.title(), font, w - 10), x + 5, rowY + 4, selected ? TEXT : MUTED, false);
            graphics.text(font, trim(record.metadataValue(SignalOsNetService.META_ADDRESS, ""), font, w - 10),
                    x + 5, rowY + 14, MUTED, false);
            rowY += 27;
        }
    }

    private void drawDetail(GuiGraphicsExtractor graphics, Font font, int x, int y, int w, int h,
            int mouseX, int mouseY, SignalOsAppRenderContext context) {
        graphics.fill(x, y, x + w, y + h, PANEL);
        graphics.outline(x, y, w, h, 0x3338DFF4);
        if (selectedRecord == null) {
            graphics.text(font, "NO SIGNALNET PAGE", x + 8, y + 8, WARN, false);
            return;
        }
        graphics.text(font, trim(selectedRecord.title(), font, w - 16), x + 8, y + 8, TEXT, false);
        graphics.text(font, trim(selectedRecord.metadataValue(SignalOsNetService.META_ADDRESS, ""), font, w - 16),
                x + 8, y + 20, CYAN, false);
        int cy = y + 38;
        cy = wrapped(graphics, font, selectedRecord.body(), x + 8, cy, w - 16, MUTED, 5);
        graphics.text(font, "LINKS", x + 8, cy + 8, MUTED, false);
        int linkY = cy + 22;
        for (SignalOsNetLink link : SignalOsNetService.decodedLinks(selectedRecord)) {
            boolean hovered = inside(mouseX, mouseY, x + 8, linkY - 2, w - 16, 16);
            graphics.text(font, trim("> " + link.label() + " :: " + link.address(), font, w - 16),
                    x + 8, linkY, hovered ? GREEN : CYAN, false);
            linkY += 18;
        }
        int buttonY = y + h - 24;
        actionButton(graphics, font, mouseX, mouseY, "BOOKMARK", x + 8, buttonY, 70, CYAN,
                SignalOsClientState.activeDriveWritable());
        actionButton(graphics, font, mouseX, mouseY, "SAVE", x + 84, buttonY, 58, GREEN,
                SignalOsClientState.activeDriveWritable());
        if (!SignalOsClientState.activeDriveWritable()) {
            graphics.text(font, trim(SignalOsClientState.activeDriveStatus(), font, w - 154),
                    x + 148, buttonY + 5, RED, false);
        }
    }

    private boolean button(SignalOsAppRenderContext context, double mouseX, double mouseY, int x, int y, int w,
            net.minecraft.resources.Identifier action) {
        if (!SignalOsClientState.activeDriveWritable() || !inside(mouseX, mouseY, x, y, w, 18)) {
            return false;
        }
        send(context, action);
        return true;
    }

    private void actionButton(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY, String label,
            int x, int y, int w, int color, boolean enabled) {
        boolean hovered = enabled && inside(mouseX, mouseY, x, y, w, 18);
        graphics.fill(x, y, x + w, y + 18, enabled ? hovered ? 0xB01D455A : 0x80152B38 : 0x66151B22);
        graphics.outline(x, y, w, 18, enabled ? color : 0x44607078);
        graphics.text(font, trim(label, font, w - 8), x + 4, y + 5, enabled ? color : MUTED, false);
    }

    private int wrapped(GuiGraphicsExtractor graphics, Font font, String text, int x, int y, int maxWidth,
            int color, int maxLines) {
        if (text == null || text.isBlank()) {
            return y;
        }
        int cy = y;
        int lines = 0;
        for (String rawLine : text.split("\\R")) {
            String line = rawLine.strip();
            while (!line.isEmpty() && lines < maxLines) {
                int cut = fitChars(font, line, maxWidth);
                String part = line.substring(0, cut).strip();
                graphics.text(font, part, x, cy, color, false);
                line = line.substring(cut).strip();
                cy += 10;
                lines++;
            }
            if (lines >= maxLines) {
                break;
            }
        }
        return cy;
    }

    private String trim(String value, Font font, int maxWidth) {
        String safe = value == null ? "" : value;
        if (font.width(safe) <= maxWidth) {
            return safe;
        }
        String ellipsis = "...";
        int max = Math.max(0, maxWidth - font.width(ellipsis));
        while (!safe.isEmpty() && font.width(safe) > max) {
            safe = safe.substring(0, safe.length() - 1);
        }
        return safe + ellipsis;
    }

    private int fitChars(Font font, String value, int maxWidth) {
        if (font.width(value) <= maxWidth) {
            return value.length();
        }
        int cut = Math.min(value.length(), Math.max(1, maxWidth / 5));
        while (cut > 1 && font.width(value.substring(0, cut)) > maxWidth) {
            cut--;
        }
        int space = value.lastIndexOf(' ', cut);
        return space > 0 ? space : cut;
    }

    private static boolean inside(double px, double py, int x, int y, int w, int h) {
        return px >= x && py >= y && px < x + w && py < y + h;
    }

    private static String clamp(String value, int max) {
        String safe = value == null ? "" : value;
        return safe.length() <= max ? safe : safe.substring(0, max);
    }

    private enum Focus {
        NONE,
        ADDRESS,
        SEARCH
    }
}
