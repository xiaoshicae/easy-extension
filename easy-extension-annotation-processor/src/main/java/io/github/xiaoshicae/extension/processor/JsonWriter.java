package io.github.xiaoshicae.extension.processor;

import java.util.List;
import java.util.Map;

/**
 * 零依赖 JSON 序列化，用于生成 metadata.json。
 */
public final class JsonWriter {

    private JsonWriter() {
    }

    public static String toJson(String version, String generatedAt, List<ClassMetadata> classes) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"version\": ").append(quote(version)).append(",\n");
        sb.append("  \"generatedAt\": ").append(quote(generatedAt)).append(",\n");
        sb.append("  \"classes\": [\n");
        for (int i = 0; i < classes.size(); i++) {
            writeClassMetadata(sb, classes.get(i), "    ");
            if (i < classes.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]\n");
        sb.append("}");
        return sb.toString();
    }

    static void writeClassMetadata(StringBuilder sb, ClassMetadata m, String indent) {
        sb.append(indent).append("{\n");
        sb.append(indent).append("  \"className\": ").append(quote(m.className())).append(",\n");
        sb.append(indent).append("  \"qualifiedName\": ").append(quote(m.qualifiedName())).append(",\n");
        sb.append(indent).append("  \"annotationType\": ").append(quote(m.annotationType())).append(",\n");
        sb.append(indent).append("  \"sourceCode\": ").append(quote(m.sourceCode())).append(",\n");
        sb.append(indent).append("  \"javadoc\": ").append(quote(m.javadoc())).append(",\n");
        sb.append(indent).append("  \"annotationAttributes\": ");
        writeValue(sb, m.annotationAttributes(), indent + "  ");
        sb.append("\n");
        sb.append(indent).append("}");
    }

    @SuppressWarnings("unchecked")
    static void writeValue(StringBuilder sb, Object value, String indent) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String s) {
            sb.append(quote(s));
        } else if (value instanceof Number n) {
            sb.append(n);
        } else if (value instanceof Boolean b) {
            sb.append(b);
        } else if (value instanceof Map<?, ?> map) {
            writeMap(sb, (Map<String, Object>) map, indent);
        } else if (value instanceof List<?> list) {
            writeArray(sb, list, indent);
        } else {
            sb.append(quote(value.toString()));
        }
    }

    private static void writeMap(StringBuilder sb, Map<String, Object> map, String indent) {
        if (map.isEmpty()) {
            sb.append("{}");
            return;
        }
        sb.append("{\n");
        var entries = map.entrySet().stream().toList();
        for (int i = 0; i < entries.size(); i++) {
            var entry = entries.get(i);
            sb.append(indent).append("  ").append(quote(entry.getKey())).append(": ");
            writeValue(sb, entry.getValue(), indent + "  ");
            if (i < entries.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append(indent).append("}");
    }

    private static void writeArray(StringBuilder sb, List<?> list, String indent) {
        if (list.isEmpty()) {
            sb.append("[]");
            return;
        }
        sb.append("[");
        for (int i = 0; i < list.size(); i++) {
            writeValue(sb, list.get(i), indent);
            if (i < list.size() - 1) sb.append(", ");
        }
        sb.append("]");
    }

    static String quote(String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append("\"");
        return sb.toString();
    }
}
