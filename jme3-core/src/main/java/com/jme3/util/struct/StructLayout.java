package com.jme3.util.struct;

import com.jme3.math.*;
import com.jme3.vulkan.buffer.DataBuffer;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StructLayout {
    
    private static final Logger logger = Logger.getLogger(StructLayout.class.getName());
    
    private static final int VEC2_WIDTH = Float.BYTES * 2;
    private static final int VEC3_WIDTH = Float.BYTES * 3;
    private static final int VEC4_WIDTH = Float.BYTES * 4;

    public static final StructLayout std140 = new StructLayout("std140", VEC4_WIDTH, VEC4_WIDTH);
    public static final StructLayout std430 = new StructLayout("std430", VEC4_WIDTH, Float.BYTES);
    public static final StructLayout optimal = new StructLayout("optimal", Byte.BYTES, Byte.BYTES);
    private static final Map<String, StructLayout> layouts = new HashMap<>();

    private static void addToAllLayouts(FieldDescription desc, Class... types) {
        std140.addFieldDescription(desc, types);
        std430.addFieldDescription(desc, types);
        optimal.addFieldDescription(desc, types);
    }

    private static void addToLayouts(FieldDescription desc, StructLayout[] layouts, Class... types) {
        for (StructLayout l : layouts) {
            l.addFieldDescription(desc, types);
        }
    }
    
    public static StructLayout getLayoutByIdentifier(String identifier) {
        return layouts.get(identifier);
    }

    static {
        
        final StructLayout[] stds = {std140, std430};

        // general descriptions
        addToAllLayouts(new ObjectDesc<Boolean>(Float.BYTES, Float.BYTES) {
            @Override
            public void write(DataBuffer buffer, Boolean value) {
                buffer.put(value ? Integer.MAX_VALUE : 0);
            }
            @Override
            public Boolean read(DataBuffer buffer, Boolean store) {
                return buffer.getByte() != 0;
            }
        }, boolean.class, Boolean.class);
        addToAllLayouts(new ObjectDesc<Integer>(Float.BYTES, Float.BYTES) {
            @Override
            public void write(DataBuffer buffer, Integer value) {
                buffer.put(value);
            }
            @Override
            public Integer read(DataBuffer buffer, Integer store) {
                return buffer.getInt();
            }
        }, int.class, Integer.class);
        addToAllLayouts(new ObjectDesc<Float>(Float.BYTES, Float.BYTES) {
            @Override
            public void write(DataBuffer buffer, Float value) {
                buffer.put(value);
            }
            @Override
            public Float read(DataBuffer buffer, Float store) {
                return buffer.getFloat();
            }
        }, float.class, Float.class);
        addToLayouts(new ObjectDesc<Long>(VEC2_WIDTH, VEC2_WIDTH) {
            @Override
            public void write(DataBuffer buffer, Long value) {
                buffer.put(value);
            }
            @Override
            public Long read(DataBuffer buffer, Long store) {
                return buffer.getLong();
            }
        }, stds, long.class, Long.class);
        addToLayouts(new ObjectDesc<Vector2f>(VEC2_WIDTH, VEC2_WIDTH) {
            @Override
            public void write(DataBuffer buffer, Vector2f value) {
                buffer.put(value);
            }
            @Override
            public Vector2f read(DataBuffer buffer, Vector2f store) {
                return buffer.get(store);
            }
        }, stds, Vector2f.class);
        addToLayouts(new ObjectDesc<Vector3f>(Float.BYTES * 3, VEC4_WIDTH) {
            @Override
            public void write(DataBuffer buffer, Vector3f value) {
                buffer.put(value);
            }
            @Override
            public Vector3f read(DataBuffer buffer, Vector3f store) {
                return buffer.get(store);
            }
        }, stds, Vector3f.class);
        addToLayouts(new ObjectDesc<Vector4f>(VEC4_WIDTH, VEC4_WIDTH) {
            @Override
            public void write(DataBuffer buffer, Vector4f value) {
                buffer.put(value);
            }
            @Override
            public Vector4f read(DataBuffer buffer, Vector4f store) {
                return buffer.get(store);
            }
        }, stds, Vector4f.class);
        addToLayouts(new ObjectDesc<ColorRGBA>(VEC4_WIDTH, VEC4_WIDTH) {
            @Override
            public void write(DataBuffer buffer, ColorRGBA value) {
                buffer.put(value);
            }
            @Override
            public ColorRGBA read(DataBuffer buffer, ColorRGBA store) {
                return buffer.get(store);
            }
        }, stds, ColorRGBA.class);
        addToLayouts(new ObjectDesc<Matrix3f>(Float.BYTES * 12, VEC4_WIDTH) {
            @Override
            public void write(DataBuffer buffer, Matrix3f value) {
                buffer.putStd(value);
            }
            @Override
            public Matrix3f read(DataBuffer buffer, Matrix3f store) {
                return buffer.getStd(store);
            }
        }, stds, Matrix3f.class);
        addToLayouts(new ObjectDesc<Matrix4f>(Float.BYTES << 4, VEC4_WIDTH) {
            @Override
            public void write(DataBuffer buffer, Matrix4f value) {
                buffer.put(value);
            }
            @Override
            public Matrix4f read(DataBuffer buffer, Matrix4f store) {
                return buffer.get(store);
            }
        }, stds, Matrix4f.class);

        // packed descriptions
        optimal.addFieldDescription(new ObjectDesc<Long>(VEC2_WIDTH, Float.BYTES) {
            @Override
            public void write(DataBuffer buffer, Long value) {
                buffer.put(value);
            }
            @Override
            public Long read(DataBuffer buffer, Long store) {
                return buffer.getLong();
            }
        }, long.class, Long.class);
        optimal.addFieldDescription(new ObjectDesc<Vector2f>(VEC2_WIDTH, Float.BYTES) {
            @Override
            public void write(DataBuffer buffer, Vector2f value) {
                buffer.put(value);
            }
            @Override
            public Vector2f read(DataBuffer buffer, Vector2f store) {
                return buffer.get(store);
            }
        }, Vector2f.class);
        optimal.addFieldDescription(new ObjectDesc<Vector3f>(VEC3_WIDTH, Float.BYTES) {
            @Override
            public void write(DataBuffer buffer, Vector3f value) {
                buffer.put(value);
            }
            @Override
            public Vector3f read(DataBuffer buffer, Vector3f store) {
                return buffer.get(store);
            }
        }, Vector3f.class);
        optimal.addFieldDescription(new ObjectDesc<Vector4f>(VEC4_WIDTH, Float.BYTES) {
            @Override
            public void write(DataBuffer buffer, Vector4f value) {
                buffer.put(value);
            }
            @Override
            public Vector4f read(DataBuffer buffer, Vector4f store) {
                return buffer.get(store);
            }
        }, Vector4f.class);
        optimal.addFieldDescription(new ObjectDesc<ColorRGBA>(VEC4_WIDTH, Float.BYTES) {
            @Override
            public void write(DataBuffer buffer, ColorRGBA value) {
                buffer.put(value);
            }
            @Override
            public ColorRGBA read(DataBuffer buffer, ColorRGBA store) {
                return buffer.get(store);
            }
        }, ColorRGBA.class);
        optimal.addFieldDescription(new ObjectDesc<Matrix3f>(VEC3_WIDTH * 3, Float.BYTES) {
            @Override
            public void write(DataBuffer buffer, Matrix3f value) {
                buffer.putStd(value);
            }
            @Override
            public Matrix3f read(DataBuffer buffer, Matrix3f store) {
                return buffer.getStd(store);
            }
        }, Matrix3f.class);
        optimal.addFieldDescription(new ObjectDesc<Matrix4f>(Float.BYTES << 4, Float.BYTES) {
            @Override
            public void write(DataBuffer buffer, Matrix4f value) {
                buffer.put(value);
            }
            @Override
            public Matrix4f read(DataBuffer buffer, Matrix4f store) {
                return buffer.get(store);
            }
        }, Matrix4f.class);

    }
    
    private final String identifier;
    private final int minStructAlignment, minArrayAlignment;
    private final Map<Class, FieldDescription> fields = new HashMap<>();
    private final Map<Class, Class> typeRemappings = new HashMap<>();

    public StructLayout(String identifier, int minStructAlignment, int minArrayAlignment) {
        this.identifier = identifier;
        this.minStructAlignment = minStructAlignment;
        this.minArrayAlignment = minArrayAlignment;
        if (layouts.put(identifier, this) != null) {
            logger.log(Level.WARNING, "Overwriting layout \"{0}\" can result in incoherent data.", identifier);
        }
    }
    
    public String getIdentifier() {
        return identifier;
    }

    public void addFieldDescription(FieldDescription desc, Class... types) {
        for (Class t : types) {
            if (fields.putIfAbsent(t, desc) != null) {
                throw new IllegalArgumentException(t + " is already described.");
            }
        }
    }

    public void remapType(Class src, Class dst) {
        typeRemappings.put(src, dst);
    }

    public int getMinStructAlignment() {
        return minStructAlignment;
    }

    public int getMinArrayAlignment() {
        return minArrayAlignment;
    }

    public <T> FieldDescription<T> getFieldDescription(Class type) {
        return getFieldDescription(type, type);
    }

    protected FieldDescription getFieldDescription(Class origin, Class type) {
        if (type == null) {
            throw new NullPointerException(origin + " is not described.");
        }
        type = typeRemappings.getOrDefault(type, type);
        FieldDescription d = fields.get(type);
        if (d != null) return d;
        else return getFieldDescription(type.getSuperclass());
    }

    public static abstract class ObjectDesc <T> implements FieldDescription<T> {

        private final int size, alignment;

        public ObjectDesc(int size, int alignment) {
            this.size = size;
            this.alignment = alignment;
        }

        @Override
        public int getSize() {
            return size;
        }

        @Override
        public int getAlignment() {
            return alignment;
        }

    }

}
