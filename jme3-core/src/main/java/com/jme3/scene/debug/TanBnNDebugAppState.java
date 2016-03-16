package com.jme3.scene.debug;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Mesh.Mode;
import com.jme3.scene.Spatial;
import com.jme3.util.TangentBinormalGenerator;

/**
 * AppState that shows tangents for debugging purposes
 * 
 * @author Riccardo Balbo
 */
public class TanBnNDebugAppState extends AbstractGeometryProcessorDebugAppState{
	private static final Logger log=Logger.getLogger(TanBnNDebugAppState.class.getName());

	private Map<Geometry,Geometry> generatedGeometries=new HashMap<Geometry,Geometry>();
	private Material mat;

	private float debugLineLenght=0.1f;

	public TanBnNDebugAppState(SimpleApplication app){
		super("TanBnNDebug",app.getRootNode());
	}

	public TanBnNDebugAppState(Spatial rootNode){
		super("TanBnNDebug",rootNode);
	}

	@Override
	public void initialize(Application app) {
		super.initialize(app);
		mat=app.getAssetManager().loadMaterial("Common/Materials/VertexColor.j3m");
	}

	@Override
	public boolean processGeometry(float tpf, Geometry g, GeometryState s) {
		Mesh mesh=g.getMesh();

		if(s==GeometryState.REMOVE||s==GeometryState.MESH_UPDATE_REQUIRED){
			Geometry generated=generatedGeometries.get(g);
			if(generated!=null){
				generated.removeFromParent();
				generatedGeometries.remove(g);
			}else{
				log.log(Level.WARNING,"A remove or regen action has been triggered on {0}, but there is no generated geometry.",g);
			}
			if(s==GeometryState.REMOVE) return true;
		}else if(s==GeometryState.UPDATE){
			Geometry generated=generatedGeometries.get(g);
			generated.setLocalTransform(g.getLocalTransform());
			return true;
		}

		// Works only with triangle based meshes
		if(mesh.getMode()!=Mode.Triangles) return false;

		Geometry generated=new Geometry(g.getName(),TangentBinormalGenerator.genTbnLines(mesh,debugLineLenght));
		generated.setMaterial(mat);
		generated.setLocalTransform(g.getWorldTransform());

		rootNode.attachChild(generated);
		generatedGeometries.put(g,generated);
		return true;
	}

	public void setDebugLinesLength(float l) {
		debugLineLenght=l;
	}

}