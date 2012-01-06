/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.nmgen;

import com.jme3.bounding.BoundingBox;
import com.jme3.gde.core.assets.AssetDataObject;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.gde.core.sceneexplorer.nodes.JmeSpatial;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.terrain.Terrain;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import jme3tools.optimize.GeometryBatchFactory;
import org.critterai.nmgen.IntermediateData;

/**
 *
 * @author sploreg
 */
public class NavMeshController {
    private JmeSpatial jmeRootNode;
    private Node rootNode;
    private AssetDataObject currentFileObject;
    private NavMeshTopComponent topComponent;
    //private NavMesh navMesh; // current nav mesh in this scene
    private Material navMaterial;

    public NavMeshController(JmeSpatial jmeRootNode, AssetDataObject currentFileObject, NavMeshTopComponent topComponent) {
        this.jmeRootNode = jmeRootNode;
        this.currentFileObject = currentFileObject;
        this.topComponent = topComponent;
        rootNode = this.jmeRootNode.getLookup().lookup(Node.class);
    }
    
    protected void cleanup() {
        
    }
    
    public void setNeedsSave(boolean state) {
        currentFileObject.setModified(state);
    }

    protected Mesh generateNavMesh(float cellSize, 
            float cellHeight, 
            float minTraversableHeight, 
            float maxTraversableStep, 
            float maxTraversableSlope, 
            boolean clipLedges, 
            float traversableAreaBorderSize, 
            float smoothingThreshold, 
            boolean useConservativeExpansion, 
            float minUnconnectedRegionSize, 
            float mergeRegionSize, 
            float maxEdgeLength, 
            float edgeMaxDeviation, 
            float maxVertsPerPoly, 
            float contourSampleDistance, 
            float contourMaxDeviation) 
    {
        NavMeshGenerator generator = new NavMeshGenerator();
        generator.setCellSize(cellSize);
        generator.setCellHeight(cellHeight);
        generator.setMinTraversableHeight(minTraversableHeight);
        generator.setMaxTraversableStep(maxTraversableStep);
        generator.setMaxTraversableSlope(maxTraversableSlope);
        generator.setClipLedges(clipLedges);
        generator.setTraversableAreaBorderSize(traversableAreaBorderSize);
        generator.setSmoothingThreshold((int)smoothingThreshold);
        generator.setUseConservativeExpansion(useConservativeExpansion);
        generator.setMergeRegionSize((int)mergeRegionSize);
        generator.setMaxEdgeLength(maxEdgeLength);
        generator.setEdgeMaxDeviation(edgeMaxDeviation);
        generator.setMaxVertsPerPoly((int)maxVertsPerPoly);
        generator.setContourSampleDistance(contourSampleDistance);
        generator.setContourMaxDeviation(contourMaxDeviation);
        
        IntermediateData id = new IntermediateData();
        
        generator.setIntermediateData(null);
        
        Mesh mesh = new Mesh();
        //NavMesh navMesh = new NavMesh();
        
        GeometryBatchFactory.mergeGeometries(findGeometries(rootNode, new LinkedList<Geometry>(), generator), mesh);
        Mesh optiMesh = generator.optimize(mesh);

        final Geometry navMesh = new Geometry("NavMesh");
        navMesh.setMesh(optiMesh);
        navMesh.setCullHint(CullHint.Always);
        navMesh.setModelBound(new BoundingBox());
        
        Spatial previous = rootNode.getChild("NavMesh");
        if (previous != null)
            previous.removeFromParent();
        
        SceneApplication.getApplication().enqueue(new Callable<Void>() {
            public Void call() throws Exception {
                rootNode.attachChild(navMesh);
                return null;
            }
        });
        
        jmeRootNode.refresh(true);
        
        //navMesh.setUserData("NavMeshGenerator", generator);
        
        //navMesh.loadFromMesh(optiMesh);
        //saveNavMesh(navMesh, generator);
        
        setNeedsSave(true);
        
        return optiMesh;
    }
    
    /**
     * NavMesh saves as user data on the scene's root node
     */
    /*private void saveNavMesh(NavMesh navMesh, NavMeshGenerator generator) {
        rootNode.setUserData("NavMesh", navMesh);
        rootNode.setUserData("NavMeshGenerator", generator);
        this.navMesh = navMesh;
    }
    
    protected NavMesh getNavMesh() {
        if (navMesh == null) {
            navMesh = findNavMesh();
        }
        return navMesh;
    }
    
    protected NavMesh findNavMesh() {
        return rootNode.getUserData("NavMesh");
    }*/
    
    protected NavMeshGenerator getNavMeshGenerator() {
        return rootNode.getUserData("NavMeshGenerator");
    }
    
    private List<Geometry> findGeometries(Node node, List<Geometry> geoms, NavMeshGenerator generator) {
        for (Iterator<Spatial> it = node.getChildren().iterator(); it.hasNext();) {
            Spatial spatial = it.next();
            if (spatial instanceof Geometry) {
                geoms.add((Geometry) spatial);
            } else if (spatial instanceof Node) {
                if (spatial instanceof Terrain) {
                    Mesh merged = generator.terrain2mesh((Terrain)spatial);
                    Geometry g = new Geometry("mergedTerrain");
                    g.setMesh(merged);
                    geoms.add(g);
                } else 
                    findGeometries((Node) spatial, geoms, generator);
            }
        }
        return geoms;
    }

    /**
     * Create polygons from each of the Nav Mesh Cells.
     * @return a single mesh representing the nav mesh
     */
    /*protected Mesh createDebugMesh() {
        NavMesh navMesh = getNavMesh();
        if (navMesh != null) {
            List<Geometry> meshes = new ArrayList<Geometry>();
            for (int i=0; i<navMesh.getNumCells(); i++) {
                Mesh m = navMesh.getCell(i).getDebugMesh();
                Geometry g = new Geometry();
                g.setMaterial(getNavMaterial());
                g.setMesh(m);
                meshes.add(g);
            }
            Mesh mesh = new Mesh();
            GeometryBatchFactory.mergeGeometries(meshes, mesh);
            return mesh;
        }
        return null;
    }*/
    
    private Material getNavMaterial() {
        if (navMaterial != null)
            return navMaterial;
        navMaterial = new Material(SceneApplication.getApplication().getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        navMaterial.setColor("Color", ColorRGBA.Green);
        navMaterial.getAdditionalRenderState().setWireframe(true);
        return navMaterial;
    }
    
}
