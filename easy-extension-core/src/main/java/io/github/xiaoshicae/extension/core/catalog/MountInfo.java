package io.github.xiaoshicae.extension.core.catalog;

/**
 * One slot of a business's precedence order: a mounted ability, or the business itself.
 *
 * @param abilityCode code of the mounted ability; {@code null} for the business itself
 */
public record MountInfo(String abilityCode) {

    /** The business itself. */
    public static MountInfo self() {
        return new MountInfo(null);
    }

    /** A mounted ability. */
    public static MountInfo ability(String abilityCode) {
        return new MountInfo(abilityCode);
    }

    /** Whether this position is the business itself. */
    public boolean isSelf() {
        return abilityCode == null;
    }
}
