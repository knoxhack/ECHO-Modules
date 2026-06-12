package com.knoxhack.echofamiliarcore.api;

import com.knoxhack.echoarcanacore.api.AetherSignalType;
import com.knoxhack.echoarcanacore.api.ArcanaCoreServices;
import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.mission.MissionObjectiveType;
import com.knoxhack.echofamiliarcore.EchoFamiliarCore;
import com.knoxhack.echofamiliarcore.entity.ArcanaFamiliarEntity;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class FamiliarCoreApi {
    public static final Identifier AETHER_WISP = EchoFamiliarCore.id("familiar/aether_wisp");
    public static final Identifier SPIRIT_DRONE = EchoFamiliarCore.id("familiar/spirit_drone");
    private static final String ROOT = "echofamiliarcore_bond";
    public static final String UPGRADE_ATTUNEMENT = "attunement";
    public static final String UPGRADE_WARDING = "warding";
    public static final String UPGRADE_SCOUTING = "scouting";

    private FamiliarCoreApi() {
    }

    public static boolean bind(ServerPlayer player, Identifier familiarId, String source) {
        if (player == null || familiarId == null || !isStarter(familiarId)) {
            return false;
        }
        ArcanaFamiliarEntity familiar = ArcanaFamiliarEntity.summonOrRefresh(player, AETHER_WISP.equals(familiarId)
                ? ArcanaFamiliarEntity.KIND_AETHER_WISP
                : ArcanaFamiliarEntity.KIND_SPIRIT_DRONE);
        if (familiar == null) {
            return false;
        }
        CompoundTag root = root(player);
        root.putString("active", familiarId.toString());
        root.putString("entity_uuid", familiar.getUUID().toString());
        root.putInt("bond_level", Math.max(1, root.getIntOr("bond_level", 0)));
        root.putInt("bond_xp", Math.max(0, root.getIntOr("bond_xp", 0)));
        root.putLong("last_changed", player.level().getGameTime());
        player.sendSystemMessage(Component.translatable("message.echofamiliarcore.bound", title(familiarId)));
        EchoCoreServices.recordMissionObjective(player, MissionObjectiveType.CUSTOM, familiarId, 1,
                Map.of("source", EchoFamiliarCore.MODID, "action", source == null ? "bind" : source));
        return true;
    }

    public static void recordCommand(ServerPlayer player, Identifier familiarId, String command) {
        if (player == null || familiarId == null) {
            return;
        }
        addBondExperience(player, 4, "command");
        EchoCoreServices.recordMissionObjective(player, MissionObjectiveType.CUSTOM, familiarId, 1,
                Map.of("source", EchoFamiliarCore.MODID, "action", "command_" + (command == null ? "" : command)));
    }

    public static boolean setCommand(ServerPlayer player, int command, String source) {
        ArcanaFamiliarEntity familiar = activeEntity(player);
        if (familiar == null || command < ArcanaFamiliarEntity.COMMAND_FOLLOW
                || command > ArcanaFamiliarEntity.COMMAND_DEFEND) {
            return false;
        }
        familiar.setCommandFromMenu(player, command);
        recordCommand(player, familiar.familiarKind() == ArcanaFamiliarEntity.KIND_SPIRIT_DRONE
                ? SPIRIT_DRONE : AETHER_WISP, source == null ? "menu" : source);
        return true;
    }

    public static Identifier activeFamiliar(Player player) {
        Identifier parsed = Identifier.tryParse(root(player).getStringOr("active", ""));
        return isStarter(parsed) ? parsed : null;
    }

    public static int bondLevel(Player player) {
        return Math.max(0, root(player).getIntOr("bond_level", 0));
    }

    public static int bondExperience(Player player) {
        return Math.max(0, root(player).getIntOr("bond_xp", 0));
    }

    public static int nextLevelExperience(Player player) {
        return Math.max(30, (bondLevel(player) + 1) * 40);
    }

    public static int evolutionTier(Player player) {
        int level = bondLevel(player);
        if (level >= 6) {
            return 5;
        }
        if (level >= 5) {
            return 4;
        }
        if (level >= 4) {
            return 3;
        }
        if (level >= 3) {
            return 2;
        }
        return level >= 2 ? 1 : 0;
    }

    public static String evolutionName(Player player) {
        return switch (evolutionTier(player)) {
            case 5 -> "mythic";
            case 4 -> "ascended";
            case 3 -> "bound";
            case 2 -> "trusted";
            case 1 -> "awakened";
            default -> "dormant";
        };
    }

    public static String evolutionBranch(Player player) {
        int attunement = upgradeRank(player, UPGRADE_ATTUNEMENT);
        int warding = upgradeRank(player, UPGRADE_WARDING);
        int scouting = upgradeRank(player, UPGRADE_SCOUTING);
        if (warding > attunement && warding >= scouting) {
            return UPGRADE_WARDING;
        }
        if (scouting > attunement && scouting > warding) {
            return UPGRADE_SCOUTING;
        }
        return UPGRADE_ATTUNEMENT;
    }

    public static int evolutionFormCode(Player player) {
        return switch (evolutionBranch(player)) {
            case UPGRADE_WARDING -> 2;
            case UPGRADE_SCOUTING -> 3;
            default -> 1;
        };
    }

    public static String evolutionForm(Player player) {
        String branch = evolutionBranch(player);
        int tier = evolutionTier(player);
        Identifier active = activeFamiliar(player);
        String base = SPIRIT_DRONE.equals(active) ? "signal chassis" : "aether mantle";
        if (tier <= 0) {
            return "dormant " + base;
        }
        return switch (branch) {
            case UPGRADE_WARDING -> tier >= 4 ? "guardian " + base : "warded " + base;
            case UPGRADE_SCOUTING -> tier >= 4 ? "pathfinder " + base : "scout " + base;
            default -> tier >= 4 ? "overclocked " + base : "attuned " + base;
        };
    }

    public static int evolutionPower(Player player) {
        int branchTotal = upgradeRank(player, UPGRADE_ATTUNEMENT)
                + upgradeRank(player, UPGRADE_WARDING)
                + upgradeRank(player, UPGRADE_SCOUTING);
        return evolutionTier(player) * 10 + branchTotal * 4 + bondLevel(player);
    }

    public static int evolutionAbilityCode(Player player) {
        String branch = evolutionBranch(player);
        Identifier active = activeFamiliar(player);
        if (SPIRIT_DRONE.equals(active) && UPGRADE_WARDING.equals(branch)) {
            return 5;
        }
        if (SPIRIT_DRONE.equals(active) && UPGRADE_SCOUTING.equals(branch)) {
            return 6;
        }
        if (SPIRIT_DRONE.equals(active)) {
            return 4;
        }
        if (UPGRADE_WARDING.equals(branch)) {
            return 2;
        }
        if (UPGRADE_SCOUTING.equals(branch)) {
            return 3;
        }
        return 1;
    }

    public static String evolutionAbility(Player player) {
        return switch (evolutionAbilityCode(player)) {
            case 6 -> "signal sweep";
            case 5 -> "hardlight guard";
            case 4 -> "aether uplink";
            case 3 -> "trace scout";
            case 2 -> "veil ward";
            default -> "aether bloom";
        };
    }

    public static int upgradePoints(Player player) {
        return Math.max(0, root(player).getIntOr("upgrade_points", 0));
    }

    public static int upgradeRank(Player player) {
        return upgradeRank(player, UPGRADE_ATTUNEMENT);
    }

    public static int upgradeRank(Player player, String upgradeId) {
        String safe = safeUpgrade(upgradeId);
        return Math.max(0, root(player).getIntOr("upgrade_" + safe, 0));
    }

    public static boolean addBondExperience(ServerPlayer player, int amount, String source) {
        if (player == null || activeFamiliar(player) == null || amount <= 0) {
            return false;
        }
        CompoundTag root = root(player);
        int level = Math.max(1, root.getIntOr("bond_level", 1));
        int xp = Math.max(0, root.getIntOr("bond_xp", 0)) + amount;
        int next = Math.max(30, (level + 1) * 40);
        boolean leveled = false;
        int levelUps = 0;
        while (xp >= next && level < 6) {
            xp -= next;
            level++;
            levelUps++;
            next = Math.max(30, (level + 1) * 40);
            leveled = true;
        }
        root.putInt("bond_level", level);
        root.putInt("bond_xp", xp);
        if (leveled) {
            root.putInt("upgrade_points", root.getIntOr("upgrade_points", 0) + levelUps);
        }
        root.putLong("last_changed", player.level().getGameTime());
        if (leveled) {
            player.sendSystemMessage(Component.translatable("message.echofamiliarcore.bond_level", level));
            EchoCoreServices.recordMissionObjective(player, MissionObjectiveType.CUSTOM,
                    activeFamiliar(player), 1, Map.of("source", EchoFamiliarCore.MODID,
                            "action", source == null ? "bond_level" : source));
        }
        return true;
    }

    public static boolean spendUpgrade(ServerPlayer player, String upgradeId) {
        if (player == null || activeFamiliar(player) == null || upgradePoints(player) <= 0) {
            return false;
        }
        String safeUpgrade = safeUpgrade(upgradeId);
        String key = "upgrade_" + safeUpgrade;
        CompoundTag root = root(player);
        int rank = Math.max(0, root.getIntOr(key, 0));
        if (rank >= 3) {
            player.sendSystemMessage(Component.translatable("message.echofamiliarcore.upgrade_maxed"));
            return false;
        }
        root.putInt(key, rank + 1);
        root.putInt("upgrade_points", Math.max(0, upgradePoints(player) - 1));
        root.putString("evolution_branch", safeUpgrade);
        root.putLong("last_changed", player.level().getGameTime());
        player.sendSystemMessage(Component.translatable("message.echofamiliarcore.upgrade",
                upgradeTitle(safeUpgrade), rank + 1));
        EchoCoreServices.recordMissionObjective(player, MissionObjectiveType.CUSTOM,
                activeFamiliar(player), 1, Map.of("source", EchoFamiliarCore.MODID, "action", "familiar_upgrade_" + safeUpgrade));
        return true;
    }

    public static ArcanaFamiliarEntity activeEntity(ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        String uuid = root(player).getStringOr("entity_uuid", "");
        if (!uuid.isBlank()) {
            try {
                UUID parsed = UUID.fromString(uuid);
                Entity entity = serverLevel.getEntity(parsed);
                if (entity instanceof ArcanaFamiliarEntity familiar) {
                    return familiar;
                }
                for (Entity candidate : serverLevel.getAllEntities()) {
                    if (candidate instanceof ArcanaFamiliarEntity familiar && parsed.equals(familiar.getUUID())) {
                        return familiar;
                    }
                }
            } catch (IllegalArgumentException ignored) {
                // Fall back to owner/kind lookup below.
            }
        }
        Identifier active = activeFamiliar(player);
        int kind = AETHER_WISP.equals(active) ? ArcanaFamiliarEntity.KIND_AETHER_WISP
                : SPIRIT_DRONE.equals(active) ? ArcanaFamiliarEntity.KIND_SPIRIT_DRONE : -1;
        return kind < 0 ? null : ArcanaFamiliarEntity.ownedFamiliars(serverLevel, player, kind, 128.0D)
                .stream()
                .findFirst()
                .orElse(null);
    }

    public static String summary(Player player) {
        Identifier active = activeFamiliar(player);
        return active == null ? "none" : title(active) + " bond " + bondLevel(player);
    }

    public static void tick(ServerPlayer player) {
        if (player == null || player.level().getGameTime() % 60L != 0L) {
            return;
        }
        pulse(player);
    }

    public static void pulse(ServerPlayer player) {
        if (player == null) {
            return;
        }
        Identifier active = activeFamiliar(player);
        int bonus = evolutionTier(player) + upgradeRank(player);
        int warding = upgradeRank(player, UPGRADE_WARDING);
        int scouting = upgradeRank(player, UPGRADE_SCOUTING);
        int ability = evolutionAbilityCode(player);
        if (AETHER_WISP.equals(active)) {
            ArcanaCoreServices.aether().addAether(player, 0.75D + bonus * 0.2D, AetherSignalType.RAW_AETHER);
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 100 + bonus * 10, 0, false, false));
            if (ability == 2) {
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 70 + warding * 14, 0, false, true));
            }
            if (scouting > 0) {
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, 70 + scouting * 12, Math.min(1, scouting - 1),
                        false, true));
            }
            addBondExperience(player, 1, "pulse");
        } else if (SPIRIT_DRONE.equals(active)) {
            ArcanaCoreServices.aether().addAether(player, 0.45D + bonus * 0.15D, AetherSignalType.SIGNAL_AETHER);
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100 + bonus * 10,
                    Math.min(2, bonus / 3 + warding / 2), false, true));
            if (ability == 6) {
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, 65 + scouting * 12, 0, false, true));
            }
            if (warding > 0) {
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 60 + warding * 10, 0, false, true));
            }
            addBondExperience(player, 1, "pulse");
        }
    }

    private static String safeUpgrade(String upgradeId) {
        if (UPGRADE_WARDING.equals(upgradeId) || UPGRADE_SCOUTING.equals(upgradeId)) {
            return upgradeId;
        }
        return UPGRADE_ATTUNEMENT;
    }

    private static String upgradeTitle(String upgradeId) {
        return switch (safeUpgrade(upgradeId)) {
            case UPGRADE_WARDING -> "Warding";
            case UPGRADE_SCOUTING -> "Scouting";
            default -> "Attunement";
        };
    }

    private static boolean isStarter(Identifier id) {
        return AETHER_WISP.equals(id) || SPIRIT_DRONE.equals(id);
    }

    private static CompoundTag root(Player player) {
        if (player == null) {
            return new CompoundTag();
        }
        CompoundTag root = player.getPersistentData().getCompoundOrEmpty(ROOT);
        player.getPersistentData().put(ROOT, root);
        return root;
    }

    private static String title(Identifier id) {
        String path = id == null ? "unknown" : id.getPath();
        if (path.startsWith("familiar/")) {
            path = path.substring("familiar/".length());
        }
        String text = path.replace('_', ' ');
        StringBuilder builder = new StringBuilder(text.length());
        boolean upper = true;
        for (char c : text.toCharArray()) {
            if (Character.isWhitespace(c)) {
                builder.append(c);
                upper = true;
            } else if (upper) {
                builder.append(Character.toUpperCase(c));
                upper = false;
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }
}
