/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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
// $Id: Cylinder.java 4131 2009-03-19 20:15:28Z blaine.dev $
package com.jme3.scene.shape;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;
import java.io.IOException;

/**
 * A simple cylinder, defined by its height and radius.
 * (Ported to jME3)
 *
 * @author Mark Powell
 * @version $Revision: 4131 $, $Date: 2009-03-19 16:15:28 -0400 (Thu, 19 Mar 2009) $
 */
public class Cylinder extends Mesh {

    private int axisSamples;

    private int radialSamples;

    private float radius;
    private float radius2;

    private float height;
    private boolean closed;
    private boolean inverted;

    /**
     * constructor for serialization only. Do not use.
     */
    protected Cylinder() {
    }

    /**
     * Creates a Cylinder. By default, its center is the origin. More
     * samples create a better looking cylinder, at the cost
     * of more vertex data.
     *
     * @param axisSamples
     *            Number of triangle samples along the axis.
     * @param radialSamples
     *            Number of triangle samples along the radial.
     * @param radius
     *            The radius of the cylinder.
     * @param height
     *            The cylinder's height.
     */
    public Cylinder(int axisSamples, int radialSamples,
            float radius, float height) {
        this(axisSamples, radialSamples, radius, height, false);
    }

    /**
     * Creates a Cylinder. By default, its center is the origin. More
     * samples create a better looking cylinder, at the cost
     * of more vertex data. <br>
     * If the cylinder is closed, the texture is split into axisSamples parts:
     * the topmost and bottommost parts are used for top and bottom of the cylinder,
     * and the rest of the texture is used for the cylinder wall. The middle of the top is
     * mapped to texture coordinates (0.5, 1), bottom to (0.5, 0). Thus, it requires
     *
     * @param axisSamples
     *            Number of triangle samples along the axis.
     * @param radialSamples
     *            Number of triangle samples along the radial.
     * @param radius
     *            The radius of the cylinder.
     * @param height
     *            The cylinder's height.
     * @param closed
     *            true to create a cylinder with top and bottom surface
     */
    public Cylinder(int axisSamples, int radialSamples,
            float radius, float height, boolean closed) {
        this(axisSamples, radialSamples, radius, height, closed, false);
    }

    /**
     * Creates a new Cylinder. By default, its center is the origin. More
     * samples create a better looking cylinder, at the cost
     * of more vertex data. <br>
     * If the cylinder is closed, the texture is split into axisSamples parts:
     * the topmost and bottommost parts are used for top and bottom of the cylinder,
     * and the rest of the texture is used for the cylinder wall. The middle of the top is
     * mapped to texture coordinates (0.5, 1), bottom to (0.5, 0). Thus, it requires
     *
     * @param axisSamples The number of vertices samples along the axis. It is equal to the number of segments + 1; so
     * that, for instance, 4 samples mean the cylinder will be made of 3 segments.
     * @param radialSamples The number of triangle samples along the radius. For instance, 4 means that the sides of the
     * cylinder are made of 4 rectangles, and the top and bottom are made of 4 triangles.
     * @param radius
     *            The radius of the cylinder.
     * @param height
     *            The cylinder's height.
     * @param closed
     *            true to create a cylinder with top and bottom surface
     * @param inverted
     *            true to create a cylinder that is meant to be viewed from the
     *            interior.
     */
    public Cylinder(int axisSamples, int radialSamples,
            float radius, float height, boolean closed, boolean inverted) {
        this(axisSamples, radialSamples, radius, radius, height, closed, inverted);
    }

    public Cylinder(int axisSamples, int radialSamples,
            float radius, float radius2, float height, boolean closed, boolean inverted) {
        super();
        updateGeometry(axisSamples, radialSamples, radius, radius2, height, closed, inverted);
    }

    /**
     * @return the number of samples along the cylinder axis
     */
    public int getAxisSamples() {
        return axisSamples;
    }

    /**
     * @return Returns the height.
     */
    public float getHeight() {
        return height;
    }

    /**
     * @return number of samples around cylinder
     */
    public int getRadialSamples() {
        return radialSamples;
    }

    /**
     * @return Returns the radius.
     */
    public float getRadius() {
        return radius;
    }

    public float getRadius2() {
        return radius2;
    }

    /**
     * @return true if end caps are used.
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * @return true if normals and uvs are created for interior use
     */
    public boolean isInverted() {
        return inverted;
    }

    /**
     * Rebuilds the cylinder based on a new set of parameters.
     *
     * @param axisSamples The number of vertices samples along the axis.
     *     It is equal to the number of segments + 1; so
     *     that, for instance, 4 samples mean the cylinder will be made of 3 segments.
     * @param radialSamples The number of triangle samples along the radius.
     *     For instance, 4 means that the sides of the
     *     cylinder are made of 4 rectangles, and the top and bottom are made of 4 triangles.
     * @param topRadius the radius of the top of the cylinder.
     * @param bottomRadius the radius of the bottom of the cylinder.
     * @param height the cylinder's height.
     * @param closed should the cylinder have top and bottom surfaces.
     * @param inverted is the cylinder is meant to be viewed from the inside.
     */
    public void updateGeometry(int axisSamples, int radialSamples,
            float topRadius, float bottomRadius, float height, boolean closed, boolean inverted) {
        // Ensure there's at least two axis samples and 3 radial samples, and positive dimensions.
        if (axisSamples < 2
            || radialSamples < 3
            || topRadius <= 0
            || bottomRadius <= 0
            || height <= 0) {
            throw new IllegalArgumentException("Cylinders must have at least 2 axis samples and 3 radial samples, and positive dimensions.");
        }

        this.axisSamples = axisSamples;
        this.radialSamples = radialSamples;
        this.radius = bottomRadius;
        this.radius2 = topRadius;
        this.height = height;
        this.closed = closed;
        this.inverted = inverted;

        // Vertices : One per radial sample plus one duplicate for texture closing around the sides.
        int verticesCount = axisSamples * (radialSamples +1);
        // Triangles: Two per side rectangle, which is the product of numbers of samples.
        int trianglesCount = axisSamples * radialSamples * 2 ;
        if (closed) {
            // If there are caps, add two additional rims and two summits.
            verticesCount += 2 + 2 * (radialSamples +1);
            // Add one triangle per radial sample, twice, to form the caps.
            trianglesCount += 2 * radialSamples ;
        }

        // Compute the points along a unit circle:
        float[][] circlePoints = new float[radialSamples+1][2];
        for (int circlePoint = 0; circlePoint < radialSamples; circlePoint++) {
            float angle = FastMath.TWO_PI / radialSamples * circlePoint;
            circlePoints[circlePoint][0] = FastMath.cos(angle);
            circlePoints[circlePoint][1] = FastMath.sin(angle);
        }
        // Add a point to close the texture around the side of the cylinder.
        circlePoints[radialSamples][0] = circlePoints[0][0];
        circlePoints[radialSamples][1] = circlePoints[0][1];

        // Calculate normals.
        //
        // A---------B
        //  \        |
        //   \       |
        //    \      |
        //     D-----C
        //
        // Let be B and C the top and bottom points of the axis, and A and D the top and bottom edges.
        // The normal in A and D is simply orthogonal to AD, which means we can get it once per sample.
        //
        Vector3f[] circleNormals = new Vector3f[radialSamples+1];
        for (int circlePoint = 0; circlePoint < radialSamples+1; circlePoint++) {
            // The normal is the orthogonal to the side, which can be got without trigonometry.
            // The edge direction is oriented so that it goes up by Height, and out by the radius difference; let's use
            // those values in reverse order.
            Vector3f normal = new Vector3f(height * circlePoints[circlePoint][0],
                    height * circlePoints[circlePoint][1],
                    bottomRadius - topRadius);
            circleNormals[circlePoint] = normal.normalizeLocal();
        }

        float[] vertices = new float[verticesCount * 3];
        float[] normals = new float[verticesCount * 3];
        float[] textureCoords = new float[verticesCount * 2];
        int currentIndex = 0;

        // Add a circle of points for each axis sample.
        for (int axisSample = 0; axisSample < axisSamples; axisSample++) {
            float currentHeight = -height / 2 + height * axisSample / (axisSamples - 1);
            float currentRadius = bottomRadius + (topRadius - bottomRadius) * axisSample / (axisSamples - 1);

            for (int circlePoint = 0; circlePoint < radialSamples + 1; circlePoint++) {
                // Position, by multiplying the position on a unit circle with the current radius.
                vertices[currentIndex*3] = circlePoints[circlePoint][0] * currentRadius;
                vertices[currentIndex*3 +1] = circlePoints[circlePoint][1] * currentRadius;
                vertices[currentIndex*3 +2] = currentHeight;

                // Normal
                Vector3f currentNormal = circleNormals[circlePoint];
                normals[currentIndex*3] = currentNormal.x;
                normals[currentIndex*3+1] = currentNormal.y;
                normals[currentIndex*3+2] = currentNormal.z;

                // Texture
                // The X is the angular position of the point.
                textureCoords[currentIndex * 2] = (float) circlePoint / radialSamples;
                // Depending on whether there is a cap, the Y is either the height scaled to [0,1], or the radii of
                // the cap count as well.
                if (closed)
                    textureCoords[currentIndex * 2 + 1] = (bottomRadius + height / 2 + currentHeight)
                            / (bottomRadius + height + topRadius);
                else
                    textureCoords[currentIndex * 2 + 1] = height / 2 + currentHeight;

                currentIndex++;
            }
        }

        // If closed, add duplicate rims on top and bottom, with normals facing up and down.
        if (closed) {
            // Bottom
            for (int circlePoint = 0; circlePoint < radialSamples + 1; circlePoint++) {
                vertices[currentIndex*3] = circlePoints[circlePoint][0] * bottomRadius;
                vertices[currentIndex*3 +1] = circlePoints[circlePoint][1] * bottomRadius;
                vertices[currentIndex*3 +2] = -height/2;

                normals[currentIndex*3] = 0;
                normals[currentIndex*3+1] = 0;
                normals[currentIndex*3+2] = -1;

                textureCoords[currentIndex *2] = (float) circlePoint / radialSamples;
                textureCoords[currentIndex *2 +1] = bottomRadius / (bottomRadius + height + topRadius);

                currentIndex++;
            }
            // Top
            for (int circlePoint = 0; circlePoint < radialSamples + 1; circlePoint++) {
                vertices[currentIndex*3] = circlePoints[circlePoint][0] * topRadius;
                vertices[currentIndex*3 +1] = circlePoints[circlePoint][1] * topRadius;
                vertices[currentIndex*3 +2] = height/2;

                normals[currentIndex*3] = 0;
                normals[currentIndex*3+1] = 0;
                normals[currentIndex*3+2] = 1;

                textureCoords[currentIndex *2] = (float) circlePoint / radialSamples;
                textureCoords[currentIndex *2 +1] = (bottomRadius + height) / (bottomRadius + height + topRadius);

                currentIndex++;
            }

            // Add the centers of the caps.
            vertices[currentIndex*3] = 0;
            vertices[currentIndex*3 +1] = 0;
            vertices[currentIndex*3 +2] = -height/2;

            normals[currentIndex*3] = 0;
            normals[currentIndex*3+1] = 0;
            normals[currentIndex*3+2] = -1;

            textureCoords[currentIndex *2] = 0.5f;
            textureCoords[currentIndex *2+1] = 0f;

            currentIndex++;

            vertices[currentIndex*3] = 0;
            vertices[currentIndex*3 +1] = 0;
            vertices[currentIndex*3 +2] = height/2;

            normals[currentIndex*3] = 0;
            normals[currentIndex*3+1] = 0;
            normals[currentIndex*3+2] = 1;

            textureCoords[currentIndex *2] = 0.5f;
            textureCoords[currentIndex *2+1] = 1f;
        }

        // Add the triangles indexes.
        short[] indices = new short[trianglesCount * 3];
        currentIndex = 0;
        for (short axisSample = 0; axisSample < axisSamples - 1; axisSample++) {
            for (int circlePoint = 0; circlePoint < radialSamples; circlePoint++) {
                indices[currentIndex++] = (short) (axisSample * (radialSamples + 1) + circlePoint);
                indices[currentIndex++] =  (short) (axisSample * (radialSamples + 1) + circlePoint + 1);
                indices[currentIndex++] =  (short) ((axisSample + 1) * (radialSamples + 1) + circlePoint);

                indices[currentIndex++] =  (short) ((axisSample + 1) * (radialSamples + 1) + circlePoint);
                indices[currentIndex++] =  (short) (axisSample * (radialSamples + 1) + circlePoint + 1);
                indices[currentIndex++] =  (short) ((axisSample + 1) * (radialSamples + 1) + circlePoint + 1);
            }
        }
        // Add caps if needed.
        if(closed) {
            short bottomCapIndex = (short) (verticesCount - 2);
            short topCapIndex = (short) (verticesCount - 1);

            int bottomRowOffset = (axisSamples) * (radialSamples +1 );
            int topRowOffset = (axisSamples+1) * (radialSamples +1 );

            for (int circlePoint = 0; circlePoint < radialSamples; circlePoint++) {
                indices[currentIndex++] =  (short) (bottomRowOffset + circlePoint +1);
                indices[currentIndex++] = (short) (bottomRowOffset + circlePoint);
                indices[currentIndex++] =  bottomCapIndex;


                indices[currentIndex++] = (short) (topRowOffset + circlePoint);
                indices[currentIndex++] =  (short) (topRowOffset + circlePoint +1);
                indices[currentIndex++] =  topCapIndex;
            }
        }

        // If inverted, the triangles and normals are all reverted.
        if (inverted) {
            for (int i = 0; i < indices.length / 2; i++) {
                short temp = indices[i];
                indices[i] = indices[indices.length - 1 - i];
                indices[indices.length - 1 - i] = temp;
            }

            for (int i = 0; i< normals.length; i++) {
                normals[i] = -normals[i];
            }
        }

        // Fill in the buffers.
        setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(vertices));
        setBuffer(Type.Normal, 3, BufferUtils.createFloatBuffer(normals));
        setBuffer(Type.TexCoord, 2, BufferUtils.createFloatBuffer(textureCoords));
        setBuffer(Type.Index, 3, BufferUtils.createShortBuffer(indices));

        updateBound();
        setStatic();
    }

    @Override
    public void read(JmeImporter importer) throws IOException {
        super.read(importer);
        InputCapsule capsule = importer.getCapsule(this);
        axisSamples = capsule.readInt("axisSamples", 0);
        radialSamples = capsule.readInt("radialSamples", 0);
        radius = capsule.readFloat("radius", 0);
        radius2 = capsule.readFloat("radius2", 0);
        height = capsule.readFloat("height", 0);
        closed = capsule.readBoolean("closed", false);
        inverted = capsule.readBoolean("inverted", false);
    }

    @Override
    public void write(JmeExporter e) throws IOException {
        super.write(e);
        OutputCapsule capsule = e.getCapsule(this);
        capsule.write(axisSamples, "axisSamples", 0);
        capsule.write(radialSamples, "radialSamples", 0);
        capsule.write(radius, "radius", 0);
        capsule.write(radius2, "radius2", 0);
        capsule.write(height, "height", 0);
        capsule.write(closed, "closed", false);
        capsule.write(inverted, "inverted", false);
    }
}
