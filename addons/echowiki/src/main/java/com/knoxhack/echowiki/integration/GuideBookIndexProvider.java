package com.knoxhack.echowiki.integration;

import com.knoxhack.echocore.api.index.IIndexContentProvider;
import com.knoxhack.echocore.api.index.IndexBuildContext;
import com.knoxhack.echocore.api.index.IndexCategory;
import com.knoxhack.echocore.api.index.IndexContentSnapshot;
import com.knoxhack.echocore.api.index.IndexEntry;
import com.knoxhack.echocore.api.index.IndexEntryState;
import com.knoxhack.echowiki.EchoWiki;
import com.knoxhack.echowiki.content.GuideBookDefinition;
import com.knoxhack.echowiki.content.GuideBookLabels;
import com.knoxhack.echowiki.content.GuideBookRegistry;
import com.knoxhack.echowiki.item.GuideBookStacks;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public enum GuideBookIndexProvider implements IIndexContentProvider {
    INSTANCE;

    private static final Identifier CATEGORY = EchoWiki.id("index/guide_books");

    @Override
    public Identifier id() {
        return EchoWiki.id("index/provider/guide_books");
    }

    @Override
    public IndexContentSnapshot snapshot(IndexBuildContext context) {
        List<GuideBookDefinition> guides = GuideBookRegistry.visibleGuideBooks();
        List<IndexCategory> categories = List.of(new IndexCategory(
                CATEGORY,
                "Guide Books",
                "Physical ECHO manuals backed by Wiki chapters.",
                guideIcon(guides.stream().findFirst().orElse(null)),
                75,
                EchoWiki.MODID));
        List<IndexEntry> entries = guides.stream()
                .map(GuideBookIndexProvider::entry)
                .toList();
        return new IndexContentSnapshot(id(), categories, entries, List.of(), List.of(), List.of(), List.of(), List.of());
    }

    private static IndexEntry entry(GuideBookDefinition guide) {
        return new IndexEntry(
                EchoWiki.id("guide_book/" + sanitize(guide.id())),
                CATEGORY,
                guide.title(),
                guide.subtitle(),
                guide.summary(),
                body(guide),
                GuideBookStacks.stackFor(guide),
                guide.requiredModId(),
                guide.tags(),
                IndexEntryState.VISIBLE,
                guide.allArticleIds(),
                List.of(EchoWiki.id("guide_book")),
                List.of(),
                guide.sortOrder());
    }

    private static ItemStack guideIcon(GuideBookDefinition guide) {
        return guide == null ? ItemStack.EMPTY : GuideBookStacks.stackFor(guide);
    }

    private static String body(GuideBookDefinition guide) {
        return Component.translatable("index.echowiki.guide_book.body",
                guide.title(),
                GuideBookLabels.chapterCountComponent(guide.allArticleIds().size()),
                guide.collectionId() == null ? "none" : guide.collectionId().toString()).getString();
    }

    private static String sanitize(Identifier id) {
        return id.getNamespace() + "/" + id.getPath().replace(':', '/');
    }
}
