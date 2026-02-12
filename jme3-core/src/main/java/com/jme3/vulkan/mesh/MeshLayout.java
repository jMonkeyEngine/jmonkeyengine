package com.jme3.vulkan.mesh;

import com.jme3.scene.Mesh;
import com.jme3.vulkan.mesh.attribute.Attribute;

import java.util.*;
import java.util.function.Consumer;

public class MeshLayout {

    private final List<VertexBinding> bindings = new ArrayList<>();

    protected MeshLayout() {}

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        MeshLayout that = (MeshLayout) o;
        return Objects.equals(bindings, that.bindings);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(bindings);
    }

    public <T extends Attribute> T mapAttribute(Mesh mesh, String name) {
        for (VertexBinding vb : bindings) {
            T attr = vb.mapAttribute(name, mesh.getVertexBuffer(vb).getData(), mesh.getElements(vb.getInputRate()));
            if (attr != null) {
                return attr;
            }
        }
        return null;
    }

    public boolean attributeExists(String name) {
        for (VertexBinding binding : bindings) {
            if (binding.getAttributes().stream().anyMatch(a -> a.getName().equals(name))) {
                return true;
            }
        }
        return false;
    }

    public List<VertexBinding> getBindings() {
        return Collections.unmodifiableList(bindings);
    }

    public VertexBinding getBinding(int binding) {
        return bindings.get(binding);
    }

    public boolean isOneAttributePerBinding() {
        return bindings.stream().allMatch(v -> v.getAttributes().size() == 1);
    }

    public static MeshLayout build(Consumer<Builder> config) {
        Builder b = new MeshLayout().new Builder();
        config.accept(b);
        return b.build();
    }

    public class Builder {

        public MeshLayout build() {
            return MeshLayout.this;
        }

        public void addBinding(VertexBinding binding) {
            if (binding.getAttributes().isEmpty()) {
                throw new IllegalArgumentException("Vertex binding must have at least one attribute.");
            }
            bindings.add(binding);
        }

    }

}
