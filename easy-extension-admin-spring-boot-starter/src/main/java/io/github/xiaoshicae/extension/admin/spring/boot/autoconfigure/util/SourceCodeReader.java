package io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.util;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class SourceCodeReader {
    private static final Logger logger = Logger.getLogger(SourceCodeReader.class.getName());

    private static final String searchPathPrefix = "classpath:";
    private static final String sourceCodeSuffix = ".java";
    private static final Map<String, String> sourceCodeCache = new ConcurrentHashMap<>();

    private final ResourceLoader resourceLoader;

    public SourceCodeReader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public String readSourceCode(Class<?> clazz) {
        String sourceCodePath = clazz.getName().replace(".", "/") + sourceCodeSuffix;
        String searchPath = searchPathPrefix + sourceCodePath;

        String sourceCode = sourceCodeCache.get(searchPath);
        if (!Objects.isNull(sourceCode)) {
            return sourceCode;
        }

        Resource resource = resourceLoader.getResource(searchPath);
        try {
            sourceCode = resource.getContentAsString(Charset.defaultCharset());
        } catch (Exception ignore) {
            logger.fine(String.format("Easy Extension Admin readSourceCode failed, resource not found, searchPath: %s, maybe your pom.xml should include ${your-dependency}-sources.jar", searchPath));
        }

        sourceCode = Objects.isNull(sourceCode) ? "" : sourceCode;
        sourceCodeCache.put(searchPath, sourceCode);
        return sourceCode;
    }
}
