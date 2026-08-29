package com.jme3.util.struct;

import com.jme3.math.FastMath;
import com.jme3.vulkan.alloc.BufferDescription;

import java.util.Objects;

/**
 * Struct field member that serializes and deserializes values to native memory
 * relative to the bound struct's memory address.
 *
 * @param <T>
 */
public interface StructField <T> extends BufferDescription {

    /**
     * Binds this field to the Struct {@code struct}.
     *
     * @param struct struct to bind to
     */
    void bind(Struct struct);

    /**
     * Computes the layout of this field.
     *
     * @param offset recommended offset for the field (can use a greater offset but not a lesser offset)
     * @return computed offset of this field
     */
    int layout(int offset);

    /**
     * Gets the struct that this field is computed relative to.
     *
     * @return struct
     */
    Struct getStruct();

    /**
     * Serializes {@code value} to the proper memory address through the
     * bound field description.
     *
     * @param value value to serialize
     */
    void set(T value);

    /**
     * Assigns {@code value} to this field's {@link #alias()}.
     *
     * @param value value to assign to the alias
     */
    void alias(T value);

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
     * Gets the offset in bytes of this field within its struct.
     *
     * @return offset in bytes in struct
     */
    int getStructLocalOffset();

    /**
     * Gets the alignment in bytes of this field. The managing struct
     * must be bound.
     *
     * @return alignment in bytes
     */
    int alignment();

    /**
     * Serializes {@link #alias()} to the proper memory address through
     * the bound field description.
     */
    default void set() {
        set(alias());
    }

    /**
     * Assigns {@code value} to the alias and {@link #set(Object) sets} {@code value}.
     *
     * @param value value to assign
     */
    default void aliasAndSet(T value) {
        alias(value);
        set(value);
    }

    /**
     * Compares {@code value} against the value returned by {@link #get()}. If they are
     * not {@link Objects#equals(Object, Object) equal}, then {@code value} is {@link #set(Object)}.
     *
     * @param value value to compare and set
     * @return true if the value was set
     */
    default boolean compareAndSet(T value) {
        T current = get();
        if (!Objects.equals(current, value)) {
            set(value);
            return true;
        }
        return false;
    }

    /**
     * Gets the aligned size of this field, which is {@link #size()}
     * rounded up to the nearest multiple of {@link #alignment()}.
     *
     * @return aligned size in bytes
     */
    default int alignedSize() {
        return FastMath.toMultipleOf(size(), alignment());
    }
}
