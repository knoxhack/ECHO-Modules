package com.knoxhack.echonexusprotocol.compat.jei;

import com.knoxhack.echonexusprotocol.EchoNexusProtocol;
import com.knoxhack.echonexusprotocol.block.NexusMachineBlock;
import com.knoxhack.echonexusprotocol.registry.ModBlocks;
import com.knoxhack.echonexusprotocol.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

public final class NexusJeiRecipeCatalog {
   private NexusJeiRecipeCatalog() {
   }

   public static List<NexusJeiRecipe> recipes(NexusMachineBlock.MachineKind kind) {
      return all().stream().filter(recipe -> recipe.machine() == kind).toList();
   }

   public static List<NexusJeiRecipe> all() {
      List<NexusJeiRecipe> recipes = new ArrayList<>();
      recipes.add(recipe("recycler_nexus_shard", NexusMachineBlock.MachineKind.NEXUS_RECYCLER, ModItems.NEXUS_SHARD.get(), ModItems.REALITY_DUST.get(), 120, 0, 700, 1, 0));
      recipes.add(recipe("recycler_iron_ingot", NexusMachineBlock.MachineKind.NEXUS_RECYCLER, Items.IRON_INGOT, ModItems.CORRUPTED_FERRITE.get(), 130, 0, 160, 2, 0));
      recipes.add(recipe("recycler_corrupted_ferrite", NexusMachineBlock.MachineKind.NEXUS_RECYCLER, ModItems.CORRUPTED_FERRITE.get(), ModItems.REALITY_DUST.get(), 150, 0, 500, 6, 0));
      recipes.add(recipe("recycler_rotten_flesh", NexusMachineBlock.MachineKind.NEXUS_RECYCLER, Items.ROTTEN_FLESH, ModItems.NEXUS_GEL.get(), 110, 0, 90, 4, 0));
      recipes.add(recipe("recycler_static_fluid", NexusMachineBlock.MachineKind.NEXUS_RECYCLER, ModItems.STATIC_FLUID.get(), ModItems.NEXUS_GEL.get(), 120, 0, 350, 14, -2));
      recipes.add(recipe("recycler_redstone", NexusMachineBlock.MachineKind.NEXUS_RECYCLER, Items.REDSTONE, ItemStack.EMPTY, 90, 0, 120, 0, 0));
      recipes.add(recipe("infuser_stable_nexus_core", NexusMachineBlock.MachineKind.NEXUS_INFUSER, ModItems.NEXUS_SHARD.get(), ModItems.STABLE_NEXUS_CORE.get(), 180, 500, 0, 0, -1));
      recipes.add(recipe("infuser_nexus_pickaxe", NexusMachineBlock.MachineKind.NEXUS_INFUSER, Items.DIAMOND_PICKAXE, ModItems.NEXUS_PICKAXE.get(), 220, 450, 0, 0, -1));
      recipes.add(recipe("infuser_signal_blade", NexusMachineBlock.MachineKind.NEXUS_INFUSER, Items.DIAMOND_SWORD, ModItems.SIGNAL_BLADE.get(), 220, 420, 0, 0, -1));
      recipes.add(recipe("infuser_reality_anchor", NexusMachineBlock.MachineKind.NEXUS_INFUSER, Items.ENDER_PEARL, ModItems.REALITY_ANCHOR.get(), 180, 360, 0, 0, 0));
      recipes.add(recipe("infuser_core_access_key", NexusMachineBlock.MachineKind.NEXUS_INFUSER, ModItems.BLACKBOX_FRAGMENT.get(), ModItems.CORE_ACCESS_KEY.get(), 240, 600, 0, 0, -2));
      recipes.add(recipe("memory_decoder_data_fragment", NexusMachineBlock.MachineKind.MEMORY_DECODER, ModItems.DATA_FRAGMENT.get(), ModItems.MEMORY_SHARD.get(), 120, 80, 0, 0, 0));
      recipes.add(recipe("memory_decoder_blackbox_fragment", NexusMachineBlock.MachineKind.MEMORY_DECODER, ModItems.BLACKBOX_FRAGMENT.get(), stack(ModItems.MEMORY_SHARD.get(), 2), 160, 150, 0, 0, 0));
      recipes.add(recipe("reality_forge_blackbox_plate", NexusMachineBlock.MachineKind.REALITY_FORGE, ModBlocks.DATA_CRACKED_STONE.get().asItem(), ModBlocks.BLACKBOX_PLATE.get().asItem(), 200, 260, 0, 0, -1));
      recipes.add(recipe("reality_forge_clean_stone", NexusMachineBlock.MachineKind.REALITY_FORGE, ModBlocks.DATA_CRACKED_STONE.get().asItem(), Items.STONE, 120, 180, 0, 0, 1));
      recipes.add(recipe("reality_forge_core_glass", NexusMachineBlock.MachineKind.REALITY_FORGE, ModItems.CORE_GLASS.get(), ModBlocks.CORE_GLASS_BLOCK.get().asItem(), 120, 120, 0, 0, 0));
      recipes.add(recipe("reality_forge_core_key_assembly", NexusMachineBlock.MachineKind.REALITY_FORGE, ModItems.CORE_ACCESS_KEY.get(), ModItems.CORE_KEY_ASSEMBLY.get(), 260, 900, 0, 0, -3));
      recipes.add(recipe("corruption_reactor_static_fuel", NexusMachineBlock.MachineKind.CORRUPTION_REACTOR, ModItems.STATIC_FLUID.get(), ItemStack.EMPTY, 80, 0, 900, 8, -4));
      return List.copyOf(recipes);
   }

   private static NexusJeiRecipe recipe(String path, NexusMachineBlock.MachineKind kind, ItemLike input, ItemLike output, int duration, int chargeCost, int chargeOutput, int corruptionDelta, int fieldDelta) {
      return recipe(path, kind, input, output == null ? ItemStack.EMPTY : new ItemStack(output), duration, chargeCost, chargeOutput, corruptionDelta, fieldDelta);
   }

   private static NexusJeiRecipe recipe(String path, NexusMachineBlock.MachineKind kind, ItemLike input, ItemStack output, int duration, int chargeCost, int chargeOutput, int corruptionDelta, int fieldDelta) {
      List<ItemStack> outputs = output.isEmpty() ? List.of() : List.of(output);
      return new NexusJeiRecipe(id(path), kind, List.of(new ItemStack(input)), outputs, duration, chargeCost, chargeOutput, corruptionDelta, fieldDelta);
   }

   private static ItemStack stack(ItemLike item, int count) {
      return new ItemStack(item, count);
   }

   private static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath(EchoNexusProtocol.MODID, path);
   }
}
