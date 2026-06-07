package com.knoxhack.echosoundcore.registry;

import com.knoxhack.echosoundcore.EchoSoundCore;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class SoundCoreSounds {
    private static final List<NativeRegistryHolder<SoundEvent>> SOUNDS = new ArrayList<>();

    // Menu music
    public static final NativeRegistryHolder<SoundEvent> MUSIC_MENU_ASHFALL = music("music.menu.ashfall");
    public static final NativeRegistryHolder<SoundEvent> MUSIC_MENU_NEXUS = music("music.menu.nexus");
    public static final NativeRegistryHolder<SoundEvent> MUSIC_MENU_ORBITAL = music("music.menu.orbital");
    public static final NativeRegistryHolder<SoundEvent> MUSIC_MENU_BLACKBOX = music("music.menu.blackbox");

    // Gameplay music
    public static final NativeRegistryHolder<SoundEvent> MUSIC_GAMEPLAY_SAFE_BASE = music("music.gameplay.safe_base");
    public static final NativeRegistryHolder<SoundEvent> MUSIC_GAMEPLAY_EXPLORATION = music("music.gameplay.exploration");
    public static final NativeRegistryHolder<SoundEvent> MUSIC_GAMEPLAY_NIGHT = music("music.gameplay.night");
    public static final NativeRegistryHolder<SoundEvent> MUSIC_GAMEPLAY_UNDERGROUND = music("music.gameplay.underground");

    // Biome music
    public static final NativeRegistryHolder<SoundEvent> MUSIC_BIOME_WASTELAND = music("music.biome.wasteland");
    public static final NativeRegistryHolder<SoundEvent> MUSIC_BIOME_CRASH_ZONE = music("music.biome.crash_zone");
    public static final NativeRegistryHolder<SoundEvent> MUSIC_BIOME_TOXIC_SWAMP = music("music.biome.toxic_swamp");
    public static final NativeRegistryHolder<SoundEvent> MUSIC_BIOME_RADIATION_ZONE = music("music.biome.radiation_zone");
    public static final NativeRegistryHolder<SoundEvent> MUSIC_BIOME_CRYOGENIC_RUINS = music("music.biome.cryogenic_ruins");
    public static final NativeRegistryHolder<SoundEvent> MUSIC_BIOME_RUINED_CITY = music("music.biome.ruined_city");
    public static final NativeRegistryHolder<SoundEvent> MUSIC_BIOME_INDUSTRIAL_RUINS = music("music.biome.industrial_ruins");
    public static final NativeRegistryHolder<SoundEvent> MUSIC_BIOME_NEXUS_SCAR = music("music.biome.nexus_scar");

    // Faction music
    public static final NativeRegistryHolder<SoundEvent> MUSIC_FACTION_RADWARDEN_AMBIENT = music("music.faction.radwarden.ambient");
    public static final NativeRegistryHolder<SoundEvent> MUSIC_FACTION_CRASHBREAK_AMBIENT = music("music.faction.crashbreak.ambient");
    public static final NativeRegistryHolder<SoundEvent> MUSIC_FACTION_SPOREBOUND_AMBIENT = music("music.faction.sporebound.ambient");
    public static final NativeRegistryHolder<SoundEvent> MUSIC_FACTION_RADWARDEN_COMBAT = music("music.faction.radwarden.combat");
    public static final NativeRegistryHolder<SoundEvent> MUSIC_FACTION_CRASHBREAK_COMBAT = music("music.faction.crashbreak.combat");
    public static final NativeRegistryHolder<SoundEvent> MUSIC_FACTION_SPOREBOUND_COMBAT = music("music.faction.sporebound.combat");

    // Combat music
    public static final NativeRegistryHolder<SoundEvent> MUSIC_COMBAT_LIGHT = music("music.combat.light");
    public static final NativeRegistryHolder<SoundEvent> MUSIC_COMBAT_HEAVY = music("music.combat.heavy");
    public static final NativeRegistryHolder<SoundEvent> MUSIC_COMBAT_NEXUS = music("music.combat.nexus");
    public static final NativeRegistryHolder<SoundEvent> MUSIC_COMBAT_FACTION_RAID = music("music.combat.faction_raid");
    public static final NativeRegistryHolder<SoundEvent> MUSIC_COMBAT_SIEGE = music("music.combat.siege");

    // Boss music
    public static final NativeRegistryHolder<SoundEvent> MUSIC_BOSS_WARDEN = music("music.boss.warden");
    public static final NativeRegistryHolder<SoundEvent> MUSIC_BOSS_GUARDIAN_WASTELAND = music("music.boss.guardian.wasteland");
    public static final NativeRegistryHolder<SoundEvent> MUSIC_BOSS_GUARDIAN_TOXIC = music("music.boss.guardian.toxic");
    public static final NativeRegistryHolder<SoundEvent> MUSIC_BOSS_GUARDIAN_RADIATION = music("music.boss.guardian.radiation");
    public static final NativeRegistryHolder<SoundEvent> MUSIC_BOSS_GUARDIAN_CRYO = music("music.boss.guardian.cryo");
    public static final NativeRegistryHolder<SoundEvent> MUSIC_BOSS_GUARDIAN_INDUSTRIAL = music("music.boss.guardian.industrial");
    public static final NativeRegistryHolder<SoundEvent> MUSIC_BOSS_GUARDIAN_CITY = music("music.boss.guardian.city");
    public static final NativeRegistryHolder<SoundEvent> MUSIC_BOSS_GUARDIAN_NEXUS = music("music.boss.guardian.nexus");
    public static final NativeRegistryHolder<SoundEvent> MUSIC_BOSS_CORRUPTION_BLOOM = music("music.boss.corruption_bloom");
    public static final NativeRegistryHolder<SoundEvent> MUSIC_BOSS_SEVERANCE_ENGINE = music("music.boss.severance_engine");
    public static final NativeRegistryHolder<SoundEvent> MUSIC_BOSS_MIRROR_COMMAND = music("music.boss.mirror_command");
    public static final NativeRegistryHolder<SoundEvent> MUSIC_BOSS_STATION_MOTHER = music("music.boss.station_mother");

    // Chapter music
    public static final NativeRegistryHolder<SoundEvent> MUSIC_CHAPTER_ASHFALL_PROTOCOL = music("music.chapter.ashfall_protocol");
    public static final NativeRegistryHolder<SoundEvent> MUSIC_CHAPTER_ORBITAL_REMNANTS = music("music.chapter.orbital_remnants");
    public static final NativeRegistryHolder<SoundEvent> MUSIC_CHAPTER_AGRICULTURE_RECLAMATION = music("music.chapter.agriculture_reclamation");
    public static final NativeRegistryHolder<SoundEvent> MUSIC_CHAPTER_STATIONFALL = music("music.chapter.stationfall");
    public static final NativeRegistryHolder<SoundEvent> MUSIC_CHAPTER_NEXUS_PROTOCOL = music("music.chapter.nexus_protocol");
    public static final NativeRegistryHolder<SoundEvent> MUSIC_CHAPTER_INDUSTRIAL_NEXUS = music("music.chapter.industrial_nexus");
    public static final NativeRegistryHolder<SoundEvent> MUSIC_CHAPTER_LOGISTICS_NETWORK = music("music.chapter.logistics_network");
    public static final NativeRegistryHolder<SoundEvent> MUSIC_CHAPTER_CONVOY_PROTOCOL = music("music.chapter.convoy_protocol");
    public static final NativeRegistryHolder<SoundEvent> MUSIC_CHAPTER_ARMORY = music("music.chapter.armory");
    public static final NativeRegistryHolder<SoundEvent> MUSIC_CHAPTER_BLACKBOX_PROTOCOL = music("music.chapter.blackbox_protocol");

    // Terminal music beds
    public static final NativeRegistryHolder<SoundEvent> MUSIC_TERMINAL_COMMAND_BED = music("music.terminal.command_bed");
    public static final NativeRegistryHolder<SoundEvent> MUSIC_TERMINAL_NEXUS_CORRUPTED = music("music.terminal.nexus_corrupted");

    // Nexus music
    public static final NativeRegistryHolder<SoundEvent> MUSIC_NEXUS_CORE_AMBIENCE = music("music.nexus.core_ambience");
    public static final NativeRegistryHolder<SoundEvent> MUSIC_NEXUS_CHOICE_ROOM = music("music.nexus.choice_room");
    public static final NativeRegistryHolder<SoundEvent> MUSIC_NEXUS_SIEGE = music("music.nexus.siege");

    // Blackbox music
    public static final NativeRegistryHolder<SoundEvent> MUSIC_BLACKBOX_MEMORY_FRAGMENT = music("music.blackbox.memory_fragment");
    public static final NativeRegistryHolder<SoundEvent> MUSIC_BLACKBOX_FALSE_SIGNAL = music("music.blackbox.false_signal");
    public static final NativeRegistryHolder<SoundEvent> MUSIC_BLACKBOX_TRUTH_ENGINE = music("music.blackbox.truth_engine");

    // Stingers
    public static final NativeRegistryHolder<SoundEvent> STINGER_MISSION_ACCEPT = stinger("stinger.mission.accept");
    public static final NativeRegistryHolder<SoundEvent> STINGER_MISSION_UPDATE = stinger("stinger.mission.update");
    public static final NativeRegistryHolder<SoundEvent> STINGER_OBJECTIVE_COMPLETE = stinger("stinger.objective.complete");
    public static final NativeRegistryHolder<SoundEvent> STINGER_MISSION_COMPLETE = stinger("stinger.mission.complete");
    public static final NativeRegistryHolder<SoundEvent> STINGER_SIGNAL_DETECTED = stinger("stinger.signal.detected");
    public static final NativeRegistryHolder<SoundEvent> STINGER_GUARDIAN_LOCATED = stinger("stinger.guardian.located");
    public static final NativeRegistryHolder<SoundEvent> STINGER_NEXUS_STATE_CHANGED = stinger("stinger.nexus.state_changed");
    public static final NativeRegistryHolder<SoundEvent> STINGER_CHAPTER_UNLOCKED = stinger("stinger.chapter.unlocked");
    public static final NativeRegistryHolder<SoundEvent> STINGER_REWARD_AVAILABLE = stinger("stinger.reward.available");
    public static final NativeRegistryHolder<SoundEvent> STINGER_FACTION_RADWARDEN_REP_UP = stinger("stinger.faction.radwarden.rep_up");
    public static final NativeRegistryHolder<SoundEvent> STINGER_FACTION_CRASHBREAK_REP_UP = stinger("stinger.faction.crashbreak.rep_up");
    public static final NativeRegistryHolder<SoundEvent> STINGER_FACTION_SPOREBOUND_REP_UP = stinger("stinger.faction.sporebound.rep_up");

    // Terminal UI
    public static final NativeRegistryHolder<SoundEvent> UI_TERMINAL_OPEN = ui("ui.terminal.open");
    public static final NativeRegistryHolder<SoundEvent> UI_TERMINAL_CLOSE = ui("ui.terminal.close");
    public static final NativeRegistryHolder<SoundEvent> UI_TERMINAL_TAB = ui("ui.terminal.tab");
    public static final NativeRegistryHolder<SoundEvent> UI_TERMINAL_SELECT = ui("ui.terminal.select");
    public static final NativeRegistryHolder<SoundEvent> UI_TERMINAL_ERROR = ui("ui.terminal.error");
    public static final NativeRegistryHolder<SoundEvent> UI_TERMINAL_REWARD_CLAIM = ui("ui.terminal.reward_claim");
    public static final NativeRegistryHolder<SoundEvent> UI_TERMINAL_NEW_INTEL = ui("ui.terminal.new_intel");
    public static final NativeRegistryHolder<SoundEvent> UI_TERMINAL_WARNING = ui("ui.terminal.warning");
    public static final NativeRegistryHolder<SoundEvent> UI_TERMINAL_CORRUPT_TICK = ui("ui.terminal.corrupt_tick");

    // Lens UI
    public static final NativeRegistryHolder<SoundEvent> UI_LENS_SCAN_COMPACT = ui("ui.lens.scan.compact");
    public static final NativeRegistryHolder<SoundEvent> UI_LENS_SCAN_EXPANDED = ui("ui.lens.scan.expanded");
    public static final NativeRegistryHolder<SoundEvent> UI_LENS_DEEP_SCAN_START = ui("ui.lens.deep_scan.start");
    public static final NativeRegistryHolder<SoundEvent> UI_LENS_DEEP_SCAN_LOOP = ui("ui.lens.deep_scan.loop");
    public static final NativeRegistryHolder<SoundEvent> UI_LENS_DEEP_SCAN_COMPLETE = ui("ui.lens.deep_scan.complete");
    public static final NativeRegistryHolder<SoundEvent> UI_LENS_SCAN_INVALID = ui("ui.lens.scan.invalid");
    public static final NativeRegistryHolder<SoundEvent> UI_LENS_HAZARD_DETECTED = ui("ui.lens.hazard.detected");
    public static final NativeRegistryHolder<SoundEvent> UI_LENS_MACHINE_DIAGNOSTICS = ui("ui.lens.machine.diagnostics");

    // HoloMap UI
    public static final NativeRegistryHolder<SoundEvent> UI_HOLOMAP_OPEN = ui("ui.holomap.open");
    public static final NativeRegistryHolder<SoundEvent> UI_HOLOMAP_CLOSE = ui("ui.holomap.close");
    public static final NativeRegistryHolder<SoundEvent> UI_HOLOMAP_WAYPOINT_PLACE = ui("ui.holomap.waypoint.place");
    public static final NativeRegistryHolder<SoundEvent> UI_HOLOMAP_WAYPOINT_REMOVE = ui("ui.holomap.waypoint.remove");
    public static final NativeRegistryHolder<SoundEvent> UI_HOLOMAP_OVERLAY_HAZARD = ui("ui.holomap.overlay.hazard");
    public static final NativeRegistryHolder<SoundEvent> UI_HOLOMAP_ROUTE_CALCULATED = ui("ui.holomap.route.calculated");
    public static final NativeRegistryHolder<SoundEvent> UI_HOLOMAP_UNKNOWN = ui("ui.holomap.unknown");

    // SignalOS UI
    public static final NativeRegistryHolder<SoundEvent> UI_SIGNALOS_BOOT = ui("ui.signaloos.boot");
    public static final NativeRegistryHolder<SoundEvent> UI_SIGNALOS_LOGIN = ui("ui.signaloos.login");
    public static final NativeRegistryHolder<SoundEvent> UI_SIGNALOS_FILE_OPEN = ui("ui.signaloos.file.open");
    public static final NativeRegistryHolder<SoundEvent> UI_SIGNALOS_FILE_SAVE = ui("ui.signaloos.file.save");
    public static final NativeRegistryHolder<SoundEvent> UI_SIGNALOS_NETWORK_PING = ui("ui.signaloos.network.ping");
    public static final NativeRegistryHolder<SoundEvent> UI_SIGNALOS_DRIVE_INSERT = ui("ui.signaloos.drive.insert");
    public static final NativeRegistryHolder<SoundEvent> UI_SIGNALOS_DRIVE_REMOVE = ui("ui.signaloos.drive.remove");
    public static final NativeRegistryHolder<SoundEvent> UI_SIGNALOS_VAULT_UNLOCK = ui("ui.signaloos.vault.unlock");
    public static final NativeRegistryHolder<SoundEvent> UI_SIGNALOS_APP_OPEN = ui("ui.signaloos.app.open");
    public static final NativeRegistryHolder<SoundEvent> UI_SIGNALOS_APP_CLOSE = ui("ui.signaloos.app.close");

    // SignalOS UI aliases using the canonical spelling. The historical
    // signaloos ids stay registered for datapack and theme compatibility.
    public static final NativeRegistryHolder<SoundEvent> UI_SIGNALOS_BOOT_CANONICAL = ui("ui.signalos.boot");
    public static final NativeRegistryHolder<SoundEvent> UI_SIGNALOS_LOGIN_CANONICAL = ui("ui.signalos.login");
    public static final NativeRegistryHolder<SoundEvent> UI_SIGNALOS_FILE_OPEN_CANONICAL = ui("ui.signalos.file.open");
    public static final NativeRegistryHolder<SoundEvent> UI_SIGNALOS_FILE_SAVE_CANONICAL = ui("ui.signalos.file.save");
    public static final NativeRegistryHolder<SoundEvent> UI_SIGNALOS_NETWORK_PING_CANONICAL = ui("ui.signalos.network.ping");
    public static final NativeRegistryHolder<SoundEvent> UI_SIGNALOS_DRIVE_INSERT_CANONICAL = ui("ui.signalos.drive.insert");
    public static final NativeRegistryHolder<SoundEvent> UI_SIGNALOS_DRIVE_REMOVE_CANONICAL = ui("ui.signalos.drive.remove");
    public static final NativeRegistryHolder<SoundEvent> UI_SIGNALOS_VAULT_UNLOCK_CANONICAL = ui("ui.signalos.vault.unlock");
    public static final NativeRegistryHolder<SoundEvent> UI_SIGNALOS_APP_OPEN_CANONICAL = ui("ui.signalos.app.open");
    public static final NativeRegistryHolder<SoundEvent> UI_SIGNALOS_APP_CLOSE_CANONICAL = ui("ui.signalos.app.close");

    // PowerGrid sounds
    public static final NativeRegistryHolder<SoundEvent> POWERGRID_GENERATOR_HAND_CRANK = block("powergrid.generator.hand_crank");
    public static final NativeRegistryHolder<SoundEvent> POWERGRID_GENERATOR_SCRAP_BURNER = block("powergrid.generator.scrap_burner");
    public static final NativeRegistryHolder<SoundEvent> POWERGRID_BATTERY_HUM = block("powergrid.battery.hum");
    public static final NativeRegistryHolder<SoundEvent> POWERGRID_CABLE_LOW_VOLTAGE = block("powergrid.cable.low_voltage");
    public static final NativeRegistryHolder<SoundEvent> POWERGRID_SUBSTATION_LOOP = block("powergrid.substation.loop");
    public static final NativeRegistryHolder<SoundEvent> POWERGRID_BREAKER_TRIP = block("powergrid.breaker.trip");
    public static final NativeRegistryHolder<SoundEvent> POWERGRID_BROWNOUT = block("powergrid.brownout");
    public static final NativeRegistryHolder<SoundEvent> POWERGRID_OVERLOAD_WARNING = block("powergrid.overload.warning");
    public static final NativeRegistryHolder<SoundEvent> POWERGRID_OVERLOAD_FAILURE = block("powergrid.overload.failure");
    public static final NativeRegistryHolder<SoundEvent> POWERGRID_POWER_RESTORED = block("powergrid.power_restored");

    // Drone sounds
    public static final NativeRegistryHolder<SoundEvent> DRONE_BOOT = entity("drone.boot");
    public static final NativeRegistryHolder<SoundEvent> DRONE_FOLLOW = entity("drone.follow");
    public static final NativeRegistryHolder<SoundEvent> DRONE_SCOUT_PING = entity("drone.scout_ping");
    public static final NativeRegistryHolder<SoundEvent> DRONE_TARGET_MARK = entity("drone.target_mark");
    public static final NativeRegistryHolder<SoundEvent> DRONE_LOW_POWER = entity("drone.low_power");
    public static final NativeRegistryHolder<SoundEvent> DRONE_DAMAGED = entity("drone.damaged");
    public static final NativeRegistryHolder<SoundEvent> DRONE_REPAIRED = entity("drone.repaired");
    public static final NativeRegistryHolder<SoundEvent> DRONE_MODE_SWITCH = entity("drone.mode_switch");
    public static final NativeRegistryHolder<SoundEvent> DRONE_COMBAT_SUPPRESSION = entity("drone.combat_suppression");
    public static final NativeRegistryHolder<SoundEvent> DRONE_RETURN = entity("drone.return");

    // Nexus ambience
    public static final NativeRegistryHolder<SoundEvent> NEXUS_AMBIENCE_LOW_HUM = ambient("nexus.ambience.low_hum");
    public static final NativeRegistryHolder<SoundEvent> NEXUS_AMBIENCE_GLITCH_PULSE = ambient("nexus.ambience.glitch_pulse");
    public static final NativeRegistryHolder<SoundEvent> NEXUS_AMBIENCE_MEMORY_WHISPER = ambient("nexus.ambience.memory_whisper");
    public static final NativeRegistryHolder<SoundEvent> NEXUS_AMBIENCE_REALITY_TEAR = ambient("nexus.ambience.reality_tear");
    public static final NativeRegistryHolder<SoundEvent> NEXUS_CORE_HEARTBEAT = ambient("nexus.core.heartbeat");
    public static final NativeRegistryHolder<SoundEvent> NEXUS_CHOICE_ROOM = ambient("nexus.choice_room");

    private SoundCoreSounds() {}

    public static void register() {
    }

    public static Collection<NativeRegistryHolder<SoundEvent>> getEntries() {
        return List.copyOf(SOUNDS);
    }

    private static NativeRegistryHolder<SoundEvent> music(String name) {
        return register(name);
    }

    private static NativeRegistryHolder<SoundEvent> stinger(String name) {
        return register(name);
    }

    private static NativeRegistryHolder<SoundEvent> ui(String name) {
        return register(name);
    }

    private static NativeRegistryHolder<SoundEvent> block(String name) {
        return register(name);
    }

    private static NativeRegistryHolder<SoundEvent> entity(String name) {
        return register(name);
    }

    private static NativeRegistryHolder<SoundEvent> ambient(String name) {
        return register(name);
    }

    private static NativeRegistryHolder<SoundEvent> register(String name) {
        Identifier id = Identifier.fromNamespaceAndPath(EchoSoundCore.MODID, name);
        NativeRegistryHolder<SoundEvent> holder = NativeRegistryHolder.of(id, SoundEvent.createVariableRangeEvent(id));
        SOUNDS.add(holder);
        return holder;
    }
}
