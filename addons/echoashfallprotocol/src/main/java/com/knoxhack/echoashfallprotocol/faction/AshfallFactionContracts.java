package com.knoxhack.echoashfallprotocol.faction;

import com.echoplatform.echocore.api.EchoFactionContract;
import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

/**
 * Contract catalogue for the three Ashfall Echo Core factions.
 */
public final class AshfallFactionContracts {
    private static final Map<String, Loadout> LOADOUTS = new LinkedHashMap<>();

    static {
        loadout("radwarden_compact", "Radwarden Compact", "Containment", ServiceKind.RADIATION,
                List.of("survivor_cache", "relay_station", "radiation_zone", "reactor_ruin", "military_vault",
                        "cryogenic_ruins", "cryo_lab", "frozen_outpost", "thermal_station"),
                item("echoashfallprotocol:rad_away", 2, "Deliver RadAway and filter stock for containment crews"),
                objective(ObjectiveType.REPAIR, List.of("relay", "beacon", "power_node", "thermal_station"), 2,
                        "Restore two warning, power, or thermal nodes"),
                List.of("echoashfallprotocol:filter_cartridge_advanced", "echoashfallprotocol:hand_warmer"));
        loadout("crashbreak_salvage", "Crashbreak Salvage", "Salvage", ServiceKind.SALVAGE,
                List.of("crash_zone_wasteland", "drop_pod", "train_yard", "ruined_cityscape", "data_center_ruin",
                        "subway_station", "industrial_ruins", "industrial_factory", "ruined_plains", "scavenger_camp"),
                item("echoashfallprotocol:scrap_circuit", 3, "Deliver intact wreck circuits and route records"),
                objective(ObjectiveType.RAID_DEFENSE, List.of("crashbreak_salvage"), 1,
                        "Repel one raid pressure event against a Crashbreak salvage site"),
                List.of("echoashfallprotocol:power_cell", "echoashfallprotocol:scrap_metal"));
        loadout("sporebound_sanctum", "Sporebound Sanctum", "Sample", ServiceKind.MEDICAL,
                List.of("toxic_swamp", "bio_lab", "sporebound_sanctum", "nexus_scar", "scar_anchor", "nexus_anomaly"),
                item("echoashfallprotocol:mutated_tissue", 2, "Deliver controlled biological samples"),
                objective(ObjectiveType.REPAIR, List.of("anchor", "nexus", "scar", "bio_lab"), 1,
                        "Stabilize one bio-anomaly or scar route anchor"),
                List.of("echoashfallprotocol:rad_away", "echoashfallprotocol:mutagen_vial"));
    }

    private AshfallFactionContracts() {
    }

    public static List<EchoFactionContract> echoContracts(String path, String fieldTitle, String fieldSummary,
            String fieldObjective, String fieldReward, String route) {
        Loadout loadout = loadout(path);
        return specs(path, fieldTitle, fieldSummary, fieldObjective, fieldReward, route).stream()
                .map(spec -> new EchoFactionContract(
                        spec.contractId(),
                        spec.title(),
                        spec.summary(),
                        spec.requiredReputation(),
                        spec.reputationReward(),
                        spec.objectives().stream().map(Objective::displayText).reduce((a, b) -> a + " | " + b).orElse("Field work"),
                        spec.rewardLine(),
                        route.isBlank() ? loadout.routeName() : route))
                .toList();
    }

    public static Optional<Spec> spec(Identifier contractId) {
        if (contractId == null || !EchoAshfallProtocol.MODID.equals(contractId.getNamespace())) {
            return Optional.empty();
        }
        String path = contractId.getPath();
        for (String factionPath : LOADOUTS.keySet()) {
            if (path.equals(factionPath + "_field_contract")
                    || path.equals(factionPath + "_trusted_contract")
                    || path.equals(factionPath + "_aligned_contract")) {
                return specs(factionPath, "", "", "", "", "").stream()
                        .filter(spec -> spec.contractId().equals(contractId))
                        .findFirst();
            }
        }
        return Optional.empty();
    }

    public static List<String> poiTargets(String path) {
        return loadout(path).poiTargets();
    }

    public static ServiceKind serviceKind(Identifier factionId) {
        Identifier canonical = AshfallFactionMap.canonicalOrDefault(factionId);
        return loadout(canonical.getPath()).serviceKind();
    }

    private static List<Spec> specs(String path, String fieldTitle, String fieldSummary, String fieldObjective,
            String fieldReward, String route) {
        Loadout loadout = loadout(path);
        List<Spec> specs = new ArrayList<>();
        specs.add(new Spec(
                id(path),
                id(path + "_field_contract"),
                fieldTitle.isBlank() ? loadout.routeName() + " Field Proof" : fieldTitle,
                fieldSummary.isBlank() ? "Log one field route for " + loadout.displayName() + "." : fieldSummary,
                0,
                15,
                List.of(objective(ObjectiveType.POI_DISCOVERY, loadout.poiTargets(), 1,
                        fieldObjective.isBlank() ? "Log one matching route POI" : fieldObjective)),
                loadout.rewardItems(),
                8,
                fieldReward.isBlank() ? loadout.routeName() + " field supplies." : fieldReward));
        specs.add(new Spec(
                id(path),
                id(path + "_trusted_contract"),
                loadout.routeName() + " Trusted Supply",
                "Prove dependable logistics before " + loadout.displayName() + " opens higher-risk work.",
                35,
                20,
                List.of(loadout.deliveryObjective()),
                loadout.rewardItems(),
                12,
                loadout.routeName() + " trusted support cache."));
        specs.add(new Spec(
                id(path),
                id(path + "_aligned_contract"),
                loadout.routeName() + " Aligned Operation",
                "Complete a route-changing operation for " + loadout.displayName() + ".",
                75,
                25,
                List.of(loadout.alignedObjective()),
                loadout.rewardItems(),
                18,
                loadout.routeName() + " aligned support cache."));
        return specs;
    }

    private static Loadout loadout(String path) {
        String canonicalPath = AshfallFactionMap.resolveFactionId(path).getPath();
        return LOADOUTS.getOrDefault(canonicalPath, LOADOUTS.get("radwarden_compact"));
    }

    private static void loadout(String path, String displayName, String routeName, ServiceKind serviceKind,
            List<String> poiTargets, Objective deliveryObjective, Objective alignedObjective, List<String> rewardItems) {
        LOADOUTS.put(path, new Loadout(displayName, routeName, serviceKind, List.copyOf(poiTargets),
                deliveryObjective, alignedObjective, List.copyOf(rewardItems)));
    }

    private static Objective item(String itemId, int count, String text) {
        return objective(ObjectiveType.ITEM_DELIVERY, List.of(itemId), count, text);
    }

    private static Objective objective(ObjectiveType type, List<String> targetIds, int count, String text) {
        return new Objective(type, targetIds.stream().map(AshfallFactionContracts::normalize).toList(),
                Math.max(1, count), text);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, path);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }

    public enum ObjectiveType {
        POI_DISCOVERY,
        ITEM_DELIVERY,
        KILL,
        REPAIR,
        RAID_DEFENSE
    }

    public enum ServiceKind {
        SUPPLY,
        HAZARD,
        REPAIR,
        MEDICAL,
        COLD,
        RADIATION,
        SALVAGE,
        ARCHIVE,
        NEXUS
    }

    public record Objective(ObjectiveType type, List<String> targetIds, int requiredCount, String displayText) {
        public Objective {
            targetIds = targetIds == null ? List.of() : List.copyOf(targetIds);
            requiredCount = Math.max(1, requiredCount);
            displayText = displayText == null || displayText.isBlank()
                    ? Arrays.toString(targetIds.toArray())
                    : displayText.trim();
        }
    }

    public record Spec(Identifier factionId, Identifier contractId, String title, String summary,
            int requiredReputation, int reputationReward, List<Objective> objectives, List<String> rewardItems,
            int researchReward, String rewardLine) {
        public Spec {
            objectives = objectives == null ? List.of() : List.copyOf(objectives);
            rewardItems = rewardItems == null ? List.of() : List.copyOf(rewardItems);
            rewardLine = rewardLine == null ? "" : rewardLine.trim();
        }
    }

    private record Loadout(String displayName, String routeName, ServiceKind serviceKind, List<String> poiTargets,
            Objective deliveryObjective, Objective alignedObjective, List<String> rewardItems) {
    }
}
