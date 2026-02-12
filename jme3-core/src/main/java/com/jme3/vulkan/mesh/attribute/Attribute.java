package com.jme3.vulkan.mesh.attribute;

import com.jme3.vulkan.buffers.MappableBuffer;

/**
 * Interacts with a vertex or instance attribute stored in a buffer.
 *
 * @param <T> object type the attribute represents
 */
public interface Attribute <T> {

    /**
     * Unmaps this attribute from the vertex buffer.
     */
    void unmap();

    /**
     * Sets the property described by this attribute of the element
     * at the index with {@code value}.
     *
     * @param element element index
     * @param value value to assign
     */
    void set(int element, T value);

    /**
     * Gets the property described by this attribute of the element
     * at the index.
     *
     * @param element element index
     * @return value to assign
     */
    T get(int element);

    /**
     * Gets the property described by this attribute of the element at the index.
     * If {@code store} is not null, implementations may choose to store the
     * property's value in {@code store} and return {@code store}.
     *
     * @param element index of element
     * @param store object to store element's value
     * @return object storing the element's value (can be {@code store})
     */
    T get(int element, T store);

    /**
     * {@link MappableBuffer#push(int, int) Pushes} the elements
     * in this attribute's vertex buffer in the described region.
     *
     * @param baseElement index of the first element to push
     * @param elements number of elements to push
     */
    void push(int baseElement, int elements);

    /**
     * {@link MappableBuffer#push() Pushes} all elements in this attribute's vertex buffer.
     */
    void push();

    /**
     * Returns an Iterable for iterating over each element of this attribute.
     * If {@code store} is not null, implementations may choose to use {@code store}
     * as each iteration's object.
     *
     * @param store object to use as each element (or null to create a new object each time)
     * @return iterable
     */
    Iterable<T> read(T store);

    /**
     * Returns an Iterable for iterating over each element of this attribute.
     * If an iteration's object is modified, the change is reflected in this
     * attribute. If {@code store} is not null, implementations may choose to
     * use {@code store} as each iteration's object.
     *
     * @param store object to use as each element (or null to create a new object each time)
     * @return iterable
     */
    Iterable<T> readWrite(T store);

    /**
     * Returns an iterable for iterating over each element of this attribute.
     * Iteration objects are not assigned with the corresponding element's value.
     * All elements are assigned with the corresponding iteration's object at the
     * end of each iteration. In other words, the iterator writes, but does not
     * read, to the attribute. The initial state of an iteration's object is
     * undefined.
     *
     * @param store object to use as each element (cannot be null)
     * @return iterable
     */
    Iterable<T> write(T store);

    /**
     * Returns an Iterable for iterating over each element index in this attribute.
     *
     * @return index iterable
     */
    Iterable<Integer> indices();

    /**
     * Assigns {@code values} each element in order starting from {@code startElement}.
     *
     * @param startElement index of element to start at
     * @param values values to assign (one value per element)
     * @see #set(int, Object)
     */
    default void set(int startElement, T[] values) {
        for (int i = 0; i < values.length; i++) {
            set(startElement + i, values[i]);
        }
    }

    /**
     * Gets the values of each vertex buffer element for each element of {@code store},
     * starting at {@code startElement}. If an element of {@code store} is not null,
     * implements may choose to store the corresponding vertex buffer element's
     * value in that object. Otherwise each element of {@code store} is assigned with
     * a new object containing the corresponding vertex buffer element's value.
     *
     * @param startElement index of element to start at
     * @param store array defining the number of elements to read and optionally the
     *              objects to hold the results
     * @return {@code store} holding the value's of each element in the region
     */
    default T[] get(int startElement, T[] store) {
        for (int i = 0; i < store.length; i++) {
            store[i] = get(startElement + i, store[i]);
        }
        return store;
    }

    /**
     * Returns an Iterable for iterating over each element of this attribute.
     * A new object is created for each iteration.
     *
     * @return iterable
     * @see #read(Object)
     */
    default Iterable<T> read() {
        return read(null);
    }

    /**
     * Returns an Iterable for iterating over each element of this attribute.
     * A new object is created for each iteration.
     *
     * @return iterable
     * @see #readWrite(Object)
     */
    default Iterable<T> readWrite() {
        return read(null);
    }

}
