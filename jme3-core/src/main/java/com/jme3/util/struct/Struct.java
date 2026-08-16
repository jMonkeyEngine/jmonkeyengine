package com.jme3.util.struct;

import com.jme3.export.*;
import com.jme3.math.FastMath;
import com.jme3.util.natives.Destructor;
import com.jme3.vulkan.alloc.RelativeBuffer;
import com.jme3.vulkan.buffer.DataBuffer;
import com.jme3.vulkan.buffer.EngineBuffer;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.util.Flag;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Defines the layout of properties in native memory. In contrast to traditional struct patterns,
 * {@code Struct} does not directly contain any data. Rather, it dictates how the data is read from
 * and written to memory. This design allows a single Struct instance to be used to read and write
 * data from multiple memory locations instead of needing a seperate instance for each unique memory
 * location.
 *
 * <p>Struct fields are expected to be added sometime during initialization through {@link #addFields(StructField[])}
 * and at no other point. Adding or altering fields post-initialization can result in undefined behavior.</p>
 *
 * <p>The precise layout represented by a Struct is determined by assigning {@link FieldDescription} to each
 * member field depending on their type from a specific {@link StructLayout}. Structs (unless otherwise
 * stated by an implementation) are initialized without a StructLayout assigned. Attempting to access
 * statistics or fields of a struct without a layout results in undefined behavior.</p>
 *
 * @param <T> field type accepted by the struct
 */
public abstract class Struct <T extends StructField> implements RelativeBuffer, Savable {

    protected static final Logger logger = Logger.getLogger(Struct.class.getName());

    private final List<T> fields = new LinkedList<>();
    protected StructLayout layout;
    protected int size, alignment;
    private EngineBuffer parent;

    @Override
    public Destructor getDestructor() {
        return parent.getDestructor();
    }

    @Override
    public void update(CommandBuffer cmd) {
        if (parent != null) {
            parent.update(cmd);
        }
    }

    @Override
    public DataBuffer cache() {
        return parent.cache();
    }

    @Override
    public void bind(EngineBuffer parent) {
        this.parent = parent;
    }

    @Override
    public void invalidateCache() {
        parent.invalidateCache();
    }

    @Override
    public int capacity() {
        return size;
    }

    @Override
    public int getBufferLocalOffset() {
        return parent.getBufferLocalOffset();
    }

    @Override
    public long getHandle() {
        return parent.getHandle();
    }

    @Override
    public long getDeviceAddress() {
        return parent.getDeviceAddress();
    }

    @Override
    public Flag<Role> getRoles() {
        return parent.getRoles();
    }

    @Override
    public Flag<MemoryProp> getMemoryProperties() {
        return parent.getMemoryProperties();
    }

    @Override
    public boolean isDeviceAccessible() {
        return parent.isDeviceAccessible();
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        if (layout != null) {
            OutputCapsule out = ex.getCapsule(this);
            out.write(layout.getIdentifier(), "layoutId", null);
        }
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        String layoutId = in.readString("layoutId", null);
        if (layoutId != null) {
            StructLayout l = StructLayout.getLayoutByIdentifier(layoutId);
            if (l != null) {
                bind(l);
            } else {
                logger.log(Level.WARNING, "Layout \"{0}\" is unknown. No layout assigned to struct.", layoutId);
            }
        }
    }

    /**
     * Adds fields to this struct in order. This should only be performed during initialization.
     *
     * @param fields fields to add
     */
    @SuppressWarnings("unchecked")
    protected final void addFields(StructField... fields) {
        this.fields.addAll(Arrays.asList((T[])fields));
    }

    /**
     * Adds a field to this struct in order. This should only be performed during initialization.
     *
     * @param field field to add
     * @return added field
     * @param <F> field type
     */
    protected final <F extends T> F addField(F field) {
        fields.add(field);
        return field;
    }

    /**
     * Binds this struct with a layout defined by {@code layout}. If the layout
     * is changed, this struct is {@link #computeOffsets() recomputed}.
     *
     * @param layout layout
     */
    public void bind(StructLayout layout) {
        if (this.layout != layout) {
            this.layout = layout;
            computeOffsets();
        }
    }

    /**
     * Computes field offsets, struct size, and struct alignment based on
     * the currently bound layout.
     */
    public void computeOffsets() {
        this.size = 0;
        this.alignment = layout.getMinStructAlignment();
        for (T f : fields) {
            size = f.bind(this, size) + f.capacity();
            alignment = Math.max(alignment, f.getAlignment());
        }
        size = FastMath.toMultipleOf(size, alignment);
    }

    /**
     * Gets this struct's member fields in the order they were registered
     * as an unmodifiable list.
     *
     * @return unmodifiable list of fields
     */
    public List<T> getFields() {
        return Collections.unmodifiableList(fields);
    }

    /**
     * Gets the StructLayout this struct is currently bound to.
     *
     * @return bound layout
     */
    public StructLayout getLayout() {
        return layout;
    }

    /**
     * Gets the size in bytes of this struct as defined by the bound layout.
     *
     * @return size in bytes
     */
    public int getSize() {
        return size;
    }

    /**
     * Gets the alignment in bytes of this struct as defined by the bound layout.
     * Must be a power of two.
     *
     * @return alignment in bytes
     */
    public int getAlignment() {
        return alignment;
    }

    /**
     * Gets the aligned size in bytes of this struct as defined by the bound layout.
     * The aligned size is {@link #getSize()} rounded up to the nearest {@link #getAlignment()}.
     *
     * @return aligned size in bytes
     */
    public int getAlignedSize() {
        return FastMath.toMultipleOf(getSize(), getAlignment());
    }

    /**
     * Standard {@link StructField} implementation.
     *
     * @param <T> field type
     */
    public static class Field <T> implements StructField<T> {

        private final String name;
        private T alias;
        private Struct struct;
        private FieldDescription<T> description;
        private int offset;

        public Field(T alias) {
            this(null, alias);
        }

        public Field(String name, T alias) {
            assert alias != null : "Alias cannot be null.";
            this.name = name;
            this.alias = alias;
        }

        @Override
        public DataBuffer cache() {
            return struct.cache().offset(offset);
        }

        @Override
        public int bind(Struct struct, int offset) {
            this.struct = struct;
            this.description = struct.getLayout().getFieldDescription(alias.getClass());
            return this.offset = FastMath.toMultipleOf(offset, getAlignment());
        }

        @Override
        public Struct getBoundStruct() {
            return struct;
        }

        @Override
        public int capacity() {
            return description.getSize();
        }

        @Override
        public int getBufferLocalOffset() {
            return struct.getBufferLocalOffset() + offset;
        }

        @Override
        public void set(T value) {
            assert description != null : "Field not bound: unable to write.";
            description.write(cache(), value);
        }

        @Override
        public void alias(T value) {
            assert alias != null : "Alias cannot be null.";
            this.alias = value;
        }

        @Override
        public T get() {
            assert description != null : "Field not bound: unable to read.";
            return alias = description.read(cache(), alias);
        }

        @Override
        public T alias() {
            return alias;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getAlignment() {
            assert description != null : "Struct not bound to a layout: alignment unknown.";
            return description.getAlignment();
        }

        @Override
        public boolean isDeviceAccessible() {
            return struct.isDeviceAccessible();
        }

        public FieldDescription<T> getDescription() {
            return description;
        }

        @Override
        public int getStructLocalOffset() {
            return offset;
        }

    }

}
