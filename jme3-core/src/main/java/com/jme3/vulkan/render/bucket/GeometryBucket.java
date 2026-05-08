package com.jme3.vulkan.render.bucket;

import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.vulkan.commands.StandardRenderSettings;

import java.util.*;
import java.util.function.Predicate;

public class GeometryBucket extends LinkedList<Geometry> {

    private final Comparator<RenderElement> comparator;

    public GeometryBucket(Comparator<RenderElement> comparator) {
        this.comparator = comparator;
    }

    public void setupRender(ViewPort vp, StandardRenderSettings settings) {}

    public void cleanupRender(ViewPort vp, StandardRenderSettings settings) {}

    public Collection<Geometry> selectGeometries(Predicate<Geometry> selector) {
        Collection<Geometry> result = new ArrayList<>(size());
        for (Iterator<Geometry> it = iterator(); it.hasNext();) {
            Geometry g = it.next();
            if (selector.test(g)) {
                result.add(g);
                it.remove();
            }
        }
        return result;
    }

    public Comparator<RenderElement> getComparator() {
        return comparator;
    }

}
