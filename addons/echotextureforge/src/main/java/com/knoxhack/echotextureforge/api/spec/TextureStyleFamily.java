package com.knoxhack.echotextureforge.api.spec;

import java.util.List;
import java.util.Locale;

public enum TextureStyleFamily {
    ASHFALL_SURVIVAL(
            "ruined, dusty, worn, practical, survival tech",
            List.of("dust gray", "ash black", "rust", "muted survival orange"),
            List.of("scratched metal", "cloth wraps", "burned plastic", "reclaimed parts"),
            "low, practical contrast with small worn highlights; avoid glossy showroom lighting",
            "chunky repaired silhouettes, exposed bolts, rugged survival readability",
            List.of("clean sci-fi glass", "luxury polish", "large magical glow"),
            "A battered field component with dust-worn edges and salvage-tech utility."),
    ECHO_CYBERGLASS(
            "clean cyan UI, dark panels, readable icons",
            List.of("cyan", "deep charcoal", "soft white", "small amber status accents"),
            List.of("dark glass", "thin luminous traces", "clean panel bevels"),
            "crisp internal glow with restrained bloom; keep edges pixel-clean",
            "flat readable icon silhouettes, thin signal lines, compact panel geometry",
            List.of("busy noise", "muddy browns", "photoreal monitor glare"),
            "A clean cyberglass UI icon with cyan signal geometry on transparent edges."),
    ORBITAL_TELEMETRY(
            "aerospace, white/orange/cyan, signal markings",
            List.of("off white", "orbital orange", "cyan", "dark graphite"),
            List.of("ceramic plating", "aerospace panels", "antenna markings", "thermal shielding"),
            "bright technical highlights with clear dark separation; no lens-flare effects",
            "aerospace modules, compact antennas, warning ticks, readable telemetry silhouettes",
            List.of("fantasy runes", "dirty medieval metal", "illegible microtext"),
            "A compact orbital telemetry part with orange/cyan signal markings."),
    RELICTECH(
            "ancient tech, dark metal, cyan cracks, artifact glow",
            List.of("ancient black metal", "aged bronze", "cyan energy cracks", "cold gray"),
            List.of("old alloy", "fractured crystal", "etched artifact plates"),
            "small cyan fissure glow inside dark material; keep glow contained",
            "mysterious artifact silhouettes, cracked cores, old-tech asymmetry",
            List.of("clean plastic", "oversized glow blobs", "flat modern UI panels"),
            "A dark ancient tech component with controlled cyan cracks and a strong inventory silhouette."),
    INDUSTRIAL_NEXUS(
            "heavy machine, steel, hazard stripes, functional ports",
            List.of("gunmetal", "steel gray", "hazard yellow", "black", "small red warnings"),
            List.of("machined steel", "ribbed vents", "rubber seals", "functional port plates"),
            "hard mechanical contrast, edge highlights on metal, readable ports",
            "heavy blocky machines, front/side/top clarity, ports and warning overlays",
            List.of("soft magical shapes", "decorative-only noise", "unreadable vent clutter"),
            "A heavy functional machine face with clear ports, steel panels, and restrained hazard marks."),
    ARMORY_TACTICAL(
            "weapons, armor, tactical plates, readable silhouettes",
            List.of("matte black", "dark olive", "steel", "small red/cyan status accents"),
            List.of("tactical plates", "weapon grips", "reinforced fabric", "scratched alloy"),
            "matte lighting with crisp edge accents; avoid glossy toy-like surfaces",
            "weapon and armor silhouettes that read instantly in a small inventory slot",
            List.of("oversized muzzle flash", "soft blur", "ornamental fantasy clutter"),
            "A tactical armory asset with layered plates and a clear combat-ready silhouette."),
    WEATHERCORE_HAZARD(
            "storms, filters, radiation, acid, ash, warning colors",
            List.of("storm gray", "acid green", "warning yellow", "radiation orange", "filter black"),
            List.of("sealed filters", "weathered sensors", "hazard housings", "protective glass"),
            "urgent readable contrast, small warning accents, no full-background glow",
            "hazard equipment silhouettes, filters, emitters, sensors, weatherproof casings",
            List.of("calm pastel palette", "clean luxury UI", "large background storms"),
            "A compact hazard-tech component with warning color accents and weatherproof structure."),
    AGRICULTURE_RECLAMATION(
            "recovered soil, hydroponics, greenhouse tech, seed/plant stages",
            List.of("reclaimed green", "soil brown", "greenhouse glass cyan", "warm grow-light amber"),
            List.of("soil", "seed pods", "hydroponic trays", "greenhouse glass", "recovered organic matter"),
            "warm growth highlights balanced with dirty reclaimed materials",
            "readable seeds, crop stages, trays, greenhouse parts, practical reclamation tech",
            List.of("pure fantasy flowers", "photoreal leaves", "flat single-color blobs"),
            "A reclamation agriculture asset with recovered organic material and practical greenhouse tech."),
    NEXUS_PROTOCOL(
            "mysterious endgame, dark glass, violet/cyan signal scars",
            List.of("near black glass", "violet", "cyan", "cold white", "deep blue-gray"),
            List.of("dark glass", "signal-scarred alloy", "fractured protocol plates"),
            "controlled high-tech glow, sharp signal scars, strong contrast against transparency",
            "endgame mysterious silhouettes, angular protocol marks, dark glass fragments",
            List.of("warm rustic palettes", "cartoon magic stars", "uncontrolled glow fog"),
            "A dark endgame protocol asset with violet/cyan signal scars and sharp readable edges.");

    private final String visualDirection;
    private final List<String> paletteHints;
    private final List<String> materialHints;
    private final String lightingRules;
    private final String shapeLanguage;
    private final List<String> avoidList;
    private final String exampleDirection;

    TextureStyleFamily(String visualDirection, List<String> paletteHints, List<String> materialHints,
                       String lightingRules, String shapeLanguage, List<String> avoidList, String exampleDirection) {
        this.visualDirection = visualDirection;
        this.paletteHints = List.copyOf(paletteHints);
        this.materialHints = List.copyOf(materialHints);
        this.lightingRules = lightingRules;
        this.shapeLanguage = shapeLanguage;
        this.avoidList = List.copyOf(avoidList);
        this.exampleDirection = exampleDirection;
    }

    public String visualDirection() {
        return visualDirection;
    }

    public List<String> paletteHints() {
        return paletteHints;
    }

    public List<String> materialHints() {
        return materialHints;
    }

    public String lightingRules() {
        return lightingRules;
    }

    public String shapeLanguage() {
        return shapeLanguage;
    }

    public List<String> avoidList() {
        return avoidList;
    }

    public String exampleDirection() {
        return exampleDirection;
    }

    public static TextureStyleFamily byId(String raw, TextureStyleFamily fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String normalized = raw.strip().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        for (TextureStyleFamily family : values()) {
            if (family.name().equals(normalized)) {
                return family;
            }
        }
        return fallback;
    }
}
