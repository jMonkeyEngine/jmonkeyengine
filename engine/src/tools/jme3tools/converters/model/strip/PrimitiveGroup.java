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

package jme3tools.converters.model.strip;

/**
 * 
 */
public class PrimitiveGroup {

    public static final int PT_LIST = 0;
    public static final int PT_STRIP = 1;
    public static final int PT_FAN = 2;

    public int type;
    public int[] indices;
    public int numIndices;
    
    public PrimitiveGroup() {
        type = PT_STRIP;
    }
    
    public String getTypeString() {
        switch(type) {
            case PT_LIST : return "list";
            case PT_STRIP: return "strip";
            case PT_FAN: return "fan";
            default: return "????";
        }
    }
    
    public String toString() {
        return getTypeString() + " : " + numIndices;
    }
    
    public String getFullInfo() {
        if ( type != PT_STRIP )
            return toString();
        
        int[] stripLengths = new int[numIndices];
        
        int prev = -1;
        int length = -1;
        for ( int i =0; i < numIndices; i++) {
            if (indices[i] == prev) {
                stripLengths[length]++;
                length = -1;
                prev = -1;
            } else {
                prev = indices[i];
                length++;
            }
        }
        stripLengths[length]++;
        
        StringBuffer sb = new StringBuffer();
        sb.append("Strip:").append(numIndices).append("\n");
        for ( int i =0; i < stripLengths.length; i++) {
            if ( stripLengths[i] > 0) {
                sb.append(i).append("->").append(stripLengths[i]).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * @return
     */
    public int[] getTrimmedIndices() {
        if ( indices.length == numIndices )
            return indices;
        int[] nind = new int[numIndices];
        System.arraycopy(indices,0,nind,0,numIndices);
        return nind;
    }

}

