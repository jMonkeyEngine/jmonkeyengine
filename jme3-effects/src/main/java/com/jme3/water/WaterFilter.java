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
package com.jme3.water;

import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.post.Filter;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.texture.Texture2D;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;

import java.io.IOException;

/**
 * The WaterFilter is a 2-D post process that simulates water.
 * It renders water from both above and below the surface.
 * See the jMonkeyEngine wiki for more info <a href="https://jmonkeyengine.github.io/wiki/jme3/advanced/post-processor_water.html">https://jmonkeyengine.github.io/wiki/jme3/advanced/post-processor_water.html</a>.
 *
 *
 * @author Rémy Bouquet aka Nehon
 */
public class WaterFilter extends Filter implements JmeCloneable, Cloneable {

    public static final String DEFAULT_NORMAL_MAP = "Common/MatDefs/Water/Textures/water_normalmap.dds";
    public static final String DEFAULT_FOAM = "Common/MatDefs/Water/Textures/foam.jpg";
    public static final String DEFAULT_CAUSTICS = "Common/MatDefs/Water/Textures/caustics.jpg";
    public static final String DEFAULT_HEIGHT_MAP = "Common/MatDefs/Water/Textures/heightmap.jpg";

    private Pass reflectionPass;
    protected Spatial reflectionScene;
    protected Spatial rootScene;
    protected ViewPort reflectionView;
    private Texture2D normalTexture;
    private Texture2D foamTexture;
    private Texture2D causticsTexture;
    private Texture2D heightTexture;
    private Camera reflectionCam;
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
    private Plane plane = new Plane(Vector3f.UNIT_Y, waterHeight);
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
    //positional attributes
    private Vector3f center;
    private float radius;
    private AreaShape shapeType = AreaShape.Circular;

    private boolean needSaveReflectionScene;

    public enum AreaShape{
        Circular,
        Square
    }

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
        //material.setFloat("WaterHeight", waterHeight);

        //update reflection cam      
        //plane = new Plane(Vector3f.UNIT_Y, new Vector3f(0, waterHeight, 0).dot(Vector3f.UNIT_Y));
        //reflectionProcessor.setReflectionClipPlane(plane);        
        WaterUtils.updateReflectionCam(reflectionCam, plane, sceneCam);
      

        // If we're underwater, we don't need to compute reflection.
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

    /**
     * @return true to try using directional light from a scene
     */
    protected boolean useDirectionLightFromScene() {
        return true;
    }

    @Override
    protected void initFilter(AssetManager manager, RenderManager renderManager, ViewPort vp, int w, int h) {
        rootScene = vp.getScenes().get(0);

        if (reflectionScene == null) {
            reflectionScene = rootScene;
            DirectionalLight directionalLight = findLight((Node) reflectionScene);
            if (directionalLight != null && useDirectionLightFromScene()) {
                lightDirection = directionalLight.getDirection();
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
        reflectionProcessor.setReflectionClipPlane(plane);
        reflectionView.addProcessor(reflectionProcessor);

        if (normalTexture == null) {
            normalTexture = (Texture2D) manager.loadTexture(DEFAULT_NORMAL_MAP);
            normalTexture.setWrap(WrapMode.Repeat);
        }

        if (foamTexture == null) {
            foamTexture = (Texture2D) manager.loadTexture(DEFAULT_FOAM);
            foamTexture.setWrap(WrapMode.Repeat);
        }

        if (causticsTexture == null) {
            causticsTexture = (Texture2D) manager.loadTexture(DEFAULT_CAUSTICS);
            causticsTexture.setWrap(WrapMode.Repeat);
        }

        if (heightTexture == null) {
            heightTexture = (Texture2D) manager.loadTexture(DEFAULT_HEIGHT_MAP);
            heightTexture.setWrap(WrapMode.Repeat);
        }

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
        if (center != null) {
            material.setVector3("Center", center);
            material.setFloat("Radius", radius * radius);
            material.setBoolean("SquareArea", shapeType == AreaShape.Square);
        }
        material.setFloat("WaterHeight", waterHeight);
    }

    @Override
    protected void cleanUpFilter(Renderer r) {
        reflectionPass.cleanup(r);
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);

        OutputCapsule oc = ex.getCapsule(this);

        final Spatial reflectionScene = getReflectionScene();
        final boolean needSaveReflectionScene = isNeedSaveReflectionScene();

        final AssetKey causticsTextureKey = causticsTexture.getKey();
        final AssetKey heightTextureKey = heightTexture.getKey();
        final AssetKey normalTextureKey = normalTexture.getKey();
        final AssetKey foamTextureKey = foamTexture.getKey();

        if (causticsTextureKey != null && !DEFAULT_CAUSTICS.equals(causticsTextureKey.getName())) {
            oc.write(causticsTextureKey, "causticsTexture", null);
        }
        if (heightTextureKey != null && !DEFAULT_HEIGHT_MAP.equals(heightTextureKey.getName())) {
            oc.write(heightTextureKey, "heightTexture", null);
        }
        if (normalTextureKey != null && !DEFAULT_NORMAL_MAP.equals(normalTextureKey.getName())) {
            oc.write(normalTextureKey, "normalTexture", null);
        }
        if (foamTextureKey != null && !DEFAULT_FOAM.equals(foamTextureKey.getName())) {
            oc.write(foamTextureKey, "foamTexture", null);
        }

        oc.write(needSaveReflectionScene, "needSaveReflectionScene", false);

        if (needSaveReflectionScene) {
            oc.write(reflectionScene, "reflectionScene", null);
        }

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
        
        //positional attributes
        oc.write(center, "center", null);
        oc.write(radius, "radius", 0f);
        oc.write(shapeType.ordinal(), "shapeType", AreaShape.Circular.ordinal());
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

        final TextureKey causticsTextureKey = (TextureKey) ic.readSavable("causticsTexture", null);
        final TextureKey heightTextureKey = (TextureKey) ic.readSavable("heightTexture", null);
        final TextureKey normalTextureKey = (TextureKey) ic.readSavable("normalTexture", null);
        final TextureKey foamTextureKey = (TextureKey) ic.readSavable("foamTexture", null);

        needSaveReflectionScene = ic.readBoolean("needSaveReflectionScene", false);
        reflectionScene = (Spatial) ic.readSavable("reflectionScene", null);

        final AssetManager assetManager = im.getAssetManager();

        if (causticsTextureKey != null) {
            setCausticsTexture((Texture2D) assetManager.loadTexture(causticsTextureKey));
        }
        if (heightTextureKey != null) {
            setHeightTexture((Texture2D) assetManager.loadTexture(heightTextureKey));
        }
        if (normalTextureKey != null) {
            setNormalTexture((Texture2D) assetManager.loadTexture(normalTextureKey));
        }
        if (foamTextureKey != null) {
            setFoamTexture((Texture2D) assetManager.loadTexture(foamTextureKey));
        }

        //positional attributes
        center = (Vector3f) ic.readSavable("center", null);
        radius = ic.readFloat("radius", 0f);
        int shapeType = ic.readInt("shapeType", AreaShape.Circular.ordinal());
        this.shapeType = AreaShape.values()[shapeType];
    }

    /**
     * gets the height of the water plane
     * @return the height
     */
    public float getWaterHeight() {
        return waterHeight;
    }

    /**
     * Sets the height of the water plane
     * default is 0.0
     *
     * @param waterHeight the desired height (default=0)
     */
    public void setWaterHeight(float waterHeight) {
        this.waterHeight = waterHeight;
        this.plane = new Plane(Vector3f.UNIT_Y, waterHeight);
        if (material != null) {
            material.setFloat("WaterHeight", waterHeight);
        }
        if (reflectionProcessor != null) {
            reflectionProcessor.setReflectionClipPlane(plane);
        }
    }

    /**
     * Sets the scene to render in the reflection map.
     *
     * @param reflectionScene the refraction scene.
     */
    public void setReflectionScene(final Spatial reflectionScene) {

        final Spatial currentScene = getReflectionScene();

        if (reflectionView != null) {
            reflectionView.detachScene(currentScene == null? rootScene : currentScene);
        }

        this.reflectionScene = reflectionScene;

        if (reflectionView != null) {
            reflectionView.attachScene(reflectionScene == null? rootScene : reflectionScene);
        }
    }

    /**
     * Gets the scene which is used to render in the reflection map.
     *
     * @return the refraction scene.
     */
    public Spatial getReflectionScene() {
        return reflectionScene;
    }

    /**
     * Gets the view port used to render reflection scene.
     *
     * @return the reflection view port.
     */
    public ViewPort getReflectionView() {
        return reflectionView;
    }

    /**
     * returns the waterTransparency value
     * @return the transparency value
     */
    public float getWaterTransparency() {
        return waterTransparency;
    }

    /**
     * Sets how fast colours fade out. You can also think about this
     * as how clear water is. Therefore, use smaller values (e.g. 0.05)
     * for crystal-clear water and bigger values for "muddy" water.
     * default is 0.1f
     *
     * @param waterTransparency the desired muddiness (default=0.1)
     */
    public void setWaterTransparency(float waterTransparency) {
        this.waterTransparency = waterTransparency;
        if (material != null) {
            material.setFloat("WaterTransparency", waterTransparency);
        }
    }

    /**
     * Returns the normal scales applied to the normal map
     * @return the scale factor
     */
    public float getNormalScale() {
        return normalScale;
    }

    /**
     * Sets the normal scaling factors to apply to the normal map.
     * the higher the value the more small ripples will be visible on the waves.
     * default is 3
     *
     * @param normalScale the scaling factor (default=3)
     */
    public void setNormalScale(float normalScale) {
        this.normalScale = normalScale;
        if (material != null) {
            material.setFloat("NormalScale", normalScale);
        }
    }

    /**
     * returns the refraction constant
     * @return the refraction constant
     */
    public float getRefractionConstant() {
        return refractionConstant;
    }

    /**
     * This is a constant related to the index of refraction (IOR) used to compute the fresnel term.
     * F = R0 + (1-R0)( 1 - N.V)^5
     * where F is the fresnel term, R0 the constant, N the normal vector and V the view vector.
     * It depends on the substance you are looking through (here water).
     * Default value is 0.5
     * In practice, the lowest the value and the less the reflection can be seen on water
     *
     * @param refractionConstant the desired R0 value (default=0.5)
     */
    public void setRefractionConstant(float refractionConstant) {
        this.refractionConstant = refractionConstant;
        if (material != null) {
            material.setFloat("R0", refractionConstant);
        }
    }

    /**
     * return the maximum wave amplitude
     * @return the maximum amplitude
     */
    public float getMaxAmplitude() {
        return maxAmplitude;
    }

    /**
     * Sets the maximum waves amplitude
     * default is 1.5
     *
     * @param maxAmplitude the desired maximum amplitude (default=1.5)
     */
    public void setMaxAmplitude(float maxAmplitude) {
        this.maxAmplitude = maxAmplitude;
        if (material != null) {
            material.setFloat("MaxAmplitude", maxAmplitude);
        }
    }

    /**
     * gets the light direction
     * @return the pre-existing vector
     */
    public Vector3f getLightDirection() {
        return lightDirection;
    }

    /**
     * Sets the light direction
     *
     * @param lightDirection the direction vector to use (alias created,
     * default=(0,-1,0))
     */
    public void setLightDirection(Vector3f lightDirection) {
        this.lightDirection = lightDirection;
        if (material != null) {
            material.setVector3("LightDir", lightDirection);
        }
    }

    /**
     * returns the light color
     * @return the pre-existing instance
     */
    public ColorRGBA getLightColor() {
        return lightColor;
    }

    /**
     * Sets the light color to use
     * default is white
     *
     * @param lightColor the color to use (alias created, default=(1,1,1,1))
     */
    public void setLightColor(ColorRGBA lightColor) {
        this.lightColor = lightColor;
        if (material != null) {
            material.setColor("LightColor", lightColor);
        }
    }

    /**
     * Return the shore hardness.
     * @return the hardness value
     */
    public float getShoreHardness() {
        return shoreHardness;
    }

    /**
     * The smaller this value is, the softer the transition between
     * shore and water. If you want hard edges use very big value.
     * Default is 0.1f.
     *
     * @param shoreHardness the desired hardness (default=0.1)
     */
    public void setShoreHardness(float shoreHardness) {
        this.shoreHardness = shoreHardness;
        if (material != null) {
            material.setFloat("ShoreHardness", shoreHardness);
        }
    }

    /**
     * returns the foam hardness
     * @return the hardness value
     */
    public float getFoamHardness() {
        return foamHardness;
    }

    /**
     * Sets the foam hardness : How much the foam will blend with the shore to avoid hard edged water plane.
     * Default is 1.0
     *
     * @param foamHardness the desired hardness (default=1)
     */
    public void setFoamHardness(float foamHardness) {
        this.foamHardness = foamHardness;
        if (material != null) {
            material.setFloat("FoamHardness", foamHardness);
        }
    }

    /**
     * returns the refractionStrength
     * @return the strength value
     */
    public float getRefractionStrength() {
        return refractionStrength;
    }

    /**
     * This value modifies current fresnel term. If you want to weaken
     * reflections use bigger value. If you want to emphasize them use
     * a value smaller than 0. Default is 0.
     *
     * @param refractionStrength the desired strength (default=0)
     */
    public void setRefractionStrength(float refractionStrength) {
        this.refractionStrength = refractionStrength;
        if (material != null) {
            material.setFloat("RefractionStrength", refractionStrength);
        }
    }

    /**
     * Returns the scale factor of the waves' height map.
     * @return the scale factor
     */
    public float getWaveScale() {
        return waveScale;
    }

    /**
     * Sets the scale factor of the waves' height map.
     * The smaller the value, the bigger the waves.
     * Default is 0.005 .
     *
     * @param waveScale the desired scale factor (default=0.005)
     */
    public void setWaveScale(float waveScale) {
        this.waveScale = waveScale;
        if (material != null) {
            material.setFloat("WaveScale", waveScale);
        }
    }

    /**
     * returns the foam existence vector
     * @return the pre-existing vector
     */
    public Vector3f getFoamExistence() {
        return foamExistence;
    }

    /**
     * Describes at what depth foam starts to fade out and
     * at what it is completely invisible. The third value is at
     * what height foam for waves appear (+ waterHeight).
     * default is (0.45, 4.35, 1.0);
     *
     * @param foamExistence the desired parameters (alias created)
     */
    public void setFoamExistence(Vector3f foamExistence) {
        this.foamExistence = foamExistence;
        if (material != null) {
            material.setVector3("FoamExistence", foamExistence);
        }
    }

    /**
     * gets the scale of the sun
     * @return the scale factor
     */
    public float getSunScale() {
        return sunScale;
    }

    /**
     * Sets the scale of the sun for specular effect
     *
     * @param sunScale the desired scale factor (default=3)
     */
    public void setSunScale(float sunScale) {
        this.sunScale = sunScale;
        if (material != null) {
            material.setFloat("SunScale", sunScale);
        }
    }

    /**
     * Returns the color extinction vector of the water
     * @return the pre-existing vector
     */
    public Vector3f getColorExtinction() {
        return colorExtinction;
    }

    /**
     * Return at what depth the refraction color extinct
     * the first value is for red
     * the second is for green
     * the third is for blue
     * Play with those parameters to "trouble" the water
     * default is (5.0, 20.0, 30.0f);
     *
     * @param colorExtinction the desired depth for each color component (alias
     * created, default=(5,20,30))
     */
    public void setColorExtinction(Vector3f colorExtinction) {
        this.colorExtinction = colorExtinction;
        if (material != null) {
            material.setVector3("ColorExtinction", colorExtinction);
        }
    }

    /**
     * Sets the foam texture.
     *
     * @param foamTexture the foam texture.
     */
    public void setFoamTexture(Texture2D foamTexture) {
        this.foamTexture = foamTexture;
        foamTexture.setWrap(WrapMode.Repeat);
        if (material != null) {
            material.setTexture("FoamMap", foamTexture);
        }
    }

    /**
     * Gets the foam texture.
     *
     * @return the foam texture.
     */
    public Texture2D getFoamTexture() {
        return foamTexture;
    }

    /**
     * Sets the height texture
     *
     * @param heightTexture the texture to use (alias created)
     */
    public void setHeightTexture(Texture2D heightTexture) {
        this.heightTexture = heightTexture;
        heightTexture.setWrap(WrapMode.Repeat);
        if (material != null) {
            material.setTexture("HeightMap", heightTexture);
        }
    }

    /**
     * Gets the height texture.
     *
     * @return the height texture.
     */
    public Texture2D getHeightTexture() {
        return heightTexture;
    }

    /**
     * Sets the normal texture.
     *
     * @param normalTexture the normal texture.
     */
    public void setNormalTexture(Texture2D normalTexture) {
        this.normalTexture = normalTexture;
        normalTexture.setWrap(WrapMode.Repeat);
        if (material != null) {
            material.setTexture("NormalMap", normalTexture);
        }
    }

    /**
     * Gets the normal texture.
     *
     * @return the normal texture.
     */
    public Texture2D getNormalTexture() {
        return normalTexture;
    }

    /**
     * return the shininess factor of the water
     * @return the shininess factor
     */
    public float getShininess() {
        return shininess;
    }

    /**
     * Sets the shininess factor of the water
     * default is 0.7f
     *
     * @param shininess the desired factor (default=0.7)
     */
    public void setShininess(float shininess) {
        this.shininess = shininess;
        if (material != null) {
            material.setFloat("Shininess", shininess);
        }
    }

    /**
     * returns the speed of the waves
     * @return the speed value
     */
    public float getSpeed() {
        return speed;
    }

    /**
     * Set the speed of the waves (0.0 is still) default is 1.0
     *
     * @param speed the desired speedup factor (default=1)
     */
    public void setSpeed(float speed) {
        this.speed = speed;
    }

    /**
     * returns the color of the water
     *
     * @return the pre-existing instance
     */
    public ColorRGBA getWaterColor() {
        return waterColor;
    }

    /**
     * Sets the color of the water
     * see setDeepWaterColor for deep water color
     * default is (0.0078f, 0.3176f, 0.5f,1.0f) (greenish blue)
     *
     * @param waterColor the color to use (alias created,
     * default=(0.0078,0.3176,0.5,1))
     */
    public void setWaterColor(ColorRGBA waterColor) {
        this.waterColor = waterColor;
        if (material != null) {
            material.setColor("WaterColor", waterColor);
        }
    }

    /**
     * returns the deep water color
     * @return the pre-existing instance
     */
    public ColorRGBA getDeepWaterColor() {
        return deepWaterColor;
    }

    /**
     * sets the deep water color
     * see setWaterColor for general color
     * default is (0.0039f, 0.00196f, 0.145f,1.0f) (very dark blue)
     *
     * @param deepWaterColor the color to use (alias created,
     * default=(0.0039,0.00196,0.145,1))
     */
    public void setDeepWaterColor(ColorRGBA deepWaterColor) {
        this.deepWaterColor = deepWaterColor;
        if (material != null) {
            material.setColor("DeepWaterColor", deepWaterColor);
        }
    }

    /**
     * returns the wind direction
     * @return the pre-existing direction vector
     */
    public Vector2f getWindDirection() {
        return windDirection;
    }

    /**
     * sets the wind direction
     * the direction where the waves move
     * default is (0.0f, -1.0f)
     *
     * @param windDirection the direction vector to use (alias created,
     * default=(0,-1))
     */
    public void setWindDirection(Vector2f windDirection) {
        this.windDirection = windDirection;
        if (material != null) {
            material.setVector2("WindDirection", windDirection);
        }
    }

    /**
     * returns the size of the reflection map
     * @return the size (in pixels)
     */
    public int getReflectionMapSize() {
        return reflectionMapSize;
    }

    /**
     * Sets the size of the reflection map
     * default is 512, the higher, the better quality, but the slower the effect.
     *
     * @param reflectionMapSize the desired size (in pixels per side,
     * default=512)
     */
    public void setReflectionMapSize(int reflectionMapSize) {
        this.reflectionMapSize = reflectionMapSize;
        //if reflection pass is already initialized we must update it
        if(reflectionPass !=  null){
            reflectionPass.init(renderManager.getRenderer(), reflectionMapSize, reflectionMapSize, Format.RGBA8, Format.Depth);
            reflectionCam.resize(reflectionMapSize, reflectionMapSize, true);
            reflectionProcessor.setReflectionBuffer(reflectionPass.getRenderFrameBuffer());
            material.setTexture("ReflectionMap", reflectionPass.getRenderedTexture());
        }

    }

    /**
     * Whether or not the water uses foam
     * @return true if the water uses foam
     */
    public boolean isUseFoam() {
        return useFoam;
    }

    /**
     * set to true to use foam with water
     * default true
     *
     * @param useFoam true for foam, false for no foam (default=true)
     */
    public void setUseFoam(boolean useFoam) {
        this.useFoam = useFoam;
        if (material != null) {
            material.setBoolean("UseFoam", useFoam);
        }

    }

    /**
     * Sets the texture to use to render caustics on the ground underwater.
     *
     * @param causticsTexture the caustics texture.
     */
    public void setCausticsTexture(Texture2D causticsTexture) {
        this.causticsTexture = causticsTexture;
        if (material != null) {
            material.setTexture("causticsMap", causticsTexture);
        }
    }

    /**
     * Gets the texture which is used to render caustics on the ground underwater.
     *
     * @return the caustics texture.
     */
    public Texture2D getCausticsTexture() {
        return causticsTexture;
    }

    /**
     * Whether or not caustics are rendered
     * @return true if caustics are rendered
     */
    public boolean isUseCaustics() {
        return useCaustics;
    }

    /**
     * set to true if you want caustics to be rendered on the ground underwater, false otherwise
     *
     * @param useCaustics true to enable rendering fo caustics, false to disable
     * it (default=true)
     */
    public void setUseCaustics(boolean useCaustics) {
        this.useCaustics = useCaustics;
        if (material != null) {
            material.setBoolean("UseCaustics", useCaustics);
        }
    }

    /**
     * Whether or not the shader is set to use high-quality shoreline.
     * @return true if high-quality shoreline is enabled
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
     * Whether or not the water uses the refraction
     * @return true if the water uses refraction
     */
    public boolean isUseRefraction() {
        return useRefraction;
    }

    /**
     * set to true to use refraction (default is true)
     *
     * @param useRefraction true to enable refraction, false to disable it
     * (default=true)
     */
    public void setUseRefraction(boolean useRefraction) {
        this.useRefraction = useRefraction;
        if (material != null) {
            material.setBoolean("UseRefraction", useRefraction);
        }

    }

    /**
     * Whether or not the water uses ripples
     * @return true if the water is set to use ripples
     */
    public boolean isUseRipples() {
        return useRipples;
    }

    /**
     * 
     * Set to true to use ripples
     *
     * @param useRipples true to enable ripples, false to disable them
     * (default=true)
     */
    public void setUseRipples(boolean useRipples) {
        this.useRipples = useRipples;
        if (material != null) {
            material.setBoolean("UseRipples", useRipples);
        }

    }

    /**
     * Whether or not the water is using specular
     * @return true if the water is set to use specular
     */
    public boolean isUseSpecular() {
        return useSpecular;
    }

    /**
     * Set to true to use specular lighting on the water
     *
     * @param useSpecular true to enable the specular effect, false to disable
     * it (default=true)
     */
    public void setUseSpecular(boolean useSpecular) {
        this.useSpecular = useSpecular;
        if (material != null) {
            material.setBoolean("UseSpecular", useSpecular);
        }
    }

    /**
     * returns the foam intensity
     * @return the intensity value
     */
    public float getFoamIntensity() {
        return foamIntensity;
    }

    /**
     * sets the foam intensity default is 0.5f
     *
     * @param foamIntensity the desired intensity (default=0.5)
     */
    public void setFoamIntensity(float foamIntensity) {
        this.foamIntensity = foamIntensity;
        if (material != null) {
            material.setFloat("FoamIntensity", foamIntensity);

        }
    }

    /**
     * returns the reflection displace
     * see {@link #setReflectionDisplace(float) }
     * @return the displacement value
     */
    public float getReflectionDisplace() {
        return reflectionDisplace;
    }

    /**
     * Sets the reflection displace. define how troubled will look the reflection in the water. default is 30
     *
     * @param reflectionDisplace the desired displacement (default=30)
     */
    public void setReflectionDisplace(float reflectionDisplace) {
        this.reflectionDisplace = reflectionDisplace;
        if (material != null) {
            material.setFloat("ReflectionDisplace", reflectionDisplace);
        }
    }

    /**
     * Whether or not the camera is under the water level
     * @return true if the camera is under the water level
     */
    public boolean isUnderWater() {
        return underWater;
    }

    /**
     * returns the distance of the fog when underwater
     * @return the distance
     */
    public float getUnderWaterFogDistance() {
        return underWaterFogDistance;
    }

    /**
     * Sets the distance of the fog when underwater.
     * Default is 120 (120 world units). Use a high value to raise the view range underwater.
     *
     * @param underWaterFogDistance the desired distance (in world units,
     * default=120)
     */
    public void setUnderWaterFogDistance(float underWaterFogDistance) {
        this.underWaterFogDistance = underWaterFogDistance;
        if (material != null) {
            material.setFloat("UnderWaterFogDistance", underWaterFogDistance);
        }
    }

    /**
     * Gets the intensity of caustics underwater
     * @return the intensity value (&ge;0, &le;1)
     */
    public float getCausticsIntensity() {
        return causticsIntensity;
    }

    /**
     * Sets the intensity of caustics underwater. Goes from 0 to 1, default is 0.5.
     *
     * @param causticsIntensity the desired intensity (&ge;0, &le;1,
     * default=0.5)
     */
    public void setCausticsIntensity(float causticsIntensity) {
        this.causticsIntensity = causticsIntensity;
        if (material != null) {
            material.setFloat("CausticsIntensity", causticsIntensity);
        }
    }

    /**
     * returns the center of this effect
     * @return the center of this effect
     */
    public Vector3f getCenter() {
        return center;
    }

    /**
     * Set the center of the effect.
     * By default, the water will extend across the entire scene.
     * By setting a center and a radius you can restrain it to a portion of the scene.
     * @param center the center of the effect
     */
    public void setCenter(Vector3f center) {
        this.center = center;
        if (material != null) {
            material.setVector3("Center", center);
        }
    }

    /**
     * returns the radius of this effect
     * @return the radius of this effect
     */
    public float getRadius() {
        return radius;

    }

    /**
     * Set the radius of the effect.
     * By default, the water will extend across the entire scene.
     * By setting a center and a radius you can restrain it to a portion of the scene.
     * @param radius the radius of the effect
     */
    public void setRadius(float radius) {
        this.radius = radius;
        if (material != null) {
            material.setFloat("Radius", radius * radius);
        }
    }

    /**
     * returns the shape of the water area
     * @return the shape of the water area
     */
    public AreaShape getShapeType() {
        return shapeType;
    }

    /**
     * Set the shape of the water area (Circular (default) or Square).
     * if the shape is square the radius is considered as an extent.
     * @param shapeType the shape type
     */
    public void setShapeType(AreaShape shapeType) {
        this.shapeType = shapeType;
        if (material != null) {
            material.setBoolean("SquareArea", shapeType==AreaShape.Square);
        }
    }

    @Override
    public Object jmeClone() {
        try {
            return super.clone();
        } catch (final CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void cloneFields(final Cloner cloner, final Object original) {
        this.normalTexture = cloner.clone(normalTexture);
        this.foamTexture = cloner.clone(foamTexture);
        this.causticsTexture = cloner.clone(causticsTexture);
        this.heightTexture = cloner.clone(heightTexture);
        this.targetLocation = cloner.clone(targetLocation);
        this.biasMatrix = cloner.clone(biasMatrix);
        this.textureProjMatrix = cloner.clone(textureProjMatrix);
        this.lightDirection = cloner.clone(lightDirection);
        this.lightColor = cloner.clone(lightColor);
        this.waterColor = cloner.clone(waterColor);
        this.deepWaterColor = cloner.clone(deepWaterColor);
        this.colorExtinction = cloner.clone(colorExtinction);
        this.foamExistence = cloner.clone(foamExistence);
        this.windDirection = cloner.clone(windDirection);
    }

    /**
     * Sets the flag.
     *
     * @param needSaveReflectionScene true if need to save reflection scene.
     */
    public void setNeedSaveReflectionScene(final boolean needSaveReflectionScene) {
        this.needSaveReflectionScene = needSaveReflectionScene;
    }

    /**
     * @return true if need to save reflection scene.
     */
    public boolean isNeedSaveReflectionScene() {
        return needSaveReflectionScene;
    }
}