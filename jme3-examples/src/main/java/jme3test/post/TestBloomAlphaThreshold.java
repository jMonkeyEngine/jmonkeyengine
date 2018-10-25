/*
 * Copyright (c) 2009-2015 jMonkeyEngine
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
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.post.filters.BloomFilter.GlowMode;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.WireFrustum;
import com.jme3.scene.shape.Box;
import com.jme3.util.SkyFactory;
import com.jme3.util.SkyFactory.EnvMapType;

public class TestBloomAlphaThreshold extends SimpleApplication
{

	float angle;
	Spatial lightMdl;
	Spatial teapot;
	Geometry frustumMdl;
	WireFrustum frustum;
	boolean active = true;
	FilterPostProcessor fpp;

	public static void main(String[] args)
	{
		TestBloomAlphaThreshold app = new TestBloomAlphaThreshold();
		app.start();
	}

	@Override
	public void simpleInitApp()
	{
		// put the camera in a bad position
		cam.setLocation(new Vector3f(-2.336393f, 11.91392f, -10));
		cam.setRotation(new Quaternion(0.23602544f, 0.11321983f, -0.027698677f, 0.96473104f));
		// cam.setFrustumFar(1000);

		Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");

		mat.setFloat("Shininess", 15f);
		mat.setBoolean("UseMaterialColors", true);
		mat.setColor("Ambient", ColorRGBA.Yellow.mult(0.2f));
		mat.setColor("Diffuse", ColorRGBA.Yellow.mult(0.2f));
		mat.setColor("Specular", ColorRGBA.Yellow.mult(0.8f));
		mat.setColor("GlowColor", ColorRGBA.Green);

		Material matSoil = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
		matSoil.setFloat("Shininess", 15f);
		matSoil.setBoolean("UseMaterialColors", true);
		matSoil.setColor("Ambient", ColorRGBA.Gray);
		matSoil.setColor("Diffuse", ColorRGBA.Black);
		matSoil.setColor("Specular", ColorRGBA.Gray);

		teapot = assetManager.loadModel("Models/Teapot/Teapot.obj");
		teapot.setLocalTranslation(0, 0, 10);

		teapot.setMaterial(mat);
		teapot.setShadowMode(ShadowMode.CastAndReceive);
		teapot.setLocalScale(10.0f);
		rootNode.attachChild(teapot);

                Vector3f boxMin1 = new Vector3f(-800f, -23f, -150f);
                Vector3f boxMax1 = new Vector3f(800f, 3f, 1250f);
                Box boxMesh1 = new Box(boxMin1, boxMax1);
		Geometry soil = new Geometry("soil", boxMesh1);
		soil.setMaterial(matSoil);
		soil.setShadowMode(ShadowMode.CastAndReceive);
		rootNode.attachChild(soil);

		Material matBox = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		matBox.setTexture("ColorMap", assetManager.loadTexture("Textures/ColoredTex/Monkey.png"));
		matBox.setFloat("AlphaDiscardThreshold", 0.5f);
    
                Vector3f boxMin2 = new Vector3f(-5.5f, 8f, -4f);
                Vector3f boxMax2 = new Vector3f(-1.5f, 12f, 0f);
                Box boxMesh2 = new Box(boxMin2, boxMax2);
		Geometry box = new Geometry("box", boxMesh2);
		box.setMaterial(matBox);
                box.setQueueBucket(RenderQueue.Bucket.Translucent);
		// box.setShadowMode(ShadowMode.CastAndReceive);
		rootNode.attachChild(box);

		DirectionalLight light = new DirectionalLight();
		light.setDirection(new Vector3f(-1, -1, -1).normalizeLocal());
		light.setColor(ColorRGBA.White.mult(1.5f));
		rootNode.addLight(light);

		// load sky
		Spatial sky = SkyFactory.createSky(assetManager, 
                        "Textures/Sky/Bright/FullskiesBlueClear03.dds",
                        EnvMapType.CubeMap);
		sky.setCullHint(Spatial.CullHint.Never);
		rootNode.attachChild(sky);

		fpp = new FilterPostProcessor(assetManager);
		int numSamples = getContext().getSettings().getSamples();
		if (numSamples > 0)
		{
			fpp.setNumSamples(numSamples);
		}

		BloomFilter bloom = new BloomFilter(GlowMode.Objects);
		bloom.setDownSamplingFactor(2);
		bloom.setBlurScale(1.37f);
		bloom.setExposurePower(3.30f);
		bloom.setExposureCutOff(0.2f);
		bloom.setBloomIntensity(2.45f);
		BloomUI ui = new BloomUI(inputManager, bloom);

		viewPort.addProcessor(fpp);
		fpp.addFilter(bloom);
		initInputs();

	}

	private void initInputs()
	{
		inputManager.addMapping("toggle", new KeyTrigger(KeyInput.KEY_SPACE));

		ActionListener acl = new ActionListener()
		{

			@Override
			public void onAction(String name, boolean keyPressed, float tpf)
			{
				if (name.equals("toggle") && keyPressed)
				{
					if (active)
					{
						active = false;
						viewPort.removeProcessor(fpp);
					}
					else
					{
						active = true;
						viewPort.addProcessor(fpp);
					}
				}
			}
		};

		inputManager.addListener(acl, "toggle");

	}

}
