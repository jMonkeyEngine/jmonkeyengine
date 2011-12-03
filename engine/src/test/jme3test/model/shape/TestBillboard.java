/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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

package jme3test.model.shape;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.control.BillboardControl;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Quad;

/**
 *
 * @author Kirill Vainer
 */
public class TestBillboard extends SimpleApplication {

    public void simpleInitApp() {
        flyCam.setMoveSpeed(10);

        Quad q = new Quad(2, 2);
        Geometry g = new Geometry("Quad", q);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Blue);
        g.setMaterial(mat);

        Quad q2 = new Quad(1, 1);
        Geometry g3 = new Geometry("Quad2", q2);
        Material mat2 = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat2.setColor("Color", ColorRGBA.Yellow);
        g3.setMaterial(mat2);
        g3.setLocalTranslation(.5f, .5f, .01f);

        Box b = new Box(new Vector3f(0, 0, 3), .25f, .5f, .25f);
        Geometry g2 = new Geometry("Box", b);
        g2.setMaterial(mat);

        Node bb = new Node("billboard");

        BillboardControl control=new BillboardControl();
        
        bb.addControl(control);
        bb.attachChild(g);
        bb.attachChild(g3);       
        

        n=new Node("parent");
        n.attachChild(g2);
        n.attachChild(bb);
        rootNode.attachChild(n);

        n2=new Node("parentParent");
        n2.setLocalTranslation(Vector3f.UNIT_X.mult(5));
        n2.attachChild(n);

        rootNode.attachChild(n2);


//        rootNode.attachChild(bb);
//        rootNode.attachChild(g2);
    }
 Node n;
 Node n2;
    @Override
    public void simpleUpdate(float tpf) {
        super.simpleUpdate(tpf);
        n.rotate(0, tpf, 0);
        n.move(0.1f*tpf, 0, 0);
        n2.rotate(0, 0, -tpf);
    }



    public static void main(String[] args) {
        TestBillboard app = new TestBillboard();
        app.start();
    }
}