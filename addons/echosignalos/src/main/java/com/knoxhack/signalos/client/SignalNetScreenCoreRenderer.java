package com.knoxhack.signalos.client;

import com.google.gson.JsonObject;
import com.knoxhack.echonetcore.client.EchoNetClientActions;
import com.knoxhack.echoscreencore.api.EchoDataContext;
import com.knoxhack.echoscreencore.api.EchoScreenRegistry;
import com.knoxhack.echoscreencore.api.theme.EchoAccessibilitySettings;
import com.knoxhack.echoscreencore.client.api.EchoEmbeddedSurface;
import com.knoxhack.signalos.SignalOS;
import com.knoxhack.signalos.api.SignalOsDataRecord;
import com.knoxhack.signalos.api.SignalOsNetLink;
import com.knoxhack.signalos.client.api.SignalOsAppRenderContext;
import com.knoxhack.signalos.network.SignalOsActionPacket;
import com.knoxhack.signalos.service.SignalOsBuiltinActions;
import com.knoxhack.signalos.service.SignalOsNetService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.resources.Identifier;

public final class SignalNetScreenCoreRenderer extends SignalNetNativeRenderer {
    private static final Identifier PAGE_ID = Identifier.fromNamespaceAndPath(SignalOS.MODID, "signalnet");
    private static final AtomicBoolean ACTIONS_REGISTERED = new AtomicBoolean(false);
    private static String selectedAddress = "";
    private static String screenAddress = "";
    private static String screenQuery = "";
    private EchoEmbeddedSurface surface;
    private String netStatus = "";
    private String netDrive = "";
    private String lastSignature = "";

    public SignalNetScreenCoreRenderer() {
        registerActions();
    }

    @Override
    public void render(SignalOsAppRenderContext context, GuiGraphicsExtractor graphics, int mouseX, int mouseY,
            float partialTick) {
        netStatus = "tier " + context.accessTier() + " | " + SignalOsClientState.networkId();
        netDrive = SignalOsClientState.activeDriveWritable() ? "drive ready" : SignalOsClientState.activeDriveStatus();
        List<SignalOsDataRecord> records = records();
        SignalOsDataRecord selected = select(records);
        String signature = signature(records, selected);
        if (surface == null) {
            surface = new EchoEmbeddedSurface(PAGE_ID, dataContext(), EchoAccessibilitySettings.DEFAULT);
        } else if (!signature.equals(lastSignature)) {
            surface.markDataDirty();
        }
        lastSignature = signature;
        surface.render(graphics, context.x(), context.y(), context.width(), context.height(), mouseX, mouseY,
                partialTick);
    }

    @Override
    public boolean mouseClicked(SignalOsAppRenderContext context, double mouseX, double mouseY, int button) {
        if (surface != null && surface.mouseClicked(mouseX, mouseY, button)) {
            surface.markDataDirty();
            return true;
        }
        return super.mouseClicked(context, mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(SignalOsAppRenderContext context, double mouseX, double mouseY, int button) {
        return surface != null && surface.mouseReleased(mouseX, mouseY, button)
                || super.mouseReleased(context, mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(SignalOsAppRenderContext context, double mouseX, double mouseY, int button,
            double dragX, double dragY) {
        return surface != null && surface.mouseDragged(mouseX, mouseY, button, dragX, dragY)
                || super.mouseDragged(context, mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(SignalOsAppRenderContext context, double mouseX, double mouseY, double deltaY) {
        return surface != null && surface.mouseScrolled(mouseX, mouseY, deltaY)
                || super.mouseScrolled(context, mouseX, mouseY, deltaY);
    }

    @Override
    public boolean keyPressed(SignalOsAppRenderContext context, KeyEvent event) {
        if (surface != null && surface.keyPressed(event.key())) {
            surface.markDataDirty();
            return true;
        }
        return super.keyPressed(context, event);
    }

    @Override
    public boolean charTyped(SignalOsAppRenderContext context, CharacterEvent event) {
        if (surface != null && surface.charTyped(event.codepointAsString())) {
            surface.markDataDirty();
            return true;
        }
        return super.charTyped(context, event);
    }

    private static void registerActions() {
        if (!ACTIONS_REGISTERED.compareAndSet(false, true)) {
            return;
        }
        EchoScreenRegistry.registerAction("signalos.signalnet.select", action -> {
            selectedAddress = SignalOsNetService.normalizeAddress(action.actionValue());
            screenAddress = selectedAddress;
            sendServerAction(SignalOsBuiltinActions.RECORD_NET_RECENT);
            return !selectedAddress.isBlank();
        });
        EchoScreenRegistry.registerAction("signalos.signalnet.address", action -> {
            screenAddress = clamp(action.actionValue(), 96);
            return true;
        });
        EchoScreenRegistry.registerAction("signalos.signalnet.navigate", action -> {
            String normalized = SignalOsNetService.normalizeAddress(action.actionValue());
            if (normalized.isBlank()) {
                return false;
            }
            selectedAddress = normalized;
            screenAddress = normalized;
            sendServerAction(SignalOsBuiltinActions.RECORD_NET_RECENT);
            return true;
        });
        EchoScreenRegistry.registerAction("signalos.signalnet.search", action -> {
            screenQuery = clamp(action.actionValue(), 60);
            return true;
        });
        EchoScreenRegistry.registerAction("signalos.signalnet.bookmark", action -> {
            sendServerAction(SignalOsBuiltinActions.BOOKMARK_NET_PAGE);
            return true;
        });
        EchoScreenRegistry.registerAction("signalos.signalnet.save", action -> {
            sendServerAction(SignalOsBuiltinActions.SAVE_NET_PAGE);
            return true;
        });
    }

    private static void sendServerAction(Identifier actionId) {
        if (selectedAddress.isBlank()) {
            return;
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("address", selectedAddress);
        EchoNetClientActions.sendServerboundAction(new SignalOsActionPacket(
                SignalOsBuiltinActions.PAGE_SIGNALNET, actionId, payload.toString()));
    }

    private List<SignalOsDataRecord> records() {
        return SignalOsNetService.searchRecords(SignalOsClientState.dataRecords(), screenQuery);
    }

    private SignalOsDataRecord select(List<SignalOsDataRecord> records) {
        if (records.isEmpty()) {
            return null;
        }
        SignalOsDataRecord selected = records.stream()
                .filter(record -> selectedAddress.equals(record.metadataValue(SignalOsNetService.META_ADDRESS, "")))
                .findFirst()
                .orElse(records.getFirst());
        selectedAddress = selected.metadataValue(SignalOsNetService.META_ADDRESS, "");
        if (screenAddress.isBlank()) {
            screenAddress = selectedAddress;
        }
        return selected;
    }

    private EchoDataContext dataContext() {
        return EchoDataContext.empty().provider("net", (context, path) -> {
            List<SignalOsDataRecord> records = records();
            SignalOsDataRecord selected = select(records);
            if (path.isEmpty()) {
                return Map.of();
            }
            return switch (path.getFirst()) {
                case "status" -> netStatus;
                case "drive" -> netDrive;
                case "query" -> screenQuery;
                case "address" -> screenAddress;
                case "count" -> records.size();
                case "results" -> records.stream().map(this::row).toList();
                case "selected" -> selected == null ? Map.of() : row(selected);
                case "links" -> selected == null ? List.of() : SignalOsNetService.decodedLinks(selected).stream()
                        .map(this::linkRow)
                        .toList();
                default -> "";
            };
        });
    }

    private Map<String, Object> row(SignalOsDataRecord record) {
        LinkedHashMap<String, Object> row = new LinkedHashMap<>();
        row.put("title", record.title());
        row.put("address", record.metadataValue(SignalOsNetService.META_ADDRESS, ""));
        row.put("source", record.source());
        row.put("body", record.body());
        return row;
    }

    private Map<String, Object> linkRow(SignalOsNetLink link) {
        LinkedHashMap<String, Object> row = new LinkedHashMap<>();
        row.put("label", link.label());
        row.put("address", link.address());
        return row;
    }

    private String signature(List<SignalOsDataRecord> records, SignalOsDataRecord selected) {
        String addresses = records.stream()
                .map(record -> record.metadataValue(SignalOsNetService.META_ADDRESS, ""))
                .collect(Collectors.joining(","));
        String selectedValue = selected == null ? "" : selected.metadataValue(SignalOsNetService.META_ADDRESS, "");
        return screenQuery + "|" + screenAddress + "|" + selectedValue + "|" + netStatus + "|" + netDrive + "|"
                + addresses;
    }

    private static String clamp(String value, int max) {
        String safe = value == null ? "" : value;
        return safe.length() <= max ? safe : safe.substring(0, max);
    }
}
