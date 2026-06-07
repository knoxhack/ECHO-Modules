package com.knoxhack.echocursecore.integration.arcana;

import com.knoxhack.echoarcanacore.api.ArcanaCoreServices;
import com.knoxhack.echoarcanacore.api.ArcanaProviderInterfaces;
import com.knoxhack.echoarcanacore.api.CurseCategory;
import com.knoxhack.echoarcanacore.api.CurseDefinition;
import com.knoxhack.echocursecore.EchoCurseCore;
import com.knoxhack.echocursecore.api.CurseCoreApi;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public enum CurseCoreArcanaIntegration implements ArcanaProviderInterfaces.CurseProvider,
        ArcanaProviderInterfaces.ArcaneIndexProvider,
        ArcanaProviderInterfaces.GrimoireEntryProvider,
        ArcanaProviderInterfaces.ArcaneLensProvider,
        ArcanaProviderInterfaces.ArcaneMissionProvider,
        ArcanaProviderInterfaces.TerminalArcanaProvider {
    INSTANCE;

    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
    private static final List<CurseDefinition> CURSES = List.of(
            curse(CurseCoreApi.ECHO_ROT, "echo_rot", CurseCategory.MIND,
                    List.of("Terminal flicker", "false Lens pings", "signal afterimages"),
                    List.of("minor signal noise", "weakness during symptoms", "hostile signal attention")),
            curse(CurseCoreApi.GLASS_VEINS, "glass_veins", CurseCategory.BODY,
                    List.of("crystal ache", "hard light under skin", "fragile over-resonance"),
                    List.of("brief resistance", "movement drag at high stage", "crystal spell tradeoffs")),
            curse(CurseCoreApi.RIFT_HUNGER, "rift_hunger", CurseCategory.WORLD,
                    List.of("rift pings behind the eyes", "teleport appetite", "distant gate static"),
                    List.of("rift visibility", "movement drag at high stage", "teleport spell tradeoffs")),
            curse(CurseCoreApi.SOUL_STATIC, "soul_static", CurseCategory.SOUL,
                    List.of("soft voices in quiet rooms", "familiar bond jitter", "afterimage memories"),
                    List.of("night perception", "weakness at high stage", "soul spell volatility")),
            curse(CurseCoreApi.PHANTOM_BURN, "phantom_burn", CurseCategory.BODY,
                    List.of("heat with no flame", "ash halos", "water-shock response"),
                    List.of("brief fire resistance", "visibility flare at high stage", "ash spell tradeoffs")),
            curse(CurseCoreApi.BLOOD_DEBT, "blood_debt", CurseCategory.CONTRACT,
                    List.of("pulse ledger", "red static", "debt pressure"),
                    List.of("strength surge", "healing tax at high stage", "contract consequences")),
            curse(CurseCoreApi.VOID_MARK, "void_mark", CurseCategory.VOID,
                    List.of("black edge in vision", "wrong shadows", "portal attention"),
                    List.of("darkness", "movement drag", "void tracking risk")));

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        ArcanaCoreServices.registerProvider(INSTANCE);
        EchoCurseCore.LOGGER.info("ECHO: CurseCore registered Arcana Core curse provider.");
    }

    @Override
    public Identifier id() {
        return EchoCurseCore.id("arcana_provider/cursecore");
    }

    @Override
    public List<CurseDefinition> curses() {
        return CURSES;
    }

    @Override
    public List<Identifier> pageIds(Player player) {
        return List.of(
                Identifier.fromNamespaceAndPath("echoarcaneindex", "cursecore/echo_rot"),
                Identifier.fromNamespaceAndPath("echoarcaneindex", "cursecore/glass_veins"),
                Identifier.fromNamespaceAndPath("echoarcaneindex", "cursecore/rift_hunger"),
                Identifier.fromNamespaceAndPath("echoarcaneindex", "cursecore/soul_static"),
                Identifier.fromNamespaceAndPath("echoarcaneindex", "cursecore/phantom_burn"),
                Identifier.fromNamespaceAndPath("echoarcaneindex", "cursecore/blood_debt"),
                Identifier.fromNamespaceAndPath("echoarcaneindex", "cursecore/void_mark"),
                Identifier.fromNamespaceAndPath("echoarcaneindex", "cursecore/curse_cleansing_bridge"));
    }

    @Override
    public List<Identifier> grimoireEntryIds(Player player) {
        return List.of(
                Identifier.fromNamespaceAndPath("echogrimoire", "archive/curses/echo_rot"),
                Identifier.fromNamespaceAndPath("echogrimoire", "archive/curses/glass_veins"),
                Identifier.fromNamespaceAndPath("echogrimoire", "archive/curses/rift_hunger"),
                Identifier.fromNamespaceAndPath("echogrimoire", "archive/curses/soul_static"),
                Identifier.fromNamespaceAndPath("echogrimoire", "archive/curses/phantom_burn"),
                Identifier.fromNamespaceAndPath("echogrimoire", "archive/curses/blood_debt"),
                Identifier.fromNamespaceAndPath("echogrimoire", "archive/curses/void_mark"),
                Identifier.fromNamespaceAndPath("echogrimoire", "archive/curses/curse_cleansing_bridge"));
    }

    @Override
    public List<String> scanHints(Player player, Identifier targetId) {
        if (targetId == null || !EchoCurseCore.MODID.equals(targetId.getNamespace())) {
            return List.of();
        }
        if ("echo_rot_sample".equals(targetId.getPath())) {
            return List.of("Live curse sample", "Use to apply Echo Rot stage I for cleansing tests.");
        }
        return List.of("CurseCore diagnostic", "Active stages can be reduced by RitualCore Curse Cleansing I.");
    }

    @Override
    public List<Identifier> missionIds(Player player) {
        return List.of(
                EchoCurseCore.id("arcana_cursecore/gain_echo_rot"),
                EchoCurseCore.id("arcana_cursecore/cleanse_minor_curse"));
    }

    @Override
    public Map<String, String> terminalSummary(Player player) {
        Map<String, String> summary = new LinkedHashMap<>();
        summary.put("module", "ECHO: CurseCore");
        summary.put("active_curses", CurseCoreApi.summary(player));
        summary.put("live_targets", "echo_rot,glass_veins,rift_hunger,soul_static,phantom_burn,blood_debt,void_mark");
        summary.put("ritual_bridge", "echoritualcore:curse_cleansing_i");
        return Map.copyOf(summary);
    }

    private static CurseDefinition curse(Identifier id, String path, CurseCategory category,
            List<String> symptoms, List<String> effects) {
        return new CurseDefinition(
                id,
                "curse.echocursecore." + path,
                category,
                5,
                5,
                true,
                EchoCurseCore.id("discovery/" + path),
                symptoms,
                effects,
                List.of(Identifier.fromNamespaceAndPath("echoritualcore", "curse_cleansing_i")),
                null,
                EchoCurseCore.id("visual/" + path),
                EchoCurseCore.id("sound/" + path),
                Identifier.fromNamespaceAndPath("echoarcaneindex", "cursecore/" + path),
                Identifier.fromNamespaceAndPath("echogrimoire", "archive/curses/" + path));
    }
}
