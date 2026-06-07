package com.knoxhack.echorecovery.integration;

import com.knoxhack.echolens.api.LensContext;
import com.knoxhack.echolens.api.LensDataCategory;
import com.knoxhack.echolens.api.LensInfoRow;
import com.knoxhack.echolens.api.LensInfoSection;
import com.knoxhack.echolens.api.LensTone;
import com.knoxhack.echolens.api.LensVisibility;
import com.knoxhack.echolens.api.ServerLensProvider;
import com.knoxhack.echolens.registry.LensProviderRegistry;
import com.knoxhack.echorecovery.EchoRecovery;
import com.knoxhack.echorecovery.block.entity.GraveBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;

public final class RecoveryLensIntegration {
    private static boolean registered;

    private RecoveryLensIntegration() {}

    public static void registerCommon() {
        if (registered) {
            return;
        }
        registered = true;
        LensProviderRegistry.register(Provider.INSTANCE);
        EchoRecovery.LOGGER.info("Recovery Lens provider registered.");
    }

    public enum Provider implements ServerLensProvider {
        INSTANCE;

        @Override
        public Identifier id() {
            return Identifier.fromNamespaceAndPath(EchoRecovery.MODID, "lens/grave_status");
        }

        @Override
        public int priority() {
            return 80;
        }

        @Override
        public LensDataCategory category() {
            return LensDataCategory.INTEGRATION;
        }

        @Override
        public boolean supports(LensContext context) {
            return context != null && context.hasBlock()
                    && context.level().getBlockEntity(context.blockPos()) instanceof GraveBlockEntity;
        }

        @Override
        public List<LensInfoSection> inspect(LensContext context) {
            if (!(context.level().getBlockEntity(context.blockPos()) instanceof GraveBlockEntity grave)) {
                return List.of();
            }
            List<LensInfoRow> rows = new ArrayList<>();
            rows.add(LensInfoRow.of("Owner", grave.ownerName().isBlank() ? "Unknown" : grave.ownerName(), "O",
                    LensTone.INFO, LensVisibility.COMPACT));
            rows.add(LensInfoRow.of("Access", grave.isRecovered() ? "Recovered" : grave.isExpired() ? "Expired" : "Protected", "A",
                    grave.isExpired() ? LensTone.WARNING : LensTone.GOOD, LensVisibility.COMPACT));
            rows.add(LensInfoRow.of("Stored", grave.itemCount() + " items, " + grave.xpStored() + " XP", "I",
                    LensTone.INFO, LensVisibility.EXPANDED));
            rows.add(LensInfoRow.of("Type", grave.graveTypeId(), "T", LensTone.MUTED, LensVisibility.EXPANDED));
            if (grave.expiresAt() > 0L) {
                long remaining = Math.max(0L, (grave.expiresAt() - System.currentTimeMillis()) / 60000L);
                rows.add(LensInfoRow.of("Expiry", remaining + " minutes", "E",
                        remaining == 0L ? LensTone.WARNING : LensTone.INFO, LensVisibility.EXPANDED));
            }
            if (grave.contaminated()) {
                rows.add(LensInfoRow.of("Contamination", "Cosmetic warning; contents protected", "!",
                        LensTone.WARNING, LensVisibility.DEEP));
            }
            for (String note : grave.hazardNoteList()) {
                rows.add(LensInfoRow.of("Recovery Note", note, "R", LensTone.MUTED, LensVisibility.DEEP));
            }
            return List.of(LensInfoSection.of(id(), LensDataCategory.INTEGRATION, "Recovery Cache", "G",
                    grave.isExpired() ? LensTone.WARNING : LensTone.ECHO, LensVisibility.COMPACT, rows));
        }

        @Override
        public List<LensInfoRow> deepScanSignals(LensContext context) {
            if (!supports(context)) {
                return List.of();
            }
            return List.of(LensInfoRow.of("Recovery", "Server verified grave/cache data", "G",
                    LensTone.ECHO, LensVisibility.DEEP));
        }
    }
}
