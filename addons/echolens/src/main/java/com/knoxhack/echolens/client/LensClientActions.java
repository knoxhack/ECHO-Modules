package com.knoxhack.echolens.client;

import com.knoxhack.echonetcore.client.EchoNetClientActions;
import com.knoxhack.echolens.EchoLens;
import com.knoxhack.echolens.integration.LensSoundFeedback;
import com.knoxhack.echolens.platform.LensModuleAccess;
import dev.echo.nativeplatform.contracts.EchoNativeClientRuntimeEnvironment;
import dev.echo.nativeplatform.contracts.EchoNativeClientRouteRegistries;
import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;
import java.lang.reflect.Constructor;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public final class LensClientActions {
    private LensClientActions() {
    }

    public static void openIndexRecipes(ItemStack stack) {
        if (dispatchNativeIndexRoute(stack, "index.open_recipes_for_item", "recipes")) {
            return;
        }
        openIndexRecipeScreen(stack, "RECIPES");
    }

    public static void openIndexUses(ItemStack stack) {
        if (dispatchNativeIndexRoute(stack, "index.open_usages_for_item", "usages")) {
            return;
        }
        openIndexRecipeScreen(stack, "USES");
    }

    public static void trackInIndex(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (dispatchNativeIndexRoute(stack, "index.track_item", "track")) {
            LensSoundFeedback.play(LensSoundFeedback.ACTION_SHORTCUT);
            return;
        }
        if (!LensModuleAccess.isLoaded("echoindex")) {
            tell("ECHO: Index is not installed.");
            return;
        }
        try {
            Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            Class<?> packetClass = Class.forName("com.knoxhack.echoindex.network.IndexActionPacket");
            Class<?> actionClass = Class.forName("com.knoxhack.echoindex.network.IndexActionPacket$Action");
            Object action = Enum.valueOf(actionClass.asSubclass(Enum.class), "BOOKMARK");
            Constructor<?> constructor = packetClass.getConstructor(actionClass, Identifier.class);
            Object payload = constructor.newInstance(action, itemId);
            if (payload instanceof CustomPacketPayload packet) {
                if (EchoNetClientActions.trySendServerboundAction(packet)) {
                    LensSoundFeedback.play(LensSoundFeedback.ACTION_SHORTCUT);
                    tell("Tracking " + itemId + " in ECHO: Index.");
                } else {
                    tell("ECHO: Index tracking is unavailable.");
                }
            }
        } catch (ReflectiveOperationException exception) {
            EchoLens.LOGGER.warn("Could not send ECHO: Index track request from Lens.", exception);
            tell("ECHO: Index tracking is unavailable.");
        }
    }

    private static void openIndexRecipeScreen(ItemStack stack, String modeName) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (!LensModuleAccess.isLoaded("echoindex")) {
            tell("ECHO: Index is not installed.");
            return;
        }
        try {
            Class<?> screenClass = Class.forName("com.knoxhack.echoindex.client.IndexRecipeScreen");
            Class<?> modeClass = Class.forName("com.knoxhack.echoindex.client.IndexRecipeScreen$Mode");
            Object mode = Enum.valueOf(modeClass.asSubclass(Enum.class), modeName);
            Constructor<?> constructor = screenClass.getConstructor(ItemStack.class, modeClass);
            Screen screen = (Screen) constructor.newInstance(stack.copy(), mode);
            LensSoundFeedback.play(LensSoundFeedback.ACTION_SHORTCUT);
            Minecraft.getInstance().setScreen(screen);
        } catch (ReflectiveOperationException exception) {
            EchoLens.LOGGER.warn("Could not open ECHO: Index {} screen from Lens.", modeName, exception);
            tell("ECHO: Index recipe view is unavailable.");
        }
    }

    private static boolean dispatchNativeIndexRoute(ItemStack stack, String actionId, String mode) {
        if (!EchoNativeClientRuntimeEnvironment.isNativeLoaderActive()
                || stack == null
                || stack.isEmpty()
                || !LensModuleAccess.isLoaded("echoindex")) {
            return false;
        }
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null) {
            return false;
        }
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("source", "echolens_legacy_client_action_adapter");
        metadata.put("eventType", "lens_legacy_index_route_handoff");
        metadata.put("upstreamSurfaceType", "lens");
        metadata.put("upstreamSurfaceId", "echolens:field_lens");
        metadata.put("upstreamRecipeMode", mode == null ? "" : mode);
        metadata.put("itemId", itemId.toString());
        metadata.put("itemCount", stack.getCount());
        EchoNativeLoadStatus status = EchoNativeClientRouteRegistries.get().dispatchStatus(
                "client_overlay",
                actionId,
                Map.copyOf(metadata));
        return status == EchoNativeLoadStatus.MUTATED;
    }

    private static void tell(String message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.sendSystemMessage(Component.literal(message));
        }
    }
}
