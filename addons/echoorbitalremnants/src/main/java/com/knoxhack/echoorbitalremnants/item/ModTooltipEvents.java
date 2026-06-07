package com.knoxhack.echoorbitalremnants.item;

import com.knoxhack.echo.adaptercore.EchoBackendClientBridge;
import com.knoxhack.echoorbitalremnants.EchoOrbitalRemnants;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public class ModTooltipEvents {
    public void onItemTooltip(Object event) {
        ItemStack stack = EchoBackendClientBridge.tooltipItemStack(event);
        Identifier id = stack.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) {
            return;
        }
        if (!EchoOrbitalRemnants.MODID.equals(id.getNamespace())) {
            return;
        }

        String line = switch (id.getPath()) {
            case "echo_terminal" -> "Tracks objectives, route locks, suit diagnostics, factions, and ECHO memory.";
            case "emergency_rocket" -> "Stage on a complete 5x5 Launch Platform, board the vehicle, then launch; use in space to return to Earth.";
            case "orbital_shuttle" -> "Reusable route vessel to the Lunar Scar Zone; sneak-use in space to burn the saved return vector.";
            case "mars_transfer_window" -> "Reusable route key to Mars Ash Basin after lunar Helium-3 telemetry is resolved; sneak-use in space to return.";
            case "europa_transfer_window" -> "Reusable route key to Europa Cryo Ocean after Martian silica telemetry is resolved; sneak-use in space to return.";
            case "saturn_transfer_window" -> "Reusable route key to Saturn Ring Graveyard after Europa Thermal Arrays resolve ring telemetry; sneak-use in space to return.";
            case "titan_transfer_window" -> "Reusable route key to Titan Methane Shelf after Saturn Ring Relays stabilize descent; sneak-use in space to return.";
            case "nexus_drive_vessel" -> "Reusable endgame vessel for the Nexus Anomaly Belt after Titan methane telemetry; sneak-use in space to return.";
            case "pressurized_helmet", "pressurized_chestplate", "pressurized_leggings", "magnetic_boots" -> "Part of the pressure suit required for vacuum survival.";
            case "oxygen_tank" -> "Required for launch readiness and suit oxygen support.";
            case "oxygen_booster" -> "Carry to slow oxygen drain; use to flush reserve oxygen into the suit.";
            case "emergency_oxygen_cell" -> "Use manually or let suit auto-consume at critical oxygen.";
            case "suit_sealant_patch" -> "Use manually or let suit auto-consume when pressure gets dangerous.";
            case "radiation_visor" -> "Carry with a full suit to reduce radiation gain; use to recalibrate dose shielding.";
            case "thermal_space_liner" -> "Carry to resist Europa cold pressure loss; use to stabilize suit temperature.";
            case "jet_burst_module" -> "Use in orbital exposure for a short movement burst at an oxygen cost.";
            case "scanner_visor" -> "Use for route, dimension, and suit diagnostics.";
            case "rocket_assembly_frame" -> "Interact when launch infrastructure and rocket parts are ready to assemble an Emergency Rocket.";
            case "fuel_refinery" -> "Converts kelp into Fuel Tanks.";
            case "oxygen_compressor" -> "Converts glass bottles into Emergency Oxygen Cells.";
            case "solar_reclaimer" -> "Breaks broken solar panels into Vacuum Circuits.";
            case "vacuum_smelter" -> "Refines Satellite Plating into Orbital Alloy.";
            case "heat_shield_fabricator" -> "Turns copper ingots into Heat Shield Plates.";
            case "orbital_fabricator" -> "Converts Orbital Alloy into Life Support Modules.";
            case "suit_charging_station" -> "Converts Oxygen Canisters into Oxygen Tanks.";
            case "signal_analyzer" -> "Processes route survey data and Nexus stabilization shards.";
            case "survey_marker", "signal_relay", "thermal_vent", "nexus_anchor" -> "Generated route objective block; scan it with the ECHO-7 Terminal.";
            case "orbit_survey_data" -> "Scan in Low Earth Orbit to map station fragments and debris belts.";
            case "lunar_core_sample" -> "Scan on the Moon to complete crater surveys and improve Mars routing.";
            case "martian_pressure_valve" -> "Carry on Mars to reduce dust-pressure hazards, or scan to repair habitats.";
            case "europa_thermal_probe" -> "Scan near Europa vents to chart safe thermal pockets.";
            case "nexus_stabilizer_shard" -> "Post-ECHO-0 shard used to stabilize Nexus anchors.";
            case "stabilized_echo_core" -> "Final post-survey core from stabilized Nexus anchors.";
            case "orbital_black_box" -> "Major-encounter flight recorder proof; marks orbital threats without duplicating rewards.";
            case "station_relay_fuse" -> "Consumed to repair Station Relay Nodes in Low Earth Orbit.";
            case "station_power_matrix" -> "Route reward from the restored Station Network; used in advanced orbital systems.";
            case "helium_extractor_core" -> "Consumed to restore lunar Helium Extractor Nodes.";
            case "lunar_pressure_map" -> "Route reward that records safer lunar pressure and radiation paths.";
            case "martian_habitat_key" -> "Recovered from pressurized Mars habitats and useful for route caches.";
            case "pressure_regulator" -> "Consumed to repair Mars Pressure Consoles in dust hazard zones.";
            case "europa_probe_array" -> "Consumed to calibrate Europa Thermal Arrays.";
            case "thermal_stabilizer" -> "Europa route reward used for Deep Space Protocol support.";
            case "station_relay_node" -> "Orbit repair objective; scan with a Station Relay Fuse to restore the network.";
            case "helium_extractor_node" -> "Lunar repair objective; scan with a Helium Extractor Core.";
            case "mars_pressure_console" -> "Mars repair objective; scan with a Pressure Regulator.";
            case "europa_thermal_array" -> "Europa repair objective; scan with a Europa Probe Array.";
            case "saturn_ring_relay" -> "Saturn repair objective; scan with a Saturn Relay Lens to stabilize Titan descent.";
            case "titan_methane_pump" -> "Titan repair objective; scan with a Titan Methane Cell to unlock Deep Space Protocol.";
            case "faction_vendor_kiosk" -> "Beta faction support kiosk; scan after pledging to authorize one barter cache per hub.";
            case "faction_relay_hub" -> "Beta faction relay hub; scan after pledging for support caches and contract routing.";
            case "cryo_battery" -> "Europa power core; can be fabricated by charging Cryo Crystals.";
            case "navigation_chip" -> "Recovered by reclaiming Vacuum Circuits; used for route computers.";
            case "nexus_drive_core" -> "Endgame drive core; craft it or fabricate one from Lunar Core Fragments.";
            case "navigation_console" -> "Reports station signal and debris diagnostics.";
            case "station_life_support_core" -> "Carry or recover one, then sneak-use the terminal in orbit to unlock Lunar Signal.";
            case "plasma_cutter" -> "Right-click to cut a locked target at short range.";
            case "rail_spike_launcher" -> "Right-click for a longer-range precision shot.";
            case "gravity_hammer" -> "Right-click or strike to deliver heavy knockback.";
            case "solar_lance" -> "Right-click for a focused mid-range beam hit.";
            case "nexus_pulse_blade" -> "Right-click for a dangerous Nexus pulse strike.";
            case "orbital_remnant_badge" -> FactionPledgeItem.Faction.ORBITAL_REMNANT.tooltip();
            case "void_salvager_marker" -> FactionPledgeItem.Faction.VOID_SALVAGERS.tooltip();
            case "nexus_choir_sigil" -> FactionPledgeItem.Faction.NEXUS_CHOIR.tooltip();
            default -> resourceTooltip(id.getPath());
        };

        EchoBackendClientBridge.addTooltipLine(event, Component.literal(line).withStyle(ChatFormatting.DARK_AQUA));
    }

    private static String resourceTooltip(String path) {
        if (path.contains("nexus")) {
            return "Nexus-grade material used in anomaly tech and endgame routes.";
        }
        if (path.contains("lunar") || path.contains("moon") || path.contains("helium")) {
            return "Lunar resource used for shuttle upgrades and route telemetry.";
        }
        if (path.contains("martian")) {
            return "Mars resource used to triangulate the Europa route.";
        }
        if (path.contains("cryo") || path.contains("frozen")) {
            return "Europa cryo resource used for deep-space systems.";
        }
        if (path.contains("saturn")) {
            return "Saturn ring resource used to stabilize Titan descent telemetry.";
        }
        if (path.contains("titan") || path.contains("methane")) {
            return "Titan methane resource used to unlock Deep Space and Nexus prep.";
        }
        if (path.contains("orbital") || path.contains("satellite") || path.contains("vacuum") || path.contains("solar")) {
            return "Orbital salvage used for launch, station, and ship systems.";
        }
        return "ECHO-7 component used in orbital survival progression.";
    }
}
