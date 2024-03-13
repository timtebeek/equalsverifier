package nl.jqno.equalsverifier.internal.checkers.fieldchecks;

import static nl.jqno.equalsverifier.internal.util.Assert.assertEquals;
import static nl.jqno.equalsverifier.internal.util.Assert.assertFalse;

import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.Set;
import nl.jqno.equalsverifier.Warning;
import nl.jqno.equalsverifier.internal.instantiation.FieldProbe;
import nl.jqno.equalsverifier.internal.instantiation.SubjectCreator;
import nl.jqno.equalsverifier.internal.prefabvalues.PrefabValues;
import nl.jqno.equalsverifier.internal.prefabvalues.TypeTag;
import nl.jqno.equalsverifier.internal.reflection.ClassAccessor;
import nl.jqno.equalsverifier.internal.reflection.FieldAccessor;
import nl.jqno.equalsverifier.internal.reflection.annotations.AnnotationCache;
import nl.jqno.equalsverifier.internal.reflection.annotations.NonnullAnnotationVerifier;
import nl.jqno.equalsverifier.internal.reflection.annotations.SupportedAnnotations;
import nl.jqno.equalsverifier.internal.util.Configuration;
import nl.jqno.equalsverifier.internal.util.Formatter;

public class ReflexivityFieldCheck<T> implements FieldCheck<T> {

    private final SubjectCreator<T> subjectCreator;
    private final TypeTag typeTag;
    private final PrefabValues prefabValues;
    private final EnumSet<Warning> warningsToSuppress;
    private final Set<String> nonnullFields;
    private final AnnotationCache annotationCache;

    public ReflexivityFieldCheck(SubjectCreator<T> subjectCreator, Configuration<T> config) {
        this.subjectCreator = subjectCreator;
        this.typeTag = config.getTypeTag();
        this.prefabValues = config.getPrefabValues();
        this.warningsToSuppress = config.getWarningsToSuppress();
        this.nonnullFields = config.getNonnullFields();
        this.annotationCache = config.getAnnotationCache();
    }

    @Override
    public void execute(FieldProbe fieldProbe) {
        if (warningsToSuppress.contains(Warning.IDENTICAL_COPY_FOR_VERSIONED_ENTITY)) {
            return;
        }

        checkReferenceReflexivity();
        checkValueReflexivity(fieldProbe.getField());
        checkNullReflexivity(fieldProbe);
    }

    private void checkReferenceReflexivity() {
        T left = subjectCreator.plain();
        T right = subjectCreator.plain();
        checkReflexivityFor(left, right);
    }

    private void checkValueReflexivity(Field field) {
        Class<?> fieldType = field.getType();
        if (warningsToSuppress.contains(Warning.REFERENCE_EQUALITY)) {
            return;
        }
        if (fieldType.equals(Object.class) || fieldType.isInterface()) {
            return;
        }
        if (FieldAccessor.of(field).fieldIsStatic()) {
            return;
        }
        ClassAccessor<?> fieldTypeAccessor = ClassAccessor.of(fieldType, prefabValues);
        if (!fieldTypeAccessor.declaresEquals()) {
            return;
        }
        if (fieldType.isSynthetic()) {
            // Sometimes not the fieldType, but its content, is synthetic.
            return;
        }

        TypeTag tag = TypeTag.of(field, typeTag);
        Object left = subjectCreator.withFieldSetTo(field, prefabValues.giveRed(tag));
        Object right = subjectCreator.withFieldSetTo(field, prefabValues.giveRedCopy(tag));

        Formatter f = Formatter.of(
            "Reflexivity: == used instead of .equals() on field: %%" +
            "\nIf this is intentional, consider suppressing Warning.%%",
            field.getName(),
            Warning.REFERENCE_EQUALITY.toString()
        );
        assertEquals(f, left, right);
    }

    private void checkNullReflexivity(FieldProbe fieldProbe) {
        Field field = fieldProbe.getField();
        if (fieldProbe.fieldIsPrimitive() && warningsToSuppress.contains(Warning.ZERO_FIELDS)) {
            return;
        }

        boolean nullWarningIsSuppressed = warningsToSuppress.contains(Warning.NULL_FIELDS);
        boolean fieldIsNonNull = NonnullAnnotationVerifier.fieldIsNonnull(field, annotationCache);
        boolean fieldIsMentionedExplicitly = nonnullFields.contains(field.getName());
        if (nullWarningIsSuppressed || fieldIsNonNull || fieldIsMentionedExplicitly) {
            return;
        }

        T left = subjectCreator.withFieldDefaulted(field);
        T right = subjectCreator.withFieldDefaulted(field);
        checkReflexivityFor(left, right);
    }

    private void checkReflexivityFor(T left, T right) {
        if (warningsToSuppress.contains(Warning.IDENTICAL_COPY)) {
            Formatter f = Formatter.of(
                "Unnecessary suppression: %%. Two identical copies are equal.",
                Warning.IDENTICAL_COPY.toString()
            );
            assertFalse(f, left.equals(right));
        } else {
            boolean isEntity = annotationCache.hasClassAnnotation(
                typeTag.getType(),
                SupportedAnnotations.ENTITY
            );
            if (isEntity) {
                Formatter f = Formatter.of(
                    "Reflexivity: entity does not equal an identical copy of itself:\n  %%" +
                    "\nIf this is intentional, consider suppressing Warning.%%.",
                    left,
                    Warning.IDENTICAL_COPY_FOR_VERSIONED_ENTITY.toString()
                );
                assertEquals(f, left, right);
            } else {
                Formatter f = Formatter.of(
                    "Reflexivity: object does not equal an identical copy of itself:\n  %%" +
                    "\nIf this is intentional, consider suppressing Warning.%%.",
                    left,
                    Warning.IDENTICAL_COPY.toString()
                );
                assertEquals(f, left, right);
            }
        }
    }
}
