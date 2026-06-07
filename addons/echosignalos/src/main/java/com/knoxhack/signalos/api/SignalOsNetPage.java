package com.knoxhack.signalos.api;

import java.util.List;

/**
 * One page inside a curated SignalNet site.
 */
public record SignalOsNetPage(
        String path,
        String title,
        String body,
        List<SignalOsNetLink> links,
        int order) {
    public SignalOsNetPage {
        path = SignalOsNetSite.cleanPath(path);
        title = title == null || title.isBlank() ? SignalOsNetSite.titleFromPath(path) : title.strip();
        body = body == null ? "" : body.strip();
        links = List.copyOf(links == null ? List.of() : links.stream()
                .filter(java.util.Objects::nonNull)
                .toList());
    }
}
