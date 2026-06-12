package com.echoplatform.echocore.api;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class EchoAddonRegistry {
    private static final Map<String, EchoAddonChapter> CHAPTERS = new LinkedHashMap<>();

    private EchoAddonRegistry() {
    }

    public static synchronized boolean register(EchoAddonChapter chapter) {
        if (chapter == null || chapter.id() == null || chapter.id().isBlank()) {
            return false;
        }
        return CHAPTERS.putIfAbsent(chapter.id(), chapter) == null;
    }

    public static boolean isRegistered(String id) {
        return CHAPTERS.containsKey(id);
    }

    public static Optional<EchoAddonChapter> find(String id) {
        return Optional.ofNullable(CHAPTERS.get(id));
    }

    public static Collection<EchoAddonChapter> all() {
        return List.copyOf(CHAPTERS.values());
    }

    public static List<EchoAddonChapter> chapters() {
        return List.copyOf(CHAPTERS.values());
    }
}
