package com.knoxhack.echowiki.client;

import com.knoxhack.echowiki.content.WikiArticleDefinition;
import com.knoxhack.echowiki.content.GuideBookDefinition;
import com.knoxhack.echowiki.content.GuideBookRegistry;
import com.knoxhack.echowiki.content.WikiContentRegistry;
import net.minecraft.resources.Identifier;

public final class WikiUiState {
    public static final WikiUiState INSTANCE = new WikiUiState();
    public static final int ARTICLE_PAGE_SIZE = 24;
    public static final int GUIDE_BOOK_PAGE_SIZE = 12;

    private Identifier currentPage = WikiScreenCorePages.DASHBOARD;
    private Identifier selectedArticle;
    private Identifier selectedGuideBook;
    private Identifier selectedRegion;
    private Identifier selectedHazard;
    private Identifier selectedMission;
    private Identifier selectedFaction;
    private Identifier selectedDiscovery;
    private String searchQuery = "";
    private String category = "All";
    private int articlePage;
    private int guideBookPage;

    private WikiUiState() {
    }

    public Identifier currentPage() {
        return currentPage;
    }

    public void currentPage(Identifier currentPage) {
        this.currentPage = currentPage == null ? WikiScreenCorePages.DASHBOARD : currentPage;
    }

    public Identifier selectedArticle() {
        if (selectedArticle != null && WikiContentRegistry.article(selectedArticle).isPresent()) {
            return selectedArticle;
        }
        return WikiContentRegistry.articles().stream()
                .findFirst()
                .map(WikiArticleDefinition::id)
                .orElse(null);
    }

    public void selectedArticle(Identifier selectedArticle) {
        this.selectedArticle = selectedArticle;
    }

    public Identifier selectedGuideBook() {
        if (selectedGuideBook != null && GuideBookRegistry.visibleGuideBook(selectedGuideBook).isPresent()) {
            return selectedGuideBook;
        }
        return GuideBookRegistry.visibleGuideBooks().stream()
                .findFirst()
                .map(GuideBookDefinition::id)
                .orElse(null);
    }

    public void selectedGuideBook(Identifier selectedGuideBook) {
        this.selectedGuideBook = selectedGuideBook;
    }

    public boolean selectVisibleGuideBook(Identifier selectedGuideBook) {
        if (GuideBookRegistry.visibleGuideBook(selectedGuideBook).isPresent()) {
            this.selectedGuideBook = selectedGuideBook;
            return true;
        }
        return false;
    }

    public void selectFirstVisibleGuideBook() {
        this.selectedGuideBook = GuideBookRegistry.visibleGuideBooks().stream()
                .findFirst()
                .map(GuideBookDefinition::id)
                .orElse(null);
    }

    public Identifier selectedRegion() {
        return selectedRegion;
    }

    public void selectedRegion(Identifier selectedRegion) {
        this.selectedRegion = selectedRegion;
    }

    public Identifier selectedHazard() {
        return selectedHazard;
    }

    public void selectedHazard(Identifier selectedHazard) {
        this.selectedHazard = selectedHazard;
    }

    public Identifier selectedMission() {
        return selectedMission;
    }

    public void selectedMission(Identifier selectedMission) {
        this.selectedMission = selectedMission;
    }

    public Identifier selectedFaction() {
        return selectedFaction;
    }

    public void selectedFaction(Identifier selectedFaction) {
        this.selectedFaction = selectedFaction;
    }

    public Identifier selectedDiscovery() {
        return selectedDiscovery;
    }

    public void selectedDiscovery(Identifier selectedDiscovery) {
        this.selectedDiscovery = selectedDiscovery;
    }

    public String searchQuery() {
        return searchQuery;
    }

    public void searchQuery(String searchQuery) {
        this.searchQuery = searchQuery == null ? "" : searchQuery.strip();
        resetPagedLists();
    }

    public String category() {
        return category;
    }

    public void category(String category) {
        String clean = category == null ? "" : category.strip();
        this.category = clean.isBlank() ? "All" : clean;
        resetPagedLists();
    }

    public void clearFilters() {
        searchQuery = "";
        category = "All";
        resetPagedLists();
    }

    public int articlePage() {
        return articlePage;
    }

    public void articlePage(int articlePage) {
        this.articlePage = Math.max(0, articlePage);
    }

    public void nextArticlePage() {
        articlePage++;
    }

    public void previousArticlePage() {
        articlePage = Math.max(0, articlePage - 1);
    }

    public int guideBookPage() {
        return guideBookPage;
    }

    public void guideBookPage(int guideBookPage) {
        this.guideBookPage = Math.max(0, guideBookPage);
    }

    public void nextGuideBookPage() {
        guideBookPage++;
    }

    public void previousGuideBookPage() {
        guideBookPage = Math.max(0, guideBookPage - 1);
    }

    public void resetPagedLists() {
        articlePage = 0;
        guideBookPage = 0;
    }
}
