package com.jme3.shader.bufferobject.layout;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.jme3.math.FastMath;
import com.jme3.util.functional.Function;

/**
 * Layout serializer for buffers
 * 
 * @author Riccardo Balbo
 */
public abstract class  BufferLayout {

    public static abstract class ObjectSerializer<T> {
        private Function<Boolean, Object> filter;

        public ObjectSerializer(Class<T> cls) {
            this(obj -> {
                Class<?> objc = obj instanceof Class ? (Class<?>) obj : obj.getClass();
                return cls.isAssignableFrom(objc);
            });

        }

        public ObjectSerializer(Function<Boolean, Object> filter) {
            this.filter = filter;
        }

        public final boolean canSerialize(Object obj) {
            return filter.eval(obj);
        }

        public abstract int length(BufferLayout layout, T obj);

        public abstract int basicAlignment(BufferLayout layout, T obj);

        public abstract void write(BufferLayout layout, ByteBuffer bbf, T obj);
    }

    protected List<ObjectSerializer<?>> serializers = new ArrayList<ObjectSerializer<?>>();

    protected ObjectSerializer<?> getSerializer(Object obj) {
        for (int i = serializers.size() - 1; i >= 0; i--) {
            ObjectSerializer<?> sr = serializers.get(i);
            if (sr.canSerialize(obj)) return sr;
            
        }
        throw new RuntimeException("Serializer not found for " + obj + " of type " + obj.getClass());
    }

    /**
     * Register a serializer
     * 
     * @param type
     */
    protected void registerSerializer(ObjectSerializer<?> serializer) {
        serializers.add(serializer);
    }
    
    /**
     * Estimate size of Object when serialized accordingly with std140
     * 
     * @param o
     *            the object to serialize
     * @return the size
     */
    public int estimateSize(Object o) {
        ObjectSerializer s = getSerializer(o);
        return s.length(this, o);
    }
    /**
     * Get basic alignment of Object when serialized accordingly with std140
     * 
     * @param o
     *            the object to serialize
     * @return the basic alignment
     */

    public int getBasicAlignment(Object o) {
        ObjectSerializer s = getSerializer(o);
        return s.basicAlignment(this, o);
    }
    /**
     * Align a position to the given basicAlignment
     * 
     * @param pos
     *            the position to align
     * @param basicAlignment
     *            the basic alignment
     * @return the aligned position
     */
    public int align(int pos, int basicAlignment) {
        return pos==0?pos:FastMath.toMultipleOf(pos, basicAlignment);
    }

    /**
     * Serialize an object accordingly with the std140 layout and write the
     * result to a BufferObject
     * 
     * @param out
     *            the output BufferObject where the object will be serialized
     *            (starting from the current position)
     * @param o
     *            the Object to serialize
     */
    public void write(ByteBuffer out, Object o) {
        ObjectSerializer s = getSerializer(o);        
        s.write(this, out, o);
    }


    public abstract String getId();
}
