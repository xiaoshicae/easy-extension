package io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SourceCodeReader {
    private static final Logger logger = LoggerFactory.getLogger(SourceCodeReader.class);

    private static final String searchPathPrefix = "classpath:";
    private static final String sourceCodeSuffix = ".java";
    private final Map<String, String> sourceCodeCache = new ConcurrentHashMap<>();

    private final ResourceLoader resourceLoader;

    public SourceCodeReader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * Clear the source code cache. Called when caches need to be invalidated.
     */
    public void clearCache() {
        sourceCodeCache.clear();
    }

    public String readSourceCode(Class<?> clazz) {
        String sourceCodePath = clazz.getName().replace(".", "/") + sourceCodeSuffix;
        String searchPath = searchPathPrefix + sourceCodePath;

        String sourceCode = sourceCodeCache.get(searchPath);
        if (sourceCode != null) {
            return sourceCode;
        }

        Resource resource = resourceLoader.getResource(searchPath);
        try {
            sourceCode = resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.debug("Easy Extension Admin readSourceCode failed, resource not found, searchPath: {}, maybe your pom.xml should include ${{your-dependency}}-sources.jar", searchPath);
            sourceCode = "";
        }

        sourceCodeCache.put(searchPath, sourceCode);
        return sourceCode;
    }
}
