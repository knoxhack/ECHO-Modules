package com.knoxhack.signalos.kubejs;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Script-facing facade shaped like a KubeJS event group while staying compile-time optional.
 */
public final class SignalOSEvents {
    private SignalOSEvents() {
    }

    public static void content(Consumer<ContentEvent> consumer) {
        Objects.requireNonNull(consumer, "consumer").accept(new ContentEvent());
    }

    public static final class ContentEvent {
        private ContentEvent() {
        }

        public SignalOSKubeBridge.ChapterScriptBuilder chapter(String id) {
            return SignalOSKubeBridge.chapter(id);
        }

        public SignalOSKubeBridge.MissionScriptBuilder mission(String id) {
            return SignalOSKubeBridge.mission(id);
        }

        public SignalOSKubeBridge.ArchiveScriptBuilder archive(String id) {
            return SignalOSKubeBridge.archive(id);
        }

        public void clear() {
            SignalOSKubeBridge.clearScriptContent();
        }
    }
}
