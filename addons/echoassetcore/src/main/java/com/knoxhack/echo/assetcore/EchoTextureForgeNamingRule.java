package com.knoxhack.echo.assetcore;

import java.util.Map;

public record EchoTextureForgeNamingRule(
        String ruleId,
        String namespace,
        String pathPrefix,
        String extension,
        boolean lowercaseRequired,
        boolean animationMetadataAllowed,
        Map<String, String> attributes
) {
    public EchoTextureForgeNamingRule {
        ruleId = AssetContractGuards.normalizedId(ruleId, "textureforge naming rule id");
        namespace = AssetContractGuards.optionalText(namespace);
        pathPrefix = AssetContractGuards.optionalText(pathPrefix).replace('\\', '/');
        extension = AssetContractGuards.optionalText(extension);
        if (extension.isBlank()) {
            extension = ".png";
        }
        attributes = AssetContractGuards.immutableMap(attributes);
    }

    public boolean matches(EchoAssetPath path) {
        if (path == null) {
            return false;
        }
        String value = path.value();
        return (pathPrefix.isBlank() || value.startsWith(pathPrefix))
                && value.endsWith(extension)
                && (!lowercaseRequired || value.equals(value.toLowerCase(java.util.Locale.ROOT)));
    }
}
