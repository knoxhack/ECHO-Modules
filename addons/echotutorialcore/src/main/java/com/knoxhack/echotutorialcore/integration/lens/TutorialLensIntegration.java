package com.knoxhack.echotutorialcore.integration.lens;

import com.knoxhack.echolens.api.LensContext;
import com.knoxhack.echolens.api.LensDataCategory;
import com.knoxhack.echolens.api.LensInfoRow;
import com.knoxhack.echolens.api.LensInfoSection;
import com.knoxhack.echolens.api.LensTone;
import com.knoxhack.echolens.api.LensVisibility;
import com.knoxhack.echolens.api.ServerLensProvider;
import com.knoxhack.echolens.registry.LensProviderRegistry;
import com.knoxhack.echotutorialcore.EchoTutorialCore;
import com.knoxhack.echotutorialcore.api.TutorialCoreApi;
import com.knoxhack.echotutorialcore.data.TutorialPlayerData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public final class TutorialLensIntegration {
    private TutorialLensIntegration() {}

    public static void register() {
        LensProviderRegistry.register(Provider.INSTANCE);
        EchoTutorialCore.LOGGER.info("ECHO: TutorialCore integrated with Lens. Tutorial hint provider registered.");
    }

    private enum Provider implements ServerLensProvider {
        INSTANCE;

        @Override
        public Identifier id() {
            return Identifier.fromNamespaceAndPath(EchoTutorialCore.MODID, "lens/tutorial_hints");
        }

        @Override
        public int priority() {
            return 720;
        }

        @Override
        public LensDataCategory category() {
            return LensDataCategory.HINTS;
        }

        @Override
        public List<LensInfoSection> inspect(LensContext context) {
            if (context == null || context.player() == null) {
                return List.of();
            }
            Identifier target = targetId(context);
            TutorialCoreApi.reportLensScan(context.player(), target);
            TutorialPlayerData data = TutorialPlayerData.get(context.player());
            List<LensInfoRow> rows = new ArrayList<>();
            rows.add(row("Guide", data.guideMode().name(), "?", LensTone.ECHO, LensVisibility.COMPACT));
            rows.add(row("Unread Cards", String.valueOf(data.unreadCardCount()), "G",
                    data.unreadCardCount() > 0 ? LensTone.WARNING : LensTone.GOOD, LensVisibility.COMPACT));
            if (context.hasBlock()) {
                rows.add(row("Machine Check", "Power, input, output, filter", "M", LensTone.INFO, LensVisibility.EXPANDED));
            } else if (context.hasEntity()) {
                rows.add(row("Field Read", "Scan routes before committing", "R", LensTone.INFO, LensVisibility.EXPANDED));
            } else {
                rows.add(row("Unknown Target", "Deep Scan for context", "D", LensTone.WARNING, LensVisibility.COMPACT));
            }
            if (!data.lastPowerAlert().isBlank()) {
                rows.add(row("Power Alert", data.lastPowerAlert(), "!", LensTone.DANGER, LensVisibility.COMPACT));
            }
            return List.of(LensInfoSection.of(id(), LensDataCategory.HINTS, "ECHO-7 Tutorial", "?", LensTone.ECHO,
                    LensVisibility.COMPACT, rows));
        }

        @Override
        public List<LensInfoRow> deepScanSignals(LensContext context) {
            if (context == null || context.player() == null) {
                return List.of();
            }
            TutorialPlayerData data = TutorialPlayerData.get(context.player());
            if (!data.hasProgress(Identifier.fromNamespaceAndPath(EchoTutorialCore.MODID, "opened_holomap"))) {
                return List.of(row("Tutorial", "Open HoloMap after scan", "G", LensTone.INFO, LensVisibility.DEEP));
            }
            return List.of(row("Tutorial", "Guide state synced", "G", LensTone.GOOD, LensVisibility.DEEP));
        }

        private static LensInfoRow row(String label, String value, String icon, LensTone tone, LensVisibility visibility) {
            return LensInfoRow.of(label, value == null || value.isBlank() ? "-" : value, icon, tone, visibility);
        }

        private static Identifier targetId(LensContext context) {
            if (context.hasBlock()) {
                return BuiltInRegistries.BLOCK.getKey(context.blockState().getBlock());
            }
            if (context.hasEntity()) {
                return BuiltInRegistries.ENTITY_TYPE.getKey(context.entity().getType());
            }
            return Identifier.fromNamespaceAndPath(EchoTutorialCore.MODID, "unknown_lens_target");
        }
    }
}
