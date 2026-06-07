package com.knoxhack.echo.creatorcore.api;

public record CreatorProject(
        String id,
        String displayName,
        String pack,
        String root,
        boolean writable) {
    public CreatorProject {
        id = id == null || id.isBlank() ? "default" : id;
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        pack = pack == null || pack.isBlank() ? "default" : pack;
        root = root == null ? "" : root;
    }
}
