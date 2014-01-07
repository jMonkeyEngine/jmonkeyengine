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
package jme3test.animation;

import com.jme3.animation.AnimControl;
import com.jme3.animation.AnimationFactory;
import com.jme3.animation.LoopMode;
import com.jme3.app.DebugKeysAppState;
import com.jme3.app.FlyCamAppState;
import com.jme3.app.ResetStatsState;
import com.jme3.app.SimpleApplication;
import com.jme3.app.StatsAppState;
import com.jme3.cinematic.Cinematic;
import com.jme3.cinematic.MotionPath;
import com.jme3.cinematic.PlayState;
import com.jme3.cinematic.events.AnimationEvent;
import com.jme3.cinematic.events.MotionEvent;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.FXAAFilter;
import com.jme3.post.ssao.SSAOFilter;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.shadow.SpotLightShadowRenderer;

/**
 *
 * @author Nehon
 */
public class TestJaime  extends SimpleApplication {

   
    Cinematic cinematic;
    
    public static void main(String... argv){
        TestJaime app = new TestJaime();
        app.start();
    }

    
    
    @Override
    public void simpleInitApp() {
        stateManager.detach(stateManager.getState(FlyCamAppState.class));
        stateManager.detach(stateManager.getState(ResetStatsState.class));
        stateManager.detach(stateManager.getState(DebugKeysAppState.class));
        stateManager.detach(stateManager.getState(StatsAppState.class));
        final Node jaime = LoadModel();
        
        setupLights();        
        setupCamera();
        setupFloor();
        setupCinematic(jaime);
        setupInput();
    }
    
    public Node LoadModel() {
        Node jaime = (Node)assetManager.loadModel("Models/Jaime/Jaime.j3o");
        jaime.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        rootNode.attachChild(jaime);
        return jaime;
    }

    public void setupLights() {
        AmbientLight al = new AmbientLight();
        al.setColor(new ColorRGBA(0.1f, 0.1f, 0.1f, 1));
        rootNode.addLight(al);
        
        SpotLight sl = new SpotLight();
        sl.setColor(ColorRGBA.White.mult(1.0f));
        sl.setPosition(new Vector3f(1.2074411f, 10.6868908f, 4.1489987f));
        sl.setDirection(sl.getPosition().mult(-1)); 
        sl.setSpotOuterAngle(0.1f);
        sl.setSpotInnerAngle(0.004f);      
        rootNode.addLight(sl);
        
        //pointlight to fake indirect light coming from the ground
        PointLight pl = new PointLight();
        pl.setColor(ColorRGBA.White.mult(1.5f));
        pl.setPosition(new Vector3f(0, 0, 1));
        pl.setRadius(2);
        rootNode.addLight(pl);
        
        SpotLightShadowRenderer shadows = new SpotLightShadowRenderer(assetManager, 1024);
        shadows.setLight(sl);
        shadows.setShadowIntensity(0.3f);
        shadows.setEdgeFilteringMode(EdgeFilteringMode.PCF8);
        viewPort.addProcessor(shadows);

        
        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        SSAOFilter filter = new SSAOFilter(0.10997847f,0.440001f,0.39999998f,-0.008000026f);;
        fpp.addFilter(filter);
        fpp.addFilter(new FXAAFilter());
        fpp.addFilter(new FXAAFilter());     
        
        viewPort.addProcessor(fpp);
    }

    public void setupCamera() {
         flyCam.setEnabled(false);
    }

    public void setupCinematic(final Node jaime) {
        cinematic = new Cinematic(rootNode, 60);
        stateManager.attach(cinematic);
        
        jaime.move(0, 0, -3);
        AnimationFactory af = new AnimationFactory(0.7f, "JumpForward");
        af.addTimeTranslation(0, new Vector3f(0, 0, -3));
        af.addTimeTranslation(0.35f, new Vector3f(0, 1, -1.5f));
        af.addTimeTranslation(0.7f, new Vector3f(0, 0, 0));
        jaime.getControl(AnimControl.class).addAnim(af.buildAnimation());
   
        cinematic.enqueueCinematicEvent(new AnimationEvent(jaime, "Idle",3, LoopMode.DontLoop));
        float jumpStart = cinematic.enqueueCinematicEvent(new AnimationEvent(jaime, "JumpStart", LoopMode.DontLoop));
        cinematic.addCinematicEvent(jumpStart+0.2f, new AnimationEvent(jaime, "JumpForward", LoopMode.DontLoop,1));        
        cinematic.enqueueCinematicEvent( new AnimationEvent(jaime, "JumpEnd", LoopMode.DontLoop));                
        cinematic.enqueueCinematicEvent( new AnimationEvent(jaime, "Punches", LoopMode.DontLoop));
        cinematic.enqueueCinematicEvent( new AnimationEvent(jaime, "SideKick", LoopMode.DontLoop));        
        float camStart = cinematic.enqueueCinematicEvent( new AnimationEvent(jaime, "Taunt", LoopMode.DontLoop));
        cinematic.enqueueCinematicEvent( new AnimationEvent(jaime, "Idle",1, LoopMode.DontLoop));
        cinematic.enqueueCinematicEvent( new AnimationEvent(jaime, "Wave", LoopMode.DontLoop));
        cinematic.enqueueCinematicEvent( new AnimationEvent(jaime, "Idle", LoopMode.DontLoop));        
        
        CameraNode camNode = cinematic.bindCamera("cam", cam);
        camNode.setLocalTranslation(new Vector3f(1.1f, 1.2f, 2.9f));
        camNode.lookAt(new Vector3f(0, 0.5f, 0), Vector3f.UNIT_Y);
        
        MotionPath path = new MotionPath();
        path.addWayPoint(new Vector3f(1.1f, 1.2f, 2.9f));
        path.addWayPoint(new Vector3f(0f, 1.2f, 3.0f));
        path.addWayPoint(new Vector3f(-1.1f, 1.2f, 2.9f));        
        path.enableDebugShape(assetManager, rootNode);
        path.setCurveTension(0.8f);
        
        MotionEvent camMotion = new MotionEvent(camNode, path,6);
        camMotion.setDirectionType(MotionEvent.Direction.LookAt);
        camMotion.setLookAt(new Vector3f(0, 0.5f, 0), Vector3f.UNIT_Y);
        cinematic.addCinematicEvent(camStart, camMotion);
        cinematic.activateCamera(0, "cam");
       
        
        cinematic.fitDuration();
        cinematic.setSpeed(1.2f);
        cinematic.setLoopMode(LoopMode.Loop);
        cinematic.play();
    }

    public void setupFloor() {
        Quad q = new Quad(20, 20);
       q.scaleTextureCoordinates(Vector2f.UNIT_XY.mult(10));
       Geometry geom = new Geometry("floor", q);
       Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
       mat.setColor("Diffuse", ColorRGBA.White);
       mat.setColor("Specular", ColorRGBA.White);
       mat.setColor("Ambient", ColorRGBA.Black);
       mat.setBoolean("UseMaterialColors", true);
       mat.setFloat("Shininess", 0);
       geom.setMaterial(mat);

       geom.rotate(-FastMath.HALF_PI, 0, 0);
       geom.center();
       geom.setShadowMode(RenderQueue.ShadowMode.Receive);
       rootNode.attachChild(geom);
    }

    public void setupInput() {
        inputManager.addMapping("start", new KeyTrigger(KeyInput.KEY_PAUSE));
        inputManager.addListener(new ActionListener() {

            public void onAction(String name, boolean isPressed, float tpf) {
                if(name.equals("start") && isPressed){
                    if(cinematic.getPlayState() != PlayState.Playing){                                                
                        cinematic.play();
                    }else{
                        cinematic.pause();
                    }
                }
            }
        }, "start");
    }

   
    
}
