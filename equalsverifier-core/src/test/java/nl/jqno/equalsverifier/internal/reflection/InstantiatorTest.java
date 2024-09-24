package nl.jqno.equalsverifier.internal.reflection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.List;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import nl.jqno.equalsverifier.testhelpers.types.ColorBlindColorPoint;
import nl.jqno.equalsverifier.testhelpers.types.FinalPoint;
import nl.jqno.equalsverifier.testhelpers.types.Point;
import nl.jqno.equalsverifier.testhelpers.types.TypeHelper.AbstractClass;
import nl.jqno.equalsverifier.testhelpers.types.TypeHelper.ArrayContainer;
import nl.jqno.equalsverifier.testhelpers.types.TypeHelper.Interface;
import org.junit.jupiter.api.Test;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;
import org.w3c.dom.Element;

public class InstantiatorTest {

    private Objenesis objenesis = new ObjenesisStd();

    @Test
    public void instantiateClass() {
        Instantiator<Point> instantiator = Instantiator.of(Point.class, objenesis);
        Point p = instantiator.instantiate();
        assertEquals(Point.class, p.getClass());
    }

    @Test
    public void fieldsOfInstantiatedObjectHaveDefaultValues() {
        ColorBlindColorPoint p = Instantiator
            .of(ColorBlindColorPoint.class, objenesis)
            .instantiate();
        assertEquals(0, p.x);
        assertEquals(null, p.color);
    }

    @Test
    public void instantiateInterface() {
        Instantiator<Interface> instantiator = Instantiator.of(Interface.class, objenesis);
        Interface i = instantiator.instantiate();
        assertTrue(Interface.class.isAssignableFrom(i.getClass()));
    }

    @Test
    public void instantiateFinalClass() {
        Instantiator.of(FinalPoint.class, objenesis);
    }

    @Test
    public void instantiateArrayContainer() {
        Instantiator.of(ArrayContainer.class, objenesis);
    }

    @Test
    public void instantiateAbstractClass() {
        Instantiator<AbstractClass> instantiator = Instantiator.of(AbstractClass.class, objenesis);
        AbstractClass ac = instantiator.instantiate();
        assertTrue(AbstractClass.class.isAssignableFrom(ac.getClass()));
    }

    @Test
    public void instantiateSubclass() {
        Instantiator<Point> instantiator = Instantiator.of(Point.class, objenesis);
        Point p = instantiator.instantiateAnonymousSubclass();
        assertFalse(p.getClass() == Point.class);
        assertTrue(Point.class.isAssignableFrom(p.getClass()));
    }

    @Test
    public void instantiateAnNonToplevelClass() {
        class Something {}
        Instantiator<Something> instantiator = Instantiator.of(Something.class, objenesis);
        instantiator.instantiateAnonymousSubclass();
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void instantiateJavaApiClassWhichHasBootstrapClassLoader() {
        Instantiator instantiator = Instantiator.of(List.class, objenesis);
        instantiator.instantiateAnonymousSubclass();
    }

    @Test
    public void instantiateOrgW3cDomClassWhichHasBootstrapClassLoader() {
        Instantiator<Element> instantiator = Instantiator.of(Element.class, objenesis);
        instantiator.instantiateAnonymousSubclass();
    }

    @Test
    public void instantiateTheSameSubclass() {
        Instantiator<Point> instantiator = Instantiator.of(Point.class, objenesis);
        Class<?> expected = instantiator.instantiateAnonymousSubclass().getClass();
        Class<?> actual = instantiator.instantiateAnonymousSubclass().getClass();
        assertEquals(expected, actual);
    }

    @Test
    public void giveDynamicSubclass() throws Exception {
        class Super {}
        Class<?> sub = Instantiator.giveDynamicSubclass(
            Super.class,
            "dynamicField",
            b -> b.defineField("dynamicField", int.class, Visibility.PRIVATE)
        );
        Field f = sub.getDeclaredField("dynamicField");
        assertNotNull(f);
    }

    @Test
    public void giveDynamicSubclassForClassWithNoPackage() {
        Class<?> type = new ByteBuddy()
            .with(TypeValidation.DISABLED)
            .subclass(Object.class)
            .name("NoPackage")
            .make()
            .load(getClass().getClassLoader())
            .getLoaded();
        Instantiator.giveDynamicSubclass(type, "X", b -> b);
    }
}
