package com.knoxhack.echo.adaptercore;

import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Proxy;
import java.util.function.Supplier;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.GuiLayer;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * AdapterCore backend bridge for client-only registration events.
 */
public final class EchoBackendClientBridge {
    private EchoBackendClientBridge() {
    }

    public static boolean registerEntityRenderer(Object event, Object entityType, Object provider) {
        return invokeTwoArgumentRegistration(event, "registerEntityRenderer", entityType, provider);
    }

    public static <T extends Entity> boolean registerEntityRenderer(Object event, EntityType<? extends T> entityType,
            EntityRendererProvider<T> provider) {
        return registerEntityRenderer(event, entityType, (Object) provider);
    }

    public static boolean registerBlockEntityRenderer(Object event, Object blockEntityType, Object provider) {
        return invokeTwoArgumentRegistration(event, "registerBlockEntityRenderer", blockEntityType, provider);
    }

    public static <T extends BlockEntity, S extends BlockEntityRenderState> boolean registerBlockEntityRenderer(
            Object event, BlockEntityType<? extends T> blockEntityType, BlockEntityRendererProvider<T, S> provider) {
        return registerBlockEntityRenderer(event, blockEntityType, (Object) provider);
    }

    public static boolean registerLayerDefinition(Object event, Object layerLocation, Supplier<?> definitionSupplier) {
        return invokeTwoArgumentRegistration(event, "registerLayerDefinition", layerLocation, definitionSupplier);
    }

    public static boolean addClientReloadListener(Object event, Identifier id, PreparableReloadListener listener) {
        if (event instanceof AddClientReloadListenersEvent reload && id != null && listener != null) {
            reload.addListener(id, listener);
            return true;
        }
        return false;
    }

    public static boolean registerTooltipComponentFactory(Object event, Class<?> dataClass, Class<?> componentClass) {
        if (!(event instanceof RegisterClientTooltipComponentFactoriesEvent tooltipEvent)
                || dataClass == null || componentClass == null) {
            return false;
        }
        try {
            java.util.function.Function<TooltipComponent, ClientTooltipComponent> factory = data -> {
                try {
                    for (var constructor : componentClass.getConstructors()) {
                        if (constructor.getParameterCount() == 1) {
                            return (ClientTooltipComponent) constructor.newInstance(data);
                        }
                    }
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                }
                return null;
            };
            tooltipEvent.register((Class) dataClass, factory);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public static boolean registerGuiLayerAboveAir(Object event, Identifier id, GuiLayer layer) {
        if (event instanceof RegisterGuiLayersEvent guiLayers && id != null && layer != null) {
            guiLayers.registerAbove(VanillaGuiLayers.AIR_LEVEL, id, layer);
            return true;
        }
        return false;
    }

    public static <T extends CustomPacketPayload> boolean registerClientPayloadHandler(
            Object event,
            CustomPacketPayload.Type<T> type,
            IPayloadHandler<T> handler) {
        if (event instanceof RegisterClientPayloadHandlersEvent payloadHandlers && type != null && handler != null) {
            payloadHandlers.register(type, handler);
            return true;
        }
        return false;
    }

    public static boolean registerMenuScreen(Object event, Object menuType, Class<?> screenClass) {
        if (event == null || menuType == null || screenClass == null) {
            return false;
        }
        try {
            for (var method : event.getClass().getMethods()) {
                if (!"register".equals(method.getName()) || method.getParameterCount() != 2) {
                    continue;
                }
                var constructorType = method.getParameterTypes()[1];
                var constructor = Proxy.newProxyInstance(
                        constructorType.getClassLoader(),
                        new Class<?>[] { constructorType },
                        (proxy, invoked, args) -> {
                            if (invoked.getDeclaringClass() == Object.class) {
                                return switch (invoked.getName()) {
                                    case "toString" -> "EchoScreenConstructor[" + screenClass.getName() + "]";
                                    case "hashCode" -> System.identityHashCode(proxy);
                                    case "equals" -> proxy == (args == null ? null : args[0]);
                                    default -> null;
                                };
                            }
                            for (var constructorCandidate : screenClass.getConstructors()) {
                                if (constructorCandidate.getParameterCount() == 3) {
                                    return constructorCandidate.newInstance(args);
                                }
                            }
                            throw new IllegalStateException("No three-argument screen constructor for "
                                    + screenClass.getName());
                        });
                method.invoke(event, menuType, constructor);
                return true;
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return false;
        }
        return false;
    }

    public static boolean registerMenuScreenFactory(Object event, Object menuType, Class<?> factoryClass,
            String methodName) {
        if (event == null || menuType == null || factoryClass == null || methodName == null || methodName.isBlank()) {
            return false;
        }
        try {
            for (var method : event.getClass().getMethods()) {
                if (!"register".equals(method.getName()) || method.getParameterCount() != 2) {
                    continue;
                }
                var constructorType = method.getParameterTypes()[1];
                var constructor = Proxy.newProxyInstance(
                        constructorType.getClassLoader(),
                        new Class<?>[] { constructorType },
                        (proxy, invoked, args) -> {
                            if (invoked.getDeclaringClass() == Object.class) {
                                return switch (invoked.getName()) {
                                    case "toString" -> "EchoScreenFactory[" + factoryClass.getName() + "#"
                                            + methodName + "]";
                                    case "hashCode" -> System.identityHashCode(proxy);
                                    case "equals" -> proxy == (args == null ? null : args[0]);
                                    default -> null;
                                };
                            }
                            for (var candidate : factoryClass.getMethods()) {
                                if (candidate.getName().equals(methodName) && candidate.getParameterCount() == 3) {
                                    return candidate.invoke(null, args);
                                }
                            }
                            throw new IllegalStateException("No three-argument screen factory "
                                    + factoryClass.getName() + "#" + methodName);
                        });
                method.invoke(event, menuType, constructor);
                return true;
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return false;
        }
        return false;
    }

    public static boolean registerKeyCategory(Object event, Object category) {
        return invokeOneArgumentRegistration(event, "registerCategory", category);
    }

    public static boolean registerKeyMapping(Object event, KeyMapping mapping) {
        return invokeOneArgumentRegistration(event, "register", mapping);
    }

    public static boolean keyActionEquals(Object event, int action) {
        Object actual = invokeNoArgumentMethod(event, "getAction");
        return actual instanceof Number value && value.intValue() == action;
    }

    public static boolean keyMappingMatches(KeyMapping mapping, Object event) {
        if (mapping == null || event == null) {
            return false;
        }
        Object keyEvent = invokeNoArgumentMethod(event, "getKeyEvent");
        if (keyEvent == null) {
            return false;
        }
        try {
            for (var method : mapping.getClass().getMethods()) {
                if ("matches".equals(method.getName()) && method.getParameterCount() == 1
                        && method.getParameterTypes()[0].isInstance(keyEvent)) {
                    Object matched = method.invoke(mapping, keyEvent);
                    return Boolean.TRUE.equals(matched);
                }
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return false;
        }
        return false;
    }

    public static GuiGraphicsExtractor guiGraphics(Object event) {
        Object graphics = invokeNoArgumentMethod(event, "getGuiGraphics");
        return graphics instanceof GuiGraphicsExtractor extractor ? extractor : null;
    }

    public static Screen containerScreen(Object event) {
        if (event instanceof ContainerScreenEvent.Render.Foreground foreground) {
            return foreground.getContainerScreen();
        }
        return null;
    }

    public static float guiPartialTick(Object event) {
        Object partialTick = invokeNoArgumentMethod(event, "getPartialTick");
        if (partialTick == null) {
            return 0.0F;
        }
        if (partialTick instanceof Number number) {
            return number.floatValue();
        }
        try {
            var method = partialTick.getClass().getMethod("getGameTimeDeltaPartialTick", boolean.class);
            Object value = method.invoke(partialTick, true);
            return value instanceof Number number ? number.floatValue() : 0.0F;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return 0.0F;
        }
    }

    public static Vec3 renderCameraPosition(Object event) {
        Object levelRenderState = invokeNoArgumentMethod(event, "getLevelRenderState");
        if (levelRenderState == null) {
            return Vec3.ZERO;
        }
        try {
            Object cameraRenderState = levelRenderState.getClass().getField("cameraRenderState").get(levelRenderState);
            Object position = cameraRenderState == null
                    ? null
                    : cameraRenderState.getClass().getField("pos").get(cameraRenderState);
            return position instanceof Vec3 vector ? vector : Vec3.ZERO;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return Vec3.ZERO;
        }
    }

    public static PoseStack renderPoseStack(Object event) {
        Object poseStack = invokeNoArgumentMethod(event, "getPoseStack");
        return poseStack instanceof PoseStack stack ? stack : null;
    }

    public static ItemStack tooltipItemStack(Object event) {
        if (event instanceof ItemTooltipEvent tooltipEvent) {
            return tooltipEvent.getItemStack();
        }
        return ItemStack.EMPTY;
    }

    public static Screen screen(Object event) {
        if (event instanceof ScreenEvent.CharacterTyped.Pre typed) {
            return typed.getScreen();
        }
        if (event instanceof ScreenEvent.MouseScrolled.Pre scrolled) {
            return scrolled.getScreen();
        }
        if (event instanceof ScreenEvent.Render.Post render) {
            return render.getScreen();
        }
        if (event instanceof ScreenEvent.KeyPressed.Pre key) {
            return key.getScreen();
        }
        if (event instanceof ScreenEvent.MouseButtonPressed.Pre mouse) {
            return mouse.getScreen();
        }
        if (event instanceof ScreenEvent.MouseDragged.Pre mouse) {
            return mouse.getScreen();
        }
        if (event instanceof ScreenEvent.MouseButtonReleased.Pre mouse) {
            return mouse.getScreen();
        }
        return null;
    }

    public static int screenMouseX(Object event) {
        if (event instanceof ScreenEvent.Render.Post render) {
            return render.getMouseX();
        }
        return (int) mouseX(event);
    }

    public static int screenMouseY(Object event) {
        if (event instanceof ScreenEvent.Render.Post render) {
            return render.getMouseY();
        }
        return (int) mouseY(event);
    }

    public static CharacterEvent characterEvent(Object event) {
        return event instanceof ScreenEvent.CharacterTyped.Pre typed ? typed.getCharacterEvent() : null;
    }

    public static String characterText(Object event) {
        CharacterEvent character = characterEvent(event);
        return character == null ? "" : character.codepointAsString();
    }

    public static boolean allowedChatCharacter(Object event) {
        CharacterEvent character = characterEvent(event);
        return character != null && character.isAllowedChatCharacter();
    }

    public static KeyEvent keyEvent(Object event) {
        return event instanceof ScreenEvent.KeyPressed.Pre key ? key.getKeyEvent() : null;
    }

    public static int keyCode(Object event) {
        Object value = invokeNoArgumentMethod(event, "getKey");
        return value instanceof Number number ? number.intValue() : 0;
    }

    public static double mouseX(Object event) {
        if (event instanceof ScreenEvent.MouseScrolled.Pre scrolled) {
            return scrolled.getMouseX();
        }
        if (event instanceof ScreenEvent.MouseButtonPressed.Pre pressed) {
            return pressed.getMouseX();
        }
        if (event instanceof ScreenEvent.MouseDragged.Pre dragged) {
            return dragged.getMouseX();
        }
        if (event instanceof ScreenEvent.MouseButtonReleased.Pre released) {
            return released.getMouseX();
        }
        return 0.0D;
    }

    public static double mouseY(Object event) {
        if (event instanceof ScreenEvent.MouseScrolled.Pre scrolled) {
            return scrolled.getMouseY();
        }
        if (event instanceof ScreenEvent.MouseButtonPressed.Pre pressed) {
            return pressed.getMouseY();
        }
        if (event instanceof ScreenEvent.MouseDragged.Pre dragged) {
            return dragged.getMouseY();
        }
        if (event instanceof ScreenEvent.MouseButtonReleased.Pre released) {
            return released.getMouseY();
        }
        return 0.0D;
    }

    public static int mouseButton(Object event) {
        if (event instanceof ScreenEvent.MouseButtonPressed.Pre pressed) {
            return pressed.getButton();
        }
        if (event instanceof ScreenEvent.MouseButtonReleased.Pre released) {
            return released.getButton();
        }
        return 0;
    }

    public static int mouseModifiers(Object event) {
        Object mouseButtonEvent = invokeNoArgumentMethod(event, "getMouseButtonEvent");
        if (mouseButtonEvent == null) {
            return 0;
        }
        Object modifiers = invokeNoArgumentMethod(mouseButtonEvent, "modifiers");
        return modifiers instanceof Number number ? number.intValue() : 0;
    }

    public static double dragX(Object event) {
        return event instanceof ScreenEvent.MouseDragged.Pre dragged ? dragged.getDragX() : 0.0D;
    }

    public static double dragY(Object event) {
        return event instanceof ScreenEvent.MouseDragged.Pre dragged ? dragged.getDragY() : 0.0D;
    }

    public static double scrollDeltaX(Object event) {
        return event instanceof ScreenEvent.MouseScrolled.Pre scrolled ? scrolled.getScrollDeltaX() : 0.0D;
    }

    public static double scrollDeltaY(Object event) {
        return event instanceof ScreenEvent.MouseScrolled.Pre scrolled ? scrolled.getScrollDeltaY() : 0.0D;
    }

    public static void cancel(Object event) {
        if (event instanceof net.neoforged.bus.api.ICancellableEvent cancellable) {
            cancellable.setCanceled(true);
        }
    }

    public static boolean addTooltipLine(Object event, Component line) {
        if (event instanceof ItemTooltipEvent tooltipEvent && line != null) {
            tooltipEvent.getToolTip().add(line);
            return true;
        }
        return false;
    }

    private static boolean invokeOneArgumentRegistration(Object event, String methodName, Object value) {
        if (event == null || value == null) {
            return false;
        }
        try {
            for (var method : event.getClass().getMethods()) {
                if (methodName.equals(method.getName()) && method.getParameterCount() == 1) {
                    method.invoke(event, value);
                    return true;
                }
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return false;
        }
        return false;
    }

    private static boolean invokeTwoArgumentRegistration(Object event, String methodName, Object first, Object second) {
        if (event == null || first == null || second == null) {
            return false;
        }
        try {
            for (var method : event.getClass().getMethods()) {
                if (methodName.equals(method.getName()) && method.getParameterCount() == 2) {
                    method.invoke(event, first, second);
                    return true;
                }
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return false;
        }
        return false;
    }

    private static Object invokeNoArgumentMethod(Object event, String methodName) {
        if (event == null) {
            return null;
        }
        try {
            return event.getClass().getMethod(methodName).invoke(event);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }
}
