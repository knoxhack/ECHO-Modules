package com.knoxhack.echoindustrialnexus.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoindustrialnexus.EchoIndustrialNexus;
import com.knoxhack.echoindustrialnexus.recipe.IndustrialProcessingRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

public final class ModRecipes {
   private static final Object RECIPE_TYPES = EchoBackendRegistryBridge.create(Registries.RECIPE_TYPE, EchoIndustrialNexus.MODID);
   private static final Object RECIPE_SERIALIZERS = EchoBackendRegistryBridge.create(Registries.RECIPE_SERIALIZER, EchoIndustrialNexus.MODID);
   public static final EchoBackendRegistryEntry<RecipeType<IndustrialProcessingRecipe>> INDUSTRIAL_PROCESSING_TYPE = EchoBackendRegistryBridge.register(
      RECIPE_TYPES, "industrial_processing", () -> RecipeType.simple(Identifier.fromNamespaceAndPath(EchoIndustrialNexus.MODID, "industrial_processing"))
   );
   public static final EchoBackendRegistryEntry<RecipeSerializer<IndustrialProcessingRecipe>> INDUSTRIAL_PROCESSING_SERIALIZER = EchoBackendRegistryBridge.register(
      RECIPE_SERIALIZERS, "industrial_processing", () -> new RecipeSerializer(IndustrialProcessingRecipe.CODEC, IndustrialProcessingRecipe.STREAM_CODEC)
   );

   private ModRecipes() {
   }

   public static void register(Object eventBus) {
      EchoBackendRegistryBridge.registerEventBus(RECIPE_TYPES, eventBus);
      EchoBackendRegistryBridge.registerEventBus(RECIPE_SERIALIZERS, eventBus);
   }
}
