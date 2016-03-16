package com.jme3.scene.debug;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.SceneGraphVisitor;
import com.jme3.scene.Spatial;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Usage;

/**
 * Abstract appstate that loops between geometries and trigger a method according to their state.
 * 
 * @author Riccardo Balbo
 */
public abstract class AbstractGeometryProcessorDebugAppState extends BaseAppState{
	public static enum GeometryState{
		PROCESS_FIRST,MESH_UPDATE_REQUIRED,UPDATE,REMOVE
	}

	private static final Logger log=Logger.getLogger(AbstractGeometryProcessorDebugAppState.class.getName());
	private static final Level logLevel=Level.FINEST;

	protected Node rootNode;
	protected Spatial targetSpatial;

	protected RenderManager renderManager;
	protected ViewPort viewPort;

	protected Application app;
	protected boolean drawOnTop;

	private String name;
	private Map<Geometry,Byte> processedGeometries=new HashMap<Geometry,Byte>();
	private byte updateId=0;
	private Map<Mesh,ArrayList<Integer>> meshVBsnapshot=new WeakHashMap<Mesh,ArrayList<Integer>>();

	public AbstractGeometryProcessorDebugAppState(String name,Spatial rootNode){
		targetSpatial=rootNode;
		this.name=name;
	}

	@Override
	public void initialize(Application app) {
		renderManager=app.getRenderManager();
		this.app=app;
		rootNode=new Node(name+"Node");
		rootNode.setCullHint(CullHint.Never);
	}

	public void setDrawAlwaysOnTop(boolean v) {
		drawOnTop=v;
		if(viewPort!=null) viewPort.setClearFlags(false,v,false);
	}

	@Override
	public void render(RenderManager rm) {
		super.render(rm);
		if(viewPort!=null){
			rm.renderScene(rootNode,viewPort);
		}
	}

	@Override
	public void onEnable() {
		viewPort=renderManager.createMainView(name+"ViewPort",app.getCamera());
		setDrawAlwaysOnTop(drawOnTop);
		viewPort.attachScene(rootNode);
	}

	@Override
	public void onDisable() {
		renderManager.removeMainView(viewPort);
		viewPort=null;
	}

	@Override
	public void update(float tpf) {
		updateId++;
		targetSpatial.depthFirstTraversal(new SceneGraphVisitor(){
			@Override
			public void visit(Spatial spatial) {
				if(!(spatial instanceof Geometry)) return;

				Geometry geom=(Geometry)spatial;
				Mesh mesh=geom.getMesh();

				GeometryState action=GeometryState.UPDATE;

				if(processedGeometries.get(spatial)==null) action=GeometryState.PROCESS_FIRST;
				if(action==GeometryState.UPDATE&&isMeshUpdateNeeded(mesh)) action=GeometryState.MESH_UPDATE_REQUIRED;

				log.log(logLevel,"Execute action {0} on {1}",new Object[]{action,spatial});

				boolean processed=processGeometry(tpf,geom,action);
				if(processed||action!=GeometryState.PROCESS_FIRST){ // If the action was PROCESS_FIRST but failed, do not add the geometry to the processed list.
					processedGeometries.put(geom,updateId);
					log.log(logLevel,"has been processed {0}",processed);
				}
			}
		});

		// Remove references to removed geometries.
		Iterator<Map.Entry<Geometry,Byte>> processedGeometries_i=processedGeometries.entrySet().iterator();
		while(processedGeometries_i.hasNext()){
			Map.Entry<Geometry,Byte> entry=processedGeometries_i.next();
			if(entry.getValue()!=updateId){
				Geometry g=entry.getKey();
				processGeometry(tpf,g,GeometryState.REMOVE);
				processedGeometries_i.remove();
				log.log(logLevel,"{0} is removed!",g);
			}
		}
		rootNode.updateLogicalState(tpf);
		rootNode.updateGeometricState();
	}

	protected boolean isMeshUpdateNeeded(Mesh m) {
		ArrayList<Integer> sn_ids=meshVBsnapshot.get(m);
		ArrayList<Integer> ids=new ArrayList<Integer>();

		boolean updateNeeded=false;

		VertexBuffer[] blist=(VertexBuffer[])m.getBufferList().toArray();
		int i=0;
		for(VertexBuffer b:blist){
			if(b.isUnused()||b.getUsage()==Usage.CpuOnly) continue;
			boolean vBupdateNeeded=b.isUpdateNeeded()||sn_ids==null;
			int id=b.getId();
			ids.add(id);
			if(!vBupdateNeeded){
				if(sn_ids.size()<id||sn_ids.get(i)!=id){ // Control the order as well
					log.log(logLevel,"Mesh {0}: VertexBuffer {1} hash been replaced.",new Object[]{m,b.getBufferType()});
					vBupdateNeeded=true;
				}
			}
			if(vBupdateNeeded){
				log.log(logLevel,"Mesh {0}: VertexBuffer {1} need update.",new Object[]{m,b.getBufferType()});
				updateNeeded=true;
				//return true; We want to collect the id of every vertexbuffer, so the cycle must continue.
			}
			i++;
		}

		if(!updateNeeded&&ids.size()!=sn_ids.size()){
			log.log(logLevel,"Mesh {0}: One or more vertexbuffers has been added or removed",m);
			updateNeeded=true;
		}

		if(updateNeeded){
			meshVBsnapshot.put(m,ids);
			log.log(logLevel,"Mesh {0}:  Store snapshot of buffers: {1}",new Object[]{m,ids});
			log.log(logLevel,"Mesh {0}: need update.",m);
		}

		return updateNeeded;
	}

	public abstract boolean processGeometry(float tpf, Geometry g, GeometryState s);

	@Override
	protected void cleanup(Application app) {}

}