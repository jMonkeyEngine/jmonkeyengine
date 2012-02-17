
/*
 * Android 2.2+ SimpleTextured test.
 *
 * created: Mon Nov  8 00:08:22 EST 2010
 */

package jme3test.android;


import com.jme3.app.SimpleApplication;
import com.jme3.asset.TextureKey;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.texture.Texture;
import com.jme3.util.TangentBinormalGenerator;


public class SimpleTexturedTest extends SimpleApplication {

	private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(SimpleTexturedTest.class.getName());


	private Node spheresContainer = new Node("spheres-container");


	private boolean lightingEnabled = true;
	private boolean texturedEnabled = true;
	private boolean spheres = true;

	@Override
	public void simpleInitApp() {
	    
	    //flyCam.setRotationSpeed(0.01f);


		Mesh shapeSphere = null;
		Mesh shapeBox = null;


		shapeSphere = new Sphere(16, 16, .5f);
		shapeBox = new Box(Vector3f.ZERO, 0.3f, 0.3f, 0.3f);


	//	ModelConverter.optimize(geom);

		Texture texture = assetManager.loadTexture(new TextureKey("Interface/Logo/Monkey.jpg"));
		Texture textureMonkey = assetManager.loadTexture(new TextureKey("Interface/Logo/Monkey.jpg"));

		Material material = null;
		Material materialMonkey = null;

		if (texturedEnabled) {
			if (lightingEnabled) {
				material = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
				material.setBoolean("VertexLighting", true);
				material.setFloat("Shininess", 127);
				material.setBoolean("LowQuality", true);
				material.setTexture("DiffuseMap", texture);
				
				materialMonkey = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
				materialMonkey.setBoolean("VertexLighting", true);
				materialMonkey.setFloat("Shininess", 127);
				materialMonkey.setBoolean("LowQuality", true);
				materialMonkey.setTexture("DiffuseMap", textureMonkey);
				
			} else {
				material = new Material(assetManager, "Common/MatDefs/Misc/SimpleTextured.j3md");
				material.setTexture("ColorMap", texture);
				
				materialMonkey = new Material(assetManager, "Common/MatDefs/Misc/SimpleTextured.j3md");
				materialMonkey.setTexture("ColorMap", textureMonkey);
			}
		} else {
			material = new Material(assetManager, "Common/MatDefs/Misc/SolidColor.j3md");
			material.setColor("Color", ColorRGBA.Red);
			materialMonkey = new Material(assetManager, "Common/MatDefs/Misc/SolidColor.j3md");
			materialMonkey.setColor("Color", ColorRGBA.Red);			
		}

		TangentBinormalGenerator.generate(shapeSphere);
		TangentBinormalGenerator.generate(shapeBox);

		int iFlipper = 0;
		for (int y = -1; y < 2; y++) {
			for (int x = -1; x < 2; x++){
				Geometry geomClone = null;
				
				//iFlipper++;
				if (iFlipper % 2 == 0)
				{
					geomClone = new Geometry("geometry-" + y + "-" + x, shapeBox);
				}
				else
				{
					geomClone = new Geometry("geometry-" + y + "-" + x, shapeSphere);
				}
				if (iFlipper % 3 == 0)
				{
					geomClone.setMaterial(materialMonkey);
				}
				else
				{
					geomClone.setMaterial(material);
				}
				geomClone.setLocalTranslation(x, y, 0);
                
//				Transform t = geom.getLocalTransform().clone();
//				Transform t2 = geomClone.getLocalTransform().clone();
//				t.combineWithParent(t2);
//				geomClone.setLocalTransform(t);

				spheresContainer.attachChild(geomClone); 
			}
		}

		spheresContainer.setLocalTranslation(new Vector3f(0, 0, -4f));
		spheresContainer.setLocalScale(2.0f);

		rootNode.attachChild(spheresContainer);

		PointLight pointLight = new PointLight();

		pointLight.setColor(new ColorRGBA(0.7f, 0.7f, 1.0f, 1.0f));

		pointLight.setPosition(new Vector3f(0f, 0f, 0f));
		pointLight.setRadius(8);

		rootNode.addLight(pointLight);
	}

	@Override
	public void simpleUpdate(float tpf) {

		// secondCounter has been removed from SimpleApplication
                //if (secondCounter == 0)
		//	logger.info("Frames per second: " + timer.getFrameRate());

		spheresContainer.rotate(0.2f * tpf, 0.4f * tpf, 0.8f * tpf);
	}

}

