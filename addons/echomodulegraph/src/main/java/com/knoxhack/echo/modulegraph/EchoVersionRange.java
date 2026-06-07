package com.knoxhack.echo.modulegraph;

import com.knoxhack.echo.platformcore.EchoModuleVersion;

public record EchoVersionRange(String value) {
    public EchoVersionRange {
        value = ModuleGraphContractGuards.optionalText(value);
    }

    public static EchoVersionRange any() {
        return new EchoVersionRange("");
    }

    public static EchoVersionRange exact(String version) {
        return new EchoVersionRange(ModuleGraphContractGuards.requireText(version, "version"));
    }

    public boolean anyVersion() {
        return value.isEmpty() || "*".equals(value);
    }

    public boolean accepts(EchoModuleVersion version) {
        if (version == null || anyVersion()) {
            return true;
        }
        String actual = version.value();
        if (value.equals(actual)) {
            return true;
        }
        if ((value.startsWith("[") || value.startsWith("(")) && (value.endsWith("]") || value.endsWith(")")) && value.contains(",")) {
            String body = value.substring(1, value.length() - 1);
            String[] parts = body.split(",", 2);
            String min = parts[0].trim();
            String max = parts.length > 1 ? parts[1].trim() : "";
            boolean minOk = min.isEmpty() || actual.compareTo(min) >= 0;
            boolean maxOk = max.isEmpty() || actual.compareTo(max) < 0 || (value.endsWith("]") && actual.compareTo(max) == 0);
            return minOk && maxOk;
        }
        return false;
    }
}
