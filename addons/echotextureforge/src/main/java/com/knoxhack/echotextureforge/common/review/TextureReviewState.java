package com.knoxhack.echotextureforge.common.review;

import java.time.Instant;
import java.util.List;

public record TextureReviewState(
        Instant updatedAt,
        List<TextureReviewEntry> entries) {
    public TextureReviewState {
        updatedAt = updatedAt == null ? Instant.now() : updatedAt;
        entries = entries == null ? List.of() : List.copyOf(entries);
    }
}
