package com.jme3.util.struct;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.jme3.util.SafeArrayList;

/**
 * StructUtils
 * @author Riccardo Balbo
 */
public class StructUtils {
    private static final Comparator<StructField> fieldComparator = new Comparator<StructField>() {
        @Override
        public int compare(final StructField a, final StructField b) {
            return a.getPosition() - b.getPosition();
        }
    };


    public static List<StructField> sortFields(final List<StructField> fields) {
        fields.sort(fieldComparator);
        return fields;
    }

    public static List<StructField> getFromClass(final Struct struct) {
        return getFromClass(struct, new ArrayList<Field>());
    }

    private static List<StructField> getFromClass(final Struct struct, final List<Field> fieldList) {
        final Class<? extends Struct> structClass = struct.getClass();
        final SafeArrayList<StructField> structFields = new SafeArrayList<StructField>(StructField.class);
        try {
            final Field[] fields = structClass.getDeclaredFields();
            for (final Field field : fields) {
                fieldList.add(field);

                field.setAccessible(true);
                final Object o = field.get(struct);
                if (o instanceof StructField) {
                    final StructField<?> so = (StructField<?>) o;
                    if (so.getValue() instanceof Struct) {
                        final List<StructField> subStruct = getFromClass((Struct<?>) so.getValue(), fieldList);
                        final StructField<List<StructField>> subField = new StructField<List<StructField>>(so.getPosition(), subStruct);
                        subField.fieldPtr = fieldList.toArray(new Field[0]);
                        structFields.expandAndSet(so.getPosition(), subField, () -> null);
                    } else  if (so.getValue().getClass().isArray()&&Struct.class.isAssignableFrom(so.getValue().getClass().getComponentType())) {
                        throw new RuntimeException("Array of structs not supported yet");
                    } else {
                        so.fieldPtr = fieldList.toArray(new Field[0]);
                        structFields.expandAndSet(so.getPosition(), so, () -> null);
                    }
                }

                fieldList.remove(fieldList.size() - 1);

            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
        assert structFields.size() != 0;
        return structFields;
    }

    public StructField getValueFromRoot(final Struct<?> root, final StructField field) throws IllegalArgumentException, IllegalAccessException {
        Object co = root;
        for (final Field f : field.fieldPtr) co = f.get(co);
        return (StructField) co;
    }

    public StructField getValue(final Struct<?> struct, final StructField field) throws IllegalArgumentException, IllegalAccessException {
        final Object co = field.fieldPtr[field.fieldPtr.length - 1].get(struct);
        return (StructField) co;
    }

    // public static SafeArrayList<Field> getFields(Class<? extends Struct> f) {
    // SafeArrayList<Field> structFields = new SafeArrayList<Field>(Field.class);
    // try {
    // Field[] fields = f.getDeclaredFields();
    // for (Field field : fields) {
    // field.setAccessible(true);
    // Object o = field.get(f);
    // if (o instanceof StructField) {
    // StructField<?> sfield = (StructField<?>) o;
    // structFields.expandAndSet(sfield.getPosition(), field, () -> null);
    // }
    // }
    // } catch (Exception e) {
    // throw new RuntimeException(e);
    // }
    // assert structFields.size()!=0;
    // return structFields;
    // }

    // public static <T> StructField<T> getFieldValue(Struct s, Field field) {
    // try {
    // return (StructField<T>) field.get(s);
    // } catch (Exception e) {
    // throw new RuntimeException();
    // }
    // }

}