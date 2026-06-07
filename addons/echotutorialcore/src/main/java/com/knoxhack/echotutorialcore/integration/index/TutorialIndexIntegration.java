package com.knoxhack.echotutorialcore.integration.index;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.index.IIndexContentProvider;
import com.knoxhack.echocore.api.index.IndexBuildContext;
import com.knoxhack.echocore.api.index.IndexCategory;
import com.knoxhack.echocore.api.index.IndexContentBuilder;
import com.knoxhack.echocore.api.index.IndexContentSnapshot;
import com.knoxhack.echocore.api.index.IndexEntry;
import com.knoxhack.echocore.api.index.IndexEntryState;
import com.knoxhack.echocore.api.index.IndexProviderDiagnostic;
import com.knoxhack.echotutorialcore.EchoTutorialCore;
import com.knoxhack.echotutorialcore.api.card.TutorialCard;
import com.knoxhack.echotutorialcore.data.TutorialCoreRegistries;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class TutorialIndexIntegration {
    private static boolean registered;

    private TutorialIndexIntegration() {}

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        EchoCoreServices.registerIndexContentProvider(Provider.INSTANCE);
        EchoTutorialCore.LOGGER.info("ECHO: TutorialCore integrated with Index. Guide card provider registered.");
    }

    private enum Provider implements IIndexContentProvider {
        INSTANCE;

        private static final Identifier CATEGORY = TutorialIndexIntegration.id("category/guide_cards");

        @Override
        public Identifier id() {
            return TutorialIndexIntegration.id("provider/guide_cards");
        }

        @Override
        public IndexContentSnapshot snapshot(IndexBuildContext context) {
            IndexContentBuilder builder = IndexContentBuilder.create(id());
            builder.registerCategory(new IndexCategory(
                    CATEGORY,
                    "ECHO-7 Guide",
                    "Tutorial cards, route prep, troubleshooting, and onboarding references.",
                    new ItemStack(Items.BOOK),
                    45,
                    EchoTutorialCore.MODID));

            int order = 0;
            for (TutorialCard card : TutorialCoreRegistries.allCards()) {
                builder.registerEntry(entry(card, order++));
            }
            builder.addDiagnostics(List.of(IndexProviderDiagnostic.info(id(),
                    "Published " + TutorialCoreRegistries.cardCount() + " TutorialCore guide cards.")));
            return builder.snapshot();
        }

        private static IndexEntry entry(TutorialCard card, int order) {
            List<String> tags = new ArrayList<>();
            tags.add("tutorial");
            tags.add(card.category().name().toLowerCase(Locale.ROOT));
            tags.add(card.addonOwnerId());
            String body = String.join("\n", card.body());
            if (!card.steps().isEmpty()) {
                body = body + "\nSteps: " + String.join("; ", card.steps());
            }
            return new IndexEntry(
                    TutorialIndexIntegration.id("card/" + sanitize(card.id().getNamespace() + "/" + card.id().getPath())),
                    CATEGORY,
                    card.title(),
                    card.id().toString(),
                    card.summary(),
                    body,
                    new ItemStack(Items.PAPER),
                    EchoTutorialCore.MODID,
                    tags,
                    card.defaultUnlocked() ? IndexEntryState.VISIBLE : IndexEntryState.LOCKED,
                    card.related(),
                    List.of(),
                    List.of(),
                    order);
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoTutorialCore.MODID, sanitize(path));
    }

    private static String sanitize(String path) {
        String clean = path == null ? "unknown" : path.trim().toLowerCase(Locale.ROOT);
        clean = clean.replace('\\', '/').replace(':', '/').replaceAll("[^a-z0-9_./-]", "_");
        while (clean.contains("//")) {
            clean = clean.replace("//", "/");
        }
        return clean.isBlank() ? "unknown" : clean;
    }
}
