package io.github.xiaoshicae.extension.core.exception;

/**
 * The extension setup is invalid. Thrown while building the context, never at request time.
 */
public class RegistrationException extends ExtensionException {
    public RegistrationException(String message) {
        super(message);
    }

    public RegistrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
