package com.jme3.vulkan.struct;

import com.fasterxml.jackson.databind.JsonNode;
import org.lwjgl.system.*;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class MappedStruct extends Struct<MappedStruct> implements NativeResource {

    private final Layout layout;

    public MappedStruct(long address, Layout layout) {
        this(address, null, layout);
    }

    public MappedStruct(long address, ByteBuffer container, Layout layout) {
        super(address, container);
        this.layout = layout;
    }

    @Override
    protected MappedStruct create(long address, ByteBuffer container) {
        return new MappedStruct(address, container, layout);
    }

    @Override
    public int sizeof() {
        return layout.sizeof;
    }

    public int alignof() {
        return layout.alignof;
    }

    public <T extends StructMember> T getMember(String name) {
        return layout.getMember(name);
    }

    public void set(String name, Object value) {
        Objects.requireNonNull(layout.members.get(name), "Struct member \"" + name + "\" does not exist.")
                .setAtPointer(address, value);
    }

    public <T> T get(String name) {
        return (T)Objects.requireNonNull(layout.members.get(name), "Struct member \"" + name + "\" does not exist.")
                .getAtPointer(address);
    }

    public void parse(String name, JsonNode json) {
        Objects.requireNonNull(layout.members.get(name), "Struct member \"" + name + "\" does not exist.")
                .parse(address, json);
    }

    public Layout getLayout() {
        return layout;
    }

    public static MappedStruct create(long address, Layout layout) {
        return new MappedStruct(address, layout);
    }

    public static MappedStruct.Buffer create(long address, int capacity, Layout layout) {
        return new MappedStruct.Buffer(layout, address, capacity);
    }

    public static MappedStruct malloc(Consumer<Layout> config) {
        return malloc(layout(config));
    }

    public static MappedStruct malloc(Layout layout) {
        return new MappedStruct(MemoryUtil.nmemAllocChecked(layout.sizeof), layout);
    }

    public static MappedStruct calloc(Consumer<Layout> config) {
        return calloc(layout(config));
    }

    public static MappedStruct calloc(Layout layout) {
        return new MappedStruct(MemoryUtil.nmemCallocChecked(1, layout.sizeof), layout);
    }

    public static MappedStruct.Buffer malloc(int capacity, Consumer<Layout> config) {
        return malloc(capacity, layout(config));
    }

    public static MappedStruct.Buffer malloc(int capacity, Layout layout) {
        return new Buffer(layout, MemoryUtil.nmemAllocChecked(__checkMalloc(capacity, layout.sizeof)), capacity);
    }

    public static MappedStruct.Buffer calloc(int capacity, Consumer<Layout> config) {
        return calloc(capacity, layout(config));
    }

    public static MappedStruct.Buffer calloc(int capacity, Layout layout) {
        return new Buffer(layout, MemoryUtil.nmemCallocChecked(capacity, layout.sizeof), capacity);
    }

    public static MappedStruct malloc(MemoryStack stack, Consumer<Layout> config) {
        return malloc(stack, layout(config));
    }

    public static MappedStruct malloc(MemoryStack stack, Layout layout) {
        return new MappedStruct(stack.nmalloc(layout.alignof, layout.sizeof), layout);
    }

    public static MappedStruct calloc(MemoryStack stack, Consumer<Layout> config) {
        return calloc(stack, layout(config));
    }

    public static MappedStruct calloc(MemoryStack stack, Layout layout) {
        return new MappedStruct(stack.ncalloc(layout.alignof, 1, layout.sizeof), layout);
    }

    public static MappedStruct.Buffer malloc(int capacity, MemoryStack stack, Consumer<Layout> config) {
        return malloc(capacity, stack, layout(config));
    }

    public static MappedStruct.Buffer malloc(int capacity, MemoryStack stack, Layout layout) {
        return new Buffer(layout, stack.nmalloc(layout.alignof, capacity * layout.sizeof), capacity);
    }

    public static MappedStruct.Buffer calloc(int capacity, MemoryStack stack, Consumer<Layout> config) {
        return calloc(capacity, stack, layout(config));
    }

    public static MappedStruct.Buffer calloc(int capacity, MemoryStack stack, Layout layout) {
        return new Buffer(layout, stack.ncalloc(layout.alignof, capacity, layout.sizeof), capacity);
    }

    public static Layout layout(Consumer<MappedStruct.Layout> config) {
        Layout l = new Layout();
        config.accept(l);
        l.build();
        return l;
    }

    public static Layout layout(JsonNode jsonLayout, Map<String, JsonNode> defaults) {
        Layout layout = new Layout();
        for (JsonNode member : jsonLayout) {
            String type;
            if (member.has("type")) {
                type = member.get("type").asText();
            } else {
                type = member.asText();
            }
            String name = member.get("name").asText();
            layout.add(name, StructMember.create(type));
            if (member.has("default")) {
                defaults.put(name, member.get("default"));
            }
        }
        layout.build();
        return layout;
    }

    public static class Layout extends Struct<Layout> {

        private final Map<String, StructMember> members = new LinkedHashMap<>();
        private int sizeof, alignof;
        private boolean built = false;

        private Layout() {
            super(0L, null);
        }

        public void add(String name, StructMember member) {
            assert !built : "Layout already built.";
            members.put(name, member);
        }

        private void build() {
            built = true;
            if (members.isEmpty()) {
                throw new IllegalStateException("Struct cannot be empty.");
            }
            Layout layout = __struct(members.values().stream()
                    .map(m -> __member(m.size))
                    .toArray(Struct.Member[]::new));
            sizeof = layout.getSize();
            alignof = layout.getAlignment();
            int i = 0;
            for (StructMember m : members.values()) {
                m.offset = layout.offsetof(i++);
            }
        }

        public <T extends StructMember> T getMember(String name) {
            return (T)members.get(name);
        }

        @Override
        protected MappedStruct.Layout create(long l, ByteBuffer byteBuffer) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int sizeof() {
            return sizeof;
        }

    }

    public static class Buffer extends StructBuffer<MappedStruct, Buffer> implements NativeResource {

        private final MappedStruct factory;

        public Buffer(Layout layout, long address, int cap) {
            super(address, null, -1, 0, cap, cap);
            this.factory = new MappedStruct(0L, layout);
        }

        public Buffer(Layout layout, long address, ByteBuffer container, int mark, int position, int limit, int capacity) {
            super(address, container, mark, position, limit, capacity);
            this.factory = new MappedStruct(0L, layout);
        }

        public Buffer(Layout layout, ByteBuffer container, int remaining) {
            super(container, remaining);
            this.factory = new MappedStruct(0L, layout);
        }

        @Override
        protected MappedStruct getElementFactory() {
            return factory;
        }

        @Override
        protected Buffer self() {
            return this;
        }

        @Override
        protected Buffer create(long address, ByteBuffer container, int mark, int position, int limit, int capacity) {
            return new Buffer(factory.layout, address, container, mark, position, limit, capacity);
        }

    }

}
