package io.github.xiaoshicae.extension.core;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * core is a plain Java library: JDK and slf4j only.
 */
@AnalyzeClasses(packages = "io.github.xiaoshicae.extension.core", importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitectureTest {

    @ArchTest
    static final ArchRule coreDoesNotDependOnSpring = noClasses().should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta..", "javax.servlet..");
}
