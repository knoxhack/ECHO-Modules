package com.knoxhack.echo.scriptcore.network;

import com.knoxhack.echo.scriptcore.EchoScriptCore;
import com.knoxhack.echo.scriptcore.runtime.ScriptCoreUiExecutionService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ScriptCoreUiResultPacket(
        ScriptCoreUiExecutionService.UiExecutionMode mode,
        Identifier definitionId,
        String slot,
        String pageId,
        String componentId,
        boolean success,
        String code,
        String message,
        int actionCount,
        int executedActions) implements CustomPacketPayload {
    private static final int MAX_IDENTIFIER_LENGTH = 160;
    private static final int MAX_SLOT_LENGTH = 64;
    private static final int MAX_PAGE_ID_LENGTH = 160;
    private static final int MAX_COMPONENT_ID_LENGTH = 80;
    private static final int MAX_CODE_LENGTH = 64;
    private static final int MAX_MESSAGE_LENGTH = 256;
    private static final int MAX_MODE_LENGTH = 16;

    public static final Identifier ID = EchoScriptCore.id("screencore_ui_result");
    public static final Type<ScriptCoreUiResultPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, ScriptCoreUiResultPacket> CODEC =
            StreamCodec.of(ScriptCoreUiResultPacket::write, ScriptCoreUiResultPacket::read);

    public ScriptCoreUiResultPacket {
        mode = mode == null ? ScriptCoreUiExecutionService.UiExecutionMode.EXECUTE : mode;
        definitionId = definitionId == null ? EchoScriptCore.id("missing") : definitionId;
        slot = limit(slot, MAX_SLOT_LENGTH);
        pageId = limit(pageId, MAX_PAGE_ID_LENGTH);
        componentId = limit(componentId, MAX_COMPONENT_ID_LENGTH);
        code = limit(code == null || code.isBlank() ? (success ? "ok" : "rejected") : code, MAX_CODE_LENGTH);
        message = limit(message, MAX_MESSAGE_LENGTH);
        actionCount = Math.max(0, actionCount);
        executedActions = Math.max(0, executedActions);
    }

    public static ScriptCoreUiResultPacket from(ScriptCoreUiExecutionService.UiExecutionResult result) {
        return new ScriptCoreUiResultPacket(
                result.mode(),
                result.definitionId(),
                result.slot(),
                result.pageId(),
                result.componentId(),
                result.success(),
                result.code(),
                result.message(),
                result.actionCount(),
                result.executedActions());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(FriendlyByteBuf buffer, ScriptCoreUiResultPacket packet) {
        buffer.writeUtf(packet.mode.wireName(), MAX_MODE_LENGTH);
        writeIdentifier(buffer, packet.definitionId);
        buffer.writeUtf(packet.slot, MAX_SLOT_LENGTH);
        buffer.writeUtf(packet.pageId, MAX_PAGE_ID_LENGTH);
        buffer.writeUtf(packet.componentId, MAX_COMPONENT_ID_LENGTH);
        buffer.writeBoolean(packet.success);
        buffer.writeUtf(packet.code, MAX_CODE_LENGTH);
        buffer.writeUtf(packet.message, MAX_MESSAGE_LENGTH);
        buffer.writeVarInt(packet.actionCount);
        buffer.writeVarInt(packet.executedActions);
    }

    private static ScriptCoreUiResultPacket read(FriendlyByteBuf buffer) {
        return new ScriptCoreUiResultPacket(
                ScriptCoreUiExecutionService.UiExecutionMode.fromWire(buffer.readUtf(MAX_MODE_LENGTH)),
                readIdentifier(buffer),
                buffer.readUtf(MAX_SLOT_LENGTH),
                buffer.readUtf(MAX_PAGE_ID_LENGTH),
                buffer.readUtf(MAX_COMPONENT_ID_LENGTH),
                buffer.readBoolean(),
                buffer.readUtf(MAX_CODE_LENGTH),
                buffer.readUtf(MAX_MESSAGE_LENGTH),
                buffer.readVarInt(),
                buffer.readVarInt());
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
}
