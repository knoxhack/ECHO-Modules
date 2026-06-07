package com.knoxhack.echolens.integration;

import com.knoxhack.echocore.api.ILensService;
import com.knoxhack.echolens.EchoLens;
import com.knoxhack.echolens.api.LensScanMode;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public enum LensCoreService implements ILensService {
    INSTANCE;

    private final Set<Identifier> scanTypes = ConcurrentHashMap.newKeySet();

    LensCoreService() {
        for (LensScanMode mode : LensScanMode.values()) {
            scanTypes.add(EchoLens.id("scan/" + mode.name().toLowerCase(java.util.Locale.ROOT)));
        }
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public boolean registerScanType(Identifier scanId, String displayName) {
        return scanId != null && scanTypes.add(scanId);
    }

    @Override
    public List<Identifier> scanTypes() {
        return scanTypes.stream()
                .sorted(Comparator.comparing(Identifier::toString))
                .toList();
    }

    @Override
    public boolean openLens(Player player) {
        return player != null;
    }
}
