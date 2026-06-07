package com.knoxhack.echomultiblockcore.network;

import com.knoxhack.echomultiblockcore.EchoMultiblockCore;
import com.knoxhack.echomultiblockcore.api.AutomationRecipeRegistry;
import com.knoxhack.echomultiblockcore.api.MultiblockAutomationRecipe;
import com.knoxhack.echonetcore.api.EchoPayloadCodecs;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record AutomationRecipeMetadataPacket(List<Entry> entries) implements CustomPacketPayload {
    private static final int MAX_ENTRIES = 1024;
    public static final Identifier ID = EchoMultiblockCore.id("automation_recipe_metadata");
    public static final Type<AutomationRecipeMetadataPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, AutomationRecipeMetadataPacket> CODEC =
            StreamCodec.of(AutomationRecipeMetadataPacket::write, AutomationRecipeMetadataPacket::read);

    public AutomationRecipeMetadataPacket {
        entries = List.copyOf(entries == null ? List.of() : entries.stream().limit(MAX_ENTRIES).toList());
    }

    public static AutomationRecipeMetadataPacket current() {
        return new AutomationRecipeMetadataPacket(AutomationRecipeRegistry.all().stream().map(Entry::from).toList());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buffer, AutomationRecipeMetadataPacket packet) {
        buffer.writeVarInt(packet.entries().size());
        for (Entry entry : packet.entries()) {
            EchoPayloadCodecs.writeIdentifier(buffer, entry.id());
            buffer.writeUtf(entry.displayName(), 160);
            EchoPayloadCodecs.writeIdentifier(buffer, entry.category());
            buffer.writeUtf(entry.allowedMultiblocks(), 512);
            buffer.writeUtf(entry.requiredWorkcell(), 64);
            buffer.writeUtf(entry.tools(), 160);
            buffer.writeVarInt(entry.durationTicks());
            buffer.writeUtf(entry.inputs(), 256);
            buffer.writeUtf(entry.outputs(), 256);
            buffer.writeUtf(entry.effects(), 256);
        }
    }

    private static AutomationRecipeMetadataPacket read(RegistryFriendlyByteBuf buffer) {
        int count = Math.max(0, Math.min(MAX_ENTRIES, buffer.readVarInt()));
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            entries.add(new Entry(
                    EchoPayloadCodecs.readIdentifier(buffer),
                    buffer.readUtf(160),
                    EchoPayloadCodecs.readIdentifier(buffer),
                    buffer.readUtf(512),
                    buffer.readUtf(64),
                    buffer.readUtf(160),
                    buffer.readVarInt(),
                    buffer.readUtf(256),
                    buffer.readUtf(256),
                    buffer.readUtf(256)));
        }
        return new AutomationRecipeMetadataPacket(entries);
    }

    public record Entry(
            Identifier id,
            String displayName,
            Identifier category,
            String allowedMultiblocks,
            String requiredWorkcell,
            String tools,
            int durationTicks,
            String inputs,
            String outputs,
            String effects) {
        public Entry {
            displayName = displayName == null || displayName.isBlank() ? id.getPath() : displayName.strip();
            allowedMultiblocks = allowedMultiblocks == null ? "" : allowedMultiblocks.strip();
            requiredWorkcell = requiredWorkcell == null || requiredWorkcell.isBlank() ? "ASSEMBLY" : requiredWorkcell.strip();
            tools = tools == null ? "" : tools.strip();
            durationTicks = Math.max(20, durationTicks);
            inputs = inputs == null ? "" : inputs.strip();
            outputs = outputs == null ? "" : outputs.strip();
            effects = effects == null ? "" : effects.strip();
        }

        public boolean allows(Identifier definitionId) {
            if (definitionId == null || allowedMultiblocks.isBlank()) {
                return true;
            }
            for (String raw : allowedMultiblocks.split(",")) {
                Identifier allowed = Identifier.tryParse(raw.strip());
                if (definitionId.equals(allowed)) {
                    return true;
                }
            }
            return false;
        }

        public static Entry from(MultiblockAutomationRecipe recipe) {
            return new Entry(
                    recipe.id(),
                    recipe.displayName(),
                    recipe.category(),
                    recipe.allowedMultiblocks().stream().map(Identifier::toString).reduce((left, right) -> left + ", " + right).orElse(""),
                    recipe.requiredWorkcell().name(),
                    recipe.requiredTools().stream().map(Enum::name).reduce((left, right) -> left + ", " + right).orElse(""),
                    recipe.durationTicks(),
                    recipe.inputSummary(),
                    recipe.outputSummary(),
                    recipe.effects().stream().map(Identifier::toString).reduce((left, right) -> left + ", " + right).orElse(""));
        }
    }
}
