package com.jme3.scene.instancing;

import com.jme3.material.Material;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.BatchNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.GeometryGroupNode;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.UserData;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.control.Control;
import java.util.HashMap;

public class InstancedNode extends GeometryGroupNode {
    
    static int getGeometryStartIndex2(Geometry geom) {
        return getGeometryStartIndex(geom);
    }
    
    static void setGeometryStartIndex2(Geometry geom, int startIndex) {
        setGeometryStartIndex(geom, startIndex);
    }
    
    private static class InstanceTypeKey implements Cloneable {

        Mesh mesh;
        Material material;
        int lodLevel;

        public InstanceTypeKey(Mesh mesh, Material material, int lodLevel) {
            this.mesh = mesh;
            this.material = material;
            this.lodLevel = lodLevel;
        }
        
        public InstanceTypeKey(){
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 41 * hash + this.mesh.hashCode();
            hash = 41 * hash + this.material.hashCode();
            hash = 41 * hash + this.lodLevel;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            final InstanceTypeKey other = (InstanceTypeKey) obj;
            if (this.mesh != other.mesh) {
                return false;
            }
            if (this.material != other.material) {
                return false;
            }
            if (this.lodLevel != other.lodLevel) {
                return false;
            }
            return true;
        }
        
        @Override
        public InstanceTypeKey clone() {
            try {
                return (InstanceTypeKey) super.clone();
            } catch (CloneNotSupportedException ex) {
                throw new AssertionError();
            }
        }
    }
    
    private static class InstancedNodeControl extends AbstractControl {

        private InstancedNode node;
        
        public InstancedNodeControl() {
        }
        
        public InstancedNodeControl(InstancedNode node) {
            this.node = node;
        }
        
        @Override
        public Control cloneForSpatial(Spatial spatial) {
            return this; 
            // WARNING: Sets wrong control on spatial. Will be
            // fixed automatically by InstancedNode.clone() method.
        }
        
        @Override
        protected void controlUpdate(float tpf) {
        }

        @Override
        protected void controlRender(RenderManager rm, ViewPort vp) {
            node.renderFromControl();
        }
    }
    
    protected final HashMap<Geometry, InstancedGeometry> igByGeom 
            = new HashMap<Geometry, InstancedGeometry>();
    
    private final InstanceTypeKey lookUp = new InstanceTypeKey();
    
    private final HashMap<InstanceTypeKey, InstancedGeometry> instancesMap = 
            new HashMap<InstanceTypeKey, InstancedGeometry>();
    
    public InstancedNode() {
        super();
        // NOTE: since we are deserializing,
        // the control is going to be added automatically here.
    }
    
    public InstancedNode(String name) {
        super(name);
        addControl(new InstancedNodeControl(this));
    }
    
    private void renderFromControl() {
        for (InstancedGeometry ig : instancesMap.values()) {
            ig.updateInstances();
        }
    }
    
    private static boolean isInstancedGeometry(Geometry geom) {
        return geom instanceof InstancedGeometry;
    }
    
    private InstancedGeometry lookUpByGeometry(Geometry geom) {
        lookUp.mesh = geom.getMesh();
        lookUp.material = geom.getMaterial();
        lookUp.lodLevel = geom.getLodLevel();

        InstancedGeometry ig = instancesMap.get(lookUp);

        if (ig == null) {
            ig = new InstancedGeometry(
                    "material-" + lookUp.material.getMaterialDef().getName() + ","
                    + "lod-" + lookUp.lodLevel);
            ig.setMaterial(lookUp.material);
            ig.setMesh(lookUp.mesh);
            ig.setUserData(UserData.JME_PHYSICSIGNORE, true);
            ig.setCullHint(CullHint.Never);
            instancesMap.put(lookUp.clone(), ig);
            attachChild(ig);
        }

        return ig;
    }
    
    private void removeFromInstancedGeometry(Geometry geom) {
        InstancedGeometry ig = igByGeom.remove(geom);
        if (ig != null) {
            ig.deleteInstance(geom);
        }
    }
    
    private void ungroupSceneGraph(Spatial s) {
        if (s instanceof Node) {
            for (Spatial sp : ((Node) s).getChildren()) {
                ungroupSceneGraph(sp);
            }
        } else if (s instanceof Geometry) {
            Geometry g = (Geometry) s;
            if (g.isGrouped()) {
                // Will invoke onGeometryUnassociated automatically.
                g.unassociateFromGroupNode();
                if (InstancedNode.getGeometryStartIndex(g) != -1) {
                    throw new AssertionError();
                }
            }
        }
    }
    
    @Override
    public Spatial detachChildAt(int index) {
        Spatial s = super.detachChildAt(index);
        if (s instanceof Node) {
            ungroupSceneGraph(s);
        }
        return s;
    }
    
    private void instance(Spatial n) {
        if (n instanceof Geometry) {
            Geometry g = (Geometry) n;
            if (!g.isGrouped() && g.getBatchHint() != BatchHint.Never) {
                InstancedGeometry ig = lookUpByGeometry(g);
                igByGeom.put(g, ig);
                g.associateWithGroupNode(this, 0);
                ig.addInstance(g);
            }
        } else if (n instanceof Node) {
            for (Spatial child : ((Node) n).getChildren()) {
                if (child instanceof GeometryGroupNode) {
                    continue;
                }
                instance(child);
            }
        }
    }
    
    public void instance() {
        instance(this);
    }
    
    @Override
    public Node clone(boolean cloneMaterials) {
        InstancedNode clone = (InstancedNode)super.clone(cloneMaterials);
        if (instancesMap.size() > 0) {
            // Remove all instanced geometries from the clone
            for (int i = 0; i < clone.children.size(); i++) {
                if (clone.children.get(i) instanceof InstancedGeometry) {
                    clone.children.remove(i);
                }
            }
            
            // Clear state (which is incorrect)
            clone.igByGeom.clear();
            clone.instancesMap.clear();
            clone.instance();
        }
        return clone;
    }
    
    private void majorChange(Geometry geom) {
        InstancedGeometry oldIG = igByGeom.get(geom);
        InstancedGeometry newIG = lookUpByGeometry(geom);
        if (oldIG != newIG) {
            oldIG.deleteInstance(geom);
            newIG.addInstance(geom);
            igByGeom.put(geom, newIG);
        }
    }
    
    @Override
    public void onTransformChange(Geometry geom) {
        // Handled automatically
    }

    @Override
    public void onMaterialChange(Geometry geom) {
        majorChange(geom);
    }

    @Override
    public void onMeshChange(Geometry geom) {
        majorChange(geom);
    }

    @Override
    public void onGeoemtryUnassociated(Geometry geom) {
        removeFromInstancedGeometry(geom);
    }
}
