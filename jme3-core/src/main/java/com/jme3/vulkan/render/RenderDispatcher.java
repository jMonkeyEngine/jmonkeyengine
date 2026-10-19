package com.jme3.vulkan.render;

import com.jme3.bounding.BoundingBox;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.material.experimental.ShadingTechnique;
import com.jme3.vulkan.scene.Scene;

import java.util.List;

/**
 * Figures out which of a scene's nodes should be rendered this frame, splits them up
 * between {@code viewport}'s {@link Bucket}s and {@code techniques}, and renders each
 * resulting group immediately via {@link ShadingTechnique#render} - there is no result to
 * read back afterward, since every group is handed to its technique as soon as it is
 * formed rather than being collected anywhere first.
 * <p>
 * Determining "should be rendered" is this interface's responsibility, not its callers':
 * an implementation decides what counts (attached, geometric, not culled against
 * {@code viewport}'s camera, whatever else it judges relevant) before distributing
 * anything.
 * <p>
 * Sorting happens in four levels, only the first, second, and fourth of which are this
 * interface's concern:
 * <ol>
 *     <li>Bucket. Every candidate node belongs to exactly one bucket, chosen by matching
 *     {@link Scene#getWorldBucketHint} against each of {@code viewport}'s
 *     {@link Bucket#getHint()} - that's what keeps a transparent node from ever competing
 *     with an opaque one for a technique.</li>
 *     <li>Technique. Within one bucket, {@code techniques} are tried in the order given:
 *     the first technique in the list gets first crack at every node in that bucket via
 *     {@link ShadingTechnique#acceptNode}, whatever it claims is removed from
 *     consideration, and the next technique only ever sees what's left. A node therefore
 *     ends up claimed by at most one technique, in at most one bucket - never rendered
 *     twice.</li>
 *     <li>Pipeline. Not this interface's job - once a technique is handed its group of
 *     nodes, how it further groups them by pipeline internally is entirely up to that
 *     technique.</li>
 *     <li>Bucket sort. The last word on order within a single bucket/technique pairing:
 *     a group's nodes should be sorted with its bucket's {@link Bucket#getSort()}, if it
 *     has one, before being handed to the technique.</li>
 * </ol>
 * Buckets are processed in the order {@code viewport} lists them (so, e.g., every opaque
 * group is rendered before any transparent one regardless of technique), and within a
 * bucket, techniques are processed in the order {@code techniques} was given.
 * Implementations may skip calling {@link ShadingTechnique#render} for a bucket/technique
 * pairing entirely rather than calling it with an empty group.
 */
public interface RenderDispatcher {

    Scene.Group extractNodes(Scene scene, ViewPort viewport);

    void dispatch(CommandBuffer cmd, ViewPort viewport, Scene.Group nodes, List<ShadingTechnique> techniques);

    RenderDispatcher DEFAULT = new RenderDispatcher() {
        @Override
        public Scene.Group extractNodes(Scene scene, ViewPort viewport) {
            BoundingBox bounds = new BoundingBox();
            Scene.Group group = scene.createHierarchalGroup(node -> {
                int cullHint = scene.getWorldCullHint(node);
                if (cullHint == Spatial.CullHint.Never.ordinal()) {
                    return true;
                } else if (cullHint == Spatial.CullHint.Always.ordinal()) {
                    return false;
                }
                return viewport.getCamera().contains(scene.getWorldBounds(node, bounds)).isInside();
            });
            group.removeIf((int node) -> !scene.hasComponent(Scene.MeshType.class, node));
            return group;
        }
        @Override
        public void dispatch(CommandBuffer cmd, ViewPort viewport, Scene.Group nodes, List<ShadingTechnique> techniques) {
            for (Integer i : viewport.getBuckets().keySet()) {
                Scene.Group batch = nodes.transfer(node -> i == nodes.getScene().getWorldBucketHint(node));
                for (ShadingTechnique tech : techniques) {
                    tech.render(cmd, batch.transfer(tech::acceptNode));
                }
            }
        }
    };

}
