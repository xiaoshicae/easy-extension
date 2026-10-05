package io.github.xiaoshicae.extension.core.exception;

/**
 * The extension setup is invalid. Thrown while building the context, never at request time.
 */
public class RegistrationException extends ExtensionException {
    /** Creates the exception. */
    public RegistrationException(String message) {
        super(message);
    }

    /** Creates the exception with a cause. */
    public RegistrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
