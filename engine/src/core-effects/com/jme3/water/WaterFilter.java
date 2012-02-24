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
package com.jme3.water;

import com.jme3.asset.AssetManager;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.post.Filter;
import com.jme3.post.Filter.Pass;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.texture.Texture2D;
import com.jme3.util.TempVars;
import java.io.IOException;

/**
 * The WaterFilter is a 2D post process that simulate water.
 * It renders water above and under water.
 * See this blog post for more info <a href="http://jmonkeyengine.org/2011/01/15/new-advanced-water-effect-for-jmonkeyengine-3/">http://jmonkeyengine.org/2011/01/15/new-advanced-water-effect-for-jmonkeyengine-3/</a>
 * 
 * 
 * @author Rémy Bouquet aka Nehon
 */
public class WaterFilter extends Filter {

    private Pass reflectionPass;
    protected Spatial reflectionScene;
    protected ViewPort reflectionView;
    private Texture2D normalTexture;
    private Texture2D foamTexture;
    private Texture2D causticsTexture;
    private Texture2D heightTexture;
    private Plane plane;
    private Camera reflectionCam;
    protected Ray ray = new Ray();
    private Vector3f targetLocation = new Vector3f();
    private ReflectionProcessor reflectionProcessor;
    private Matrix4f biasMatrix = new Matrix4f(0.5f, 0.0f, 0.0f, 0.5f,
            0.0f, 0.5f, 0.0f, 0.5f,
            0.0f, 0.0f, 0.0f, 0.5f,
            0.0f, 0.0f, 0.0f, 1.0f);
    private Matrix4f textureProjMatrix = new Matrix4f();
    private boolean underWater;
    private RenderManager renderManager;
    private ViewPort viewPort;
    private float time = 0;
    //properties
    private float speed = 1;
    private Vector3f lightDirection = new Vector3f(0, -1, 0);
    private ColorRGBA lightColor = ColorRGBA.White;
    private float waterHeight = 0.0f;
    private ColorRGBA waterColor = new ColorRGBA(0.0078f, 0.3176f, 0.5f, 1.0f);
    private ColorRGBA deepWaterColor = new ColorRGBA(0.0039f, 0.00196f, 0.145f, 1.0f);
    private Vector3f colorExtinction = new Vector3f(5.0f, 20.0f, 30.0f);
    private float waterTransparency = 0.1f;
    private float maxAmplitude = 1.5f;
    private float shoreHardness = 0.1f;
    private boolean useFoam = true;
    private float foamIntensity = 0.5f;
    private float foamHardness = 1.0f;
    private Vector3f foamExistence = new Vector3f(0.45f, 4.35f, 1.5f);
    private float waveScale = 0.005f;
    private float sunScale = 3.0f;
    private float shininess = 0.7f;
    private Vector2f windDirection = new Vector2f(0.0f, -1.0f);
    private int reflectionMapSize = 512;
    private boolean useRipples = true;
    private float normalScale = 3.0f;
    private boolean useHQShoreline = true;
    private boolean useSpecular = true;
    private boolean useRefraction = true;
    private float refractionStrength = 0.0f;
    private float refractionConstant = 0.5f;
    private float reflectionDisplace = 30;
    private float underWaterFogDistance = 120;
    private boolean useCaustics = true;
    private float causticsIntensity = 0.5f;

    /**
     * Create a Water Filter
     */
    public WaterFilter() {
        super("WaterFilter");
    }

    public WaterFilter(Node reflectionScene, Vector3f lightDirection) {
        super("WaterFilter");
        this.reflectionScene = reflectionScene;
        this.lightDirection = lightDirection;
    }

    @Override
    protected boolean isRequiresDepthTexture() {
        return true;
    }

    @Override
    protected void preFrame(float tpf) {
        time = time + (tpf * speed);
        material.setFloat("Time", time);
        Camera sceneCam = viewPort.getCamera();
        biasMatrix.mult(sceneCam.getViewProjectionMatrix(), textureProjMatrix);
        material.setMatrix4("TextureProjMatrix", textureProjMatrix);
        material.setVector3("CameraPosition", sceneCam.getLocation());
        material.setMatrix4("ViewProjectionMatrixInverse", sceneCam.getViewProjectionMatrix().invert());

        material.setFloat("WaterHeight", waterHeight);

        //update reflection cam
        ray.setOrigin(sceneCam.getLocation());
        ray.setDirection(sceneCam.getDirection());
        plane = new Plane(Vector3f.UNIT_Y, new Vector3f(0, waterHeight, 0).dot(Vector3f.UNIT_Y));
        reflectionProcessor.setReflectionClipPlane(plane);
        boolean inv = false;
        if (!ray.intersectsWherePlane(plane, targetLocation)) {
            ray.setDirection(ray.getDirection().negateLocal());
            ray.intersectsWherePlane(plane, targetLocation);
            inv = true;
        }
        Vector3f loc = plane.reflect(sceneCam.getLocation(), new Vector3f());
        reflectionCam.setLocation(loc);
        reflectionCam.setFrustum(sceneCam.getFrustumNear(),
                sceneCam.getFrustumFar(),
                sceneCam.getFrustumLeft(),
                sceneCam.getFrustumRight(),
                sceneCam.getFrustumTop(),
                sceneCam.getFrustumBottom());
        TempVars vars = TempVars.get();


        vars.vect1.set(sceneCam.getLocation()).addLocal(sceneCam.getUp());
        float planeDistance = plane.pseudoDistance(vars.vect1);
        vars.vect2.set(plane.getNormal()).multLocal(planeDistance * 2.0f);
        vars.vect3.set(vars.vect1.subtractLocal(vars.vect2)).subtractLocal(loc).normalizeLocal().negateLocal();

        reflectionCam.lookAt(targetLocation, vars.vect3);
        vars.release();

        if (inv) {
            reflectionCam.setAxes(reflectionCam.getLeft().negateLocal(), reflectionCam.getUp(), reflectionCam.getDirection().negateLocal());
        }

        //if we're under water no need to compute reflection
        if (sceneCam.getLocation().y >= waterHeight) {
            boolean rtb = true;
            if (!renderManager.isHandleTranslucentBucket()) {
                renderManager.setHandleTranslucentBucket(true);
                rtb = false;
            }
            renderManager.renderViewPort(reflectionView, tpf);
            if (!rtb) {
                renderManager.setHandleTranslucentBucket(false);
            }
            renderManager.setCamera(sceneCam, false);
            renderManager.getRenderer().setFrameBuffer(viewPort.getOutputFrameBuffer());


            underWater = false;
        } else {
            underWater = true;
        }
    }

    @Override
    protected Material getMaterial() {
        return material;
    }

    private DirectionalLight findLight(Node node) {
        for (Light light : node.getWorldLightList()) {    
            if (light instanceof DirectionalLight) {
                return (DirectionalLight) light;
            }
        }
        for (Spatial child : node.getChildren()) {
            if (child instanceof Node) {
                return findLight((Node) child);
            }
        }

        return null;
    }

    @Override
    protected void initFilter(AssetManager manager, RenderManager renderManager, ViewPort vp, int w, int h) {

        if (reflectionScene == null) {
            reflectionScene = vp.getScenes().get(0);
            DirectionalLight l = findLight((Node) reflectionScene);
            if (l != null) {
                lightDirection = l.getDirection();
            }

        }

        this.renderManager = renderManager;
        this.viewPort = vp;
        reflectionPass = new Pass();
        reflectionPass.init(renderManager.getRenderer(), reflectionMapSize, reflectionMapSize, Format.RGBA8, Format.Depth);
        reflectionCam = new Camera(reflectionMapSize, reflectionMapSize);
        reflectionView = new ViewPort("reflectionView", reflectionCam);
        reflectionView.setClearFlags(true, true, true);
        reflectionView.attachScene(reflectionScene);
        reflectionView.setOutputFrameBuffer(reflectionPass.getRenderFrameBuffer());
        plane = new Plane(Vector3f.UNIT_Y, new Vector3f(0, waterHeight, 0).dot(Vector3f.UNIT_Y));
        reflectionProcessor = new ReflectionProcessor(reflectionCam, reflectionPass.getRenderFrameBuffer(), plane);
        reflectionView.addProcessor(reflectionProcessor);

        normalTexture = (Texture2D) manager.loadTexture("Common/MatDefs/Water/Textures/water_normalmap.dds");
        if (foamTexture == null) {
            foamTexture = (Texture2D) manager.loadTexture("Common/MatDefs/Water/Textures/foam.jpg");
        }
        if (causticsTexture == null) {
            causticsTexture = (Texture2D) manager.loadTexture("Common/MatDefs/Water/Textures/caustics.jpg");
        }
        heightTexture = (Texture2D) manager.loadTexture("Common/MatDefs/Water/Textures/heightmap.jpg");

        normalTexture.setWrap(WrapMode.Repeat);
        foamTexture.setWrap(WrapMode.Repeat);
        causticsTexture.setWrap(WrapMode.Repeat);
        heightTexture.setWrap(WrapMode.Repeat);

        material = new Material(manager, "Common/MatDefs/Water/Water.j3md");
        material.setTexture("HeightMap", heightTexture);
        material.setTexture("CausticsMap", causticsTexture);
        material.setTexture("FoamMap", foamTexture);
        material.setTexture("NormalMap", normalTexture);
        material.setTexture("ReflectionMap", reflectionPass.getRenderedTexture());

        material.setFloat("WaterTransparency", waterTransparency);
        material.setFloat("NormalScale", normalScale);
        material.setFloat("R0", refractionConstant);
        material.setFloat("MaxAmplitude", maxAmplitude);
        material.setVector3("LightDir", lightDirection);
        material.setColor("LightColor", lightColor);
        material.setFloat("ShoreHardness", shoreHardness);
        material.setFloat("RefractionStrength", refractionStrength);
        material.setFloat("WaveScale", waveScale);
        material.setVector3("FoamExistence", foamExistence);
        material.setFloat("SunScale", sunScale);
        material.setVector3("ColorExtinction", colorExtinction);
        material.setFloat("Shininess", shininess);
        material.setColor("WaterColor", waterColor);
        material.setColor("DeepWaterColor", deepWaterColor);
        material.setVector2("WindDirection", windDirection);
        material.setFloat("FoamHardness", foamHardness);
        material.setBoolean("UseRipples", useRipples);
        material.setBoolean("UseHQShoreline", useHQShoreline);
        material.setBoolean("UseSpecular", useSpecular);
        material.setBoolean("UseFoam", useFoam);
        material.setBoolean("UseCaustics", useCaustics);
        material.setBoolean("UseRefraction", useRefraction);
        material.setFloat("ReflectionDisplace", reflectionDisplace);
        material.setFloat("FoamIntensity", foamIntensity);
        material.setFloat("UnderWaterFogDistance", underWaterFogDistance);
        material.setFloat("CausticsIntensity", causticsIntensity);


    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);

        oc.write(speed, "speed", 1f);
        oc.write(lightDirection, "lightDirection", new Vector3f(0, -1, 0));
        oc.write(lightColor, "lightColor", ColorRGBA.White);
        oc.write(waterHeight, "waterHeight", 0.0f);
        oc.write(waterColor, "waterColor", new ColorRGBA(0.0078f, 0.3176f, 0.5f, 1.0f));
        oc.write(deepWaterColor, "deepWaterColor", new ColorRGBA(0.0039f, 0.00196f, 0.145f, 1.0f));

        oc.write(colorExtinction, "colorExtinction", new Vector3f(5.0f, 20.0f, 30.0f));
        oc.write(waterTransparency, "waterTransparency", 0.1f);
        oc.write(maxAmplitude, "maxAmplitude", 1.5f);
        oc.write(shoreHardness, "shoreHardness", 0.1f);
        oc.write(useFoam, "useFoam", true);

        oc.write(foamIntensity, "foamIntensity", 0.5f);
        oc.write(foamHardness, "foamHardness", 1.0f);

        oc.write(foamExistence, "foamExistence", new Vector3f(0.45f, 4.35f, 1.5f));
        oc.write(waveScale, "waveScale", 0.005f);

        oc.write(sunScale, "sunScale", 3.0f);
        oc.write(shininess, "shininess", 0.7f);
        oc.write(windDirection, "windDirection", new Vector2f(0.0f, -1.0f));
        oc.write(reflectionMapSize, "reflectionMapSize", 512);
        oc.write(useRipples, "useRipples", true);

        oc.write(normalScale, "normalScale", 3.0f);
        oc.write(useHQShoreline, "useHQShoreline", true);

        oc.write(useSpecular, "useSpecular", true);

        oc.write(useRefraction, "useRefraction", true);
        oc.write(refractionStrength, "refractionStrength", 0.0f);
        oc.write(refractionConstant, "refractionConstant", 0.5f);
        oc.write(reflectionDisplace, "reflectionDisplace", 30f);
        oc.write(underWaterFogDistance, "underWaterFogDistance", 120f);
        oc.write(causticsIntensity, "causticsIntensity", 0.5f);

        oc.write(useCaustics, "useCaustics", true);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        speed = ic.readFloat("speed", 1f);
        lightDirection = (Vector3f) ic.readSavable("lightDirection", new Vector3f(0, -1, 0));
        lightColor = (ColorRGBA) ic.readSavable("lightColor", ColorRGBA.White);
        waterHeight = ic.readFloat("waterHeight", 0.0f);
        waterColor = (ColorRGBA) ic.readSavable("waterColor", new ColorRGBA(0.0078f, 0.3176f, 0.5f, 1.0f));
        deepWaterColor = (ColorRGBA) ic.readSavable("deepWaterColor", new ColorRGBA(0.0039f, 0.00196f, 0.145f, 1.0f));

        colorExtinction = (Vector3f) ic.readSavable("colorExtinction", new Vector3f(5.0f, 20.0f, 30.0f));
        waterTransparency = ic.readFloat("waterTransparency", 0.1f);
        maxAmplitude = ic.readFloat("maxAmplitude", 1.5f);
        shoreHardness = ic.readFloat("shoreHardness", 0.1f);
        useFoam = ic.readBoolean("useFoam", true);

        foamIntensity = ic.readFloat("foamIntensity", 0.5f);
        foamHardness = ic.readFloat("foamHardness", 1.0f);

        foamExistence = (Vector3f) ic.readSavable("foamExistence", new Vector3f(0.45f, 4.35f, 1.5f));
        waveScale = ic.readFloat("waveScale", 0.005f);

        sunScale = ic.readFloat("sunScale", 3.0f);
        shininess = ic.readFloat("shininess", 0.7f);
        windDirection = (Vector2f) ic.readSavable("windDirection", new Vector2f(0.0f, -1.0f));
        reflectionMapSize = ic.readInt("reflectionMapSize", 512);
        useRipples = ic.readBoolean("useRipples", true);

        normalScale = ic.readFloat("normalScale", 3.0f);
        useHQShoreline = ic.readBoolean("useHQShoreline", true);

        useSpecular = ic.readBoolean("useSpecular", true);

        useRefraction = ic.readBoolean("useRefraction", true);
        refractionStrength = ic.readFloat("refractionStrength", 0.0f);
        refractionConstant = ic.readFloat("refractionConstant", 0.5f);
        reflectionDisplace = ic.readFloat("reflectionDisplace", 30f);
        underWaterFogDistance = ic.readFloat("underWaterFogDistance", 120f);
        causticsIntensity = ic.readFloat("causticsIntensity", 0.5f);

        useCaustics = ic.readBoolean("useCaustics", true);

    }

    /**
     * gets the height of the water plane
     * @return
     */
    public float getWaterHeight() {
        return waterHeight;
    }

    /**
     * Sets the height of the water plane
     * default is 0.0
     * @param waterHeight
     */
    public void setWaterHeight(float waterHeight) {
        this.waterHeight = waterHeight;
    }

    /**
     * sets the scene to render in the reflection map
     * @param reflectionScene 
     */
    public void setReflectionScene(Spatial reflectionScene) {
        this.reflectionScene = reflectionScene;
    }

    /**
     * returns the waterTransparency value
     * @return
     */
    public float getWaterTransparency() {
        return waterTransparency;
    }

    /**
     * Sets how fast will colours fade out. You can also think about this
     * values as how clear water is. Therefore use smaller values (eg. 0.05)
     * to have crystal clear water and bigger to achieve "muddy" water.
     * default is 0.1f
     * @param waterTransparency
     */
    public void setWaterTransparency(float waterTransparency) {
        this.waterTransparency = waterTransparency;
        if (material != null) {
            material.setFloat("WaterTransparency", waterTransparency);
        }
    }

    /**
     * Returns the normal scales applied to the normal map
     * @return
     */
    public float getNormalScale() {
        return normalScale;
    }

    /**
     * Sets the normal scaling factors to apply to the normal map.
     * the higher the value the more small ripples will be visible on the waves.
     * default is 1.0
     * @param normalScale
     */
    public void setNormalScale(float normalScale) {
        this.normalScale = normalScale;
        if (material != null) {
            material.setFloat("NormalScale", normalScale);
        }
    }

    /**
     * returns the refractoin constant
     * @return 
     */
    public float getRefractionConstant() {
        return refractionConstant;
    }

    /**
     * This is a constant related to the index of refraction (IOR) used to compute the fresnel term.
     * F = R0 + (1-R0)( 1 - N.V)^5
     * where F is the fresnel term, R0 the constant, N the normal vector and V tne view vector.
     * It usually depend on the material you are lookinh through (here water).
     * Default value is 0.3f
     * In practice, the lowest the value and the less the reflection can be seen on water
     * @param refractionConstant
     */
    public void setRefractionConstant(float refractionConstant) {
        this.refractionConstant = refractionConstant;
        if (material != null) {
            material.setFloat("R0", refractionConstant);
        }
    }

    /**
     * return the maximum wave amplitude
     * @return 
     */
    public float getMaxAmplitude() {
        return maxAmplitude;
    }

    /**
     * Sets the maximum waves amplitude
     * default is 1.0
     * @param maxAmplitude
     */
    public void setMaxAmplitude(float maxAmplitude) {
        this.maxAmplitude = maxAmplitude;
        if (material != null) {
            material.setFloat("MaxAmplitude", maxAmplitude);
        }
    }

    /**
     * gets the light direction
     * @return
     */
    public Vector3f getLightDirection() {
        return lightDirection;
    }

    /**
     * Sets the light direction
     * @param lightDirection
     */
    public void setLightDirection(Vector3f lightDirection) {
        this.lightDirection = lightDirection;
        if (material != null) {
            material.setVector3("LightDir", lightDirection);
        }
    }

    /**
     * returns the light color
     * @return
     */
    public ColorRGBA getLightColor() {
        return lightColor;
    }

    /**
     * Sets the light color to use
     * default is white
     * @param lightColor
     */
    public void setLightColor(ColorRGBA lightColor) {
        this.lightColor = lightColor;
        if (material != null) {
            material.setColor("LightColor", lightColor);
        }
    }

    /**
     * Return the shoreHardeness
     * @return
     */
    public float getShoreHardness() {
        return shoreHardness;
    }

    /**
     * The smaller this value is, the softer the transition between
     * shore and water. If you want hard edges use very big value.
     * Default is 0.1f.
     * @param shoreHardness
     */
    public void setShoreHardness(float shoreHardness) {
        this.shoreHardness = shoreHardness;
        if (material != null) {
            material.setFloat("ShoreHardness", shoreHardness);
        }
    }

    /**
     * returns the foam hardness
     * @return
     */
    public float getFoamHardness() {
        return foamHardness;
    }

    /**
     * Sets the foam hardness : How much the foam will blend with the shore to avoid hard edged water plane.
     * Default is 1.0
     * @param foamHardness
     */
    public void setFoamHardness(float foamHardness) {
        this.foamHardness = foamHardness;
        if (material != null) {
            material.setFloat("FoamHardness", foamHardness);
        }
    }

    /**
     * returns the refractionStrenght
     * @return
     */
    public float getRefractionStrength() {
        return refractionStrength;
    }

    /**
     * This value modifies current fresnel term. If you want to weaken
     * reflections use bigger value. If you want to empasize them use
     * value smaller then 0. Default is 0.0f.
     * @param refractionStrength
     */
    public void setRefractionStrength(float refractionStrength) {
        this.refractionStrength = refractionStrength;
        if (material != null) {
            material.setFloat("RefractionStrength", refractionStrength);
        }
    }

    /**
     * returns the scale factor of the waves height map
     * @return
     */
    public float getWaveScale() {
        return waveScale;
    }

    /**
     * Sets the scale factor of the waves height map
     * the smaller the value the bigger the waves
     * default is 0.005f
     * @param waveScale
     */
    public void setWaveScale(float waveScale) {
        this.waveScale = waveScale;
        if (material != null) {
            material.setFloat("WaveScale", waveScale);
        }
    }

    /**
     * returns the foam existance vector
     * @return
     */
    public Vector3f getFoamExistence() {
        return foamExistence;
    }

    /**
     * Describes at what depth foam starts to fade out and
     * at what it is completely invisible. The third value is at
     * what height foam for waves appear (+ waterHeight).
     * default is (0.45, 4.35, 1.0);
     * @param foamExistence
     */
    public void setFoamExistence(Vector3f foamExistence) {
        this.foamExistence = foamExistence;
        if (material != null) {
            material.setVector3("FoamExistence", foamExistence);
        }
    }

    /**
     * gets the scale of the sun
     * @return
     */
    public float getSunScale() {
        return sunScale;
    }

    /**
     * Sets the scale of the sun for specular effect
     * @param sunScale
     */
    public void setSunScale(float sunScale) {
        this.sunScale = sunScale;
        if (material != null) {
            material.setFloat("SunScale", sunScale);
        }
    }

    /**
     * Returns the color exctinction vector of the water
     * @return
     */
    public Vector3f getColorExtinction() {
        return colorExtinction;
    }

    /**
     * Return at what depth the refraction color extinct
     * the first value is for red
     * the second is for green
     * the third is for blue
     * Play with thos parameters to "trouble" the water
     * default is (5.0, 20.0, 30.0f);
     * @param colorExtinction
     */
    public void setColorExtinction(Vector3f colorExtinction) {
        this.colorExtinction = colorExtinction;
        if (material != null) {
            material.setVector3("ColorExtinction", colorExtinction);
        }
    }

    /**
     * Sets the foam texture
     * @param foamTexture
     */
    public void setFoamTexture(Texture2D foamTexture) {
        this.foamTexture = foamTexture;
        foamTexture.setWrap(WrapMode.Repeat);
        if (material != null) {
            material.setTexture("FoamMap", foamTexture);
        }
    }

    /**
     * Sets the height texture
     * @param heightTexture
     */
    public void setHeightTexture(Texture2D heightTexture) {
        this.heightTexture = heightTexture;
        heightTexture.setWrap(WrapMode.Repeat);
    }

    /**
     * Sets the normal Texture
     * @param normalTexture
     */
    public void setNormalTexture(Texture2D normalTexture) {
        this.normalTexture = normalTexture;
        normalTexture.setWrap(WrapMode.Repeat);
    }

    /**
     * return the shininess factor of the water
     * @return
     */
    public float getShininess() {
        return shininess;
    }

    /**
     * Sets the shinines factor of the water
     * default is 0.7f
     * @param shininess
     */
    public void setShininess(float shininess) {
        this.shininess = shininess;
        if (material != null) {
            material.setFloat("Shininess", shininess);
        }
    }

    /**
     * retruns the speed of the waves
     * @return
     */
    public float getSpeed() {
        return speed;
    }

    /**
     * Set the speed of the waves (0.0 is still) default is 1.0
     * @param speed
     */
    public void setSpeed(float speed) {
        this.speed = speed;
    }

    /**
     * returns the color of the water
     *
     * @return
     */
    public ColorRGBA getWaterColor() {
        return waterColor;
    }

    /**
     * Sets the color of the water
     * see setDeepWaterColor for deep water color
     * default is (0.0078f, 0.5176f, 0.5f,1.0f) (greenish blue)
     * @param waterColor
     */
    public void setWaterColor(ColorRGBA waterColor) {
        this.waterColor = waterColor;
        if (material != null) {
            material.setColor("WaterColor", waterColor);
        }
    }

    /**
     * returns the deep water color
     * @return
     */
    public ColorRGBA getDeepWaterColor() {
        return deepWaterColor;
    }

    /**
     * sets the deep water color
     * see setWaterColor for general color
     * default is (0.0039f, 0.00196f, 0.145f,1.0f) (very dark blue)
     * @param deepWaterColor
     */
    public void setDeepWaterColor(ColorRGBA deepWaterColor) {
        this.deepWaterColor = deepWaterColor;
        if (material != null) {
            material.setColor("DeepWaterColor", deepWaterColor);
        }
    }

    /**
     * returns the wind direction
     * @return
     */
    public Vector2f getWindDirection() {
        return windDirection;
    }

    /**
     * sets the wind direction
     * the direction where the waves move
     * default is (0.0f, -1.0f)
     * @param windDirection
     */
    public void setWindDirection(Vector2f windDirection) {
        this.windDirection = windDirection;
        if (material != null) {
            material.setVector2("WindDirection", windDirection);
        }
    }

    /**
     * returns the size of the reflection map
     * @return
     */
    public int getReflectionMapSize() {
        return reflectionMapSize;
    }

    /**
     * Sets the size of the reflection map
     * default is 512, the higher, the better quality, but the slower the effect.
     * @param reflectionMapSize
     */
    public void setReflectionMapSize(int reflectionMapSize) {
        this.reflectionMapSize = reflectionMapSize;
    }

    /**
     * returns true if the water uses foam
     * @return
     */
    public boolean isUseFoam() {
        return useFoam;
    }

    /**
     * set to true to use foam with water
     * default true
     * @param useFoam
     */
    public void setUseFoam(boolean useFoam) {
        this.useFoam = useFoam;
        if (material != null) {
            material.setBoolean("UseFoam", useFoam);
        }

    }

    /**
     * sets the texture to use to render caustics on the ground underwater
     * @param causticsTexture 
     */
    public void setCausticsTexture(Texture2D causticsTexture) {
        this.causticsTexture = causticsTexture;
        if (material != null) {
            material.setTexture("causticsMap", causticsTexture);
        }
    }

    /**
     * returns true if caustics are rendered
     * @return 
     */
    public boolean isUseCaustics() {
        return useCaustics;
    }

    /**
     * set to true if you want caustics to be rendered on the ground underwater, false otherwise
     * @param useCaustics 
     */
    public void setUseCaustics(boolean useCaustics) {
        this.useCaustics = useCaustics;
        if (material != null) {
            material.setBoolean("UseCaustics", useCaustics);
        }
    }

    /**
     * return true 
     * @return
     */
    public boolean isUseHQShoreline() {
        return useHQShoreline;
    }

    public void setUseHQShoreline(boolean useHQShoreline) {
        this.useHQShoreline = useHQShoreline;
        if (material != null) {
            material.setBoolean("UseHQShoreline", useHQShoreline);
        }

    }

    /**
     * returns true if the water use the refraction
     * @return 
     */
    public boolean isUseRefraction() {
        return useRefraction;
    }

    /**
     * set to true to use refraction (default is true)
     * @param useRefraction 
     */
    public void setUseRefraction(boolean useRefraction) {
        this.useRefraction = useRefraction;
        if (material != null) {
            material.setBoolean("UseRefraction", useRefraction);
        }

    }

    /**
     * returns true if the ater use ripples
     * @return 
     */
    public boolean isUseRipples() {
        return useRipples;
    }

    /**
     * 
     * Set to true tu use ripples
     * @param useRipples 
     */
    public void setUseRipples(boolean useRipples) {
        this.useRipples = useRipples;
        if (material != null) {
            material.setBoolean("UseRipples", useRipples);
        }

    }

    /**
     * returns true if the water use specular
     * @return 
     */
    public boolean isUseSpecular() {
        return useSpecular;
    }

    /**
     * Set to true to use specular lightings on the water
     * @param useSpecular 
     */
    public void setUseSpecular(boolean useSpecular) {
        this.useSpecular = useSpecular;
        if (material != null) {
            material.setBoolean("UseSpecular", useSpecular);
        }
    }

    /**
     * returns the foam intensity
     * @return 
     */
    public float getFoamIntensity() {
        return foamIntensity;
    }

    /**
     * sets the foam intensity default is 0.5f
     * @param foamIntensity 
     */
    public void setFoamIntensity(float foamIntensity) {
        this.foamIntensity = foamIntensity;
        if (material != null) {
            material.setFloat("FoamIntensity", foamIntensity);

        }
    }

    /**
     * returns the reflection displace
     * see {@link setReflectionDisplace(float reflectionDisplace)}
     * @return 
     */
    public float getReflectionDisplace() {
        return reflectionDisplace;
    }

    /**
     * Sets the reflection displace. define how troubled will look the reflection in the water. default is 30
     * @param reflectionDisplace 
     */
    public void setReflectionDisplace(float reflectionDisplace) {
        this.reflectionDisplace = reflectionDisplace;
        if (material != null) {
            material.setFloat("m_ReflectionDisplace", reflectionDisplace);
        }
    }

    /**
     * returns true if the camera is under the water level
     * @return 
     */
    public boolean isUnderWater() {
        return underWater;
    }

    /**
     * returns the distance of the fog when under water
     * @return 
     */
    public float getUnderWaterFogDistance() {
        return underWaterFogDistance;
    }

    /**
     * sets the distance of the fog when under water.
     * default is 120 (120 world units) use a high value to raise the view range under water
     * @param underWaterFogDistance 
     */
    public void setUnderWaterFogDistance(float underWaterFogDistance) {
        this.underWaterFogDistance = underWaterFogDistance;
        if (material != null) {
            material.setFloat("UnderWaterFogDistance", underWaterFogDistance);
        }
    }

    /**
     * get the intensity of caustics under water
     * @return 
     */
    public float getCausticsIntensity() {
        return causticsIntensity;
    }

    /**
     * sets the intensity of caustics under water. goes from 0 to 1, default is 0.5f
     * @param causticsIntensity 
     */
    public void setCausticsIntensity(float causticsIntensity) {
        this.causticsIntensity = causticsIntensity;
        if (material != null) {
            material.setFloat("CausticsIntensity", causticsIntensity);
        }
    }
}
