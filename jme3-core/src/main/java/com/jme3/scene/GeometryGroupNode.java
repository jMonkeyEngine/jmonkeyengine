package com.jme3.scene;

/**
 * An abstract class for implementations that perform grouping of geometries
 * via instancing or batching.
 * 
 * @author Kirill Vainer
 */
public abstract class GeometryGroupNode extends Node {
    
    public static int getGeometryStartIndex(Geometry geom) {
        return geom.startIndex;
    }
    
    protected static void setGeometryStartIndex(Geometry geom, int startIndex) {
        if (startIndex < -1) {
            throw new AssertionError();
        }
        geom.startIndex = startIndex;
    }
    
    /**
     * Construct a <code>GeometryGroupNode</code>
     */
    public GeometryGroupNode() {
        super();
    }

    /**
     * Construct a <code>GeometryGroupNode</code>
     * 
     * @param name The name of the GeometryGroupNode.
     */
    public GeometryGroupNode(String name) {
        super(name);
    }
    
    /**
     * Called by {@link Geometry geom} to specify that its world transform
     * has been changed.
     * 
     * @param geom The Geometry whose transform changed.
     */
    public abstract void onTransformChange(Geometry geom);
    
    /**
     * Called by {@link Geometry geom} to specify that its 
     * {@link Geometry#setMaterial(com.jme3.material.Material) material}
     * has been changed.
     * 
     * @param geom The Geometry whose material changed.
     * 
     * @throws UnsupportedOperationException If this implementation does
     * not support dynamic material changes.
     */
    public abstract void onMaterialChange(Geometry geom);
    
    /**
     * Called by {@link Geometry geom} to specify that its 
     * {@link Geometry#setMesh(com.jme3.scene.Mesh) mesh}
     * has been changed.
     * 
     * This is also called when the geometry's 
     * {@link Geometry#setLodLevel(int) lod level} changes.
     * 
     * @param geom The Geometry whose mesh changed.
     * 
     * @throws UnsupportedOperationException If this implementation does
     * not support dynamic mesh changes.
     */
    public abstract void onMeshChange(Geometry geom);
    
    /**
     * Called by {@link Geometry geom} to specify that it
     * has been unassociated from its <code>GeoemtryGroupNode</code>.
     * 
     * Unassociation occurs when the {@link Geometry} is 
     * {@link Spatial#removeFromParent() detached} from its parent
     * {@link Node}.
     * 
     * @param geom The Geometry which is being unassociated.
     */
    public abstract void onGeometryUnassociated(Geometry geom);
}
