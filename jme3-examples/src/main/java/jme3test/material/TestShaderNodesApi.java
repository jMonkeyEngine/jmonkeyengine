package jme3test.material;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.material.Technique;
import com.jme3.material.TechniqueDef;
import com.jme3.math.*;
import com.jme3.scene.Geometry;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.shape.Box;
import com.jme3.shader.*;
import com.jme3.shader.builder.MaterialBuilder;
import com.jme3.texture.Texture;

import java.util.logging.Level;
import java.util.logging.Logger;

public class TestShaderNodesApi extends SimpleApplication {

    public static void main(String[] args) {
        TestShaderNodesApi app = new TestShaderNodesApi();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        flyCam.setMoveSpeed(20);
        Logger.getLogger("com.jme3").setLevel(Level.WARNING);
        Box boxshape1 = new Box(1f, 1f, 1f);
        Geometry cube = new Geometry("A Box", boxshape1);
        Texture tex = assetManager.loadTexture("Interface/Logo/Monkey.jpg");

        MaterialBuilder mb = new MaterialBuilder(assetManager);
        mb.addMatParam(VarType.Vector4, "Color");
        mb.addMatParam(VarType.Texture2D, "Texture");

        mb.technique().addNode("CommonVert", "CommonVert", "jme3test/matdefs/CommonVert.vert")
                .inputs(
                        mb.map("worldViewProjectionMatrix", UniformBinding.WorldViewProjectionMatrix),
                        mb.map("modelPosition", VertexBuffer.Type.Position))
                .outputs(
                        mb.map("result", "Global.position")
                );

        mb.technique().inlineVertexNode("vec2","TexCoord", "%texIn")
                .inputs(
                        mb.map("texIn", VertexBuffer.Type.TexCoord)
                );

        mb.technique().addNode("ColorMult", "ColorMult", "jme3test/matdefs/ColorMult.frag")
                .inputs(
                        mb.map("color1", "vec4(0.1, 0.1, 0.1, 1.0)"),
                        mb.map("color2", "MatParam.Color"))
                .outputs(
                        mb.map("result", "Global.color")
                );

        mb.technique().inlineFragmentNode("vec4","InlineNode","%color1 * texture2D(%tex, %texCoord)")
                .inputs(
                        mb.map("color1", "ColorMult.result"),
                        mb.map("tex", "MatParam.Texture"),
                        mb.map("texCoord", "TexCoord.result")
                ).outputs(
                        mb.map("result", "Global.color")
                );

        Material mat = mb.build();

        //Material mat = new Material(assetManager, "jme3test/matdefs/test2.j3md");

        mat.selectTechnique(TechniqueDef.DEFAULT_TECHNIQUE_NAME, renderManager);
        Technique t = mat.getActiveTechnique();

        for (Shader.ShaderSource shaderSource : t.getDef().getShader(assetManager, renderer.getCaps(), t.getDynamicDefines()).getSources()) {
            System.out.println(shaderSource.getSource());
        }

        mat.setColor("Color", ColorRGBA.Yellow);
        mat.setTexture("Texture", tex);
        cube.setMaterial(mat);
        cube.move(0, 0, 0);
        rootNode.attachChild(cube);


    }
}
