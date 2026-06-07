package com.knoxhack.echowiki.content;

import com.knoxhack.echowiki.EchoWiki;
import com.knoxhack.echowiki.platform.WikiModuleAccess;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public final class GuideBookRegistry {
    private static volatile Map<Identifier, GuideBookDefinition> dataGuideBooks = Map.of();
    private static volatile long revision;
    private static volatile GuideBookSnapshot cachedSnapshot;

    private GuideBookRegistry() {
    }

    public static synchronized void replaceData(Map<Identifier, GuideBookDefinition> guideBooks) {
        dataGuideBooks = Map.copyOf(guideBooks == null ? Map.of() : guideBooks);
        revision++;
        cachedSnapshot = null;
    }

    public static List<GuideBookDefinition> guideBooks() {
        return snapshot().guideBooks();
    }

    public static List<GuideBookDefinition> visibleGuideBooks() {
        return snapshot().visibleGuideBooks();
    }

    public static Optional<GuideBookDefinition> guideBook(Identifier id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(dataGuideBooks.get(id));
    }

    public static Optional<GuideBookDefinition> visibleGuideBook(Identifier id) {
        return guideBook(id).filter(GuideBookRegistry::isVisible);
    }

    public static int dataGuideBookCount() {
        return dataGuideBooks.size();
    }

    public static synchronized void clearDataForTests() {
        dataGuideBooks = Map.of();
        revision++;
        cachedSnapshot = null;
    }

    public static long revision() {
        return revision;
    }

    private static GuideBookSnapshot snapshot() {
        GuideBookSnapshot snapshot = cachedSnapshot;
        if (snapshot != null && snapshot.revision() == revision) {
            return snapshot;
        }
        synchronized (GuideBookRegistry.class) {
            snapshot = cachedSnapshot;
            if (snapshot != null && snapshot.revision() == revision) {
                return snapshot;
            }
            snapshot = buildSnapshot();
            cachedSnapshot = snapshot;
            return snapshot;
        }
    }

    private static GuideBookSnapshot buildSnapshot() {
        List<GuideBookDefinition> guideBooks = dataGuideBooks.values().stream()
                .sorted(Comparator.comparingInt(GuideBookDefinition::sortOrder)
                        .thenComparing(guide -> guide.title().toLowerCase(java.util.Locale.ROOT))
                        .thenComparing(guide -> guide.id().toString()))
                .toList();
        List<GuideBookDefinition> visibleGuideBooks = guideBooks.stream()
                .filter(GuideBookRegistry::isVisible)
                .toList();
        return new GuideBookSnapshot(revision, guideBooks, visibleGuideBooks);
    }

    public static boolean isVisible(GuideBookDefinition guide) {
        if (guide == null) {
            return false;
        }
        String requiredModId = guide.requiredModId();
        if (requiredModId.isBlank() || EchoWiki.MODID.equals(requiredModId)) {
            return true;
        }
        try {
            return WikiModuleAccess.isLoaded(requiredModId);
        } catch (LinkageError | RuntimeException exception) {
            return false;
        }
    }

    private record GuideBookSnapshot(
            long revision,
            List<GuideBookDefinition> guideBooks,
            List<GuideBookDefinition> visibleGuideBooks) {
    }
}
