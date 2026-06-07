package com.knoxhack.echoashfallprotocol.integration;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echocore.api.EchoRuntimeModules;
import java.lang.reflect.Method;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class AshfallWikiIntegration {
    public static final String WIKI_MODID = "echowiki";
    public static final Identifier ASHFALL_MANUAL_ID = Identifier.fromNamespaceAndPath(WIKI_MODID, "ashfall");
    public static final String RECEIVED_ASHFALL_MANUAL_FLAG = "ashes_of_tomorrow.received_wiki_manual";

    private static final String API_CLASS = "com.knoxhack.echowiki.api.EchoWikiApi";
    private static boolean loggedReflectionFailure;

    private AshfallWikiIntegration() {
    }

    public static boolean isWikiLoaded() {
        try {
            return EchoRuntimeModules.isLoaded(WIKI_MODID);
        } catch (LinkageError | RuntimeException exception) {
            return false;
        }
    }

    public static boolean isGuideBookVisible(Identifier guideBookId) {
        if (!isWikiLoaded()) {
            return false;
        }
        Object result = invokeApi("isGuideBookVisible", new Class<?>[] { Identifier.class }, guideBookId);
        return result instanceof Boolean visible && visible;
    }

    public static ItemStack guideBookStack(Identifier guideBookId) {
        if (!isWikiLoaded()) {
            return ItemStack.EMPTY;
        }
        Object result = invokeApi("guideBookStack", new Class<?>[] { Identifier.class }, guideBookId);
        return result instanceof ItemStack stack ? stack.copy() : ItemStack.EMPTY;
    }

    public static ItemStack guideBookLibraryStack() {
        if (!isWikiLoaded()) {
            return ItemStack.EMPTY;
        }
        Object result = invokeApi("guideBookLibraryStack", new Class<?>[0]);
        return result instanceof ItemStack stack ? stack.copy() : ItemStack.EMPTY;
    }

    public static ItemStack ashfallManualStack() {
        return guideBookStack(ASHFALL_MANUAL_ID);
    }

    public static boolean giveAshfallManualIfNeeded(ServerPlayer player, CompoundTag playerData) {
        if (player == null || playerData == null
                || playerData.getBoolean(RECEIVED_ASHFALL_MANUAL_FLAG).orElse(false)) {
            return false;
        }

        ItemStack manual = ashfallManualStack();
        if (manual.isEmpty()) {
            return false;
        }

        if (!player.getInventory().add(manual.copy())) {
            player.drop(manual.copy(), false);
        }
        playerData.putBoolean(RECEIVED_ASHFALL_MANUAL_FLAG, true);
        player.sendSystemMessage(Component.literal(
                "\u00A7b[ECHO-7]\u00A7r Ashfall Field Manual added. Use it for water, shelter, hazards, and route basics."));
        return true;
    }

    private static Object invokeApi(String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Class<?> apiClass = Class.forName(API_CLASS);
            Method method = apiClass.getMethod(methodName, parameterTypes);
            return method.invoke(null, args);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            logReflectionFailure(methodName, exception);
            return null;
        }
    }

    private static void logReflectionFailure(String methodName, Throwable exception) {
        if (loggedReflectionFailure) {
            return;
        }
        loggedReflectionFailure = true;
        EchoAshfallProtocol.LOGGER.debug(
                "Unable to call optional EchoWiki API method {}. Ashfall Wiki integration will skip this path.",
                methodName,
                exception);
    }
}
