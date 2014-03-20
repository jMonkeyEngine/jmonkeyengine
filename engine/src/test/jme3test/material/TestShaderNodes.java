package jme3test.material;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.material.Technique;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.shader.Shader;
import com.jme3.texture.Texture;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TestShaderNodes extends SimpleApplication {

    public static void main(String[] args) {
        TestShaderNodes app = new TestShaderNodes();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        flyCam.setMoveSpeed(20);
        Logger.getLogger("com.jme3").setLevel(Level.WARNING);
        Box boxshape1 = new Box(1f, 1f, 1f);
        Geometry cube_tex = new Geometry("A Textured Box", boxshape1);
        Texture tex = assetManager.loadTexture("Interface/Logo/Monkey.jpg");

        Material mat = new Material(assetManager, "Common/MatDefs/Misc/UnshadedNodes.j3md");
        mat.selectTechnique("Default", renderManager);
        Technique t = mat.getActiveTechnique();

        for (Shader.ShaderSource shaderSource : t.getShader().getSources()) {
            System.out.println(shaderSource.getSource());
        }

        
        mat.setColor("Color", ColorRGBA.Yellow);
        mat.setTexture("ColorMap", tex);
        cube_tex.setMaterial(mat);
        rootNode.attachChild(cube_tex);
    }
}
