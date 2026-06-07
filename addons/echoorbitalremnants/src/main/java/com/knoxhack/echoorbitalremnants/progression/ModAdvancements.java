package com.knoxhack.echoorbitalremnants.progression;

import com.knoxhack.echoorbitalremnants.EchoOrbitalRemnants;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public final class ModAdvancements {
    public static final Identifier ECHO_ZERO_RESOLVED =
            Identifier.fromNamespaceAndPath(EchoOrbitalRemnants.MODID, "echo_zero_resolved");
    public static final Identifier ORBIT_SURVEY_COMPLETE =
            Identifier.fromNamespaceAndPath(EchoOrbitalRemnants.MODID, "orbit_survey_complete");
    public static final Identifier MOON_SURVEY_COMPLETE =
            Identifier.fromNamespaceAndPath(EchoOrbitalRemnants.MODID, "moon_survey_complete");
    public static final Identifier MARS_SURVEY_COMPLETE =
            Identifier.fromNamespaceAndPath(EchoOrbitalRemnants.MODID, "mars_survey_complete");
    public static final Identifier EUROPA_SURVEY_COMPLETE =
            Identifier.fromNamespaceAndPath(EchoOrbitalRemnants.MODID, "europa_survey_complete");
    public static final Identifier SATURN_SURVEY_COMPLETE =
            Identifier.fromNamespaceAndPath(EchoOrbitalRemnants.MODID, "saturn_survey_complete");
    public static final Identifier TITAN_SURVEY_COMPLETE =
            Identifier.fromNamespaceAndPath(EchoOrbitalRemnants.MODID, "titan_survey_complete");
    public static final Identifier NEXUS_STABILIZED =
            Identifier.fromNamespaceAndPath(EchoOrbitalRemnants.MODID, "nexus_stabilized");
    public static final Identifier ORBIT_DEEP_SITE_DISCOVERED =
            Identifier.fromNamespaceAndPath(EchoOrbitalRemnants.MODID, "orbit_deep_site_discovered");
    public static final Identifier MOON_DEEP_SITE_DISCOVERED =
            Identifier.fromNamespaceAndPath(EchoOrbitalRemnants.MODID, "moon_deep_site_discovered");
    public static final Identifier MARS_DEEP_SITE_DISCOVERED =
            Identifier.fromNamespaceAndPath(EchoOrbitalRemnants.MODID, "mars_deep_site_discovered");
    public static final Identifier EUROPA_DEEP_SITE_DISCOVERED =
            Identifier.fromNamespaceAndPath(EchoOrbitalRemnants.MODID, "europa_deep_site_discovered");
    public static final Identifier SATURN_DEEP_SITE_DISCOVERED =
            Identifier.fromNamespaceAndPath(EchoOrbitalRemnants.MODID, "saturn_deep_site_discovered");
    public static final Identifier TITAN_DEEP_SITE_DISCOVERED =
            Identifier.fromNamespaceAndPath(EchoOrbitalRemnants.MODID, "titan_deep_site_discovered");
    public static final Identifier NEXUS_DEEP_SITE_DISCOVERED =
            Identifier.fromNamespaceAndPath(EchoOrbitalRemnants.MODID, "nexus_deep_site_discovered");
    public static final Identifier FIRST_FACTION_CONTRACT =
            Identifier.fromNamespaceAndPath(EchoOrbitalRemnants.MODID, "first_faction_contract");
    public static final Identifier STATION_NETWORK_RESTORED =
            Identifier.fromNamespaceAndPath(EchoOrbitalRemnants.MODID, "station_network_restored");
    public static final Identifier HELIUM_EXTRACTOR_ONLINE =
            Identifier.fromNamespaceAndPath(EchoOrbitalRemnants.MODID, "helium_extractor_online");
    public static final Identifier MARS_HABITATS_PRESSURIZED =
            Identifier.fromNamespaceAndPath(EchoOrbitalRemnants.MODID, "mars_habitats_pressurized");
    public static final Identifier EUROPA_ARRAY_CALIBRATED =
            Identifier.fromNamespaceAndPath(EchoOrbitalRemnants.MODID, "europa_array_calibrated");
    public static final Identifier SATURN_RELAYS_RESTORED =
            Identifier.fromNamespaceAndPath(EchoOrbitalRemnants.MODID, "saturn_relays_restored");
    public static final Identifier TITAN_PUMPS_PRESSURIZED =
            Identifier.fromNamespaceAndPath(EchoOrbitalRemnants.MODID, "titan_pumps_pressurized");
    public static final Identifier MID_GAME_ROUTE_MASTERY =
            Identifier.fromNamespaceAndPath(EchoOrbitalRemnants.MODID, "mid_game_route_mastery");
    public static final Identifier ORBITAL_REMNANTS_COMPLETE =
            Identifier.fromNamespaceAndPath(EchoOrbitalRemnants.MODID, "orbital_remnants_complete");

    private ModAdvancements() {
    }

    public static boolean grantEchoZeroResolved(ServerPlayer player) {
        return grantManual(player, ECHO_ZERO_RESOLVED, "resolved");
    }

    public static boolean grantManual(ServerPlayer player, Identifier advancement, String criterion) {
        AdvancementHolder holder = player.level().getServer().getAdvancements().get(advancement);
        return holder != null && player.getAdvancements().award(holder, criterion);
    }

    public static boolean hasEchoZeroResolved(ServerPlayer player) {
        AdvancementHolder holder = player.level().getServer().getAdvancements().get(ECHO_ZERO_RESOLVED);
        return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
    }
}
