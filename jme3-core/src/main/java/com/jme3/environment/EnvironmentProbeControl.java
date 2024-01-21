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
package com.jme3.environment;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import com.jme3.asset.AssetManager;
import com.jme3.environment.baker.IBLGLEnvBakerLight;
import com.jme3.environment.baker.IBLHybridEnvBakerLight;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.light.LightProbe;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import com.jme3.texture.Image.Format;

/**
 * A control that automatically handles environment bake and rebake including
 * only tagged spatials.
 * 
 * Simple usage example: <code>
 * 1. Load a scene
 *    Node scene=(Node)assetManager.loadModel("Scenes/MyScene.j3o"); 
 * 2. Add one or more EnvironmentProbeControl to the root of the scene
 *    EnvironmentProbeControl ec1=new EnvironmentProbeControl(assetManager, 512);
 *    //  EnvironmentProbeControl ec2=new EnvironmentProbeControl(assetManager, 512);
 * 2b. (optional) Set the position of the probes
 *    ec1.setPosition(new Vector3f(0,0,0));
 *    // ec2.setPosition(new Vector3f(0,0,10));
 * 3. Tag the spatials that are part of the environment
 *    scene.deepFirstTraversal(s->{
 *        if(s.getUserData("isEnvNode")!=null){
 *          EnvironmentProbeControl.tagGlobal(s);
 *          // or ec1.tag(s); 
 *          //    ec2.tag(s);
 *        }
 *    });
 *</code>
 * 
 * @author Riccardo Balbo
 */
public class EnvironmentProbeControl extends LightProbe implements Control {
    private static AtomicInteger instanceCounter = new AtomicInteger(0);

    private AssetManager assetManager;
    private boolean bakeNeeded = true;
    private int envMapSize = 256;
    private Spatial spatial;
    private boolean requiredSavableResults = false;
    private float frustumNear = 0.001f, frustumFar = 1000f;
    private String uuid = "none";
    private boolean enabled = true;

    private Predicate<Geometry> filter = (s) -> {
        return s.getUserData("tags.env") != null || s.getUserData("tags.env.env" + uuid) != null;
    };

    protected EnvironmentProbeControl() {
        super();
        uuid = System.currentTimeMillis() + "_" + instanceCounter.getAndIncrement();
        this.setAreaType(AreaType.Spherical);
        this.getArea().setRadius(Float.MAX_VALUE);
    }

    /**
     * Creates a new environment probe control.
     * 
     * @param assetManager
     *            the asset manager used to load the shaders needed for the
     *            baking
     * @param size
     *            the size of side of the resulting cube map (eg. 1024)
     */
    public EnvironmentProbeControl(AssetManager assetManager, int size) {
        this();
        this.envMapSize = size;
        this.assetManager = assetManager;        
    }

    /**
     * Tags the specified spatial as part of the environment for this EnvironmentProbeControl.
     * Only tagged spatials will be rendered in the environment map.
     * 
     * @param s
     *            the spatial
     */
    public void tag(Spatial s) {
        if (s instanceof Node) {
            Node n = (Node) s;
            for (Spatial sx : n.getChildren()) {
                tag(sx);
            }
        } else if (s instanceof Geometry) {
            s.setUserData("tags.env.env" + uuid, true);
        }
    }

    /**
     * Untags the specified spatial as part of the environment for this
     * EnvironmentProbeControl.
     * 
     * @param s
     *            the spatial
     */
    public void untag(Spatial s) {
        if (s instanceof Node) {
            Node n = (Node) s;
            for (Spatial sx : n.getChildren()) {
                untag(sx);
            }
        } else if (s instanceof Geometry) {
            s.setUserData("tags.env.env" + uuid, null);
        }
    }

    /**
     * Tags the specified spatial as part of the environment for every EnvironmentProbeControl.
     * Only tagged spatials will be rendered in the environment map.
     * 
     * @param s
     *            the spatial
     */
    public static void tagGlobal(Spatial s) {
        if (s instanceof Node) {
            Node n = (Node) s;
            for (Spatial sx : n.getChildren()) {
                tagGlobal(sx);
            }
        } else if (s instanceof Geometry) {
            s.setUserData("tags.env", true);
        }
    }

    /**
     * Untags the specified spatial as part of the environment for every
     * EnvironmentProbeControl.
     * 
     * @param s the spatial
     */
    public static void untagGlobal(Spatial s) {
        if (s instanceof Node) {
            Node n = (Node) s;
            for (Spatial sx : n.getChildren()) {
                untagGlobal(sx);
            }
        } else if (s instanceof Geometry) {
            s.setUserData("tags.env", null);
        }
    }

    @Override
    public Control cloneForSpatial(Spatial spatial) {
        throw new UnsupportedOperationException();
    }

    /**
     * Requests savable results from the baking process. This will make the
     * baking process slower and more memory intensive but will allow to
     * serialize the results with the control.
     * 
     * @param v
     *            true to enable (default: false)
     */
    public void setRequiredSavableResults(boolean v) {
        requiredSavableResults = v;
    }

    /**
     * Returns true if savable results are required by this control.
     * 
     * @return true if savable results are required.
     */
    public boolean isRequiredSavableResults() {
        return requiredSavableResults;
    }

    @Override
    public void setSpatial(Spatial spatial) {
        if (this.spatial != null && spatial != null && spatial != this.spatial) {
            throw new IllegalStateException("This control has already been added to a Spatial");
        }
        this.spatial = spatial;
        if (spatial != null) spatial.addLight(this);
    }

    @Override
    public void update(float tpf) {

    }

    @Override
    public void render(RenderManager rm, ViewPort vp) {
        if (!isEnabled()) return;
        if (bakeNeeded) {
            bakeNeeded = false;
            rebakeNow(rm);
        }
    }

    /**
     * Schedules a rebake of the environment map.
     */
    public void rebake() {
        bakeNeeded = true;
    }

    /**
     * Sets the minimum distance to render.
     * 
     * @param frustumNear the minimum distance to render
     */
    public void setFrustumNear(float frustumNear) {
        this.frustumNear = frustumNear;
    }

    /**
     * Sets the maximum distance to render.
     * 
     * @param frustumFar the maximum distance to render
     */
    public void setFrustumFar(float frustumFar) {
        this.frustumFar = frustumFar;
    }

    /**
     * Get the minimum distance to render
     * 
     * @return frustum near
     */
    public float getFrustumNear() {
        return frustumNear;
    }

    /**
     * Gets the maximum distance to render
     * 
     * @return frustum far
     */
    public float getFrustumFar() {
        return frustumFar;
    }

    /**
     * Sets the asset manager used to load the shaders needed for the baking
     * 
     * @param assetManager the asset manager
     */
    public void setAssetManager(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    void rebakeNow(RenderManager renderManager) {
        IBLHybridEnvBakerLight baker = new IBLGLEnvBakerLight(renderManager, assetManager, Format.RGB16F, Format.Depth,
                envMapSize, envMapSize);
                    
        baker.setTexturePulling(isRequiredSavableResults());
        baker.bakeEnvironment(spatial, getPosition(), frustumNear, frustumFar, filter);
        baker.bakeSpecularIBL();
        baker.bakeSphericalHarmonicsCoefficients();

        setPrefilteredMap(baker.getSpecularIBL());

        int[] mipSizes = getPrefilteredEnvMap().getImage().getMipMapSizes();
        setNbMipMaps(mipSizes != null ? mipSizes.length : 1);

        setShCoeffs(baker.getSphericalHarmonicsCoefficients());
        setPosition(Vector3f.ZERO);
        setReady(true);

        baker.clean();
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Spatial getSpatial() {
        return spatial;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(enabled, "enabled", true);
        oc.write(spatial, "spatial", null);
        oc.write(envMapSize, "size", 256);
        oc.write(requiredSavableResults, "requiredSavableResults", false);
        oc.write(bakeNeeded, "bakeNeeded", true);
        oc.write(frustumFar, "frustumFar", 1000f);
        oc.write(frustumNear, "frustumNear", 0.001f);
        oc.write(uuid, "envProbeControlUUID", "none");
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        enabled = ic.readBoolean("enabled", true);
        spatial = (Spatial) ic.readSavable("spatial", null);
        envMapSize = ic.readInt("size", 256);
        requiredSavableResults = ic.readBoolean("requiredSavableResults", false);
        bakeNeeded = ic.readBoolean("bakeNeeded", true);
        assetManager = im.getAssetManager();
        frustumFar = ic.readFloat("frustumFar", 1000f);
        frustumNear = ic.readFloat("frustumNear", 0.001f);
        uuid = ic.readString("envProbeControlUUID", "none");
    }

}
