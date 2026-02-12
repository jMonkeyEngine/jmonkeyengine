package com.jme3.vulkan.struct;

import com.fasterxml.jackson.databind.JsonNode;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import static org.lwjgl.system.MemoryUtil.*;

public abstract class StructMember <T> {

    private static final Map<String, Supplier<StructMember<?>>> members = new HashMap<>();

    public static StructMember<?> create(String name) {
        return Objects.requireNonNull(members.get(name.toLowerCase()), "Struct member \"" + name + "\" is not recognized.").get();
    }

    public static void addMemberFactory(Supplier<StructMember<?>> factory, String... names) {
        for (String n : names) {
            members.put(n.toLowerCase(), factory);
        }
    }

    protected final int size;
    protected int offset;

    public StructMember(int size) {
        this.size = size;
    }

    public ByteBuffer position(ByteBuffer buffer, int bufferOffset) {
        buffer.position(offset - bufferOffset);
        return buffer;
    }

    public void set(long structAddress, T value) {
        setAtPointer(structAddress + offset, value);
    }

    public T get(long structAddress) {
        return getAtPointer(structAddress + offset);
    }

    public void parse(long structAddress, JsonNode json) {
        parseIntoPointer(structAddress + offset, json);
    }

    protected abstract void setAtPointer(long ptr, T value);

    protected abstract T getAtPointer(long ptr);

    protected abstract void parseIntoPointer(long ptr, JsonNode json);

    public int getSize() {
        return size;
    }

    public int getOffset() {
        return offset;
    }

    public static final StructMember<Byte> Bytes = new StructMember<Byte>(Byte.BYTES) {
        @Override
        public void setAtPointer(long ptr, Byte value) {
            memPutByte(ptr, value);
        }
        @Override
        public Byte getAtPointer(long ptr) {
            return memGetByte(ptr);
        }
        @Override
        protected void parseIntoPointer(long ptr, JsonNode json) {
            setAtPointer(ptr, (byte)json.intValue());
        }
    };

    public static final StructMember<Short> Shorts = new StructMember<Short>(Short.BYTES) {
        @Override
        public void setAtPointer(long ptr, Short value) {
            memPutShort(ptr, value);
        }
        @Override
        public Short getAtPointer(long ptr) {
            return memGetShort(ptr);
        }
        @Override
        protected void parseIntoPointer(long ptr, JsonNode json) {
            setAtPointer(ptr, json.shortValue());
        }
    };

    public static final StructMember<Integer> Ints = new StructMember<Integer>(Integer.BYTES) {
        @Override
        public void setAtPointer(long ptr, Integer value) {
            memPutInt(ptr, value);
        }
        @Override
        public Integer getAtPointer(long ptr) {
            return memGetInt(ptr);
        }
        @Override
        protected void parseIntoPointer(long ptr, JsonNode json) {
            setAtPointer(ptr, json.asInt());
        }
    };

    public static final StructMember<Float> Floats = new StructMember<Float>(Float.BYTES) {
        @Override
        public void setAtPointer(long ptr, Float value) {
            memPutFloat(ptr, value);
        }
        @Override
        public Float getAtPointer(long ptr) {
            return memGetFloat(ptr);
        }
        @Override
        protected void parseIntoPointer(long ptr, JsonNode json) {
            setAtPointer(ptr, json.floatValue());
        }
    };

    public static final StructMember<Double> Doubles = new StructMember<Double>(Double.BYTES) {
        @Override
        public void setAtPointer(long ptr, Double value) {
            memPutDouble(ptr, value);
        }
        @Override
        public Double getAtPointer(long ptr) {
            return memGetDouble(ptr);
        }
        @Override
        protected void parseIntoPointer(long ptr, JsonNode json) {
            setAtPointer(ptr, json.doubleValue());
        }
    };

    public static final StructMember<Long> Longs = new StructMember<Long>(Long.BYTES) {
        @Override
        public void setAtPointer(long ptr, Long value) {
            memPutLong(ptr, value);
        }
        @Override
        public Long getAtPointer(long ptr) {
            return memGetLong(ptr);
        }
        @Override
        protected void parseIntoPointer(long ptr, JsonNode json) {
            setAtPointer(ptr, json.longValue());
        }
    };

    public static final StructMember<Vector2f> Vec2f = new StructMember<Vector2f>(Float.BYTES * 2) {
        @Override
        public void setAtPointer(long ptr, Vector2f value) {
            memPutFloat(ptr, value.x);
            memPutFloat(ptr + Float.BYTES, value.y);
        }
        @Override
        public Vector2f getAtPointer(long ptr) {
            return new Vector2f(memGetFloat(ptr), memGetFloat(ptr + Float.BYTES));
        }
        @Override
        protected void parseIntoPointer(long ptr, JsonNode json) {
            memPutFloat(ptr, json.get(0).floatValue());
            memPutFloat(ptr + Float.BYTES, json.get(1).floatValue());
        }
    };

    public static final StructMember<Vector3f> Vec3f = new StructMember<Vector3f>(Float.BYTES * 4) {
        @Override
        public void setAtPointer(long ptr, Vector3f value) {
            memPutFloat(ptr, value.x);
            memPutFloat(ptr += Float.BYTES, value.y);
            memPutFloat(ptr + Float.BYTES, value.z);
        }
        @Override
        public Vector3f getAtPointer(long ptr) {
            return new Vector3f(memGetFloat(ptr),
                    memGetFloat(ptr += Float.BYTES),
                    memGetFloat(ptr + Float.BYTES));
        }
        @Override
        protected void parseIntoPointer(long ptr, JsonNode json) {
            memPutFloat(ptr, json.get(0).floatValue());
            memPutFloat(ptr += Float.BYTES, json.get(1).floatValue());
            memPutFloat(ptr + Float.BYTES, json.get(2).floatValue());
        }
    };

    public static final StructMember<Vector4f> Vec4f = new StructMember<Vector4f>(Float.BYTES * 4) {
        @Override
        public void setAtPointer(long ptr, Vector4f value) {
            memPutFloat(ptr, value.x);
            memPutFloat(ptr += Float.BYTES, value.y);
            memPutFloat(ptr += Float.BYTES, value.z);
            memPutFloat(ptr + Float.BYTES, value.w);
        }
        @Override
        public Vector4f getAtPointer(long ptr) {
            return new Vector4f(memGetFloat(ptr),
                    memGetFloat(ptr += Float.BYTES),
                    memGetFloat(ptr += Float.BYTES),
                    memGetFloat(ptr + Float.BYTES));
        }
        @Override
        protected void parseIntoPointer(long ptr, JsonNode json) {
            memPutFloat(ptr, json.get(0).floatValue());
            memPutFloat(ptr += Float.BYTES, json.get(1).floatValue());
            memPutFloat(ptr += Float.BYTES, json.get(2).floatValue());
            memPutFloat(ptr + Float.BYTES, json.get(3).floatValue());
        }
    };

    static {
        addMemberFactory(() -> Bytes, "byte");
        addMemberFactory(() -> Shorts, "short");
        addMemberFactory(() -> Ints, "int", "integer");
        addMemberFactory(() -> Floats, "float");
        addMemberFactory(() -> Doubles, "double");
        addMemberFactory(() -> Longs, "long");
        addMemberFactory(() -> Vec2f, "vec2", "vec2f", "vector2", "vector2f");
        addMemberFactory(() -> Vec3f, "vec3", "vec3f", "vector3", "vector3f");
        addMemberFactory(() -> Vec4f, "vec4", "vec4f", "vector4", "vector4f");
    }

}
