package com.knoxhack.echowiki.client;

import com.knoxhack.echowiki.EchoWiki;
import net.minecraft.resources.Identifier;

public final class WikiScreenCorePages {
    public static final Identifier DASHBOARD = EchoWiki.id("wiki_dashboard");
    public static final Identifier ARTICLES = EchoWiki.id("wiki_articles");
    public static final Identifier ARTICLE_DETAIL = EchoWiki.id("wiki_article_detail");
    public static final Identifier GUIDE_BOOKS = EchoWiki.id("wiki_guide_books");
    public static final Identifier GUIDE_BOOK_DETAIL = EchoWiki.id("wiki_guide_book_detail");
    public static final Identifier GUIDE_BOOK_READER = EchoWiki.id("wiki_guide_book_reader");
    public static final Identifier REGIONS = EchoWiki.id("wiki_regions");
    public static final Identifier HAZARDS = EchoWiki.id("wiki_hazards");
    public static final Identifier MISSIONS = EchoWiki.id("wiki_missions");
    public static final Identifier MACHINES = EchoWiki.id("wiki_machines");
    public static final Identifier FACTIONS = EchoWiki.id("wiki_factions");
    public static final Identifier DISCOVERIES = EchoWiki.id("wiki_discoveries");
    public static final Identifier SETTINGS = EchoWiki.id("wiki_settings");

    private WikiScreenCorePages() {
    }

    public static Identifier fromMode(String raw) {
        String clean = raw == null ? "" : raw.strip().toLowerCase(java.util.Locale.ROOT);
        return switch (clean) {
            case "articles", "article", "browser" -> ARTICLES;
            case "article_detail", "detail" -> ARTICLE_DETAIL;
            case "guide_books", "guidebooks", "guides", "manuals" -> GUIDE_BOOKS;
            case "guide_book_reader", "guidebook_reader", "guide_reader", "manual_reader", "reader" -> GUIDE_BOOK_READER;
            case "guide_book_detail", "guidebook_detail", "guide_detail", "manual_detail" -> GUIDE_BOOK_DETAIL;
            case "regions", "region", "world" -> REGIONS;
            case "hazards", "hazard" -> HAZARDS;
            case "missions", "mission" -> MISSIONS;
            case "machines", "machine", "index" -> MACHINES;
            case "factions", "faction" -> FACTIONS;
            case "discoveries", "discovery", "intel" -> DISCOVERIES;
            case "settings", "debug" -> SETTINGS;
            default -> DASHBOARD;
        };
    }

    public static String titleFor(Identifier pageId) {
        if (ARTICLES.equals(pageId)) {
            return "Articles";
        }
        if (ARTICLE_DETAIL.equals(pageId)) {
            return "Article Detail";
        }
        if (GUIDE_BOOKS.equals(pageId)) {
            return "Guide Books";
        }
        if (GUIDE_BOOK_DETAIL.equals(pageId)) {
            return "Guide Book";
        }
        if (GUIDE_BOOK_READER.equals(pageId)) {
            return "Guide Book";
        }
        if (REGIONS.equals(pageId)) {
            return "Regions";
        }
        if (HAZARDS.equals(pageId)) {
            return "Hazards";
        }
        if (MISSIONS.equals(pageId)) {
            return "Missions";
        }
        if (MACHINES.equals(pageId)) {
            return "Machines";
        }
        if (FACTIONS.equals(pageId)) {
            return "Factions";
        }
        if (DISCOVERIES.equals(pageId)) {
            return "Discoveries";
        }
        if (SETTINGS.equals(pageId)) {
            return "Settings";
        }
        return "Overview";
    }
}
