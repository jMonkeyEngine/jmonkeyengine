package jme3test.renderer;
import com.jme3.app.SimpleApplication;

import com.jme3.renderer.lwjgl.LwjglGL;
import com.jme3.renderer.opengl.GL;
import com.jme3.renderer.opengl.GLExt;
import com.jme3.renderer.opengl.GLFbo;
import com.jme3.renderer.opengl.GLRenderer;
import com.jme3.renderer.lwjgl.LwjglGLExt;
import com.jme3.renderer.lwjgl.LwjglGLFboEXT;
import com.jme3.renderer.Caps;

import java.util.EnumSet;

/**
 * Simple application to test the getter and setters of AlphaToCoverage and
 * DefaultAnisotropicFilter from the GLRenderer class.
 *
 * Since the app doesn't display anything relevant a stop() has been added
 * This starts and closes the app on a successful run
 */
public class TestAlphaToCoverage extends SimpleApplication {

    public static void main(String[] args) {
        new TestAlphaToCoverage().start();
    }

    public GL gl = new LwjglGL();
    public GLExt glext = new LwjglGLExt();
    public GLFbo glfbo = new LwjglGLFboEXT();
    private GLRenderer glRenderer= new GLRenderer(gl,glext,glfbo);

    public EnumSet<Caps> caps = glRenderer.getCaps();



    @Override
    public void simpleInitApp() {
        glRenderer.setAlphaToCoverage(false);
        assert !glRenderer.getAlphaToCoverage();

        caps.add(Caps.Multisample);
        glRenderer.setAlphaToCoverage(true);
        assert glRenderer.getAlphaToCoverage();
        glRenderer.setAlphaToCoverage(false);
        assert !glRenderer.getAlphaToCoverage();

        glRenderer.setDefaultAnisotropicFilter(1);
        assert glRenderer.getDefaultAnisotropicFilter() == 1;

        stop();
    }

}
