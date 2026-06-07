package com.knoxhack.echoarmory.registry;

import com.knoxhack.echoarmory.EchoArmory;
import java.util.EnumMap;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Util;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

public final class ModArmorMaterials {
   public static final ArmorMaterial VEIL_RESISTANT_HELM = material("veil_resistant_helm");
   public static final ArmorMaterial THERMAL_CHESTPLATE = material("thermal_chestplate");
   public static final ArmorMaterial DRONE_LEGGINGS = material("drone_leggings");
   public static final ArmorMaterial ORBITAL_BOOTS = material("orbital_boots");
   public static final ArmorMaterial CONSTRUCT_HARNESS = material("construct_harness");
   public static final ArmorMaterial SIGIL_AUGMENTED_SUIT = material("sigil_augmented_suit");

   private ModArmorMaterials() {
   }

   public static ArmorMaterial forArmor(String name) {
      return switch (name) {
         case "veil_resistant_helm" -> VEIL_RESISTANT_HELM;
         case "thermal_chestplate" -> THERMAL_CHESTPLATE;
         case "drone_leggings" -> DRONE_LEGGINGS;
         case "orbital_boots" -> ORBITAL_BOOTS;
         case "construct_harness" -> CONSTRUCT_HARNESS;
         case "sigil_augmented_suit" -> SIGIL_AUGMENTED_SUIT;
         default -> VEIL_RESISTANT_HELM;
      };
   }

   private static ArmorMaterial material(String assetName) {
      return new ArmorMaterial(
         33,
         Util.make(new EnumMap<>(ArmorType.class), map -> {
            map.put(ArmorType.BOOTS, 3);
            map.put(ArmorType.LEGGINGS, 6);
            map.put(ArmorType.CHESTPLATE, 8);
            map.put(ArmorType.HELMET, 3);
            map.put(ArmorType.BODY, 11);
         }),
         10,
         SoundEvents.ARMOR_EQUIP_DIAMOND,
         2.0F,
         0.0F,
         ItemTags.REPAIRS_DIAMOND_ARMOR,
         asset(assetName)
      );
   }

   private static ResourceKey<EquipmentAsset> asset(String name) {
      return ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(EchoArmory.MODID, name));
   }
}
