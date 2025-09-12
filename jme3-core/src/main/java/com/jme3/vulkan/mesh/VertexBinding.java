package com.jme3.vulkan.mesh;

import com.jme3.vulkan.Format;
import com.jme3.vulkan.util.IntEnum;

import java.util.*;

public class VertexBinding implements Iterable<VertexAttribute> {

    private final int binding;
    private final IntEnum<InputRate> rate;
    private final Map<String, VertexAttribute> attributes = new HashMap<>();
    private int stride;

    public VertexBinding(int binding, IntEnum<InputRate> rate) {
        this.binding = binding;
        this.rate = rate;
    }

    private int findNextAttributeOffset(Format format) {
        int offset = 0;
        for (VertexAttribute a : attributes.values()) {
            offset = Math.max(offset, a.getOffset() + a.getFormat().getTotalBytes());
        }
        stride = offset + format.getTotalBytes();
        return offset;
    }

    protected void addAttribute(String name, Format format, int location) {
        attributes.put(name, new VertexAttribute(this, name, format, location, findNextAttributeOffset(format)));
    }

    public int getBindingIndex() {
        return binding;
    }

    public int getStride() {
        return stride;
    }

    public IntEnum<InputRate> getRate() {
        return rate;
    }

    public VertexAttribute getAttribute(String name) {
        return attributes.get(name);
    }

    public Map<String, VertexAttribute> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    @Override
    public Iterator<VertexAttribute> iterator() {
        return attributes.values().iterator();
    }

}
