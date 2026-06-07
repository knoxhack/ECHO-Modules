package com.knoxhack.echocursecore.integration.lens;

import com.knoxhack.echocursecore.EchoCurseCore;
import com.knoxhack.echocursecore.api.CurseCoreApi;
import com.knoxhack.echolens.api.LensContext;
import com.knoxhack.echolens.api.LensDataCategory;
import com.knoxhack.echolens.api.LensInfoRow;
import com.knoxhack.echolens.api.LensInfoSection;
import com.knoxhack.echolens.api.LensTone;
import com.knoxhack.echolens.api.LensVisibility;
import com.knoxhack.echolens.api.ServerLensProvider;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

enum CurseCoreLensProvider implements ServerLensProvider {
    INSTANCE;

    @Override
    public Identifier id() {
        return EchoCurseCore.id("cursecore_scan");
    }

    @Override
    public int priority() {
        return 87;
    }

    @Override
    public LensDataCategory category() {
        return LensDataCategory.INTEGRATION;
    }

    @Override
    public boolean supports(LensContext context) {
        return context != null && context.hasEntity() && context.entity() instanceof Player player
                && !CurseCoreApi.activeCurses(player).isEmpty();
    }

    @Override
    public List<LensInfoSection> inspect(LensContext context) {
        Player player = (Player) context.entity();
        List<LensInfoRow> rows = new ArrayList<>();
        CurseCoreApi.activeCurses(player).forEach((curse, stage) ->
                rows.add(row(curse.getPath().replace("curse/", ""), "stage " + stage, "!",
                        stage >= 3 ? LensTone.DANGER : LensTone.WARNING, LensVisibility.COMPACT)));
        rows.add(row("Cleansing", "RitualCore Curse Cleansing I", "R", LensTone.INFO, LensVisibility.EXPANDED));
        return List.of(LensInfoSection.of(
                EchoCurseCore.id("lens/cursecore"),
                LensDataCategory.INTEGRATION,
                "ECHO CurseCore",
                "!",
                LensTone.WARNING,
                LensVisibility.COMPACT,
                rows));
    }

    private static LensInfoRow row(String label, String value, String icon, LensTone tone, LensVisibility visibility) {
        return LensInfoRow.of(label, value, icon, tone, visibility);
    }
}
