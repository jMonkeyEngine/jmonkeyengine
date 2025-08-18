/*
 * Copyright (c) 2009-2025 jMonkeyEngine
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
package jme3test.stress;

import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.Trigger;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.control.LodControl;

public class TestLodStress extends SimpleApplication implements ActionListener {

    public static void main(String[] args) {
        TestLodStress app = new TestLodStress();
        app.setPauseOnLostFocus(false);
        app.start();
    }

    private Material lightMaterial;
    private final Node debugNode = new Node("DebugNode");

    @Override
    public void simpleInitApp() {

        configureCamera();

        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-1, -1, -1).normalizeLocal());
        rootNode.addLight(dl);

        Node teapotNode = (Node) assetManager.loadModel("Models/Teapot/Teapot.mesh.xml");
        Geometry teapot = (Geometry) teapotNode.getChild(0);

        lightMaterial = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        lightMaterial.setFloat("Shininess", 16f);
        lightMaterial.setBoolean("VertexLighting", true);
        lightMaterial.getAdditionalRenderState().setWireframe(true);
        teapot.setMaterial(lightMaterial);

        boolean cloneMaterial = false;
        for (int y = -10; y < 10; y++) {
            for (int x = -10; x < 10; x++) {
                Geometry geo = teapot.clone(cloneMaterial);
                geo.setLocalTranslation(x * .5f, 0, y * .5f);
                geo.setLocalScale(.15f);

                geo.addControl(new LodControl());
                debugNode.attachChild(geo);
            }
        }

        rootNode.attachChild(debugNode);
        registerInputMappings();
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (!isPressed) return;

        if (name.equals("toggleWireframe")) {
            RenderState renderState = lightMaterial.getAdditionalRenderState();
            boolean wireframe = renderState.isWireframe();
            renderState.setWireframe(!wireframe);
        }
    }

    private void registerInputMappings() {
        addMapping("toggleWireframe", new KeyTrigger(KeyInput.KEY_SPACE));
    }

    private void addMapping(String mappingName, Trigger... triggers) {
        inputManager.addMapping(mappingName, triggers);
        inputManager.addListener(this, mappingName);
    }

    private void configureCamera() {
        flyCam.setMoveSpeed(25f);
        flyCam.setDragToRotate(true);

        cam.setLocation(Vector3f.UNIT_XYZ.mult(8f));
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
    }

}
