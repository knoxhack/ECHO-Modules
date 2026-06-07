package com.knoxhack.echotextureforge.api.spec;

import java.util.Locale;

public enum TextureType {
    CUBE_ALL("cube_all"),
    CUBE_COLUMN("cube_column"),
    CUBE_TOP_BOTTOM("cube_top_bottom"),
    MACHINE_FRONT_SIDE_TOP("machine_front_side_top"),
    MACHINE_ACTIVE_INACTIVE("machine_active_inactive"),
    CONNECTED_TEXTURE("connected_texture"),
    LAYERED_OVERLAY("layered_overlay"),
    GLOWING_OVERLAY("glowing_overlay"),
    TRANSPARENT_CUTOUT("transparent_cutout"),
    CROP_STAGE("crop_stage"),
    FLUID_BLOCK("fluid_block"),
    ORE("ore"),
    DECORATIVE_PANEL("decorative_panel"),
    RUINED_VARIANT("ruined_variant"),
    HAZARD_VARIANT("hazard_variant"),
    SIMPLE_ITEM("simple_item"),
    TOOL("tool"),
    WEAPON("weapon"),
    ARMOR_PIECE("armor_piece"),
    UPGRADE_CHIP("upgrade_chip"),
    COMPONENT("component"),
    MATERIAL("material"),
    FOOD("food"),
    RELIC("relic"),
    BLUEPRINT("blueprint"),
    BATTERY("battery"),
    CONTAINER("container"),
    DATA_DRIVE("data_drive"),
    MODULE("module"),
    FRONT("front"),
    SIDE("side"),
    TOP("top"),
    BOTTOM("bottom"),
    BACK("back"),
    ACTIVE_FRONT("active_front"),
    INACTIVE_FRONT("inactive_front"),
    WARNING_OVERLAY("warning_overlay"),
    INPUT_MARKER("input_marker"),
    OUTPUT_MARKER("output_marker"),
    ENERGY_PORT("energy_port"),
    FLUID_PORT("fluid_port"),
    ITEM_PORT("item_port"),
    HELMET_ICON("helmet_icon"),
    CHESTPLATE_ICON("chestplate_icon"),
    LEGGINGS_ICON("leggings_icon"),
    BOOTS_ICON("boots_icon"),
    ARMOR_LAYER_1("armor_layer_1"),
    ARMOR_LAYER_2("armor_layer_2"),
    TRIM_OVERLAY("trim_overlay"),
    DAMAGED_VARIANT("damaged_variant"),
    MOB_BASE("mob_base"),
    ARMOR_OVERLAY("armor_overlay"),
    BIOME_VARIANT("biome_variant"),
    ANIMATION_SHEET("animation_sheet"),
    ICON("icon"),
    BUTTON("button"),
    PANEL("panel"),
    TAB("tab"),
    STATUS_CHIP("status_chip"),
    MISSION_ICON("mission_icon"),
    RECIPE_ICON("recipe_icon"),
    WARNING_ICON("warning_icon"),
    PROGRESS_ICON("progress_icon");

    private final String id;

    TextureType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static TextureType byId(String raw, TextureType fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String normalized = raw.strip().toLowerCase(Locale.ROOT).replace('-', '_');
        for (TextureType type : values()) {
            if (type.id.equals(normalized) || type.name().equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        return fallback;
    }
}
