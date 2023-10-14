/*
 * Copyright (c) 2009-2019 jMonkeyEngine
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
package com.jme3.environment;

import com.jme3.asset.AssetManager;
import com.jme3.environment.baker.IBLGLEnvBakerLight;
import com.jme3.environment.baker.IBLHybridEnvBakerLight;
import com.jme3.environment.util.EnvMapUtils;
import com.jme3.light.LightProbe;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.Image.Format;

/**
 * A faster version of LightProbeFactory that uses accelerated Baking.
 * @author Riccardo Balbo
 */
public class LightProbeFactory2 {

 
    /**
     * Creates a LightProbe with the giver EnvironmentCamera in the given scene.
     * @param rm The RenderManager
     * @param am The AssetManager
     * @param size The size of the probe
     * @param pos The position of the probe
     * @param frustumNear The near frustum of the probe
     * @param frustumFar The far frustum of the probe
     * @param scene The scene to bake
     * @return The baked LightProbe
     */
    public static LightProbe makeProbe(RenderManager rm,
    AssetManager am, int size,Vector3f pos, float frustumNear,float frustumFar,Spatial scene) {
        IBLHybridEnvBakerLight baker=new IBLGLEnvBakerLight(rm, am, Format.RGB16F, Format.Depth, size, size);

        baker.setTexturePulling(true);
        baker.bakeEnvironment(scene,pos, frustumNear,frustumFar,null);
        baker.bakeSpecularIBL();
        baker.bakeSphericalHarmonicsCoefficients();
        
        LightProbe probe = new LightProbe();
 
        probe.setPosition(pos);
        probe.setPrefilteredMap(baker.getSpecularIBL());
        
        int[] mipSizes = probe.getPrefilteredEnvMap().getImage().getMipMapSizes();
        probe.setNbMipMaps(mipSizes != null ? mipSizes.length : 1);
        
        probe.setShCoeffs(baker.getSphericalHarmonicsCoefficients());
        probe.setReady(true);

        baker.clean();

        return probe;

    }




    /**
     * For debuging porpose only
     * Will return a Node meant to be added to a GUI presenting the 2 cube maps in a cross pattern with all the mip maps.
     *
     * @param manager the asset manager
     * @return a debug node
     */
    public static Node getDebugGui(AssetManager manager, LightProbe probe) {
        if (!probe.isReady()) {
            throw new UnsupportedOperationException("This EnvProbe is not ready yet, try to test isReady()");
        }

        Node debugNode = new Node("debug gui probe");
        Node debugPfemCm = EnvMapUtils.getCubeMapCrossDebugViewWithMipMaps(probe.getPrefilteredEnvMap(), manager);
        debugNode.attachChild(debugPfemCm);
        debugPfemCm.setLocalTranslation(520, 0, 0);

        return debugNode;
    }


    
}
