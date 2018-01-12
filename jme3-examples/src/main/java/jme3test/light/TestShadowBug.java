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
package jme3test.light;


import com.jme3.app.SimpleApplication;
import com.jme3.light.DirectionalLight;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.shadow.PointLightShadowRenderer;
import com.jme3.shadow.SpotLightShadowRenderer;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;


public class TestShadowBug extends SimpleApplication {
  public static void main(String[] args) {
    TestShadowBug app = new TestShadowBug();
    app.start();
  }

  @Override
  public void simpleInitApp() {
    flyCam.setMoveSpeed(100f);
    rootNode.attachChild(makeFloor());

    Node characters = new Node("Characters");
    characters.setShadowMode(ShadowMode.Cast);
    rootNode.attachChild(characters);

    Spatial golem = assetManager.loadModel("Models/Oto/Oto.mesh.xml");
    golem.scale(0.5f);
    golem.setLocalTranslation(200.0f, -6f, 200f);
    golem.setShadowMode(ShadowMode.CastAndReceive);
    characters.attachChild(golem);

    DirectionalLight sun = new DirectionalLight();
    sun.setDirection(new Vector3f(-1f, -1f, 1f));
    sun.setColor(ColorRGBA.White.mult(1.3f));
    rootNode.addLight(sun);
    characters.addLight(sun);

    SpotLight spot = new SpotLight();
    spot.setSpotRange(13f);                           // distance
    spot.setSpotInnerAngle(15f * FastMath.DEG_TO_RAD); // inner light cone (central beam)
    spot.setSpotOuterAngle(20f * FastMath.DEG_TO_RAD); // outer light cone (edge of the light)
    spot.setColor(ColorRGBA.White.mult(1.3f));         // light color
    spot.setPosition(new Vector3f(192.0f, -1f, 192f));
    spot.setDirection(new Vector3f(1, -0.5f, 1));
    rootNode.addLight(spot);

    PointLight lamp_light = new PointLight();
    lamp_light.setColor(ColorRGBA.Yellow);
    lamp_light.setRadius(20f);
    lamp_light.setPosition(new Vector3f(210.0f, 0f, 210f));
    rootNode.addLight(lamp_light);

    SpotLightShadowRenderer slsr = new SpotLightShadowRenderer(assetManager, 512);
    slsr.setLight(spot);
    slsr.setEdgeFilteringMode(EdgeFilteringMode.Nearest);
    slsr.setShadowIntensity(0.6f);
    viewPort.addProcessor(slsr);

    PointLightShadowRenderer plsr = new PointLightShadowRenderer(assetManager, 512);
    plsr.setLight(lamp_light);
    plsr.setShadowIntensity(0.6f);
    plsr.setEdgeFilteringMode(EdgeFilteringMode.Nearest);
    viewPort.addProcessor(plsr);

    viewPort.getCamera().setLocation(new Vector3f(192.0f, 10f, 192f));
    float[] angles = new float[]{3.14f/2, 3.14f/2, 0};
    viewPort.getCamera().setRotation(new Quaternion(angles));
  }

  protected Geometry makeFloor() {
    Box box = new Box(220, .2f, 220);
    box.scaleTextureCoordinates(new Vector2f(10, 10));
    Geometry floor = new Geometry("the Floor", box);
    floor.setLocalTranslation(200, -9, 200);
    Material matGroundL = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
    Texture grass = assetManager.loadTexture("Textures/Terrain/splat/grass.jpg");
    grass.setWrap(WrapMode.Repeat);
    matGroundL.setTexture("DiffuseMap", grass);
    floor.setMaterial(matGroundL);
    floor.setShadowMode(ShadowMode.CastAndReceive);
    return floor;
  }
}