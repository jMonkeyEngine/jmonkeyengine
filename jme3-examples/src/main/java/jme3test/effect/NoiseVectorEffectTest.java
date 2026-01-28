package jme3test.effect;

import com.jme3.app.SimpleApplication;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.FastNoiseLite;
import com.jme3.math.FastNoiseLite.NoiseType;
import com.jme3.math.Vector2f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;
import com.jme3.system.AppSettings;
import com.jme3.vectoreffect.NoiseVectorEffect;
import com.jme3.vectoreffect.VectorEffectManagerState;
import com.jme3.vectoreffect.VectorGroup;
import com.jme3.vectoreffect.VectorSupplier;

public class NoiseVectorEffectTest extends SimpleApplication {

    private ColorRGBA colorToShift  = new ColorRGBA(0.0f, 0.0f, 1.0f, 1.0f);;
    
    private VectorEffectManagerState vectorEffectManagerState;    
    private PointLight light;
    
    private Vector2f lightRadiusVector = new Vector2f();
    
    public static void main(String[] args) {
        NoiseVectorEffectTest app = new NoiseVectorEffectTest();
        AppSettings settings = new AppSettings(true);
        app.setSettings(settings);
        app.start();
    }
   

    @Override
    public void simpleInitApp() {
        flyCam.setMoveSpeed(10f);
        
        vectorEffectManagerState = new VectorEffectManagerState();
        stateManager.attach(vectorEffectManagerState);
        
        initBloom();
        initPbrRoom(13);
        
        initLightAndEmissiveSphere();
        initShakingSphere();
        
        lightRadiusVector.setX(light.getRadius());
    
     //initiate VectorEffectManagerState    
        vectorEffectManagerState = new VectorEffectManagerState();
        stateManager.attach(vectorEffectManagerState);
        vectorEffectManagerState.setEnabled(true);        

    //create noise effect : 
        VectorGroup vectorsToModify = new VectorGroup(VectorSupplier.of(colorToShift), VectorSupplier.of(lightRadiusVector));
        VectorGroup noiseMagnitudes = new VectorGroup(colorToShift.toVector4f().negate());
        
        NoiseVectorEffect noiseVectorEffect = new NoiseVectorEffect(vectorsToModify, noiseMagnitudes);
        
        noiseVectorEffect.getNoiseGenerator().SetFrequency(0.95f); // Determines the overall scale of the noise. Higher values produce more detailed, rapidly changing patterns. Lower values produce larger, smoother patterns.
        
    //choose a noise type:
        noiseVectorEffect.getNoiseGenerator().SetNoiseType(NoiseType.Cellular);  // Produces organic cell patterns
      //  noiseVectorEffect.getNoiseGenerator().SetNoiseType(NoiseType.OpenSimplex2);  // Smooth, continuous noise, less directional artifacts than Perlin. 
      //  noiseVectorEffect.getNoiseGenerator().SetNoiseType(NoiseType.OpenSimplex2S); // Variant of OpenSimplex2 with slightly sharper features. Similar smooth natural noise.
      //  noiseVectorEffect.getNoiseGenerator().SetNoiseType(NoiseType.Perlin);       // Classic Perlin noise. Smooth gradient noise, can show directional artifacts in some cases.
      //  noiseVectorEffect.getNoiseGenerator().SetNoiseType(NoiseType.Value);        // Piecewise linear noise. Blockier, grid-like structures, less smooth.
      //  noiseVectorEffect.getNoiseGenerator().SetNoiseType(NoiseType.ValueCubic);   // Smooth interpolation of Value noise. Smoother than plain Value noise, less natural than simplex types.

    // (optional) choose a fractal type for more detail/complexity/chaos
        noiseVectorEffect.getNoiseGenerator().SetFractalType(FastNoiseLite.FractalType.Ridged); // Produces sharp ridges and high-contrast peaks.
      //  noiseVectorEffect.getNoiseGenerator().SetFractalType(FastNoiseLite.FractalType.DomainWarpIndependent);  // Applies domain warping independently per octave, creating twisted, chaotic patterns.
      //  noiseVectorEffect.getNoiseGenerator().SetFractalType(FastNoiseLite.FractalType.DomainWarpProgressive);   // Applies domain warping progressively, later octaves warp the results of earlier octaves. Creates flowing, warped patterns.
      //  noiseVectorEffect.getNoiseGenerator().SetFractalType(FastNoiseLite.FractalType.FBm);  // Standard fractional Brownian motion. Smooth additive fractal layers. 
      //  noiseVectorEffect.getNoiseGenerator().SetFractalType(FastNoiseLite.FractalType.PingPong);  // Folds and mirrors noise across octaves, increasing contrast and sharp detail in bouncy, mirrored patterns.

    //configure fractal params:
        noiseVectorEffect.getNoiseGenerator().SetFractalGain(0.5f); // Gain Determines how much each successive octave contributes. higher fractal gain values result in more chaotic / less smooth effect
        noiseVectorEffect.getNoiseGenerator().SetFractalLacunarity(2f);// Higher Lacunarity values make higher octaves "faster" (more detail / rougher), lower values make them "slower" (broader, smoother patterns)        
        noiseVectorEffect.getNoiseGenerator().SetFractalOctaves(5); // Number of noise layers to combine. Higher octave count results in more detail but requires more computations. Lower octave count results in smoother simpler patterns.
        noiseVectorEffect.getNoiseGenerator().SetFractalWeightedStrength(0.1f);  // Controls how much higher octaves contribute when lower octaves are already strong.    
        
      //  noiseVectorEffect.getNoiseGenerator().SetFractalPingPongStrength(speed);// Controls how strong the ping‑pong effect is when using the PingPong fractal type.    
 
        vectorEffectManagerState.registerVectorEffect(noiseVectorEffect);        
    }
    
    

    @Override
    public void simpleUpdate(float tpf) {
        super.simpleUpdate(tpf);
        
        //float values like a light's radius cannot be altered by refernce like a vector object, 
        //so the float value must be manually extracted from the x component of a vector2f and set as radius every from
        if(lightRadiusVector != null){ 
           light.setRadius(lightRadiusVector.getX());
        }
    }
    
    private void initBloom() {
        
        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        BloomFilter bloom = new BloomFilter(BloomFilter.GlowMode.Scene);
        bloom.setBloomIntensity(5f);
        bloom.setExposurePower(4.5f);
        bloom.setExposureCutOff(0.2f);
        bloom.setBlurScale(2);
        fpp.addFilter(bloom);
        viewPort.addProcessor(fpp);
    }

    private void initLightAndEmissiveSphere(){
      

      //make point light
        light = new PointLight();
        light.setRadius(10);
        colorToShift = light.getColor();
        colorToShift.set(1.0f,0.97f, 0.63f, 1.0f);
        
      //make sphere with Emissive color  
        Sphere sphereMesh = new Sphere(32, 32, 0.5f);
        Geometry glowingSphere = new Geometry("ShakingSphere", sphereMesh);

        Material sphereMat = new Material(assetManager, "Common/MatDefs/Light/PBRLighting.j3md");
        sphereMat.setColor("BaseColor", ColorRGBA.DarkGray);
        sphereMat.setFloat("Roughness", 0.04f);
        sphereMat.setFloat("Metallic", 0.98f);
        sphereMat.setFloat("EmissivePower", 0.1f);
        sphereMat.setFloat("EmissiveIntensity", 0.3f);
        sphereMat.setBoolean("UseVertexColor", false);
        glowingSphere.setMaterial(sphereMat);
        
        
        //assign the same colorToShift vector to both the light and emissive value (important not to clone)
        light.setColor(colorToShift);        
        sphereMat.setColor("Emissive", colorToShift);  
        
        
        rootNode.attachChild(glowingSphere);          
        rootNode.addLight(light);
        
        
    }

    private void initShakingSphere() {
        Sphere mesh = new Sphere(16, 16, 0.5f);
        Geometry shakingSphere = new Geometry("ShakingSphere", mesh);

        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Diffuse", ColorRGBA.Blue);
        mat.setColor("Specular", ColorRGBA.White);
        mat.setFloat("Shininess", 32f);

        shakingSphere.setMaterial(mat);
        shakingSphere.setLocalTranslation(2, 0.5f, 0);
        rootNode.attachChild(shakingSphere);
    }


    
    public void initPbrRoom(float size) {
        float half = size * 0.5f;
        
        Material wallMat = new Material(assetManager, "Common/MatDefs/Light/PBRLighting.j3md");

        wallMat.setColor("BaseColor", new ColorRGBA(1,1,1, 1f));
        wallMat.setFloat("Roughness", 0.12f);
        wallMat.setFloat("Metallic", 0.02f);

        // Floor
        Geometry floor = new Geometry("Floor",
                new Quad(size, size));
        floor.setMaterial(wallMat);
        floor.rotate(-FastMath.HALF_PI, 0, 0);
        floor.setLocalTranslation(-half, -half, half);
        rootNode.attachChild(floor);

        // Ceiling
        Geometry ceiling = new Geometry("Ceiling",
                new Quad(size, size));
        ceiling.setMaterial(wallMat);
        ceiling.rotate(FastMath.HALF_PI, 0, 0);
        ceiling.setLocalTranslation(-half, size-half, -half);
        rootNode.attachChild(ceiling);

        // Back wall
        Geometry backWall = new Geometry("BackWall",
                new Quad(size, size));
        backWall.setMaterial(wallMat);
        backWall.setLocalTranslation(-half, -half, -half);
        rootNode.attachChild(backWall);

        // Left wall
        Geometry leftWall = new Geometry("LeftWall",
                new Quad(size, size));
        leftWall.setMaterial(wallMat);
        leftWall.rotate(0, FastMath.HALF_PI, 0);
        leftWall.setLocalTranslation(-half, -half, half);
        rootNode.attachChild(leftWall);

        // Right wall
        Geometry rightWall = new Geometry("RightWall",
                new Quad(size, size));
        rightWall.setMaterial(wallMat);
        rightWall.rotate(0, -FastMath.HALF_PI, 0);
        rightWall.setLocalTranslation(half, -half, -half);
        rootNode.attachChild(rightWall);

    }
    
    
}