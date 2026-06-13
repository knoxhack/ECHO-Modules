package com.knoxhack.echo.assetpipeline;

import java.util.List;

public final class EchoAssetPipeline {
    public static final String MODID = "echoassetpipeline";
    public static final List<String> REQUIRES = List.of(
            "echocore",
            "echoadaptercore",
            "echoassetcore",
            "echotextureforge",
            "echosoundcore"
        );
    public static final List<String> PROVIDES = List.of(
            "assetpipeline.audit",
            "assetpipeline.thumbnails",
            "assetpipeline.manifests"
        );
    public static final List<String> MVP_CONTRACTS = List.of(
            "asset_audit",
            "preview_thumbnail_manifest",
            "missing_asset_report"
        );

    public EchoAssetPipeline() {
        bootstrap();
    }

    public void bootstrap() {
    }

    public String moduleId() {
        return MODID;
    }

    public List<String> provides() {
        return PROVIDES;
    }
}
