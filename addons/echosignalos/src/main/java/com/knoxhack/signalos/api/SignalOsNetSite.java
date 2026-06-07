package com.knoxhack.signalos.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.resources.Identifier;

/**
 * Curated in-world internet site for SignalOS. A site owns a safe address such
 * as {@code echo.home}; pages live below it with paths such as {@code /status}.
 */
public record SignalOsNetSite(
        Identifier id,
        String address,
        String title,
        String summary,
        int requiredTier,
        List<String> tags,
        List<SignalOsNetPage> pages,
        int order) {
    private static final int MAX_ADDRESS = 96;
    private static final int MAX_PATH = 120;

    public SignalOsNetSite {
        id = TerminalIds.requireLowercase(id, "SignalNet site");
        address = cleanAddress(address);
        title = title == null || title.isBlank() ? address : title.strip();
        summary = summary == null ? "" : summary.strip();
        requiredTier = Math.max(0, requiredTier);
        tags = cleanTags(tags);
        pages = cleanPages(address, pages);
    }

    public String pageAddress(SignalOsNetPage page) {
        return pageAddress(address, page == null ? "/" : page.path());
    }

    public static String pageAddress(String siteAddress, String path) {
        String site = cleanAddress(siteAddress);
        String safePath = cleanPath(path);
        return "/".equals(safePath) ? site : site + safePath;
    }

    public static String cleanAddress(String value) {
        String safe = value == null ? "" : value.strip().toLowerCase(Locale.ROOT)
                .replace('\\', '/');
        while (safe.contains("//")) {
            safe = safe.replace("//", "/");
        }
        if (safe.startsWith("http://") || safe.startsWith("https://") || safe.contains("://")) {
            throw new IllegalArgumentException("SignalNet addresses must not use external URL schemes.");
        }
        if (safe.startsWith("/") || safe.endsWith("/") || safe.isBlank() || safe.length() > MAX_ADDRESS
                || safe.contains("..") || safe.contains(" ")) {
            throw new IllegalArgumentException("Invalid SignalNet address: " + value);
        }
        for (int i = 0; i < safe.length(); i++) {
            char c = safe.charAt(i);
            boolean allowed = c >= 'a' && c <= 'z'
                    || c >= '0' && c <= '9'
                    || c == '.' || c == '-' || c == '_' || c == '/';
            if (!allowed) {
                throw new IllegalArgumentException("Invalid SignalNet address: " + value);
            }
        }
        return safe;
    }

    public static String cleanPath(String value) {
        String safe = value == null || value.isBlank() ? "/" : value.strip().toLowerCase(Locale.ROOT)
                .replace('\\', '/');
        if (!safe.startsWith("/")) {
            safe = "/" + safe;
        }
        while (safe.contains("//")) {
            safe = safe.replace("//", "/");
        }
        if (safe.length() > 1 && safe.endsWith("/")) {
            safe = safe.substring(0, safe.length() - 1);
        }
        if (safe.length() > MAX_PATH || safe.contains("/../") || safe.endsWith("/..")
                || safe.contains("/./") || safe.endsWith("/.")) {
            throw new IllegalArgumentException("Invalid SignalNet page path: " + value);
        }
        for (int i = 0; i < safe.length(); i++) {
            char c = safe.charAt(i);
            boolean allowed = c >= 'a' && c <= 'z'
                    || c >= '0' && c <= '9'
                    || c == '/' || c == '-' || c == '_' || c == '.';
            if (!allowed) {
                throw new IllegalArgumentException("Invalid SignalNet page path: " + value);
            }
        }
        return safe;
    }

    public static String titleFromPath(String path) {
        String safe = cleanPath(path);
        if ("/".equals(safe)) {
            return "Home";
        }
        int slash = safe.lastIndexOf('/');
        String name = slash < 0 ? safe : safe.substring(slash + 1);
        return name.replace('_', ' ').replace('-', ' ');
    }

    private static List<String> cleanTags(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.strip().toLowerCase(Locale.ROOT))
                .distinct()
                .limit(16)
                .toList();
    }

    private static List<SignalOsNetPage> cleanPages(String address, List<SignalOsNetPage> values) {
        List<SignalOsNetPage> safe = values == null || values.isEmpty()
                ? List.of(new SignalOsNetPage("/", "Home", "", List.of(), 0))
                : values.stream().filter(java.util.Objects::nonNull).toList();
        Map<String, SignalOsNetPage> byPath = new LinkedHashMap<>();
        for (SignalOsNetPage page : safe) {
            if (byPath.putIfAbsent(page.path(), page) != null) {
                throw new IllegalArgumentException("Duplicate SignalNet page path " + page.path() + " on " + address);
            }
        }
        return byPath.values().stream()
                .sorted(java.util.Comparator.comparingInt(SignalOsNetPage::order)
                        .thenComparing(SignalOsNetPage::path))
                .toList();
    }
}
