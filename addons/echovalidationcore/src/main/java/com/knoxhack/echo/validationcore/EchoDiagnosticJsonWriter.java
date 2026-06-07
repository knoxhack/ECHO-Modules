package com.knoxhack.echo.validationcore;

import java.util.List;
import java.util.function.Function;

public final class EchoDiagnosticJsonWriter {
    private EchoDiagnosticJsonWriter() {
    }

    public static String write(EchoDiagnosticReport report) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        appendField(json, "title", report.title()).append(",");
        appendField(json, "generatedAt", report.generatedAt().toString()).append(",");
        appendField(json, "highestSeverity", report.highestSeverity().serializedName()).append(",");
        json.append("\"blocking\":").append(report.hasBlockingDiagnostics()).append(",");
        json.append("\"diagnostics\":");
        appendArray(json, report.diagnostics(), EchoDiagnosticJsonWriter::writeDiagnostic);
        json.append("}");
        return json.toString();
    }

    public static String writeDiagnostic(EchoDiagnostic diagnostic) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        appendField(json, "code", diagnostic.code().value()).append(",");
        appendField(json, "severity", diagnostic.severity().serializedName()).append(",");
        appendField(json, "title", diagnostic.title()).append(",");
        appendField(json, "summary", diagnostic.summary()).append(",");
        appendField(json, "moduleId", diagnostic.moduleId() == null ? "" : diagnostic.moduleId().value()).append(",");
        appendField(json, "packId", diagnostic.packId() == null ? "" : diagnostic.packId().value()).append(",");
        appendField(json, "affectedFeature", diagnostic.affectedFeature() == null ? "" : diagnostic.affectedFeature().featureId().value()).append(",");
        appendField(json, "category", diagnostic.category().serializedName()).append(",");
        appendField(json, "cause", diagnostic.cause()).append(",");
        appendField(json, "playerFix", diagnostic.playerFix()).append(",");
        appendField(json, "developerDetails", diagnostic.developerDetails()).append(",");
        json.append("\"repairable\":").append(diagnostic.repairable()).append(",");
        json.append("\"suggestedRepairActions\":");
        appendArray(json, diagnostic.suggestedRepairActions(), EchoDiagnosticJsonWriter::writeRepairSuggestion).append(",");
        json.append("\"likelyOwners\":");
        appendArray(json, diagnostic.likelyOwners(), EchoDiagnosticJsonWriter::writeLikelyOwner).append(",");
        appendStringArrayField(json, "likelyFiles", diagnostic.likelyFiles()).append(",");
        appendField(json, "suggestedAgentLane", diagnostic.suggestedAgentLane()).append(",");
        appendStringArrayField(json, "safeCommands", diagnostic.safeCommands()).append(",");
        appendStringArrayField(json, "relatedDocs", diagnostic.relatedDocs());
        json.append("}");
        return json.toString();
    }

    private static String writeRepairSuggestion(EchoRepairSuggestion suggestion) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        appendField(json, "id", suggestion.id()).append(",");
        appendField(json, "label", suggestion.label()).append(",");
        appendField(json, "summary", suggestion.summary()).append(",");
        appendField(json, "risk", suggestion.risk()).append(",");
        json.append("\"requiresConfirmation\":").append(suggestion.requiresConfirmation()).append(",");
        appendStringArrayField(json, "actions", suggestion.actions()).append(",");
        appendStringArrayField(json, "relatedDocs", suggestion.relatedDocs());
        json.append("}");
        return json.toString();
    }

    private static String writeLikelyOwner(EchoLikelyOwner owner) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        appendField(json, "moduleId", owner.moduleId().value()).append(",");
        appendField(json, "displayName", owner.displayName()).append(",");
        appendField(json, "contactHint", owner.contactHint()).append(",");
        appendField(json, "suggestedAgentLane", owner.suggestedAgentLane());
        json.append("}");
        return json.toString();
    }

    private static StringBuilder appendStringArrayField(StringBuilder json, String name, List<String> values) {
        json.append("\"").append(escape(name)).append("\":");
        return appendArray(json, values, value -> "\"" + escape(value) + "\"");
    }

    private static StringBuilder appendField(StringBuilder json, String name, String value) {
        return json.append("\"")
                .append(escape(name))
                .append("\":\"")
                .append(escape(value))
                .append("\"");
    }

    private static <T> StringBuilder appendArray(StringBuilder json, List<T> values, Function<T, String> writer) {
        json.append("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                json.append(",");
            }
            json.append(writer.apply(values.get(i)));
        }
        json.append("]");
        return json;
    }

    private static String escape(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        StringBuilder escaped = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (c < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
                }
            }
        }
        return escaped.toString();
    }
}
