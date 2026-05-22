package io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.service;


import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.model.AbilityInfo;
import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.model.BusinessInfo;
import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.model.ClassInfo;
import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.model.ConfigInfo;
import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.model.DefaultImplInfo;
import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.model.ExtensionPointInfo;
import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.model.MatcherParamInfo;
import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.properties.EasyExtensionAdminConfigurationProperties;
import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.util.ClassUtils;
import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.util.ClassUtils.ClassInfoResult;
import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.util.MetadataJsonReader;
import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.util.SourceCodeReader;
import io.github.xiaoshicae.extension.core.IExtensionReader;
import io.github.xiaoshicae.extension.core.ability.IAbility;
import io.github.xiaoshicae.extension.core.annotation.Ability;
import io.github.xiaoshicae.extension.core.annotation.Business;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPointDefaultImplementation;
import io.github.xiaoshicae.extension.core.business.IBusiness;
import io.github.xiaoshicae.extension.core.extension.IExtensionPointGroupDefaultImplementation;
import io.github.xiaoshicae.extension.core.proxy.IProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Service that extracts extension point, ability, and business information from the extension context.
 * <p>
 * All list results are cached after the first computation to avoid repeated JavaParser invocations.
 * Cache is automatically invalidated when the Spring context is refreshed.
 * </p>
 */
public class ExtensionInfoService {
    private static final Logger logger = LoggerFactory.getLogger(ExtensionInfoService.class);
    private static final String[] prefixes = new String[]{"/**", "*/", "*", "//"};

    private static final int MAX_WALK_DEPTH = 12;
    private static final String[] SRC_ROOTS = {"src/main/java", "src/main/kotlin", "src"};
    private static final Set<String> PROJECT_ROOT_MARKERS = Set.of(
            "pom.xml", "build.gradle", "build.gradle.kts", "settings.gradle", "settings.gradle.kts");

    private final IExtensionReader<?> reader;
    private final SourceCodeReader sourceCodeReader;
    private final MetadataJsonReader metadataReader;
    private final EasyExtensionAdminConfigurationProperties properties;

    // Cache for ClassInfo objects to avoid repeated JavaParser invocations
    private final Map<Class<?>, ClassInfo> classInfoCache = new ConcurrentHashMap<>();
    // Single-entry lazy caches; computeIfAbsent guarantees one-shot computation per key.
    private static final String KEY_EXT_POINTS = "extensionPoints";
    private static final String KEY_ABILITIES = "abilities";
    private static final String KEY_BUSINESSES = "businesses";
    private static final String KEY_DEFAULT_IMPL = "defaultImpl";
    private final Map<String, Object> resultCache = new ConcurrentHashMap<>();

    public ExtensionInfoService(IExtensionReader<?> reader, SourceCodeReader sourceCodeReader,
                               MetadataJsonReader metadataReader, EasyExtensionAdminConfigurationProperties properties) {
        this.reader = reader;
        this.sourceCodeReader = sourceCodeReader;
        this.metadataReader = metadataReader;
        this.properties = properties;
    }

    /**
     * Automatically invalidate all caches when the application context is refreshed
     * (e.g., after hot-reload or dynamic re-registration of extension points).
     */
    @EventListener(ContextRefreshedEvent.class)
    public void onContextRefreshed() {
        logger.info("Context refreshed, invalidating admin info cache");
        invalidateCache();
    }

    public ConfigInfo getConfigInfo() {
        return new ConfigInfo(properties.getDocUrl(), resolveFrameworkVersion());
    }

    private String resolveFrameworkVersion() {
        try {
            java.util.Properties props = new java.util.Properties();
            var stream = ExtensionInfoService.class.getResourceAsStream(
                    "/META-INF/maven/io.github.xiaoshicae/easy-extension-admin-spring-boot-starter/pom.properties");
            if (stream != null) {
                props.load(stream);
                stream.close();
                return props.getProperty("version", "dev");
            }
        } catch (Exception ignored) {
        }
        return "dev";
    }

    public MatcherParamInfo getMatcherParamInfo() {
        return new MatcherParamInfo(resolveClassInfo(reader.getMatcherParamClass()));
    }

    public DefaultImplInfo getDefaultImplInfo() {
        return cached(KEY_DEFAULT_IMPL, () -> {
            IExtensionPointGroupDefaultImplementation<?> defaultImpl = reader.getExtensionPointDefaultImplementation();
            Class<?> clazz;
            if (defaultImpl instanceof IProxy<?> proxy) {
                clazz = resolveClassWithAnn(proxy.getInstance().getClass(), ExtensionPointDefaultImplementation.class);
            } else {
                clazz = resolveClassWithAnn(defaultImpl.getClass(), ExtensionPointDefaultImplementation.class);
            }
            return new DefaultImplInfo(resolveClassInfo(clazz));
        });
    }

    public List<ExtensionPointInfo> getAllExtensionPoints() {
        return cached(KEY_EXT_POINTS, this::computeAllExtensionPoints);
    }

    public List<AbilityInfo> getAllAbilities() {
        return cached(KEY_ABILITIES, this::computeAllAbilities);
    }

    public List<BusinessInfo> getAllBusiness() {
        return cached(KEY_BUSINESSES, this::computeAllBusiness);
    }

    @SuppressWarnings("unchecked")
    private <T> T cached(String key, Supplier<T> supplier) {
        return (T) resultCache.computeIfAbsent(key, k -> supplier.get());
    }

    /**
     * Invalidate all cached data. Called automatically on context refresh,
     * or can be called manually when extension points are modified at runtime.
     */
    public void invalidateCache() {
        classInfoCache.clear();
        sourceCodeReader.clearCache();
        metadataReader.reload();
        resultCache.clear();
    }

    private List<ExtensionPointInfo> computeAllExtensionPoints() {
        DefaultImplInfo defaultImplInfo = getDefaultImplInfo();
        String sourceCode = defaultImplInfo.classInfo().sourceCode();

        List<ExtensionPointInfo> result = new ArrayList<>();
        for (Class<?> extPointClass : reader.listAllExtensionPoint()) {
            String defaultImplCode = ClassUtils.transformSourceCodeWithInterface(sourceCode, extPointClass);
            List<String> scenarios = extractScenarios(extPointClass);
            int version = extractVersion(extPointClass);
            ExtensionPointInfo info = new ExtensionPointInfo(resolveClassInfo(extPointClass), defaultImplCode, scenarios, version);
            result.add(info);
        }

        return sortByConfiguredOrder(result, properties.getExtensionPointOrder());
    }

    /**
     * Sort extension point info list according to the configured display order.
     * Items matching the order list come first (in the specified order),
     * unlisted items are appended at the end in their original order.
     */
    private List<ExtensionPointInfo> sortByConfiguredOrder(List<ExtensionPointInfo> list, List<String> order) {
        if (order == null || order.isEmpty()) {
            return list;
        }

        // Build index map: simpleName/fullName -> position
        Map<String, Integer> orderIndex = new HashMap<>();
        for (int i = 0; i < order.size(); i++) {
            orderIndex.put(order.get(i), i);
        }

        List<ExtensionPointInfo> ordered = new ArrayList<>();
        List<ExtensionPointInfo> unordered = new ArrayList<>();

        for (ExtensionPointInfo info : list) {
            String simpleName = info.classInfo().name();
            String fullName = info.classInfo().fullName();
            if (orderIndex.containsKey(simpleName) || orderIndex.containsKey(fullName)) {
                ordered.add(info);
            } else {
                unordered.add(info);
            }
        }

        // Sort the matched items by their configured position
        ordered.sort((a, b) -> {
            int posA = getOrderPosition(a, orderIndex);
            int posB = getOrderPosition(b, orderIndex);
            return Integer.compare(posA, posB);
        });

        ordered.addAll(unordered);
        return ordered;
    }

    private int getOrderPosition(ExtensionPointInfo info, Map<String, Integer> orderIndex) {
        Integer pos = orderIndex.get(info.classInfo().name());
        if (pos != null) return pos;
        pos = orderIndex.get(info.classInfo().fullName());
        return pos != null ? pos : Integer.MAX_VALUE;
    }

    /**
     * Extract scenario list from the @ExtensionPoint annotation on the given class.
     */
    private List<String> extractScenarios(Class<?> extPointClass) {
        ExtensionPoint ann = extPointClass.getAnnotation(ExtensionPoint.class);
        if (ann == null || ann.scenarios() == null || ann.scenarios().length == 0) {
            return List.of();
        }
        return List.of(ann.scenarios());
    }

    /**
     * Extract version from the @ExtensionPoint annotation on the given class.
     */
    private int extractVersion(Class<?> extPointClass) {
        ExtensionPoint ann = extPointClass.getAnnotation(ExtensionPoint.class);
        return ann != null ? ann.version() : 1;
    }

    private List<AbilityInfo> computeAllAbilities() {
        List<AbilityInfo> result = new ArrayList<>();
        for (IAbility<?> ability : reader.listAllAbility()) {
            String code = ability.code();
            List<String> implExtensionPoints = ability.implementExtensionPoints().stream().map(Class::getName).toList();
            Class<?> clazz;
            if (ability instanceof IProxy<?> proxy) {
                clazz = resolveClassWithAnn(proxy.getInstance().getClass(), Ability.class);
            } else {
                clazz = resolveClassWithAnn(ability.getClass(), Ability.class);
            }
            result.add(new AbilityInfo(code, implExtensionPoints, resolveClassInfo(clazz)));
        }
        return result;
    }

    private List<BusinessInfo> computeAllBusiness() {
        List<BusinessInfo> result = new ArrayList<>();
        for (IBusiness<?> business : reader.listAllBusiness()) {
            String code = business.code();
            Integer priority = business.priority();
            List<BusinessInfo.UsedAbility> usedAbilities = business.usedAbilities().stream().map(e -> new BusinessInfo.UsedAbility(e.code(), e.priority())).toList();
            List<String> implementExtensionPoints = business.implementExtensionPoints().stream().map(Class::getName).toList();
            Class<?> clazz;
            if (business instanceof IProxy<?> proxy) {
                clazz = resolveClassWithAnn(proxy.getInstance().getClass(), Business.class);
            } else {
                clazz = resolveClassWithAnn(business.getClass(), Business.class);
            }
            result.add(new BusinessInfo(code, priority, usedAbilities, implementExtensionPoints, resolveClassInfo(clazz)));
        }
        return result;
    }

    private ClassInfo resolveClassInfo(Class<?> clazz) {
        return classInfoCache.computeIfAbsent(clazz, this::computeClassInfo);
    }

    /**
     * Resolve source code and javadoc for a class via the following fallback chain:
     * <ol>
     *   <li>metadata.json (generated by the annotation processor at compile time)</li>
     *   <li>sources.jar on the classpath</li>
     *   <li>filesystem walk (development-time only)</li>
     *   <li>reflection-based synthetic source</li>
     * </ol>
     * Comments are resolved independently — preferring metadata javadoc even when the source itself
     * came from a different layer.
     */
    private ClassInfo computeClassInfo(Class<?> clazz) {
        String name = clazz.getSimpleName();
        String fullName = clazz.getName();

        MetadataJsonReader.MetadataEntry metadataEntry = metadataReader.findByQualifiedName(fullName);

        if (metadataEntry != null && !metadataEntry.getSourceCode().isEmpty()) {
            String fullSource = metadataEntry.getSourceCode();
            String comment = metadataEntry.getJavadoc().isEmpty()
                    ? parseJavadocComment(ClassUtils.parseClassInfo(fullSource).getComment())
                    : metadataEntry.getJavadoc();
            logger.debug("[computeClassInfo] {} <- metadata.json ({} chars)", name, fullSource.length());
            return new ClassInfo(name, fullName, fullSource, comment);
        }

        String sourceCode = sourceCodeReader.readSourceCode(clazz);
        boolean sourceFromClasspath = !sourceCode.isEmpty();
        if (sourceFromClasspath) {
            logger.debug("[computeClassInfo] {} <- sources.jar ({} chars)", name, sourceCode.length());
        } else {
            sourceCode = readSourceFromFileSystem(fullName, clazz);
            if (sourceCode != null) {
                logger.debug("[computeClassInfo] {} <- filesystem ({} chars)", name, sourceCode.length());
            } else {
                sourceCode = ClassUtils.classInfoToString(clazz);
                logger.debug("[computeClassInfo] {} <- reflection ({} chars)", name, sourceCode.length());
            }
        }

        ClassInfoResult parsedInfo = ClassUtils.parseClassInfo(sourceCode);

        // Comment: metadata javadoc → parsed javadoc → method summary (only when source itself was synthesized)
        String comment;
        if (metadataEntry != null && !metadataEntry.getJavadoc().isEmpty()) {
            comment = metadataEntry.getJavadoc();
        } else {
            comment = parseJavadocComment(parsedInfo.getComment());
            if (comment.isEmpty() && !sourceFromClasspath) {
                comment = generateMethodSummary(clazz);
            }
        }

        return new ClassInfo(name, fullName, parsedInfo.getSourceCode(), comment);
    }

    /**
     * Generate a brief description from the class's declared public, non-default, non-synthetic methods.
     */
    private String generateMethodSummary(Class<?> clazz) {
        List<String> methodNames = new ArrayList<>();
        for (java.lang.reflect.Method m : clazz.getDeclaredMethods()) {
            if (java.lang.reflect.Modifier.isPublic(m.getModifiers()) && !m.isDefault() && !m.isSynthetic()) {
                methodNames.add(m.getName() + "()");
            }
        }
        if (methodNames.isEmpty()) {
            return "";
        }
        return "定义了 " + methodNames.size() + " 个方法: " + String.join(", ", methodNames);
    }

    /**
     * Try to locate the original source file on disk. Search order:
     * <ol>
     *   <li>Module root derived from the class's own CodeSource (independent of CWD)</li>
     *   <li>Walk up from CWD, stopping at a project-root marker</li>
     *   <li>Scan CWD's first-level subdirectories (multi-module case)</li>
     * </ol>
     * Returns {@code null} if no source file could be found — typical in production fat-jar
     * deployments where the metadata.json route should already have handled it.
     */
    private String readSourceFromFileSystem(String qualifiedName, Class<?> clazz) {
        String relativePath = qualifiedName.replace('.', '/') + ".java";

        Path codeSourceDir = resolveCodeSourceDir(clazz);
        if (codeSourceDir != null) {
            String content = walkUpForSource(codeSourceDir, relativePath);
            if (content != null) return content;
        }

        Path cwd = Path.of("").toAbsolutePath();
        String content = walkUpForSource(cwd, relativePath);
        if (content != null) return content;

        content = scanSubdirsForSource(cwd, relativePath);
        if (content != null) return content;

        logger.debug("[fsSearch] NOT FOUND: {}", qualifiedName);
        return null;
    }

    private Path resolveCodeSourceDir(Class<?> clazz) {
        try {
            var cs = clazz.getProtectionDomain().getCodeSource();
            if (cs == null || cs.getLocation() == null) return null;
            // Path.of() throws for jar:/nested: URLs (fat-jar deployments) — caller falls through.
            return Path.of(cs.getLocation().toURI());
        } catch (Exception e) {
            return null;
        }
    }

    private String walkUpForSource(Path start, String relativePath) {
        Path dir = start;
        for (int depth = 0; dir != null && depth < MAX_WALK_DEPTH; depth++) {
            String content = tryReadSource(dir, relativePath);
            if (content != null) {
                logger.debug("[fsSearch] FOUND {} at depth {} from {}", relativePath, depth, start);
                return content;
            }
            if (hasProjectRootMarker(dir)) break;
            dir = dir.getParent();
        }
        return null;
    }

    private String scanSubdirsForSource(Path baseDir, String relativePath) {
        try (var entries = Files.newDirectoryStream(baseDir)) {
            for (Path entry : entries) {
                if (Files.isDirectory(entry)) {
                    String content = tryReadSource(entry, relativePath);
                    if (content != null) {
                        logger.debug("[fsSearch] FOUND {} in subdir {}", relativePath, entry);
                        return content;
                    }
                }
            }
        } catch (IOException e) {
            // ignore
        }
        return null;
    }

    private boolean hasProjectRootMarker(Path dir) {
        for (String marker : PROJECT_ROOT_MARKERS) {
            if (Files.exists(dir.resolve(marker))) return true;
        }
        return false;
    }

    private String tryReadSource(Path baseDir, String relativePath) {
        for (String srcRoot : SRC_ROOTS) {
            Path srcFile = baseDir.resolve(srcRoot).resolve(relativePath);
            if (Files.exists(srcFile)) {
                try {
                    return Files.readString(srcFile);
                } catch (IOException e) {
                    // skip
                }
            }
        }
        return null;
    }

    private String parseJavadocComment(String rawComment) {
        if (rawComment.isEmpty()) {
            return "";
        }

        String[] items = rawComment.split("\n");
        StringBuilder builder = new StringBuilder();

        for (String item : items) {
            item = item.strip();
            for (String p : prefixes) {
                if (item.startsWith(p)) {
                    String newItem = item.substring(p.length());
                    if (!newItem.isEmpty()) {
                        builder.append(newItem.strip());
                        builder.append("\n");
                    }
                    break;
                }
            }
        }

        return builder.toString().stripTrailing();
    }

    private Class<?> resolveClassWithAnn(Class<?> clazz, Class<? extends Annotation> annotation) {
        Class<?> c = ClassUtils.resolveClassWithAnn(clazz, annotation);
        return c == null ? clazz : c;
    }
}
