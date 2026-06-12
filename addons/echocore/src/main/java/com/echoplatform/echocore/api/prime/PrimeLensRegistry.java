package com.echoplatform.echocore.api.prime;

import java.util.List;
import net.minecraft.resources.Identifier;

public interface PrimeLensRegistry {
    boolean registerScanType(PrimeScanType scanType);

    boolean registerScanData(PrimeScanData value);

    List<PrimeScanType> scanTypes();

    List<PrimeScanData> scanData();

    record PrimeScanType(
            Identifier id,
            String title,
            String key,
            int order) {
        public PrimeScanType {
            title = title == null ? "" : title;
            key = key == null ? "" : key;
        }
    }

    record PrimeScanData(
            Identifier id,
            Identifier scanType,
            String target,
            String uses,
            String threat,
            String drops,
            String hint,
            String sourceModule) {
        public PrimeScanData {
            target = target == null ? "" : target;
            uses = uses == null ? "" : uses;
            threat = threat == null ? "" : threat;
            drops = drops == null ? "" : drops;
            hint = hint == null ? "" : hint;
            sourceModule = sourceModule == null ? "" : sourceModule;
        }
    }
}
