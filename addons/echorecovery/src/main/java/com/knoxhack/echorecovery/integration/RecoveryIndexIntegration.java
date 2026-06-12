package com.knoxhack.echorecovery.integration;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.index.IIndexContentProvider;
import com.echoplatform.echocore.api.index.IIndexRegistry;
import com.echoplatform.echocore.api.index.IndexBuildContext;
import com.echoplatform.echocore.api.index.IndexCategory;
import com.echoplatform.echocore.api.index.IndexContentBuilder;
import com.echoplatform.echocore.api.index.IndexContentSnapshot;
import com.echoplatform.echocore.api.index.IndexEntry;
import com.echoplatform.echocore.api.index.IndexEntryState;
import com.knoxhack.echorecovery.EchoRecovery;
import com.knoxhack.echorecovery.registry.ModBlocks;
import com.knoxhack.echorecovery.registry.ModItems;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public final class RecoveryIndexIntegration {
    private static boolean registered;

    private RecoveryIndexIntegration() {}

    public static void registerCommon() {
        if (registered) {
            return;
        }
        registered = true;
        EchoCoreServices.registerIndexContentProvider(Provider.INSTANCE);
        EchoRecovery.LOGGER.info("Recovery Index entries registered.");
    }

    private enum Provider implements IIndexContentProvider {
        INSTANCE;

        private static final Identifier CATEGORY = RecoveryIndexIntegration.id("category/recovery");

        @Override
        public Identifier id() {
            return RecoveryIndexIntegration.id("provider/index_entries");
        }

        @Override
        public IndexContentSnapshot snapshot(IndexBuildContext context) {
            IndexContentBuilder builder = IndexContentBuilder.create(id());
            register(builder);
            return builder.snapshot();
        }

        public void register(IIndexRegistry registry) {
            registry.registerCategory(new IndexCategory(CATEGORY,
                    "index.echorecovery.category.title",
                    "index.echorecovery.category.desc",
                    new ItemStack(ModBlocks.GRAVE.asItem()),
                    420,
                    EchoRecovery.MODID));
            entry(registry, "graves", "index.echorecovery.graves", new ItemStack(ModBlocks.GRAVE.asItem()), 1);
            entry(registry, "grave_key", "index.echorecovery.grave_key", new ItemStack(ModItems.GRAVE_KEY.get()), 2);
            entry(registry, "recovery_compass", "index.echorecovery.recovery_compass", new ItemStack(ModItems.RECOVERY_COMPASS.get()), 3);
            entry(registry, "rules", "index.echorecovery.rules", new ItemStack(ModItems.DEATH_RECORD.get()), 4);
            entry(registry, "presets", "index.echorecovery.presets", new ItemStack(ModItems.RECOVERY_TOKEN.get()), 5);
        }

        private static void entry(IIndexRegistry registry, String path, String keyPrefix, ItemStack icon, int order) {
            registry.registerEntry(new IndexEntry(
                    RecoveryIndexIntegration.id("entry/" + path),
                    CATEGORY,
                    keyPrefix + ".title",
                    keyPrefix + ".subtitle",
                    keyPrefix + ".summary",
                    keyPrefix + ".body",
                    icon,
                    EchoRecovery.MODID,
                    List.of("recovery", "grave", "death"),
                    IndexEntryState.VISIBLE,
                    List.of(),
                    List.of(),
                    List.of(),
                    order));
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoRecovery.MODID, path);
    }
}
