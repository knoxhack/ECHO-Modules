package com.knoxhack.echo.scriptcore.network;

import com.knoxhack.echo.scriptcore.EchoScriptCore;
import com.knoxhack.echo.scriptcore.runtime.ScriptCoreUiExecutionService;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ScriptCoreUiActionPacket(
        ScriptCoreUiExecutionService.UiExecutionMode mode,
        Identifier definitionId,
        String slot,
        String pageId,
        String componentId,
        String actionValue,
        Map<String, String> params) implements CustomPacketPayload {
    private static final int MAX_IDENTIFIER_LENGTH = 160;
    private static final int MAX_SLOT_LENGTH = 64;
    private static final int MAX_PAGE_ID_LENGTH = 160;
    private static final int MAX_COMPONENT_ID_LENGTH = 80;
    private static final int MAX_ACTION_VALUE_LENGTH = 512;
    private static final int MAX_MODE_LENGTH = 16;

    public static final Identifier ID = EchoScriptCore.id("screencore_ui_action");
    public static final Type<ScriptCoreUiActionPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, ScriptCoreUiActionPacket> CODEC =
            StreamCodec.of(ScriptCoreUiActionPacket::write, ScriptCoreUiActionPacket::read);

    public ScriptCoreUiActionPacket {
        mode = mode == null ? ScriptCoreUiExecutionService.UiExecutionMode.EXECUTE : mode;
        definitionId = definitionId == null ? EchoScriptCore.id("missing") : definitionId;
        slot = limit(slot, MAX_SLOT_LENGTH);
        pageId = limit(pageId, MAX_PAGE_ID_LENGTH);
        componentId = limit(componentId, MAX_COMPONENT_ID_LENGTH);
        actionValue = limit(actionValue, MAX_ACTION_VALUE_LENGTH);
        params = boundedParams(params);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(FriendlyByteBuf buffer, ScriptCoreUiActionPacket packet) {
        buffer.writeUtf(packet.mode.wireName(), MAX_MODE_LENGTH);
        writeIdentifier(buffer, packet.definitionId);
        buffer.writeUtf(packet.slot, MAX_SLOT_LENGTH);
        buffer.writeUtf(packet.pageId, MAX_PAGE_ID_LENGTH);
        buffer.writeUtf(packet.componentId, MAX_COMPONENT_ID_LENGTH);
        buffer.writeUtf(packet.actionValue, MAX_ACTION_VALUE_LENGTH);
        buffer.writeVarInt(packet.params.size());
        for (Map.Entry<String, String> entry : packet.params.entrySet()) {
            buffer.writeUtf(entry.getKey(), ScriptCoreUiExecutionService.MAX_PARAM_KEY_LENGTH);
            buffer.writeUtf(entry.getValue(), ScriptCoreUiExecutionService.MAX_PARAM_VALUE_LENGTH);
        }
    }

    private static ScriptCoreUiActionPacket read(FriendlyByteBuf buffer) {
        ScriptCoreUiExecutionService.UiExecutionMode mode =
                ScriptCoreUiExecutionService.UiExecutionMode.fromWire(buffer.readUtf(MAX_MODE_LENGTH));
        return new ScriptCoreUiActionPacket(
                mode,
                readIdentifier(buffer),
                buffer.readUtf(MAX_SLOT_LENGTH),
                buffer.readUtf(MAX_PAGE_ID_LENGTH),
                buffer.readUtf(MAX_COMPONENT_ID_LENGTH),
                buffer.readUtf(MAX_ACTION_VALUE_LENGTH),
                readParams(buffer));
    }

    private static void writeIdentifier(FriendlyByteBuf buffer, Identifier id) {
        buffer.writeUtf(limit(id == null ? "" : id.toString(), MAX_IDENTIFIER_LENGTH), MAX_IDENTIFIER_LENGTH);
    }

    private static Identifier readIdentifier(FriendlyByteBuf buffer) {
        Identifier id = Identifier.tryParse(buffer.readUtf(MAX_IDENTIFIER_LENGTH));
        return id == null ? EchoScriptCore.id("missing") : id;
    }

    private static String limit(String value, int maxLength) {
        String clean = value == null ? "" : value.trim();
        return clean.length() <= maxLength ? clean : clean.substring(0, maxLength);
    }

    private static Map<String, String> readParams(FriendlyByteBuf buffer) {
        int count = Math.max(0, buffer.readVarInt());
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            String key = limit(buffer.readUtf(ScriptCoreUiExecutionService.MAX_PARAM_KEY_LENGTH),
                    ScriptCoreUiExecutionService.MAX_PARAM_KEY_LENGTH);
            String value = limit(buffer.readUtf(ScriptCoreUiExecutionService.MAX_PARAM_VALUE_LENGTH),
                    ScriptCoreUiExecutionService.MAX_PARAM_VALUE_LENGTH);
            if (params.size() < ScriptCoreUiExecutionService.MAX_PARAMS_PER_TRIGGER && !key.isBlank()) {
                params.put(key, value);
            }
        }
        return params;
    }

    private static Map<String, String> boundedParams(Map<String, String> raw) {
        if (raw == null || raw.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : raw.entrySet()) {
            if (params.size() >= ScriptCoreUiExecutionService.MAX_PARAMS_PER_TRIGGER) {
                break;
            }
            String key = limit(entry.getKey(), ScriptCoreUiExecutionService.MAX_PARAM_KEY_LENGTH);
            if (!key.isBlank()) {
                params.put(key, limit(entry.getValue(), ScriptCoreUiExecutionService.MAX_PARAM_VALUE_LENGTH));
            }
        }
        return Map.copyOf(params);
    }
}
