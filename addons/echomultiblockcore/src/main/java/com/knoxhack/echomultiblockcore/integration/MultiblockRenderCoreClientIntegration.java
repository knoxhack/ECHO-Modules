package com.knoxhack.echomultiblockcore.integration;

import com.knoxhack.echo.adaptercore.EchoBackendClientBridge;
import com.knoxhack.echomultiblockcore.EchoMultiblockCore;
import com.knoxhack.echomultiblockcore.api.MultiblockState;
import com.knoxhack.echomultiblockcore.api.RobotState;
import com.knoxhack.echomultiblockcore.block.entity.MultiblockControllerBlockEntity;
import com.knoxhack.echomultiblockcore.block.entity.RoboticArmBlockEntity;
import com.knoxhack.echomultiblockcore.registry.ModBlockEntities;
import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class MultiblockRenderCoreClientIntegration {
   private static final Identifier CONTROLLER_PROFILE = Identifier.fromNamespaceAndPath(EchoMultiblockCore.MODID, "multiblock_controller");
   private static final Identifier ROBOTIC_ARM_PROFILE = Identifier.fromNamespaceAndPath(EchoMultiblockCore.MODID, "robotic_arm");
   private static Class<?> rendererType;
   private static Constructor<?> rendererConstructor;
   private static Class<?> factoryType;
   private static Class<?> hostType;
   private static Class<? extends Enum> visualStateType;

   private MultiblockRenderCoreClientIntegration() {
   }

   public static void registerBlockRenderers(Object event) {
      if (!resolveRenderCoreTypes()) {
         return;
      }
      BlockEntityRendererProvider<MultiblockControllerBlockEntity, BlockEntityRenderState> controllerProvider =
         context -> renderer(context, MultiblockRenderCoreClientIntegration::controllerHost);
      BlockEntityRendererProvider<RoboticArmBlockEntity, BlockEntityRenderState> robotProvider =
         context -> renderer(context, MultiblockRenderCoreClientIntegration::robotHost);
      EchoBackendClientBridge.registerBlockEntityRenderer(event, ModBlockEntities.CONTROLLER.get(), controllerProvider);
      EchoBackendClientBridge.registerBlockEntityRenderer(event, ModBlockEntities.ROBOTIC_ARM.get(), robotProvider);
   }

   private static Object controllerHost(MultiblockControllerBlockEntity controller, float partialTick) {
      return visualHost(
         CONTROLLER_PROFILE,
         () -> {
            MultiblockState state = controller.getState();
            return switch (state) {
               case FORMED, ACTIVE -> "ACTIVE";
               case VALIDATING -> "SCANNING";
               case DAMAGED, JAMMED, OVERLOADED -> "DAMAGED";
               case OFFLINE -> "OFFLINE";
               default -> "IDLE";
            };
         },
         () -> Math.max(0.0F, Math.min(1.0F, controller.getIntegrity() / 100.0F)),
         () -> false,
         () -> controller.getState() == MultiblockState.DAMAGED
            || controller.getState() == MultiblockState.JAMMED
            || controller.getState() == MultiblockState.OVERLOADED);
   }

   private static Object robotHost(RoboticArmBlockEntity arm, float partialTick) {
      return visualHost(
         ROBOTIC_ARM_PROFILE,
         () -> {
            RobotState state = arm.getRobotState();
            return switch (state) {
               case WORKING -> "WORKING";
               case MOVING -> "ACTIVE";
               case COOLING -> "OVERHEATED";
               case JAMMED, DAMAGED -> "DAMAGED";
               case OFFLINE -> "OFFLINE";
               default -> "IDLE";
            };
         },
         () -> Math.max(0.0F, Math.min(1.0F, arm.getHeat() / (float)arm.getMaxHeat())),
         () -> arm.getRobotState() == RobotState.MOVING || arm.getRobotState() == RobotState.WORKING,
         () -> arm.getRobotState() == RobotState.JAMMED || arm.getRobotState() == RobotState.DAMAGED);
   }

   @SuppressWarnings("unchecked")
   private static <T extends BlockEntity> BlockEntityRenderer<T, BlockEntityRenderState> renderer(BlockEntityRendererProvider.Context context,
         HostFactory<T> hostFactory) {
      try {
         Object factory = Proxy.newProxyInstance(factoryType.getClassLoader(), new Class<?>[] { factoryType },
            (proxy, method, args) -> {
               if ("create".equals(method.getName()) && args != null && args.length == 2) {
                  return hostFactory.create((T)args[0], args[1] instanceof Float value ? value : 0.0F);
               }
               return defaultValue(method.getReturnType(), null);
            });
         return (BlockEntityRenderer<T, BlockEntityRenderState>)rendererConstructor.newInstance(context, factory);
      } catch (ReflectiveOperationException | LinkageError exception) {
         throw new IllegalStateException("RenderCore block renderer bridge failed.", exception);
      }
   }

   private static Object visualHost(Identifier profileId, StateSupplier state, DoubleSupplier progress,
         BooleanSupplier moving, BooleanSupplier damaged) {
      return Proxy.newProxyInstance(hostType.getClassLoader(), new Class<?>[] { hostType },
         (proxy, method, args) -> switch (method.getName()) {
            case "toString" -> "MultiblockRenderCoreVisualHost[" + profileId + "]";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == (args == null || args.length == 0 ? null : args[0]);
            case "visualProfileId" -> profileId;
            case "visualState" -> visualState(state.get());
            case "visualProgress" -> (float)progress.getAsDouble();
            case "visualMoving" -> moving.getAsBoolean();
            case "visualDamaged" -> damaged.getAsBoolean();
            case "visualSurfaceType" -> "block_entity";
            case "visualFallbackStatus" -> "rendercore_native";
            case "visualDebugTarget" -> profileId == null ? "rendercore:block" : profileId.toString();
            case "visualAnchors" -> Map.of();
            case "visualNamedParts" -> List.of();
            case "visualDependencies" -> List.of();
            default -> defaultValue(method.getReturnType(), profileId);
         });
   }

   private static boolean resolveRenderCoreTypes() {
      if (rendererConstructor != null && factoryType != null && hostType != null && visualStateType != null) {
         return true;
      }
      try {
         rendererType = Class.forName("com.knoxhack.echorendercore.client.RenderCoreBlockEntityRenderer");
         factoryType = Class.forName("com.knoxhack.echorendercore.client.RenderCoreBlockEntityRenderer$VisualHostFactory");
         hostType = Class.forName("com.knoxhack.echorendercore.api.RenderCoreBlockVisualHost");
         visualStateType = Class.forName("com.knoxhack.echorendercore.api.VisualState").asSubclass(Enum.class);
         for (Constructor<?> constructor : rendererType.getConstructors()) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            if (parameterTypes.length == 2
               && BlockEntityRendererProvider.Context.class.isAssignableFrom(parameterTypes[0])
               && factoryType.isAssignableFrom(parameterTypes[1])) {
               rendererConstructor = constructor;
               return true;
            }
         }
         EchoMultiblockCore.LOGGER.warn("ECHO MultiblockCore could not find RenderCore block renderer constructor.");
         return false;
      } catch (ReflectiveOperationException | LinkageError exception) {
         EchoMultiblockCore.LOGGER.warn("ECHO MultiblockCore RenderCore block integration is unavailable.", exception);
         return false;
      }
   }

   @SuppressWarnings({ "unchecked", "rawtypes" })
   private static Object visualState(String name) {
      try {
         return Enum.valueOf((Class)visualStateType, name == null || name.isBlank() ? "IDLE" : name);
      } catch (IllegalArgumentException exception) {
         return Enum.valueOf((Class)visualStateType, "IDLE");
      }
   }

   private static Object defaultValue(Class<?> returnType, Identifier profileId) {
      if (returnType == Void.TYPE) {
         return null;
      }
      if (returnType == Boolean.TYPE) {
         return false;
      }
      if (returnType == Float.TYPE) {
         return 0.0F;
      }
      if (returnType == Double.TYPE) {
         return 0.0D;
      }
      if (returnType == Integer.TYPE) {
         return 0;
      }
      if (returnType == Long.TYPE) {
         return 0L;
      }
      if (returnType == Identifier.class) {
         return null;
      }
      if (returnType == String.class) {
         return profileId == null ? "" : profileId.toString();
      }
      if (returnType == List.class) {
         return List.of();
      }
      if (returnType == Map.class) {
         return Map.of();
      }
      if (returnType.isEnum()) {
         Object[] constants = returnType.getEnumConstants();
         return constants.length == 0 ? null : constants[0];
      }
      return null;
   }

   @FunctionalInterface
   private interface HostFactory<T extends BlockEntity> {
      Object create(T blockEntity, float partialTick);
   }

   @FunctionalInterface
   private interface StateSupplier {
      String get();
   }
}
