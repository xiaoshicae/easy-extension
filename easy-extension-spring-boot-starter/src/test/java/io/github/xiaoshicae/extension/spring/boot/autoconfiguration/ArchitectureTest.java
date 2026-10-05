package io.github.xiaoshicae.extension.spring.boot.autoconfiguration;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Integrations build on the public API of core only.
 */
@AnalyzeClasses(packages = "io.github.xiaoshicae.extension.spring", importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitectureTest {

    @ArchTest
    static final ArchRule starterDoesNotUseCoreInternals = noClasses().should().dependOnClassesThat()
            .resideInAPackage("io.github.xiaoshicae.extension.core.internal..");
}
