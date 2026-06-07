package com.knoxhack.echoterminal.client.screen;

import com.knoxhack.echoterminal.api.TerminalNavigationProfile;
import com.knoxhack.echoterminal.api.TerminalNavigationProfiles;
import com.knoxhack.echoterminal.api.TerminalNavigationSection;
import com.knoxhack.echoterminal.api.TerminalTab;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class TerminalNavigationModel {
    private static final List<TerminalNavigationSection> SECTION_ORDER = TerminalNavigationSection.storyFirstOrder();

    private final List<TerminalTab> tabs;
    private final List<IndexedTab> indexedTabs;
    private final List<String> groups;
    private final Map<String, List<IndexedTab>> tabsByGroup;
    private final Map<String, List<IndexedTab>> directTabsByGroup;
    private final Map<String, List<ChapterGroup>> chaptersByGroup;

    private TerminalNavigationModel(List<TerminalTab> tabs) {
        this.tabs = List.copyOf(tabs);
        this.indexedTabs = collectIndexedTabs(tabs);
        this.groups = collectGroups(indexedTabs);
        this.tabsByGroup = collectTabsByGroup(indexedTabs, groups);
        this.directTabsByGroup = collectDirectTabsByGroup(indexedTabs, groups);
        this.chaptersByGroup = collectChaptersByGroup(indexedTabs, groups);
    }

    static TerminalNavigationModel of(List<TerminalTab> tabs) {
        return new TerminalNavigationModel(tabs == null ? List.of() : tabs);
    }

    List<TerminalTab> tabs() {
        return tabs;
    }

    List<String> groups() {
        return groups;
    }

    List<IndexedTab> tabsInGroup(String group) {
        return tabsByGroup.getOrDefault(group, List.of());
    }

    List<IndexedTab> visibleTabsInGroup(String group, int activeTab) {
        List<IndexedTab> visible = new ArrayList<>(directTabsInGroup(group));
        String activeChapter = activeChapterId(activeTab);
        for (ChapterGroup chapter : chaptersInGroup(group)) {
            if (chapter.id().equals(activeChapter)) {
                visible.addAll(chapter.tabs());
            }
        }
        return List.copyOf(visible);
    }

    List<IndexedTab> directTabsInGroup(String group) {
        return directTabsByGroup.getOrDefault(group, List.of());
    }

    List<ChapterGroup> chaptersInGroup(String group) {
        return chaptersByGroup.getOrDefault(group, List.of());
    }

    boolean hasChaptersInGroup(String group) {
        return !chaptersInGroup(group).isEmpty();
    }

    boolean tabHasChapter(int tabIndex) {
        IndexedTab entry = indexed(tabIndex);
        return entry != null && entry.profile().hasChapter();
    }

    boolean activeChapterInGroup(String group, int activeTab) {
        IndexedTab entry = indexed(activeTab);
        return entry != null
                && entry.profile().hasChapter()
                && entry.profile().section().key().equals(group);
    }

    String activeGroup(int activeTab) {
        IndexedTab entry = indexed(activeTab);
        if (entry == null) {
            return TerminalNavigationSection.COMMAND.key();
        }
        return entry.profile().section().key();
    }

    String activeChapterId(int activeTab) {
        IndexedTab entry = indexed(activeTab);
        return entry == null ? "" : entry.profile().chapterId();
    }

    String activePathLabel(int activeTab) {
        IndexedTab entry = indexed(activeTab);
        if (entry == null) {
            return TerminalNavigationSection.COMMAND.label();
        }
        String section = groupLabel(entry.profile().section().key());
        if (entry.profile().hasChapter()) {
            return section + " / " + chapterLabel(entry.profile()) + " / " + entry.tab().chrome().shortTitle();
        }
        return section + " / " + entry.tab().chrome().shortTitle();
    }

    int firstTabInGroup(String group) {
        List<IndexedTab> direct = directTabsInGroup(group);
        if (!direct.isEmpty()) {
            return direct.get(0).index();
        }
        for (ChapterGroup chapter : chaptersInGroup(group)) {
            if (!chapter.tabs().isEmpty()) {
                return chapter.tabs().get(0).index();
            }
        }
        return tabs.isEmpty() ? 0 : Math.min(0, tabs.size() - 1);
    }

    int firstTabInChapter(ChapterGroup chapter) {
        return chapter == null || chapter.tabs().isEmpty() ? 0 : chapter.tabs().get(0).index();
    }

    int groupAccent(String group, int fallback) {
        for (IndexedTab entry : tabsInGroup(group)) {
            return entry.tab().descriptor().accentColor();
        }
        return fallback;
    }

    int visibleRowCount(String group, int activeTab) {
        int rows = directTabsInGroup(group).size() + chaptersInGroup(group).size();
        String activeChapter = activeChapterId(activeTab);
        for (ChapterGroup chapter : chaptersInGroup(group)) {
            if (chapter.id().equals(activeChapter)) {
                rows += chapter.tabs().size();
                break;
            }
        }
        return Math.max(1, rows);
    }

    String groupLabel(String group) {
        for (TerminalNavigationSection section : SECTION_ORDER) {
            if (section.key().equals(group)) {
                return section.label();
            }
        }
        return TerminalNavigationSection.fromKey(group).label();
    }

    private IndexedTab indexed(int activeTab) {
        if (activeTab < 0 || activeTab >= indexedTabs.size()) {
            return null;
        }
        return indexedTabs.get(activeTab);
    }

    private static List<IndexedTab> collectIndexedTabs(List<TerminalTab> tabs) {
        List<IndexedTab> result = new ArrayList<>();
        for (int i = 0; i < tabs.size(); i++) {
            TerminalTab tab = tabs.get(i);
            String chromeGroup = tab.chrome() == null ? "" : tab.chrome().group();
            result.add(new IndexedTab(i, tab, TerminalNavigationProfiles.profileFor(tab)));
        }
        return List.copyOf(result);
    }

    private static List<String> collectGroups(List<IndexedTab> tabs) {
        List<String> result = new ArrayList<>();
        for (TerminalNavigationSection section : SECTION_ORDER) {
            if (containsGroup(tabs, section.key())) {
                result.add(section.key());
            }
        }
        for (IndexedTab tab : tabs) {
            String group = tab.profile().section().key();
            if (!result.contains(group)) {
                result.add(group);
            }
        }
        return List.copyOf(result);
    }

    private static Map<String, List<IndexedTab>> collectTabsByGroup(List<IndexedTab> tabs, List<String> groups) {
        Map<String, List<IndexedTab>> result = emptyGroupMap(groups);
        for (IndexedTab entry : sortedEntries(tabs)) {
            result.computeIfAbsent(entry.profile().section().key(), ignored -> new ArrayList<>()).add(entry);
        }
        result.replaceAll((group, entries) -> List.copyOf(entries));
        return Map.copyOf(result);
    }

    private static Map<String, List<IndexedTab>> collectDirectTabsByGroup(List<IndexedTab> tabs, List<String> groups) {
        Map<String, List<IndexedTab>> result = emptyGroupMap(groups);
        for (IndexedTab entry : sortedEntries(tabs)) {
            if (!entry.profile().hasChapter()) {
                result.computeIfAbsent(entry.profile().section().key(), ignored -> new ArrayList<>()).add(entry);
            }
        }
        result.replaceAll((group, entries) -> List.copyOf(entries));
        return Map.copyOf(result);
    }

    private static Map<String, List<ChapterGroup>> collectChaptersByGroup(List<IndexedTab> tabs, List<String> groups) {
        Map<String, Map<String, ChapterBuilder>> buildersByGroup = new LinkedHashMap<>();
        for (String group : groups) {
            buildersByGroup.put(group, new LinkedHashMap<>());
        }
        for (IndexedTab entry : sortedEntries(tabs)) {
            TerminalNavigationProfile profile = entry.profile();
            if (!profile.hasChapter()) {
                continue;
            }
            buildersByGroup
                    .computeIfAbsent(profile.section().key(), ignored -> new LinkedHashMap<>())
                    .computeIfAbsent(profile.chapterId(), ignored -> new ChapterBuilder(profile))
                    .add(entry);
        }
        Map<String, List<ChapterGroup>> result = new LinkedHashMap<>();
        for (String group : groups) {
            List<ChapterGroup> chapters = buildersByGroup.getOrDefault(group, Map.of()).values().stream()
                    .map(ChapterBuilder::build)
                    .sorted(Comparator
                            .comparingInt(TerminalNavigationModel::chapterRailPriority)
                            .thenComparingInt(ChapterGroup::order)
                            .thenComparing(ChapterGroup::title)
                            .thenComparing(ChapterGroup::id))
                    .toList();
            result.put(group, chapters);
        }
        return Map.copyOf(result);
    }

    private static List<IndexedTab> sortedEntries(List<IndexedTab> tabs) {
        return tabs.stream()
                .sorted(Comparator
                        .comparingInt((IndexedTab entry) -> entry.profile().order())
                        .thenComparing(entry -> entry.tab().descriptor().id().toString()))
                .toList();
    }

    private static Map<String, List<IndexedTab>> emptyGroupMap(List<String> groups) {
        Map<String, List<IndexedTab>> result = new LinkedHashMap<>();
        for (String group : groups) {
            result.put(group, new ArrayList<>());
        }
        return result;
    }

    private static boolean containsGroup(List<IndexedTab> tabs, String group) {
        for (IndexedTab tab : tabs) {
            if (tab.profile().section().key().equals(group)) {
                return true;
            }
        }
        return false;
    }

    private static String chapterLabel(TerminalNavigationProfile profile) {
        String label = profile.chapterTitle().isBlank() ? profile.chapterId() : profile.chapterTitle();
        return progressChapterTitle(profile.chapterId(), label);
    }

    private static int chapterRailPriority(ChapterGroup chapter) {
        return isAshfallChapter(chapter == null ? "" : chapter.id(), chapter == null ? "" : chapter.title()) ? 0 : 1;
    }

    private static String progressChapterTitle(String chapterId, String title) {
        String cleaned = title == null ? "" : title.strip();
        cleaned = cleaned.replaceFirst("(?i)^optional\\s*:\\s*", "");
        cleaned = cleaned.replaceFirst("(?i)^chapter\\s+\\d+\\s*:\\s*", "");
        if (cleaned.isBlank() && isAshfallChapter(chapterId, cleaned)) {
            return "Ashfall Protocol";
        }
        return cleaned.isBlank() ? cleanChapterId(chapterId) : cleaned;
    }

    private static boolean isAshfallChapter(String chapterId, String title) {
        String key = cleanChapterId(chapterId);
        String name = title == null ? "" : title.strip().toLowerCase(Locale.ROOT);
        return key.equals("ashfall")
                || key.equals("ashfall_protocol")
                || key.equals("echoashfallprotocol")
                || name.equals("ashfall")
                || name.equals("ashfall protocol");
    }

    private static String cleanChapterId(String chapterId) {
        String cleaned = chapterId == null ? "" : chapterId.strip().toLowerCase(Locale.ROOT);
        int namespaceSeparator = cleaned.indexOf(':');
        if (namespaceSeparator >= 0 && namespaceSeparator + 1 < cleaned.length()) {
            cleaned = cleaned.substring(namespaceSeparator + 1);
        }
        return cleaned;
    }

    record IndexedTab(int index, TerminalTab tab, TerminalNavigationProfile profile) {
    }

    record ChapterGroup(String id, String title, String rawTitle, String iconLabel, int order, int accent, List<IndexedTab> tabs) {
    }

    private static final class ChapterBuilder {
        private final String id;
        private final String title;
        private final String rawTitle;
        private final String iconLabel;
        private final int order;
        private final List<IndexedTab> tabs = new ArrayList<>();

        private ChapterBuilder(TerminalNavigationProfile profile) {
            this.id = profile.chapterId();
            this.rawTitle = profile.chapterTitle().isBlank() ? profile.chapterId() : profile.chapterTitle();
            this.title = chapterLabel(profile);
            this.iconLabel = profile.chapterIcon().isBlank() ? fallbackIcon(title) : profile.chapterIcon();
            this.order = profile.order();
        }

        private void add(IndexedTab tab) {
            tabs.add(tab);
        }

        private ChapterGroup build() {
            List<IndexedTab> sortedTabs = sortedEntries(tabs);
            int accent = sortedTabs.isEmpty() ? 0xFF66D9FF : sortedTabs.get(0).tab().descriptor().accentColor();
            return new ChapterGroup(id, title, rawTitle, iconLabel, order, accent, List.copyOf(sortedTabs));
        }
    }

    private static String fallbackIcon(String title) {
        String cleaned = title == null ? "" : title.replaceAll("[^A-Za-z0-9]", "");
        if (cleaned.length() >= 2) {
            return cleaned.substring(0, 2).toUpperCase(Locale.ROOT);
        }
        return cleaned.isBlank() ? "CH" : cleaned.toUpperCase(Locale.ROOT);
    }
}
