/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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

package jme3test.renderer;

import com.jme3.app.SimpleApplication;
import com.jme3.bounding.BoundingBox;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.*;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.renderer.QueryObject;
import com.jme3.scene.*;
import com.jme3.scene.debug.Arrow;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import com.jme3.util.BufferUtils;
import java.nio.FloatBuffer;

/**
 * Transform feedback test.
 * Implements a simplified cone culling algorithm with the use of
 * transform feedback, vertex and geometry shader.
 * 
 * @author Juraj Papp
 */
public class TestTransformFeedback extends SimpleApplication {


    public static void main(String[] args){
        TestTransformFeedback app = new TestTransformFeedback();
        app.settings = new AppSettings(true);
        app.settings.setWidth(800);
        app.settings.setWidth(600);
//        app.settings.putBoolean("GraphicsDebug", true);
        app.settings.setRenderer(AppSettings.LWJGL_OPENGL3);
        app.setShowSettings(false);
        app.start();
    }
   
    TransformFeedbackOutput tf; //transform feedback object
    QueryObject query; //query to retrieve the number of visible instances
    Geometry cullGeometry; //geometry with location of instances
    
    Geometry instancedGeometry; //the actual instanced geometry
    
    BitmapText text;
    boolean pauseCulling = false;
    
    //input here a large number of instances
    int maxInstances = 1024*1024; //(1 048 576)    
    
    //size of space where random cubes are generated
    int randomSize = 100;
    

    public void simpleInitApp() {
        flyCam.setDragToRotate(true);
        flyCam.setMoveSpeed(10);
        cam.setFrustumPerspective(60f,cam.getWidth()/(float)cam.getHeight(), 0.1f, 300f);        
        
        //------------Cull Geometry----------------
        
        cullGeometry = createCullGeometry();
        
        //do not attach to rootnode
        
        //------------Instanced Geometry----------------
        
        instancedGeometry = createInstancedGeometry();
        
        //attach to root node
        rootNode.attachChild(instancedGeometry);
        
        //Update state so that checkCulling doesn't throw an exception
        instancedGeometry.updateGeometricState();
        
        //------------Query Object----------------
        
        //Create a query object to retrieve the number of primitives (in this case points) written
        query = new QueryObject(renderer, QueryObject.Type.TransformFeedbackPrimitivesGenerated);        
        
        //------------Transform Feedback Output----------------
        
        //The output mode is points (single mat4 per instance)
        //It will connect directly to InstancedData VertexBuffer
        tf = new TransformFeedbackOutput(Mesh.Mode.Points);
        tf.add(instancedGeometry.getMesh().getBuffer(VertexBuffer.Type.InstanceData));
        
        //------------etc----------------
        
        text = new BitmapText(guiFont);
        text.setSize(guiFont.getPreferredSize());
        text.setText("...");
        text.setColor(ColorRGBA.White);
        text.setLocalTranslation(0, cam.getHeight(), 0);
        guiNode.attachChild(text);
                
        //add some coordinate axes
        attachCoordinateAxes();
        
        //On key pressed paused/resume culling
        key(KeyInput.KEY_P, new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if(isPressed) {
                    pauseCulling = !pauseCulling;
                    if(pauseCulling) text.setText("paused");
                    else text.setText("...");
                }
            }
        });
       
    }
    
    public Geometry createCullGeometry() {
        //Let's create the mesh with the positions of instances
        //For simplicity pass whole matrix
        Mesh pointMesh = new Mesh();
        //Each instance has one mat4, so mode is points
        pointMesh.setMode(Mesh.Mode.Points);
        
        //Create vertex buffer which will store the data
        //Use type Position, so that we do not need to specify an index buffers
        //The index buffer would contain [0,1,2,3,4...,maxInstance-1] numbers anyway.
        VertexBuffer dataForCulling = new VertexBuffer(VertexBuffer.Type.Position);
        
        //Create FloatBuffer, matrix4 has 16 floats
        //fill it with matrix4 with random positions
        FloatBuffer fb = (FloatBuffer)BufferUtils.createFloatBuffer(16*maxInstances);
        
        fb.clear();
        
        Matrix4f mat4 = new Matrix4f();
        for(int i = 0; i < maxInstances; i++) {
            mat4.setTranslation(FastMath.nextRandomFloat()*randomSize,
                    FastMath.nextRandomFloat()*randomSize,
                    FastMath.nextRandomFloat()*randomSize);
            mat4.fillFloatBuffer(fb, true);
        }
        fb.flip();
        
        //Setup the data, Usage static since here we do not modify it.
        dataForCulling.setupData(VertexBuffer.Usage.Static, 16, VertexBuffer.Format.Float, fb);
        pointMesh.setBuffer(dataForCulling);
        
        //Create cull geometry, and material
        Geometry cullGeom = new Geometry("CullGeom", pointMesh);
        Material cullTestMat = new Material(assetManager, "jme3test/renderer/TestTR.j3md");
        cullGeom.setMaterial(cullTestMat);
        return cullGeom;
    }
    
    public Geometry createInstancedGeometry() {
        VertexBuffer instanceData = new VertexBuffer(VertexBuffer.Type.InstanceData);
        instanceData.setInstanced(true);
//        FloatBuffer fb2 = (FloatBuffer)BufferUtils.createFloatBuffer(16*maxInstances); 
//        fb2.clear();
//        
//         for(int i = 0; i < maxInstances; i++) {
//            mat4.setTranslation(
//                    FastMath.nextRandomFloat()*randomSize,
//                    FastMath.nextRandomFloat()*randomSize,
//                    FastMath.nextRandomFloat()*randomSize);
//            mat4.fillFloatBuffer(fb2, true);
//        }
//        
//        fb2.flip();

        //No need to create another cpu buffer
        //optimally just the size of the buffer should be passed here
        //we already got a buffer with the same size
        FloatBuffer dummy = (FloatBuffer)cullGeometry.getMesh().getBuffer(VertexBuffer.Type.Position).getData();
        
        instanceData.setupData(VertexBuffer.Usage.StreamCopy, 16,
                VertexBuffer.Format.Float,
                dummy
//              fb2
        );        
        
        //Setting a bounding box so that culling against camera works.
        Geometry instancedGeom = new Geometry("geom", new Box(0.1f, 0.1f,0.1f));
        instancedGeom.setModelBound(new BoundingBox(Vector3f.ZERO, new Vector3f(randomSize+1f,randomSize+1f,randomSize+1f)));
        instancedGeom.getMesh().setBuffer(instanceData);
        instancedGeom.getMesh().updateCounts();
                
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Orange);
        mat.setBoolean("UseInstancing", true);
                
        instancedGeom.setMaterial(mat);
        return instancedGeom;
    }
    
    
//    Vector3f lastCamPos = new Vector3f();
//    Vector3f lastCamDir = new Vector3f();
    public void testTransformFeedback() {
        //This code is slightly convoluted
        //due to the special case of 0 written instances 
        
        if(pauseCulling) return;
        //No need to cull instances if completely outside camera frustrum
        instancedGeometry.setCullHint(Spatial.CullHint.Inherit);
        if(!instancedGeometry.checkCulling(cam) ) {
            instancedGeometry.setCullHint(Spatial.CullHint.Always);
            text.setText("outside of camera frustrum.");
            return;
        }
        
        //For static geom, don't need to cull again if cam & cam dir didn't change
        //But let's be more strict for this test case and disable this optimization
//        if(lastCamPos.equals(cam.getLocation()) && lastCamDir.equals(cam.getDirection()))
//            return;
//        lastCamPos.set(cam.getLocation());
//        lastCamDir.set(cam.getDirection());
        
        //Why not use g_CameraPosition and g_CameraDirection?
        //Since they don't work in update loop
        cullGeometry.getMaterial().setVector3("CamPos", cam.getLocation());
        cullGeometry.getMaterial().setVector3("CamDir", cam.getDirection());
        
        //------------Transform Feedback Start----------------
        renderer.setTransformFeedbackOutput(tf);
        query.begin();
        renderManager.renderGeometry(cullGeometry);
        query.end();
        renderer.setTransformFeedbackOutput(null);
        //------------Transform Feedback End----------------

        //do some cpu work here
        //otherwise would be waiting for gpu to finish
        
        //also a multi-buffer approach can be used
        //where query from previous frame is used
        //and buffers are swapped
        
//        long timeDelay = System.nanoTime();
        long instancesWritten = query.getAndWait();
//        timeDelay = System.nanoTime()-timeDelay;
//        System.out.println("Delay " + timeDelay*0.000001f + " ms.");
        
        text.setText("(P) to pause. Instances Written " + instancesWritten);
        
        //Currently setting instanceCount to 0 will render the mesh as if uninstanced
        //thus...
        //things would be much simpler if the mesh wouldn't be rendered
        if(instancesWritten == 0) {
            instancedGeometry.setCullHint(Spatial.CullHint.Always);
        }
        else {
            instancedGeometry.setCullHint(Spatial.CullHint.Inherit);
            instancedGeometry.getMesh().setInstanceCount((int)instancesWritten);
        }
    }

    @Override
    public void simpleUpdate(float tpf) {
        testTransformFeedback();
        
    }

    
    
    
    //some utility methods 
    public Node attachCoordinateAxes() {
        return attachCoordinateAxes(Vector3f.ZERO);
    }
    public Node attachCoordinateAxes(Vector3f pos) {
        return attachCoordinateAxes(pos, rootNode);
    }
    public Node attachCoordinateAxes(Vector3f pos, Node node) {
        Node coords = new Node("coords");
        Arrow arrow = new Arrow(Vector3f.UNIT_X);
        putShape(arrow, ColorRGBA.Red.mult(0.9f), coords).setLocalTranslation(pos);

        arrow = new Arrow(Vector3f.UNIT_Y);
        putShape(arrow, ColorRGBA.Green.mult(0.8f), coords).setLocalTranslation(pos);

        arrow = new Arrow(Vector3f.UNIT_Z);
        putShape(arrow, ColorRGBA.Blue.mult(0.9f), coords).setLocalTranslation(pos);
        node.attachChild(coords);
        return coords;
    }
    public Geometry putShape(Mesh shape, ColorRGBA color, Node node) {
        Geometry g = new Geometry("coordinate axis", shape);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
//        mat.getAdditionalRenderState().setWireframe(true);
        mat.setColor("Color", color);
        g.setMaterial(mat);
        node.attachChild(g);
        return g;
    }
    private static int actionc = 0;
    
    public void trigger(Trigger t, InputListener a) {
        trigger(t, a, "trigger_"+(++actionc));
    }
    public void trigger(Trigger t, InputListener a, String name) {
        inputManager.addMapping(name, t);
        inputManager.addListener(a, name);
    }
    public void key(int keyInput, InputListener a) {
        key(keyInput, a, "trigger_"+(++actionc));
    }
    public void key(int keyInput, InputListener a, String name) {
        trigger(new KeyTrigger(keyInput), a, name);
    }
}
