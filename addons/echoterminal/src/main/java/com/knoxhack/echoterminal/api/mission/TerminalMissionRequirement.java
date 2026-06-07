package com.knoxhack.echoterminal.api.mission;

import net.minecraft.world.item.ItemStack;

public record TerminalMissionRequirement(
        Kind kind,
        String label,
        String detail,
        ItemStack icon,
        int have,
        int need,
        boolean satisfied) {
    public TerminalMissionRequirement {
        kind = kind == null ? Kind.CUSTOM : kind;
        label = label == null ? "" : label;
        detail = detail == null ? "" : detail;
        icon = icon == null ? ItemStack.EMPTY : icon.copy();
        need = Math.max(0, need);
        have = Math.max(0, have);
    }

    public static TerminalMissionRequirement item(ItemStack stack, int have, int need, boolean satisfied) {
        ItemStack icon = stack == null ? ItemStack.EMPTY : stack.copy();
        String name = icon.isEmpty() ? "Item" : icon.getHoverName().getString();
        return new TerminalMissionRequirement(Kind.ITEM, name, have + "/" + need + " carried", icon, have, need, satisfied);
    }

    public static TerminalMissionRequirement block(String label, String detail, ItemStack icon, int have, int need, boolean satisfied) {
        return new TerminalMissionRequirement(Kind.BLOCK, label, detail, icon, have, need, satisfied);
    }

    public static TerminalMissionRequirement equipment(String label, String detail, ItemStack icon, boolean satisfied) {
        return new TerminalMissionRequirement(Kind.EQUIPMENT, label, detail, icon, satisfied ? 1 : 0, 1, satisfied);
    }

    public static TerminalMissionRequirement entity(String label, String detail, int have, int need, boolean satisfied) {
        return new TerminalMissionRequirement(Kind.ENTITY_KILL, label, detail, ItemStack.EMPTY, have, need, satisfied);
    }

    public static TerminalMissionRequirement location(String label, String detail, boolean satisfied) {
        return new TerminalMissionRequirement(Kind.LOCATION, label, detail, ItemStack.EMPTY, satisfied ? 1 : 0, 1, satisfied);
    }

    public static TerminalMissionRequirement custom(String label, String detail, ItemStack icon, int have, int need, boolean satisfied) {
        return new TerminalMissionRequirement(Kind.CUSTOM, label, detail, icon, have, need, satisfied);
    }

    public enum Kind {
        ITEM,
        BLOCK,
        EQUIPMENT,
        ENTITY_KILL,
        LOCATION,
        CUSTOM
    }
}
