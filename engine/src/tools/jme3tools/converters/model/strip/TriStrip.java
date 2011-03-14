/*
 * Copyright (c) 2003-2009 jMonkeyEngine
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

package jme3tools.converters.model.strip;

import java.util.Arrays;

/**
 * To use, call generateStrips method, passing your triangle index list and 
 * then construct geometry/render resulting PrimitiveGroup objects.
 * Features:
 * <ul> 
 * <li>generates strips from arbitrary geometry. 
 * <li>flexibly optimizes for post TnL vertex caches (16 on GeForce1/2, 24 on GeForce3). 
 * <li>can stitch together strips using degenerate triangles, or not. 
 * <li>can output lists instead of strips. 
 * <li>can optionally throw excessively small strips into a list instead. 
 * <li>can remap indices to improve spatial locality in your vertex buffers.
 * </ul>
 * On cache sizes: Note that it's better to UNDERESTIMATE the cache size
 * instead of OVERESTIMATING. So, if you're targetting GeForce1, 2, and 3, be
 * conservative and use the GeForce1_2 cache size, NOT the GeForce3 cache size.
 * This will make sure you don't "blow" the cache of the GeForce1 and 2. Also
 * note that the cache size you specify is the "actual" cache size, not the
 * "effective" cache size you may have heard about. This is 16 for GeForce1 and 2,
 * and 24 for GeForce3.
 * 
 * Credit goes to Curtis Beeson and Joe Demers for the basis for this
 * stripifier and to Jason Regier and Jon Stone at Blizzard for providing a
 * much cleaner version of CreateStrips().
 * 
 * Ported to java by Artur Biesiadowski <abies@pg.gda.pl> 
 */
public class TriStrip {

    public static final int CACHESIZE_GEFORCE1_2 = 16;
    public static final int CACHESIZE_GEFORCE3 = 24;

    int cacheSize = CACHESIZE_GEFORCE1_2;
    boolean bStitchStrips = true;
    int minStripSize = 0;
    boolean bListsOnly = false;

    /**
	 *  
	 */
    public TriStrip() {
        super();
    }

    /**
	 * If set to true, will return an optimized list, with no strips at all.
	 * Default value: false
	 */
    public void setListsOnly(boolean _bListsOnly) {
        bListsOnly = _bListsOnly;
    }

    /**
	 * Sets the cache size which the stripfier uses to optimize the data.
	 * Controls the length of the generated individual strips. This is the
	 * "actual" cache size, so 24 for GeForce3 and 16 for GeForce1/2 You may
	 * want to play around with this number to tweak performance. Default
	 * value: 16
	 */
    public void setCacheSize(int _cacheSize) {
        cacheSize = _cacheSize;
    }

    /**
	 * bool to indicate whether to stitch together strips into one huge strip
	 * or not. If set to true, you'll get back one huge strip stitched together
	 * using degenerate triangles. If set to false, you'll get back a large
	 * number of separate strips. Default value: true
	 */
    public void setStitchStrips(boolean _bStitchStrips) {
        bStitchStrips = _bStitchStrips;
    }

    /**
	 * Sets the minimum acceptable size for a strip, in triangles. All strips
	 * generated which are shorter than this will be thrown into one big,
	 * separate list. Default value: 0
	 */
    public void setMinStripSize(int _minStripSize) {
        minStripSize = _minStripSize;
    }

    /**
	 * @param in_indices
	 *            input index list, the indices you would use to render
	 * @return array of optimized/stripified PrimitiveGroups
	 */
    public PrimitiveGroup[] generateStrips(int[] in_indices) {
        int numGroups = 0;
        PrimitiveGroup[] primGroups;
        //put data in format that the stripifier likes
        IntVec tempIndices = new IntVec();
        int maxIndex = 0;

        for (int i = 0; i < in_indices.length; i++) {
            tempIndices.add(in_indices[i]);
            if (in_indices[i] > maxIndex)
                maxIndex = in_indices[i];
        }

        StripInfoVec tempStrips = new StripInfoVec();
        FaceInfoVec tempFaces = new FaceInfoVec();

        Stripifier stripifier = new Stripifier();

        //do actual stripification
        stripifier.stripify(tempIndices, cacheSize, minStripSize, maxIndex, tempStrips, tempFaces);

        //stitch strips together
        IntVec stripIndices = new IntVec();
        int numSeparateStrips = 0;

        if (bListsOnly) {
            //if we're outputting only lists, we're done
            numGroups = 1;
            primGroups = new PrimitiveGroup[numGroups];
            primGroups[0] = new PrimitiveGroup();
            PrimitiveGroup[] primGroupArray = primGroups;

            //count the total number of indices
            int numIndices = 0;
            for (int i = 0; i < tempStrips.size(); i++) {
                numIndices += tempStrips.at(i).m_faces.size() * 3;
            }

            //add in the list
            numIndices += tempFaces.size() * 3;

            primGroupArray[0].type = PrimitiveGroup.PT_LIST;
            primGroupArray[0].indices = new int[numIndices];
            primGroupArray[0].numIndices = numIndices;

            //do strips
            int indexCtr = 0;
            for (int i = 0; i < tempStrips.size(); i++) {
                for (int j = 0; j < tempStrips.at(i).m_faces.size(); j++) {
                    //degenerates are of no use with lists
                    if (!Stripifier.isDegenerate(tempStrips.at(i).m_faces.at(j))) {
                        primGroupArray[0].indices[indexCtr++] = tempStrips.at(i).m_faces.at(j).m_v0;
                        primGroupArray[0].indices[indexCtr++] = tempStrips.at(i).m_faces.at(j).m_v1;
                        primGroupArray[0].indices[indexCtr++] = tempStrips.at(i).m_faces.at(j).m_v2;
                    } else {
                        //we've removed a tri, reduce the number of indices
                        primGroupArray[0].numIndices -= 3;
                    }
                }
            }

            //do lists
            for (int i = 0; i < tempFaces.size(); i++) {
                primGroupArray[0].indices[indexCtr++] = tempFaces.at(i).m_v0;
                primGroupArray[0].indices[indexCtr++] = tempFaces.at(i).m_v1;
                primGroupArray[0].indices[indexCtr++] = tempFaces.at(i).m_v2;
            }
        } else {
            numSeparateStrips = stripifier.createStrips(tempStrips, stripIndices, bStitchStrips);

            //if we're stitching strips together, we better get back only one
            // strip from CreateStrips()
            
            //convert to output format
            numGroups = numSeparateStrips; //for the strips
            if (tempFaces.size() != 0)
                numGroups++; //we've got a list as well, increment
            primGroups = new PrimitiveGroup[numGroups];
            for (int i = 0; i < primGroups.length; i++) {
                primGroups[i] = new PrimitiveGroup();
            }

            PrimitiveGroup[] primGroupArray = primGroups;

            //first, the strips
            int startingLoc = 0;
            for (int stripCtr = 0; stripCtr < numSeparateStrips; stripCtr++) {
                int stripLength = 0;

                if (!bStitchStrips) {
                    int i;
                    //if we've got multiple strips, we need to figure out the
                    // correct length
                    for (i = startingLoc; i < stripIndices.size(); i++) {
                        if (stripIndices.get(i) == -1)
                            break;
                    }

                    stripLength = i - startingLoc;
                } else
                    stripLength = stripIndices.size();

                primGroupArray[stripCtr].type = PrimitiveGroup.PT_STRIP;
                primGroupArray[stripCtr].indices = new int[stripLength];
                primGroupArray[stripCtr].numIndices = stripLength;

                int indexCtr = 0;
                for (int i = startingLoc; i < stripLength + startingLoc; i++)
                    primGroupArray[stripCtr].indices[indexCtr++] = stripIndices.get(i);

                //we add 1 to account for the -1 separating strips
                //this doesn't break the stitched case since we'll exit the
                // loop
                startingLoc += stripLength + 1;
            }

            //next, the list
            if (tempFaces.size() != 0) {
                int faceGroupLoc = numGroups - 1; //the face group is the last
                // one
                primGroupArray[faceGroupLoc].type = PrimitiveGroup.PT_LIST;
                primGroupArray[faceGroupLoc].indices = new int[tempFaces.size() * 3];
                primGroupArray[faceGroupLoc].numIndices = tempFaces.size() * 3;
                int indexCtr = 0;
                for (int i = 0; i < tempFaces.size(); i++) {
                    primGroupArray[faceGroupLoc].indices[indexCtr++] = tempFaces.at(i).m_v0;
                    primGroupArray[faceGroupLoc].indices[indexCtr++] = tempFaces.at(i).m_v1;
                    primGroupArray[faceGroupLoc].indices[indexCtr++] = tempFaces.at(i).m_v2;
                }
            }
        }
        return primGroups;
    }

    /**
	 * Function to remap your indices to improve spatial locality in your
	 * vertex buffer.
	 * 
	 * in_primGroups: array of PrimitiveGroups you want remapped numGroups:
	 * number of entries in in_primGroups numVerts: number of vertices in your
	 * vertex buffer, also can be thought of as the range of acceptable values
	 * for indices in your primitive groups. remappedGroups: array of remapped
	 * PrimitiveGroups
	 * 
	 * Note that, according to the remapping handed back to you, you must
	 * reorder your vertex buffer.
	 *  
	 */

    public static int[] remapIndices(int[] indices, int numVerts) {
        int[] indexCache = new int[numVerts];
        Arrays.fill(indexCache, -1);

        int numIndices = indices.length;
        int[] remappedIndices = new int[numIndices];
        int indexCtr = 0;
        for (int j = 0; j < numIndices; j++) {
            int cachedIndex = indexCache[indices[j]];
            if (cachedIndex == -1) //we haven't seen this index before
                {
                //point to "last" vertex in VB
                remappedIndices[j] = indexCtr;

                //add to index cache, increment
                indexCache[indices[j]] = indexCtr++;
            } else {
                //we've seen this index before
                remappedIndices[j] = cachedIndex;
            }
        }

        return remappedIndices;
    }

    public static void remapArrays(float[] vertexBuffer, int vertexSize, int[] indices) {
        int[] remapped = remapIndices(indices, vertexBuffer.length / vertexSize);
        float[] bufferCopy = vertexBuffer.clone();
        for (int i = 0; i < remapped.length; i++) {
            int from = indices[i] * vertexSize;
            int to = remapped[i] * vertexSize;
            for (int j = 0; j < vertexSize; j++) {
                vertexBuffer[to + j] = bufferCopy[from + j];
            }
        }

        System.arraycopy(remapped, 0, indices, 0, indices.length);
    }

}
