package com.jme3.vulkan.buffer;

import com.jme3.math.*;
import com.jme3.vulkan.buffer.tracking.BufferTracker;
import com.jme3.vulkan.buffer.tracking.NullBufferTracker;
import com.jme3.vulkan.buffer.tracking.SliceBufferTracker;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class DataBuffer implements EngineBuffer {

    private static final Flag<Role> ROLES = Flag.empty();

    private static final int VEC2 = Float.BYTES * 2;
    private static final int VEC3 = Float.BYTES * 3;
    private static final int VEC4 = Float.BYTES * 4;

    private final ByteBuffer buffer;
    private final BufferTracker tracker;
    private int markedPos = -1;
    private int markedLim = -1;

    public DataBuffer(ByteBuffer buffer) {
        this(buffer, NullBufferTracker.INSTANCE);
    }

    public DataBuffer(ByteBuffer buffer, BufferTracker tracker) {
        this.buffer = buffer;
        this.tracker = tracker;
    }

    @Override
    public DataBuffer cache() {
        return clear();
    }

    @Override
    public void flushCache() {}

    @Override
    public void invalidateCache() {}

    @Override
    public int getBufferLocalOffset() {
        return 0;
    }

    @Override
    public long getHandle() {
        return MemoryUtil.NULL;
    }

    @Override
    public long getDeviceAddress() {
        return MemoryUtil.NULL;
    }

    @Override
    public Flag<Role> getRoles() {
        return ROLES;
    }

    @Override
    public int capacity() {
        return buffer.capacity();
    }

    @Override
    public void update(CommandBuffer cmd) {}

    @Override
    public boolean isHostAccessible() {
        return true;
    }

    @Override
    public boolean isDeviceAccessible() {
        return false;
    }

    @Override
    public Flag<MemoryProp> getMemoryProperties() {
        return null;
    }

    public long getAddress() {
        return MemoryUtil.memAddress(buffer);
    }

    public ByteBuffer getBytes() {
        return buffer;
    }

    public BufferTracker getTracker() {
        return tracker;
    }

    public DataBuffer copyTo(int srcOffset, DataBuffer dst, int dstOffset, int size) {
        int spos = buffer.position();
        int slimit = buffer.limit();
        int dpos = dst.buffer.position();
        int dlimit = dst.buffer.limit();
        MemoryUtil.memCopy(buffer.position(srcOffset).limit(srcOffset + size),
                dst.buffer.position(dstOffset).limit(dstOffset + size));
        buffer.position(spos).limit(slimit);
        dst.buffer.position(dpos).limit(dlimit);
        dst.tracker.add(dstOffset, size);
        return this;
    }

    public DataBuffer copyTo(DataBuffer dst) {
        MemoryUtil.memCopy(buffer, dst.getBytes());
        dst.tracker.add(dst.position(), Math.min(remaining(), dst.remaining()));
        return this;
    }

    public DataBuffer stage(int offset, int size) {
        tracker.add(offset, size);
        return this;
    }

    public DataBuffer stage() {
        tracker.add(buffer.position(), buffer.remaining());
        return this;
    }

    public DataBuffer clearStaging() {
        tracker.clear();
        return this;
    }

    public DataBuffer position(int p) {
        buffer.position(p);
        return this;
    }

    public DataBuffer limit(int p) {
        buffer.limit(p);
        return this;
    }

    public DataBuffer range(int p, int l) {
        buffer.position(p).limit(l);
        return this;
    }

    public DataBuffer region(int p, int size) {
        buffer.position(p).limit(p + size);
        return this;
    }

    public DataBuffer offset(int offset, int size) {
        buffer.position(buffer.position() + offset).limit(buffer.position() + size);
        return this;
    }

    public DataBuffer offset(int offset) {
        buffer.position(buffer.position() + offset);
        return this;
    }

    public DataBuffer size(int size) {
        buffer.limit(buffer.position() + size);
        return this;
    }

    public DataBuffer flip() {
        buffer.flip();
        return this;
    }

    public DataBuffer clear() {
        buffer.clear();
        markedPos = markedLim = -1;
        return this;
    }

    public DataBuffer mark() {
        markedPos = buffer.position();
        markedLim = buffer.limit();
        return this;
    }

    public DataBuffer markPosition() {
        markedPos = buffer.position();
        return this;
    }

    public DataBuffer markLimit() {
        markedLim = buffer.position();
        return this;
    }

    public DataBuffer reset() {
        buffer.position(markedPos).limit(markedLim);
        return this;
    }

    public DataBuffer resetPosition() {
        buffer.position(markedPos);
        return this;
    }

    public DataBuffer resetLimit() {
        buffer.limit(markedLim);
    }

    public DataBuffer slice(int p, int l) {
        int pos = buffer.position();
        int lim = buffer.limit();
        ByteBuffer buf = buffer.position(p).limit(l).slice();
        buffer.position(pos).limit(lim);
        return new DataBuffer(buf, new SliceBufferTracker(tracker, p, l - p));
    }

    public DataBuffer slice() {
        return new DataBuffer(buffer.slice(), new SliceBufferTracker(tracker, buffer.position(), buffer.remaining()));
    }

    public DataBuffer duplicate() {
        return new DataBuffer(buffer.duplicate(), tracker);
    }

    public int position() {
        return buffer.position();
    }

    public int limit() {
        return buffer.limit();
    }

    public int remaining() {
        return buffer.remaining();
    }

    public DataBuffer put(int p, byte v) {
        buffer.put(p, v);
        tracker.add(p, Byte.BYTES);
        return this;
    }

    public DataBuffer put(int p, short v) {
        buffer.putShort(p, v);
        tracker.add(p, Short.BYTES);
        return this;
    }

    public DataBuffer put(int p, int v) {
        buffer.putInt(p, v);
        tracker.add(p, Integer.BYTES);
        return this;
    }

    public DataBuffer put(int p, float v) {
        buffer.putFloat(p, v);
        tracker.add(p, Float.BYTES);
        return this;
    }

    public DataBuffer put(int p, double v) {
        buffer.putDouble(p, v);
        tracker.add(p, Double.BYTES);
        return this;
    }

    public DataBuffer put(int p, long v) {
        buffer.putLong(p, v);
        tracker.add(p, Long.BYTES);
        return this;
    }

    public DataBuffer put(byte v) {
        tracker.add(buffer.position(), Byte.BYTES);
        buffer.put(v);
        return this;
    }

    public DataBuffer put(short v) {
        tracker.add(buffer.position(), Short.BYTES);
        buffer.putShort(v);
        return this;
    }

    public DataBuffer put(int v) {
        tracker.add(buffer.position(), Integer.BYTES);
        buffer.putInt(v);
        return this;
    }

    public DataBuffer put(float v) {
        tracker.add(buffer.position(), Float.BYTES);
        buffer.putFloat(v);
        return this;
    }

    public DataBuffer put(double v) {
        tracker.add(buffer.position(), Double.BYTES);
        buffer.putDouble(v);
        return this;
    }

    public DataBuffer put(long v) {
        tracker.add(buffer.position(), Long.BYTES);
        buffer.putLong(v);
        return this;
    }

    public DataBuffer put(int p, byte[] v) {
        int pos = buffer.position();
        buffer.position(p).put(v).position(pos);
        tracker.add(p, v.length);
        return this;
    }

    public DataBuffer put(int p, short[] v) {
        for (int i = 0; i < v.length; i++) {
            buffer.putShort(p + (i << 1), v[i]);
        }
        tracker.add(p, v.length << 1);
        return this;
    }

    public DataBuffer put(int p, int[] v) {
        for (int i = 0; i < v.length; i++) {
            buffer.putInt(p + (i << 2), v[i]);
        }
        tracker.add(p, v.length << 2);
        return this;
    }

    public DataBuffer put(int p, float[] v) {
        for (int i = 0; i < v.length; i++) {
            buffer.putFloat(p + (i << 2), v[i]);
        }
        tracker.add(p, v.length << 2);
        return this;
    }

    public DataBuffer put(int p, double[] v) {
        for (int i = 0; i < v.length; i++) {
            buffer.putDouble(p + (i << 3), v[i]);
        }
        tracker.add(p, v.length << 3);
        return this;
    }

    public DataBuffer put(int p, long[] v) {
        for (int i = 0; i < v.length; i++) {
            buffer.putLong(p + (i << 3), v[i]);
        }
        tracker.add(p, v.length << 3);
        return this;
    }

    public DataBuffer put(byte[] v) {
        tracker.add(buffer.position(), v.length);
        buffer.put(v);
        return this;
    }

    public DataBuffer put(short[] v) {
        tracker.add(buffer.position(), v.length << 1);
        for (short a : v) {
            buffer.putShort(a);
        }
        return this;
    }

    public DataBuffer put(int[] v) {
        tracker.add(buffer.position(), v.length << 2);
        for (int a : v) {
            buffer.putInt(a);
        }
        return this;
    }

    public DataBuffer put(float[] v) {
        tracker.add(buffer.position(), v.length << 2);
        for (float a : v) {
            buffer.putFloat(a);
        }
        return this;
    }

    public DataBuffer put(double[] v) {
        tracker.add(buffer.position(), v.length << 3);
        for (double a : v) {
            buffer.putDouble(a);
        }
        return this;
    }

    public DataBuffer put(long[] v) {
        tracker.add(buffer.position(), v.length << 3);
        for (long a : v) {
            buffer.putLong(a);
        }
        return this;
    }

    public DataBuffer put(int p, Vector2f v) {
        buffer.putFloat(p, v.x).putFloat(p + Float.BYTES, v.y);
        tracker.add(p, VEC2);
        return this;
    }

    public DataBuffer put(int p, Vector3f v) {
        buffer.putFloat(p, v.x).putFloat(p + Float.BYTES, v.y).putFloat(p + VEC2, v.z);
        tracker.add(p, VEC3);
        return this;
    }

    public DataBuffer put(int p, Vector4f v) {
        buffer.putFloat(p, v.x).putFloat(p + Float.BYTES, v.y).putFloat(p + VEC2, v.z).putFloat(p + VEC3, v.w);
        tracker.add(p, VEC4);
        return this;
    }

    public DataBuffer put(int p, ColorRGBA v) {
        buffer.putFloat(p, v.r).putFloat(p + Float.BYTES, v.g).putFloat(p + VEC2, v.b).putFloat(p + VEC3, v.a);
        tracker.add(p, VEC4);
        return this;
    }

    public DataBuffer put(int p, Matrix3f v) {
        int pos = buffer.position();
        v.writeToPackedBuffer(buffer.position(p));
        buffer.position(pos);
        tracker.add(p, VEC3 * 3);
        return this;
    }

    public DataBuffer putStd(int p, Matrix3f v) {
        int pos = buffer.position();
        v.writeToStdBuffer(buffer.position(p));
        buffer.position(pos);
        tracker.add(p, VEC4 * 3);
        return this;
    }

    public DataBuffer put(int p, Matrix4f v) {
        int pos = buffer.position();
        v.writeToBuffer(buffer.position(p));
        buffer.position(pos);
        tracker.add(p, VEC4 << 2);
        return this;
    }

    public DataBuffer put(Matrix3f v) {
        tracker.add(buffer.position(), VEC3 * 3);
        v.writeToPackedBuffer(buffer);
        return this;
    }

    public DataBuffer putStd(Matrix3f v) {
        tracker.add(buffer.position(), VEC4 * 3);
        v.writeToStdBuffer(buffer);
        return this;
    }

    public DataBuffer put(Matrix4f v) {
        tracker.add(buffer.position(), VEC4 << 2);
        v.writeToBuffer(buffer);
        return this;
    }

    public DataBuffer put(Vector2f v) {
        tracker.add(buffer.position(), VEC2);
        buffer.putFloat(v.x).putFloat(v.y);
        return this;
    }

    public DataBuffer put(Vector3f v) {
        tracker.add(buffer.position(), VEC3);
        buffer.putFloat(v.x).putFloat(v.y).putFloat(v.z);
        return this;
    }

    public DataBuffer put(Vector4f v) {
        tracker.add(buffer.position(), VEC4);
        buffer.putFloat(v.x).putFloat(v.y).putFloat(v.z).putFloat(v.w);
        return this;
    }

    public DataBuffer put(ColorRGBA v) {
        tracker.add(buffer.position(), VEC4);
        buffer.putFloat(v.r).putFloat(v.g).putFloat(v.b).putFloat(v.a);
        return this;
    }

    public byte getByte(int p) {
        return buffer.get(p);
    }

    public short getShort(int p) {
        return buffer.getShort(p);
    }

    public int getInt(int p) {
        return buffer.getInt(p);
    }

    public float getFloat(int p) {
        return buffer.getFloat(p);
    }

    public double getDouble(int p) {
        return buffer.getDouble(p);
    }

    public long getLong(int p) {
        return buffer.getLong(p);
    }

    public byte getByte() {
        return buffer.get();
    }

    public short getShort() {
        return buffer.getShort();
    }

    public int getInt() {
        return buffer.getInt();
    }

    public float getFloat() {
        return buffer.getFloat();
    }

    public double getDouble() {
        return buffer.getDouble();
    }

    public long getLong() {
        return buffer.getLong();
    }

    public byte[] get(int p, byte[] v) {
        int pos = buffer.position();
        buffer.position(p).get(v).position(pos);
        return v;
    }

    public short[] get(int p, short[] v) {
        for (int i = 0; i < v.length; i++) {
            v[i] = buffer.getShort(p + (i << 1));
        }
        return v;
    }

    public int[] get(int p, int[] v) {
        for (int i = 0; i < v.length; i++) {
            v[i] = buffer.getInt(p + (i << 2));
        }
        return v;
    }

    public float[] get(int p, float[] v) {
        for (int i = 0; i < v.length; i++) {
            v[i] = buffer.getFloat(p + (i << 2));
        }
        return v;
    }

    public double[] get(int p, double[] v) {
        for (int i = 0; i < v.length; i++) {
            v[i] = buffer.getDouble(p + (i << 3));
        }
        return v;
    }

    public long[] get(int p, long[] v) {
        for (int i = 0; i < v.length; i++) {
            v[i] = buffer.getLong(p + (i << 3));
        }
        return v;
    }

    public byte[] get(byte[] v) {
        buffer.get(v);
        return v;
    }

    public short[] get(short[] v) {
        for (int i = 0; i < v.length; i++) {
            v[i] = buffer.getShort();
        }
        return v;
    }

    public int[] get(int[] v) {
        for (int i = 0; i < v.length; i++) {
            v[i] = buffer.getInt();
        }
        return v;
    }

    public float[] get(float[] v) {
        for (int i = 0; i < v.length; i++) {
            v[i] = buffer.getFloat();
        }
        return v;
    }

    public double[] get(double[] v) {
        for (int i = 0; i < v.length; i++) {
            v[i] = buffer.getDouble();
        }
        return v;
    }

    public long[] get(long[] v) {
        for (int i = 0; i < v.length; i++) {
            v[i] = buffer.getLong();
        }
        return v;
    }

    public Vector2f get(int p, Vector2f v) {
        v.x = buffer.getFloat(p);
        v.y = buffer.getFloat(p + Float.BYTES);
        return v;
    }

    public Vector3f get(int p, Vector3f v) {
        v.x = buffer.getFloat(p);
        v.y = buffer.getFloat(p + Float.BYTES);
        v.z = buffer.getFloat(p + VEC2);
        return v;
    }

    public Vector4f get(int p, Vector4f v) {
        v.x = buffer.getFloat(p);
        v.y = buffer.getFloat(p + Float.BYTES);
        v.z = buffer.getFloat(p + VEC2);
        v.w = buffer.getFloat(p + VEC3);
        return v;
    }

    public ColorRGBA get(int p, ColorRGBA v) {
        v.r = buffer.getFloat(p);
        v.g = buffer.getFloat(p + Float.BYTES);
        v.b = buffer.getFloat(p + VEC2);
        v.a = buffer.getFloat(p + VEC3);
        return v;
    }

    public Vector2f get(Vector2f v) {
        v.x = buffer.getFloat();
        v.y = buffer.getFloat();
        return v;
    }

    public Vector3f get(Vector3f v) {
        v.x = buffer.getFloat();
        v.y = buffer.getFloat();
        v.z = buffer.getFloat();
        return v;
    }

    public Vector4f get(Vector4f v) {
        v.x = buffer.getFloat();
        v.y = buffer.getFloat();
        v.z = buffer.getFloat();
        v.w = buffer.getFloat();
        return v;
    }

    public ColorRGBA get(ColorRGBA v) {
        v.r = buffer.getFloat();
        v.g = buffer.getFloat();
        v.b = buffer.getFloat();
        v.a = buffer.getFloat();
        return v;
    }

    public Matrix3f get(int p, Matrix3f v) {
        int pos = buffer.position();
        v.readFromPackedBuffer(buffer.position(p));
        buffer.position(pos);
        return v;
    }

    public Matrix3f getStd(int p, Matrix3f v) {
        int pos = buffer.position();
        v.readFromStdBuffer(buffer.position(p));
        buffer.position(pos);
        return v;
    }

    public Matrix4f get(int p, Matrix4f v) {
        int pos = buffer.position();
        v.readFromBuffer(buffer.position(p));
        buffer.position(pos);
        return v;
    }

    public Matrix3f get(Matrix3f v) {
        v.readFromPackedBuffer(buffer);
        return v;
    }

    public Matrix3f getStd(Matrix3f v) {
        v.readFromStdBuffer(buffer);
        return v;
    }

    public Matrix4f get(Matrix4f v) {
        v.readFromBuffer(buffer);
        return v;
    }

}
