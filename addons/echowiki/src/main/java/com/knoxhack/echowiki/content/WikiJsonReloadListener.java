package com.knoxhack.echowiki.content;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.knoxhack.echowiki.EchoWiki;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public final class WikiJsonReloadListener extends SimplePreparableReloadListener<WikiJsonReloadListener.LoadedContent> {
    private static final String ARTICLE_DIR = "echowiki/articles";
    private static final String COLLECTION_DIR = "echowiki/collections";
    private static final String GUIDE_BOOK_DIR = "echowiki/guide_books";

    @Override
    protected LoadedContent prepare(ResourceManager manager, ProfilerFiller profiler) {
        List<String> warnings = new ArrayList<>();
        Map<Identifier, WikiArticleDefinition> articles = loadArticles(manager, warnings);
        Map<Identifier, WikiCollectionDefinition> collections = loadCollections(manager, warnings);
        Map<Identifier, GuideBookDefinition> guideBooks = loadGuideBooks(manager, warnings);
        validateGuideReferences(guideBooks, articles, collections, warnings);
        return new LoadedContent(articles, collections, guideBooks, warnings);
    }

    @Override
    protected void apply(LoadedContent content, ResourceManager manager, ProfilerFiller profiler) {
        WikiContentRegistry.replaceData(content.articles(), content.collections(), content.warnings());
        GuideBookRegistry.replaceData(content.guideBooks());
        EchoWiki.LOGGER.info("ECHO Wiki loaded {} data article(s), {} collection(s), {} guide book(s), and {} warning(s).",
                content.articles().size(), content.collections().size(), content.guideBooks().size(), content.warnings().size());
    }

    public static WikiArticleDefinition parseArticleForTests(Identifier fallbackId, JsonObject json) {
        return parseArticle(fallbackId, json);
    }

    public static WikiCollectionDefinition parseCollectionForTests(Identifier fallbackId, JsonObject json) {
        return parseCollection(fallbackId, json);
    }

    public static GuideBookDefinition parseGuideBookForTests(Identifier fallbackId, JsonObject json) {
        return parseGuideBook(fallbackId, json);
    }

    public static void reloadClientData(ResourceManager manager) {
        if (manager == null) {
            return;
        }
        List<String> warnings = new ArrayList<>();
        Map<Identifier, WikiArticleDefinition> articles = loadArticles(manager, warnings);
        Map<Identifier, WikiCollectionDefinition> collections = loadCollections(manager, warnings);
        Map<Identifier, GuideBookDefinition> guideBooks = loadGuideBooks(manager, warnings);
        validateGuideReferences(guideBooks, articles, collections, warnings);
        WikiContentRegistry.replaceData(articles, collections, warnings);
        GuideBookRegistry.replaceData(guideBooks);
        EchoWiki.LOGGER.info("ECHO Wiki client loaded {} data article(s), {} collection(s), {} guide book(s), and {} warning(s).",
                articles.size(), collections.size(), guideBooks.size(), warnings.size());
    }

    private static Map<Identifier, WikiArticleDefinition> loadArticles(ResourceManager manager, List<String> warnings) {
        Map<Identifier, WikiArticleDefinition> articles = new LinkedHashMap<>();
        for (Map.Entry<Identifier, Resource> entry : manager.listResources(ARTICLE_DIR, id -> id.getPath().endsWith(".json")).entrySet()) {
            Identifier resourceId = entry.getKey();
            Identifier fallbackId = contentId(resourceId, ARTICLE_DIR);
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement root = JsonParser.parseReader(reader);
                if (!root.isJsonObject()) {
                    throw new JsonParseException("Root must be a JSON object.");
                }
                WikiArticleDefinition article = parseArticle(fallbackId, root.getAsJsonObject());
                if (articles.put(article.id(), article) != null) {
                    warnings.add("Duplicate Wiki article id " + article.id() + " from " + resourceId + " replaced earlier data entry.");
                }
            } catch (IOException | RuntimeException exception) {
                String message = "Could not parse Wiki article " + resourceId + ": " + exception.getMessage();
                warnings.add(message);
                EchoWiki.LOGGER.warn(message, exception);
            }
        }
        return articles;
    }

    private static Map<Identifier, WikiCollectionDefinition> loadCollections(ResourceManager manager, List<String> warnings) {
        Map<Identifier, WikiCollectionDefinition> collections = new LinkedHashMap<>();
        for (Map.Entry<Identifier, Resource> entry : manager.listResources(COLLECTION_DIR, id -> id.getPath().endsWith(".json")).entrySet()) {
            Identifier resourceId = entry.getKey();
            Identifier fallbackId = contentId(resourceId, COLLECTION_DIR);
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement root = JsonParser.parseReader(reader);
                if (!root.isJsonObject()) {
                    throw new JsonParseException("Root must be a JSON object.");
                }
                WikiCollectionDefinition collection = parseCollection(fallbackId, root.getAsJsonObject());
                if (collections.put(collection.id(), collection) != null) {
                    warnings.add("Duplicate Wiki collection id " + collection.id() + " from " + resourceId + " replaced earlier data entry.");
                }
            } catch (IOException | RuntimeException exception) {
                String message = "Could not parse Wiki collection " + resourceId + ": " + exception.getMessage();
                warnings.add(message);
                EchoWiki.LOGGER.warn(message, exception);
            }
        }
        return collections;
    }

    private static Map<Identifier, GuideBookDefinition> loadGuideBooks(ResourceManager manager, List<String> warnings) {
        Map<Identifier, GuideBookDefinition> guideBooks = new LinkedHashMap<>();
        for (Map.Entry<Identifier, Resource> entry : manager.listResources(GUIDE_BOOK_DIR, id -> id.getPath().endsWith(".json")).entrySet()) {
            Identifier resourceId = entry.getKey();
            Identifier fallbackId = contentId(resourceId, GUIDE_BOOK_DIR);
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement root = JsonParser.parseReader(reader);
                if (!root.isJsonObject()) {
                    throw new JsonParseException("Root must be a JSON object.");
                }
                GuideBookDefinition guideBook = parseGuideBook(fallbackId, root.getAsJsonObject());
                if (guideBooks.put(guideBook.id(), guideBook) != null) {
                    warnings.add("Duplicate Wiki guide book id " + guideBook.id() + " from " + resourceId + " replaced earlier data entry.");
                }
            } catch (IOException | RuntimeException exception) {
                String message = "Could not parse Wiki guide book " + resourceId + ": " + exception.getMessage();
                warnings.add(message);
                EchoWiki.LOGGER.warn(message, exception);
            }
        }
        return guideBooks;
    }

    public static List<String> validateGuideReferencesForTests(Map<Identifier, GuideBookDefinition> guideBooks,
            Map<Identifier, WikiArticleDefinition> articles, Map<Identifier, WikiCollectionDefinition> collections) {
        ArrayList<String> warnings = new ArrayList<>();
        validateGuideReferences(guideBooks, articles, collections, warnings);
        return List.copyOf(warnings);
    }

    private static void validateGuideReferences(Map<Identifier, GuideBookDefinition> guideBooks,
            Map<Identifier, WikiArticleDefinition> articles, Map<Identifier, WikiCollectionDefinition> collections,
            List<String> warnings) {
        if (guideBooks == null || guideBooks.isEmpty()) {
            return;
        }
        for (GuideBookDefinition guide : guideBooks.values()) {
            if (guide.collectionId() != null && !collectionExists(guide.collectionId(), collections)) {
                addWarning(warnings, "Guide book " + guide.id() + " references missing collection " + guide.collectionId() + ".");
            }
            Set<Identifier> warnedArticles = new LinkedHashSet<>();
            if (guide.homeArticleId() != null && !articleExists(guide.homeArticleId(), articles)) {
                warnedArticles.add(guide.homeArticleId());
                addWarning(warnings, "Guide book " + guide.id() + " references missing home article " + guide.homeArticleId() + ".");
            }
            for (Identifier articleId : guide.chapterArticleIds()) {
                if (!articleExists(articleId, articles) && warnedArticles.add(articleId)) {
                    addWarning(warnings, "Guide book " + guide.id() + " references missing chapter article " + articleId + ".");
                }
            }
            if (!GuideBookLabels.hasItemIcon(guide.icon())) {
                addWarning(warnings, "Guide book " + guide.id() + " references missing icon item " + guide.icon()
                        + "; falling back to " + GuideBookLabels.DEFAULT_ICON + ".");
            }
        }
    }

    private static void addWarning(List<String> warnings, String warning) {
        if (warnings != null && warning != null && !warnings.contains(warning)) {
            warnings.add(warning);
        }
    }

    private static boolean articleExists(Identifier id, Map<Identifier, WikiArticleDefinition> articles) {
        return id != null && ((articles != null && articles.containsKey(id)) || WikiContentRegistry.article(id).isPresent());
    }

    private static boolean collectionExists(Identifier id, Map<Identifier, WikiCollectionDefinition> collections) {
        return id != null && ((collections != null && collections.containsKey(id))
                || WikiContentRegistry.collections().stream().anyMatch(collection -> collection.id().equals(id)));
    }

    private static WikiArticleDefinition parseArticle(Identifier fallbackId, JsonObject json) {
        Identifier id = identifier(json, "id", fallbackId);
        return new WikiArticleDefinition(
                id,
                string(json, "title", id.getPath()),
                string(json, "category", "general"),
                string(json, "summary", ""),
                sections(json),
                strings(json, "tags"),
                identifier(json, "icon", Identifier.fromNamespaceAndPath("minecraft", "book")),
                nullableIdentifier(json, "heroArt"),
                identifiers(json, "relatedArticles"),
                identifiers(json, "relatedItems"),
                identifiers(json, "relatedRecipes"),
                identifiers(json, "relatedMissions"),
                identifiers(json, "relatedRegions"),
                identifiers(json, "relatedHazards"),
                identifiers(json, "relatedFactions"),
                nullableIdentifier(json, "unlockDiscovery"),
                integer(json, "spoilerLevel", 0),
                integer(json, "sortOrder", 0));
    }

    private static WikiCollectionDefinition parseCollection(Identifier fallbackId, JsonObject json) {
        Identifier id = identifier(json, "id", fallbackId);
        return new WikiCollectionDefinition(
                id,
                string(json, "title", id.getPath()),
                string(json, "summary", ""),
                string(json, "category", "general"),
                identifiers(json, "articles"),
                integer(json, "sortOrder", 0));
    }

    private static GuideBookDefinition parseGuideBook(Identifier fallbackId, JsonObject json) {
        Identifier id = identifier(json, "id", fallbackId);
        String moduleId = string(json, "moduleId", id.getNamespace());
        String requiredModId = string(json, "requiredModId", moduleId);
        List<Identifier> chapterIds = mergedIdentifiers(json, "chapterArticleIds", "chapters", "chapterArticles");
        return new GuideBookDefinition(
                id,
                moduleId,
                requiredModId,
                string(json, "title", id.getPath()),
                string(json, "subtitle", moduleId),
                string(json, "summary", ""),
                identifier(json, "icon", Identifier.fromNamespaceAndPath("minecraft", "written_book")),
                string(json, "accent", "#FF66E8FF"),
                nullableIdentifier(json, "collectionId"),
                nullableIdentifier(json, "homeArticleId"),
                chapterIds,
                strings(json, "tags"),
                integer(json, "sortOrder", 0));
    }

    private static List<WikiArticleSection> sections(JsonObject json) {
        JsonArray blocks = array(json, "blocks");
        if (blocks != null) {
            return blocks(blocks);
        }
        JsonArray sections = array(json, "sections");
        if (sections != null) {
            return blocks(sections);
        }
        String body = string(json, "body", "");
        return body.isBlank() ? List.of() : List.of(new WikiArticleSection("", body, "body"));
    }

    private static List<WikiArticleSection> blocks(JsonArray array) {
        List<WikiArticleSection> blocks = new ArrayList<>();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject object = element.getAsJsonObject();
            String tone = string(object, "tone", "body");
            blocks.add(new WikiArticleSection(
                    string(object, "type", defaultBlockType(tone)),
                    string(object, "title", ""),
                    string(object, "body", string(object, "text", "")),
                    tone,
                    string(object, "image", ""),
                    string(object, "imageFit", "cover"),
                    string(object, "icon", ""),
                    string(object, "item", ""),
                    integer(object, "count", 1),
                    string(object, "targetKind", ""),
                    string(object, "target", ""),
                    string(object, "label", ""),
                    string(object, "subtitle", "")));
        }
        return List.copyOf(blocks);
    }

    private static String defaultBlockType(String tone) {
        return tone == null || tone.isBlank() || "body".equalsIgnoreCase(tone.strip()) ? "paragraph" : "callout";
    }

    private static Identifier contentId(Identifier resourceId, String directory) {
        String path = resourceId.getPath();
        String prefix = directory + "/";
        if (path.startsWith(prefix)) {
            path = path.substring(prefix.length());
        }
        if (path.endsWith(".json")) {
            path = path.substring(0, path.length() - ".json".length());
        }
        return Identifier.fromNamespaceAndPath(resourceId.getNamespace(), path);
    }

    private static Identifier identifier(JsonObject json, String key, Identifier fallback) {
        if (json == null || !json.has(key) || !json.get(key).isJsonPrimitive()) {
            return fallback;
        }
        Identifier parsed = Identifier.tryParse(json.get(key).getAsString());
        return parsed == null ? fallback : parsed;
    }

    private static Identifier nullableIdentifier(JsonObject json, String key) {
        return identifier(json, key, null);
    }

    private static List<Identifier> identifiers(JsonObject json, String key) {
        JsonArray array = array(json, key);
        if (array == null) {
            return List.of();
        }
        List<Identifier> values = new ArrayList<>();
        for (JsonElement element : array) {
            if (!element.isJsonPrimitive()) {
                continue;
            }
            Identifier parsed = Identifier.tryParse(element.getAsString());
            if (parsed != null) {
                values.add(parsed);
            }
        }
        return List.copyOf(values);
    }

    private static List<Identifier> mergedIdentifiers(JsonObject json, String... keys) {
        List<Identifier> values = new ArrayList<>();
        for (String key : keys) {
            for (Identifier id : identifiers(json, key)) {
                if (!values.contains(id)) {
                    values.add(id);
                }
            }
        }
        return List.copyOf(values);
    }

    private static List<String> strings(JsonObject json, String key) {
        JsonArray array = array(json, key);
        if (array == null) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonElement element : array) {
            if (element.isJsonPrimitive()) {
                String value = element.getAsString();
                if (!value.isBlank()) {
                    values.add(value.strip());
                }
            }
        }
        return List.copyOf(values);
    }

    private static JsonArray array(JsonObject json, String key) {
        return json != null && json.has(key) && json.get(key).isJsonArray() ? json.getAsJsonArray(key) : null;
    }

    private static String string(JsonObject json, String key, String fallback) {
        if (json == null || !json.has(key) || !json.get(key).isJsonPrimitive()) {
            return fallback;
        }
        String value = json.get(key).getAsString();
        return value == null ? fallback : value.strip();
    }

    private static int integer(JsonObject json, String key, int fallback) {
        if (json == null || !json.has(key) || !json.get(key).isJsonPrimitive()) {
            return fallback;
        }
        try {
            return json.get(key).getAsInt();
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    public record LoadedContent(
            Map<Identifier, WikiArticleDefinition> articles,
            Map<Identifier, WikiCollectionDefinition> collections,
            Map<Identifier, GuideBookDefinition> guideBooks,
            List<String> warnings) {
        public LoadedContent {
            articles = Map.copyOf(articles == null ? Map.of() : articles);
            collections = Map.copyOf(collections == null ? Map.of() : collections);
            guideBooks = Map.copyOf(guideBooks == null ? Map.of() : guideBooks);
            warnings = List.copyOf(warnings == null ? List.of() : warnings);
        }
    }
}
