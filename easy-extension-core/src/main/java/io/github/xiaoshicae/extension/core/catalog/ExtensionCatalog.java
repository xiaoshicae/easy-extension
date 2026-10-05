package io.github.xiaoshicae.extension.core.catalog;

import java.util.List;

/**
 * Read-only description of everything registered in a context.
 *
 * @param matcherParamType type of the request parameter; {@code null} if it could not be determined
 */
public record ExtensionCatalog(List<ExtensionPointInfo> extensionPoints, List<AbilityInfo> abilities,
                               List<BusinessInfo> businesses, List<DefaultImplementationInfo> defaultImplementations,
                               Class<?> matcherParamType) {
}
