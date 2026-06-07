package com.knoxhack.echonexusprotocol.compat.jei;

import com.knoxhack.echonexusprotocol.EchoNexusProtocol;
import com.knoxhack.echonexusprotocol.block.NexusMachineBlock;
import com.knoxhack.echonexusprotocol.client.NexusMachineScreen;
import com.knoxhack.echonexusprotocol.menu.NexusMachineMenu;
import com.knoxhack.echonexusprotocol.registry.ModBlocks;
import com.knoxhack.echonexusprotocol.registry.ModItems;
import com.knoxhack.echonexusprotocol.registry.ModMenus;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

@JeiPlugin
public class NexusJeiPlugin implements IModPlugin {
   private static final Identifier UID = Identifier.fromNamespaceAndPath(EchoNexusProtocol.MODID, "jei_plugin");

   public Identifier getPluginUid() {
      return UID;
   }

   public void registerCategories(IRecipeCategoryRegistration registration) {
      IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
      registration.addRecipeCategories(
         category(gui, NexusJeiRecipeTypes.RECYCLER, "Nexus Recycler", ModBlocks.NEXUS_RECYCLER.get().asItem()),
         category(gui, NexusJeiRecipeTypes.INFUSER, "Nexus Infuser", ModBlocks.NEXUS_INFUSER.get().asItem()),
         category(gui, NexusJeiRecipeTypes.MEMORY_DECODER, "Memory Decoder", ModBlocks.MEMORY_DECODER.get().asItem()),
         category(gui, NexusJeiRecipeTypes.REALITY_FORGE, "Reality Forge", ModBlocks.REALITY_FORGE.get().asItem()),
         category(gui, NexusJeiRecipeTypes.CORRUPTION_REACTOR, "Corruption Reactor", ModBlocks.CORRUPTION_REACTOR.get().asItem())
      );
   }

   public void registerRecipes(IRecipeRegistration registration) {
      registration.addRecipes(NexusJeiRecipeTypes.RECYCLER, NexusJeiRecipeCatalog.recipes(NexusMachineBlock.MachineKind.NEXUS_RECYCLER));
      registration.addRecipes(NexusJeiRecipeTypes.INFUSER, NexusJeiRecipeCatalog.recipes(NexusMachineBlock.MachineKind.NEXUS_INFUSER));
      registration.addRecipes(NexusJeiRecipeTypes.MEMORY_DECODER, NexusJeiRecipeCatalog.recipes(NexusMachineBlock.MachineKind.MEMORY_DECODER));
      registration.addRecipes(NexusJeiRecipeTypes.REALITY_FORGE, NexusJeiRecipeCatalog.recipes(NexusMachineBlock.MachineKind.REALITY_FORGE));
      registration.addRecipes(NexusJeiRecipeTypes.CORRUPTION_REACTOR, NexusJeiRecipeCatalog.recipes(NexusMachineBlock.MachineKind.CORRUPTION_REACTOR));
      info(registration, ModBlocks.NEXUS_CHARGE_TANK.get().asItem(), "Stores and relays stable Nexus Charge as FE-backed machine energy.");
      info(registration, ModBlocks.CORRUPTION_FILTER.get().asItem(), "Separates contamination from stored charge and leaks if allowed to clog.");
      info(registration, ModBlocks.NEXUS_FIELD_STABILIZER.get().asItem(), "Consumes Nexus Charge to raise local chunk field stability.");
      info(registration, ModBlocks.PROTOCOL_SEAL.get().asItem(), "Cycle modes in world: Collect, Extract, Repair, Quarantine, Purify, Relay, Defense, Rewrite, Collapse.");
      info(registration, ModBlocks.REALITY_TEAR.get().asItem(), "Nexus Field pressure below safe thresholds can open Reality Tears. Stabilize collapsed chunks from the outside edge first.");
      info(registration, ModItems.CORE_ACCESS_KEY.get(), "Unlocked through Stationfall/Nexus progression. Use to enter or return from the Nexus dimension. Return position is saved before entry.");
      info(registration, ModItems.CORE_KEY_ASSEMBLY.get(), "Final Core access item. The ending choice after Guardian defeat is permanent for the player path milestone.");
      info(registration, ModItems.PURITY_CHARGE.get(), "Cleans corrupted blocks, harms Nexus mobs, and briefly stabilizes local reality.");
      info(registration, ModItems.STABILIZED_PURITY_CHARGE.get(), "Collapsed-chunk recovery tool. Cleans a larger area, adds quarantine time, and restores more field stability.");
      info(registration, ModItems.COLLAPSE_CHARGE.get(), "Forbidden charge that worsens field stability to weaponize corruption.");
      info(registration, ModItems.FIELD_ANCHOR.get(), "Pins a damaged chunk for recovery. Deploy before running Stabilizers or Purify seals in collapsed areas.");
      info(registration, ModItems.NEXUS_SCANNER_VISOR.get(), "Scanner and Terminal Field tabs reveal field state, corruption pressure, storms, tears, and nearby chunk risk.");
   }

   public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
      registration.addCraftingStation(NexusJeiRecipeTypes.RECYCLER, ModBlocks.NEXUS_RECYCLER.get().asItem());
      registration.addCraftingStation(NexusJeiRecipeTypes.INFUSER, ModBlocks.NEXUS_INFUSER.get().asItem());
      registration.addCraftingStation(NexusJeiRecipeTypes.MEMORY_DECODER, ModBlocks.MEMORY_DECODER.get().asItem());
      registration.addCraftingStation(NexusJeiRecipeTypes.REALITY_FORGE, ModBlocks.REALITY_FORGE.get().asItem());
      registration.addCraftingStation(NexusJeiRecipeTypes.CORRUPTION_REACTOR, ModBlocks.CORRUPTION_REACTOR.get().asItem());
   }

   public void registerGuiHandlers(IGuiHandlerRegistration registration) {
      registration.addRecipeClickArea(NexusMachineScreen.class, 118, 96, 112, 8, NexusJeiRecipeTypes.ALL.toArray(IRecipeType[]::new));
   }

   public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
      for (IRecipeType<NexusJeiRecipe> type : NexusJeiRecipeTypes.ALL) {
         registration.addRecipeTransferHandler(NexusMachineMenu.class, ModMenus.NEXUS_MACHINE.get(), type, 0, 1, 2, 36);
      }
   }

   private static NexusJeiCategory category(IGuiHelper gui, IRecipeType<NexusJeiRecipe> type, String title, ItemLike icon) {
      return new NexusJeiCategory(type, Component.literal(title), gui.createDrawableItemLike(icon));
   }

   private static void info(IRecipeRegistration registration, ItemLike item, String text) {
      registration.addItemStackInfo(new ItemStack(item), Component.literal(text));
   }
}
