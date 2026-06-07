package com.knoxhack.echotextureforge.common.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TextureForgeMarkdown {
    private TextureForgeMarkdown() {
    }

    public static void write(Path path, String markdown) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, markdown == null ? "" : markdown, StandardCharsets.UTF_8);
    }

    public static String heading(String title) {
        return "# " + title + "\n\n";
    }

    public static String codeFence(String info, String body) {
        return "```" + (info == null ? "" : info) + "\n" + (body == null ? "" : body.stripTrailing()) + "\n```\n";
    }
}
