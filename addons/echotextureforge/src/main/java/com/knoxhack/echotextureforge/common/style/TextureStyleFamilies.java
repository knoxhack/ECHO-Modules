package com.knoxhack.echotextureforge.common.style;

import com.knoxhack.echotextureforge.api.spec.TextureStyleFamily;
import java.util.Locale;
import java.util.Map;

public final class TextureStyleFamilies {
    private static final Map<String, TextureStyleFamily> ADDON_DEFAULTS = Map.ofEntries(
            Map.entry("echoashfallprotocol", TextureStyleFamily.ASHFALL_SURVIVAL),
            Map.entry("echoterminal", TextureStyleFamily.ECHO_CYBERGLASS),
            Map.entry("echoindex", TextureStyleFamily.ECHO_CYBERGLASS),
            Map.entry("echoholomap", TextureStyleFamily.ORBITAL_TELEMETRY),
            Map.entry("echorelictech", TextureStyleFamily.RELICTECH),
            Map.entry("echoblockworks", TextureStyleFamily.INDUSTRIAL_NEXUS),
            Map.entry("echopowergrid", TextureStyleFamily.INDUSTRIAL_NEXUS),
            Map.entry("echoweathercore", TextureStyleFamily.WEATHERCORE_HAZARD),
            Map.entry("echoarmory", TextureStyleFamily.ARMORY_TACTICAL),
            Map.entry("echologisticsnetwork", TextureStyleFamily.INDUSTRIAL_NEXUS),
            Map.entry("echoindustrialnexus", TextureStyleFamily.INDUSTRIAL_NEXUS),
            Map.entry("echoorbitalremnants", TextureStyleFamily.ORBITAL_TELEMETRY),
            Map.entry("echonexusprotocol", TextureStyleFamily.NEXUS_PROTOCOL),
            Map.entry("echoagriculturereclamation", TextureStyleFamily.AGRICULTURE_RECLAMATION)
    );

    private TextureStyleFamilies() {
    }

    public static TextureStyleFamily defaultForNamespace(String namespace) {
        if (namespace == null || namespace.isBlank()) {
            return TextureStyleFamily.ASHFALL_SURVIVAL;
        }
        return ADDON_DEFAULTS.getOrDefault(namespace.toLowerCase(Locale.ROOT), TextureStyleFamily.ASHFALL_SURVIVAL);
    }

    public static Map<String, TextureStyleFamily> addonDefaults() {
        return ADDON_DEFAULTS;
    }
}
