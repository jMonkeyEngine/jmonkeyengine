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
package jme3test.light;

import com.jme3.app.BasicProfilerState;
import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.LightList;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.material.Material;
import com.jme3.material.TechniqueDef;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.LightNode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.shape.Box;
import com.jme3.util.MaterialDebugAppState;

public class TestManyLightsSingle extends SimpleApplication {

    public static void main(String[] args) {
        TestManyLightsSingle app = new TestManyLightsSingle();
        app.start();
    }

    /**
     * Switch mode with space bar at run time
     */
    TechniqueDef.LightMode lm = TechniqueDef.LightMode.SinglePass;

    @Override
    public void simpleInitApp() {
        renderManager.setPreferredLightMode(lm);
        renderManager.setSinglePassLightBatchSize(6);

        flyCam.setMoveSpeed(10);

        Node scene = (Node) assetManager.loadModel("Scenes/ManyLights/Main.scene");
        rootNode.attachChild(scene);
        Node n = (Node) rootNode.getChild(0);
        final LightList lightList = n.getWorldLightList();
        final Geometry g = (Geometry) n.getChild("Grid-geom-1");

        g.getMaterial().setColor("Ambient", new ColorRGBA(0.2f, 0.2f, 0.2f, 1f));

        /* A colored lit cube. Needs light source! */
        Box boxMesh = new Box(1f, 1f, 1f);
        final Geometry boxGeo = new Geometry("Colored Box", boxMesh);
        Material boxMat = g.getMaterial().clone();
        boxMat.clearParam("DiffuseMap");
        boxMat.setBoolean("UseMaterialColors", true);
        boxMat.setColor("Ambient", new ColorRGBA(0.2f, 0.2f, 0.2f, 1f));
        boxMat.setColor("Diffuse", ColorRGBA.Blue);
        boxGeo.setMaterial(boxMat);

        final Node cubeNodes = new Node();
        n.attachChild(cubeNodes);
        int nb = 0;
        for (Light light : lightList) {
            nb++;
            PointLight p = (PointLight) light;
            if (nb > 60) {
                n.removeLight(light);
            } else {

                LightNode ln = new LightNode("l", light);
                n.attachChild(ln);
                ln.setLocalTranslation(p.getPosition());
                int rand = FastMath.nextRandomInt(0, 3);
                switch (rand) {
                    case 0:
                        light.setColor(ColorRGBA.Red);
                        //   ln.addControl(new MoveControl(5f));
                        break;
                    case 1:
                        light.setColor(ColorRGBA.Yellow);
                        //    ln.addControl(new MoveControl(5f));
                        break;
                    case 2:
                        light.setColor(ColorRGBA.Green);
                        //ln.addControl(new MoveControl(-5f));
                        break;
                    case 3:
                        light.setColor(ColorRGBA.Orange);
                        //ln.addControl(new MoveControl(-5f));
                        break;
                }
            }
            Geometry b = boxGeo.clone(false);
            cubeNodes.attachChild(b);
            b.setLocalTranslation(p.getPosition().x, 2, p.getPosition().z);

        }


//        cam.setLocation(new Vector3f(3.1893547f, 17.977385f, 30.8378f));
//        cam.setRotation(new Quaternion(0.14317635f, 0.82302624f, -0.23777823f, 0.49557027f));

        cam.setLocation(new Vector3f(-1.8901939f, 29.34097f, 73.07533f));
        cam.setRotation(new Quaternion(0.0021000702f, 0.971012f, -0.23886925f, 0.008527749f));


        BasicProfilerState profiler = new BasicProfilerState(true);
        profiler.setGraphScale(1000f);

        //  getStateManager().attach(profiler);
//        guiNode.setCullHint(CullHint.Always);


        flyCam.setDragToRotate(true);
        flyCam.setMoveSpeed(50);

        final MaterialDebugAppState debug = new MaterialDebugAppState();
        stateManager.attach(debug);

        inputManager.addListener(new ActionListener() {
            public void onAction(String name, boolean isPressed, float tpf) {
                if (name.equals("toggle") && isPressed) {
                    if (lm == TechniqueDef.LightMode.SinglePass) {
                        lm = TechniqueDef.LightMode.MultiPass;
                        helloText.setText("(Multi pass)");
                    } else {
                        lm = TechniqueDef.LightMode.SinglePass;
                        helloText.setText("(Single pass) nb lights per batch : " + renderManager.getSinglePassLightBatchSize());
                    }
                    renderManager.setPreferredLightMode(lm);
                    reloadScene(g, boxGeo, cubeNodes);
                }
                if (name.equals("lightsUp") && isPressed) {
                    renderManager.setSinglePassLightBatchSize(renderManager.getSinglePassLightBatchSize() + 1);
                    helloText.setText("(Single pass) nb lights per batch : " + renderManager.getSinglePassLightBatchSize());
                }
                if (name.equals("lightsDown") && isPressed) {
                    renderManager.setSinglePassLightBatchSize(renderManager.getSinglePassLightBatchSize() - 1);
                    helloText.setText("(Single pass) nb lights per batch : " + renderManager.getSinglePassLightBatchSize());
                }
                if (name.equals("toggleOnOff") && isPressed) {
                    for (final Light light : lightList) {
                        if (light instanceof AmbientLight) {
                            continue;
                        }

                        light.setEnabled(!light.isEnabled());
                    }
                }
            }
        }, "toggle", "lightsUp", "lightsDown", "toggleOnOff");

        inputManager.addMapping("toggle", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("lightsUp", new KeyTrigger(KeyInput.KEY_UP));
        inputManager.addMapping("lightsDown", new KeyTrigger(KeyInput.KEY_DOWN));
        inputManager.addMapping("toggleOnOff", new KeyTrigger(KeyInput.KEY_L));


        SpotLight spot = new SpotLight();
        spot.setDirection(new Vector3f(-1f, -1f, -1f).normalizeLocal());
        spot.setColor(ColorRGBA.Blue.mult(5));
        spot.setSpotOuterAngle(FastMath.DEG_TO_RAD * 20);
        spot.setSpotInnerAngle(FastMath.DEG_TO_RAD * 5);
        spot.setPosition(new Vector3f(10, 10, 20));
        rootNode.addLight(spot);

        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-1, -1, 1));
        rootNode.addLight(dl);

        AmbientLight al = new AmbientLight();
        al.setColor(new ColorRGBA(0.2f, 0.2f, 0.2f, 1f));
        rootNode.addLight(al);


        /**
         * Write text on the screen (HUD)
         */
        guiNode.detachAllChildren();
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        helloText = new BitmapText(guiFont, false);
        helloText.setSize(guiFont.getCharSet().getRenderedSize());
        helloText.setText("(Single pass) nb lights per batch : " + renderManager.getSinglePassLightBatchSize());
        helloText.setLocalTranslation(300, helloText.getLineHeight(), 0);
        guiNode.attachChild(helloText);
    }

    protected void reloadScene(Geometry g, Geometry boxGeo, Node cubeNodes) {
        MaterialDebugAppState debug = stateManager.getState(MaterialDebugAppState.class);
        Material m = debug.reloadMaterial(g.getMaterial());
        if (m != null) {
            g.setMaterial(m);
        }
        m = debug.reloadMaterial(boxGeo.getMaterial());
        if (m != null) {
            cubeNodes.setMaterial(m);
        }
    }

    BitmapText helloText;
    long time;
    long nbFrames;
    long startTime = 0;

    @Override
    public void simpleUpdate(float tpf) {
//        if (nbFrames == 4000) {
//            startTime = System.nanoTime();
//        }
//        if (nbFrames > 4000) {
//            time = System.nanoTime();
//            float average = ((float) time - (float) startTime) / ((float) nbFrames - 4000f);
//            helloText.setText("Average = " + average);
//        }
//        nbFrames++;
    }

    class MoveControl extends AbstractControl {

        float direction;
        Vector3f origPos = new Vector3f();

        public MoveControl(float direction) {
            this.direction = direction;
        }

        @Override
        public void setSpatial(Spatial spatial) {
            super.setSpatial(spatial); //To change body of generated methods, choose Tools | Templates.
            origPos.set(spatial.getLocalTranslation());
        }
        float time = 0;

        @Override
        protected void controlUpdate(float tpf) {
            time += tpf;
            spatial.setLocalTranslation(origPos.x + FastMath.cos(time) * direction, origPos.y, origPos.z + FastMath.sin(time) * direction);
        }

        @Override
        protected void controlRender(RenderManager rm, ViewPort vp) {
        }
    }
}