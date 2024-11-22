package nl.jqno.equalsverifier.internal.reflection.vintage.prefabvalues.factories;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import nl.jqno.equalsverifier.internal.reflection.*;
import nl.jqno.equalsverifier.internal.reflection.instantiation.ValueProvider.Attributes;
import nl.jqno.equalsverifier.internal.reflection.instantiation.VintageValueProvider;
import nl.jqno.equalsverifier.internal.reflection.vintage.ClassAccessor;
import org.objenesis.Objenesis;

/**
 * Implementation of {@link PrefabValueFactory} that instantiates types "by force".
 *
 * <p>It instantiates the type using bytecode magic, bypassing the constructor. Then it uses {@link
 * VintageValueProvider} to fill up all the fields, recursively.
 */
public class FallbackFactory<T> implements PrefabValueFactory<T> {

    private final Objenesis objenesis;

    public FallbackFactory(Objenesis objenesis) {
        this.objenesis = objenesis;
    }

    @Override
    public Tuple<T> createValues(
        TypeTag tag,
        VintageValueProvider valueProvider,
        Attributes attributes
    ) {
        Attributes clone = attributes.cloneAndAdd(tag);

        Class<T> type = tag.getType();
        if (type.isEnum()) {
            return giveEnumInstances(tag);
        }
        if (type.isArray()) {
            return giveArrayInstances(tag, valueProvider, clone);
        }

        traverseFields(tag, valueProvider, clone);
        return giveInstances(tag, valueProvider, clone);
    }

    private Tuple<T> giveEnumInstances(TypeTag tag) {
        Class<T> type = tag.getType();
        T[] enumConstants = type.getEnumConstants();

        switch (enumConstants.length) {
            case 0:
                return new Tuple<>(null, null, null);
            case 1:
                return new Tuple<>(enumConstants[0], enumConstants[0], enumConstants[0]);
            default:
                return new Tuple<>(enumConstants[0], enumConstants[1], enumConstants[0]);
        }
    }

    @SuppressWarnings("unchecked")
    private Tuple<T> giveArrayInstances(
        TypeTag tag,
        VintageValueProvider valueProvider,
        Attributes attributes
    ) {
        Class<T> type = tag.getType();
        Class<?> componentType = type.getComponentType();
        TypeTag componentTag = new TypeTag(componentType);
        valueProvider.realizeCacheFor(componentTag, attributes);

        T red = (T) Array.newInstance(componentType, 1);
        Array.set(red, 0, valueProvider.giveRed(componentTag));
        T blue = (T) Array.newInstance(componentType, 1);
        Array.set(blue, 0, valueProvider.giveBlue(componentTag));
        T redCopy = (T) Array.newInstance(componentType, 1);
        Array.set(redCopy, 0, valueProvider.giveRed(componentTag));

        return new Tuple<>(red, blue, redCopy);
    }

    private void traverseFields(
        TypeTag tag,
        VintageValueProvider valueProvider,
        Attributes attributes
    ) {
        Class<?> type = tag.getType();
        for (Field field : FieldIterable.of(type)) {
            FieldProbe probe = FieldProbe.of(field);
            boolean isStaticAndFinal = probe.isStatic() && probe.isFinal();
            if (!isStaticAndFinal) {
                valueProvider.realizeCacheFor(TypeTag.of(field, tag), attributes);
            }
        }
    }

    private Tuple<T> giveInstances(
        TypeTag tag,
        VintageValueProvider valueProvider,
        Attributes attributes
    ) {
        ClassAccessor<T> accessor = ClassAccessor.of(tag.getType(), valueProvider, objenesis);
        T red = accessor.getRedObject(tag, attributes);
        T blue = accessor.getBlueObject(tag, attributes);
        T redCopy = accessor.getRedObject(tag, attributes);
        return new Tuple<>(red, blue, redCopy);
    }
}
