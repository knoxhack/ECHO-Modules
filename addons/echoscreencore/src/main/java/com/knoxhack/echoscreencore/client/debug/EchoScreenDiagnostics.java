package com.knoxhack.echoscreencore.client.debug;

import com.knoxhack.echoscreencore.EchoScreenCoreMod;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class EchoScreenDiagnostics {
    private final ArrayList<Issue> issues = new ArrayList<>();
    private final Set<String> once = new LinkedHashSet<>();

    public void warn(String code, String message) {
        add(Severity.WARNING, code, message);
    }

    public void warnOnce(String code, String message) {
        String key = code + ":" + message;
        if (once.add(key)) {
            warn(code, message);
        }
    }

    public void error(String code, String message) {
        add(Severity.ERROR, code, message);
    }

    public List<Issue> issues() {
        return List.copyOf(issues);
    }

    public void clearFrameTransient() {
        issues.removeIf(issue -> issue.transientFrame());
    }

    public void clear() {
        issues.clear();
        once.clear();
    }

    private void add(Severity severity, String code, String message) {
        Issue issue = new Issue(severity, safe(code), safe(message), false);
        issues.add(issue);
        if (severity == Severity.ERROR) {
            EchoScreenCoreMod.LOGGER.warn("ScreenCore {}: {}", issue.code(), issue.message());
        } else {
            EchoScreenCoreMod.LOGGER.debug("ScreenCore {}: {}", issue.code(), issue.message());
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public enum Severity {
        WARNING,
        ERROR
    }

    public record Issue(Severity severity, String code, String message, boolean transientFrame) {
    }
}
