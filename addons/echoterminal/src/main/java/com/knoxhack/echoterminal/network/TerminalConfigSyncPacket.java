package com.knoxhack.echoterminal.network;

import com.knoxhack.echocore.api.config.EchoConfigCategorySnapshot;
import com.knoxhack.echocore.api.config.EchoConfigEntrySnapshot;
import com.knoxhack.echocore.api.config.EchoConfigModuleSnapshot;
import com.knoxhack.echocore.api.config.EchoConfigSide;
import com.knoxhack.echocore.api.config.EchoConfigValueKind;
import com.knoxhack.echoterminal.EchoTerminal;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record TerminalConfigSyncPacket(
        List<EchoConfigModuleSnapshot> modules,
        String status) implements CustomPacketPayload {
    private static final int MAX_ID = 160;
    private static final int MAX_TEXT = 320;
    private static final int MAX_MODULES = 64;
    private static final int MAX_CATEGORIES = 64;
    private static final int MAX_ENTRIES = 512;
    private static final int MAX_OPTIONS = 64;

    public static final Identifier ID = Identifier.fromNamespaceAndPath(EchoTerminal.MODID, "terminal_config_sync");
    public static final Type<TerminalConfigSyncPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalConfigSyncPacket> CODEC =
            StreamCodec.of(TerminalConfigSyncPacket::write, TerminalConfigSyncPacket::read);

    public TerminalConfigSyncPacket {
        modules = List.copyOf(modules == null
                ? List.of()
                : modules.stream().filter(module -> module != null).limit(MAX_MODULES).toList());
        status = status == null ? "" : trim(status, MAX_TEXT);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buffer, TerminalConfigSyncPacket packet) {
        buffer.writeVarInt(Math.min(MAX_MODULES, packet.modules().size()));
        for (EchoConfigModuleSnapshot module : packet.modules().stream().limit(MAX_MODULES).toList()) {
            buffer.writeUtf(module.moduleId(), MAX_ID);
            buffer.writeUtf(module.displayName(), MAX_TEXT);
            buffer.writeVarInt(Math.min(MAX_CATEGORIES, module.categories().size()));
            int writtenEntries = 0;
            for (EchoConfigCategorySnapshot category : module.categories().stream().limit(MAX_CATEGORIES).toList()) {
                buffer.writeUtf(category.categoryId(), MAX_ID);
                buffer.writeUtf(category.title(), MAX_TEXT);
                List<EchoConfigEntrySnapshot> entries = category.entries().stream()
                        .limit(Math.max(0, MAX_ENTRIES - writtenEntries))
                        .toList();
                buffer.writeVarInt(entries.size());
                for (EchoConfigEntrySnapshot entry : entries) {
                    writeEntry(buffer, entry);
                    writtenEntries++;
                }
            }
        }
        buffer.writeUtf(packet.status(), MAX_TEXT);
    }

    private static TerminalConfigSyncPacket read(RegistryFriendlyByteBuf buffer) {
        int moduleCount = Math.max(0, Math.min(MAX_MODULES, buffer.readVarInt()));
        List<EchoConfigModuleSnapshot> modules = new ArrayList<>();
        int readEntries = 0;
        for (int i = 0; i < moduleCount; i++) {
            String moduleId = buffer.readUtf(MAX_ID);
            String displayName = buffer.readUtf(MAX_TEXT);
            int categoryCount = Math.max(0, Math.min(MAX_CATEGORIES, buffer.readVarInt()));
            List<EchoConfigCategorySnapshot> categories = new ArrayList<>();
            for (int c = 0; c < categoryCount; c++) {
                String categoryId = buffer.readUtf(MAX_ID);
                String title = buffer.readUtf(MAX_TEXT);
                int entryCount = Math.max(0, Math.min(MAX_ENTRIES - readEntries, buffer.readVarInt()));
                List<EchoConfigEntrySnapshot> entries = new ArrayList<>();
                for (int e = 0; e < entryCount; e++) {
                    entries.add(readEntry(buffer));
                    readEntries++;
                }
                categories.add(new EchoConfigCategorySnapshot(categoryId, title, entries));
            }
            modules.add(new EchoConfigModuleSnapshot(moduleId, displayName, categories));
        }
        return new TerminalConfigSyncPacket(modules, buffer.readUtf(MAX_TEXT));
    }

    private static void writeEntry(RegistryFriendlyByteBuf buffer, EchoConfigEntrySnapshot entry) {
        buffer.writeUtf(entry.moduleId(), MAX_ID);
        buffer.writeUtf(entry.categoryId(), MAX_ID);
        buffer.writeUtf(entry.entryId(), MAX_ID);
        buffer.writeUtf(entry.label(), MAX_TEXT);
        buffer.writeUtf(entry.description(), MAX_TEXT);
        buffer.writeUtf(entry.side().name(), 32);
        buffer.writeUtf(entry.kind().name(), 32);
        buffer.writeUtf(entry.value(), MAX_TEXT);
        buffer.writeUtf(entry.defaultValue(), MAX_TEXT);
        buffer.writeUtf(entry.minValue(), MAX_TEXT);
        buffer.writeUtf(entry.maxValue(), MAX_TEXT);
        buffer.writeVarInt(Math.min(MAX_OPTIONS, entry.options().size()));
        for (String option : entry.options().stream().limit(MAX_OPTIONS).toList()) {
            buffer.writeUtf(option, MAX_TEXT);
        }
        buffer.writeBoolean(entry.editable());
        buffer.writeBoolean(entry.restartRequired());
        buffer.writeBoolean(entry.newWorldOnly());
        buffer.writeUtf(entry.status(), MAX_TEXT);
    }

    private static EchoConfigEntrySnapshot readEntry(RegistryFriendlyByteBuf buffer) {
        String moduleId = buffer.readUtf(MAX_ID);
        String categoryId = buffer.readUtf(MAX_ID);
        String entryId = buffer.readUtf(MAX_ID);
        String label = buffer.readUtf(MAX_TEXT);
        String description = buffer.readUtf(MAX_TEXT);
        EchoConfigSide side = safeSide(buffer.readUtf(32));
        EchoConfigValueKind kind = safeKind(buffer.readUtf(32));
        String value = buffer.readUtf(MAX_TEXT);
        String defaultValue = buffer.readUtf(MAX_TEXT);
        String minValue = buffer.readUtf(MAX_TEXT);
        String maxValue = buffer.readUtf(MAX_TEXT);
        int optionCount = Math.max(0, Math.min(MAX_OPTIONS, buffer.readVarInt()));
        List<String> options = new ArrayList<>();
        for (int i = 0; i < optionCount; i++) {
            options.add(buffer.readUtf(MAX_TEXT));
        }
        boolean editable = buffer.readBoolean();
        boolean restartRequired = buffer.readBoolean();
        boolean newWorldOnly = buffer.readBoolean();
        String status = buffer.readUtf(MAX_TEXT);
        return new EchoConfigEntrySnapshot(moduleId, categoryId, entryId, label, description, side, kind,
                value, defaultValue, minValue, maxValue, options, editable, restartRequired, newWorldOnly, status);
    }

    private static EchoConfigSide safeSide(String value) {
        try {
            return EchoConfigSide.valueOf(value);
        } catch (RuntimeException exception) {
            return EchoConfigSide.COMMON;
        }
    }

    private static EchoConfigValueKind safeKind(String value) {
        try {
            return EchoConfigValueKind.valueOf(value);
        } catch (RuntimeException exception) {
            return EchoConfigValueKind.STRING;
        }
    }

    private static String trim(String value, int limit) {
        String cleaned = value == null ? "" : value.strip();
        return cleaned.length() <= limit ? cleaned : cleaned.substring(0, limit);
    }
}
