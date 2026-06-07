package com.knoxhack.echodatacore.integration;

import com.knoxhack.echocore.api.DataKeyMetadata;
import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.index.IIndexContentProvider;
import com.knoxhack.echocore.api.index.IndexBuildContext;
import com.knoxhack.echocore.api.index.IndexCategory;
import com.knoxhack.echocore.api.index.IndexContentBuilder;
import com.knoxhack.echocore.api.index.IndexContentSnapshot;
import com.knoxhack.echocore.api.index.IndexEntry;
import com.knoxhack.echocore.api.index.IndexEntryState;
import com.knoxhack.echocore.api.index.IndexProviderDiagnostic;
import com.knoxhack.echocore.api.index.IndexSourceFact;
import com.knoxhack.echocore.api.index.IndexSourceKind;
import com.knoxhack.echodatacore.DataCoreDataService;
import com.knoxhack.echodatacore.EchoDataCore;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public enum DataCoreIndexProvider implements IIndexContentProvider {
    INSTANCE;

    private static final Identifier CATEGORY = id("category/data_keys");
    private static final int MAX_ENTRIES = 256;

    public static void register() {
        EchoCoreServices.registerIndexContentProvider(INSTANCE);
    }

    @Override
    public Identifier id() {
        return id("provider/data_core_catalog");
    }

    @Override
    public IndexContentSnapshot snapshot(IndexBuildContext context) {
        IndexContentBuilder builder = IndexContentBuilder.create(id());
        builder.registerCategory(new IndexCategory(
                CATEGORY,
                "Data Core Catalog",
                "Registered shared player, world, and team data contracts.",
                new ItemStack(Items.WRITABLE_BOOK),
                145,
                EchoDataCore.MODID));
        List<DataKeyMetadata> metadata = DataCoreDataService.INSTANCE.allKeyMetadata().values().stream()
                .sorted(Comparator.comparing(meta -> meta.id().toString()))
                .limit(MAX_ENTRIES)
                .toList();
        List<IndexSourceFact> facts = new ArrayList<>();
        int order = 0;
        for (DataKeyMetadata meta : metadata) {
            builder.registerEntry(entry(meta, order++));
            facts.add(sourceFact(meta));
        }
        builder.addSourceFacts(facts);
        int total = DataCoreDataService.INSTANCE.allKeyMetadata().size();
        if (total > MAX_ENTRIES) {
            builder.addDiagnostics(List.of(IndexProviderDiagnostic.warning(id(),
                    "DataCore catalog truncated to " + MAX_ENTRIES + " of " + total + " metadata entries.")));
        } else {
            builder.addDiagnostics(List.of(IndexProviderDiagnostic.info(id(),
                    "DataCore catalog published " + total + " metadata entries.")));
        }
        return builder.snapshot();
    }

    private static IndexEntry entry(DataKeyMetadata meta, int order) {
        String title = meta.title().isBlank() ? meta.id().toString() : meta.title();
        String summary = meta.scope() + " " + meta.kind()
                + (meta.synced() ? " synced" : " server-only")
                + " owned by " + meta.owner();
        String legacy = meta.legacyRoot().isBlank()
                ? "No legacy mirror root is declared."
                : "Legacy mirror: " + meta.legacyRoot()
                        + (meta.legacyField().isBlank() ? "" : "." + meta.legacyField());
        String body = summary + ". Default: " + meta.defaultValue()
                + ". Source: " + meta.source() + ". " + legacy;
        return new IndexEntry(
                id("key/" + sanitize(meta.id().getNamespace() + "/" + meta.id().getPath())),
                CATEGORY,
                title,
                meta.id().toString(),
                summary,
                body,
                new ItemStack(Items.PAPER),
                EchoDataCore.MODID,
                List.of("datacore", meta.scope().name().toLowerCase(Locale.ROOT),
                        meta.kind().name().toLowerCase(Locale.ROOT), meta.owner()),
                IndexEntryState.VISIBLE,
                List.of(),
                List.of(),
                List.of(),
                order);
    }

    private static IndexSourceFact sourceFact(DataKeyMetadata meta) {
        List<String> notes = new ArrayList<>();
        notes.add(meta.scope() + " " + meta.kind() + (meta.synced() ? " synced" : " server-only"));
        notes.add("Owner: " + meta.owner());
        if (!meta.legacyRoot().isBlank()) {
            notes.add("Legacy: " + meta.legacyRoot()
                    + (meta.legacyField().isBlank() ? "" : "." + meta.legacyField()));
        }
        return new IndexSourceFact(
                Identifier.withDefaultNamespace("paper"),
                meta.id(),
                IndexSourceKind.SOURCE_CARD,
                meta.title().isBlank() ? meta.id().toString() : meta.title(),
                notes,
                new ItemStack(Items.PAPER),
                EchoDataCore.MODID);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoDataCore.MODID, sanitize(path));
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
