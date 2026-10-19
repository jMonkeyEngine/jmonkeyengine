package com.jme3.vulkan.scene.components;

@Deprecated
public interface ComponentPipe {

    void onComponentChanged(Component type, int node);

    void activate(int node);

    void deactivate(int node);

    @SuppressWarnings("unchecked")
    default Class<? extends Component>[] getObservedTypes() {
        Observes o = getClass().getAnnotation(Observes.class);
        return o != null ? o.components() : new Class[0];
    }

}
