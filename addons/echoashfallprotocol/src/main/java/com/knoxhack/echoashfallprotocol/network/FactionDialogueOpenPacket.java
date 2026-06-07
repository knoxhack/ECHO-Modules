package com.knoxhack.echoashfallprotocol.network;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record FactionDialogueOpenPacket(
        int entityId,
        String factionId,
        String factionName,
        String shortName,
        String roleId,
        String roleName,
        String standing,
        int reputation,
        int contactCount,
        long lastInteractionTick,
        String lastRoleId,
        String npcMemory,
        String greeting,
        String localContext,
        String activeContractId,
        List<ActionEntry> actions,
        List<ContractEntry> contracts) implements CustomPacketPayload {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "faction_dialogue_open");

    public static final StreamCodec<FriendlyByteBuf, FactionDialogueOpenPacket> CODEC = StreamCodec.of(
            FactionDialogueOpenPacket::write,
            FactionDialogueOpenPacket::read
    );

    public static final Type<FactionDialogueOpenPacket> TYPE = new Type<>(ID);

    public FactionDialogueOpenPacket {
        factionId = clean(factionId);
        factionName = clean(factionName);
        shortName = clean(shortName);
        roleId = clean(roleId);
        roleName = clean(roleName);
        standing = clean(standing);
        npcMemory = clean(npcMemory);
        greeting = clean(greeting);
        localContext = clean(localContext);
        activeContractId = clean(activeContractId);
        actions = actions == null ? List.of() : List.copyOf(actions);
        contracts = contracts == null ? List.of() : List.copyOf(contracts);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(FriendlyByteBuf buf, FactionDialogueOpenPacket packet) {
        buf.writeVarInt(packet.entityId);
        buf.writeUtf(packet.factionId);
        buf.writeUtf(packet.factionName);
        buf.writeUtf(packet.shortName);
        buf.writeUtf(packet.roleId);
        buf.writeUtf(packet.roleName);
        buf.writeUtf(packet.standing);
        buf.writeVarInt(packet.reputation);
        buf.writeVarInt(packet.contactCount);
        buf.writeLong(packet.lastInteractionTick);
        buf.writeUtf(packet.lastRoleId);
        buf.writeUtf(packet.npcMemory);
        buf.writeUtf(packet.greeting);
        buf.writeUtf(packet.localContext);
        buf.writeUtf(packet.activeContractId);
        buf.writeVarInt(packet.actions.size());
        for (ActionEntry action : packet.actions) {
            action.write(buf);
        }
        buf.writeVarInt(packet.contracts.size());
        for (ContractEntry contract : packet.contracts) {
            contract.write(buf);
        }
    }

    private static FactionDialogueOpenPacket read(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        String factionId = buf.readUtf();
        String factionName = buf.readUtf();
        String shortName = buf.readUtf();
        String roleId = buf.readUtf();
        String roleName = buf.readUtf();
        String standing = buf.readUtf();
        int reputation = buf.readVarInt();
        int contactCount = buf.readVarInt();
        long lastInteractionTick = buf.readLong();
        String lastRoleId = buf.readUtf();
        String npcMemory = buf.readUtf();
        String greeting = buf.readUtf();
        String localContext = buf.readUtf();
        String activeContractId = buf.readUtf();
        List<ActionEntry> actions = new ArrayList<>();
        int actionCount = buf.readVarInt();
        for (int i = 0; i < actionCount; i++) {
            actions.add(ActionEntry.read(buf));
        }
        List<ContractEntry> contracts = new ArrayList<>();
        int contractCount = buf.readVarInt();
        for (int i = 0; i < contractCount; i++) {
            contracts.add(ContractEntry.read(buf));
        }
        return new FactionDialogueOpenPacket(entityId, factionId, factionName, shortName, roleId, roleName,
                standing, reputation, contactCount, lastInteractionTick, lastRoleId, npcMemory, greeting,
                localContext, activeContractId, actions, contracts);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    public record ActionEntry(String id, String label, String description, int requiredReputation,
            boolean service, boolean enabled, String lockedReason) {
        public ActionEntry {
            id = clean(id);
            label = clean(label);
            description = clean(description);
            lockedReason = clean(lockedReason);
        }

        private void write(FriendlyByteBuf buf) {
            buf.writeUtf(id);
            buf.writeUtf(label);
            buf.writeUtf(description);
            buf.writeVarInt(requiredReputation);
            buf.writeBoolean(service);
            buf.writeBoolean(enabled);
            buf.writeUtf(lockedReason);
        }

        private static ActionEntry read(FriendlyByteBuf buf) {
            return new ActionEntry(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readVarInt(),
                    buf.readBoolean(), buf.readBoolean(), buf.readUtf());
        }
    }

    public record ContractEntry(String id, String title, String summary, String objective, String reward,
            String route, String progressLine, String lockedReason, boolean active, boolean completed,
            boolean canAccept, boolean canComplete) {
        public ContractEntry {
            id = clean(id);
            title = clean(title);
            summary = clean(summary);
            objective = clean(objective);
            reward = clean(reward);
            route = clean(route);
            progressLine = clean(progressLine);
            lockedReason = clean(lockedReason);
        }

        private void write(FriendlyByteBuf buf) {
            buf.writeUtf(id);
            buf.writeUtf(title);
            buf.writeUtf(summary);
            buf.writeUtf(objective);
            buf.writeUtf(reward);
            buf.writeUtf(route);
            buf.writeUtf(progressLine);
            buf.writeUtf(lockedReason);
            buf.writeBoolean(active);
            buf.writeBoolean(completed);
            buf.writeBoolean(canAccept);
            buf.writeBoolean(canComplete);
        }

        private static ContractEntry read(FriendlyByteBuf buf) {
            return new ContractEntry(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf(),
                    buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readBoolean(), buf.readBoolean(),
                    buf.readBoolean(), buf.readBoolean());
        }
    }
}
