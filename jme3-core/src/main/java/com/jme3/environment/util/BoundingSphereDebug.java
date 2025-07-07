 /*
  * Copyright (c) 2009-2025 jMonkeyEngine
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
 package com.jme3.environment.util;

 import com.jme3.asset.AssetManager;
 import com.jme3.export.InputCapsule;
 import com.jme3.export.JmeExporter;
 import com.jme3.export.JmeImporter;
 import com.jme3.export.OutputCapsule;
 import com.jme3.material.Material;
 import com.jme3.math.ColorRGBA;
 import com.jme3.math.FastMath;
 import com.jme3.scene.Geometry;
 import com.jme3.scene.Mesh;
 import com.jme3.scene.VertexBuffer.Type;
 import com.jme3.util.BufferUtils;

 import java.io.IOException;
 import java.nio.FloatBuffer;
 import java.nio.ShortBuffer;

 /**
  * A debugging shape for a BoundingSphere.
  * This mesh consists of three axis-aligned circles (XY, XZ, YZ planes),
  * providing a visual representation of a bounding sphere.
  *
  * @author nehon
  */
 public class BoundingSphereDebug extends Mesh {

     protected int radialSamples = 32;

     /**
      * Constructs a new BoundingSphereDebug mesh.
      */
     public BoundingSphereDebug() {
         setGeometryData();
         setIndexData();
     }

     /**
      * Generates and sets the position and color data for the three circles.
      * The circles are drawn in the XY (blue), XZ (green), and YZ (yellow) planes.
      */
     private void setGeometryData() {

         int numVertices = radialSamples + 1;

         // Each circle has radialSamples + 1 vertices (to close the loop)
         // We have 3 circles, so (radialSamples + 1) * 3 vertices in total.
         FloatBuffer posBuf = BufferUtils.createVector3Buffer(numVertices * 3);
         FloatBuffer colBuf = BufferUtils.createVector3Buffer(numVertices * 4);

         // --- Generate Geometry Data ---
         float angleStep = FastMath.TWO_PI / radialSamples;

         // Generate points for a unit circle
         float[] sin = new float[numVertices];
         float[] cos = new float[numVertices];

         for (int i = 0; i <= radialSamples; i++) {
             float angle = angleStep * i;
             cos[i] = FastMath.cos(angle);
             sin[i] = FastMath.sin(angle);
         }

         // XY Plane Circle (Blue)
         for (int i = 0; i < numVertices; i++) {
             addCircleData(posBuf, colBuf, cos[i], sin[i], 0, ColorRGBA.Blue);
         }
         // XZ Plane Circle (Green)
         for (int i = 0; i < numVertices; i++) {
             addCircleData(posBuf, colBuf, cos[i], 0, sin[i], ColorRGBA.Green);
         }
         // YZ Plane Circle (Yellow)
         for (int i = 0; i < numVertices; i++) {
             addCircleData(posBuf, colBuf, 0, cos[i], sin[i], ColorRGBA.Yellow);
         }

         setBuffer(Type.Position, 3, posBuf);
         setBuffer(Type.Color, 4, colBuf);

         setMode(Mode.Lines);
         updateBound();
         setStatic();
     }

     private void addCircleData(FloatBuffer posBuf, FloatBuffer colBuf,
                                float x, float y, float z, ColorRGBA c) {
         posBuf.put(x).put(y).put(z);
         colBuf.put(c.r).put(c.g).put(c.b).put(c.a);
     }

     /**
      * Sets the index data for rendering the circles as lines.
      * Each circle is made of `radialSamples` line segments.
      */
     private void setIndexData() {

         // allocate connectivity
         int nbSegments = (radialSamples) * 3; // 3 circles

         ShortBuffer idxBuf = BufferUtils.createShortBuffer(2 * nbSegments);
         setBuffer(Type.Index, 2, idxBuf);

         for (int c = 0; c < 3; c++) { // For each of the 3 circles
             int baseIndex = c * (radialSamples + 1);
             for (int i = 0; i < radialSamples; i++) {
                 idxBuf.put((short) (baseIndex + i));
                 idxBuf.put((short) (baseIndex + i + 1));
             }
         }
     }

     /**
      * Convenience factory method that creates a debug bounding-sphere geometry
      *
      * @param assetManager the assetManager
      * @return the bounding sphere debug geometry.
      */
     public static Geometry createDebugSphere(AssetManager assetManager) {
         BoundingSphereDebug mesh = new BoundingSphereDebug();
         Geometry geom = new Geometry("BoundingDebug", mesh);
         Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
         mat.setBoolean("VertexColor", true);
         mat.getAdditionalRenderState().setWireframe(true);
         geom.setMaterial(mat);
         return geom;
     }

     @Override
     public void write(JmeExporter ex) throws IOException {
         super.write(ex);
         OutputCapsule oc = ex.getCapsule(this);
         oc.write(radialSamples, "radialSamples", 32);
     }

     @Override
     public void read(JmeImporter im) throws IOException {
         super.read(im);
         InputCapsule ic = im.getCapsule(this);
         radialSamples = ic.readInt("radialSamples", 32);
     }

 }
