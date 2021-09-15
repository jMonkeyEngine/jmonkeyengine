package jme3test.material;

import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.shape.Quad;
import com.jme3.system.AppSettings;
import com.jme3.util.BufferUtils;

import java.util.concurrent.Callable;

/**
 * Created by michael on 28.02.15.
 */
public class TestTessellationShader extends SimpleApplication {
    private Material tessellationMaterial;
    private int tessFactor=5;
    @Override
    public void simpleInitApp() {
        tessellationMaterial = new Material(getAssetManager(), "Materials/Tess/SimpleTess.j3md");
        tessellationMaterial.setInt("TessellationFactor", tessFactor);
        tessellationMaterial.getAdditionalRenderState().setWireframe(true);
        Quad quad = new Quad(10, 10);
        quad.clearBuffer(VertexBuffer.Type.Index);
        quad.setBuffer(VertexBuffer.Type.Index, 4, BufferUtils.createIntBuffer(0, 1, 2, 3));
        quad.setMode(Mesh.Mode.Patch);
        quad.setPatchVertexCount(4);
        Geometry geometry = new Geometry("tessTest", quad);
        geometry.setMaterial(tessellationMaterial);
        rootNode.attachChild(geometry);

        getInputManager().addMapping("TessUp", new KeyTrigger(KeyInput.KEY_O));
        getInputManager().addMapping("TessDo", new KeyTrigger(KeyInput.KEY_L));
        getInputManager().addListener(new AnalogListener() {
            @Override
            public void onAnalog(String name, float value, float tpf) {
                if(name.equals("TessUp")){
                    tessFactor++;
                    enqueue(new Callable<Boolean>() {
                        @Override
                        public Boolean call() throws Exception {
                            tessellationMaterial.setInt("TessellationFactor",tessFactor);
                            return true;
                        }
                    });
                }
                if(name.equals("TessDo")){
                    tessFactor--;
                    enqueue(new Callable<Boolean>() {
                        @Override
                        public Boolean call() throws Exception {
                            tessellationMaterial.setInt("TessellationFactor",tessFactor);
                            return true;
                        }
                    });
                }
            }
        },"TessUp","TessDo");
    }

    public static void main(String[] args) {
        TestTessellationShader app = new TestTessellationShader();
        AppSettings settings = new AppSettings(true);
        settings.setRenderer(AppSettings.LWJGL_OPENGL40);
        app.setSettings(settings);
        app.start();
    }
}
