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

package jme3tools.optimize;

import com.jme3.bounding.BoundingBox;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import java.util.Set;

public class FastOctnode {

    int offset;
    int length;
    FastOctnode child;
    FastOctnode next;

    private static final BoundingBox tempBox = new BoundingBox();

    public int getSide(){
        return ((offset & 0xE0000000) >> 29) & 0x7;
    }

    public void setSide(int side){
        offset &= 0x1FFFFFFF;
        offset |= (side << 29);
    }

    public void setOffset(int offset){
        if (offset < 0 || offset > 20000000){
            throw new IllegalArgumentException();
        }

        this.offset &= 0xE0000000;
        this.offset |= offset;
    }

    public int getOffset(){
        return this.offset & 0x1FFFFFFF;
    }

    private void generateRenderSetNoCheck(Geometry[] globalGeomList, Set<Geometry> renderSet, Camera cam){
        if (length != 0){
            int start = getOffset();
            int end   = start + length;
            for (int i = start; i < end; i++){
                renderSet.add(globalGeomList[i]);
            }
        }

        if (child == null)
            return;

        FastOctnode node = child;
        while (node != null){
            node.generateRenderSetNoCheck(globalGeomList, renderSet, cam);
            node = node.next;
        }
    }

    private static void findChildBound(BoundingBox bbox, int side){
        float extent = bbox.getXExtent() * 0.5f;
        bbox.getCenter().set(bbox.getCenter().x + extent * Octnode.extentMult[side].x,
                             bbox.getCenter().y + extent * Octnode.extentMult[side].y,
                             bbox.getCenter().z + extent * Octnode.extentMult[side].z);
        bbox.setXExtent(extent);
        bbox.setYExtent(extent);
        bbox.setZExtent(extent);
    }

    public void generateRenderSet(Geometry[] globalGeomList, Set<Geometry> renderSet, Camera cam, BoundingBox parentBox, boolean isRoot){
        tempBox.setCenter(parentBox.getCenter());
        tempBox.setXExtent(parentBox.getXExtent());
        tempBox.setYExtent(parentBox.getYExtent());
        tempBox.setZExtent(parentBox.getZExtent());

        if (!isRoot){
            findChildBound(tempBox, getSide());
        }
        
        tempBox.setCheckPlane(0);
        cam.setPlaneState(0);
        Camera.FrustumIntersect result = cam.contains(tempBox);
        if (result != Camera.FrustumIntersect.Outside){
            if (length != 0){
                int start = getOffset();
                int end   = start + length;
                for (int i = start; i < end; i++){
                    renderSet.add(globalGeomList[i]);
                }
            }

            if (child == null)
                return;

            FastOctnode node = child;

            float x = tempBox.getCenter().x;
            float y = tempBox.getCenter().y;
            float z = tempBox.getCenter().z;
            float ext = tempBox.getXExtent();

            while (node != null){
                if (result == Camera.FrustumIntersect.Inside){
                    node.generateRenderSetNoCheck(globalGeomList, renderSet, cam);
                }else{
                    node.generateRenderSet(globalGeomList, renderSet, cam, tempBox, false);
                }

                tempBox.getCenter().set(x,y,z);
                tempBox.setXExtent(ext);
                tempBox.setYExtent(ext);
                tempBox.setZExtent(ext);

                node = node.next;
            }
        }
    }

    @Override
    public String toString(){
        return "OCTNode[O=" + getOffset() + ", L=" + length +
                ", S=" + getSide() + "]";
    }

    public String toStringVerbose(int indent){
        String str = "------------------".substring(0,indent) + toString() + "\n";
        if (child == null)
            return str;

        FastOctnode children = child;
        while (children != null){
            str += children.toStringVerbose(indent+1);
            children = children.next;
        }

        return str;
    }

}
