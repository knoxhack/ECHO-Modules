package com.knoxhack.echocursecore.api;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.mission.MissionObjectiveType;
import com.knoxhack.echocursecore.EchoCurseCore;
import com.knoxhack.echocursecore.network.CurseCoreNetwork;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

public final class CurseCoreApi {
    public static final Identifier ECHO_ROT = EchoCurseCore.id("curse/echo_rot");
    public static final Identifier GLASS_VEINS = EchoCurseCore.id("curse/glass_veins");
    public static final Identifier RIFT_HUNGER = EchoCurseCore.id("curse/rift_hunger");
    public static final Identifier SOUL_STATIC = EchoCurseCore.id("curse/soul_static");
    public static final Identifier PHANTOM_BURN = EchoCurseCore.id("curse/phantom_burn");
    public static final Identifier BLOOD_DEBT = EchoCurseCore.id("curse/blood_debt");
    public static final Identifier VOID_MARK = EchoCurseCore.id("curse/void_mark");
    public static final Identifier CURSE_CLEANSED = EchoCurseCore.id("curse_cleansed");
    private static final String ROOT = "echocursecore_curses";
    private static final List<Identifier> KNOWN_CURSES = List.of(ECHO_ROT, GLASS_VEINS, RIFT_HUNGER,
            SOUL_STATIC, PHANTOM_BURN, BLOOD_DEBT, VOID_MARK);

    private CurseCoreApi() {
    }

    public static boolean applyEchoRot(ServerPlayer player, int stage, String source) {
        return applyCurse(player, ECHO_ROT, stage, source);
    }

    public static List<Identifier> knownCurses() {
        return KNOWN_CURSES;
    }

    public static boolean applyCurse(ServerPlayer player, Identifier curseId, int stage, String source) {
        if (player == null || curseId == null || stage <= 0) {
            return false;
        }
        int next = Math.min(5, Math.max(stage(player, curseId), stage));
        root(player).putInt(key(curseId), next);
        root(player).putLong("last_changed", player.level().getGameTime());
        player.sendSystemMessage(Component.translatable("curse.echocursecore.applied", title(curseId), next));
        CurseCoreEvents.fireGained(player, curseId, next, source == null ? "" : source);
        record(player, curseId, "gained");
        CurseCoreNetwork.sendTo(player);
        return true;
    }

    public static boolean acceptContract(ServerPlayer player, Identifier curseId, int stage, String source) {
        if (player == null || curseId == null || !KNOWN_CURSES.contains(curseId)) {
            return false;
        }
        root(player).putBoolean(contractKey(curseId), true);
        root(player).putInt(debtKey(curseId), Math.max(contractDebt(player, curseId), Math.max(1, stage) * 40));
        root(player).putLong("last_changed", player.level().getGameTime());
        boolean applied = applyCurse(player, curseId, Math.max(1, stage), source == null ? "contract" : source);
        player.sendSystemMessage(Component.translatable("curse.echocursecore.contract_bound", title(curseId)));
        record(player, curseId, "contract");
        CurseCoreNetwork.sendTo(player);
        return applied;
    }

    public static boolean breakContract(ServerPlayer player, Identifier curseId) {
        if (player == null || curseId == null || !contractBound(player, curseId)) {
            return false;
        }
        root(player).putBoolean(contractKey(curseId), false);
        root(player).putInt(debtKey(curseId), 0);
        root(player).putLong("last_changed", player.level().getGameTime());
        player.sendSystemMessage(Component.translatable("curse.echocursecore.contract_broken", title(curseId)));
        record(player, CURSE_CLEANSED, "contract_broken");
        CurseCoreNetwork.sendTo(player);
        return true;
    }

    public static boolean cleanseFirstMinorCurse(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        for (Identifier curse : KNOWN_CURSES) {
            if (stage(player, curse) <= 0) {
                continue;
            }
            return cleanseCurse(player, curse, 1, false, "ritual_cleansing");
        }
        return false;
    }

    public static boolean cleanseCurse(ServerPlayer player, Identifier curseId, int amount,
            boolean allowContractBound, String source) {
        if (player == null || curseId == null || stage(player, curseId) <= 0) {
            return false;
        }
        if (contractBound(player, curseId) && !allowContractBound) {
            player.sendSystemMessage(Component.translatable("curse.echocursecore.cleansing_blocked", title(curseId)));
            return false;
        }
        return reduceCurse(player, curseId, amount, source == null ? "direct_cleansing" : source);
    }

    public static boolean severContractAndCleanse(ServerPlayer player, Identifier curseId) {
        if (player == null || curseId == null || !contractBound(player, curseId)) {
            return false;
        }
        int debt = contractDebt(player, curseId);
        if (debt > 0) {
            player.sendSystemMessage(Component.translatable("curse.echocursecore.sever_blocked", title(curseId), debt));
            return false;
        }
        root(player).putBoolean(contractKey(curseId), false);
        root(player).putLong("last_changed", player.level().getGameTime());
        reduceCurse(player, curseId, 5, "contract_break");
        player.sendSystemMessage(Component.translatable("curse.echocursecore.contract_broken", title(curseId)));
        record(player, CURSE_CLEANSED, "contract_severed");
        CurseCoreNetwork.sendTo(player);
        return true;
    }

    public static boolean canSeverContract(Player player, Identifier curseId) {
        return player != null && curseId != null && contractBound(player, curseId) && contractDebt(player, curseId) <= 0;
    }

    public static boolean stabilizeFirstContract(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        for (Identifier curse : KNOWN_CURSES) {
            if (contractBound(player, curse) && contractDebt(player, curse) > 0) {
                return payContractDebt(player, curse, 25, "ledger_stabilize");
            }
        }
        return false;
    }

    public static boolean payContractDebt(ServerPlayer player, Identifier curseId, int amount, String source) {
        if (player == null || curseId == null || amount <= 0 || !contractBound(player, curseId)) {
            return false;
        }
        int current = contractDebt(player, curseId);
        if (current <= 0) {
            player.sendSystemMessage(Component.translatable("curse.echocursecore.debt_clear", title(curseId)));
            return false;
        }
        int next = Math.max(0, current - amount);
        root(player).putInt(debtKey(curseId), next);
        root(player).putLong("last_changed", player.level().getGameTime());
        if (next == 0) {
            reduceCurse(player, curseId, 1, source == null ? "contract_payment" : source);
            player.sendSystemMessage(Component.translatable("curse.echocursecore.debt_clear", title(curseId)));
        } else {
            player.sendSystemMessage(Component.translatable("curse.echocursecore.debt_paid", title(curseId), next));
        }
        record(player, curseId, "contract_payment");
        CurseCoreNetwork.sendTo(player);
        return true;
    }

    public static boolean reduceCurse(ServerPlayer player, Identifier curseId, int amount, String source) {
        int current = stage(player, curseId);
        if (player == null || current <= 0 || amount <= 0) {
            return false;
        }
        int next = Math.max(0, current - amount);
        if (next == 0 && contractBound(player, curseId) && !"contract_break".equals(source)) {
            next = 1;
            player.sendSystemMessage(Component.translatable("curse.echocursecore.contract_resists", title(curseId)));
        }
        root(player).putInt(key(curseId), next);
        root(player).putLong("last_changed", player.level().getGameTime());
        player.sendSystemMessage(next == 0
                ? Component.translatable("curse.echocursecore.cleansed", title(curseId))
                : Component.translatable("curse.echocursecore.reduced", title(curseId), next));
        CurseCoreEvents.fireCleansed(player, curseId, next, source == null ? "" : source);
        record(player, CURSE_CLEANSED, "cleansed");
        CurseCoreNetwork.sendTo(player);
        return true;
    }

    public static int stage(Player player, Identifier curseId) {
        if (player == null || curseId == null) {
            return 0;
        }
        return Math.max(0, root(player).getIntOr(key(curseId), 0));
    }

    public static boolean hasCurse(Player player, Identifier curseId) {
        return stage(player, curseId) > 0;
    }

    public static boolean contractBound(Player player, Identifier curseId) {
        return player != null && curseId != null && root(player).getBooleanOr(contractKey(curseId), false);
    }

    public static int contractDebt(Player player, Identifier curseId) {
        if (player == null || curseId == null) {
            return 0;
        }
        return Math.max(0, root(player).getIntOr(debtKey(curseId), 0));
    }

    public static int totalContractDebt(Player player) {
        int debt = 0;
        for (Identifier curse : KNOWN_CURSES) {
            debt += contractDebt(player, curse);
        }
        return debt;
    }

    public static int contractResistance(Player player) {
        int resistance = 0;
        for (Identifier curse : KNOWN_CURSES) {
            resistance += contractBound(player, curse) ? stage(player, curse) * 12 : 0;
            resistance += contractDebt(player, curse) / 5;
        }
        return Math.min(100, resistance);
    }

    public static int cleansingReadiness(Player player) {
        int active = activeCurses(player).size();
        int cleanseable = cleanseableCount(player);
        int debtPenalty = Math.min(60, totalContractDebt(player) / 4);
        int resistancePenalty = contractResistance(player) / 2;
        return Math.max(0, Math.min(100, cleanseable * 24 + active * 8 - debtPenalty - resistancePenalty));
    }

    public static int contractCount(Player player) {
        int count = 0;
        for (Identifier curse : KNOWN_CURSES) {
            count += contractBound(player, curse) ? 1 : 0;
        }
        return count;
    }

    public static int severReadyCount(Player player) {
        int count = 0;
        for (Identifier curse : KNOWN_CURSES) {
            count += canSeverContract(player, curse) ? 1 : 0;
        }
        return count;
    }

    public static int cleansingPlanCode(Player player) {
        if (severReadyCount(player) > 0) {
            return 3;
        }
        if (totalContractDebt(player) > 0) {
            return 2;
        }
        if (cleanseableCount(player) > 0) {
            return 1;
        }
        return 0;
    }

    public static Identifier recommendedCleansingTarget(Player player) {
        for (Identifier curse : KNOWN_CURSES) {
            if (canSeverContract(player, curse)) {
                return curse;
            }
        }
        for (Identifier curse : KNOWN_CURSES) {
            if (contractBound(player, curse) && contractDebt(player, curse) > 0) {
                return curse;
            }
        }
        for (Identifier curse : KNOWN_CURSES) {
            if (stage(player, curse) > 0 && !contractBound(player, curse)) {
                return curse;
            }
        }
        return null;
    }

    public static int recommendedCleansingTargetCode(Player player) {
        Identifier target = recommendedCleansingTarget(player);
        if (BLOOD_DEBT.equals(target)) {
            return 2;
        }
        if (VOID_MARK.equals(target)) {
            return 3;
        }
        if (ECHO_ROT.equals(target)) {
            return 1;
        }
        return target == null ? 0 : 4;
    }

    public static String recommendedCleansingAction(Player player) {
        return switch (cleansingPlanCode(player)) {
            case 3 -> "sever_ready_contract";
            case 2 -> "pay_contract_debt";
            case 1 -> "cleanse_unbound_curse";
            default -> "observe";
        };
    }

    public static int cleanseableCount(Player player) {
        int count = 0;
        for (Identifier curse : KNOWN_CURSES) {
            if (stage(player, curse) > 0 && !contractBound(player, curse)) {
                count++;
            }
        }
        return count;
    }

    public static Map<Identifier, Integer> activeCurses(Player player) {
        Map<Identifier, Integer> result = new LinkedHashMap<>();
        if (player == null) {
            return result;
        }
        for (Identifier curse : KNOWN_CURSES) {
            int stage = stage(player, curse);
            if (stage > 0) {
                result.put(curse, stage);
            }
        }
        return Map.copyOf(result);
    }

    public static void tick(ServerPlayer player) {
        if (player == null || player.level().getGameTime() % 80L != 0L) {
            return;
        }
        int echoRot = stage(player, ECHO_ROT);
        if (echoRot > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 35 + echoRot * 5, 0, false, false));
            if (echoRot >= 2) {
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0, false, true));
            }
        }
        int glass = stage(player, GLASS_VEINS);
        if (glass > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 80, 0, false, true));
            if (glass >= 3) {
                player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 0, false, true));
            }
        }
        int riftHunger = stage(player, RIFT_HUNGER);
        if (riftHunger > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 55 + riftHunger * 5, 0, false, true));
            if (riftHunger >= 3) {
                player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 70, 0, false, true));
            }
        }
        int soulStatic = stage(player, SOUL_STATIC);
        if (soulStatic > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 120, 0, false, false));
            if (soulStatic >= 3) {
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 70, 0, false, true));
            }
        }
        int phantomBurn = stage(player, PHANTOM_BURN);
        if (phantomBurn > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 100, 0, false, true));
            if (phantomBurn >= 4) {
                player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, true));
            }
        }
        int bloodDebt = stage(player, BLOOD_DEBT);
        if (bloodDebt > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 80, 0, false, true));
            if (bloodDebt >= 3) {
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, false, true));
            }
        }
        int voidMark = stage(player, VOID_MARK);
        if (voidMark > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 45 + voidMark * 5, 0, false, false));
            if (voidMark >= 2) {
                player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 0, false, true));
            }
        }
        CurseCoreNetwork.sendTo(player);
    }

    public static long lastChanged(Player player) {
        return player == null ? 0L : Math.max(0L, root(player).getLongOr("last_changed", 0L));
    }

    public static String summary(Player player) {
        Map<Identifier, Integer> curses = activeCurses(player);
        if (curses.isEmpty()) {
            return "none";
        }
        return curses.entrySet().stream()
                .map(entry -> title(entry.getKey()) + " " + entry.getValue())
                .reduce((left, right) -> left + ", " + right)
                .orElse("none");
    }

    private static CompoundTag root(Player player) {
        CompoundTag root = player.getPersistentData().getCompoundOrEmpty(ROOT);
        player.getPersistentData().put(ROOT, root);
        return root;
    }

    private static String key(Identifier curseId) {
        return curseId.toString().replace(':', '_').replace('/', '_');
    }

    private static String contractKey(Identifier curseId) {
        return "contract_" + key(curseId);
    }

    private static String debtKey(Identifier curseId) {
        return "debt_" + key(curseId);
    }

    private static String title(Identifier id) {
        String path = id == null ? "unknown" : id.getPath();
        if (path.startsWith("curse/")) {
            path = path.substring("curse/".length());
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

    private static void record(ServerPlayer player, Identifier target, String action) {
        EchoCoreServices.recordMissionObjective(player, MissionObjectiveType.CUSTOM, target, 1,
                Map.of("source", EchoCurseCore.MODID, "action", action));
    }
}
