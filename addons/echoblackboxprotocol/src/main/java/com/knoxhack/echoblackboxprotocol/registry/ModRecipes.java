package com.knoxhack.echoblackboxprotocol.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoblackboxprotocol.EchoBlackboxProtocol;
import com.knoxhack.echoblackboxprotocol.recipe.BlackboxProcessingRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

public final class ModRecipes {
   private static final Object RECIPE_TYPES = EchoBackendRegistryBridge.create(Registries.RECIPE_TYPE, EchoBlackboxProtocol.MODID);
   private static final Object RECIPE_SERIALIZERS = EchoBackendRegistryBridge.create(Registries.RECIPE_SERIALIZER, EchoBlackboxProtocol.MODID);
   public static final EchoBackendRegistryEntry<RecipeType<BlackboxProcessingRecipe>> BLACKBOX_PROCESSING_TYPE = EchoBackendRegistryBridge.register(RECIPE_TYPES, 
      "blackbox_processing", () -> RecipeType.simple(Identifier.fromNamespaceAndPath(EchoBlackboxProtocol.MODID, "blackbox_processing"))
   );
   public static final EchoBackendRegistryEntry<RecipeSerializer<BlackboxProcessingRecipe>> BLACKBOX_PROCESSING_SERIALIZER = EchoBackendRegistryBridge.register(RECIPE_SERIALIZERS, 
      "blackbox_processing", () -> new RecipeSerializer(BlackboxProcessingRecipe.CODEC, BlackboxProcessingRecipe.STREAM_CODEC)
   );

   private ModRecipes() {
   }

   public static void register(Object eventBus) {
      EchoBackendRegistryBridge.registerEventBus(RECIPE_TYPES, eventBus);
      EchoBackendRegistryBridge.registerEventBus(RECIPE_SERIALIZERS, eventBus);
   }
}
