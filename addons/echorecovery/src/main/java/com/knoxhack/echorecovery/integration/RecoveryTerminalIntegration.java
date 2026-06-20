package com.knoxhack.echorecovery.integration;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.knoxhack.echorecovery.EchoRecovery;
import com.knoxhack.echorecovery.config.RecoveryConfig;
import com.knoxhack.echorecovery.data.RecoveryWorldData;
import com.knoxhack.echorecovery.registry.ModItems;
import com.knoxhack.echoterminal.api.TerminalArchiveEntry;
import com.knoxhack.echoterminal.api.TerminalArchiveRegistry;
import com.knoxhack.echoterminal.api.mission.TerminalMissionAction;
import com.knoxhack.echoterminal.api.mission.TerminalMissionChapter;
import com.knoxhack.echoterminal.api.mission.TerminalMissionDefinition;
import com.knoxhack.echoterminal.api.mission.TerminalMissionPresentation;
import com.knoxhack.echoterminal.api.mission.TerminalMissionProvider;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRequirement;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRole;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRoutePlacement;
import com.knoxhack.echoterminal.api.mission.TerminalMissionSnapshot;
import com.knoxhack.echoterminal.api.mission.TerminalMissionStatus;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRegistry;
import com.knoxhack.echoterminal.api.mission.TerminalMissionReward;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

public final class RecoveryTerminalIntegration {
    private static boolean registered;

    private RecoveryTerminalIntegration() {}

    public static void registerCommon() {
        if (registered) {
            return;
        }
        registered = true;
        registerArchives();
        TerminalMissionRegistry.register(RecoveryMissionProvider.INSTANCE);
        EchoRecovery.LOGGER.info("ECHO Recovery terminal integration registered.");
    }

    private static void registerArchives() {
        TerminalArchiveRegistry.register(new TerminalArchiveEntry(
            id("archive/graves_basics"),
            "ECHO Recovery",
            "Grave Mechanics",
            "ACTIVE",
            List.of(
                "When a player dies, a grave block is created at the death location.",
                "All inventory items, armor, and offhand items are stored inside.",
                "Right-click the grave to open its inventory and recover items individually.",
                "Graves are protected from explosions, fire, and mob griefing by default."
            ),
            false
        ));
        TerminalArchiveRegistry.register(new TerminalArchiveEntry(
            id("archive/recovery_tools"),
            "ECHO Recovery",
            "Recovery Tools",
            "ONLINE",
            List.of(
                "Grave Key: binds to a specific grave; can be crafted with iron nuggets and a name tag.",
                "Recovery Compass: points to your last grave; craft with iron nuggets and a compass.",
                "Use /graves to list, locate, and recover graves remotely if enabled."
            ),
            false
        ));
        TerminalArchiveRegistry.register(new TerminalArchiveEntry(
            id("archive/grave_protection"),
            "ECHO Recovery",
            "Protection & Decay",
            "ACTIVE",
            List.of(
                "By default, only the grave owner can access it.",
                "Public access may be granted after a configurable time period.",
                "Graves can expire and drop items if decay is enabled in config.",
                "Admin bypass allows operators to access any grave."
            ),
            false
        ));
    }

    private static net.minecraft.resources.Identifier id(String path) {
        return net.minecraft.resources.Identifier.fromNamespaceAndPath(EchoRecovery.MODID, path);
    }

    private enum RecoveryMissionProvider implements TerminalMissionProvider {
        INSTANCE;

        private static final net.minecraft.resources.Identifier CHAPTER = id("chapter/recovery");
        private static final net.minecraft.resources.Identifier FIRST_RECOVERY = id("mission/first_recovery");
        private static final net.minecraft.resources.Identifier COMPASS = id("mission/recovery_compass");
        private static final net.minecraft.resources.Identifier REMOTE = id("mission/remote_recovery");
        private static final net.minecraft.resources.Identifier TEAM = id("mission/team_recovery");
        private static final net.minecraft.resources.Identifier ASHFALL_OUTPOST =
                net.minecraft.resources.Identifier.fromNamespaceAndPath("echoashfallprotocol", "secure_crash_outpost");
        private static final net.minecraft.resources.Identifier ASHFALL_SLEEP_SHELTER =
                net.minecraft.resources.Identifier.fromNamespaceAndPath("echoashfallprotocol", "secure_sleep_shelter");

        @Override
        public TerminalMissionChapter chapter() {
            return new TerminalMissionChapter(CHAPTER, "ECHO Recovery",
                    "Protected graves, field caches, keys, and compass guidance.", 42, 0xFF66D9EF, true);
        }

        @Override
        public List<TerminalMissionDefinition> missions(Player player) {
            return List.of(
                    mission(FIRST_RECOVERY, 1, "First Recovery",
                            "Recover a grave or field cache after death.",
                            "Death creates a protected cache by default. Use /graves list or a Recovery Compass to locate it.",
                            stack(() -> (ItemLike) ModItems.GRAVE_KEY.get(), Items.NAME_TAG, 1)),
                    mission(COMPASS, 2, "Recovery Compass",
                            "Craft or carry a Recovery Compass for nearest-cache guidance.",
                            "The compass tracks same-dimension graves by default and reports cross-dimensional blocks clearly.",
                            stack(() -> (ItemLike) ModItems.RECOVERY_COMPASS.get(), Items.COMPASS, 1)),
                    mission(REMOTE, 3, "Remote Recovery",
                            "Optional service restore when remote recovery is enabled.",
                            "Remote recovery stays disabled by default. Server operators may enable it for assisted restores.",
                            stack(() -> (ItemLike) ModItems.RECOVERY_TOKEN.get(), Items.EMERALD, 1)),
                    mission(TEAM, 4, "Shared Recovery",
                            "Share a cache with another player or enable team recovery.",
                            "Use /graves share <player> for the latest active grave. Team access follows config.",
                            stack(() -> (ItemLike) ModItems.DEATH_RECORD.get(), Items.PAPER, 1))
            );
        }

        @Override
        public TerminalMissionSnapshot snapshot(Player player, net.minecraft.resources.Identifier missionId) {
            int active = activeGraves(player);
            boolean hasCompass = has(player, () -> ModItems.RECOVERY_COMPASS.get());
            TerminalMissionStatus status = TerminalMissionStatus.UNLOCKED;
            float progress = 0.0F;
            String hint = "Review Recovery tools and continue.";
            if (FIRST_RECOVERY.equals(missionId)) {
                progress = active == 0 ? 1.0F : 0.5F;
                status = active == 0 ? TerminalMissionStatus.COMPLETED : TerminalMissionStatus.UNLOCKED;
                hint = active == 0 ? "No active grave is waiting." : "Locate and recover your active grave.";
            } else if (COMPASS.equals(missionId)) {
                progress = hasCompass ? 1.0F : 0.0F;
                status = hasCompass ? TerminalMissionStatus.COMPLETED : TerminalMissionStatus.UNLOCKED;
                hint = hasCompass ? "Compass online." : "Craft or obtain a Recovery Compass.";
            } else if (REMOTE.equals(missionId)) {
                progress = RecoveryConfig.REMOTE_RECOVERY_ENABLED.get() ? 1.0F : 0.0F;
                status = RecoveryConfig.REMOTE_RECOVERY_ENABLED.get() ? TerminalMissionStatus.VIEW_ONLY : TerminalMissionStatus.LOCKED;
                hint = RecoveryConfig.REMOTE_RECOVERY_ENABLED.get() ? "Remote recovery is enabled." : "Remote recovery is disabled by default.";
            } else if (TEAM.equals(missionId)) {
                progress = RecoveryConfig.TEAM_ACCESS.get() ? 1.0F : 0.0F;
                status = RecoveryConfig.TEAM_ACCESS.get() ? TerminalMissionStatus.VIEW_ONLY : TerminalMissionStatus.UNLOCKED;
                hint = RecoveryConfig.TEAM_ACCESS.get() ? "Team access is enabled." : "Use /graves share <player> for one-off sharing.";
            }
            return new TerminalMissionSnapshot(missionId, status, progress, status.name(), "", hint,
                    List.of(TerminalMissionAction.enabled("open_graves", "/graves list")));
        }

        @Override
        public TerminalMissionPresentation presentation(Player player, TerminalMissionDefinition definition, TerminalMissionSnapshot snapshot) {
            return new TerminalMissionPresentation(definition.title(), definition.briefing(), snapshot.actionHint(),
                    "Survival support", "active", List.of("Recovery", "Support"), "archive/recovery_tools");
        }

        @Override
        public TerminalMissionRole role(Player player, TerminalMissionDefinition definition, TerminalMissionSnapshot snapshot) {
            return TerminalMissionRole.OPTIONAL;
        }

        @Override
        public Optional<TerminalMissionRoutePlacement> routePlacement(Player player, TerminalMissionDefinition definition,
                TerminalMissionSnapshot snapshot, TerminalMissionRole role) {
            return Optional.of(TerminalMissionRoutePlacement.optional(1, definition.missionOrder()));
        }

        @Override
        public Optional<net.minecraft.resources.Identifier> routeAnchor(Player player,
                TerminalMissionDefinition definition, TerminalMissionSnapshot snapshot, TerminalMissionRole role) {
            if (definition == null) {
                return Optional.empty();
            }
            if (FIRST_RECOVERY.equals(definition.id()) || COMPASS.equals(definition.id())) {
                return Optional.of(ASHFALL_OUTPOST);
            }
            if (REMOTE.equals(definition.id()) || TEAM.equals(definition.id())) {
                return Optional.of(ASHFALL_SLEEP_SHELTER);
            }
            return Optional.empty();
        }

        private static TerminalMissionDefinition mission(net.minecraft.resources.Identifier id, int order, String title,
                String briefing, String guide, ItemStack icon) {
            return new TerminalMissionDefinition(id, CHAPTER, "survival_support", "Survival Support",
                    2, order, title, briefing, guide, "Recovery Support", "Guided",
                    icon, List.of(), List.of(TerminalMissionRequirement.custom("Recovery", briefing, icon, 0, 1, false)),
                    List.of(TerminalMissionReward.text("Guidance", guide)));
        }

        private static int activeGraves(Player player) {
            if (player == null || !(player.level() instanceof ServerLevel level)) {
                return 0;
            }
            if (level.getServer() == null) {
                return RecoveryWorldData.getOrCreate(level).getActiveGraves(player.getUUID()).size();
            }
            int count = 0;
            for (ServerLevel serverLevel : level.getServer().getAllLevels()) {
                count += RecoveryWorldData.getOrCreate(serverLevel).getActiveGraves(player.getUUID()).size();
            }
            return count;
        }

        private static boolean has(Player player, Supplier<? extends Item> item) {
            if (player == null || nativeLoaderActive() || item == null) {
                return false;
            }
            try {
                Item value = item.get();
                return value != null && player.getInventory().contains(new ItemStack(value));
            } catch (RuntimeException | LinkageError ignored) {
                return false;
            }
        }

        private static ItemStack stack(Supplier<? extends ItemLike> item, ItemLike fallback, int count) {
            if (!EchoCoreServices.itemStackComponentsBound()) {
                return ItemStack.EMPTY;
            }
            try {
                ItemLike value = nativeLoaderActive() || item == null ? fallback : item.get();
                return value == null ? ItemStack.EMPTY : new ItemStack(value, Math.max(1, count));
            } catch (RuntimeException | LinkageError ignored) {
                return fallback == null ? ItemStack.EMPTY : new ItemStack(fallback, Math.max(1, count));
            }
        }

        private static boolean nativeLoaderActive() {
            return Boolean.getBoolean("echo.native.loader")
                    || !System.getProperty("echo.native.moduleIds", "").isBlank()
                    || !System.getProperty("echo.native.moduleClasspath", "").isBlank()
                    || !System.getProperty("echo.native.moduleClasspathFile", "").isBlank();
        }
    }
}
