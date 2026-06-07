package com.knoxhack.echoconvoyprotocol.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoconvoyprotocol.EchoConvoyProtocol;
import com.knoxhack.echoconvoyprotocol.recipe.ConvoyStationRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

public final class ModRecipes {
   private static final Object RECIPE_TYPES = EchoBackendRegistryBridge.create(Registries.RECIPE_TYPE, EchoConvoyProtocol.MODID);
   private static final Object RECIPE_SERIALIZERS = EchoBackendRegistryBridge.create(Registries.RECIPE_SERIALIZER, EchoConvoyProtocol.MODID);

   public static final EchoBackendRegistryEntry<RecipeType<ConvoyStationRecipe>> CONVOY_STATION_PROCESSING_TYPE =
      EchoBackendRegistryBridge.register(RECIPE_TYPES, "convoy_station_processing",
         () -> RecipeType.simple(Identifier.fromNamespaceAndPath(EchoConvoyProtocol.MODID, "convoy_station_processing")));

   public static final EchoBackendRegistryEntry<RecipeSerializer<ConvoyStationRecipe>> CONVOY_STATION_PROCESSING_SERIALIZER =
      EchoBackendRegistryBridge.register(RECIPE_SERIALIZERS, "convoy_station_processing",
         () -> new RecipeSerializer<>(ConvoyStationRecipe.CODEC, ConvoyStationRecipe.STREAM_CODEC));

   private ModRecipes() {
   }

   public static void register(Object eventBus) {
      EchoBackendRegistryBridge.registerEventBus(RECIPE_TYPES, eventBus);
      EchoBackendRegistryBridge.registerEventBus(RECIPE_SERIALIZERS, eventBus);
   }
}
