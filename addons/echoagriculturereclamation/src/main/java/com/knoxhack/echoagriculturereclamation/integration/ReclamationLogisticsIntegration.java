package com.knoxhack.echoagriculturereclamation.integration;

import com.knoxhack.echoagriculturereclamation.EchoAgricultureReclamation;
import com.knoxhack.echoagriculturereclamation.registry.ModBlocks;
import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public final class ReclamationLogisticsIntegration {
   private static final Identifier PROVIDER_ID = id("field_supply_targets");
   private static final Identifier NUTRIENTS = id("nutrients");
   private static final Identifier SEED_STOCK = id("seed_stock");
   private static final Identifier FOOD = Identifier.fromNamespaceAndPath("echologisticsnetwork", "food");
   private static final Identifier MACHINE_PARTS = Identifier.fromNamespaceAndPath("echologisticsnetwork", "machine_parts");

   private ReclamationLogisticsIntegration() {
   }

   public static void register() {
      try {
         Class<?> providerType = Class.forName("com.knoxhack.echologisticsnetwork.api.LogisticsExternalEndpointProvider");
         Object provider = Proxy.newProxyInstance(
            ReclamationLogisticsIntegration.class.getClassLoader(),
            new Class<?>[] {providerType},
            (proxy, method, args) -> switch (method.getName()) {
               case "providerId" -> PROVIDER_ID;
               case "endpoints" -> endpoints((Level)args[0], (BlockPos)args[1], (String)args[2]);
               case "toString" -> "Agriculture Reclamation logistics endpoint provider";
               case "hashCode" -> System.identityHashCode(proxy);
               case "equals" -> proxy == args[0];
               default -> null;
            });
         Class<?> service = Class.forName("com.knoxhack.echologisticsnetwork.service.LogisticsNetworkService");
         service.getMethod("registerExternalEndpointProvider", providerType).invoke(null, provider);
      } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
         EchoAgricultureReclamation.LOGGER.warn("ECHO Agriculture Reclamation logistics endpoint provider could not be registered.", exception);
      }
   }

   private static List<Object> endpoints(Level level, BlockPos origin, String networkId) {
      if (level == null || origin == null) {
         return List.of();
      }
      List<Object> endpoints = new ArrayList<>();
      for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-10, -4, -10), origin.offset(10, 4, 10))) {
         Block block = level.getBlockState(pos).getBlock();
         Identifier category = categoryFor(block);
         if (category != null) {
            Object endpoint = endpoint(pos, networkId, category);
            if (endpoint != null) {
               endpoints.add(endpoint);
            }
            if (endpoints.size() >= 48) {
               break;
            }
         }
      }
      return endpoints;
   }

   private static Object endpoint(BlockPos pos, String networkId, Identifier category) {
      try {
         Class<?> endpointType = Class.forName("com.knoxhack.echologisticsnetwork.api.LogisticsExternalEndpoint");
         Constructor<?> constructor = endpointType.getConstructor(BlockPos.class, String.class, Identifier.class, Identifier.class, Set.class);
         return constructor.newInstance(pos, networkId, category, null, logisticsRoles());
      } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
         return null;
      }
   }

   private static Set<Object> logisticsRoles() {
      try {
         Class<?> roleType = Class.forName("com.knoxhack.echologisticsnetwork.api.LogisticsExternalEndpointRole");
         return Set.of(
            Enum.valueOf(roleType.asSubclass(Enum.class), "STORAGE"),
            Enum.valueOf(roleType.asSubclass(Enum.class), "REQUEST_TARGET"),
            Enum.valueOf(roleType.asSubclass(Enum.class), "DELIVERY_TARGET"));
      } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
         return Set.of();
      }
   }

   private static Identifier categoryFor(Block block) {
      if (block == ModBlocks.HYDROPONIC_TRAY.get()
         || block == ModBlocks.SOIL_PURIFIER.get()
         || block == ModBlocks.COMPOST_RECYCLER.get()) {
         return NUTRIENTS;
      }
      if (block == ModBlocks.SEED_VAULT_TERMINAL.get()
         || block == ModBlocks.GENE_STABILIZER.get()) {
         return SEED_STOCK;
      }
      if (block == ModBlocks.BIO_REACTOR.get()
         || block == ModBlocks.GREENHOUSE_CONTROLLER.get()
         || block == ModBlocks.POLLINATOR_DRONE_DOCK.get()
         || block == ModBlocks.ECOLOGY_SCANNER.get()) {
         return MACHINE_PARTS;
      }
      if (ModBlocks.cropBlocks().stream().anyMatch(crop -> crop.get() == block)) {
         return FOOD;
      }
      return null;
   }

   private static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath(EchoAgricultureReclamation.MODID, path);
   }
}
