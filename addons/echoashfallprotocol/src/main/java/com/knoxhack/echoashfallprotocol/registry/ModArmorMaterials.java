package com.knoxhack.echoashfallprotocol.registry;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * Armor defense values as attribute modifier builders.
 * Uses the component-based system instead of ArmorMaterial registry.
 */
public class ModArmorMaterials {

    // === ALLOY ARMOR (Tier 2) - between Iron and Diamond ===
    public static ItemAttributeModifiers alloyHelmet() {
        return base(EquipmentSlotGroup.HEAD, 4.0, 1.5, "alloy_helmet");
    }
    public static ItemAttributeModifiers alloyChestplate() {
        return base(EquipmentSlotGroup.CHEST, 7.0, 1.5, "alloy_chestplate");
    }
    public static ItemAttributeModifiers alloyLeggings() {
        return base(EquipmentSlotGroup.LEGS, 5.0, 1.5, "alloy_leggings");
    }
    public static ItemAttributeModifiers alloyBoots() {
        return base(EquipmentSlotGroup.FEET, 2.0, 1.5, "alloy_boots");
    }

    // === NEXUS ARMOR (Tier 3) - endgame ===
    public static ItemAttributeModifiers nexusHelmet() {
        return withKnockback(EquipmentSlotGroup.HEAD, 5.0, 3.0, 0.05, "nexus_helmet");
    }
    public static ItemAttributeModifiers nexusChestplate() {
        return withKnockback(EquipmentSlotGroup.CHEST, 8.0, 3.0, 0.10, "nexus_chestplate");
    }
    public static ItemAttributeModifiers nexusLeggings() {
        return withKnockback(EquipmentSlotGroup.LEGS, 6.0, 3.0, 0.08, "nexus_leggings");
    }
    public static ItemAttributeModifiers nexusBoots() {
        return withKnockback(EquipmentSlotGroup.FEET, 3.0, 3.0, 0.05, "nexus_boots");
    }

    private static ItemAttributeModifiers base(EquipmentSlotGroup slot, double defense, double toughness, String key) {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ARMOR,
                        new AttributeModifier(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, key + "_armor"),
                                defense, AttributeModifier.Operation.ADD_VALUE), slot)
                .add(Attributes.ARMOR_TOUGHNESS,
                        new AttributeModifier(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, key + "_toughness"),
                                toughness, AttributeModifier.Operation.ADD_VALUE), slot)
                .build();
    }

    private static ItemAttributeModifiers withKnockback(EquipmentSlotGroup slot, double defense, double toughness, double knockbackResist, String key) {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ARMOR,
                        new AttributeModifier(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, key + "_armor"),
                                defense, AttributeModifier.Operation.ADD_VALUE), slot)
                .add(Attributes.ARMOR_TOUGHNESS,
                        new AttributeModifier(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, key + "_toughness"),
                                toughness, AttributeModifier.Operation.ADD_VALUE), slot)
                .add(Attributes.KNOCKBACK_RESISTANCE,
                        new AttributeModifier(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, key + "_kb_resist"),
                                knockbackResist, AttributeModifier.Operation.ADD_VALUE), slot)
                .build();
    }
}
