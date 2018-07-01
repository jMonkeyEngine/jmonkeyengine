package jme3test.material;

import com.jme3.app.SimpleApplication;
import com.jme3.material.*;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.shape.Box;
import com.jme3.shader.*;
import com.jme3.shader.builder.MaterialBuilder;
import com.jme3.texture.Texture;

import java.util.logging.Level;
import java.util.logging.Logger;

public class TestShaderNodesModifApi extends SimpleApplication {

    public static void main(String[] args) {
        TestShaderNodesModifApi app = new TestShaderNodesModifApi();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        flyCam.setMoveSpeed(20);
        Logger.getLogger("com.jme3").setLevel(Level.WARNING);
        Box boxshape1 = new Box(1f, 1f, 1f);
        Geometry cube = new Geometry("A Box", boxshape1);
        Texture tex = assetManager.loadTexture("Interface/Logo/Monkey.jpg");

        MaterialBuilder mb = new MaterialBuilder(assetManager,"jme3test/matdefs/test.j3md");
        mb.addMatParam(VarType.Vector4, "Color2");
        mb.addMatParam(VarType.Texture2D, "Texture");

        mb.technique().inlineVertexNode("vec2","TexCoord", "%texIn")
                .inputs(
                        mb.map("texIn", VertexBuffer.Type.TexCoord)
                );


        mb.technique().inlineFragmentNode("vec4","TextureFetch","texture2D(%tex, %texCoord)")
                .inputs(
                        mb.map("tex", "MatParam.Texture"),
                        mb.map("texCoord", "TexCoord.result")
                ).outputs(
                        mb.map("result", "Global.color")
                );


        mb.technique().addNode("ColorMult2", "ColorMult", "jme3test/matdefs/ColorMult.frag")
        .inputs(
                mb.map("color1", "ColorMult.result"),
                mb.map("color2", "MatParam.Color2"),
                mb.map("color3", "TextureFetch.result"))
        .outputs(
                mb.map("result", "Global.color")
        );

        // TODO we need a way to order the nodes. They could be sorted by scanning the inputs and outputs and building a node tree, then sort it with a topological sort,
        // but that won't work in some cases when node output is the Global color or Global position. So we'd need a fallback to manually order the nodes.
        // So as long as we implement the fallback... The sort is maybe not worth it.
        // API could be mb.technique().moveNode("Node1").before("Node2") or after("Node2")...
        // or something like mb.technique().setOrder("Node1", "Node2", etc...) but this would require the user to know all the existing nodes.
        // or maybe both...

        Material mat = mb.build();

        mat.selectTechnique(TechniqueDef.DEFAULT_TECHNIQUE_NAME, renderManager);
        Technique t = mat.getActiveTechnique();

        for (Shader.ShaderSource shaderSource : t.getDef().getShader(assetManager, renderer.getCaps(), t.getDynamicDefines()).getSources()) {
            System.out.println(shaderSource.getSource());
        }

        mat.setColor("Color", ColorRGBA.Yellow);
        mat.setColor("Color2", ColorRGBA.Red);
        mat.setTexture("Texture", tex);
        cube.setMaterial(mat);
        cube.move(0, 0, 0);
        rootNode.attachChild(cube);


    }
}
