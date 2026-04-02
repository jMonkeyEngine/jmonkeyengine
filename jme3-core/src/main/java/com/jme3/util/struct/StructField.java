package com.jme3.util.struct;

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
     * Enables or disables this field. When enabled, the field will be mapped to an offset
     * relative to its struct and read/write from that location. When disabled, no data
     * will be read or written on {@link #get()} or {@link #set()}.
     *
     * <p>If changed, the struct should be {@link Struct#computeOffsets() recomputed}.</p>
     *
     * @param enable true to enable this field
     */
    void enable(boolean enable);

    /**
     * Sets the name of this field. When created inside a {@link Struct}, the name will be
     * assigned reflectively if no name is assigned manually.
     *
     * @param name field name
     */
    void setName(String name);

    /**
     * Gets the name of this field.
     *
     * @return name of this field
     */
    String getName();

    /**
     * Gets the object type represented by this field. This is used for selecting
     * the field description to bind to this field.
     *
     * @return object type
     */
    Class getType();

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
     * Returns true if this field is {@link #enable(boolean) enabled}.
     *
     * @return true if enabled
     */
    boolean isEnabled();

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

}
