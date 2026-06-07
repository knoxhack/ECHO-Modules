package com.knoxhack.echomultiblockcore.api;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public record StructureBlockRequirement(
        SlotKind kind,
        Identifier block,
        Identifier tag,
        List<Identifier> blocks,
        boolean optional,
        String label) {
    public StructureBlockRequirement {
        kind = kind == null ? SlotKind.WILDCARD : kind;
        blocks = List.copyOf(blocks == null ? List.of() : blocks);
        label = label == null || label.isBlank() ? kind.name().toLowerCase(java.util.Locale.ROOT) : label.strip();
    }

    public static StructureBlockRequirement exact(Identifier block) {
        return new StructureBlockRequirement(SlotKind.EXACT_BLOCK, block, null, List.of(), false, "");
    }

    public static StructureBlockRequirement tag(Identifier tag) {
        return new StructureBlockRequirement(SlotKind.BLOCK_TAG, null, tag, List.of(), false, "");
    }

    public static StructureBlockRequirement air() {
        return new StructureBlockRequirement(SlotKind.AIR, null, null, List.of(), false, "Air");
    }

    public static StructureBlockRequirement wildcard() {
        return new StructureBlockRequirement(SlotKind.WILDCARD, null, null, List.of(), true, "Any block");
    }

    public StructureBlockRequirement asOptional() {
        return new StructureBlockRequirement(kind, block, tag, blocks, true, label);
    }

    public boolean required() {
        return !optional && kind != SlotKind.WILDCARD;
    }

    public boolean matches(Level level, BlockState state) {
        if (state == null) {
            return false;
        }
        return switch (kind) {
            case AIR -> state.isAir();
            case WILDCARD -> true;
            case EXACT_BLOCK, CONTROLLER, COMPONENT, ROBOTICS, UPGRADE -> block != null
                    && BuiltInRegistries.BLOCK.getOptional(block).filter(state::is).isPresent();
            case BLOCK_TAG -> tag != null && state.is(TagKey.create(Registries.BLOCK, tag));
            case BLOCK_LIST -> blocks.stream()
                    .anyMatch(id -> BuiltInRegistries.BLOCK.getOptional(id).filter(state::is).isPresent());
        };
    }

    public String expectedName() {
        return switch (kind) {
            case AIR -> "Air";
            case WILDCARD -> "Any block";
            case BLOCK_TAG -> "#" + (tag == null ? "unknown" : tag);
            case BLOCK_LIST -> blocks.toString();
            default -> block == null ? label : block.toString();
        };
    }

    public enum SlotKind {
        EXACT_BLOCK,
        BLOCK_TAG,
        BLOCK_LIST,
        AIR,
        WILDCARD,
        UPGRADE,
        CONTROLLER,
        COMPONENT,
        ROBOTICS
    }
}
