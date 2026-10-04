package io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The admin shows the source of businesses, abilities and defaults; the project requires JDK 17, so the sources use
 * what JDK 17 has: switch expressions, text blocks, records, pattern matching for {@code instanceof}.
 */
public class ClassUtilsModernSyntaxTest {

    private static void assertSourceIsKept(String source) {
        String shown = ClassUtils.parseClassInfo(source).getSourceCode();

        assertTrue(shown.contains("class Retail"), "the class is shown, was: [" + shown + "]");
    }

    @Test
    public void testPatternMatchingForInstanceofIsParsed() {
        assertSourceIsKept("public class Retail { String f(Object o) { return o instanceof String s ? s : \"x\"; } }");
    }

    @Test
    public void testSwitchExpressionIsParsed() {
        assertSourceIsKept("public class Retail { String f(int i) { return switch (i) { case 1 -> \"one\"; default -> \"other\"; }; } }");
    }

    @Test
    public void testTextBlockIsParsed() {
        assertSourceIsKept("public class Retail { String f() { return \"\"\"\n text\n \"\"\"; } }");
    }

    @Test
    public void testRecordIsParsed() {
        assertSourceIsKept("public class Retail { record Rate(int percent) {} String f() { return \"\"; } }");
    }
}
