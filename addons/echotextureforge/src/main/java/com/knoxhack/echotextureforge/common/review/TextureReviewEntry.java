package com.knoxhack.echotextureforge.common.review;

public record TextureReviewEntry(
        String specId,
        String generatedFilePath,
        String targetOutputPath,
        TextureReviewStatus status,
        String notes,
        String timestamp,
        String sourceSheet,
        String sourcePrompt) {
    public TextureReviewEntry {
        specId = clean(specId);
        generatedFilePath = clean(generatedFilePath);
        targetOutputPath = clean(targetOutputPath);
        status = status == null ? TextureReviewStatus.PENDING : status;
        notes = clean(notes);
        timestamp = clean(timestamp);
        sourceSheet = clean(sourceSheet);
        sourcePrompt = clean(sourcePrompt);
    }

    private static String clean(String value) {
        return value == null ? "" : value.strip();
    }
}
