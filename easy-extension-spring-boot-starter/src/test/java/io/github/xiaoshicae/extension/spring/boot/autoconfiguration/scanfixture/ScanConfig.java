package io.github.xiaoshicae.extension.spring.boot.autoconfiguration.scanfixture;

import io.github.xiaoshicae.extension.spring.boot.autoconfigure.EasyExtensionAutoConfiguration;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.annotation.ExtensionScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

// @ExtensionScan always scans the package of the annotated class too, so keep it inside the fixture package
@Configuration
@ExtensionScan
@Import(EasyExtensionAutoConfiguration.class)
public class ScanConfig {
}
