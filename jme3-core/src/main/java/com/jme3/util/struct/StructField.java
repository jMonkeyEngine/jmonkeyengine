package com.jme3.util.struct;

import java.util.Objects;

/**
 * Struct field member that serializes and deserializes values to native memory
 * relative to the bound struct's memory address.
 *
 * @param <T>
 */
public interface StructField <T> {

    /**
     * Binds this field to the struct and memory offset.
     *
     * @param struct struct
     * @param offset memory offset from {@code struct}'s bound memory address
     */
    int bind(Struct struct, int offset);

    /**
     * Serializes {@code value} to the proper memory address through the
     * bound field description.
     *
     * @param value value to serialize
     */
    void set(T value);

    /**
     * Deserializes from the proper memory address to the {@link #alias() alias}
     * through the bound field description.
     *
     * @return alias
     */
    T get();

    /**
     * Gets the alias object of this field used as temporary storage of values be
     * serialized or deserialized by this field. The alias object may be altered.
     *
     * @return alias object
     */
    T alias();

    /**
     * Gets the name of this field.
     *
     * @return name of this field
     */
    String getName();

    /**
     * Gets the size in bytes of this field. The managing struct
     * must be bound.
     *
     * @return size in bytes
     */
    int getSize();

    /**
     * Gets the alignment in bytes of this field. The managing struct
     * must be bound.
     *
     * @return alignment in bytes
     */
    int getAlignment();

    /**
     * Serializes {@link #alias()} to the proper memory address through
     * the bound field description.
     */
    default void set() {
        set(alias());
    }

    default String requireName() {
        String n = getName();
        if (n == null) {
            throw new NullPointerException("Name required.");
        }
        return n;
    }

    default boolean compareAndSet(T value) {
        T current = get();
        if (!Objects.equals(current, value)) {
            set(value);
            return true;
        }
        return false;
    }

}
