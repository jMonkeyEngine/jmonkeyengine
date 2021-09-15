package com.jme3.anim.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * This is a guava method used in {@link com.jme3.anim.tween.Tweens} class.
 * Maybe we should just add guava as a dependency in the engine...
 * //TODO do something about this.
 */
public class Primitives {

    /**
     * A map from primitive types to their corresponding wrapper types.
     */
    private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPER_TYPE;

    static {
        Map<Class<?>, Class<?>> primToWrap = new HashMap<>(16);

        primToWrap.put(boolean.class, Boolean.class);
        primToWrap.put(byte.class, Byte.class);
        primToWrap.put(char.class, Character.class);
        primToWrap.put(double.class, Double.class);
        primToWrap.put(float.class, Float.class);
        primToWrap.put(int.class, Integer.class);
        primToWrap.put(long.class, Long.class);
        primToWrap.put(short.class, Short.class);
        primToWrap.put(void.class, Void.class);

        PRIMITIVE_TO_WRAPPER_TYPE = Collections.unmodifiableMap(primToWrap);
    }

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private Primitives() {
    }

    /**
     * Returns the corresponding wrapper type of {@code type} if it is a primitive type; otherwise
     * returns {@code type} itself. Idempotent.
     *
     * <pre>
     *     wrap(int.class) == Integer.class
     *     wrap(Integer.class) == Integer.class
     *     wrap(String.class) == String.class
     * </pre>
     *
     * @param <T> type
     * @param type the type to be boxed (not null)
     * @return the boxed type
     */
    public static <T> Class<T> wrap(Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("type is null");
        }

        // cast is safe: long.class and Long.class are both of type Class<Long>
        @SuppressWarnings("unchecked")
        Class<T> wrapped = (Class<T>) PRIMITIVE_TO_WRAPPER_TYPE.get(type);
        return (wrapped == null) ? type : wrapped;
    }
}
