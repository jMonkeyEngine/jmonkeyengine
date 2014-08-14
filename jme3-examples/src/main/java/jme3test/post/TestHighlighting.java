/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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
package jme3test.post;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.HttpZipLocator;
import com.jme3.asset.plugins.ZipLocator;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.HighlightFilter;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.util.SkyFactory;
import java.io.File;
import jme3test.scene.*;

/**
 * Tests the highlighting of objects in a simple scene. Scene copied from
 * TestSceneLoading.
 *
 * @author Sebastian Weiss
 */
public class TestHighlighting extends SimpleApplication implements ActionListener {

	private static boolean useHttp = false;

	private Spatial scene;
	private Geometry highlighted;
	private boolean debug;
	private HighlightFilter filter;

	public static void main(String[] args) {

		TestHighlighting app = new TestHighlighting();
		app.start();
	}

	public void simpleInitApp() {
		this.flyCam.setMoveSpeed(10);
		this.flyCam.setDragToRotate(true);

		//add highlighting filter
		FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
		// fpp.setNumSamples(4);
		int numSamples = getContext().getSettings().getSamples();
		if (numSamples > 0) {
			fpp.setNumSamples(numSamples);
		}
		filter = new HighlightFilter();
		fpp.addFilter(filter);
		viewPort.addProcessor(fpp);

		// load sky
		rootNode.attachChild(SkyFactory.createSky(assetManager, "Textures/Sky/Bright/BrightSky.dds", false));

		File file = new File("wildhouse.zip");
		if (!file.exists()) {
			useHttp = true;
		}
        // create the geometry and attach it
		// load the level from zip or http zip
		if (useHttp) {
			assetManager.registerLocator("http://jmonkeyengine.googlecode.com/files/wildhouse.zip", HttpZipLocator.class);
		} else {
			assetManager.registerLocator("wildhouse.zip", ZipLocator.class);
		}
		scene = assetManager.loadModel("main.scene");
//		scene = new Node();
//        Box box1 = new Box(1,1,1);
//        Geometry blue = new Geometry("Box", box1);
//        blue.setLocalTranslation(new Vector3f(1,-1,1));
//        Material mat1 = new Material(assetManager, 
//                "Common/MatDefs/Misc/Unshaded.j3md");
//        mat1.setColor("Color", ColorRGBA.Blue);
//        blue.setMaterial(mat1);
//		((Node) scene).attachChild(blue);

		AmbientLight al = new AmbientLight();
		scene.addLight(al);

		DirectionalLight sun = new DirectionalLight();
		sun.setDirection(new Vector3f(0.69077975f, -0.6277887f, -0.35875428f).normalizeLocal());
		sun.setColor(ColorRGBA.White.clone().multLocal(2));
		scene.addLight(sun);

		rootNode.attachChild(scene);

		//initialize input
		inputManager.addMapping("clicked", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
		inputManager.addMapping("debug", new KeyTrigger(KeyInput.KEY_SPACE));
		inputManager.addMapping("+Brush", new KeyTrigger(KeyInput.KEY_ADD));
		inputManager.addMapping("-Brush", new KeyTrigger(KeyInput.KEY_SUBTRACT));
		inputManager.addMapping("enabled", new KeyTrigger(KeyInput.KEY_RETURN));
		inputManager.addMapping("color", new KeyTrigger(KeyInput.KEY_C));
		inputManager.addListener(this, "clicked", "debug", "+Brush", "-Brush", "enabled", "color");
		System.out.println("---------------------------------");
		System.out.println("Camera controls:");
		System.out.println(" WASD: move");
		System.out.println(" Mouse-Drag: rotate");
		System.out.println(" Mouse-Scroll: zoom");
		System.out.println("Filter controls:");
		System.out.println(" Left click: selection");
		System.out.println(" Space: toggle debug selection");
		System.out.println(" Numpad +/-: highlight size");
		System.out.println(" Return: toggle highlight filter");
		System.out.println(" c: change highlight color");
		System.out.println("---------------------------------");
	}

	public void onAction(String name, boolean isPressed, float tpf) {
		if (!isPressed) {
			return;
		}
		if ("debug".equals(name)) {
			debug = !debug;
			filter.debugEnabled(debug);
		} else if ("clicked".equals(name)) {
			//highlight selected element
			Vector3f origin = cam.getWorldCoordinates(inputManager.getCursorPosition(), 0.0f);
			Vector3f direction = cam.getWorldCoordinates(inputManager.getCursorPosition(), 0.3f);
			direction.subtractLocal(origin).normalizeLocal();

			Ray ray = new Ray(origin, direction);
			CollisionResults results = new CollisionResults();
			scene.collideWith(ray, results);

			if (highlighted != null) {
				highlighted.getMaterial().setBoolean("Highlighted", false);
				highlighted = null;
			}

			CollisionResult res = results.getClosestCollision();
			if (res != null) {
				highlighted = res.getGeometry();
				highlighted.getMaterial().setBoolean("Highlighted", true);
				System.out.println(highlighted + " selected");
			} else {
				System.out.println("nothing selected");
			}
		} else if ("+Brush".equals(name)) {
			filter.setBrushSize(Math.min(HighlightFilter.MAX_BRUSH_SIZE, filter.getBrushSize() + 1));
			System.out.println("brush size changed to " + filter.getBrushSize());
		} else if ("-Brush".equals(name)) {
			filter.setBrushSize(Math.max(HighlightFilter.MIN_BRUSH_SIZE, filter.getBrushSize() - 1));
			System.out.println("brush size changed to " + filter.getBrushSize());
		} else if ("enabled".equals(name)) {
			filter.setEnabled(!filter.isEnabled());
		} else if ("color".equals(name)) {
			filter.setHighlightColor(ColorRGBA.randomColor());
		}
	}
}
