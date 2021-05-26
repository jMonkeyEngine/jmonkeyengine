package jme3test.renderer;

import com.jme3.app.SimpleApplication;
import com.jme3.renderer.lwjgl.LwjglGL;
import com.jme3.renderer.lwjgl.LwjglGLExt;
import com.jme3.renderer.lwjgl.LwjglGLFboEXT;
import com.jme3.renderer.opengl.GL;
import com.jme3.renderer.opengl.GLExt;
import com.jme3.renderer.opengl.GLFbo;
import com.jme3.renderer.opengl.GLRenderer;
import com.jme3.texture.Texture;

/**
 * Simple application to test the ArrayIndexOutOfBoundException handling of bindTextureAndUnit()
 * from the GLRenderer class (issue 37). Since bindTextureAndUnit() is a private method, test case runs updateTexImageData()
 * from the GLRenderer class, which is a public method and calls bindTextureAndUnit().
 */

public class TestIssue37 extends SimpleApplication {

    public static void main(String[] args) {
        TestIssue37 app = new TestIssue37();
        app.start();

    }

    final private GL gl = new LwjglGL();
    final private GLExt glext = new LwjglGLExt();
    final private GLFbo glfbo = new LwjglGLFboEXT();
    final private GLRenderer glRenderer= new GLRenderer(gl,glext,glfbo);
    //sets the unit larger than 16, which is the upper bound limit of units due to GL2 limitation
    final private int unit = 16; //base unit = 0
    private static Texture texture;


    @Override
    public void simpleInitApp() {
        texture = assetManager.loadTexture("Textures/Terrain/BrickWall/BrickWall.jpg");
        glRenderer.initialize();

        if(texture != null) {
            //this should catch an ArrayIndexOutOfBoundsException from bindTextureAndUnit()
            glRenderer.updateTexImageData(texture.getImage(), texture.getType(), unit, true);

        }
    }
}