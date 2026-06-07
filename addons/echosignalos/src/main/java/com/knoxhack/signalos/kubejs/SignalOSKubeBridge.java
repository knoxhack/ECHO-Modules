package com.knoxhack.signalos.kubejs;

import com.knoxhack.signalos.api.TerminalArchiveRecord;
import com.knoxhack.signalos.api.TerminalChapter;
import com.knoxhack.signalos.api.TerminalMission;
import com.knoxhack.signalos.content.SignalOsContentRegistry;

/**
 * KubeJS-friendly bridge that is safe without a compile-time KubeJS dependency.
 * Scripts can access it with Java.loadClass and call the fluent builders.
 */
public final class SignalOSKubeBridge {
    private SignalOSKubeBridge() {
    }

    public static void clearScriptContent() {
        SignalOsContentRegistry.clearScriptContent();
    }

    public static ChapterScriptBuilder chapter(String id) {
        return new ChapterScriptBuilder(id);
    }

    public static MissionScriptBuilder mission(String id) {
        return new MissionScriptBuilder(id);
    }

    public static ArchiveScriptBuilder archive(String id) {
        return new ArchiveScriptBuilder(id);
    }

    public static final class ChapterScriptBuilder {
        private final TerminalChapter.Builder delegate;

        private ChapterScriptBuilder(String id) {
            this.delegate = TerminalChapter.builder(id);
        }

        public ChapterScriptBuilder title(String title) {
            delegate.title(title);
            return this;
        }

        public ChapterScriptBuilder section(String section) {
            delegate.section(section);
            return this;
        }

        public ChapterScriptBuilder order(int order) {
            delegate.order(order);
            return this;
        }

        public ChapterScriptBuilder accentColor(int color) {
            delegate.accentColor(color);
            return this;
        }

        public ChapterScriptBuilder page(String page) {
            delegate.page(page);
            return this;
        }

        public ChapterScriptBuilder visible(boolean visible) {
            delegate.visible(visible);
            return this;
        }

        public TerminalChapter register() {
            TerminalChapter chapter = delegate.build();
            SignalOsContentRegistry.registerScriptChapter(chapter);
            return chapter;
        }
    }

    public static final class MissionScriptBuilder {
        private final TerminalMission.Builder delegate;

        private MissionScriptBuilder(String id) {
            this.delegate = TerminalMission.builder(id);
        }

        public MissionScriptBuilder chapter(String chapter) {
            delegate.chapter(chapter);
            return this;
        }

        public MissionScriptBuilder title(String title) {
            delegate.title(title);
            return this;
        }

        public MissionScriptBuilder description(String description) {
            delegate.description(description);
            return this;
        }

        public MissionScriptBuilder objective(String objective) {
            delegate.objective(objective);
            return this;
        }

        public MissionScriptBuilder order(int order) {
            delegate.order(order);
            return this;
        }

        public MissionScriptBuilder icon(String itemId) {
            delegate.icon(itemId);
            return this;
        }

        public MissionScriptBuilder completionAdvancement(String advancementId) {
            delegate.completionAdvancement(advancementId);
            return this;
        }

        public MissionScriptBuilder rewardClaim(boolean rewardClaim) {
            delegate.rewardClaim(rewardClaim);
            return this;
        }

        public MissionScriptBuilder reward(String itemId, int count) {
            delegate.reward(itemId, count);
            return this;
        }

        public TerminalMission register() {
            TerminalMission mission = delegate.build();
            SignalOsContentRegistry.registerScriptMission(mission);
            return mission;
        }
    }

    public static final class ArchiveScriptBuilder {
        private final TerminalArchiveRecord.Builder delegate;

        private ArchiveScriptBuilder(String id) {
            this.delegate = TerminalArchiveRecord.builder(id);
        }

        public ArchiveScriptBuilder chapter(String chapter) {
            delegate.chapter(chapter);
            return this;
        }

        public ArchiveScriptBuilder title(String title) {
            delegate.title(title);
            return this;
        }

        public ArchiveScriptBuilder group(String group) {
            delegate.group(group);
            return this;
        }

        public ArchiveScriptBuilder status(String status) {
            delegate.status(status);
            return this;
        }

        public ArchiveScriptBuilder order(int order) {
            delegate.order(order);
            return this;
        }

        public ArchiveScriptBuilder line(String line) {
            delegate.line(line);
            return this;
        }

        public ArchiveScriptBuilder locked(boolean locked) {
            delegate.locked(locked);
            return this;
        }

        public TerminalArchiveRecord register() {
            TerminalArchiveRecord archive = delegate.build();
            SignalOsContentRegistry.registerScriptArchive(archive);
            return archive;
        }
    }
}
