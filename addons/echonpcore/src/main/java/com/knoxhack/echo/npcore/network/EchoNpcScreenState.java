package com.knoxhack.echo.npcore.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;

public record EchoNpcScreenState(
        int entityId,
        String profileId,
        String displayName,
        String role,
        String faction,
        String relationship,
        String portraitTexture,
        String badgeTexture,
        String frameTexture,
        String themeId,
        String currentTab,
        String dialogueNodeId,
        String dialogueText,
        List<DialogueOptionState> dialogueOptions,
        List<TradeGroupState> tradeGroups,
        List<ServiceState> services,
        String status) {
    public EchoNpcScreenState {
        profileId = clean(profileId);
        displayName = clean(displayName);
        role = clean(role);
        faction = clean(faction);
        relationship = clean(relationship);
        portraitTexture = clean(portraitTexture);
        badgeTexture = clean(badgeTexture);
        frameTexture = clean(frameTexture);
        themeId = clean(themeId);
        currentTab = clean(currentTab).isBlank() ? "talk" : clean(currentTab);
        dialogueNodeId = clean(dialogueNodeId);
        dialogueText = clean(dialogueText);
        dialogueOptions = List.copyOf(dialogueOptions == null ? List.of() : dialogueOptions);
        tradeGroups = List.copyOf(tradeGroups == null ? List.of() : tradeGroups);
        services = List.copyOf(services == null ? List.of() : services);
        status = clean(status);
    }

    public static void write(FriendlyByteBuf buf, EchoNpcScreenState state) {
        buf.writeVarInt(state.entityId);
        writeString(buf, state.profileId);
        writeString(buf, state.displayName);
        writeString(buf, state.role);
        writeString(buf, state.faction);
        writeString(buf, state.relationship);
        writeString(buf, state.portraitTexture);
        writeString(buf, state.badgeTexture);
        writeString(buf, state.frameTexture);
        writeString(buf, state.themeId);
        writeString(buf, state.currentTab);
        writeString(buf, state.dialogueNodeId);
        writeString(buf, state.dialogueText);
        buf.writeVarInt(state.dialogueOptions.size());
        state.dialogueOptions.forEach(option -> option.write(buf));
        buf.writeVarInt(state.tradeGroups.size());
        state.tradeGroups.forEach(group -> group.write(buf));
        buf.writeVarInt(state.services.size());
        state.services.forEach(service -> service.write(buf));
        writeString(buf, state.status);
    }

    public static EchoNpcScreenState read(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        String profileId = readString(buf);
        String displayName = readString(buf);
        String role = readString(buf);
        String faction = readString(buf);
        String relationship = readString(buf);
        String portraitTexture = readString(buf);
        String badgeTexture = readString(buf);
        String frameTexture = readString(buf);
        String themeId = readString(buf);
        String currentTab = readString(buf);
        String dialogueNodeId = readString(buf);
        String dialogueText = readString(buf);
        List<DialogueOptionState> options = new ArrayList<>();
        int optionCount = buf.readVarInt();
        for (int i = 0; i < optionCount; i++) {
            options.add(DialogueOptionState.read(buf));
        }
        List<TradeGroupState> groups = new ArrayList<>();
        int groupCount = buf.readVarInt();
        for (int i = 0; i < groupCount; i++) {
            groups.add(TradeGroupState.read(buf));
        }
        List<ServiceState> services = new ArrayList<>();
        int serviceCount = buf.readVarInt();
        for (int i = 0; i < serviceCount; i++) {
            services.add(ServiceState.read(buf));
        }
        return new EchoNpcScreenState(entityId, profileId, displayName, role, faction, relationship,
                portraitTexture, badgeTexture, frameTexture, themeId, currentTab, dialogueNodeId, dialogueText,
                options, groups, services, readString(buf));
    }

    private static void writeString(FriendlyByteBuf buf, String value) {
        buf.writeUtf(clean(value), 32767);
    }

    private static String readString(FriendlyByteBuf buf) {
        return buf.readUtf(32767);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    public record DialogueOptionState(
            String id,
            String label,
            String action,
            String next,
            boolean available,
            String disabledReason,
            String target,
            String actionId) {
        public DialogueOptionState(String id, String label, String action, String next) {
            this(id, label, action, next, true, "", "", "");
        }

        public DialogueOptionState {
            id = clean(id);
            label = clean(label);
            action = clean(action);
            next = clean(next);
            disabledReason = clean(disabledReason);
            target = clean(target);
            actionId = clean(actionId);
        }

        private void write(FriendlyByteBuf buf) {
            writeString(buf, id);
            writeString(buf, label);
            writeString(buf, action);
            writeString(buf, next);
            buf.writeBoolean(available);
            writeString(buf, disabledReason);
            writeString(buf, target);
            writeString(buf, actionId);
        }

        private static DialogueOptionState read(FriendlyByteBuf buf) {
            return new DialogueOptionState(readString(buf), readString(buf), readString(buf), readString(buf),
                    buf.readBoolean(), readString(buf), readString(buf), readString(buf));
        }
    }

    public record TradeGroupState(String id, String title, List<TradeOfferState> offers) {
        public TradeGroupState {
            id = clean(id);
            title = clean(title);
            offers = List.copyOf(offers == null ? List.of() : offers);
        }

        private void write(FriendlyByteBuf buf) {
            writeString(buf, id);
            writeString(buf, title);
            buf.writeVarInt(offers.size());
            offers.forEach(offer -> offer.write(buf));
        }

        private static TradeGroupState read(FriendlyByteBuf buf) {
            String id = readString(buf);
            String title = readString(buf);
            List<TradeOfferState> offers = new ArrayList<>();
            int count = buf.readVarInt();
            for (int i = 0; i < count; i++) {
                offers.add(TradeOfferState.read(buf));
            }
            return new TradeGroupState(id, title, offers);
        }
    }

    public record TradeOfferState(
            String id,
            String title,
            List<CostState> input,
            CostState output,
            int stock,
            boolean limitedStock,
            String requiresMission,
            boolean missionAllowed,
            String missionMessage,
            int requiresFactionStanding,
            boolean factionAllowed,
            String factionMessage,
            String disabledReason,
            int restockTime,
            long restockRemaining) {
        public TradeOfferState {
            id = clean(id);
            title = clean(title);
            input = List.copyOf(input == null ? List.of() : input);
            output = output == null ? new CostState("", 0) : output;
            requiresMission = clean(requiresMission);
            missionMessage = clean(missionMessage);
            factionMessage = clean(factionMessage);
            disabledReason = clean(disabledReason);
            restockTime = Math.max(0, restockTime);
            restockRemaining = Math.max(0L, restockRemaining);
        }

        private void write(FriendlyByteBuf buf) {
            writeString(buf, id);
            writeString(buf, title);
            buf.writeVarInt(input.size());
            input.forEach(cost -> cost.write(buf));
            output.write(buf);
            buf.writeVarInt(stock);
            buf.writeBoolean(limitedStock);
            writeString(buf, requiresMission);
            buf.writeBoolean(missionAllowed);
            writeString(buf, missionMessage);
            buf.writeVarInt(requiresFactionStanding);
            buf.writeBoolean(factionAllowed);
            writeString(buf, factionMessage);
            writeString(buf, disabledReason);
            buf.writeVarInt(restockTime);
            buf.writeLong(restockRemaining);
        }

        private static TradeOfferState read(FriendlyByteBuf buf) {
            String id = readString(buf);
            String title = readString(buf);
            List<CostState> input = new ArrayList<>();
            int count = buf.readVarInt();
            for (int i = 0; i < count; i++) {
                input.add(CostState.read(buf));
            }
            return new TradeOfferState(id, title, input, CostState.read(buf), buf.readVarInt(), buf.readBoolean(),
                    readString(buf), buf.readBoolean(), readString(buf), buf.readVarInt(), buf.readBoolean(),
                    readString(buf), readString(buf), buf.readVarInt(), buf.readLong());
        }
    }

    public record ServiceState(
            String id,
            String title,
            String description,
            List<CostState> cost,
            String action,
            int amount,
            int cooldown,
            long cooldownRemaining,
            String requiresMission,
            boolean missionAllowed,
            String missionMessage,
            int requiresFactionStanding,
            boolean factionAllowed,
            String disabledReason,
            String target,
            String actionId) {
        public ServiceState(String id, String title, String description, List<CostState> cost, String action,
                int amount, int cooldown, long cooldownRemaining) {
            this(id, title, description, cost, action, amount, cooldown, cooldownRemaining, "",
                    true, "", Integer.MIN_VALUE, true, "", "", "");
        }

        public ServiceState {
            id = clean(id);
            title = clean(title);
            description = clean(description);
            cost = List.copyOf(cost == null ? List.of() : cost);
            action = clean(action);
            requiresMission = clean(requiresMission);
            missionMessage = clean(missionMessage);
            disabledReason = clean(disabledReason);
            target = clean(target);
            actionId = clean(actionId);
        }

        private void write(FriendlyByteBuf buf) {
            writeString(buf, id);
            writeString(buf, title);
            writeString(buf, description);
            buf.writeVarInt(cost.size());
            cost.forEach(item -> item.write(buf));
            writeString(buf, action);
            buf.writeVarInt(amount);
            buf.writeVarInt(cooldown);
            buf.writeLong(cooldownRemaining);
            writeString(buf, requiresMission);
            buf.writeBoolean(missionAllowed);
            writeString(buf, missionMessage);
            buf.writeVarInt(requiresFactionStanding);
            buf.writeBoolean(factionAllowed);
            writeString(buf, disabledReason);
            writeString(buf, target);
            writeString(buf, actionId);
        }

        private static ServiceState read(FriendlyByteBuf buf) {
            String id = readString(buf);
            String title = readString(buf);
            String description = readString(buf);
            List<CostState> costs = new ArrayList<>();
            int count = buf.readVarInt();
            for (int i = 0; i < count; i++) {
                costs.add(CostState.read(buf));
            }
            return new ServiceState(id, title, description, costs, readString(buf), buf.readVarInt(),
                    buf.readVarInt(), buf.readLong(), readString(buf), buf.readBoolean(), readString(buf),
                    buf.readVarInt(), buf.readBoolean(), readString(buf), readString(buf), readString(buf));
        }
    }

    public record CostState(String item, int count) {
        public CostState {
            item = clean(item);
        }

        private void write(FriendlyByteBuf buf) {
            writeString(buf, item);
            buf.writeVarInt(count);
        }

        private static CostState read(FriendlyByteBuf buf) {
            return new CostState(readString(buf), buf.readVarInt());
        }
    }
}
