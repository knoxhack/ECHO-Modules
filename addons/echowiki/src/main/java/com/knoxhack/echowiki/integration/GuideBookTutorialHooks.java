package com.knoxhack.echowiki.integration;

import com.knoxhack.echowiki.EchoWiki;
import com.knoxhack.echowiki.content.GuideBookDefinition;
import com.knoxhack.echowiki.platform.WikiModuleAccess;
import java.lang.reflect.Method;
import java.util.Locale;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public final class GuideBookTutorialHooks {
    private GuideBookTutorialHooks() {
    }

    public static void reportOpened(ServerPlayer player, GuideBookDefinition guide) {
        if (player == null || guide == null || !WikiModuleAccess.isLoaded("echotutorialcore")) {
            return;
        }
        try {
            Class<?> api = Class.forName("com.knoxhack.echotutorialcore.api.TutorialCoreApi");
            Method progress = api.getMethod("reportProgress", net.minecraft.world.entity.player.Player.class, Identifier.class);
            progress.invoke(null, player, EchoWiki.id("opened_guide_book"));
            progress.invoke(null, player, EchoWiki.id("opened_guide_" + sanitize(guide.id().getPath())));
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            EchoWiki.LOGGER.debug("ECHO: TutorialCore guide-book hook skipped for {}.", guide.id(), exception);
        }
    }

    private static String sanitize(String value) {
        String clean = value == null ? "unknown" : value.toLowerCase(Locale.ROOT);
        clean = clean.replace('\\', '/').replace('/', '_');
        clean = clean.replaceAll("[^a-z0-9_.-]", "_");
        return clean.isBlank() ? "unknown" : clean;
    }
}
