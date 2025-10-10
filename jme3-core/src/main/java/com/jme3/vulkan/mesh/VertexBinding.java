package com.jme3.vulkan.mesh;

import com.jme3.vulkan.Format;
import com.jme3.vulkan.util.IntEnum;

import java.util.*;

public class VertexBinding implements Iterable<VertexAttribute> {

    private final int binding;
    private final IntEnum<InputRate> rate;
    private final Map<String, VertexAttribute> attributes = new HashMap<>();
    private int stride = 0;

    public VertexBinding(int binding, IntEnum<InputRate> rate) {
        this.binding = binding;
        this.rate = Objects.requireNonNull(rate);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        VertexBinding that = (VertexBinding) o;
        return binding == that.binding
                && stride == that.stride
                && rate.is(that.rate)
                && Objects.equals(attributes, that.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(binding, rate, attributes, stride);
    }

    protected VertexAttribute addAttribute(String name, Format format, int location) {
        VertexAttribute attr = new VertexAttribute(this, name, format, location, stride);
        attributes.put(name, attr);
        stride += format.getTotalBytes();
        return attr;
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
