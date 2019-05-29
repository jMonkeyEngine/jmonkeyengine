package com.jme3.renderer;

import com.jme3.renderer.lwjgl.LwjglGL;
import com.jme3.renderer.opengl.GL;
import com.jme3.renderer.opengl.GLExt;
import com.jme3.renderer.opengl.GLFbo;
import com.jme3.renderer.opengl.GLRenderer;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Set;


public class GLRendererTest {
    public GL gl;
    public GLExt glext;
    public GLFbo glfbo;
    private GLRenderer glRenderer;

    @Before
    public void setup(){
        gl = new LwjglGL();
        glRenderer = new GLRenderer(gl,glext,glfbo);
    }

    @Test
    public void testAlphaCoverage(){


        Set<Caps> caps = glRenderer.getCaps();
        caps.add(Caps.Multisample);

        glRenderer.setAlphaToCoverage(true);


        assert glRenderer.getAlphaToCoverage() == true;
//        glRenderer.setAlphaToCoverage(false);
//        assert glRenderer.getAlphaToCoverage() == false;
    }

}
