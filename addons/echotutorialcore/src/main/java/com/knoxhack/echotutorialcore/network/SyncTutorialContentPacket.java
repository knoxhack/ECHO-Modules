package com.knoxhack.echotutorialcore.network;

import com.knoxhack.echotutorialcore.EchoTutorialCore;
import com.knoxhack.echotutorialcore.api.card.TutorialCard;
import com.knoxhack.echotutorialcore.api.hint.TutorialHint;
import com.knoxhack.echotutorialcore.api.tooltip.TutorialTooltip;
import com.knoxhack.echotutorialcore.api.trigger.TutorialFlow;
import com.knoxhack.echotutorialcore.api.trigger.TutorialStep;
import com.knoxhack.echotutorialcore.data.TutorialCoreRegistries;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SyncTutorialContentPacket(
        List<CardData> cards,
        List<HintData> hints,
        List<FlowData> flows,
        List<TooltipData> tooltips) implements CustomPacketPayload {
    private static final int MAX_ENTRIES = 4096;
    private static final int MAX_TEXT = 2048;
    private static final int MAX_ID = 192;

    public static final Type<SyncTutorialContentPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(EchoTutorialCore.MODID, "sync_content"));
    public static final StreamCodec<FriendlyByteBuf, SyncTutorialContentPacket> CODEC =
            StreamCodec.of(SyncTutorialContentPacket::write, SyncTutorialContentPacket::read);

    public SyncTutorialContentPacket {
        cards = clean(cards);
        hints = clean(hints);
        flows = clean(flows);
        tooltips = clean(tooltips);
    }

    public static SyncTutorialContentPacket fromRegistries() {
        return new SyncTutorialContentPacket(
                TutorialCoreRegistries.allCards().stream().map(CardData::from).toList(),
                TutorialCoreRegistries.allHints().stream().map(HintData::from).toList(),
                TutorialCoreRegistries.allFlows().stream().map(FlowData::from).toList(),
                TutorialCoreRegistries.allTooltips().stream().map(TooltipData::from).toList());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(FriendlyByteBuf buf, SyncTutorialContentPacket packet) {
        writeList(buf, packet.cards(), SyncTutorialContentPacket::writeCard);
        writeList(buf, packet.hints(), SyncTutorialContentPacket::writeHint);
        writeList(buf, packet.flows(), SyncTutorialContentPacket::writeFlow);
        writeList(buf, packet.tooltips(), SyncTutorialContentPacket::writeTooltip);
    }

    private static SyncTutorialContentPacket read(FriendlyByteBuf buf) {
        return new SyncTutorialContentPacket(
                readList(buf, SyncTutorialContentPacket::readCard),
                readList(buf, SyncTutorialContentPacket::readHint),
                readList(buf, SyncTutorialContentPacket::readFlow),
                readList(buf, SyncTutorialContentPacket::readTooltip));
    }

    private static void writeCard(FriendlyByteBuf buf, CardData card) {
        writeId(buf, card.id());
        buf.writeUtf(card.category(), 64);
        buf.writeUtf(card.title(), MAX_TEXT);
        buf.writeUtf(card.summary(), MAX_TEXT);
        writeStrings(buf, card.body());
        writeStrings(buf, card.steps());
        writeStrings(buf, card.commonMistakes());
        writeIds(buf, card.related());
        writeStrings(buf, card.unlockTriggers());
        buf.writeBoolean(card.defaultUnlocked());
        buf.writeUtf(card.addonOwnerId(), MAX_ID);
        buf.writeVarInt(card.priority());
    }

    private static CardData readCard(FriendlyByteBuf buf) {
        return new CardData(readId(buf), buf.readUtf(64), buf.readUtf(MAX_TEXT), buf.readUtf(MAX_TEXT),
                readStrings(buf), readStrings(buf), readStrings(buf), readIds(buf), readStrings(buf),
                buf.readBoolean(), buf.readUtf(MAX_ID), buf.readVarInt());
    }

    private static void writeHint(FriendlyByteBuf buf, HintData hint) {
        writeId(buf, hint.id());
        buf.writeUtf(hint.type(), 64);
        buf.writeUtf(hint.category(), 64);
        buf.writeUtf(hint.title(), MAX_TEXT);
        buf.writeUtf(hint.message(), MAX_TEXT);
        buf.writeUtf(hint.details(), MAX_TEXT);
        buf.writeUtf(hint.actionLabel(), MAX_TEXT);
        writeNullableId(buf, hint.actionCardId());
        buf.writeVarInt(hint.cooldownTicks());
        writeStrings(buf, hint.guideModes());
        buf.writeVarInt(hint.priority());
        buf.writeBoolean(hint.dismissible());
        writeStrings(buf, hint.conditions());
    }

    private static HintData readHint(FriendlyByteBuf buf) {
        return new HintData(readId(buf), buf.readUtf(64), buf.readUtf(64), buf.readUtf(MAX_TEXT),
                buf.readUtf(MAX_TEXT), buf.readUtf(MAX_TEXT), buf.readUtf(MAX_TEXT), readNullableId(buf),
                buf.readVarInt(), readStrings(buf), buf.readVarInt(), buf.readBoolean(), readStrings(buf));
    }

    private static void writeFlow(FriendlyByteBuf buf, FlowData flow) {
        writeId(buf, flow.id());
        buf.writeUtf(flow.title(), MAX_TEXT);
        buf.writeUtf(flow.category(), 64);
        writeList(buf, flow.steps(), SyncTutorialContentPacket::writeStep);
        writeIds(buf, flow.unlockCards());
        buf.writeBoolean(flow.defaultUnlocked());
    }

    private static FlowData readFlow(FriendlyByteBuf buf) {
        return new FlowData(readId(buf), buf.readUtf(MAX_TEXT), buf.readUtf(64),
                readList(buf, SyncTutorialContentPacket::readStep), readIds(buf), buf.readBoolean());
    }

    private static void writeStep(FriendlyByteBuf buf, StepData step) {
        buf.writeUtf(step.id(), MAX_ID);
        buf.writeUtf(step.type(), 64);
        writeNullableId(buf, step.target());
        buf.writeUtf(step.text(), MAX_TEXT);
        buf.writeBoolean(step.optional());
    }

    private static StepData readStep(FriendlyByteBuf buf) {
        return new StepData(buf.readUtf(MAX_ID), buf.readUtf(64), readNullableId(buf), buf.readUtf(MAX_TEXT),
                buf.readBoolean());
    }

    private static void writeTooltip(FriendlyByteBuf buf, TooltipData tooltip) {
        writeId(buf, tooltip.targetItem());
        writeStrings(buf, tooltip.lines());
        buf.writeBoolean(tooltip.requireShift());
        buf.writeVarInt(tooltip.priority());
    }

    private static TooltipData readTooltip(FriendlyByteBuf buf) {
        return new TooltipData(readId(buf), readStrings(buf), buf.readBoolean(), buf.readVarInt());
    }

    private static <T> void writeList(FriendlyByteBuf buf, List<T> values, Writer<T> writer) {
        buf.writeVarInt(Math.min(MAX_ENTRIES, values.size()));
        for (T value : values.stream().limit(MAX_ENTRIES).toList()) {
            writer.write(buf, value);
        }
    }

    private static <T> List<T> readList(FriendlyByteBuf buf, Reader<T> reader) {
        int count = Math.max(0, Math.min(MAX_ENTRIES, buf.readVarInt()));
        List<T> values = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            values.add(reader.read(buf));
        }
        return values;
    }

    private static void writeStrings(FriendlyByteBuf buf, List<String> values) {
        writeList(buf, values == null ? List.of() : values, (buffer, value) -> buffer.writeUtf(value, MAX_TEXT));
    }

    private static List<String> readStrings(FriendlyByteBuf buf) {
        return readList(buf, buffer -> buffer.readUtf(MAX_TEXT)).stream()
                .filter(value -> value != null && !value.isBlank())
                .toList();
    }

    private static void writeIds(FriendlyByteBuf buf, List<Identifier> values) {
        writeList(buf, values == null ? List.of() : values, SyncTutorialContentPacket::writeId);
    }

    private static List<Identifier> readIds(FriendlyByteBuf buf) {
        return readList(buf, SyncTutorialContentPacket::readId).stream()
                .filter(id -> id != null)
                .toList();
    }

    private static void writeNullableId(FriendlyByteBuf buf, Identifier id) {
        buf.writeBoolean(id != null);
        if (id != null) {
            writeId(buf, id);
        }
    }

    private static Identifier readNullableId(FriendlyByteBuf buf) {
        return buf.readBoolean() ? readId(buf) : null;
    }

    private static void writeId(FriendlyByteBuf buf, Identifier id) {
        buf.writeUtf(id == null ? "" : id.toString(), MAX_ID);
    }

    private static Identifier readId(FriendlyByteBuf buf) {
        return Identifier.tryParse(buf.readUtf(MAX_ID));
    }

    private static <T> List<T> clean(List<T> values) {
        return values == null ? List.of() : values.stream()
                .filter(value -> value != null)
                .limit(MAX_ENTRIES)
                .toList();
    }

    @FunctionalInterface
    private interface Writer<T> {
        void write(FriendlyByteBuf buf, T value);
    }

    @FunctionalInterface
    private interface Reader<T> {
        T read(FriendlyByteBuf buf);
    }

    public record CardData(
            Identifier id,
            String category,
            String title,
            String summary,
            List<String> body,
            List<String> steps,
            List<String> commonMistakes,
            List<Identifier> related,
            List<String> unlockTriggers,
            boolean defaultUnlocked,
            String addonOwnerId,
            int priority) {
        public static CardData from(TutorialCard card) {
            return new CardData(card.id(), card.category().name(), card.title(), card.summary(), card.body(),
                    card.steps(), card.commonMistakes(), card.related(), card.unlockTriggers(),
                    card.defaultUnlocked(), card.addonOwnerId(), card.priority());
        }
    }

    public record HintData(
            Identifier id,
            String type,
            String category,
            String title,
            String message,
            String details,
            String actionLabel,
            Identifier actionCardId,
            int cooldownTicks,
            List<String> guideModes,
            int priority,
            boolean dismissible,
            List<String> conditions) {
        public static HintData from(TutorialHint hint) {
            return new HintData(hint.id(), hint.type().name(), hint.category().name(), hint.title(), hint.message(),
                    hint.details(), hint.actionLabel(), hint.actionCardId(), hint.cooldownTicks(),
                    hint.guideModes().stream().map(Enum::name).toList(), hint.priority(), hint.dismissible(),
                    hint.conditions());
        }
    }

    public record FlowData(
            Identifier id,
            String title,
            String category,
            List<StepData> steps,
            List<Identifier> unlockCards,
            boolean defaultUnlocked) {
        public static FlowData from(TutorialFlow flow) {
            return new FlowData(flow.id(), flow.title(), flow.category().name(),
                    flow.steps().stream().map(StepData::from).toList(), flow.unlockCards(), flow.defaultUnlocked());
        }
    }

    public record StepData(
            String id,
            String type,
            Identifier target,
            String text,
            boolean optional) {
        public static StepData from(TutorialStep step) {
            return new StepData(step.id(), step.type().name(), step.target(), step.text(), step.optional());
        }
    }

    public record TooltipData(
            Identifier targetItem,
            List<String> lines,
            boolean requireShift,
            int priority) {
        public static TooltipData from(TutorialTooltip tooltip) {
            return new TooltipData(tooltip.targetItem(), tooltip.lines(), tooltip.requireShift(), tooltip.priority());
        }
    }
}
