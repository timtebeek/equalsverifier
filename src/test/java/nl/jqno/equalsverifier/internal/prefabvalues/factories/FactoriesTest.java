package nl.jqno.equalsverifier.internal.prefabvalues.factories;

import static nl.jqno.equalsverifier.testhelpers.Util.coverThePrivateConstructor;

import org.junit.Test;

public class FactoriesTest {
    @Test
    public void coverTheConstructor() {
        coverThePrivateConstructor(Factories.class);
    }
}
