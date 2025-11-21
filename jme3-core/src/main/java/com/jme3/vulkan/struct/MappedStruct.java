package com.jme3.vulkan.struct;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.NativeResource;
import org.lwjgl.system.Struct;
import org.lwjgl.system.StructBuffer;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public abstract class MappedStruct <SELF extends MappedStruct<SELF>> extends Struct<SELF> implements NativeResource {

    protected MappedStruct(long address, ByteBuffer container) {
        super(address, container);
    }

    protected abstract MappedLayout getLayout();

    @Override
    public int sizeof() {
        return getLayout().sizeof();
    }

    public void set(String name, Object value) {
        getLayout().getMember(name).set(address, value);
    }

    public Object get(String name) {
        return getLayout().getMember(name).get(address);
    }

    protected static class MappedLayout {

        private final Map<String, MappedMember> members = new HashMap<>();
        private final List<MappedMember> memberList = new LinkedList<>();
        private int sizeof, alignof;

        protected void add(String name, MappedMember member) {
            members.put(name, member);
            memberList.add(member);
        }

        public MappedMember getMember(String name) {
            return members.get(name);
        }

        protected Layout build() {
            Struct.Member[] mems = memberList.stream().map(m -> __member(m.size)).toArray(Struct.Member[]::new);
            Layout layout = __struct(mems);
            sizeof = layout.getSize();
            alignof = layout.getAlignment();
            int i = 0;
            for (MappedMember m : memberList) {
                m.offset = layout.offsetof(i++);
            }
            memberList.clear();
            return layout;
        }

        public int sizeof() {
            return sizeof;
        }

        public int alignof() {
            return alignof;
        }

    }

    protected static abstract class MappedMember <T> {

        protected final int size;
        protected int offset;

        public MappedMember(int size) {
            this.size = size;
        }

        protected abstract void set(long struct, Object value);

        protected abstract T get(long struct);

        public int getSize() {
            return size;
        }

        public int getOffset() {
            return offset;
        }

    }

    protected static abstract class MappedBuffer <STRUCT extends MappedStruct<STRUCT>, SELF extends MappedBuffer<STRUCT, SELF>>
            extends StructBuffer<STRUCT, SELF> implements NativeResource {

        protected MappedBuffer(ByteBuffer container, int remaining) {
            super(container, remaining);
        }

        public MappedBuffer(long address, ByteBuffer container, int mark, int position, int limit, int capacity) {
            super(address, container, mark, position, limit, capacity);
        }

    }

    protected static class ByteMember extends MappedMember<Byte> {

        public ByteMember() {
            super(Byte.BYTES);
        }

        @Override
        protected void set(long struct, Object value) {
            MemoryUtil.memPutByte(struct + offset, (byte)value);
        }

        @Override
        protected Byte get(long struct) {
            return MemoryUtil.memGetByte(struct + offset);
        }

    }

    protected static class ShortMember extends MappedMember<Short> {

        public ShortMember() {
            super(Short.BYTES);
        }

        @Override
        protected void set(long struct, Object value) {
            MemoryUtil.memPutShort(struct + offset, (short)value);
        }

        @Override
        protected Short get(long struct) {
            return MemoryUtil.memGetShort(struct + offset);
        }

    }

    protected static class IntMember extends MappedMember<Integer> {

        public IntMember() {
            super(Integer.BYTES);
        }

        @Override
        protected void set(long struct, Object value) {
            MemoryUtil.memPutInt(struct + offset, (int)value);
        }

        @Override
        protected Integer get(long struct) {
            return MemoryUtil.memGetInt(struct + offset);
        }

    }

    protected static class FloatMember extends MappedMember<Float> {

        public FloatMember() {
            super(Float.BYTES);
        }

        @Override
        protected void set(long struct, Object value) {
            MemoryUtil.memPutFloat(struct + offset, (float)value);
        }

        @Override
        protected Float get(long struct) {
            return MemoryUtil.memGetFloat(struct + offset);
        }

    }

    protected static class DoubleMember extends MappedMember<Double> {

        public DoubleMember() {
            super(Double.BYTES);
        }

        @Override
        protected void set(long struct, Object value) {
            MemoryUtil.memPutDouble(struct + offset, (double)value);
        }

        @Override
        protected Double get(long struct) {
            return MemoryUtil.memGetDouble(struct + offset);
        }

    }

    protected static class LongMember extends MappedMember<Long> {

        public LongMember() {
            super(Long.BYTES);
        }

        @Override
        protected void set(long struct, Object value) {
            MemoryUtil.memPutLong(struct + offset, (long)value);
        }

        @Override
        protected Long get(long struct) {
            return MemoryUtil.memGetLong(struct + offset);
        }

    }

}
