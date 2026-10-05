package io.github.xiaoshicae.extension.core.exception;

/**
 * Resolving or looking up an extension failed at request time.
 */
public class ResolutionException extends ExtensionException {

    /**
     * Why resolution failed.
     */
    public enum Reason {
        /** No resolution is bound to the current thread. */
        NO_BINDING,
        /** Strict mode and no business matched. */
        NO_BUSINESS_MATCHED,
        /** Strict mode and more than one business matched. */
        MULTIPLE_BUSINESSES_MATCHED,
        /** A business code produced by a resolver or selector is not registered or did not match. */
        BUSINESS_NOT_FOUND,
        /** No implementation, and no default implementation, for the extension point. */
        EXTENSION_NOT_FOUND
    }

    private final Reason reason;

    /** Creates the exception. */
    public ResolutionException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    /** Why the resolution failed. */
    public Reason reason() {
        return reason;
    }
}
