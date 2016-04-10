package jme3tools.shadercheck;

import com.jme3.asset.AssetManager;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.material.MaterialDef;
import com.jme3.material.TechniqueDef;
import com.jme3.material.plugins.J3MLoader;
import com.jme3.renderer.Caps;
import com.jme3.shader.DefineList;
import com.jme3.shader.Shader;
import com.jme3.shader.plugins.GLSLLoader;
import com.jme3.system.JmeSystem;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ShaderCheck {
    
    private static final Logger logger = Logger.getLogger(ShaderCheck.class.getName());
    private static AssetManager assetManager;
    
    private static Validator[] validators = new Validator[]{
        new CgcValidator(),
//        new GpuAnalyzerValidator()
    };
    
    private static void initAssetManager(){
        assetManager = JmeSystem.newAssetManager();
        assetManager.registerLocator(".", FileLocator.class);
        assetManager.registerLocator("/", ClasspathLocator.class);
        assetManager.registerLoader(J3MLoader.class, "j3m");
        assetManager.registerLoader(J3MLoader.class, "j3md");
        assetManager.registerLoader(GLSLLoader.class, "vert", "frag","geom","tsctrl","tseval","glsllib");
    }
    
    private static void checkMatDef(String matdefName) {
        MaterialDef def = (MaterialDef) assetManager.loadAsset(matdefName);
        EnumSet<Caps> rendererCaps = EnumSet.noneOf(Caps.class);
        rendererCaps.add(Caps.GLSL100);
        for (TechniqueDef techDef : def.getTechniqueDefs(TechniqueDef.DEFAULT_TECHNIQUE_NAME)) {
            DefineList defines = techDef.createDefineList();
            Shader shader = techDef.getShader(assetManager, rendererCaps, defines);
            for (Validator validator : validators) {
                StringBuilder sb = new StringBuilder();
                validator.validate(shader, sb);
                System.out.println("==== Validator: " + validator.getName() + " "
                        + validator.getInstalledVersion() + " ====");
                System.out.println(sb.toString());
            }
        }
        throw new UnsupportedOperationException();
    }
          
    public static void main(String[] args){
        Logger.getLogger(MaterialDef.class.getName()).setLevel(Level.OFF);
        initAssetManager();
        checkMatDef("Common/MatDefs/Blur/HGaussianBlur.j3md");
        checkMatDef("Common/MatDefs/Blur/RadialBlur.j3md");
        checkMatDef("Common/MatDefs/Blur/VGaussianBlur.j3md");
        checkMatDef("Common/MatDefs/Gui/Gui.j3md");
        checkMatDef("Common/MatDefs/Hdr/LogLum.j3md");
        checkMatDef("Common/MatDefs/Hdr/ToneMap.j3md");
        checkMatDef("Common/MatDefs/Light/Lighting.j3md");
        checkMatDef("Common/MatDefs/Misc/ColoredTextured.j3md");
        checkMatDef("Common/MatDefs/Misc/Particle.j3md");
        checkMatDef("Common/MatDefs/Misc/ShowNormals.j3md");
        checkMatDef("Common/MatDefs/Misc/Sky.j3md");
        checkMatDef("Common/MatDefs/Misc/Unshaded.j3md");
        
        checkMatDef("Common/MatDefs/Post/BloomExtract.j3md");
        checkMatDef("Common/MatDefs/Post/BloomFinal.j3md");
        checkMatDef("Common/MatDefs/Post/CartoonEdge.j3md");
        checkMatDef("Common/MatDefs/Post/CrossHatch.j3md");
        checkMatDef("Common/MatDefs/Post/DepthOfField.j3md");
        checkMatDef("Common/MatDefs/Post/FXAA.j3md");
        checkMatDef("Common/MatDefs/Post/Fade.j3md");
        checkMatDef("Common/MatDefs/Post/Fog.j3md");
        checkMatDef("Common/MatDefs/Post/GammaCorrection.j3md");
        checkMatDef("Common/MatDefs/Post/LightScattering.j3md");
        checkMatDef("Common/MatDefs/Post/Overlay.j3md");
        checkMatDef("Common/MatDefs/Post/Posterization.j3md");
        
        checkMatDef("Common/MatDefs/SSAO/ssao.j3md");
        checkMatDef("Common/MatDefs/SSAO/ssaoBlur.j3md");
        checkMatDef("Common/MatDefs/Shadow/PostShadow.j3md");
        checkMatDef("Common/MatDefs/Shadow/PostShadowPSSM.j3md");
        checkMatDef("Common/MatDefs/Shadow/PreShadow.j3md");
        
        checkMatDef("Common/MatDefs/Water/SimpleWater.j3md");
        checkMatDef("Common/MatDefs/Water/Water.j3md");
    }
}
