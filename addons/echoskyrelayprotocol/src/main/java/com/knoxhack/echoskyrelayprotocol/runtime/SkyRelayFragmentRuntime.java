package com.knoxhack.echoskyrelayprotocol.runtime;

import com.knoxhack.echoskyrelayprotocol.contract.SkyRelayRuntimeContracts;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class SkyRelayFragmentRuntime {
    public record AnchorRule(
            String fragmentId,
            int tier,
            String anchorItem,
            int stablePowerRequired,
            String scanRequirement,
            String stormRisk,
            String dockingBlock,
            String unlockChapter
    ) {
        public Map<String, Object> asMap() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("fragmentId", fragmentId);
            result.put("tier", tier);
            result.put("anchorItem", anchorItem);
            result.put("stablePowerRequired", stablePowerRequired);
            result.put("scanRequirement", scanRequirement);
            result.put("stormRisk", stormRisk);
            result.put("dockingBlock", dockingBlock);
            result.put("unlockChapter", unlockChapter);
            return Map.copyOf(result);
        }
    }

    private static final List<AnchorRule> ANCHOR_RULES = List.of(
            new AnchorRule("starter_relay", 0, "operator_badge", 0, "spawn_signature", "low", "damaged_relay_core", "awakening"),
            new AnchorRule("hydroponics_deck", 1, "relay_anchor_key", 12, "lens_scan:damaged_relay_core", "medium", "relay_anchor_node", "first_anchor"),
            new AnchorRule("aero_salvage_yard", 1, "sky_fragment_chart", 18, "lens_scan:aero_salvage_crate", "medium", "fragment_docking_clamp", "salvage_expansion"),
            new AnchorRule("solar_wing", 1, "charged_relay_coil", 24, "holomap:solar_signature", "high", "fragment_docking_clamp", "solar_recovery"),
            new AnchorRule("weather_mast", 2, "signal_calibration_chip", 36, "lens_scan:storm_shield_pylon", "high", "relay_signal_array", "weather_control"),
            new AnchorRule("machine_bay", 2, "stabilized_platform_core", 48, "terminal:machine_bay_authorization", "medium", "relay_anchor_node", "machine_restoration"),
            new AnchorRule("logistics_spur", 2, "relay_firmware_shard", 42, "logistics:first_route_manifest", "medium", "fragment_docking_clamp", "network_routing"),
            new AnchorRule("orbital_debris_dock", 3, "fragment_access_cipher", 64, "holomap:orbital_debris_layer", "extreme", "skybridge_projector", "orbital_reach"),
            new AnchorRule("signal_crown", 4, "echo_crystal_charge", 96, "terminal:final_restoration_sequence", "extreme", "signal_crown_interface", "signal_crown")
    );

    private SkyRelayFragmentRuntime() {
    }

    public static List<AnchorRule> anchorRules() {
        return ANCHOR_RULES;
    }

    public static Optional<AnchorRule> ruleFor(String fragmentId) {
        return ANCHOR_RULES.stream().filter(rule -> rule.fragmentId().equals(fragmentId)).findFirst();
    }

    public static List<String> anchorableFragments(Collection<String> inventoryItemIds, Collection<String> completedScans, int stablePower) {
        return ANCHOR_RULES.stream()
                .filter(rule -> inventoryItemIds.contains(rule.anchorItem()))
                .filter(rule -> completedScans.contains(rule.scanRequirement()))
                .filter(rule -> stablePower >= rule.stablePowerRequired())
                .map(AnchorRule::fragmentId)
                .toList();
    }

    public static Map<String, Object> adapterManifest() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("schema", "echo.skyrelay.fragment_runtime.v1");
        result.put("moduleId", SkyRelayRuntimeContracts.MODULE_ID);
        result.put("fragmentIds", SkyRelayRuntimeContracts.FRAGMENT_IDS);
        result.put("anchorRules", ANCHOR_RULES.stream().map(AnchorRule::asMap).toList());
        result.put("gates", List.of("anchorItem", "stablePowerRequired", "scanRequirement", "stormRisk", "dockingBlock", "unlockChapter"));
        return Map.copyOf(result);
    }
}
