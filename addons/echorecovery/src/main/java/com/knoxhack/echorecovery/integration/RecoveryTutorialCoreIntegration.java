package com.knoxhack.echorecovery.integration;

import com.knoxhack.echorecovery.EchoRecovery;
import com.knoxhack.echorecovery.api.RecoveryEventHooks;
import com.knoxhack.echorecovery.api.RecoveryGraveSnapshot;
import com.knoxhack.echorecovery.api.RecoveryIntegrations;
import com.knoxhack.echotutorialcore.api.TutorialCategory;
import com.knoxhack.echotutorialcore.api.TutorialCoreApi;
import com.knoxhack.echotutorialcore.api.TutorialGuideMode;
import com.knoxhack.echotutorialcore.api.TutorialHintType;
import com.knoxhack.echotutorialcore.api.card.TutorialCard;
import com.knoxhack.echotutorialcore.api.hint.TutorialHint;
import com.knoxhack.echotutorialcore.api.trigger.TutorialFlow;
import com.knoxhack.echotutorialcore.api.trigger.TutorialStep;
import com.knoxhack.echotutorialcore.api.trigger.TutorialTriggerType;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public final class RecoveryTutorialCoreIntegration {
    private static boolean registered;

    private RecoveryTutorialCoreIntegration() {
    }

    public static void registerCommon() {
        if (registered) {
            return;
        }
        registered = true;
        registerCards();
        registerHints();
        TutorialCoreApi.registerFlow(new TutorialFlow(id("flow/first_recovery"), "First Recovery",
                TutorialCategory.SURVIVAL,
                List.of(new TutorialStep("death", TutorialTriggerType.DEATH, null,
                        "Death creates a protected Recovery cache.", false),
                        new TutorialStep("locate", TutorialTriggerType.CUSTOM, id("card/recovery_compass"),
                                "Use /graves list or a Recovery Compass to locate it.", false),
                        new TutorialStep("recover", TutorialTriggerType.INTERACT_BLOCK, id("grave"),
                                "Open the cache and use Recover All when ready.", false)),
                List.of(id("card/first_death"), id("card/recover_all")), true));
        RecoveryIntegrations.registerEventHooks(Hooks.INSTANCE);
    }

    private static void registerCards() {
        card("first_death", "First Death Recovery",
                "Deaths create protected graves or caches by default.",
                List.of("Use /graves list, a Recovery Compass, or HoloMap if available to find the cache.",
                        "The default beta policy protects contents and keeps expiration disabled."),
                List.of("Find the cache.", "Open it as the owner.", "Recover items when your inventory has room."), true);
        card("grave_key", "Grave Keys",
                "Bound keys support stricter server policies.",
                List.of("Key-required mode is off by default. When enabled, a matching bound key opens its grave.",
                        "Wrong keys show a clear lock message and are not consumed."),
                List.of("Carry the bound key.", "Open the matching grave.", "Recover before consuming mode applies."), false);
        card("recovery_compass", "Recovery Compass",
                "The compass tracks a selected or nearest active grave.",
                List.of("Same-dimension guidance is the default. Cross-dimensional tracking is explicit and config gated.",
                        "Storm, WorldCore, Nexus, and Ashfall integrations can annotate signal quality."),
                List.of("Craft or obtain a Recovery Compass.", "Read the tooltip.", "Travel to the listed target."), true);
        card("recover_all", "Recover All",
                "Recover All restores original slots first.",
                List.of("Hotbar, inventory, armor, and offhand intent is preserved when possible.",
                        "Overflow stays deterministic: either drops by config or leaves the grave partially recovered."),
                List.of("Make inventory space.", "Use Recover All.", "Return for leftovers if recovery was partial."), true);
        card("overflow", "Overflow Recovery",
                "Full inventories do not destroy protected items.",
                List.of("If overflow dropping is disabled, the cache remains with the remaining stacks.",
                        "If enabled, extra stacks drop near the recovering player."),
                List.of("Clear space.", "Recover again.", "Check the ground only if overflow drops are enabled."), false);
        card("sharing", "Shared Recovery",
                "Owners can share their latest active grave.",
                List.of("Use /graves share <player> for one-off access.",
                        "Team access follows server config and is disabled unless the server enables it."),
                List.of("Share with a trusted player.", "Have them open the cache.", "Recover or return remaining stacks."), false);
    }

    private static void registerHints() {
        hint("first_death", "Recovery cache created", "Use /graves list or a Recovery Compass to find your cache.",
                id("card/first_death"), TutorialHintType.SYSTEM_HELP);
        hint("recover_all", "Recover All is available", "Make room first; overflow is handled by server config.",
                id("card/recover_all"), TutorialHintType.INFO);
    }

    private static void card(String path, String title, String summary, List<String> body, List<String> steps,
            boolean defaultUnlocked) {
        TutorialCoreApi.registerCard(new TutorialCard(id("card/" + path), TutorialCategory.SURVIVAL,
                title, summary, body, steps, List.of(), List.of(), List.of(), defaultUnlocked,
                EchoRecovery.MODID, 300));
    }

    private static void hint(String path, String title, String message, Identifier card, TutorialHintType type) {
        TutorialCoreApi.registerHint(new TutorialHint(id("hint/" + path), type, TutorialCategory.SURVIVAL,
                title, message, "", "Open Guide", card, 20 * 60,
                EnumSet.of(TutorialGuideMode.NORMAL, TutorialGuideMode.ASSISTED), 300, true, List.of()));
    }

    private enum Hooks implements RecoveryEventHooks {
        INSTANCE;

        @Override
        public void graveCreated(ServerPlayer player, RecoveryGraveSnapshot snapshot) {
            if (player != null) {
                TutorialCoreApi.unlockCard(player, id("card/first_death"));
                TutorialCoreApi.showHint(player, id("hint/first_death"));
            }
        }

        @Override
        public void graveOpened(ServerPlayer player, RecoveryGraveSnapshot snapshot) {
            if (player != null) {
                TutorialCoreApi.unlockCard(player, id("card/recover_all"));
                TutorialCoreApi.showHint(player, id("hint/recover_all"));
            }
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoRecovery.MODID, path);
    }
}
