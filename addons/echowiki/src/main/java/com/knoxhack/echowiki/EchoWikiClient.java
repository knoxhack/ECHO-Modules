package com.knoxhack.echowiki;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echowiki.client.WikiScreenCoreBridge;
import com.knoxhack.echowiki.content.WikiJsonReloadListener;
import com.knoxhack.echowiki.platform.WikiModuleAccess;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class EchoWikiClient {
    public EchoWikiClient() {
        WikiScreenCoreBridge.register();
        EchoBackendLifecycleBridge.registerGameEventHandler(EchoWikiClient::onClientCommands);
        EchoBackendLifecycleBridge.registerGameEventHandler(EchoWikiClient::onClientResourceLoadFinished);
        if (WikiModuleAccess.isLoaded("echoterminal")) {
            registerTerminalClientIntegration();
        }
    }

    private static void onClientCommands(Object event) {
        CommandDispatcher<CommandSourceStack> dispatcher = dispatcher(event);
        if (dispatcher == null) {
            return;
        }
        dispatcher.register(
                Commands.literal("echowiki")
                        .executes(context -> open(""))
                        .then(Commands.literal("open")
                                .executes(context -> open(""))
                                .then(Commands.argument("article", StringArgumentType.greedyString())
                                        .executes(context -> open(StringArgumentType.getString(context, "article"))))));
    }

    private static int open(String rawArticle) {
        boolean opened = rawArticle == null || rawArticle.isBlank()
                ? WikiScreenCoreBridge.open()
                : WikiScreenCoreBridge.openArticle(parse(rawArticle));
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.sendSystemMessage(Component.literal(opened
                    ? "Opened ECHO Survival Codex."
                    : "ECHO Survival Codex could not be opened."));
        }
        return opened ? 1 : 0;
    }

    private static Identifier parse(String raw) {
        try {
            String clean = raw == null ? "" : raw.strip();
            return clean.contains(":") ? Identifier.parse(clean) : EchoWiki.id(clean);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static void onClientResourceLoadFinished(Object event) {
        WikiJsonReloadListener.reloadClientData(Minecraft.getInstance().getResourceManager());
        WikiScreenCoreBridge.invalidate();
    }

    private static void registerTerminalClientIntegration() {
        try {
            Class.forName("com.knoxhack.echowiki.integration.WikiTerminalClientIntegration")
                    .getMethod("register")
                    .invoke(null);
        } catch (ReflectiveOperationException | LinkageError exception) {
            EchoWiki.LOGGER.warn("ECHO: Wiki terminal client integration could not be registered.", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private static CommandDispatcher<CommandSourceStack> dispatcher(Object event) {
        if (event == null) {
            return null;
        }
        try {
            Object dispatcher = event.getClass().getMethod("getDispatcher").invoke(event);
            return dispatcher instanceof CommandDispatcher<?> value
                    ? (CommandDispatcher<CommandSourceStack>) value
                    : null;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }
}
