package com.jme3.util.struct;

import com.jme3.math.FastMath;
import com.jme3.util.natives.Destructor;
import com.jme3.vulkan.buffer.EngineBuffer;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.util.Flag;

import java.util.Objects;

/**
 * Struct field member that serializes and deserializes values to native memory
 * relative to the bound struct's memory address.
 *
 * @param <T>
 */
public interface StructField <T> extends EngineBuffer {

    /**
     * Binds this field to the struct and memory offset.
     *
     * @param struct struct
     * @param offset memory offset from {@code struct}'s bound memory address of this field
     */
    int bind(Struct struct, int offset);

    /**
     * Gets the struct that this field is bound to.
     *
     * @return bound struct
     */
    Struct getBoundStruct();

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
     * Gets the offset in bytes of this field with its struct.
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
    int getAlignment();

    /**
     * Serializes {@link #alias()} to the proper memory address through
     * the bound field description.
     */
    default void set() {
        set(alias());
    }

    /**
     * {@link #set(Object) Sets} {@code value} and assigns it to the alias.
     *
     * @param value value to assign
     */
    default void aliasAndSet(T value) {
        set(value);
        alias(value);
    }

    /**
     * Gets the name of this field.
     *
     * @return field name
     * @throws NullPointerException if the name is null
     */
    default String requireName() {
        String n = getName();
        if (n == null) {
            throw new NullPointerException("Name required.");
        }
        return n;
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
     * Gets the aligned size of this field, which is {@link #capacity()}
     * rounded up to the nearest multiple of {@link #getAlignment()}.
     *
     * @return aligned size in bytes
     */
    default int getAlignedSize() {
        return FastMath.toMultipleOf(capacity(), getAlignment());
    }

    @Override
    default Destructor getDestructor() {
        return getBoundStruct().getDestructor();
    }

    @Override
    default void update(CommandBuffer cmd) {
        getBoundStruct().update(cmd);
    }

    @Override
    default void invalidateCache() {
        getBoundStruct().invalidateCache();
    }

    @Override
    default long getHandle() {
        return getBoundStruct().getHandle();
    }

    @Override
    default long getDeviceAddress() {
        return getBoundStruct().getDeviceAddress();
    }

    @Override
    default Flag<Role> getRoles() {
        return getBoundStruct().getRoles();
    }

    @Override
    default Flag<MemoryProp> getMemoryProperties() {
        return getBoundStruct().getMemoryProperties();
    }

    @Override
    default boolean isDeviceAccessible() {
        return getBoundStruct().isDeviceAccessible();
    }
}
