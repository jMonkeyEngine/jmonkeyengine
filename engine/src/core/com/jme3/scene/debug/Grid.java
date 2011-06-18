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

package com.jme3.scene.debug;

import com.jme3.scene.Mesh;
import com.jme3.scene.Mesh.Mode;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Simple grid shape.
 * 
 * @author Kirill Vainer
 */
public class Grid extends Mesh {

    /**
     * Creates a grid debug shape.
     * @param xLines
     * @param yLines
     * @param lineDist 
     */
    public Grid(int xLines, int yLines, float lineDist){
        xLines -= 2;
        yLines -= 2;
        int lineCount = xLines + yLines + 4;

        FloatBuffer fpb = BufferUtils.createFloatBuffer(6 * lineCount);
        ShortBuffer sib = BufferUtils.createShortBuffer(2 * lineCount);

        float xLineLen = (yLines + 1) * lineDist;
        float yLineLen = (xLines + 1) * lineDist;
        int curIndex = 0;

        // add lines along X
        for (int i = 0; i < xLines + 2; i++){
            float y = (i) * lineDist;

            // positions
            fpb.put(0)       .put(0).put(y);
            fpb.put(xLineLen).put(0).put(y);

            // indices
            sib.put( (short) (curIndex++) );
            sib.put( (short) (curIndex++) );
        }

        // add lines along Y
        for (int i = 0; i < yLines + 2; i++){
            float x = (i) * lineDist;

            // positions
            fpb.put(x).put(0).put(0);
            fpb.put(x).put(0).put(yLineLen);

            // indices
            sib.put( (short) (curIndex++) );
            sib.put( (short) (curIndex++) );
        }

        fpb.flip();
        sib.flip();

        setBuffer(Type.Position, 3, fpb);
        setBuffer(Type.Index, 2, sib);
        
        setMode(Mode.Lines);

        updateBound();
        updateCounts();
    }
    
}
