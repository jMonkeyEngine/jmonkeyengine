/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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
package jme3test.blender;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import jme3test.blender.config.ConfigDialog;
import jme3test.blender.config.IConfigExecutable;
import jme3test.blender.scene.Pivot;

import com.jme3.animation.AnimControl;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetInfo;
import com.jme3.asset.BlenderKey;
import com.jme3.asset.BlenderKey.LoadingResults;
import com.jme3.asset.ModelKey;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.blender.BlenderLoader;
import com.jme3.scene.plugins.blender.BlenderModelLoader;
import com.jme3.texture.plugins.AWTLoader;

/**
 * This class allow to manually test the blender loader.
 * @author Marcin Roguski (Kaelthas)
 */
public class ManualBlenderTester extends SimpleApplication {
	private static final Logger	LOGGER	= Logger.getLogger(ManualBlenderTester.class.getName());
	private ModelKey modelKey;//the key that holds the test file configuration
	private final boolean debug;
	
	/**
	 * Starting method
	 * @param args input parameters; the following options can be passed to the application:
	 * <li> -debug   : this one indicates if the application runs in debug or not (it is used under linux
	 * 				   in order to enable the mouse in debug mode since linuxes tend not to give the cursor back
	 * 				   to eclipse)
	 */
	public static void main(String[] args) {
		//veryfying if the application is in debug mode
		boolean debug = false;
		for(String arg : args) {
			if("-debug".equalsIgnoreCase(arg)) {
				debug = true;
				break;
			}
		}
		final boolean debugMode = debug;
		//running the application
		new ConfigDialog("./src/test-data/Blender", new IConfigExecutable() {
			@Override
			public void execute(ModelKey modelKey, Level logLevel) {
				new ManualBlenderTester(modelKey, logLevel, debugMode).start();
			}
		});
	}
	
	/**
	 * Constructor stores the given key and disables the settings screen.
	 * @param modelKey the key to be stored
	 * @param logLevel the jme logger log level
	 * @param debug variable that indicates if the application runs in debug mode
	 * (this is required on linux to show release the mouse to be used in debug mode)
	 */
	public ManualBlenderTester(ModelKey modelKey, Level logLevel, boolean debug) {
		this.debug = debug;
		Logger.getLogger("com.jme3").setLevel(logLevel);
		this.modelKey = modelKey;
		this.showSettings = false;
	}

	@Override
	public void simpleInitApp() {
		if(debug) {
			mouseInput.setCursorVisible(true);
		}
		assetManager.registerLocator(".", FileLocator.class);
		assetManager.registerLoader(BlenderLoader.class, "blend");
		assetManager.registerLoader(AWTLoader.class, "png");

		viewPort.setBackgroundColor(ColorRGBA.Gray);

		flyCam.setMoveSpeed(20);
		cam.setFrustumFar(1000.0f);
		cam.setFrustumNear(1.0f);
		AssetInfo ai = new AssetInfo(assetManager, modelKey) {
			@Override
			public InputStream openStream() {
				try {
					return new FileInputStream(this.key.getName());
				} catch(FileNotFoundException e) {
					LOGGER.log(Level.SEVERE, e.getMessage(), e);
					return null;
				}
			}
		};
		rootNode.attachChild(new Pivot(assetManager));
		if(modelKey instanceof BlenderKey) {
			Node blenderModel = this.testBlenderLoader(ai);
			Map<String, Map<String, int[]>> animations = ((BlenderKey) modelKey).getAnimations();
			//setting the first animation as active
			if(((BlenderKey) modelKey).getAnimations()!=null) {
				for(Entry<String, Map<String, int[]>> animEntry : animations.entrySet()) {
					for(Entry<String, int[]> anim : animEntry.getValue().entrySet()) {
						Spatial animatedSpatial = this.findNode(blenderModel, animEntry.getKey());
						animatedSpatial.getControl(AnimControl.class).createChannel().setAnim(anim.getKey());
						break;
					}
					break;
				}
			}
		} else {
			this.testBlenderModelLoader(ai);
		}
		
		DirectionalLight sun = new DirectionalLight();
		sun.setDirection(new Vector3f(0, -10, 0).normalizeLocal());
		sun.setColor(ColorRGBA.White);
		rootNode.addLight(sun);
	}
	
	/**
	 * This method finds a node of a given name.
	 * @param rootNode the root node to search
	 * @param name the name of the searched node
	 * @return the found node or null
	 */
	private Spatial findNode(Node rootNode, String name) {
		if(name.equals(rootNode.getName())) {
			return rootNode;
		}
		return rootNode.getChild(name);
	}

	/**
	 * This method loads the model using blenderLoader.
	 * @param assetInfo
	 *        the asset info
	 * @return the loaded model
	 */
	private Node testBlenderLoader(AssetInfo assetInfo) {
		Node blenderModel = null;
		BlenderLoader blenderLoader = new BlenderLoader();
		try {
			LoadingResults loadingResults = blenderLoader.load(assetInfo);
			for(Node object : loadingResults.getObjects()) {
				this.rootNode.attachChild(object);
				blenderModel = object;
			}
			for(Light light : loadingResults.getLights()) {
				this.rootNode.addLight(light);
			}
			for(Camera camera : loadingResults.getCameras()) {
				LOGGER.info(camera.toString());
			}
		} catch(IOException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
		return blenderModel;
	}

	/**
	 * This method loads the model using blenderModelLoader.
	 * @param assetInfo
	 *        the asset info
	 * @return the loaded model
	 */
	private Node testBlenderModelLoader(AssetInfo assetInfo) {
		BlenderModelLoader blenderLoader = new BlenderModelLoader();
		try {
			Spatial loadingResults = blenderLoader.load(assetInfo);
			this.rootNode.attachChild(loadingResults);
			if(loadingResults instanceof Node) {
				return (Node)loadingResults;
			}
		} catch(IOException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
		return null;
	}
}
