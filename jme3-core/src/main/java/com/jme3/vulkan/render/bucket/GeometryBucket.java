package com.jme3.vulkan.render.bucket;

import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.vulkan.commands.StandardRenderSettings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GeometryBucket {

    private final Comparator<BucketElement> comparator;
    private final Collection<Geometry> bucket = new ArrayList<>();

    public GeometryBucket(Comparator<BucketElement> comparator) {
        this.comparator = comparator;
    }

    public void setupRender(ViewPort vp, StandardRenderSettings settings) {}

    public void cleanupRender(ViewPort vp, StandardRenderSettings settings) {}

    public void add(Geometry geometry) {
        bucket.add(geometry);
    }

    public <T extends BucketElement> List<T> sort(Function<Geometry, T> elementFactory) {
        return bucket.stream().map(elementFactory).sorted(comparator).collect(Collectors.toList());
    }

    public void clear() {
        bucket.clear();
    }

}
