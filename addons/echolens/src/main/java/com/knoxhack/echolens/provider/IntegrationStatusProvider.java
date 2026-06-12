package com.knoxhack.echolens.provider;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.knoxhack.echolens.EchoLens;
import com.knoxhack.echolens.api.IntegrationLensProvider;
import com.knoxhack.echolens.api.LensContext;
import com.knoxhack.echolens.api.LensDataCategory;
import com.knoxhack.echolens.api.LensInfoRow;
import com.knoxhack.echolens.api.LensInfoSection;
import com.knoxhack.echolens.api.LensTone;
import com.knoxhack.echolens.api.LensVisibility;
import com.knoxhack.echolens.platform.LensModuleAccess;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;

public enum IntegrationStatusProvider implements IntegrationLensProvider {
    INSTANCE;

    @Override
    public Identifier id() {
        return EchoLens.id("integration_status");
    }

    @Override
    public int priority() {
        return 700;
    }

    @Override
    public List<LensInfoSection> inspect(LensContext context) {
        List<LensInfoRow> rows = new ArrayList<>();
        rows.add(modRow("Terminal", LensModuleAccess.isLoaded("echoterminal"), "Archive/context hooks"));
        rows.add(modRow("Index", LensModuleAccess.isLoaded("echoindex"), "Recipe, use, and tracking shortcuts"));
        rows.add(modRow("RenderCore", LensModuleAccess.isLoaded("echorendercore"), "Optional HUD chrome"));
        rows.add(modRow("RuntimeGuard", LensModuleAccess.isLoaded("echoruntimeguard"), "Scan budgets"));
        rows.add(modRow("MissionCore", LensModuleAccess.isLoaded("echomissioncore"), "Lens side ops"));
        rows.add(modRow("HoloMap", LensModuleAccess.isLoaded("echoholomap"), "World markers and routes"));
        rows.add(modRow("WorldCore", LensModuleAccess.isLoaded("echoworldcore"), "Regions and hazards"));
        rows.add(modRow("MultiblockCore", LensModuleAccess.isLoaded("echomultiblockcore"), "Structure diagnostics"));
        rows.add(modRow("SoundCore", LensModuleAccess.isLoaded("echosoundcore"), "Optional scan feedback"));
        rows.add(modRow("ThemeCore", LensModuleAccess.isLoaded("echothemecore"), "CyberGlass styling"));
        rows.add(modRow("TutorialCore", LensModuleAccess.isLoaded("echotutorialcore"), "Onboarding hints"));
        rows.add(modRow("DataCore", LensModuleAccess.isLoaded("echodatacore"), "Public progression context"));
        rows.add(modRow("Ashfall", LensModuleAccess.isLoaded("echoashfallprotocol"), "Hazard/progression relevance"));
        rows.add(LensInfoRow.of("Missions", EchoCoreServices.missionCoreAvailable() ? "Available" : "No service",
                "◆", EchoCoreServices.missionCoreAvailable() ? LensTone.GOOD : LensTone.MUTED, LensVisibility.DEEP));
        return List.of(LensInfoSection.of(EchoLens.id("section/integrations"), LensDataCategory.INTEGRATION,
                "ECHO Links", "⌁", LensTone.ECHO, LensVisibility.DEEP, rows));
    }

    private static LensInfoRow modRow(String label, boolean loaded, String detail) {
        return LensInfoRow.of(label, loaded ? "Online - " + detail : "Offline", loaded ? "✓" : "-",
                loaded ? LensTone.GOOD : LensTone.MUTED, LensVisibility.DEEP);
    }
}
