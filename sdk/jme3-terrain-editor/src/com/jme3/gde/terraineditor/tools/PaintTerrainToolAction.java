/*
 * Copyright (c) 2009-2011 jMonkeyEngine
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
package com.jme3.gde.terraineditor.tools;

import com.jme3.gde.core.sceneexplorer.nodes.AbstractSceneExplorerNode;
import com.jme3.material.MatParam;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.terrain.Terrain;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import java.nio.ByteBuffer;

/**
 * Paint the texture at the specified location.
 * 
 * @author Brent Owens
 */
public class PaintTerrainToolAction extends AbstractTerrainToolAction {
    
    private Vector3f worldLoc;
    private float radius;
    private float weight;
    private int selectedTextureIndex;
    
    public PaintTerrainToolAction(Vector3f markerLocation, float radius, float weight, int selectedTextureIndex) {
        this.worldLoc = markerLocation.clone();
        this.radius = radius;
        this.weight = weight;
        this.selectedTextureIndex = selectedTextureIndex;
        name = "Paint terrain";
    }
    
    @Override
    protected Object doApplyTool(AbstractSceneExplorerNode rootNode) {
        Terrain terrain = getTerrain(rootNode.getLookup().lookup(Node.class));
        if (terrain == null)
            return null;
        paintTexture(terrain, worldLoc, radius, weight, selectedTextureIndex);
        return terrain;
    }
    
    @Override
    protected void doUndoTool(AbstractSceneExplorerNode rootNode, Object undoObject) {
        if (undoObject == null)
            return;
        paintTexture((Terrain)undoObject, worldLoc, radius, -weight, selectedTextureIndex);
    }
    
    public void paintTexture(Terrain terrain, Vector3f markerLocation, float toolRadius, float toolWeight, int selectedTextureIndex) {
        if (selectedTextureIndex < 0 || markerLocation == null)
            return;
        
        int alphaIdx = selectedTextureIndex/4; // 4 = rgba = 4 textures
        Texture tex = getAlphaTexture(terrain, alphaIdx);
        Image image = tex.getImage();

        Vector2f UV = terrain.getPointPercentagePosition(markerLocation.x, markerLocation.z);

        // get the radius of the brush in pixel-percent
        float brushSize = toolRadius/((TerrainQuad)terrain).getTotalSize();
        int texIndex = selectedTextureIndex - ((selectedTextureIndex/4)*4); // selectedTextureIndex/4 is an int floor, do not simplify the equation
        boolean erase = toolWeight<0;
        if (erase)
            toolWeight *= -1;

        doPaintAction(texIndex, image, UV, true, brushSize, erase, toolWeight);

        tex.getImage().setUpdateNeeded();
    }
    
    private Texture getAlphaTexture(Terrain terrain, int alphaLayer) {
        if (terrain == null)
            return null;
        MatParam matParam = null;
        if (alphaLayer == 0)
            matParam = terrain.getMaterial().getParam("AlphaMap");
        else if(alphaLayer == 1)
            matParam = terrain.getMaterial().getParam("AlphaMap_1");
        else if(alphaLayer == 2)
            matParam = terrain.getMaterial().getParam("AlphaMap_2");
        
        if (matParam == null || matParam.getValue() == null) {
            return null;
        }
        Texture tex = (Texture) matParam.getValue();
        return tex;
    }
    
    /**
     * Goes through each pixel in the image. At each pixel it looks to see if the UV mouse coordinate is within the
     * of the brush. If it is in the brush radius, it gets the existing color from that pixel so it can add/subtract to/from it.
     * Essentially it does a radius check and adds in a fade value. It does this to the color value returned by the
     * first pixel color query.
     * Next it sets the color of that pixel. If it was within the radius, the color will change. If it was outside
     * the radius, then nothing will change, the color will be the same; but it will set it nonetheless. Not efficient.
     *
     * If the mouse is being dragged with the button down, then the dragged value should be set to true. This will reduce
     * the intensity of the brush to 10% of what it should be per spray. Otherwise it goes to 100% opacity within a few pixels.
     * This makes it work a little more realistically.
     *
     * @param image to manipulate
     * @param uv the world x,z coordinate
     * @param dragged true if the mouse button is down and it is being dragged, use to reduce brush intensity
     * @param radius in percentage so it can be translated to the image dimensions
     * @param erase true if the tool should remove the paint instead of add it
     * @param fadeFalloff the percentage of the radius when the paint begins to start fading
     */
    protected void doPaintAction(int texIndex, Image image, Vector2f uv, boolean dragged, float radius, boolean erase, float fadeFalloff){
        Vector2f texuv = new Vector2f();
        ColorRGBA color = ColorRGBA.Black;
        
        float width = image.getWidth();
        float height = image.getHeight();

        int minx = (int) (uv.x*width - radius*width); // convert percents to pixels to limit how much we iterate
        int maxx = (int) (uv.x*width + radius*width);
        int miny = (int) (uv.y*height - radius*height);
        int maxy = (int) (uv.y*height + radius*height);

        float radiusSquared = radius*radius;
        float radiusFalloff = radius*fadeFalloff;
        // go through each pixel, in the radius of the tool, in the image
        for (int y = miny; y < maxy; y++){
            for (int x = minx; x < maxx; x++){
                
                texuv.set((float)x / width, (float)y / height);// gets the position in percentage so it can compare with the mouse UV coordinate

                float dist = texuv.distanceSquared(uv);
                if (dist < radiusSquared ) { // if the pixel is within the distance of the radius, set a color (distance times intensity)
                    manipulatePixel(image, x, y, color, false); // gets the color at that location (false means don't write to the buffer)

                    // calculate the fade falloff intensity
                    float intensity = 0.1f;
                    if (dist > radiusFalloff) {
                        float dr = radius - radiusFalloff; // falloff to radius length
                        float d2 = dist - radiusFalloff; // dist minus falloff
                        d2 = d2/dr; // dist percentage of falloff length
                        intensity = 1-d2; // fade out more the farther away it is
                    }

                    //if (dragged)
                    //	intensity = intensity*0.1f; // magical divide it by 10 to reduce its intensity when mouse is dragged

                    if (erase) {
                        switch (texIndex) {
                            case 0:
                                color.r -= intensity; break;
                            case 1:
                                color.g -= intensity; break;
                            case 2:
                                color.b -= intensity; break;
                            case 3:
                                color.a -= intensity; break;
                        }
                    } else {
                        switch (texIndex) {
                            case 0:
                                color.r += intensity; break;
                            case 1:
                                color.g += intensity; break;
                            case 2:
                                color.b += intensity; break;
                            case 3:
                                color.a += intensity; break;
                        }
                    }
                    color.clamp();

                    manipulatePixel(image, x, y, color, true); // set the new color
                }

            }
        }

        image.getData(0).rewind();
    }
    
    /**
     * We are only using RGBA8 images for alpha textures right now.
     * @param image to get/set the color on
     * @param x location
     * @param y location
     * @param color color to get/set
     * @param write to write the color or not
     */
    protected void manipulatePixel(Image image, int x, int y, ColorRGBA color, boolean write){
        ByteBuffer buf = image.getData(0);
        int width = image.getWidth();

        int position = (y * width + x) * 4;

        if ( position> buf.capacity()-1 || position<0 )
            return;
        
        if (write) {
            switch (image.getFormat()){
                case RGBA8:
                    buf.position( position );
                    buf.put(float2byte(color.r))
                       .put(float2byte(color.g))
                       .put(float2byte(color.b))
                       .put(float2byte(color.a));
                    return;
                default:
                    throw new UnsupportedOperationException("Image format: "+image.getFormat());
            }
        } else {
            switch (image.getFormat()){
                case RGBA8:
                    buf.position( position );
                    color.set(byte2float(buf.get()), byte2float(buf.get()), byte2float(buf.get()), byte2float(buf.get()));
                    return;
                default:
                    throw new UnsupportedOperationException("Image format: "+image.getFormat());
            }
        }
        
    }

    private float byte2float(byte b){
        return ((float)(b & 0xFF)) / 255f;
    }

    private byte float2byte(float f){
        return (byte) (f * 255f);
    }
}
