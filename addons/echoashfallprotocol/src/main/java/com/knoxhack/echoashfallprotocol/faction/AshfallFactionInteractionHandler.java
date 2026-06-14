package com.knoxhack.echoashfallprotocol.faction;

import com.echoplatform.echocore.api.EchoFactionAction;
import com.echoplatform.echocore.api.EchoFactionActionHandlerService;
import com.echoplatform.echocore.api.EchoFactionActionResult;
import com.echoplatform.echocore.api.EchoFactionContract;
import com.echoplatform.echocore.api.EchoFactionContractState;
import com.echoplatform.echocore.api.EchoFactionDefinition;
import com.echoplatform.echocore.api.EchoFactionPoiAffinity;
import com.echoplatform.echocore.api.EchoFactionProfile;
import com.echoplatform.echocore.api.EchoCoreServices;
import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.echo.QuestData;
import com.knoxhack.echoashfallprotocol.world.ExplorationSiteRegistry;
import java.util.List;
import java.util.Locale;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Ashfall-owned effects behind the shared Echo Core faction action surface.
 */
public final class AshfallFactionInteractionHandler implements EchoFactionActionHandlerService {
    public static final AshfallFactionInteractionHandler INSTANCE = new AshfallFactionInteractionHandler();
    public static final Identifier LOCAL_POI_HINT =
            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "local_poi_hint");

    private AshfallFactionInteractionHandler() {
    }

    @Override
    public boolean supports(Identifier factionId) {
        return AshfallFactionMap.isAshfall(factionId);
    }

    @Override
    public List<EchoFactionAction> actions(Player player, EchoFactionProfile profile, String roleId) {
        return List.of(new EchoFactionAction(
                LOCAL_POI_HINT,
                "Local POI Hint",
                "Ask for a route lead based on scanner and atlas state.",
                0,
                false));
    }

    @Override
    public String localContext(Player player, EchoFactionProfile profile, String roleId) {
        if (player == null || profile == null) {
            return "";
        }
        QuestData quest = QuestData.get(player);
        for (EchoFactionPoiAffinity affinity : profile.definition().poiAffinities()) {
            String profileId = ExplorationSiteRegistry.normalize(affinity.profileId());
            if (quest.isPOIDiscovered(profileId) || quest.hasVisitedLocation("poi", profileId)) {
                return "Known route: " + readable(profileId) + " (" + quest.getPOIStateSummary(profileId) + ").";
            }
        }
        if (!profile.definition().poiAffinities().isEmpty()) {
            EchoFactionPoiAffinity first = profile.definition().poiAffinities().get(0);
            return "No matching POI logged yet. Scanner leads around " + readable(first.profileId()) + " are worth checking.";
        }
        return "No local route context is available yet.";
    }

    @Override
    public EchoFactionContractState contractState(Player player, EchoFactionProfile profile,
            EchoFactionContract contract, String roleId) {
        return AshfallFactionContractProgression.state(player, profile, contract);
    }

    @Override
    public EchoFactionActionResult acceptContract(ServerPlayer player, EchoFactionProfile profile,
            EchoFactionContract contract, String roleId) {
        return AshfallFactionContractProgression.onAccepted(player, profile, contract);
    }

    @Override
    public EchoFactionActionResult completeContract(ServerPlayer player, EchoFactionProfile profile,
            EchoFactionContract contract, String roleId) {
        return AshfallFactionContractProgression.onCompleted(player, profile, contract);
    }

    @Override
    public EchoFactionActionResult handle(ServerPlayer player, Identifier factionId, Identifier actionId,
            String roleId, Identifier targetId) {
        Identifier canonicalFaction = AshfallFactionMap.canonicalOrDefault(factionId);
        EchoFactionProfile profile = EchoCoreServices.factionProfile(player, canonicalFaction)
                .orElse(null);
        if (profile == null) {
            return EchoFactionActionResult.failure("Unknown Faction", "This contact is not registered with Echo Core.");
        }

        EchoFactionDefinition definition = profile.definition();
        if (EchoCoreServices.ACCEPT_FACTION_CONTRACT_ACTION.equals(actionId)) {
            EchoFactionContract contract = contract(definition, targetId);
            if (contract == null) {
                return EchoFactionActionResult.failure("Contract Unavailable", "Requested field contract is not available.");
            }
            EchoFactionContractState state = contractState(player, profile, contract, roleId);
            if (!state.canAccept()) {
                return EchoFactionActionResult.failure("Contract Locked",
                        state.lockedReason().isBlank() ? "This field contract is not ready." : state.lockedReason());
            }
            EchoFactionActionResult result = acceptContract(player, profile, contract, roleId);
            if (result.success()) {
                EchoCoreServices.setFactionActiveContract(player, canonicalFaction, contract.id());
            }
            return result;
        }

        if (EchoCoreServices.COMPLETE_FACTION_CONTRACT_ACTION.equals(actionId)) {
            EchoFactionContract contract = contract(definition, targetId);
            if (contract == null) {
                return EchoFactionActionResult.failure("Contract Unavailable", "Requested field contract is not available.");
            }
            EchoFactionContractState state = contractState(player, profile, contract, roleId);
            if (!state.canComplete()) {
                return EchoFactionActionResult.failure("Contract Pending",
                        state.lockedReason().isBlank() ? "Field contract objective is still pending." : state.lockedReason());
            }
            EchoFactionActionResult result = completeContract(player, profile, contract, roleId);
            if (result.success()) {
                EchoCoreServices.addFactionReputation(player, canonicalFaction, contract.reputationReward());
                EchoCoreServices.markFactionContractCompleted(player, canonicalFaction, contract.id());
            }
            return result;
        }

        if (LOCAL_POI_HINT.equals(actionId)) {
            return EchoFactionActionResult.info("Route Hint", localContext(player, profile, roleId));
        }

        String path = actionId == null ? "" : actionId.getPath();
        if (path.endsWith("_talk")) {
            String voice = roleVoice(definition, roleId);
            com.echoplatform.echocore.api.EchoCoreServices.rememberFactionNpc(player, canonicalFaction, voice);
            return EchoFactionActionResult.info(definition.shortName() + " Dialogue", voice);
        }

        if (path.endsWith("_service")) {
            int required = definition.actions().stream()
                    .filter(action -> action.id().equals(actionId))
                    .mapToInt(EchoFactionAction::requiredReputation)
                    .findFirst()
                    .orElse(10);
            if (profile.reputation() < required) {
                return EchoFactionActionResult.failure("Service Locked",
                        definition.shortName() + " services require standing " + required + ".");
            }
            EchoFactionActionResult result = AshfallFactionServices.perform(player, profile, roleId);
            com.echoplatform.echocore.api.EchoCoreServices.rememberFactionNpc(player, canonicalFaction, result.message());
            return result;
        }

        if (path.endsWith("_contract")) {
            return EchoFactionActionResult.info("Contract Board",
                    "Review starter contracts in the right panel. Core tracks active and completed work.");
        }

        return EchoFactionActionResult.failure("Unavailable", "This Ashfall faction action is not available.");
    }

    private static EchoFactionContract contract(EchoFactionDefinition definition, Identifier contractId) {
        if (definition == null || contractId == null) {
            return null;
        }
        return definition.contracts().stream()
                .filter(contract -> contract.id().equals(contractId))
                .findFirst()
                .orElse(null);
    }

    private static String roleVoice(EchoFactionDefinition definition, String roleId) {
        String role = roleId == null ? "" : roleId.toLowerCase(Locale.ROOT);
        String name = definition.shortName();
        if (role.contains("medic") || role.contains("brewer") || role.contains("decon")) {
            return name + " medical channel: keep filters dry, wounds clean, and exits marked.";
        }
        if (role.contains("tech") || role.contains("foreman") || role.contains("keeper") || role.contains("ratchet")) {
            return name + " service channel: bring parts, keep power stable, and do not trust quiet machinery.";
        }
        if (role.contains("scout") || role.contains("runner") || role.contains("reader") || role.contains("lookout")) {
            return name + " route channel: scan first, move second, and leave a marker for the next operator.";
        }
        return name + " contact: standing logged. Ask for services, contracts, or local POI hints.";
    }

    private static String readable(String id) {
        String normalized = ExplorationSiteRegistry.normalize(id);
        return normalized.replace('_', ' ');
    }
}
