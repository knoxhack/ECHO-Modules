package com.knoxhack.echo.metadatacore;

import java.nio.file.Path;
import java.util.List;

public interface EchoMetadataScanner {
    EchoMetadataScanResult scan(Path workspaceRoot, List<Path> moduleRoots, String addonSet);
}
