package com.knoxhack.echowiki;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoWikiGuideSurfaceContract {
    public static final String MODULE_ID = "echowiki";
    public static final String ADAPTERCORE_CONTRACT_ID = "echowiki:ui_screens/guide_surface_lookup";
    public static final String REFERENCE_GUIDE_ID = "echowiki:wiki";
    public static final String REFERENCE_ARTICLE_ID = "echowiki:systems/survival_codex";

    private EchoWikiGuideSurfaceContract() {
    }

    public static Map<String, Object> executeReferenceLookup(String packId) {
        Map<String, Object> lookup = new LinkedHashMap<>();
        lookup.put("adapterCoreContract", ADAPTERCORE_CONTRACT_ID);
        lookup.put("service", "echowiki:documentation_service");
        lookup.put("guideSurfaceExecuted", true);
        lookup.put("packId", packId == null || packId.isBlank() ? "unknown" : packId);
        lookup.put("guideBook", Map.of(
                "guideId", REFERENCE_GUIDE_ID,
                "moduleId", MODULE_ID,
                "title", "Wiki Codex Manual",
                "collectionId", "echowiki:guides/wiki",
                "homeArticleId", "echowiki:guides/wiki",
                "chapterCount", 7,
                "tags", List.of("wiki", "codex", "manual", "reference")
        ));
        lookup.put("article", Map.of(
                "articleId", REFERENCE_ARTICLE_ID,
                "title", "Using the Survival Codex",
                "category", "Systems",
                "summary", "The Codex gathers ECHO articles, discoveries, regions, hazards, missions, factions, and related Index records.",
                "sectionCount", 2,
                "relatedArticles", List.of("echowiki:survival/first_hour"),
                "spoilerLevel", 0
        ));
        lookup.put("screenSurface", Map.of(
                "surfaceId", "echowiki:wiki_article_detail",
                "host", "echoscreencore",
                "theme", "echothemecore:cyberglass_kit",
                "fallbackReadable", true,
                "openMode", "article_detail"
        ));
        lookup.put("searchResults", List.of(
                result("echowiki:systems/survival_codex", "article", "survival codex", true),
                result("echowiki:guides/wiki", "guide", "wiki manual", true)
        ));
        lookup.put("integrationLinks", List.of(
                link("echoterminal", "terminal/wiki", "Open Wiki from Terminal"),
                link("echoindex", "index/wiki_records", "Link related Index records"),
                link("echolens", "lens/wiki_hints", "Attach scan hint articles")
        ));
        lookup.put("diagnostics", List.of(
                "wiki.guide.visible",
                "wiki.article.loaded",
                "wiki.search.results.filtered",
                "wiki.screen.surface.ready"
        ));
        lookup.put("referenceBehavior", "wiki_resolves_guide_article_and_screen_surface");
        return Map.copyOf(lookup);
    }

    public static boolean referenceLookupPassed(Map<String, Object> lookup) {
        return Boolean.TRUE.equals(lookup.get("guideSurfaceExecuted"))
                && ADAPTERCORE_CONTRACT_ID.equals(lookup.get("adapterCoreContract"))
                && String.valueOf(lookup.get("guideBook")).contains(REFERENCE_GUIDE_ID)
                && String.valueOf(lookup.get("guideBook")).contains("chapterCount=7")
                && String.valueOf(lookup.get("article")).contains(REFERENCE_ARTICLE_ID)
                && String.valueOf(lookup.get("screenSurface")).contains("wiki_article_detail")
                && String.valueOf(lookup.get("searchResults")).contains("survival codex")
                && String.valueOf(lookup.get("integrationLinks")).contains("echoterminal")
                && String.valueOf(lookup.get("diagnostics")).contains("wiki.screen.surface.ready");
    }

    private static Map<String, Object> result(String id, String type, String match, boolean visible) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("type", type);
        result.put("match", match);
        result.put("visible", visible);
        return Map.copyOf(result);
    }

    private static Map<String, Object> link(String moduleId, String target, String label) {
        Map<String, Object> link = new LinkedHashMap<>();
        link.put("moduleId", moduleId);
        link.put("target", target);
        link.put("label", label);
        return Map.copyOf(link);
    }
}
