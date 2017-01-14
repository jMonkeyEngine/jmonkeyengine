package com.jme3.material;

import com.jme3.renderer.RenderManager;
import com.jme3.shader.Shader;
import com.jme3.system.NullRenderer;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * Created by Nehon on 14/01/2017.
 */
public class TestTechniqueDefOrdering {

    @Test
    public void order() {

        RenderManager rm = new RenderManager(new NullRenderer());
        rm.setPreferredLightMode(TechniqueDef.LightMode.MultiPass);
        MaterialDef.TechDefComparator comp = new MaterialDef.TechDefComparator();
        comp.rm = rm;


        //random case
        List<TechniqueDef> defs = new ArrayList<>();
        TechniqueDef def = new TechniqueDef("tech", 1);
        def.setShaderFile("", "", "GLSL100", "GLSL100");
        def.setLightMode(TechniqueDef.LightMode.SinglePass);
        defs.add(def);
        def = new TechniqueDef("tech2", 1);
        def.setShaderFile("", "", "GLSL150", "GLSL150");
        def.setLightMode(TechniqueDef.LightMode.MultiPass);
        defs.add(def);
        def = new TechniqueDef("tech3", 1);
        def.setShaderFile("", "", "GLSL110", "GLSL110");
        defs.add(def);
        def = new TechniqueDef("tech4", 1);
        def.setShaderFile("", "", "GLSL120", "GLSL120");
        defs.add(def);
        def = new TechniqueDef("tech5", 1);
        def.setShaderFile("", "", "GLSL130", "GLSL130");
        defs.add(def);

        Collections.sort(defs, comp);

        assertEquals(defs.get(0).getName(), "tech2");
        assertEquals(defs.get(1).getName(), "tech5");
        assertEquals(defs.get(2).getName(), "tech4");
        assertEquals(defs.get(3).getName(), "tech3");
        assertEquals(defs.get(4).getName(), "tech");


        //Test the unshaded material case: 2 disabled : 150 and 100
        defs = new ArrayList<>();
        def = new TechniqueDef("unshaded", 1);
        def.setShaderFile("", "", "GLSL100", "GLSL100");
        defs.add(def);
        def = new TechniqueDef("unshaded2", 1);
        def.setShaderFile("", "", "GLSL150", "GLSL150");
        defs.add(def);
        Collections.sort(defs, comp);

        assertEquals(defs.get(0).getName(), "unshaded2");
        assertEquals(defs.get(1).getName(), "unshaded");

        //Test the lighting material case: 2 singlepass : 150 and 100, 2 multipass : 150 and 100
        defs = new ArrayList<>();
        def = new TechniqueDef("lighting1", 1);
        def.setShaderFile("", "", "GLSL100", "GLSL100");
        def.setLightMode(TechniqueDef.LightMode.MultiPass);
        defs.add(def);
        def = new TechniqueDef("lighting2", 1);
        def.setShaderFile("", "", "GLSL150", "GLSL150");
        def.setLightMode(TechniqueDef.LightMode.MultiPass);
        defs.add(def);
        def = new TechniqueDef("lighting3", 1);
        def.setShaderFile("", "", "GLSL100", "GLSL100");
        def.setLightMode(TechniqueDef.LightMode.SinglePass);
        defs.add(def);
        def = new TechniqueDef("lighting4", 1);
        def.setShaderFile("", "", "GLSL150", "GLSL150");
        def.setLightMode(TechniqueDef.LightMode.SinglePass);
        defs.add(def);
        Collections.sort(defs, comp);

        assertEquals(defs.get(0).getName(), "lighting2");
        assertEquals(defs.get(1).getName(), "lighting1");
        assertEquals(defs.get(2).getName(), "lighting4");
        assertEquals(defs.get(3).getName(), "lighting3");

        //switching preferred lighting mode
        rm.setPreferredLightMode(TechniqueDef.LightMode.SinglePass);
        Collections.sort(defs, comp);

        assertEquals(defs.get(0).getName(), "lighting4");
        assertEquals(defs.get(1).getName(), "lighting3");
        assertEquals(defs.get(2).getName(), "lighting2");
        assertEquals(defs.get(3).getName(), "lighting1");


        //test setting source through the enumMaps method with random cases
        rm.setPreferredLightMode(TechniqueDef.LightMode.MultiPass);
        defs = new ArrayList<>();
        def = new TechniqueDef("lighting1", 1);
        EnumMap<Shader.ShaderType, String> em = new EnumMap<>(Shader.ShaderType.class);
        em.put(Shader.ShaderType.Vertex, "");
        em.put(Shader.ShaderType.Fragment, "");
        em.put(Shader.ShaderType.Geometry, "");
        EnumMap<Shader.ShaderType, String> l = new EnumMap<>(Shader.ShaderType.class);
        l.put(Shader.ShaderType.Vertex, "GLSL100");
        l.put(Shader.ShaderType.Fragment, "GLSL100");
        l.put(Shader.ShaderType.Geometry, "GLSL100");
        def.setShaderFile(em, l);
        def.setLightMode(TechniqueDef.LightMode.SinglePass);
        defs.add(def);

        def = new TechniqueDef("lighting2", 1);
        em = new EnumMap<>(Shader.ShaderType.class);
        em.put(Shader.ShaderType.Vertex, "");
        em.put(Shader.ShaderType.Fragment, "");
        em.put(Shader.ShaderType.Geometry, "");
        l = new EnumMap<>(Shader.ShaderType.class);
        l.put(Shader.ShaderType.Vertex, "GLSL100");
        l.put(Shader.ShaderType.Fragment, "GLSL100");
        l.put(Shader.ShaderType.Geometry, "GLSL100");
        def.setShaderFile(em, l);
        def.setLightMode(TechniqueDef.LightMode.MultiPass);
        defs.add(def);

        def = new TechniqueDef("lighting3", 1);
        em = new EnumMap<>(Shader.ShaderType.class);
        em.put(Shader.ShaderType.Vertex, "");
        em.put(Shader.ShaderType.Fragment, "");
        em.put(Shader.ShaderType.Geometry, "");
        l = new EnumMap<>(Shader.ShaderType.class);
        l.put(Shader.ShaderType.Vertex, "GLSL150");
        l.put(Shader.ShaderType.Fragment, "GLSL150");
        l.put(Shader.ShaderType.Geometry, "GLSL150");
        def.setShaderFile(em, l);
        def.setLightMode(TechniqueDef.LightMode.MultiPass);
        defs.add(def);

        def = new TechniqueDef("lighting4", 1);
        em = new EnumMap<>(Shader.ShaderType.class);
        em.put(Shader.ShaderType.Vertex, "");
        em.put(Shader.ShaderType.Fragment, "");
        em.put(Shader.ShaderType.Geometry, "");
        l = new EnumMap<>(Shader.ShaderType.class);
        l.put(Shader.ShaderType.Vertex, "GLSL130");
        l.put(Shader.ShaderType.Fragment, "GLSL130");
        l.put(Shader.ShaderType.Geometry, "GLSL110");
        def.setShaderFile(em, l);
        def.setLightMode(TechniqueDef.LightMode.MultiPass);
        defs.add(def);

        Collections.sort(defs, comp);

        assertEquals(defs.get(0).getName(), "lighting3");
        assertEquals(defs.get(1).getName(), "lighting4");
        assertEquals(defs.get(2).getName(), "lighting2");
        assertEquals(defs.get(3).getName(), "lighting1");


    }
}
