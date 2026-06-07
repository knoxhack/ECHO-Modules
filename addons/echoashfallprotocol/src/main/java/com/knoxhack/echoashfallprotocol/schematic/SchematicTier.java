package com.knoxhack.echoashfallprotocol.schematic;

/**
 * Tier taxonomy for Schematic Fragments.
 *
 * Each tier gates a group of recipes that roughly corresponds to a progression phase.
 * See SchematicUnlockTable for the concrete recipe lists per tier.
 */
public enum SchematicTier {
    BASIC("basic", 0xAAAAAA),
    INDUSTRIAL("industrial", 0x88AAEE),
    REFINED("refined", 0xEEBB44),
    CRYSTALLINE("crystalline", 0xAA88FF),
    NEXUS("nexus", 0xFF55AA);

    private final String id;
    private final int tooltipColor;

    SchematicTier(String id, int tooltipColor) {
        this.id = id;
        this.tooltipColor = tooltipColor;
    }

    public String id() { return id; }
    public int tooltipColor() { return tooltipColor; }
}
