package com.knoxhack.echoindex.client;

import com.knoxhack.echoindex.Config;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.resources.Identifier;

public final class IndexUiState {
    public static final IndexUiState INSTANCE = new IndexUiState();

    private final IndexFilterState filters = new IndexFilterState();
    private final IndexSelectionState selection = new IndexSelectionState();
    private final IndexHistoryState history = new IndexHistoryState();
    private final Set<String> collapsedModSections = new LinkedHashSet<>();
    private Identifier currentPage = IndexScreenCorePages.DASHBOARD;
    private String searchQuery = "";
    private long revision;

    private IndexUiState() {
    }

    public IndexFilterState filters() {
        return filters;
    }

    public IndexSelectionState selection() {
        return selection;
    }

    public IndexHistoryState history() {
        return history;
    }

    public Identifier currentPage() {
        return currentPage;
    }

    public String searchQuery() {
        return searchQuery;
    }

    public long revision() {
        return revision + filters.revision() + selection.revision();
    }

    public void setCurrentPage(Identifier page) {
        if (page != null) {
            currentPage = page;
            if (Config.UI_REMEMBER_LAST_PAGE.get()) {
                IndexFavoriteStore.setSetting("lastPage", page.toString());
            }
            revision++;
        }
    }

    public Identifier startPage() {
        if (!Config.UI_REMEMBER_LAST_PAGE.get()) {
            return IndexScreenCorePages.DASHBOARD;
        }
        Identifier stored = Identifier.tryParse(IndexFavoriteStore.setting("lastPage"));
        return stored == null ? IndexScreenCorePages.DASHBOARD : stored;
    }

    public void setSearchQuery(String query) {
        searchQuery = query == null ? "" : query.strip();
        revision++;
    }

    public boolean isCollapsed(String modId) {
        return collapsedModSections.contains(clean(modId));
    }

    public Set<String> collapsedModSections() {
        return Set.copyOf(collapsedModSections);
    }

    public void toggleModSection(String modId) {
        String clean = clean(modId);
        if (clean.isBlank()) {
            return;
        }
        if (!collapsedModSections.add(clean)) {
            collapsedModSections.remove(clean);
        }
        revision++;
    }

    public void resetFilters() {
        filters.reset();
        searchQuery = "";
        revision++;
    }

    private static String clean(String value) {
        return value == null ? "" : value.strip().toLowerCase(java.util.Locale.ROOT);
    }
}
