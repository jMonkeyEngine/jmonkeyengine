/*
 * Copyright (c) 2009-2023 jMonkeyEngine
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

package jme3test.scene.instancing;

import com.jme3.app.SimpleApplication;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.scene.Geometry;
import com.jme3.scene.instancing.InstancedNode;
import com.jme3.scene.shape.Box;
import com.jme3.water.WaterFilter;

/**
 * A test case for using instancing with shadow filter. This is a test case
 * for issue 2007 (Instanced objects are culled when using the WaterFilter).
 *
 * If test succeeds, all the boxes in the camera frustum will be rendered. If
 * test fails, most of the boxes that are in the camera frustum will be culled.
 *
 * @author Ali-RS
 */
public class TestInstancingWithWaterFilter extends SimpleApplication {
    public static void main(String[] args) {
        TestInstancingWithWaterFilter test = new TestInstancingWithWaterFilter();
        test.start();
    }

    @Override
    public void simpleInitApp() {
        flyCam.setMoveSpeed(10);

        DirectionalLight light = new DirectionalLight();
        light.setDirection(new Vector3f(-1, -1, -1));
        rootNode.addLight(light);

        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseInstancing", true);

        Box mesh = new Box(0.5f, 0.5f, 0.5f);

        InstancedNode instanceNode = new InstancedNode("TestInstancedNode");
        //instanceNode.setCullHint(Spatial.CullHint.Never);
        rootNode.attachChild(instanceNode);

        for (int i = 0; i < 200; i++) {
            Geometry obj = new Geometry("TestBox" + i, mesh);
            obj.setMaterial(mat);
            obj.setLocalTranslation(i, i, 0);
            instanceNode.attachChild(obj);
        }
        instanceNode.instance();

        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        WaterFilter waterFilter = new WaterFilter(rootNode, light.getDirection());
        fpp.addFilter(waterFilter);
        viewPort.addProcessor(fpp);
    }
}
