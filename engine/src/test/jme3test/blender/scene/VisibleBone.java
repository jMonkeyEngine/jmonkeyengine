package jme3test.blender.scene;

import com.jme3.animation.Bone;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Line;
import com.jme3.scene.shape.Sphere;

/**
 * A simple class to create visualization of a skeleton.
 * @author Marcin Roguski
 */
public class VisibleBone extends Node {
	private Vector3f	globalPosition;

	public VisibleBone(Bone bone, Vector3f parentLocation, Quaternion parentRotation, AssetManager assetManager) {
		Material redMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		redMat.setColor("Color", ColorRGBA.Red);

		Material whiteMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		whiteMat.setColor("Color", ColorRGBA.White);

		Geometry g = new Geometry(bone.getName(), new Sphere(9, 9, 0.01f));
		globalPosition = bone.getLocalPosition().add(parentLocation);
		g.setLocalTranslation(globalPosition);
		g.setLocalRotation(bone.getLocalRotation().mult(parentRotation));
		g.setMaterial(redMat);
		this.attachChild(g);

		if(bone.getChildren() != null) {
			for(Bone child : bone.getChildren()) {
				VisibleBone vb = new VisibleBone(child, bone.getLocalPosition(), bone.getLocalRotation(), assetManager);
				this.attachChild(vb);
				Line line = new Line(globalPosition, vb.globalPosition);
				line.setLineWidth(2);
				Geometry geom = new Geometry("", line);
				geom.setMaterial(whiteMat);
				this.attachChild(geom);
			}
		}
	}
}