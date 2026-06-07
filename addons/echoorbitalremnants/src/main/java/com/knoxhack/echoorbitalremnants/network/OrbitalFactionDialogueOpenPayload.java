package com.knoxhack.echoorbitalremnants.network;

import com.knoxhack.echonetcore.api.EchoPayloadCodecs;
import com.knoxhack.echoorbitalremnants.EchoOrbitalRemnants;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record OrbitalFactionDialogueOpenPayload(
        int entityId,
        String factionId,
        String factionName,
        String shortName,
        String roleId,
        String roleName,
        int tier,
        String standing,
        String greeting,
        String localContext,
        String activeContractId,
        List<ActionEntry> offers,
        List<ContractEntry> contracts) implements CustomPacketPayload {
    private static final int MAX_LIST = 12;

    public static final Type<OrbitalFactionDialogueOpenPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(EchoOrbitalRemnants.MODID, "orbital_faction_dialogue_open"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OrbitalFactionDialogueOpenPayload> STREAM_CODEC =
            StreamCodec.of(OrbitalFactionDialogueOpenPayload::write, OrbitalFactionDialogueOpenPayload::read);

    public OrbitalFactionDialogueOpenPayload {
        factionId = clean(factionId);
        factionName = clean(factionName);
        shortName = clean(shortName);
        roleId = clean(roleId);
        roleName = clean(roleName);
        standing = clean(standing);
        greeting = clean(greeting);
        localContext = clean(localContext);
        activeContractId = clean(activeContractId);
        offers = offers == null ? List.of() : List.copyOf(offers);
        contracts = contracts == null ? List.of() : List.copyOf(contracts);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buffer, OrbitalFactionDialogueOpenPayload payload) {
        buffer.writeVarInt(payload.entityId);
        EchoPayloadCodecs.writeUtf(buffer, payload.factionId, EchoPayloadCodecs.ID);
        EchoPayloadCodecs.writeUtf(buffer, payload.factionName, EchoPayloadCodecs.SMALL_TEXT);
        EchoPayloadCodecs.writeUtf(buffer, payload.shortName, EchoPayloadCodecs.SMALL_TEXT);
        EchoPayloadCodecs.writeUtf(buffer, payload.roleId, EchoPayloadCodecs.ID);
        EchoPayloadCodecs.writeUtf(buffer, payload.roleName, EchoPayloadCodecs.SMALL_TEXT);
        buffer.writeVarInt(payload.tier);
        EchoPayloadCodecs.writeUtf(buffer, payload.standing, EchoPayloadCodecs.SMALL_TEXT);
        EchoPayloadCodecs.writeUtf(buffer, payload.greeting, EchoPayloadCodecs.SMALL_TEXT);
        EchoPayloadCodecs.writeUtf(buffer, payload.localContext, EchoPayloadCodecs.SMALL_TEXT);
        EchoPayloadCodecs.writeUtf(buffer, payload.activeContractId, EchoPayloadCodecs.ID);
        buffer.writeVarInt(Math.min(MAX_LIST, payload.offers.size()));
        for (ActionEntry offer : payload.offers.stream().limit(MAX_LIST).toList()) {
            offer.write(buffer);
        }
        buffer.writeVarInt(Math.min(MAX_LIST, payload.contracts.size()));
        for (ContractEntry contract : payload.contracts.stream().limit(MAX_LIST).toList()) {
            contract.write(buffer);
        }
    }

    private static OrbitalFactionDialogueOpenPayload read(RegistryFriendlyByteBuf buffer) {
        int entityId = buffer.readVarInt();
        String factionId = EchoPayloadCodecs.readUtf(buffer, EchoPayloadCodecs.ID);
        String factionName = EchoPayloadCodecs.readUtf(buffer, EchoPayloadCodecs.SMALL_TEXT);
        String shortName = EchoPayloadCodecs.readUtf(buffer, EchoPayloadCodecs.SMALL_TEXT);
        String roleId = EchoPayloadCodecs.readUtf(buffer, EchoPayloadCodecs.ID);
        String roleName = EchoPayloadCodecs.readUtf(buffer, EchoPayloadCodecs.SMALL_TEXT);
        int tier = buffer.readVarInt();
        String standing = EchoPayloadCodecs.readUtf(buffer, EchoPayloadCodecs.SMALL_TEXT);
        String greeting = EchoPayloadCodecs.readUtf(buffer, EchoPayloadCodecs.SMALL_TEXT);
        String localContext = EchoPayloadCodecs.readUtf(buffer, EchoPayloadCodecs.SMALL_TEXT);
        String activeContractId = EchoPayloadCodecs.readUtf(buffer, EchoPayloadCodecs.ID);
        List<ActionEntry> offers = new ArrayList<>();
        int offerCount = Math.min(MAX_LIST, buffer.readVarInt());
        for (int i = 0; i < offerCount; i++) {
            offers.add(ActionEntry.read(buffer));
        }
        List<ContractEntry> contracts = new ArrayList<>();
        int contractCount = Math.min(MAX_LIST, buffer.readVarInt());
        for (int i = 0; i < contractCount; i++) {
            contracts.add(ContractEntry.read(buffer));
        }
        return new OrbitalFactionDialogueOpenPayload(entityId, factionId, factionName, shortName, roleId, roleName,
                tier, standing, greeting, localContext, activeContractId, offers, contracts);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    public record ActionEntry(String id, String label, String description, boolean service, boolean enabled,
            String lockedReason) {
        public ActionEntry {
            id = clean(id);
            label = clean(label);
            description = clean(description);
            lockedReason = clean(lockedReason);
        }

        private void write(RegistryFriendlyByteBuf buffer) {
            EchoPayloadCodecs.writeUtf(buffer, id, EchoPayloadCodecs.ID);
            EchoPayloadCodecs.writeUtf(buffer, label, EchoPayloadCodecs.SMALL_TEXT);
            EchoPayloadCodecs.writeUtf(buffer, description, EchoPayloadCodecs.SMALL_TEXT);
            buffer.writeBoolean(service);
            buffer.writeBoolean(enabled);
            EchoPayloadCodecs.writeUtf(buffer, lockedReason, EchoPayloadCodecs.SMALL_TEXT);
        }

        private static ActionEntry read(RegistryFriendlyByteBuf buffer) {
            return new ActionEntry(
                    EchoPayloadCodecs.readUtf(buffer, EchoPayloadCodecs.ID),
                    EchoPayloadCodecs.readUtf(buffer, EchoPayloadCodecs.SMALL_TEXT),
                    EchoPayloadCodecs.readUtf(buffer, EchoPayloadCodecs.SMALL_TEXT),
                    buffer.readBoolean(),
                    buffer.readBoolean(),
                    EchoPayloadCodecs.readUtf(buffer, EchoPayloadCodecs.SMALL_TEXT));
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

        private void write(RegistryFriendlyByteBuf buffer) {
            EchoPayloadCodecs.writeUtf(buffer, id, EchoPayloadCodecs.ID);
            EchoPayloadCodecs.writeUtf(buffer, title, EchoPayloadCodecs.SMALL_TEXT);
            EchoPayloadCodecs.writeUtf(buffer, summary, EchoPayloadCodecs.SMALL_TEXT);
            EchoPayloadCodecs.writeUtf(buffer, objective, EchoPayloadCodecs.SMALL_TEXT);
            EchoPayloadCodecs.writeUtf(buffer, reward, EchoPayloadCodecs.SMALL_TEXT);
            EchoPayloadCodecs.writeUtf(buffer, route, EchoPayloadCodecs.SMALL_TEXT);
            EchoPayloadCodecs.writeUtf(buffer, progressLine, EchoPayloadCodecs.SMALL_TEXT);
            EchoPayloadCodecs.writeUtf(buffer, lockedReason, EchoPayloadCodecs.SMALL_TEXT);
            buffer.writeBoolean(active);
            buffer.writeBoolean(completed);
            buffer.writeBoolean(canAccept);
            buffer.writeBoolean(canComplete);
        }

        private static ContractEntry read(RegistryFriendlyByteBuf buffer) {
            return new ContractEntry(
                    EchoPayloadCodecs.readUtf(buffer, EchoPayloadCodecs.ID),
                    EchoPayloadCodecs.readUtf(buffer, EchoPayloadCodecs.SMALL_TEXT),
                    EchoPayloadCodecs.readUtf(buffer, EchoPayloadCodecs.SMALL_TEXT),
                    EchoPayloadCodecs.readUtf(buffer, EchoPayloadCodecs.SMALL_TEXT),
                    EchoPayloadCodecs.readUtf(buffer, EchoPayloadCodecs.SMALL_TEXT),
                    EchoPayloadCodecs.readUtf(buffer, EchoPayloadCodecs.SMALL_TEXT),
                    EchoPayloadCodecs.readUtf(buffer, EchoPayloadCodecs.SMALL_TEXT),
                    EchoPayloadCodecs.readUtf(buffer, EchoPayloadCodecs.SMALL_TEXT),
                    buffer.readBoolean(),
                    buffer.readBoolean(),
                    buffer.readBoolean(),
                    buffer.readBoolean());
        }
    }
}
