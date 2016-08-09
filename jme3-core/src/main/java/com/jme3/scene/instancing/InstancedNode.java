/*
 * Copyright (c) 2014 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.scene.instancing;

import com.jme3.material.Material;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.GeometryGroupNode;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.UserData;
import com.jme3.scene.control.Control;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.material.MatParam;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class InstancedNode extends GeometryGroupNode {

    static int getGeometryStartIndex2(Geometry geom) {
        return getGeometryStartIndex(geom);
    }

    static void setGeometryStartIndex2(Geometry geom, int startIndex) {
        setGeometryStartIndex(geom, startIndex);
    }

    private static final class InstanceTypeKey implements Cloneable, JmeCloneable {

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

        @Override
        public Object jmeClone() {
            try {
                return super.clone();
            } catch( CloneNotSupportedException e ) {
                throw new AssertionError();
            }
        }

        @Override
        public void cloneFields( Cloner cloner, Object original ) {
            this.mesh = cloner.clone(mesh);
            this.material = cloner.clone(material);
        }
    }

    private static class InstancedNodeControl implements Control, JmeCloneable {

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
        public Object jmeClone() {
            try {
                return super.clone();
            } catch( CloneNotSupportedException e ) {
                throw new RuntimeException("Error cloning control", e);
            }
        }

        @Override
        public void cloneFields( Cloner cloner, Object original ) {
            this.node = cloner.clone(node);
        }

        public void setSpatial(Spatial spatial){
        }

        public void update(float tpf){
        }

        public void render(RenderManager rm, ViewPort vp) {
            node.renderFromControl();
        }

        public void write(JmeExporter ex) throws IOException {
        }

        public void read(JmeImporter im) throws IOException {
        }
    }

    protected InstancedNodeControl control;

    protected HashMap<Geometry, InstancedGeometry> igByGeom
            = new HashMap<Geometry, InstancedGeometry>();

    private InstanceTypeKey lookUp = new InstanceTypeKey();

    private HashMap<InstanceTypeKey, InstancedGeometry> instancesMap =
            new HashMap<InstanceTypeKey, InstancedGeometry>();

    public InstancedNode() {
        super();
        // NOTE: since we are deserializing,
        // the control is going to be added automatically here.
    }

    public InstancedNode(String name) {
        super(name);
        control = new InstancedNodeControl(this);
        addControl(control);
    }

    private void renderFromControl() {
        for (InstancedGeometry ig : instancesMap.values()) {
            ig.updateInstances();
        }
    }

    private InstancedGeometry lookUpByGeometry(Geometry geom) {
        lookUp.mesh = geom.getMesh();
        lookUp.material = geom.getMaterial();
        lookUp.lodLevel = geom.getLodLevel();

        InstancedGeometry ig = instancesMap.get(lookUp);

        if (ig == null) {
            ig = new InstancedGeometry(
                    "mesh-" + System.identityHashCode(lookUp.mesh) + "," +
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

    private void addToInstancedGeometry(Geometry geom) {
        Material material = geom.getMaterial();
        MatParam param = material.getParam("UseInstancing");
        if (param == null || !((Boolean)param.getValue()).booleanValue()) {
            throw new IllegalStateException("You must set the 'UseInstancing' "
                    + "parameter to true on the material prior "
                    + "to adding it to InstancedNode");
        }

        InstancedGeometry ig = lookUpByGeometry(geom);
        igByGeom.put(geom, ig);
        geom.associateWithGroupNode(this, 0);
        ig.addInstance(geom);
    }

    private void removeFromInstancedGeometry(Geometry geom) {
        InstancedGeometry ig = igByGeom.remove(geom);
        if (ig != null) {
            ig.deleteInstance(geom);
        }
    }

    private void relocateInInstancedGeometry(Geometry geom) {
        InstancedGeometry oldIG = igByGeom.get(geom);
        InstancedGeometry newIG = lookUpByGeometry(geom);
        if (oldIG != newIG) {
            if (oldIG == null) {
                throw new AssertionError();
            }
            oldIG.deleteInstance(geom);
            newIG.addInstance(geom);
            igByGeom.put(geom, newIG);
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
                addToInstancedGeometry(g);
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
    public Node clone() {
        return clone(true);
    }

    @Override
    public Node clone(boolean cloneMaterials) {
        InstancedNode clone = (InstancedNode)super.clone(cloneMaterials);

        if (instancesMap.size() > 0) {
            // Remove all instanced geometries from the clone
            for (int i = 0; i < clone.children.size(); i++) {
                if (clone.children.get(i) instanceof InstancedGeometry) {
                    clone.children.remove(i);
                } else if (clone.children.get(i) instanceof Geometry) {
                    Geometry geom = (Geometry) clone.children.get(i);
                    if (geom.isGrouped()) {
                        throw new AssertionError();
                    }
                }
            }
        }

        // remove original control from the clone
        clone.controls.remove(this.control);

        // put clone's control in
        clone.control = new InstancedNodeControl(clone);
        clone.controls.add(clone.control);

        clone.lookUp = new InstanceTypeKey();
        clone.igByGeom = new HashMap<Geometry, InstancedGeometry>();
        clone.instancesMap = new HashMap<InstanceTypeKey, InstancedGeometry>();

        clone.instance();

        return clone;
    }

    /**
     *  Called internally by com.jme3.util.clone.Cloner.  Do not call directly.
     */
    @Override
    public void cloneFields( Cloner cloner, Object original ) {
        super.cloneFields(cloner, original);

        this.control = cloner.clone(control);
        this.lookUp = cloner.clone(lookUp);

        HashMap<Geometry, InstancedGeometry> newIgByGeom = new HashMap<Geometry, InstancedGeometry>();
        for( Map.Entry<Geometry, InstancedGeometry> e : igByGeom.entrySet() ) {
            newIgByGeom.put(cloner.clone(e.getKey()), cloner.clone(e.getValue()));
        }
        this.igByGeom = newIgByGeom;

        HashMap<InstanceTypeKey, InstancedGeometry> newInstancesMap = new HashMap<InstanceTypeKey, InstancedGeometry>();
        for( Map.Entry<InstanceTypeKey, InstancedGeometry> e : instancesMap.entrySet() ) {
            newInstancesMap.put(cloner.clone(e.getKey()), cloner.clone(e.getValue()));
        }
        this.instancesMap = newInstancesMap;
    }

    @Override
    public void onTransformChange(Geometry geom) {
        // Handled automatically
    }

    @Override
    public void onMaterialChange(Geometry geom) {
        relocateInInstancedGeometry(geom);
    }

    @Override
    public void onMeshChange(Geometry geom) {
        relocateInInstancedGeometry(geom);
    }

    @Override
    public void onGeometryUnassociated(Geometry geom) {
        removeFromInstancedGeometry(geom);
    }
}
