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
class StripInfo {

    StripStartInfo m_startInfo;
    FaceInfoVec    m_faces = new FaceInfoVec();
    int              m_stripId;
    int              m_experimentId;
    
    boolean visited;

    int m_numDegenerates;
    

    public StripInfo(StripStartInfo startInfo,int stripId, int experimentId) {

        m_startInfo = startInfo;
        m_stripId      = stripId;
        m_experimentId = experimentId;
        visited = false;
        m_numDegenerates = 0;
    }

    boolean isExperiment() {
        return m_experimentId >= 0;
    }
    
    boolean isInStrip(FaceInfo faceInfo) {
        if(faceInfo == null)
            return false;
        
        return (m_experimentId >= 0 ? faceInfo.m_testStripId == m_stripId : faceInfo.m_stripId == m_stripId);
    }
    

///////////////////////////////////////////////////////////////////////////////////////////
// IsMarked()
//
// If either the faceInfo has a real strip index because it is
// already assign to a committed strip OR it is assigned in an
// experiment and the experiment index is the one we are building
// for, then it is marked and unavailable
    boolean isMarked(FaceInfo faceInfo){
        return (faceInfo.m_stripId >= 0) || (isExperiment() && faceInfo.m_experimentId == m_experimentId);
    }


///////////////////////////////////////////////////////////////////////////////////////////
// MarkTriangle()
//
// Marks the face with the current strip ID
//
    void markTriangle(FaceInfo faceInfo){
        if (isExperiment()){
            faceInfo.m_experimentId = m_experimentId;
            faceInfo.m_testStripId  = m_stripId;
        }
        else{
            faceInfo.m_experimentId = -1;
            faceInfo.m_stripId      = m_stripId;
        }
    }


    boolean unique(FaceInfoVec faceVec, FaceInfo face)
    {
        boolean bv0, bv1, bv2; //bools to indicate whether a vertex is in the faceVec or not
        bv0 = bv1 = bv2 = false;

        for(int i = 0; i < faceVec.size(); i++)
           {
            if(!bv0)
               {
                if( (faceVec.at(i).m_v0 == face.m_v0) || 
                        (faceVec.at(i).m_v1 == face.m_v0) ||
                        (faceVec.at(i).m_v2 == face.m_v0) )
                    bv0 = true;
            }

            if(!bv1)
               {
                if( (faceVec.at(i).m_v0 == face.m_v1) || 
                        (faceVec.at(i).m_v1 == face.m_v1) ||
                        (faceVec.at(i).m_v2 == face.m_v1) )
                    bv1 = true;
            }

            if(!bv2)
               {
                if( (faceVec.at(i).m_v0 == face.m_v2) || 
                        (faceVec.at(i).m_v1 == face.m_v2) ||
                        (faceVec.at(i).m_v2 == face.m_v2) )
                    bv2 = true;
            }

            //the face is not unique, all it's vertices exist in the face vector
            if(bv0 && bv1 && bv2)
                return false;
        }
        
        //if we get out here, it's unique
        return true;
    }


///////////////////////////////////////////////////////////////////////////////////////////
// Build()
//
// Builds a strip forward as far as we can go, then builds backwards, and joins the two lists
//
    void build(EdgeInfoVec edgeInfos, FaceInfoVec faceInfos)
    {
        // used in building the strips forward and backward
        IntVec scratchIndices = new IntVec();
        
        // build forward... start with the initial face
        FaceInfoVec forwardFaces = new FaceInfoVec();
        FaceInfoVec backwardFaces = new FaceInfoVec();
        forwardFaces.add(m_startInfo.m_startFace);

        markTriangle(m_startInfo.m_startFace);
        
        int v0 = (m_startInfo.m_toV1 ? m_startInfo.m_startEdge.m_v0 : m_startInfo.m_startEdge.m_v1);
        int v1 = (m_startInfo.m_toV1 ? m_startInfo.m_startEdge.m_v1 : m_startInfo.m_startEdge.m_v0);
        
        // easiest way to get v2 is to use this function which requires the
        // other indices to already be in the list.
        scratchIndices.add(v0);
        scratchIndices.add(v1);
        int v2 = Stripifier.getNextIndex(scratchIndices, m_startInfo.m_startFace);
        scratchIndices.add(v2);

        //
        // build the forward list
        //
        int nv0 = v1;
        int nv1 = v2;

        FaceInfo nextFace = Stripifier.findOtherFace(edgeInfos, nv0, nv1, m_startInfo.m_startFace);
        while (nextFace != null && !isMarked(nextFace))
           {
            //check to see if this next face is going to cause us to die soon
            int testnv0 = nv1;
            int testnv1 = Stripifier.getNextIndex(scratchIndices, nextFace);
            
            FaceInfo nextNextFace = Stripifier.findOtherFace(edgeInfos, testnv0, testnv1, nextFace);

            if( (nextNextFace == null) || (isMarked(nextNextFace)) )
               {
                //uh, oh, we're following a dead end, try swapping
                FaceInfo testNextFace = Stripifier.findOtherFace(edgeInfos, nv0, testnv1, nextFace);

                if( ((testNextFace != null) && !isMarked(testNextFace)) )
                   {
                    //we only swap if it buys us something
                    
                    //add a "fake" degenerate face
                    FaceInfo tempFace = new FaceInfo(nv0, nv1, nv0);
                    
                    forwardFaces.add(tempFace);
                    markTriangle(tempFace);

                    scratchIndices.add(nv0);
                    testnv0 = nv0;

                    ++m_numDegenerates;
                }

            }

            // add this to the strip
            forwardFaces.add(nextFace);

            markTriangle(nextFace);
            
            // add the index
            //nv0 = nv1;
            //nv1 = NvStripifier::GetNextIndex(scratchIndices, nextFace);
            scratchIndices.add(testnv1);
            
            // and get the next face
            nv0 = testnv0;
            nv1 = testnv1;

            nextFace = Stripifier.findOtherFace(edgeInfos, nv0, nv1, nextFace);
            
        }
        
        // tempAllFaces is going to be forwardFaces + backwardFaces
        // it's used for Unique()
        FaceInfoVec tempAllFaces = new FaceInfoVec();
        for(int i = 0; i < forwardFaces.size(); i++)
            tempAllFaces.add(forwardFaces.at(i));

        //
        // reset the indices for building the strip backwards and do so
        //
        scratchIndices.clear();
        scratchIndices.add(v2);
        scratchIndices.add(v1);
        scratchIndices.add(v0);
        nv0 = v1;
        nv1 = v0;
        nextFace = Stripifier.findOtherFace(edgeInfos, nv0, nv1, m_startInfo.m_startFace);
        while (nextFace != null && !isMarked(nextFace))
           {
            //this tests to see if a face is "unique", meaning that its vertices aren't already in the list
            // so, strips which "wrap-around" are not allowed
            if(!unique(tempAllFaces, nextFace))
                break;

            //check to see if this next face is going to cause us to die soon
            int testnv0 = nv1;
            int testnv1 = Stripifier.getNextIndex(scratchIndices, nextFace);
            
            FaceInfo nextNextFace = Stripifier.findOtherFace(edgeInfos, testnv0, testnv1, nextFace);

            if( (nextNextFace == null) || (isMarked(nextNextFace)) )
               {
                //uh, oh, we're following a dead end, try swapping
                FaceInfo testNextFace = Stripifier.findOtherFace(edgeInfos, nv0, testnv1, nextFace);
                if( ((testNextFace != null) && !isMarked(testNextFace)) )
                   {
                    //we only swap if it buys us something
                    
                    //add a "fake" degenerate face
                    FaceInfo tempFace = new FaceInfo(nv0, nv1, nv0);

                    backwardFaces.add(tempFace);
                    markTriangle(tempFace);
                    scratchIndices.add(nv0);
                    testnv0 = nv0;

                    ++m_numDegenerates;
                }
                
            }

            // add this to the strip
            backwardFaces.add(nextFace);
            
            //this is just so Unique() will work
            tempAllFaces.add(nextFace);

            markTriangle(nextFace);
            
            // add the index
            //nv0 = nv1;
            //nv1 = NvStripifier::GetNextIndex(scratchIndices, nextFace);
            scratchIndices.add(testnv1);
            
            // and get the next face
            nv0 = testnv0;
            nv1 = testnv1;
            nextFace = Stripifier.findOtherFace(edgeInfos, nv0, nv1, nextFace);
        }
        
        // Combine the forward and backwards stripification lists and put into our own face vector
        combine(forwardFaces, backwardFaces);
    }


///////////////////////////////////////////////////////////////////////////////////////////
// Combine()
//
// Combines the two input face vectors and puts the result into m_faces
//
    void combine(FaceInfoVec forward, FaceInfoVec backward){
        
        // add backward faces
        int numFaces = backward.size();
        for (int i = numFaces - 1; i >= 0; i--)
            m_faces.add(backward.at(i));
        
        // add forward faces
        numFaces = forward.size();
        for (int i = 0; i < numFaces; i++)
            m_faces.add(forward.at(i));
    }


///////////////////////////////////////////////////////////////////////////////////////////
// SharesEdge()
//
// Returns true if the input face and the current strip share an edge
//
    boolean sharesEdge(FaceInfo faceInfo, EdgeInfoVec edgeInfos)
    {
        //check v0.v1 edge
        EdgeInfo currEdge = Stripifier.findEdgeInfo(edgeInfos, faceInfo.m_v0, faceInfo.m_v1);
        
        if(isInStrip(currEdge.m_face0) || isInStrip(currEdge.m_face1))
            return true;
        
        //check v1.v2 edge
        currEdge = Stripifier.findEdgeInfo(edgeInfos, faceInfo.m_v1, faceInfo.m_v2);
        
        if(isInStrip(currEdge.m_face0) || isInStrip(currEdge.m_face1))
            return true;
        
        //check v2.v0 edge
        currEdge = Stripifier.findEdgeInfo(edgeInfos, faceInfo.m_v2, faceInfo.m_v0);
        
        if(isInStrip(currEdge.m_face0) || isInStrip(currEdge.m_face1))
            return true;
        
        return false;
        
    }

    
    
    
    

    
    
    
    
}
