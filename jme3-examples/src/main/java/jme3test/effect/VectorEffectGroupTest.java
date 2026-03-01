package jme3test.effect;

import com.jme3.app.SimpleApplication;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Easing;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;
import com.jme3.system.AppSettings;
import com.jme3.vectoreffect.EaseVectorEffect;
import com.jme3.vectoreffect.VectorEffectManagerState;
import com.jme3.vectoreffect.VectorGroup;
import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

public class VectorEffectGroupTest extends SimpleApplication {

    private ColorRGBA lightColor  = new ColorRGBA(1.0f, 0.7f, 0.0f, 1.0f);
    private ColorRGBA emissiveColor = new ColorRGBA(0.27f, 0.24f, 0.0f, 1.0f);
    private ColorRGBA particleStartColor = new ColorRGBA(0.3f, 0.275f, 0.0f, 0.9f);
    private ColorRGBA particleEndColor = new ColorRGBA(0.6f, 0.5f, 0.2f, 0.001f);
    
    private VectorGroup vectorsToModify, originalVectorValues;    
    
    private VectorEffectManagerState vectorEffectManagerState;
    
    public static void main(String[] args) {
        VectorEffectGroupTest app = new VectorEffectGroupTest();
        AppSettings settings = new AppSettings(true);
        app.setSettings(settings);
        app.start();
    }    

    @Override
    public void simpleInitApp() {
        flyCam.setMoveSpeed(10f);
        
        vectorEffectManagerState = new VectorEffectManagerState();
        stateManager.attach(vectorEffectManagerState);
        
        initHudText();
        initInput();
        initPbrRoom(13);
        
        initLightAndEmissiveSphere();
        initParticleEmitter();
    
     //initiate VectorEffectManagerState    
        vectorEffectManagerState = new VectorEffectManagerState();
        stateManager.attach(vectorEffectManagerState);
        vectorEffectManagerState.setEnabled(true);        
        
        vectorsToModify = new VectorGroup(lightColor, emissiveColor, particleStartColor, particleEndColor);
        
        originalVectorValues = vectorsToModify.clone();

    }
    
    private final ActionListener actionListener = new ActionListener() {
        @Override
        public void onAction(String name, boolean isPressed, float tpf) {
            if (!isPressed) return; // trigger only on key down
            
            switch (name) {
                case "FadeOut":
                    vectorEffectManagerState.cancelEffects(vectorsToModify);
                    vectorEffectManagerState.registerVectorEffect(new EaseVectorEffect(vectorsToModify, new VectorGroup(ColorRGBA.Black), 2.5f, Easing.inOutQuad));
                    break;

                case "FadeIn":
                    vectorEffectManagerState.cancelEffects(vectorsToModify);
                    vectorEffectManagerState.registerVectorEffect( new EaseVectorEffect(vectorsToModify, originalVectorValues, 2.75f, Easing.inOutQuad));
                    break;

                case "Cancel":
                    vectorEffectManagerState.cancelEffects(vectorsToModify);
                    break;
            }
        }
    };

    private void initParticleEmitter(){
        /** Uses Texture from jme3-test-data library! */
        ParticleEmitter fireEffect = new ParticleEmitter("Emitter", ParticleMesh.Type.Triangle, 30);
        Material fireMat = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
        fireMat.setTexture("Texture", assetManager.loadTexture("Effects/Explosion/flame.png"));
        fireEffect.setMaterial(fireMat);
        fireEffect.setImagesX(2); fireEffect.setImagesY(2);
        fireEffect.setEndColor( particleEndColor );   
        fireEffect.setStartColor( particleStartColor ); 
        fireEffect.getParticleInfluencer().setInitialVelocity(new Vector3f(0, 2, 0));
        fireEffect.setStartSize(0.85f);
        fireEffect.setEndSize(0.01f);
        fireEffect.setGravity(0f,0f,0f);
        fireEffect.setLowLife(0.75f);
        fireEffect.setHighLife(0.9f);
        fireEffect.setParticlesPerSec(9);
        fireEffect.getParticleInfluencer().setVelocityVariation(0.12f);
        rootNode.attachChild(fireEffect);
                
        particleStartColor = fireEffect.getStartColor();
        particleEndColor = fireEffect.getEndColor();
    }

    private void initLightAndEmissiveSphere(){      
      //make point light
        PointLight light = new PointLight();
        light.setRadius(10);
        light.setColor(lightColor);
        lightColor = light.getColor();
        
      //make sphere with Emissive color  
        Sphere sphereMesh = new Sphere(32, 32, 0.5f);
        Geometry glowingSphere = new Geometry("ShakingSphere", sphereMesh);

        Material sphereMat = new Material(assetManager, "Common/MatDefs/Light/PBRLighting.j3md");
        sphereMat.setColor("BaseColor", ColorRGBA.DarkGray);
        sphereMat.setFloat("Roughness", 0.04f);
        sphereMat.setFloat("Metallic", 0.98f);
        sphereMat.setColor("Emissive", emissiveColor);
        sphereMat.setBoolean("UseVertexColor", false);
        glowingSphere.setMaterial(sphereMat);
        
        rootNode.attachChild(glowingSphere);          
        rootNode.addLight(light);        
        
    }
    
    public void initPbrRoom(float size) {
        float half = size * 0.5f;        
        Material wallMat = new Material(assetManager, "Common/MatDefs/Light/PBRLighting.j3md");

        wallMat.setColor("BaseColor", new ColorRGBA(1,1,1, 1f));
        wallMat.setFloat("Roughness", 0.5f);
        wallMat.setFloat("Metallic", 0.08f);

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
    
    private void initInput() {
        inputManager.addMapping("FadeIn", new KeyTrigger(KeyInput.KEY_1));
        inputManager.addMapping("FadeOut", new KeyTrigger(KeyInput.KEY_2));
        inputManager.addMapping("Cancel", new KeyTrigger(KeyInput.KEY_X));
        inputManager.addListener(actionListener, "FadeIn", "FadeOut", "Cancel");        
    }
    
     private void initHudText() {
        BitmapFont font = assetManager.loadFont(
                "Interface/Fonts/Default.fnt");

        BitmapText helpText = new BitmapText(font);
        helpText.setSize(font.getCharSet().getRenderedSize());
        helpText.setText(
                "Controls:\n" +
                "1  - Fade In \n" +
                "2  - Fade Out \n" +
                "X - Cancel Current Effect"
        );

        // Top-left corner
        helpText.setLocalTranslation(
                10,
                cam.getHeight() - 10,
                0
        );

        guiNode.attachChild(helpText);
    }
}