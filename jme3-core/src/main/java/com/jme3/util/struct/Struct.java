package com.jme3.util.struct;

import com.jme3.export.*;
import com.jme3.math.FastMath;
import com.jme3.vulkan.buffers.BufferMapping;

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
 * <p>The precise layout represented by a Struct is determined by assigning {@link FieldDesc} to each
 * member field depending on their type from a specific {@link StructLayout}. Structs (unless otherwise
 * stated by an implementation) are initialized without a StructLayout assigned. Attempting to access
 * statistics or fields of a struct without a layout results in undefined behavior.</p>
 *
 * @param <T> field type accepted by the struct
 */
public abstract class Struct <T extends StructField> implements Savable {

    protected static final Logger logger = Logger.getLogger(Struct.class.getName());

    private final List<T> fields = new LinkedList<>();
    protected StructLayout layout;
    protected BufferMapping mapping;
    protected int position;
    private int size, alignment;

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

    @SafeVarargs
    protected final void addFields(T... fields) {
        this.fields.addAll(Arrays.asList(fields));
    }

    public void bind(StructLayout layout, BufferMapping mapping, int position) {
        bind(layout);
        this.mapping = mapping;
        this.position = position;
    }

    public void bind(BufferMapping mapping, int position) {
        if (layout == null) {
            bind(StructLayout.packed);
        }
        this.mapping = mapping;
        this.position = position;
    }

    public void bind(Struct struct) {
        bind(struct.getLayout(), struct.getMapping(), struct.getPosition());
    }

    /**
     * Binds this struct with a layout defined by {@code layout}. If the layout
     * is changed, this struct is {@link #computeOffsets() recomputed}.
     *
     * @param layout layout
     */
    public <E extends Struct> E bind(StructLayout layout) {
        if (this.layout == layout) {
            return (E)this;
        }
        this.layout = layout;
        computeOffsets();
        return (E)this;
    }

    /**
     * Reformats the currently bound buffer region according to the {@code layout},
     * if this struct is not already bound to {@code layout}.
     *
     * @param layout new layout
     */
    public void reformat(StructLayout layout) {
        if (this.layout != null && this.layout != layout) {
            for (T f : fields) {
                f.get(); // read to alias
            }
            bind(layout);
            for (T f : fields) {
                f.set(); // write from alias
            }
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
            size = f.bind(this, size) + f.getSize();
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

    public BufferMapping getMapping() {
        return mapping;
    }

    public int getPosition() {
        return position;
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

        private String name;
        private T alias;
        private Struct struct;
        private FieldDesc<T> description;
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
        public int bind(Struct struct, int offset) {
            this.struct = struct;
            this.description = struct.getLayout().getFieldDescription(alias.getClass());
            return this.offset = FastMath.toMultipleOf(offset, getAlignment());
        }

        @Override
        public void set(T value) {
            assert description != null : "Struct not bound: unable to write.";
            description.write(struct.getLayout(), struct.getMapping(), struct.getPosition() + offset, value);
        }

        @Override
        public T get() {
            assert description != null : "Struct not bound: unable to read.";
            return alias = description.read(struct.getLayout(), struct.getMapping(), struct.getPosition() + offset, alias);
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
        public int getSize() {
            assert description != null : "Struct not bound to a layout: size unknown.";
            return description.getSize(struct.getLayout(), alias);
        }

        @Override
        public int getAlignment() {
            assert description != null : "Struct not bound to a layout: alignment unknown.";
            return description.getAlignment(struct.getLayout(), alias);
        }

        public FieldDesc<T> getDescription() {
            return description;
        }

        public int getOffset() {
            return offset;
        }

    }

}
