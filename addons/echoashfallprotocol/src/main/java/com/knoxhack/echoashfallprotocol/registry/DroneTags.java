package com.knoxhack.echoashfallprotocol.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class DroneTags {
    public static final TagKey<Block> SCAN_CONTAINERS = block("drone_scan_containers");
    public static final TagKey<Block> SCAN_RESOURCES = block("drone_scan_resources");
    public static final TagKey<Block> SCAN_HAZARDS = block("drone_scan_hazards");
    public static final TagKey<Block> SCAN_OBJECTIVES = block("drone_scan_objectives");
    public static final TagKey<Block> IGNORE_BLOCKS = block("drone_ignore_blocks");

    public static final TagKey<Item> SALVAGE_ITEMS = item("drone_salvage_items");
    public static final TagKey<Item> IGNORE_ITEMS = item("drone_ignore_items");
    public static final TagKey<Item> UPGRADE_ITEMS = item("drone_upgrade_items");

    public static final TagKey<EntityType<?>> HOSTILE_PRIORITY = entity("drone_hostile_priority");
    public static final TagKey<EntityType<?>> IGNORE_ENTITIES = entity("drone_ignore_entities");
    public static final TagKey<EntityType<?>> SCAN_INTEREST = entity("drone_scan_interest");

    private DroneTags() {
    }

    private static TagKey<Block> block(String path) {
        return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("echo", path));
    }

    private static TagKey<Item> item(String path) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("echo", path));
    }

    private static TagKey<EntityType<?>> entity(String path) {
        return TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("echo", path));
    }
}
