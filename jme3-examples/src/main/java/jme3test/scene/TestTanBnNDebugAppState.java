package jme3test.scene;

import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.debug.TanBnNDebugAppState;
import com.jme3.scene.shape.Sphere;
import com.jme3.terrain.noise.basis.ImprovedNoise;
import com.jme3.util.TangentBinormalGenerator;

/**
 * @author Riccardo Balbo
 */
public class TestTanBnNDebugAppState extends SimpleApplication implements ActionListener{
	private Geometry geom;

	public static void main(String[] args) {
		TestTanBnNDebugAppState app=new TestTanBnNDebugAppState();
		app.start();
	}

	@Override
	public void onAction(String name, boolean isPressed, float tpf) {
		if(name.equals("nextStep")&&isPressed){
			nextStep();
		}
	}

	@Override
	public void simpleInitApp() {

		System.out.println("Bindings: SPACE=nextStep");

		generateGeometry();
		applyAppState();

		inputManager.addMapping("nextStep",new KeyTrigger(KeyInput.KEY_SPACE));
		inputManager.addListener(this,"nextStep");

	}

	private void generateGeometry() {
		Sphere sp=new Sphere(10,10,1f);
		geom=new Geometry("",sp);
		TangentBinormalGenerator.generate(geom);

		Material mat=new Material(assetManager,"Common/MatDefs/Misc/Unshaded.j3md");
		mat.setColor("Color",ColorRGBA.DarkGray);
		geom.setMaterial(mat);
		rootNode.attachChild(geom);

	}

	public void applyAppState() {
		TanBnNDebugAppState appstate=new TanBnNDebugAppState(this);
		stateManager.attach(appstate);
	}

	int step=0;

	private void nextStep() {
		switch(step){
			case 0:
				transform(geom);
				break;
			case 1:
				translate(geom);
				break;
			case 2:
				delete(geom);
				break;
			default: // reset test
				generateGeometry();
				step=-1;
		}
		step++;
	}

	private void transform(Geometry g) {
		Mesh m=g.getMesh();
		VertexBuffer posb=m.getBuffer(Type.Position);
		int elements=posb.getNumElements();
		for(int i=0;i<elements;i++){
			Vector3f p=new Vector3f((float)posb.getElementComponent(i,0),(float)posb.getElementComponent(i,1),(float)posb.getElementComponent(i,2));

			p.addLocal(new Vector3f(1,1,1).multLocal(ImprovedNoise.noise(p.x,p.y,p.z)));

			posb.setElementComponent(i,0,p.x);
			posb.setElementComponent(i,0,p.y);
			posb.setElementComponent(i,0,p.z);
		}
		posb.setUpdateNeeded();
		TangentBinormalGenerator.generate(geom);

	}

	private void translate(Geometry g) {
		g.setLocalTranslation(2,-1,0);
		g.setLocalScale(0.5f);
	}

	private void delete(Geometry g) {
		g.removeFromParent();
	}

}
