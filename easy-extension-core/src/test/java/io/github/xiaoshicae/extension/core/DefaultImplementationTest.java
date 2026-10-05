package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.core.catalog.ExtensionCatalog;
import io.github.xiaoshicae.extension.core.exception.RegistrationException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.github.xiaoshicae.extension.core.Fixtures.*;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultImplementationTest {

    @ExtensionPoint
    public interface Greeting {
        String greet(String name);
    }

    @ExtensionPoint
    public interface Hook {
        void run(List<String> trail);

        default void runTwice(List<String> trail) {
            run(trail);
            run(trail);
        }
    }

    @Test
    public void testLambdaAsDefaultImplementation() {
        ExtensionContext<Param> context = ExtensionContext.<Param>builder().strict(false)
                .extensionPoint(Greeting.class)
                .defaultImplementationFor(Greeting.class, name -> "hello " + name)
                .build();

        assertEquals("hello bob", context.resolve(Param.of("x")).first(Greeting.class).greet("bob"));
        assertEquals(1, context.catalog().defaultImplementations().size());
        assertEquals(List.of(Greeting.class), context.catalog().defaultImplementations().get(0).extensionPoints());
    }

    @Test
    public void testLambdaDefaultCannotBeNull() {
        RegistrationException e = assertThrows(RegistrationException.class, () -> ExtensionContext.<Param>builder()
                .extensionPoint(Greeting.class).defaultImplementationFor(Greeting.class, null));
        assertEquals("implementation should not be null", e.getMessage());
    }

    @Test
    public void testLambdaDefaultStillConflictsWithAnotherDefault() {
        RegistrationException e = assertThrows(RegistrationException.class, () -> ExtensionContext.<Param>builder()
                .extensionPoint(Greeting.class)
                .defaultImplementationFor(Greeting.class, name -> "a")
                .defaultImplementationFor(Greeting.class, name -> "b")
                .build());
        assertTrue(e.getMessage().startsWith("extension point [" + Greeting.class.getName() + "] has more than one default implementation"));
    }

    @Test
    public void testNoOpDefaultKeepsDefaultMethods() {
        ExtensionContext<Param> context = ExtensionContext.<Param>builder().strict(false).extensionPoint(Hook.class).build();

        List<String> trail = new ArrayList<>();
        Hook hook = context.resolve(Param.of("x")).first(Hook.class);
        hook.run(trail);
        hook.runTwice(trail);
        assertEquals(List.of(), trail);
        assertEquals(hook, hook);
        assertNotEquals(hook, new Object());
        assertEquals(hook.hashCode(), hook.hashCode());
    }

    @Test
    public void testNoOpDefaultIsNotListedAsAnExplicitDefaultImplementation() {
        ExtensionCatalog catalog = ExtensionContext.<Param>builder().extensionPoint(Hook.class).build().catalog();

        assertEquals(1, catalog.extensionPoints().size());
        assertEquals(List.of(), catalog.defaultImplementations());
    }

    @Test
    public void testExtensionPointWithAReturnValueStillNeedsAnExplicitDefault() {
        RegistrationException e = assertThrows(RegistrationException.class,
                () -> ExtensionContext.<Param>builder().extensionPoint(Greeting.class).build());
        assertTrue(e.getMessage().contains("only an extension point whose methods all return void"));
    }
}
