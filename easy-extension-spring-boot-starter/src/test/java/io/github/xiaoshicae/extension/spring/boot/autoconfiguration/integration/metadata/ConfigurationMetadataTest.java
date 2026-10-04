package io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration.metadata;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What IDEs show for {@code easy-extension.*}: the metadata that ends up in the jar. The spring-boot-configuration-processor
 * writes {@code META-INF/spring-configuration-metadata.json} from the Javadoc of the properties class and replaces the
 * hand-written file of the same name in {@code src/main/resources}, so the shipped text is the Javadoc.
 */
public class ConfigurationMetadataTest {

    private static String shippedMetadata() throws IOException {
        try (InputStream in = ConfigurationMetadataTest.class.getClassLoader().getResourceAsStream("META-INF/spring-configuration-metadata.json")) {
            assertNotNull(in, "the starter ships configuration metadata");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String descriptionOf(String json, String property) {
        Matcher matcher = Pattern.compile("\"name\"\\s*:\\s*\"" + Pattern.quote(property) + "\".*?\"description\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"", Pattern.DOTALL).matcher(json);
        assertTrue(matcher.find(), "no description for " + property);
        return matcher.group(1);
    }

    @Test
    public void testAllowUnknownBusinessIsDocumentedAsTheLegacySwitchOfThePolicies() throws IOException {
        String description = descriptionOf(shippedMetadata(), "easy-extension.allow-unknown-business");

        assertTrue(description.contains("unknown-business-policy") && description.contains("multi-match-policy"), description);
    }

    @Test
    public void testBusinessMatchOrderIsDocumentedAgainstTheMultiMatchPolicy() throws IOException {
        String description = descriptionOf(shippedMetadata(), "easy-extension.business-match-order");

        assertTrue(description.contains("multi-match-policy"), description);
    }
}
