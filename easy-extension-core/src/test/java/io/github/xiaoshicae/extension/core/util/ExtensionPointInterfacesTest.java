package io.github.xiaoshicae.extension.core.util;

import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExtensionPointInterfacesTest {

    @ExtensionPoint
    public interface Base {
    }

    @ExtensionPoint
    public interface Derived extends Base {
    }

    /** Not annotated itself, but inherits from an extension point. */
    public interface Plain extends Base {
    }

    @ExtensionPoint
    public interface FromSuperclass {
    }

    @ExtensionPoint
    public interface Declared {
    }

    public interface NotAnExtensionPoint {
    }

    public static class Parent implements FromSuperclass, NotAnExtensionPoint {
    }

    public static class Child extends Parent implements Declared, Derived {
    }

    @Test
    public void testDeclaredInterfacesComeFirstFollowedByInheritedOnes() {
        assertEquals(List.of(Declared.class, Derived.class, Base.class, FromSuperclass.class),
                ExtensionPointInterfaces.implementedBy(Child.class));
    }

    @Test
    public void testInterfaceWithoutAnnotationStillContributesItsExtensionPointSuperInterfaces() {
        class Impl implements Plain {
        }
        assertEquals(List.of(Base.class), ExtensionPointInterfaces.implementedBy(Impl.class));
    }

    @Test
    public void testInterfaceReachedOnSeveralPathsIsListedOnce() {
        class Impl implements Derived, Base {
        }
        assertEquals(List.of(Derived.class, Base.class), ExtensionPointInterfaces.implementedBy(Impl.class));
    }

    /** Package-private on purpose: a JDK proxy of another package cannot implement it. */
    @ExtensionPoint
    interface NotPublic {
    }

    public static class ParentWithNotPublic implements NotPublic {
    }

    public static class ChildOfParentWithNotPublic extends ParentWithNotPublic implements Declared {
    }

    @Test
    public void testInheritedExtensionPointThatIsNotPublicIsSkipped() {
        assertEquals(List.of(Declared.class), ExtensionPointInterfaces.implementedBy(ChildOfParentWithNotPublic.class));
    }

    @Test
    public void testDeclaredExtensionPointThatIsNotPublicIsKeptSoThatItIsReported() {
        assertEquals(List.of(NotPublic.class), ExtensionPointInterfaces.implementedBy(ParentWithNotPublic.class));
    }

    @Test
    public void testClassWithoutExtensionPointsYieldsAnEmptyList() {
        assertTrue(ExtensionPointInterfaces.implementedBy(Object.class).isEmpty());
        assertTrue(ExtensionPointInterfaces.implementedBy(NotAnExtensionPoint.class).isEmpty());
        assertTrue(ExtensionPointInterfaces.implementedBy(String.class).isEmpty());
    }
}
