package com.knoxhack.echo.socialcore;

import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.platformcore.EchoFeatureId;
import dev.echo.nativeplatform.contracts.EchoNativeSurfaceModuleEntrypoint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EchoSocialCoreNativeModule implements EchoNativeSurfaceModuleEntrypoint {
    public static final String MODULE_ID = "echosocialcore";
    public static final String FACTION_CONTRACT_ID = "echosocialcore:data/faction";
    public static final String DIALOGUE_TREE_CONTRACT_ID = "echosocialcore:data/dialogue_tree";
    public static final String NPC_PROFILE_CONTRACT_ID = "echosocialcore:entity/npc_profile";
    public static final String VILLAGER_REPLACEMENT_CONTRACT_ID = "echosocialcore:entity/villager_replacement_plan";
    public static final List<String> CONTRACT_IDS = List.of(
            FACTION_CONTRACT_ID,
            DIALOGUE_TREE_CONTRACT_ID,
            NPC_PROFILE_CONTRACT_ID,
            VILLAGER_REPLACEMENT_CONTRACT_ID
    );

    private static final EchoModuleId SOCIAL_MODULE = EchoModuleId.of(MODULE_ID);

    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        Map<String, Object> probe = referenceProbe();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "socialcore_native_contract_active");
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("serviceCodeExecuted", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", context.getOrDefault("packId", "unknown"));
        result.put("registeredFeatureContracts", CONTRACT_IDS);
        result.put("logicalRegistrationCount", CONTRACT_IDS.size());
        result.put("adapterDomains", List.of("data", "entities"));
        result.put("runtimeTargets", List.of("echo_native", "echo_runtime_standalone"));
        result.put("factionDataRoundTrip", probe.get("factionDataRoundTrip"));
        result.put("dialogueDataRoundTrip", probe.get("dialogueDataRoundTrip"));
        result.put("npcEntityRoundTrip", probe.get("npcEntityRoundTrip"));
        result.put("villagerReplacementRoundTrip", probe.get("villagerReplacementRoundTrip"));
        result.put("referenceProbe", probe);
        result.put("registryInjected", false);
        result.put("registryMutated", false);
        result.put("transformsPerformed", false);
        result.put("summary", "SocialCore native contract exercised faction, dialogue, NPC profile, and villager replacement records.");
        return Map.copyOf(result);
    }

    private static Map<String, Object> referenceProbe() {
        EchoFactionId factionId = EchoFactionId.of(" Ashfall_Settlers ");
        EchoDialogueTreeId dialogueTreeId = EchoDialogueTreeId.of(" Settler_Greeting ");
        EchoDialogueNodeId rootNodeId = EchoDialogueNodeId.of(" Root ");
        EchoNpcAiProfileId aiProfileId = EchoNpcAiProfileId.of(" Quartermaster_Ai ");
        EchoNpcProfileId npcProfileId = EchoNpcProfileId.of(" Quartermaster_Iris ");
        EchoDialogueNode rootNode = new EchoDialogueNode(
                rootNodeId,
                null,
                npcProfileId,
                factionId,
                " Stay close to the relay. ",
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                Map.of("tone", "steady")
        );
        EchoDialogueTree dialogueTree = new EchoDialogueTree(
                dialogueTreeId,
                " Settler Greeting ",
                " First contact tree ",
                SOCIAL_MODULE,
                rootNodeId,
                List.of(rootNode),
                Set.of(factionId),
                Set.of(EchoFeatureId.of("social.dialogue_trees")),
                List.of(),
                Map.of("runtime", "native")
        );
        EchoFactionProfile faction = new EchoFactionProfile(
                factionId,
                " Ashfall Settlers ",
                " Relay survivors ",
                SOCIAL_MODULE,
                Set.of(EchoFeatureId.of("social.factions")),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(dialogueTreeId),
                List.of(),
                Map.of("alignment", "cooperative")
        );
        EchoNpcAiProfile aiProfile = new EchoNpcAiProfile(
                aiProfileId,
                null,
                null,
                Set.of(EchoFeatureId.of("social.npc_profiles")),
                null,
                " Keeps quartermaster close to settlement routes ",
                " Native smoke default role/hostility fallback ",
                Map.of("patrol", "settlement")
        );
        EchoNpcProfile npcProfile = new EchoNpcProfile(
                npcProfileId,
                " Quartermaster Iris ",
                null,
                SOCIAL_MODULE,
                factionId,
                aiProfile.id(),
                dialogueTreeId,
                null,
                null,
                null,
                List.of(),
                List.of(),
                Map.of("shop", "supplies")
        );
        EchoVillagerReplacementPlan replacementPlan = new EchoVillagerReplacementPlan(
                " Settler_Villager_Replacement ",
                EchoVillagerReplacementMode.PACK_PROFILE,
                true,
                "echosocialcore.replace_settlers",
                EchoFeatureId.of("social.villager_replacement_plan"),
                null,
                List.of(npcProfileId),
                Set.of("farmer", "cleric"),
                List.of(),
                " Replace villagers with settlers ",
                " Pack profile replacement smoke ",
                Map.of("mode", "pack")
        );
        EchoSocialRegistry registry = new EchoSocialRegistry(
                Map.of(faction.id(), faction),
                Map.of(dialogueTree.id(), dialogueTree),
                Map.of(aiProfile.id(), aiProfile),
                Map.of(npcProfile.id(), npcProfile),
                List.of(replacementPlan),
                List.of()
        );
        Map<String, Object> probe = new LinkedHashMap<>();
        probe.put("factionDataRoundTrip", faction.id().value().equals("ashfall_settlers")
                && faction.displayName().equals("Ashfall Settlers")
                && faction.dialogueTrees().contains(dialogueTreeId)
                && !faction.blocking());
        probe.put("dialogueDataRoundTrip", dialogueTree.id().value().equals("settler_greeting")
                && dialogueTree.title().equals("Settler Greeting")
                && dialogueTree.rootNodeId().equals(rootNodeId)
                && rootNode.kind() == EchoDialogueNodeKind.UNKNOWN
                && rootNode.terminal()
                && !dialogueTree.blocking());
        probe.put("npcEntityRoundTrip", npcProfile.id().value().equals("quartermaster_iris")
                && npcProfile.role() == EchoNpcRole.UNKNOWN
                && aiProfile.defaultHostility() == EchoHostilityState.NEUTRAL
                && !npcProfile.blocking());
        probe.put("villagerReplacementRoundTrip", replacementPlan.planId().equals("Settler_Villager_Replacement")
                && replacementPlan.replacementAllowedByContract()
                && replacementPlan.playerSummary().equals("Replace villagers with settlers")
                && !registry.blocking());
        probe.put("factionId", faction.id().value());
        probe.put("dialogueTreeId", dialogueTree.id().value());
        probe.put("rootNodeKind", rootNode.kind().name().toLowerCase());
        probe.put("npcProfileId", npcProfile.id().value());
        probe.put("npcRole", npcProfile.role().name().toLowerCase());
        probe.put("aiHostility", aiProfile.defaultHostility().name().toLowerCase());
        probe.put("replacementMode", replacementPlan.mode().serializedName());
        probe.put("registryBlocking", registry.blocking());
        return Map.copyOf(probe);
    }

    public static void main(String[] args) {
        Map<String, Object> activation = new EchoSocialCoreNativeModule()
                .describeNativeSurfaces(Map.of("packId", "ashfall"));
        require(Boolean.TRUE.equals(activation.get("activated")),
                "SocialCore native adapter should activate");
        require(Boolean.TRUE.equals(activation.get("factionDataRoundTrip")),
                "SocialCore native adapter should preserve faction behavior");
        require(Boolean.TRUE.equals(activation.get("dialogueDataRoundTrip")),
                "SocialCore native adapter should preserve dialogue behavior");
        require(Boolean.TRUE.equals(activation.get("npcEntityRoundTrip")),
                "SocialCore native adapter should preserve NPC behavior");
        require(Boolean.TRUE.equals(activation.get("villagerReplacementRoundTrip")),
                "SocialCore native adapter should preserve villager replacement behavior");
        System.out.println("socialcore native adapter smoke PASS contracts=" + CONTRACT_IDS.size());
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
