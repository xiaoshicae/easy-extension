package io.github.xiaoshicae.extension.spring.boot.autoconfiguration.ifacefixture;

import io.github.xiaoshicae.extension.spring.boot.autoconfigure.EasyExtensionAutoConfiguration;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.annotation.ExtensionScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ExtensionScan
@Import(EasyExtensionAutoConfiguration.class)
public class IfaceConfig {
}
