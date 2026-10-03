package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.ability.IAbility;
import io.github.xiaoshicae.extension.core.business.IBusiness;
import io.github.xiaoshicae.extension.core.business.UsedAbility;
import io.github.xiaoshicae.extension.core.extension.IExtensionPointGroupDefaultImplementation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Fingerprint of a registry: a short hash over everything that decides how a request resolves, namely the
 * extension points and the codes, priorities, used abilities and implemented extension points of the businesses,
 * abilities and default implementations. Two registries with the same fingerprint resolve a chain the same way.
 */
final class RegistryFingerprint {

    private static final int HEX_BYTES = 8;

    private RegistryFingerprint() {
    }

    static String of(Collection<Class<?>> extensionPoints,
                     List<? extends IAbility<?>> abilities,
                     List<? extends IBusiness<?>> businesses,
                     List<? extends IExtensionPointGroupDefaultImplementation<?>> defaults) {
        List<String> lines = new ArrayList<>();
        for (Class<?> extensionPoint : extensionPoints) {
            lines.add("extension-point:" + extensionPoint.getName());
        }
        for (IAbility<?> ability : abilities) {
            lines.add("ability:" + ability.code() + ":" + names(ability.implementExtensionPoints()));
        }
        for (IBusiness<?> business : businesses) {
            lines.add("business:" + business.code() + ":" + business.priority()
                    + ":" + usedAbilities(business) + ":" + names(business.implementExtensionPoints()));
        }
        for (IExtensionPointGroupDefaultImplementation<?> impl : defaults) {
            lines.add("default:" + impl.code() + ":" + impl.priority() + ":" + names(impl.implementExtensionPoints()));
        }
        Collections.sort(lines);

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(String.join("\n", lines).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, HEX_BYTES);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is required of every Java platform implementation
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static String names(List<Class<?>> extensionPoints) {
        return extensionPoints.stream().map(Class::getName).sorted().collect(Collectors.joining(","));
    }

    private static String usedAbilities(IBusiness<?> business) {
        List<UsedAbility> used = business.usedAbilities();
        if (used == null) {
            return "";
        }
        return used.stream().map(u -> u.code() + "@" + u.priority()).sorted().collect(Collectors.joining(","));
    }
}
