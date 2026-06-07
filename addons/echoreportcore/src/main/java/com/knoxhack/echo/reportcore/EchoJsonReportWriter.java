package com.knoxhack.echo.reportcore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class EchoJsonReportWriter implements EchoReportWriter {
    @Override
    public String writeToString(EchoReportEnvelope envelope) {
        if (envelope == null) {
            throw new IllegalArgumentException("report envelope must not be null");
        }
        StringBuilder json = new StringBuilder();
        writeValue(json, envelopeToMap(envelope), 0);
        json.append("\n");
        return json.toString();
    }

    @Override
    public void write(Path path, EchoReportEnvelope envelope) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("report path must not be null");
        }
        Files.createDirectories(path.toAbsolutePath().normalize().getParent());
        Files.writeString(path, writeToString(envelope), StandardCharsets.UTF_8);
    }

    private static Map<String, Object> envelopeToMap(EchoReportEnvelope envelope) {
        return Map.of(
                "schema", envelope.schema().value(),
                "generatedAt", envelope.generatedAt().toString(),
                "generator", envelope.generator().value(),
                "workspace", envelope.workspace(),
                "addonSet", envelope.addonSet(),
                "packId", envelope.packId() == null ? "" : envelope.packId().value(),
                "status", envelope.status().serializedName(),
                "summary", summaryToMap(envelope.summary()),
                "issues", envelope.issues().stream().map(EchoJsonReportWriter::issueToMap).toList(),
                "data", envelope.data()
        );
    }

    private static Map<String, Object> summaryToMap(EchoReportSummary summary) {
        return Map.of(
                "warnings", summary.warnings(),
                "errors", summary.errors(),
                "notices", summary.notices(),
                "fatals", summary.fatals(),
                "attributes", summary.attributes()
        );
    }

    private static Map<String, Object> issueToMap(EchoReportIssue issue) {
        return Map.of(
                "code", issue.code(),
                "severity", issue.severity().serializedName(),
                "summary", issue.summary(),
                "likelyFiles", issue.likelyFiles(),
                "suggestedFix", issue.suggestedFix(),
                "blocking", issue.blocking(),
                "attributes", issue.attributes()
        );
    }

    private static void writeValue(StringBuilder json, Object value, int indent) {
        if (value == null) {
            json.append("null");
        } else if (value instanceof String string) {
            writeString(json, string);
        } else if (value instanceof Number || value instanceof Boolean) {
            json.append(value);
        } else if (value instanceof Map<?, ?> map) {
            writeObject(json, map, indent);
        } else if (value instanceof Iterable<?> iterable) {
            writeArray(json, iterable, indent);
        } else if (value instanceof Enum<?> enumValue) {
            writeString(json, enumValue.name());
        } else {
            writeString(json, value.toString());
        }
    }

    private static void writeObject(StringBuilder json, Map<?, ?> map, int indent) {
        json.append("{");
        List<Map.Entry<?, ?>> entries = new ArrayList<>(map.entrySet());
        entries.sort(Comparator.comparing(entry -> String.valueOf(entry.getKey())));
        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<?, ?> entry = entries.get(i);
            json.append("\n");
            writeIndent(json, indent + 2);
            writeString(json, String.valueOf(entry.getKey()));
            json.append(": ");
            writeValue(json, entry.getValue(), indent + 2);
            if (i + 1 < entries.size()) {
                json.append(",");
            }
        }
        if (!entries.isEmpty()) {
            json.append("\n");
            writeIndent(json, indent);
        }
        json.append("}");
    }

    private static void writeArray(StringBuilder json, Iterable<?> values, int indent) {
        json.append("[");
        List<Object> items = new ArrayList<>();
        values.forEach(items::add);
        for (int i = 0; i < items.size(); i++) {
            json.append("\n");
            writeIndent(json, indent + 2);
            writeValue(json, items.get(i), indent + 2);
            if (i + 1 < items.size()) {
                json.append(",");
            }
        }
        if (!items.isEmpty()) {
            json.append("\n");
            writeIndent(json, indent);
        }
        json.append("]");
    }

    private static void writeString(StringBuilder json, String value) {
        json.append("\"");
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            switch (character) {
                case '\\' -> json.append("\\\\");
                case '"' -> json.append("\\\"");
                case '\b' -> json.append("\\b");
                case '\f' -> json.append("\\f");
                case '\n' -> json.append("\\n");
                case '\r' -> json.append("\\r");
                case '\t' -> json.append("\\t");
                default -> {
                    if (character < 0x20 || character > 0x7E) {
                        json.append(String.format("\\u%04x", (int) character));
                    } else {
                        json.append(character);
                    }
                }
            }
        }
        json.append("\"");
    }

    private static void writeIndent(StringBuilder json, int indent) {
        json.append(" ".repeat(Math.max(0, indent)));
    }
}
