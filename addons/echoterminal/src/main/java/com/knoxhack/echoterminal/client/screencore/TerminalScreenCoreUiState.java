package com.knoxhack.echoterminal.client.screencore;

import net.minecraft.resources.Identifier;

/**
 * Client-only ScreenCore selections and filters. Gameplay authority stays in the
 * existing terminal services and server action handlers.
 */
final class TerminalScreenCoreUiState {
    private static final TerminalScreenCoreUiState INSTANCE = new TerminalScreenCoreUiState();

    private Identifier selectedMissionId;
    private Identifier selectedRecipeId;
    private Identifier selectedRouteRecordId;
    private Identifier selectedDiscoveryId;
    private Identifier selectedFactionId;
    private Identifier selectedArchiveId;
    private String selectedAddonId = "";
    private String selectedRewardId = "";
    private String selectedMissionProviderId = "";
    private String missionSearch = "";
    private String missionProviderFilter = "all";
    private String recipeSearch = "";
    private String recipeMode = "recipes";
    private String recipeCategory = "all";
    private String routeFilter = "all";
    private String diagnosticsChapterFilter = "all";
    private String discoveryCategory = "all";
    private String discoveryState = "all";
    private String factionNamespace = "all";
    private String archiveState = "all";
    private String archiveGroup = "all";
    private boolean rewardDeferred;
    private boolean rewardViewed;

    private TerminalScreenCoreUiState() {
    }

    static TerminalScreenCoreUiState current() {
        return INSTANCE;
    }

    Identifier selectedMissionId() {
        return selectedMissionId;
    }

    void selectMission(Identifier value) {
        selectedMissionId = value;
    }

    Identifier selectedRecipeId() {
        return selectedRecipeId;
    }

    void selectRecipe(Identifier value) {
        selectedRecipeId = value;
    }

    Identifier selectedRouteRecordId() {
        return selectedRouteRecordId;
    }

    void selectRouteRecord(Identifier value) {
        selectedRouteRecordId = value;
    }

    Identifier selectedDiscoveryId() {
        return selectedDiscoveryId;
    }

    void selectDiscovery(Identifier value) {
        selectedDiscoveryId = value;
    }

    Identifier selectedFactionId() {
        return selectedFactionId;
    }

    void selectFaction(Identifier value) {
        selectedFactionId = value;
    }

    Identifier selectedArchiveId() {
        return selectedArchiveId;
    }

    void selectArchive(Identifier value) {
        selectedArchiveId = value;
    }

    String selectedAddonId() {
        return selectedAddonId;
    }

    void selectAddon(String value) {
        selectedAddonId = clean(value);
    }

    String selectedRewardId() {
        return selectedRewardId;
    }

    void selectReward(String value) {
        selectedRewardId = clean(value);
    }

    boolean rewardDeferred() {
        return rewardDeferred;
    }

    void rewardDeferred(boolean value) {
        rewardDeferred = value;
        if (value) {
            rewardViewed = false;
        }
    }

    boolean rewardViewed() {
        return rewardViewed;
    }

    void rewardViewed(boolean value) {
        rewardViewed = value;
        if (value) {
            rewardDeferred = false;
        }
    }

    String selectedMissionProviderId() {
        return selectedMissionProviderId;
    }

    void selectMissionProvider(String value) {
        selectedMissionProviderId = clean(value);
    }

    String missionSearch() {
        return missionSearch;
    }

    void missionSearch(String value) {
        missionSearch = clean(value);
    }

    String missionProviderFilter() {
        return missionProviderFilter;
    }

    void missionProviderFilter(String value) {
        missionProviderFilter = clean(value, "all");
    }

    String recipeSearch() {
        return recipeSearch;
    }

    void recipeSearch(String value) {
        recipeSearch = clean(value);
    }

    String recipeMode() {
        return recipeMode;
    }

    void recipeMode(String value) {
        String cleaned = clean(value);
        recipeMode = switch (cleaned) {
            case "uses", "sources", "info" -> cleaned;
            default -> "recipes";
        };
    }

    String recipeCategory() {
        return recipeCategory;
    }

    void recipeCategory(String value) {
        recipeCategory = clean(value, "all");
    }

    static boolean isRecipeMode(String value) {
        return switch (clean(value)) {
            case "recipes", "uses", "sources", "info" -> true;
            default -> false;
        };
    }

    String routeFilter() {
        return routeFilter;
    }

    void routeFilter(String value) {
        routeFilter = clean(value, "all");
    }

    String diagnosticsChapterFilter() {
        return diagnosticsChapterFilter;
    }

    void diagnosticsChapterFilter(String value) {
        diagnosticsChapterFilter = clean(value, "all");
    }

    String discoveryCategory() {
        return discoveryCategory;
    }

    void discoveryCategory(String value) {
        discoveryCategory = clean(value, "all");
    }

    String discoveryState() {
        return discoveryState;
    }

    void discoveryState(String value) {
        discoveryState = clean(value, "all");
    }

    String factionNamespace() {
        return factionNamespace;
    }

    void factionNamespace(String value) {
        factionNamespace = clean(value, "all");
    }

    String archiveState() {
        return archiveState;
    }

    void archiveState(String value) {
        archiveState = clean(value, "all");
    }

    String archiveGroup() {
        return archiveGroup;
    }

    void archiveGroup(String value) {
        archiveGroup = clean(value, "all");
    }

    void resetForTests() {
        selectedMissionId = null;
        selectedRecipeId = null;
        selectedRouteRecordId = null;
        selectedDiscoveryId = null;
        selectedFactionId = null;
        selectedArchiveId = null;
        selectedAddonId = "";
        selectedRewardId = "";
        selectedMissionProviderId = "";
        missionSearch = "";
        missionProviderFilter = "all";
        recipeSearch = "";
        recipeMode = "recipes";
        recipeCategory = "all";
        routeFilter = "all";
        diagnosticsChapterFilter = "all";
        discoveryCategory = "all";
        discoveryState = "all";
        factionNamespace = "all";
        archiveState = "all";
        archiveGroup = "all";
        rewardDeferred = false;
        rewardViewed = false;
    }

    private static String clean(String value) {
        return clean(value, "");
    }

    private static String clean(String value, String fallback) {
        String cleaned = value == null ? "" : value.strip();
        return cleaned.isBlank() ? fallback : cleaned;
    }
}
