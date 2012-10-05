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
package com.jme3.terrain.geomipmap;

/**
 * Used for TerrainQuad to find neighbours that are not part of the
 * same quad tree. Normally TerrainQuads function in a quad tree and
 * use the neighbour methods getRightQuad, getLeftQuad etc. to update
 * LOD values of the terrain (and for some other routines).
 * 
 * With this you can have a parent, control or spatial, that manages a group of
 * TerrainQuads by linking them together through these four methods.
 * 
 * The general orientation of TerrainQuads and their sub-quads is as such:
 * 
 * 
 *  +-- x+ ---->
 *  |
 *  |    1 | 3 (quadrants)
 *  z+   --+--
 *  |    2 | 4
 *  |
 * \/
 * 
 * Your implementation will still have to manage getHeight, getNormal, and 
 * most other Terrain.java interface methods; often by offsetting the XZ
 * coordinate parameters.
 * 
 * @author sploreg
 */
public interface NeighbourFinder {
    
    /**
     * Get the TerrainQuad to the right of the supplied 'center' quad.
     */
    public TerrainQuad getRightQuad(TerrainQuad center);
    
    /**
     * Get the TerrainQuad to the left of the supplied 'center' quad.
     */
    public TerrainQuad getLeftQuad(TerrainQuad center);
    
    /**
     * Get the TerrainQuad above the supplied 'center' quad.
     */
    public TerrainQuad getTopQuad(TerrainQuad center);
    
    /**
     * Get the TerrainQuad below the supplied 'center' quad.
     */
    public TerrainQuad getDownQuad(TerrainQuad center);
}
