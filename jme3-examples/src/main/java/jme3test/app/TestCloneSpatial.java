/*
 * Copyright (c) 2016 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package jme3test.app;

import java.lang.reflect.*;
import java.util.*;

import com.jme3.asset.*;
import com.jme3.font.*;
import com.jme3.light.*;
import com.jme3.material.*;
import com.jme3.math.*;
import com.jme3.scene.*;
import com.jme3.scene.control.*;
import com.jme3.scene.shape.*;
import com.jme3.util.clone.*;


/**
 *
 *
 *  @author    Paul Speed
 */
public class TestCloneSpatial {

    public static void main( String... args ) throws Exception {

        // Setup a test node with some children, controls, etc.
        Node root = new Node("rootNode");

        // A root light
        DirectionalLight rootLight = new DirectionalLight();
        root.addLight(rootLight);

        Box sharedBox = new Box(1, 1, 1);
        Geometry geom1 = new Geometry("box1", sharedBox);
        Material sharedMaterial = new Material(); // not a valid material, just for testing
        geom1.setMaterial(sharedMaterial);

        Geometry geom2 = new Geometry("box2", sharedBox);
        geom2.setMaterial(sharedMaterial);

        root.attachChild(geom1);
        root.attachChild(geom2);

        // Add some controls
        geom1.addControl(new BillboardControl());
        geom2.addControl(new BillboardControl());

        // A light that will only affect the children and be controlled
        // by one child
        PointLight childLight = new PointLight();
        geom1.addLight(childLight);
        geom2.addLight(childLight);

        geom1.addControl(new LightControl(childLight));

        // Set some shared user data also
        Vector3f sharedUserData = new Vector3f(1, 2, 3);
        geom1.setUserData("shared", sharedUserData);
        geom2.setUserData("shared", sharedUserData);

        dump("", root);

        System.out.println("-------- cloning spatial --------------");
        Node clone = root.clone(true);
        dump("", clone);

        System.out.println("-------- cloning spatial without cloning material --------------");
        clone = root.clone(false);
        dump("", clone);
        
        System.out.println("-------- cloning BitmapText ------------");
        DesktopAssetManager assets = new DesktopAssetManager(true);
        BitmapFont font = assets.loadFont("Interface/Fonts/Console.fnt");
        BitmapText text1 = new BitmapText(font);
        text1.setText("Testing");
        System.out.println("Original:");
        dump("", text1);
 
        System.out.println("Clone:");       
        clone = text1.clone();
        dump("", clone);
        
    }


    /**
     *  Debug dump to check structure and identity
     */
    public static void dump( String indent, Spatial s ) {
        if( s instanceof Node ) {
            dump(indent, (Node)s);
        } else if( s instanceof Geometry ) {
            dump(indent, (Geometry)s);
        }
    }

    public static void dump( String indent, Node n ) {
        System.out.println(indent + objectToString(n));
        dumpSpatialProperties(indent + "  ", n);
        if( !n.getChildren().isEmpty() ) {
            System.out.println(indent + "  children:");
            for( Spatial s : n.getChildren() ) {
                dump(indent + "    ", s);
            }
        }
    }

    public static void dump( String indent, Geometry g ) {
        System.out.println(indent +  objectToString(g));
        //System.out.println(indent + "  mesh:" + objectToString(g.getMesh()));
        //System.out.println(indent + "  material:" + objectToString(g.getMaterial()));
        dumpSpatialProperties(indent + "  ", g);
    }

    public static void dump( String indent, Control ctl ) {
        System.out.println(indent + objectToString(ctl));
        if( ctl instanceof AbstractControl ) {
            System.out.println(indent + "  spatial:" + objectToString(((AbstractControl)ctl).getSpatial()));
        }
    }

    private static void dumpSpatialProperties( String indent, Spatial s ) {
        dumpProperties(indent, s, "children");

        if( !s.getUserDataKeys().isEmpty() ) {
            System.out.println(indent + "userData:");
            for( String key : s.getUserDataKeys() ) {
                System.out.println(indent + "  " + key + ":" + objectToString(s.getUserData(key)));
            }
        }

        if( s.getNumControls() > 0 ) {
            System.out.println(indent + "controls:");
            for( int i = 0; i < s.getNumControls(); i++ ) {
                Control ctl = s.getControl(i);
                //dump(indent + "  ", ctl);
                dumpObject(indent + "  ", ctl);
            }
        }

        LightList lights = s.getLocalLightList();
        if( lights.size() > 0 ) {
            System.out.println(indent + "lights:");
            for( Light l : lights ) {
                dumpObject(indent + "  ", l);
            }
        }
    }

    private static void dumpObject( String indent, Object o ) {
        System.out.println(indent +  objectToString(o));
        dumpProperties(indent + "  ", o);
    }

    private static void dumpProperties( String indent, Object o, String... skip ) {
        if( o == null ) {
            return;
        }
        Set<String> skipSet = new HashSet<>(Arrays.asList(skip));
        for( Method m : o.getClass().getMethods() ) {
            if( m.getParameterTypes().length > 0 ) {
                continue;
            }
            String name = m.getName();
            if( "getClass".equals(name) ) {
                continue;
            }
            if( !name.startsWith("get") ) {
                continue;
            }
            Class type = m.getReturnType();
            if( type.isPrimitive() || type.isEnum() ) {
                continue;
            }
            name = name.substring(3);
            if( skipSet.contains(name.toLowerCase()) ) {
                continue;
            }
            try {
                Object value = m.invoke(o);
                System.out.println(indent + name + ":" + objectToString(value));
            } catch( Exception e ) {
                throw new RuntimeException("Error with method:" + m, e);
            }
        }
    }

    private static String objectToString( Object o ) {
        if( o == null ) {
            return null;
        }
        String s = o + "@" + System.identityHashCode(o);
        s = s.replaceAll("\\r?\\n", "");
        return s;
    }
}
