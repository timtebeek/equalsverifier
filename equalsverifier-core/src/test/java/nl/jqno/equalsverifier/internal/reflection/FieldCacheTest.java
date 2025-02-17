package nl.jqno.equalsverifier.internal.reflection;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class FieldCacheTest {

    private String stringField = "string";
    private final Tuple<String> stringValues = Tuple.of("red", "blue", "red");

    private String intField = "int";
    private final Tuple<Integer> intValues = Tuple.of(1, 2, 1);

    private FieldCache cache = new FieldCache();

    @Test
    void putAndGetTuple() {
        cache.put(stringField, stringValues);
        assertThat(cache.get(stringField)).isEqualTo(stringValues);
    }

    @Test
    void putTwiceAndGetBoth() {
        cache.put(stringField, stringValues);
        cache.put(intField, intValues);

        assertThat(cache.get(intField)).isEqualTo(intValues);
        assertThat(cache.get(stringField)).isEqualTo(stringValues);
    }

    @Test
    void putNullAndGetNothingBack() {
        cache.put(null, stringValues);
        assertThat(cache.get(null)).isNull();
    }

    @Test
    void contains() {
        cache.put(stringField, stringValues);
        assertThat(cache.contains(stringField)).isTrue();
    }

    @Test
    void doesntContain() {
        assertThat(cache.contains(stringField)).isFalse();
    }

    @Test
    void getFieldNames() {
        assertThat(cache.getFieldNames()).isEqualTo(Collections.emptySet());

        cache.put(stringField, stringValues);
        Set<String> expected = new HashSet<>();
        expected.add(stringField);
        assertThat(cache.getFieldNames()).isEqualTo(expected);
    }
}
