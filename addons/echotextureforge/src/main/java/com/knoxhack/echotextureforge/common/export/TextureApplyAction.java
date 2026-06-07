package com.knoxhack.echotextureforge.common.export;

public record TextureApplyAction(
        String namespace,
        String relativePath,
        String stagedPath,
        String targetPath,
        String status,
        String message) {
}
