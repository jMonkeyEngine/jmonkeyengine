package com.jme3.util;

import com.jme3.system.Annotations.Internal;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The factory of buffer allocators.
 *
 * @author JavaSaBR
 */
@Internal
public class BufferAllocatorFactory {

    public static final String PROPERTY_BUFFER_ALLOCATOR_IMPLEMENTATION = "com.jme3.BufferAllocatorImplementation";

    private static final Logger LOGGER = Logger.getLogger(BufferAllocatorFactory.class.getName());

    @Internal
    protected static BufferAllocator create() {

        final String className = System.getProperty(PROPERTY_BUFFER_ALLOCATOR_IMPLEMENTATION, ReflectionAllocator.class.getName());
        try {
            return (BufferAllocator) Class.forName(className).newInstance();
        } catch (final Throwable e) {
            LOGGER.log(Level.WARNING, "Unable to access {0}", className);
            return new PrimitiveAllocator();
        }
    }
}
