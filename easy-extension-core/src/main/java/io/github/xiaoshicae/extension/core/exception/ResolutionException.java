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
        /** More than one business matched (in strict and non-strict mode alike). */
        MULTIPLE_BUSINESSES_MATCHED,
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
