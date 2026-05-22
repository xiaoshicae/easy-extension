package io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 从 classpath 读取所有 META-INF/easy-extension/metadata.json 文件，
 * 提供按全限定类名查询元数据的能力。
 * <p>
 * 零外部依赖 JSON 解析，只提取需要的字段 (qualifiedName, sourceCode, javadoc)。
 * </p>
 */
public class MetadataJsonReader {
    private static final Logger logger = LoggerFactory.getLogger(MetadataJsonReader.class);
    private static final String METADATA_RESOURCE = "META-INF/easy-extension/metadata.json";
    private static final String METADATA_PATTERN = "classpath*:" + METADATA_RESOURCE;

    private Map<String, MetadataEntry> entries = Map.of();
    private ClassLoader loader;

    /**
     * 从 classpath 加载所有 metadata.json 文件。
     * 使用 Spring ResourcePatternResolver 确保在所有 ClassLoader 层级中都能找到资源。
     */
    public void load(ResourcePatternResolver resolver) {
        Map<String, MetadataEntry> result = new LinkedHashMap<>();
        try {
            Resource[] resources = resolver.getResources(METADATA_PATTERN);
            logger.info("Found {} metadata.json resources on classpath", resources.length);
            for (Resource resource : resources) {
                try (InputStream is = resource.getInputStream()) {
                    String content = readFully(is);
                    parseAndMerge(content, result);
                } catch (Exception e) {
                    logger.warn("Failed to parse easy-extension metadata from {}: {}", resource, e.getMessage());
                }
            }
        } catch (IOException e) {
            logger.warn("Failed to scan easy-extension metadata resources via ResourcePatternResolver", e);
        }

        // Fallback: try with multiple ClassLoaders if nothing found via ResourcePatternResolver
        if (result.isEmpty()) {
            loadViaClassLoaders(result);
        }

        // 记住首次加载时的 ClassLoader，供后续 reload() 使用，避免 ClassLoader 不一致导致条目丢失
        if (this.loader == null) {
            this.loader = Thread.currentThread().getContextClassLoader();
        }

        this.entries = result;
        if (!result.isEmpty()) {
            logger.info("Loaded {} class metadata entries from easy-extension metadata.json", result.size());
        } else {
            logger.info("No easy-extension metadata.json found on classpath");
        }
    }

    /**
     * 使用初始加载时相同的 ClassLoader 重新加载 metadata.json。
     * 在缓存刷新时调用，避免因 ClassLoader 不一致导致已加载的条目丢失。
     */
    public void reload() {
        ClassLoader cl = this.loader != null ? this.loader : MetadataJsonReader.class.getClassLoader();
        load(new PathMatchingResourcePatternResolver(cl));
    }

    private void loadViaClassLoaders(Map<String, MetadataEntry> result) {
        ClassLoader[] classLoaders = {
                MetadataJsonReader.class.getClassLoader(),
                Thread.currentThread().getContextClassLoader(),
                ClassLoader.getSystemClassLoader()
        };
        for (ClassLoader cl : classLoaders) {
            if (cl == null) continue;
            try {
                var resources = cl.getResources(METADATA_RESOURCE);
                while (resources.hasMoreElements()) {
                    var url = resources.nextElement();
                    logger.info("Found metadata.json via classloader fallback: {}", url);
                    try (InputStream is = url.openStream()) {
                        parseAndMerge(readFully(is), result);
                    }
                }
                if (!result.isEmpty()) return;
            } catch (IOException e) {
                // try next classloader
            }
        }
    }

    /**
     * 按全限定类名查找元数据。
     *
     * @return 元数据条目，未找到返回 null
     */
    public MetadataEntry findByQualifiedName(String qualifiedName) {
        return entries.get(qualifiedName);
    }

    private String readFully(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            char[] buf = new char[4096];
            int len;
            while ((len = reader.read(buf)) != -1) {
                sb.append(buf, 0, len);
            }
        }
        return sb.toString();
    }

    /**
     * 简单 JSON 解析：按 "classes" 数组中的对象逐个提取字段。
     * 格式由 easy-extension-annotation-processor 的 JsonWriter 生成，格式固定可控。
     */
    private void parseAndMerge(String json, Map<String, MetadataEntry> target) {
        int classesStart = json.indexOf("\"classes\"");
        if (classesStart < 0) return;

        // 找到 classes 数组的起始 [
        int arrayStart = json.indexOf('[', classesStart);
        if (arrayStart < 0) return;

        int pos = arrayStart + 1;
        while (pos < json.length()) {
            int objStart = json.indexOf('{', pos);
            if (objStart < 0) break;

            int objEnd = findMatchingBrace(json, objStart);
            if (objEnd < 0) break;

            String objStr = json.substring(objStart, objEnd + 1);
            String qualifiedName = extractJsonStringField(objStr, "qualifiedName");
            if (qualifiedName != null && !qualifiedName.isEmpty()) {
                String sourceCode = extractJsonStringField(objStr, "sourceCode");
                String javadoc = extractJsonStringField(objStr, "javadoc");
                target.put(qualifiedName, new MetadataEntry(qualifiedName,
                        sourceCode != null ? sourceCode : "",
                        javadoc != null ? javadoc : ""));
            }

            pos = objEnd + 1;
        }
    }

    /**
     * 找到与 startPos 处 '{' 匹配的 '}'，处理嵌套和字符串。
     */
    private int findMatchingBrace(String json, int startPos) {
        int depth = 0;
        boolean inString = false;
        boolean escape = false;

        for (int i = startPos; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escape) {
                escape = false;
                continue;
            }
            if (c == '\\' && inString) {
                escape = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if (inString) continue;

            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    /**
     * 从 JSON 对象字符串中提取指定 key 的 string 值。
     * 处理 JSON 转义字符。
     */
    private String extractJsonStringField(String obj, String key) {
        String search = "\"" + key + "\"";
        int keyPos = obj.indexOf(search);
        if (keyPos < 0) return null;

        // 找到冒号后的引号
        int colonPos = obj.indexOf(':', keyPos + search.length());
        if (colonPos < 0) return null;

        int valueStart = obj.indexOf('"', colonPos + 1);
        if (valueStart < 0) return null;

        // 找到结束引号（处理转义）
        StringBuilder result = new StringBuilder();
        boolean escape = false;
        for (int i = valueStart + 1; i < obj.length(); i++) {
            char c = obj.charAt(i);
            if (escape) {
                switch (c) {
                    case '"' -> result.append('"');
                    case '\\' -> result.append('\\');
                    case 'n' -> result.append('\n');
                    case 'r' -> result.append('\r');
                    case 't' -> result.append('\t');
                    case 'b' -> result.append('\b');
                    case 'f' -> result.append('\f');
                    case 'u' -> {
                        if (i + 4 < obj.length()) {
                            String hex = obj.substring(i + 1, i + 5);
                            result.append((char) Integer.parseInt(hex, 16));
                            i += 4;
                        }
                    }
                    default -> result.append(c);
                }
                escape = false;
                continue;
            }
            if (c == '\\') {
                escape = true;
                continue;
            }
            if (c == '"') break;
            result.append(c);
        }
        return result.toString();
    }

    public static class MetadataEntry {
        private final String qualifiedName;
        private final String sourceCode;
        private final String javadoc;

        public MetadataEntry(String qualifiedName, String sourceCode, String javadoc) {
            this.qualifiedName = qualifiedName;
            this.sourceCode = sourceCode;
            this.javadoc = javadoc;
        }

        public String getQualifiedName() {
            return qualifiedName;
        }

        public String getSourceCode() {
            return sourceCode;
        }

        public String getJavadoc() {
            return javadoc;
        }
    }
}
