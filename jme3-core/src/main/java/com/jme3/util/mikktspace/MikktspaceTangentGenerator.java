/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.util.mikktspace;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.*;
import com.jme3.util.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This tangent generator is Highly experimental.
 * This is the Java translation of The mikktspace generator made by Morten S. Mikkelsen
 * C Source code can be found here
 * https://developer.blender.org/diffusion/B/browse/master/intern/mikktspace/mikktspace.c
 * https://developer.blender.org/diffusion/B/browse/master/intern/mikktspace/mikktspace.h
 * 
 * MikkTspace looks like the new standard of tangent generation in 3D softwares.
 * Xnormal, Blender, Substance painter, and many more use it.
 * 
 * Usage is :
 * MikkTSpaceTangentGenerator.generate(spatial);
 * 
 * 
 * 
 * @author Nehon
 */
public class MikktspaceTangentGenerator {

    private final static int MARK_DEGENERATE = 1;
    private final static int QUAD_ONE_DEGEN_TRI = 2;
    private final static int GROUP_WITH_ANY = 4;
    private final static int ORIENT_PRESERVING = 8;
    private final static long INTERNAL_RND_SORT_SEED = 39871946 & 0xffffffffL;
    static final int CELLS = 2048;

    static int makeIndex(final int face, final int vert) {
        assert (vert >= 0 && vert < 4 && face >= 0);
        return (face << 2) | (vert & 0x3);
    }

    private static void indexToData(int[] face, int[] vert, final int indexIn) {
        vert[0] = indexIn & 0x3;
        face[0] = indexIn >> 2;
    }

    static TSpace avgTSpace(final TSpace tS0, final TSpace tS1) {
        TSpace tsRes = new TSpace();

        // this if is important. Due to floating point precision
        // averaging when s0 == s1 will cause a slight difference
        // which results in tangent space splits later on
        if (tS0.magS == tS1.magS && tS0.magT == tS1.magT && tS0.os.equals(tS1.os) && tS0.ot.equals(tS1.ot)) {
            tsRes.magS = tS0.magS;
            tsRes.magT = tS0.magT;
            tsRes.os.set(tS0.os);
            tsRes.ot.set(tS0.ot);
        } else {
            tsRes.magS = 0.5f * (tS0.magS + tS1.magS);
            tsRes.magT = 0.5f * (tS0.magT + tS1.magT);
            tsRes.os.set(tS0.os).addLocal(tS1.os).normalizeLocal();
            tsRes.ot.set(tS0.ot).addLocal(tS1.ot).normalizeLocal();
        }
        return tsRes;
    }

    public static void generate(Spatial s){
        if(s instanceof Node){
            Node n = (Node)s;
            for (Spatial child : n.getChildren()) {
                generate(child);
            }
        } else if (s instanceof Geometry){
            Geometry g = (Geometry)s;
            MikkTSpaceImpl context = new MikkTSpaceImpl(g.getMesh());
            if(!genTangSpaceDefault(context)){
                Logger.getLogger(MikktspaceTangentGenerator.class.getName()).log(Level.SEVERE, "Failed to generate tangents for geometry " + g.getName());
            }
            TangentUtils.generateBindPoseTangentsIfNecessary(g.getMesh());
        }
    }
    
    public static boolean genTangSpaceDefault(MikkTSpaceContext mikkTSpace) {
        return genTangSpace(mikkTSpace, 180.0f);
    }

    public static boolean genTangSpace(MikkTSpaceContext mikkTSpace, final float angularThreshold) {

        // count nr_triangles
        int[] piTriListIn;
        int[] piGroupTrianglesBuffer;
        TriInfo[] pTriInfos;
        Group[] pGroups;
        TSpace[] psTspace;
        int iNrTrianglesIn = 0;
        int iNrTSPaces, iTotTris, iDegenTriangles, iNrMaxGroups;
        int iNrActiveGroups, index;
        final int iNrFaces = mikkTSpace.getNumFaces();
        //boolean bRes = false;
        final float fThresCos = (float) FastMath.cos((angularThreshold * (float) FastMath.PI) / 180.0f);

        // count triangles on supported faces
        for (int f = 0; f < iNrFaces; f++) {
            final int verts = mikkTSpace.getNumVerticesOfFace(f);
            if (verts == 3) {
                ++iNrTrianglesIn;
            } else if (verts == 4) {
                iNrTrianglesIn += 2;
            }
        }
        if (iNrTrianglesIn <= 0) {
            return false;
        }

        piTriListIn = new int[3 * iNrTrianglesIn];
        pTriInfos = new TriInfo[iNrTrianglesIn];

        // make an initial triangle -. face index list
        iNrTSPaces = generateInitialVerticesIndexList(pTriInfos, piTriListIn, mikkTSpace, iNrTrianglesIn);

        // make a welded index list of identical positions and attributes (pos, norm, texc)        
        generateSharedVerticesIndexList(piTriListIn, mikkTSpace, iNrTrianglesIn);

        // Mark all degenerate triangles
        iTotTris = iNrTrianglesIn;
        iDegenTriangles = 0;
        for (int t = 0; t < iTotTris; t++) {
            final int i0 = piTriListIn[t * 3 + 0];
            final int i1 = piTriListIn[t * 3 + 1];
            final int i2 = piTriListIn[t * 3 + 2];
            final Vector3f p0 = getPosition(mikkTSpace, i0);
            final Vector3f p1 = getPosition(mikkTSpace, i1);
            final Vector3f p2 = getPosition(mikkTSpace, i2);
            if (p0.equals(p1) || p0.equals(p2) || p1.equals(p2)) {// degenerate
                pTriInfos[t].flag |= MARK_DEGENERATE;
                ++iDegenTriangles;
            }
        }
        iNrTrianglesIn = iTotTris - iDegenTriangles;

        // mark all triangle pairs that belong to a quad with only one
        // good triangle. These need special treatment in DegenEpilogue().
        // Additionally, move all good triangles to the start of
        // pTriInfos[] and piTriListIn[] without changing order and
        // put the degenerate triangles last.
        degenPrologue(pTriInfos, piTriListIn, iNrTrianglesIn, iTotTris);

        // evaluate triangle level attributes and neighbor list        
        initTriInfo(pTriInfos, piTriListIn, mikkTSpace, iNrTrianglesIn);

        // based on the 4 rules, identify groups based on connectivity
        iNrMaxGroups = iNrTrianglesIn * 3;
        pGroups = new Group[iNrMaxGroups];
        piGroupTrianglesBuffer = new int[iNrTrianglesIn * 3];

        iNrActiveGroups
                = build4RuleGroups(pTriInfos, pGroups, piGroupTrianglesBuffer, piTriListIn, iNrTrianglesIn);

        psTspace = new TSpace[iNrTSPaces];

        for (int t = 0; t < iNrTSPaces; t++) {
            TSpace tSpace = new TSpace();
            tSpace.os.set(1.0f, 0.0f, 0.0f);
            tSpace.magS = 1.0f;
            tSpace.ot.set(0.0f, 1.0f, 0.0f);
            tSpace.magT = 1.0f;
            psTspace[t] = tSpace;
        }

        // make tspaces, each group is split up into subgroups if necessary
        // based on fAngularThreshold. Finally a tangent space is made for
        // every resulting subgroup
        generateTSpaces(psTspace, pTriInfos, pGroups, iNrActiveGroups, piTriListIn, fThresCos, mikkTSpace);

        // degenerate quads with one good triangle will be fixed by copying a space from
        // the good triangle to the coinciding vertex.
        // all other degenerate triangles will just copy a space from any good triangle
        // with the same welded index in piTriListIn[].
        DegenEpilogue(psTspace, pTriInfos, piTriListIn, mikkTSpace, iNrTrianglesIn, iTotTris);

        index = 0;
        for (int f = 0; f < iNrFaces; f++) {
            final int verts = mikkTSpace.getNumVerticesOfFace(f);
            if (verts != 3 && verts != 4) {
                continue;
            }

            // I've decided to let degenerate triangles and group-with-anythings
            // vary between left/right hand coordinate systems at the vertices.
            // All healthy triangles on the other hand are built to always be either or.

            /*// force the coordinate system orientation to be uniform for every face.
             // (this is already the case for good triangles but not for
             // degenerate ones and those with bGroupWithAnything==true)
             bool bOrient = psTspace[index].bOrient;
             if (psTspace[index].iCounter == 0)  // tspace was not derived from a group
             {
             // look for a space created in GenerateTSpaces() by iCounter>0
             bool bNotFound = true;
             int i=1;
             while (i<verts && bNotFound)
             {
             if (psTspace[index+i].iCounter > 0) bNotFound=false;
             else ++i;
             }
             if (!bNotFound) bOrient = psTspace[index+i].bOrient;
             }*/
            // set data
            for (int i = 0; i < verts; i++) {
                final TSpace pTSpace = psTspace[index];
                float tang[] = {pTSpace.os.x, pTSpace.os.y, pTSpace.os.z};
                float bitang[] = {pTSpace.ot.x, pTSpace.ot.y, pTSpace.ot.z};
                mikkTSpace.setTSpace(tang, bitang, pTSpace.magS, pTSpace.magT, pTSpace.orient, f, i);
                mikkTSpace.setTSpaceBasic(tang, pTSpace.orient == true ? 1.0f : (-1.0f), f, i);
                ++index;
            }
        }

        return true;
    }

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // it is IMPORTANT that this function is called to evaluate the hash since
    // inlining could potentially reorder instructions and generate different
    // results for the same effective input value fVal.
    //TODO nehon: Wuuttt? something is fishy about this. How the fuck inlining can reorder instructions? Is that a C thing?
    static int findGridCell(final float min, final float max, final float val) {
        final float fIndex = CELLS * ((val - min) / (max - min));
        final int iIndex = (int) fIndex;
        return iIndex < CELLS ? (iIndex >= 0 ? iIndex : 0) : (CELLS - 1);
    }

    static void generateSharedVerticesIndexList(int piTriList_in_and_out[], final MikkTSpaceContext mikkTSpace, final int iNrTrianglesIn) {

        // Generate bounding box
        TmpVert[] pTmpVert;
        Vector3f vMin = getPosition(mikkTSpace, 0);
        Vector3f vMax = vMin.clone();
        Vector3f vDim;
        float fMin, fMax;
        for (int i = 1; i < (iNrTrianglesIn * 3); i++) {
            final int index = piTriList_in_and_out[i];

            final Vector3f vP = getPosition(mikkTSpace, index);
            if (vMin.x > vP.x) {
                vMin.x = vP.x;
            } else if (vMax.x < vP.x) {
                vMax.x = vP.x;
            }
            if (vMin.y > vP.y) {
                vMin.y = vP.y;
            } else if (vMax.y < vP.y) {
                vMax.y = vP.y;
            }
            if (vMin.z > vP.z) {
                vMin.z = vP.z;
            } else if (vMax.z < vP.z) {
                vMax.z = vP.z;
            }
        }

        vDim = vMax.subtract(vMin);
        int iChannel = 0;
        fMin = vMin.x;
        fMax = vMax.x;
        if (vDim.y > vDim.x && vDim.y > vDim.z) {
            iChannel = 1;
            fMin = vMin.y;
            fMax = vMax.y;
        } else if (vDim.z > vDim.x) {
            iChannel = 2;
            fMin = vMin.z;
            fMax = vMax.z;
        }

        //TODO Nehon: this is really fishy... seems like a hashtable implementation with nasty array manipulation...
        int[] piHashTable = new int[iNrTrianglesIn * 3];
        int[] piHashCount = new int[CELLS];
        int[] piHashOffsets = new int[CELLS];
        int[] piHashCount2 = new int[CELLS];

        // count amount of elements in each cell unit
        for (int i = 0; i < (iNrTrianglesIn * 3); i++) {
            final int index = piTriList_in_and_out[i];
            final Vector3f vP = getPosition(mikkTSpace, index);
            final float fVal = iChannel == 0 ? vP.x : (iChannel == 1 ? vP.y : vP.z);
            final int iCell = findGridCell(fMin, fMax, fVal);
            ++piHashCount[iCell];
        }

        // evaluate start index of each cell.
        piHashOffsets[0] = 0;
        for (int k = 1; k < CELLS; k++) {
            piHashOffsets[k] = piHashOffsets[k - 1] + piHashCount[k - 1];
        }

        // insert vertices
        for (int i = 0; i < (iNrTrianglesIn * 3); i++) {
            final int index = piTriList_in_and_out[i];
            final Vector3f vP = getPosition(mikkTSpace, index);
            final float fVal = iChannel == 0 ? vP.x : (iChannel == 1 ? vP.y : vP.z);
            final int iCell = findGridCell(fMin, fMax, fVal);

            assert (piHashCount2[iCell] < piHashCount[iCell]);

            //    int * pTable = &piHashTable[piHashOffsets[iCell]];
            //    pTable[piHashCount2[iCell]] = i;  // vertex i has been inserted.
            piHashTable[piHashOffsets[iCell] + piHashCount2[iCell]] = i;// vertex i has been inserted.     
            ++piHashCount2[iCell];
        }
        for (int k = 0; k < CELLS; k++) {
            assert (piHashCount2[k] == piHashCount[k]);  // verify the count
        }

        // find maximum amount of entries in any hash entry
        int iMaxCount = piHashCount[0];
        for (int k = 1; k < CELLS; k++) {
            if (iMaxCount < piHashCount[k]) {
                iMaxCount = piHashCount[k];
            }
        }

        pTmpVert = new TmpVert[iMaxCount];

        // complete the merge
        for (int k = 0; k < CELLS; k++) {
            // extract table of cell k and amount of entries in it
            // int * pTable = &piHashTable[piHashOffsets[k]];
            final int iEntries = piHashCount[k];
            if (iEntries < 2) {
                continue;
            }

            if (pTmpVert != null) {
                for (int e = 0; e < iEntries; e++) {
                    int j = piHashTable[piHashOffsets[k] + e];
                    final Vector3f vP = getPosition(mikkTSpace, piTriList_in_and_out[j]);
                    pTmpVert[e] = new TmpVert();
                    pTmpVert[e].vert[0] = vP.x;
                    pTmpVert[e].vert[1] = vP.y;
                    pTmpVert[e].vert[2] = vP.z;
                    pTmpVert[e].index = j;
                }
                MergeVertsFast(piTriList_in_and_out, pTmpVert, mikkTSpace, 0, iEntries - 1);
            } else {
                //TODO Nehon: pTempVert is very unlikely to be null...maybe remove this...
                int[] pTable = Arrays.copyOfRange(piHashTable, piHashOffsets[k], piHashOffsets[k] + iEntries);
                MergeVertsSlow(piTriList_in_and_out, mikkTSpace, pTable, iEntries);
            }
        }
    }

    static void MergeVertsFast(int piTriList_in_and_out[], TmpVert pTmpVert[], final MikkTSpaceContext mikkTSpace, final int iL_in, final int iR_in) {
        // make bbox        
        float[] fvMin = new float[3];
        float[] fvMax = new float[3];
        for (int c = 0; c < 3; c++) {
            fvMin[c] = pTmpVert[iL_in].vert[c];
            fvMax[c] = fvMin[c];
        }
        for (int l = (iL_in + 1); l <= iR_in; l++) {
            for (int c = 0; c < 3; c++) {
                if (fvMin[c] > pTmpVert[l].vert[c]) {
                    fvMin[c] = pTmpVert[l].vert[c];
                } else if (fvMax[c] < pTmpVert[l].vert[c]) {
                    fvMax[c] = pTmpVert[l].vert[c];
                }
            }
        }

        float dx = fvMax[0] - fvMin[0];
        float dy = fvMax[1] - fvMin[1];
        float dz = fvMax[2] - fvMin[2];

        int channel = 0;
        if (dy > dx && dy > dz) {
            channel = 1;
        } else if (dz > dx) {
            channel = 2;
        }

        float fSep = 0.5f * (fvMax[channel] + fvMin[channel]);

        // terminate recursion when the separation/average value
        // is no longer strictly between fMin and fMax values.
        if (fSep >= fvMax[channel] || fSep <= fvMin[channel]) {
            // complete the weld
            for (int l = iL_in; l <= iR_in; l++) {
                int i = pTmpVert[l].index;
                final int index = piTriList_in_and_out[i];
                final Vector3f vP = getPosition(mikkTSpace, index);
                final Vector3f vN = getNormal(mikkTSpace, index);
                final Vector3f vT = getTexCoord(mikkTSpace, index);

                boolean bNotFound = true;
                int l2 = iL_in, i2rec = -1;
                while (l2 < l && bNotFound) {
                    final int i2 = pTmpVert[l2].index;
                    final int index2 = piTriList_in_and_out[i2];
                    final Vector3f vP2 = getPosition(mikkTSpace, index2);
                    final Vector3f vN2 = getNormal(mikkTSpace, index2);
                    final Vector3f vT2 = getTexCoord(mikkTSpace, index2);
                    i2rec = i2;

                    //if (vP==vP2 && vN==vN2 && vT==vT2)
                    if (vP.x == vP2.x && vP.y == vP2.y && vP.z == vP2.z
                            && vN.x == vN2.x && vN.y == vN2.y && vN.z == vN2.z
                            && vT.x == vT2.x && vT.y == vT2.y && vT.z == vT2.z) {
                        bNotFound = false;
                    } else {
                        ++l2;
                    }
                }

                // merge if previously found
                if (!bNotFound) {
                    piTriList_in_and_out[i] = piTriList_in_and_out[i2rec];
                }
            }
        } else {
            int iL = iL_in, iR = iR_in;
            assert ((iR_in - iL_in) > 0);  // at least 2 entries

            // separate (by fSep) all points between iL_in and iR_in in pTmpVert[]
            while (iL < iR) {
                boolean bReadyLeftSwap = false, bReadyRightSwap = false;
                while ((!bReadyLeftSwap) && iL < iR) {
                    assert (iL >= iL_in && iL <= iR_in);
                    bReadyLeftSwap = !(pTmpVert[iL].vert[channel] < fSep);
                    if (!bReadyLeftSwap) {
                        ++iL;
                    }
                }
                while ((!bReadyRightSwap) && iL < iR) {
                    assert (iR >= iL_in && iR <= iR_in);
                    bReadyRightSwap = pTmpVert[iR].vert[channel] < fSep;
                    if (!bReadyRightSwap) {
                        --iR;
                    }
                }
                assert ((iL < iR) || !(bReadyLeftSwap && bReadyRightSwap));

                if (bReadyLeftSwap && bReadyRightSwap) {
                    final TmpVert sTmp = pTmpVert[iL];
                    assert (iL < iR);
                    pTmpVert[iL] = pTmpVert[iR];
                    pTmpVert[iR] = sTmp;
                    ++iL;
                    --iR;
                }
            }

            assert (iL == (iR + 1) || (iL == iR));
            if (iL == iR) {
                final boolean bReadyRightSwap = pTmpVert[iR].vert[channel] < fSep;
                if (bReadyRightSwap) {
                    ++iL;
                } else {
                    --iR;
                }
            }

            // only need to weld when there is more than 1 instance of the (x,y,z)
            if (iL_in < iR) {
                MergeVertsFast(piTriList_in_and_out, pTmpVert, mikkTSpace, iL_in, iR);  // weld all left of fSep
            }
            if (iL < iR_in) {
                MergeVertsFast(piTriList_in_and_out, pTmpVert, mikkTSpace, iL, iR_in);  // weld all right of (or equal to) fSep
            }
        }
    }

    //TODO Nehon: Used only if an array failed to be allocated... Can't happen in Java...
    static void MergeVertsSlow(int piTriList_in_and_out[], final MikkTSpaceContext mikkTSpace, final int pTable[], final int iEntries) {
        // this can be optimized further using a tree structure or more hashing.
        for (int e = 0; e < iEntries; e++) {
            int i = pTable[e];
            final int index = piTriList_in_and_out[i];
            final Vector3f vP = getPosition(mikkTSpace, index);
            final Vector3f vN = getNormal(mikkTSpace, index);
            final Vector3f vT = getTexCoord(mikkTSpace, index);

            boolean bNotFound = true;
            int e2 = 0, i2rec = -1;
            while (e2 < e && bNotFound) {
                final int i2 = pTable[e2];
                final int index2 = piTriList_in_and_out[i2];
                final Vector3f vP2 = getPosition(mikkTSpace, index2);
                final Vector3f vN2 = getNormal(mikkTSpace, index2);
                final Vector3f vT2 = getTexCoord(mikkTSpace, index2);
                i2rec = i2;

                if (vP.equals(vP2) && vN.equals(vN2) && vT.equals(vT2)) {
                    bNotFound = false;
                } else {
                    ++e2;
                }
            }

            // merge if previously found
            if (!bNotFound) {
                piTriList_in_and_out[i] = piTriList_in_and_out[i2rec];
            }
        }
    }

    //TODO Nehon : Not used...seems it's used in the original version if the structure to store the data in the regular method failed...
    static void generateSharedVerticesIndexListSlow(int piTriList_in_and_out[], final MikkTSpaceContext mikkTSpace, final int iNrTrianglesIn) {
        int iNumUniqueVerts = 0;
        for (int t = 0; t < iNrTrianglesIn; t++) {
            for (int i = 0; i < 3; i++) {
                final int offs = t * 3 + i;
                final int index = piTriList_in_and_out[offs];

                final Vector3f vP = getPosition(mikkTSpace, index);
                final Vector3f vN = getNormal(mikkTSpace, index);
                final Vector3f vT = getTexCoord(mikkTSpace, index);

                boolean bFound = false;
                int t2 = 0, index2rec = -1;
                while (!bFound && t2 <= t) {
                    int j = 0;
                    while (!bFound && j < 3) {
                        final int index2 = piTriList_in_and_out[t2 * 3 + j];
                        final Vector3f vP2 = getPosition(mikkTSpace, index2);
                        final Vector3f vN2 = getNormal(mikkTSpace, index2);
                        final Vector3f vT2 = getTexCoord(mikkTSpace, index2);

                        if (vP.equals(vP2) && vN.equals(vN2) && vT.equals(vT2)) {
                            bFound = true;
                        } else {
                            ++j;
                        }
                    }
                    if (!bFound) {
                        ++t2;
                    }
                }

                assert (bFound);
                // if we found our own
                if (index2rec == index) {
                    ++iNumUniqueVerts;
                }

                piTriList_in_and_out[offs] = index2rec;
            }
        }
    }

    static int generateInitialVerticesIndexList(TriInfo pTriInfos[], int piTriList_out[], final MikkTSpaceContext mikkTSpace, final int iNrTrianglesIn) {
        int iTSpacesOffs = 0;
        int iDstTriIndex = 0;
        for (int f = 0; f < mikkTSpace.getNumFaces(); f++) {
            final int verts = mikkTSpace.getNumVerticesOfFace(f);
            if (verts != 3 && verts != 4) {
                continue;
            }

            //TODO nehon : clean this, have a local TrinInfo and assign it to pTriInfo[iDstTriIndex] at the end... and change those variables names...
            pTriInfos[iDstTriIndex] = new TriInfo();
            pTriInfos[iDstTriIndex].orgFaceNumber = f;
            pTriInfos[iDstTriIndex].tSpacesOffs = iTSpacesOffs;

            if (verts == 3) {
                //TODO same here it should be easy once the local TriInfo is created.
                byte[] pVerts = pTriInfos[iDstTriIndex].vertNum;
                pVerts[0] = 0;
                pVerts[1] = 1;
                pVerts[2] = 2;
                piTriList_out[iDstTriIndex * 3 + 0] = makeIndex(f, 0);
                piTriList_out[iDstTriIndex * 3 + 1] = makeIndex(f, 1);
                piTriList_out[iDstTriIndex * 3 + 2] = makeIndex(f, 2);
                ++iDstTriIndex;  // next
            } else {
                //Note, Nehon: we should never get there with JME, because we don't support quads... 
                //but I'm going to let it there incase someone needs it... Just know this code is not tested.
                {//TODO remove those useless brackets...
                    pTriInfos[iDstTriIndex + 1].orgFaceNumber = f;
                    pTriInfos[iDstTriIndex + 1].tSpacesOffs = iTSpacesOffs;
                }

                {
                    // need an order independent way to evaluate
                    // tspace on quads. This is done by splitting
                    // along the shortest diagonal.
                    final int i0 = makeIndex(f, 0);
                    final int i1 = makeIndex(f, 1);
                    final int i2 = makeIndex(f, 2);
                    final int i3 = makeIndex(f, 3);
                    final Vector3f T0 = getTexCoord(mikkTSpace, i0);
                    final Vector3f T1 = getTexCoord(mikkTSpace, i1);
                    final Vector3f T2 = getTexCoord(mikkTSpace, i2);
                    final Vector3f T3 = getTexCoord(mikkTSpace, i3);
                    final float distSQ_02 = T2.subtract(T0).lengthSquared();
                    final float distSQ_13 = T3.subtract(T1).lengthSquared();
                    boolean bQuadDiagIs_02;
                    if (distSQ_02 < distSQ_13) {
                        bQuadDiagIs_02 = true;
                    } else if (distSQ_13 < distSQ_02) {
                        bQuadDiagIs_02 = false;
                    } else {
                        final Vector3f P0 = getPosition(mikkTSpace, i0);
                        final Vector3f P1 = getPosition(mikkTSpace, i1);
                        final Vector3f P2 = getPosition(mikkTSpace, i2);
                        final Vector3f P3 = getPosition(mikkTSpace, i3);
                        final float distSQ_022 = P2.subtract(P0).lengthSquared();
                        final float distSQ_132 = P3.subtract(P1).lengthSquared();

                        bQuadDiagIs_02 = distSQ_132 >= distSQ_022;
                    }

                    if (bQuadDiagIs_02) {
                        {
                            byte[] pVerts_A = pTriInfos[iDstTriIndex].vertNum;
                            pVerts_A[0] = 0;
                            pVerts_A[1] = 1;
                            pVerts_A[2] = 2;
                        }
                        piTriList_out[iDstTriIndex * 3 + 0] = i0;
                        piTriList_out[iDstTriIndex * 3 + 1] = i1;
                        piTriList_out[iDstTriIndex * 3 + 2] = i2;
                        ++iDstTriIndex;  // next
                        {
                            byte[] pVerts_B = pTriInfos[iDstTriIndex].vertNum;
                            pVerts_B[0] = 0;
                            pVerts_B[1] = 2;
                            pVerts_B[2] = 3;
                        }
                        piTriList_out[iDstTriIndex * 3 + 0] = i0;
                        piTriList_out[iDstTriIndex * 3 + 1] = i2;
                        piTriList_out[iDstTriIndex * 3 + 2] = i3;
                        ++iDstTriIndex;  // next
                    } else {
                        {
                            byte[] pVerts_A = pTriInfos[iDstTriIndex].vertNum;
                            pVerts_A[0] = 0;
                            pVerts_A[1] = 1;
                            pVerts_A[2] = 3;
                        }
                        piTriList_out[iDstTriIndex * 3 + 0] = i0;
                        piTriList_out[iDstTriIndex * 3 + 1] = i1;
                        piTriList_out[iDstTriIndex * 3 + 2] = i3;
                        ++iDstTriIndex;  // next
                        {
                            byte[] pVerts_B = pTriInfos[iDstTriIndex].vertNum;
                            pVerts_B[0] = 1;
                            pVerts_B[1] = 2;
                            pVerts_B[2] = 3;
                        }
                        piTriList_out[iDstTriIndex * 3 + 0] = i1;
                        piTriList_out[iDstTriIndex * 3 + 1] = i2;
                        piTriList_out[iDstTriIndex * 3 + 2] = i3;
                        ++iDstTriIndex;  // next
                    }
                }
            }

            iTSpacesOffs += verts;
            assert (iDstTriIndex <= iNrTrianglesIn);
        }

        for (int t = 0; t < iNrTrianglesIn; t++) {
            pTriInfos[t].flag = 0;
        }

        // return total amount of tspaces
        return iTSpacesOffs;
    }

    static Vector3f getPosition(final MikkTSpaceContext mikkTSpace, final int index) {
        //TODO nehon: very ugly but works... using arrays to pass integers as references in the indexToData
        int[] iF = new int[1];
        int[] iI = new int[1];
        float[] pos = new float[3];
        indexToData(iF, iI, index);
        mikkTSpace.getPosition(pos, iF[0], iI[0]);
        return new Vector3f(pos[0], pos[1], pos[2]);
    }

    static Vector3f getNormal(final MikkTSpaceContext mikkTSpace, final int index) {
        //TODO nehon: very ugly but works... using arrays to pass integers as references in the indexToData
        int[] iF = new int[1];
        int[] iI = new int[1];
        float[] norm = new float[3];
        indexToData(iF, iI, index);
        mikkTSpace.getNormal(norm, iF[0], iI[0]);
        return new Vector3f(norm[0], norm[1], norm[2]);
    }

    static Vector3f getTexCoord(final MikkTSpaceContext mikkTSpace, final int index) {
        //TODO nehon: very ugly but works... using arrays to pass integers as references in the indexToData
        int[] iF = new int[1];
        int[] iI = new int[1];
        float[] texc = new float[2];
        indexToData(iF, iI, index);
        mikkTSpace.getTexCoord(texc, iF[0], iI[0]);
        return new Vector3f(texc[0], texc[1], 1.0f);
    }

    // returns the texture area times 2
    static float calcTexArea(final MikkTSpaceContext mikkTSpace, final int indices[]) {
        final Vector3f t1 = getTexCoord(mikkTSpace, indices[0]);
        final Vector3f t2 = getTexCoord(mikkTSpace, indices[1]);
        final Vector3f t3 = getTexCoord(mikkTSpace, indices[2]);

        final float t21x = t2.x - t1.x;
        final float t21y = t2.y - t1.y;
        final float t31x = t3.x - t1.x;
        final float t31y = t3.y - t1.y;

        final float fSignedAreaSTx2 = t21x * t31y - t21y * t31x;

        return fSignedAreaSTx2 < 0 ? (-fSignedAreaSTx2) : fSignedAreaSTx2;
    }

    private static boolean isNotZero(float v) {
        return Math.abs(v) > 0;
    }

    static void initTriInfo(TriInfo pTriInfos[], final int piTriListIn[], final MikkTSpaceContext mikkTSpace, final int iNrTrianglesIn) {

        // pTriInfos[f].flag is cleared in GenerateInitialVerticesIndexList() which is called before this function.
        // generate neighbor info list
        for (int f = 0; f < iNrTrianglesIn; f++) {
            for (int i = 0; i < 3; i++) {
                pTriInfos[f].faceNeighbors[i] = -1;
                pTriInfos[f].assignedGroup[i] = null;

                pTriInfos[f].os.x = 0.0f;
                pTriInfos[f].os.y = 0.0f;
                pTriInfos[f].os.z = 0.0f;
                pTriInfos[f].ot.x = 0.0f;
                pTriInfos[f].ot.y = 0.0f;
                pTriInfos[f].ot.z = 0.0f;
                pTriInfos[f].magS = 0;
                pTriInfos[f].magT = 0;

                // assumed bad
                pTriInfos[f].flag |= GROUP_WITH_ANY;
            }
        }

        // evaluate first order derivatives
        for (int f = 0; f < iNrTrianglesIn; f++) {
            // initial values
            final Vector3f v1 = getPosition(mikkTSpace, piTriListIn[f * 3 + 0]);
            final Vector3f v2 = getPosition(mikkTSpace, piTriListIn[f * 3 + 1]);
            final Vector3f v3 = getPosition(mikkTSpace, piTriListIn[f * 3 + 2]);
            final Vector3f t1 = getTexCoord(mikkTSpace, piTriListIn[f * 3 + 0]);
            final Vector3f t2 = getTexCoord(mikkTSpace, piTriListIn[f * 3 + 1]);
            final Vector3f t3 = getTexCoord(mikkTSpace, piTriListIn[f * 3 + 2]);

            final float t21x = t2.x - t1.x;
            final float t21y = t2.y - t1.y;
            final float t31x = t3.x - t1.x;
            final float t31y = t3.y - t1.y;
            final Vector3f d1 = v2.subtract(v1);
            final Vector3f d2 = v3.subtract(v1);

            final float fSignedAreaSTx2 = t21x * t31y - t21y * t31x;
            //assert(fSignedAreaSTx2!=0);
            Vector3f vOs = d1.mult(t31y).subtract(d2.mult(t21y));  // eq 18
            Vector3f vOt = d1.mult(-t31x).add(d2.mult(t21x));  // eq 19

            pTriInfos[f].flag |= (fSignedAreaSTx2 > 0 ? ORIENT_PRESERVING : 0);

            if (isNotZero(fSignedAreaSTx2)) {
                final float fAbsArea = Math.abs(fSignedAreaSTx2);
                final float fLenOs = vOs.length();
                final float fLenOt = vOt.length();
                final float fS = (pTriInfos[f].flag & ORIENT_PRESERVING) == 0 ? (-1.0f) : 1.0f;
                if (isNotZero(fLenOs)) {
                    pTriInfos[f].os = vOs.multLocal(fS / fLenOs);
                }
                if (isNotZero(fLenOt)) {
                    pTriInfos[f].ot = vOt.multLocal(fS / fLenOt);
                }

                // evaluate magnitudes prior to normalization of vOs and vOt
                pTriInfos[f].magS = fLenOs / fAbsArea;
                pTriInfos[f].magT = fLenOt / fAbsArea;

                // if this is a good triangle
                if (isNotZero(pTriInfos[f].magS) && isNotZero(pTriInfos[f].magT)) {
                    pTriInfos[f].flag &= (~GROUP_WITH_ANY);
                }
            }
        }

        // force otherwise healthy quads to a fixed orientation
        int t = 0;
        while (t < (iNrTrianglesIn - 1)) {
            final int iFO_a = pTriInfos[t].orgFaceNumber;
            final int iFO_b = pTriInfos[t + 1].orgFaceNumber;
            if (iFO_a == iFO_b) {
                // this is a quad
                final boolean bIsDeg_a = (pTriInfos[t].flag & MARK_DEGENERATE) != 0;
                final boolean bIsDeg_b = (pTriInfos[t + 1].flag & MARK_DEGENERATE) != 0;

                // bad triangles should already have been removed by
                // DegenPrologue(), but just in case check bIsDeg_a and bIsDeg_a are false
                if ((bIsDeg_a || bIsDeg_b) == false) {
                    final boolean bOrientA = (pTriInfos[t].flag & ORIENT_PRESERVING) != 0;
                    final boolean bOrientB = (pTriInfos[t + 1].flag & ORIENT_PRESERVING) != 0;
                    // if this happens the quad has extremely bad mapping!!
                    if (bOrientA != bOrientB) {
                        //printf("found quad with bad mapping\n");
                        boolean bChooseOrientFirstTri = false;
                        if ((pTriInfos[t + 1].flag & GROUP_WITH_ANY) != 0) {
                            bChooseOrientFirstTri = true;
                        } else if (calcTexArea(mikkTSpace, Arrays.copyOfRange(piTriListIn, t * 3 + 0, t * 3 + 3)) >= calcTexArea(mikkTSpace, Arrays.copyOfRange(piTriListIn, (t + 1) * 3 + 0, (t + 1) * 3 + 3))) {
                            bChooseOrientFirstTri = true;
                        }

                        // force match
                        {
                            final int t0 = bChooseOrientFirstTri ? t : (t + 1);
                            final int t1 = bChooseOrientFirstTri ? (t + 1) : t;
                            pTriInfos[t1].flag &= (~ORIENT_PRESERVING);  // clear first
                            pTriInfos[t1].flag |= (pTriInfos[t0].flag & ORIENT_PRESERVING);  // copy bit
                        }
                    }
                }
                t += 2;
            } else {
                ++t;
            }
        }

        // match up edge pairs
        {
            //Edge * pEdges = (Edge *) malloc(sizeof(Edge)*iNrTrianglesIn*3);
            Edge[] pEdges = new Edge[iNrTrianglesIn * 3];

            //TODO nehon weird... original algorithm check if pEdges is null but it's just been allocated... weirder, it does soemthing different if the edges are null...
            //    if (pEdges==null)
            //      BuildNeighborsSlow(pTriInfos, piTriListIn, iNrTrianglesIn);
            //    else
            //    {
            buildNeighborsFast(pTriInfos, pEdges, piTriListIn, iNrTrianglesIn);

            //    }
        }
    }

    static int build4RuleGroups(TriInfo pTriInfos[], Group pGroups[], int piGroupTrianglesBuffer[], final int piTriListIn[], final int iNrTrianglesIn) {
        final int iNrMaxGroups = iNrTrianglesIn * 3;
        int iNrActiveGroups = 0;
        int iOffset = 0;
        // (void)iNrMaxGroups;  /* quiet warnings in non debug mode */
        for (int f = 0; f < iNrTrianglesIn; f++) {
            for (int i = 0; i < 3; i++) {
                // if not assigned to a group
                if ((pTriInfos[f].flag & GROUP_WITH_ANY) == 0 && pTriInfos[f].assignedGroup[i] == null) {
                    boolean bOrPre;                    
                    final int vert_index = piTriListIn[f * 3 + i];
                    assert (iNrActiveGroups < iNrMaxGroups);
                    pTriInfos[f].assignedGroup[i] = new Group(); 
                    pGroups[iNrActiveGroups] = pTriInfos[f].assignedGroup[i];
                    pTriInfos[f].assignedGroup[i].vertexRepresentitive = vert_index;
                    pTriInfos[f].assignedGroup[i].orientPreservering = (pTriInfos[f].flag & ORIENT_PRESERVING) != 0;
                    pTriInfos[f].assignedGroup[i].nrFaces = 0;
                    
                    ++iNrActiveGroups;

                    addTriToGroup(pTriInfos[f].assignedGroup[i], f);
                    bOrPre = (pTriInfos[f].flag & ORIENT_PRESERVING) != 0;
                    int neigh_indexL = pTriInfos[f].faceNeighbors[i];
                    int neigh_indexR = pTriInfos[f].faceNeighbors[i > 0 ? (i - 1) : 2];
                    if (neigh_indexL >= 0) {
                        // neighbor
                        final boolean bAnswer
                                = assignRecur(piTriListIn, pTriInfos, neigh_indexL,
                                        pTriInfos[f].assignedGroup[i]);

                        final boolean bOrPre2 = (pTriInfos[neigh_indexL].flag & ORIENT_PRESERVING) != 0;
                        final boolean bDiff = bOrPre != bOrPre2;
                        assert (bAnswer || bDiff);
                        //(void)bAnswer, (void)bDiff;  /* quiet warnings in non debug mode */
                    }
                    if (neigh_indexR >= 0) {
                        // neighbor
                        final boolean bAnswer
                                = assignRecur(piTriListIn, pTriInfos, neigh_indexR,
                                        pTriInfos[f].assignedGroup[i]);

                        final boolean bOrPre2 = (pTriInfos[neigh_indexR].flag & ORIENT_PRESERVING) != 0;
                        final boolean bDiff = bOrPre != bOrPre2;
                        assert (bAnswer || bDiff);
                        //(void)bAnswer, (void)bDiff;  /* quiet warnings in non debug mode */
                    }

                    int[] faceIndices = new int[pTriInfos[f].assignedGroup[i].nrFaces];
                    //pTriInfos[f].assignedGroup[i].faceIndices.toArray(faceIndices);
                    for (int j = 0; j < faceIndices.length; j++) {
                        faceIndices[j] = pTriInfos[f].assignedGroup[i].faceIndices.get(j);
                    }
                    
                    //Nehon: copy back the faceIndices data into the groupTriangleBuffer.
                    System.arraycopy( faceIndices, 0, piGroupTrianglesBuffer, iOffset, pTriInfos[f].assignedGroup[i].nrFaces);
                    // update offset
                    iOffset += pTriInfos[f].assignedGroup[i].nrFaces;
                    // since the groups are disjoint a triangle can never
                    // belong to more than 3 groups. Subsequently something
                    // is completely screwed if this assertion ever hits.
                    assert (iOffset <= iNrMaxGroups);
                }
            }
        }

        return iNrActiveGroups;
    }

    static void addTriToGroup(Group group, final int triIndex) {
        //group.faceIndices[group.nrFaces] = triIndex;
        group.faceIndices.add(triIndex);
        ++group.nrFaces;
    }

    static boolean assignRecur(final int piTriListIn[], TriInfo psTriInfos[], final int iMyTriIndex, Group pGroup) {
        TriInfo pMyTriInfo = psTriInfos[iMyTriIndex];

        // track down vertex
        final int iVertRep = pGroup.vertexRepresentitive;
        int index = 3 * iMyTriIndex;
        int i = -1;
        if (piTriListIn[index] == iVertRep) {
            i = 0;
        } else if (piTriListIn[index + 1] == iVertRep) {
            i = 1;
        } else if (piTriListIn[index + 2] == iVertRep) {
            i = 2;
        }
        assert (i >= 0 && i < 3);

        // early out
        if (pMyTriInfo.assignedGroup[i] == pGroup) {
            return true;
        } else if (pMyTriInfo.assignedGroup[i] != null) {
            return false;
        }
        if ((pMyTriInfo.flag & GROUP_WITH_ANY) != 0) {
            // first to group with a group-with-anything triangle
            // determines it's orientation.
            // This is the only existing order dependency in the code!!
            if (pMyTriInfo.assignedGroup[0] == null
                    && pMyTriInfo.assignedGroup[1] == null
                    && pMyTriInfo.assignedGroup[2] == null) {
                pMyTriInfo.flag &= (~ORIENT_PRESERVING);
                pMyTriInfo.flag |= (pGroup.orientPreservering ? ORIENT_PRESERVING : 0);
            }
        }
        {
            final boolean bOrient = (pMyTriInfo.flag & ORIENT_PRESERVING) != 0;
            if (bOrient != pGroup.orientPreservering) {
                return false;
            }
        }

        addTriToGroup(pGroup, iMyTriIndex);
        pMyTriInfo.assignedGroup[i] = pGroup;

        {
            final int neigh_indexL = pMyTriInfo.faceNeighbors[i];
            final int neigh_indexR = pMyTriInfo.faceNeighbors[i > 0 ? (i - 1) : 2];
            if (neigh_indexL >= 0) {
                assignRecur(piTriListIn, psTriInfos, neigh_indexL, pGroup);
            }
            if (neigh_indexR >= 0) {
                assignRecur(piTriListIn, psTriInfos, neigh_indexR, pGroup);
            }
        }

        return true;
    }

    static boolean generateTSpaces(TSpace psTspace[], final TriInfo pTriInfos[], final Group pGroups[],
            final int iNrActiveGroups, final int piTriListIn[], final float fThresCos,
            final MikkTSpaceContext mikkTSpace) {
        TSpace[] pSubGroupTspace;
        SubGroup[] pUniSubGroups;
        int[] pTmpMembers;
        int iMaxNrFaces = 0, iUniqueTspaces = 0, g = 0, i = 0;
        for (g = 0; g < iNrActiveGroups; g++) {
            if (iMaxNrFaces < pGroups[g].nrFaces) {
                iMaxNrFaces = pGroups[g].nrFaces;
            }
        }

        if (iMaxNrFaces == 0) {
            return true;
        }

        // make initial allocations
        pSubGroupTspace = new TSpace[iMaxNrFaces];
        pUniSubGroups = new SubGroup[iMaxNrFaces];
        pTmpMembers = new int[iMaxNrFaces];


        iUniqueTspaces = 0;
        for (g = 0; g < iNrActiveGroups; g++) {
            final Group pGroup = pGroups[g];
            int iUniqueSubGroups = 0, s = 0;

            for (i = 0; i < pGroup.nrFaces; i++) // triangles
            {
                final int f = pGroup.faceIndices.get(i);  // triangle number
                int index = -1, iVertIndex = -1, iOF_1 = -1, iMembers = 0, j = 0, l = 0;
                SubGroup tmp_group = new SubGroup();
                boolean bFound;
                Vector3f n, vOs, vOt;
                if (pTriInfos[f].assignedGroup[0] == pGroup) {
                    index = 0;
                } else if (pTriInfos[f].assignedGroup[1] == pGroup) {
                    index = 1;
                } else if (pTriInfos[f].assignedGroup[2] == pGroup) {
                    index = 2;
                }
                assert (index >= 0 && index < 3);

                iVertIndex = piTriListIn[f * 3 + index];
                assert (iVertIndex == pGroup.vertexRepresentitive);

                // is normalized already
                n = getNormal(mikkTSpace, iVertIndex);

                // project
                vOs = pTriInfos[f].os.subtract(n.mult(n.dot(pTriInfos[f].os)));
                vOt = pTriInfos[f].ot.subtract(n.mult(n.dot(pTriInfos[f].ot)));
                vOs.normalizeLocal();
                vOt.normalizeLocal();

                // original face number
                iOF_1 = pTriInfos[f].orgFaceNumber;

                iMembers = 0;
                for (j = 0; j < pGroup.nrFaces; j++) {
                    final int t = pGroup.faceIndices.get(j);  // triangle number
                    final int iOF_2 = pTriInfos[t].orgFaceNumber;

                    // project
                    Vector3f vOs2 = pTriInfos[t].os.subtract(n.mult(n.dot(pTriInfos[t].os)));
                    Vector3f vOt2 = pTriInfos[t].ot.subtract(n.mult(n.dot(pTriInfos[t].ot)));
                    vOs2.normalizeLocal();
                    vOt2.normalizeLocal();

                    {
                        final boolean bAny = ((pTriInfos[f].flag | pTriInfos[t].flag) & GROUP_WITH_ANY) != 0;
                        // make sure triangles which belong to the same quad are joined.
                        final boolean bSameOrgFace = iOF_1 == iOF_2;

                        final float fCosS = vOs.dot(vOs2);
                        final float fCosT = vOt.dot(vOt2);

                        assert (f != t || bSameOrgFace);  // sanity check
                        if (bAny || bSameOrgFace || (fCosS > fThresCos && fCosT > fThresCos)) {
                            pTmpMembers[iMembers++] = t;
                        }
                    }
                }

                // sort pTmpMembers
                tmp_group.nrFaces = iMembers;
                tmp_group.triMembers = pTmpMembers;
                if (iMembers > 1) {
                    quickSort(pTmpMembers, 0, iMembers - 1, INTERNAL_RND_SORT_SEED);
                }

                // look for an existing match
                bFound = false;
                l = 0;
                while (l < iUniqueSubGroups && !bFound) {
                    bFound = compareSubGroups(tmp_group, pUniSubGroups[l]);
                    if (!bFound) {
                        ++l;
                    }
                }

                // assign tangent space index
                assert (bFound || l == iUniqueSubGroups);
                //piTempTangIndices[f*3+index] = iUniqueTspaces+l;

                // if no match was found we allocate a new subgroup
                if (!bFound) {
                    // insert new subgroup
                    int[] pIndices = new int[iMembers];
                    pUniSubGroups[iUniqueSubGroups] = new SubGroup();
                    pUniSubGroups[iUniqueSubGroups].nrFaces = iMembers;
                    pUniSubGroups[iUniqueSubGroups].triMembers = pIndices;
                    System.arraycopy(tmp_group.triMembers, 0, pIndices, 0, iMembers);
                    //memcpy(pIndices, tmp_group.pTriMembers, iMembers*sizeof(int));
                    pSubGroupTspace[iUniqueSubGroups]
                            = evalTspace(tmp_group.triMembers, iMembers, piTriListIn, pTriInfos, mikkTSpace, pGroup.vertexRepresentitive);
                    ++iUniqueSubGroups;
                }

                // output tspace
                {
                    final int iOffs = pTriInfos[f].tSpacesOffs;
                    final int iVert = pTriInfos[f].vertNum[index];
                    TSpace pTS_out = psTspace[iOffs + iVert];
                    assert (pTS_out.counter < 2);
                    assert (((pTriInfos[f].flag & ORIENT_PRESERVING) != 0) == pGroup.orientPreservering);
                    if (pTS_out.counter == 1) {
                        pTS_out.set(avgTSpace(pTS_out, pSubGroupTspace[l]));
                        pTS_out.counter = 2;  // update counter
                        pTS_out.orient = pGroup.orientPreservering;
                    } else {
                        assert (pTS_out.counter == 0);
                        pTS_out.set(pSubGroupTspace[l]);
                        pTS_out.counter = 1;  // update counter
                        pTS_out.orient = pGroup.orientPreservering;
                    }
                }
            }

            iUniqueTspaces += iUniqueSubGroups;
        }

        return true;
    }

    static TSpace evalTspace(int face_indices[], final int iFaces, final int piTriListIn[], final TriInfo pTriInfos[],
            final MikkTSpaceContext mikkTSpace, final int iVertexRepresentitive) {
        TSpace res = new TSpace();
        float fAngleSum = 0;        

        for (int face = 0; face < iFaces; face++) {
            final int f = face_indices[face];

            // only valid triangles get to add their contribution
            if ((pTriInfos[f].flag & GROUP_WITH_ANY) == 0) {
                
                int i = -1;
                if (piTriListIn[3 * f + 0] == iVertexRepresentitive) {
                    i = 0;
                } else if (piTriListIn[3 * f + 1] == iVertexRepresentitive) {
                    i = 1;
                } else if (piTriListIn[3 * f + 2] == iVertexRepresentitive) {
                    i = 2;
                }
                assert (i >= 0 && i < 3);

                // project
                int index = piTriListIn[3 * f + i];
                Vector3f n = getNormal(mikkTSpace, index);
                Vector3f vOs = pTriInfos[f].os.subtract(n.mult(n.dot(pTriInfos[f].os)));
                Vector3f vOt = pTriInfos[f].ot.subtract(n.mult(n.dot(pTriInfos[f].ot)));
                vOs.normalizeLocal();
                vOt.normalizeLocal();

                int i2 = piTriListIn[3 * f + (i < 2 ? (i + 1) : 0)];
                int i1 = piTriListIn[3 * f + i];
                int i0 = piTriListIn[3 * f + (i > 0 ? (i - 1) : 2)];

                Vector3f p0 = getPosition(mikkTSpace, i0);
                Vector3f p1 = getPosition(mikkTSpace, i1);
                Vector3f  p2 = getPosition(mikkTSpace, i2);
                Vector3f v1 = p0.subtract(p1);
                Vector3f v2 = p2.subtract(p1);

                // project
                v1.subtractLocal(n.mult(n.dot(v1))).normalizeLocal();
                v2.subtractLocal(n.mult(n.dot(v2))).normalizeLocal();

                // weight contribution by the angle
                // between the two edge vectors
                float fCos = v1.dot(v2);
                fCos = fCos > 1 ? 1 : (fCos < (-1) ? (-1) : fCos);
                float fAngle = (float) Math.acos(fCos);
                float fMagS = pTriInfos[f].magS;
                float fMagT = pTriInfos[f].magT;

                res.os.addLocal(vOs.multLocal(fAngle));
                res.ot.addLocal(vOt.multLocal(fAngle));
                res.magS += (fAngle * fMagS);
                res.magT += (fAngle * fMagT);
                fAngleSum += fAngle;
            }
        }

        // normalize
        res.os.normalizeLocal();
        res.ot.normalizeLocal();

        if (fAngleSum > 0) {
            res.magS /= fAngleSum;
            res.magT /= fAngleSum;
        }

        return res;
    }

    static boolean compareSubGroups(final SubGroup pg1, final SubGroup pg2) {
        if(pg2 == null || (pg1.nrFaces != pg2.nrFaces)){
            return false;
        }
        boolean stillSame = true;
        int i = 0;        
        while (i < pg1.nrFaces && stillSame) {
            stillSame = pg1.triMembers[i] == pg2.triMembers[i];
            if (stillSame) {
                ++i;
            }
        }
        return stillSame;
    }

    static void quickSort(int[] pSortBuffer, int iLeft, int iRight, long uSeed) {
        int iL, iR, n, index, iMid, iTmp;

        // Random
        long t = uSeed & 31;
        t = (uSeed << t) | (uSeed >> (32 - t));
        uSeed = uSeed + t + 3;
        // Random end
        uSeed = uSeed & 0xffffffffL;

        iL = iLeft;
        iR = iRight;
        n = (iR - iL) + 1;
        assert (n >= 0);
        index = (int) ((uSeed & 0xffffffffL) % n);

        iMid = pSortBuffer[index + iL];

        do {
            while (pSortBuffer[iL] < iMid) {
                ++iL;
            }
            while (pSortBuffer[iR] > iMid) {
                --iR;
            }

            if (iL <= iR) {
                iTmp = pSortBuffer[iL];
                pSortBuffer[iL] = pSortBuffer[iR];
                pSortBuffer[iR] = iTmp;
                ++iL;
                --iR;
            }
        } while (iL <= iR);

        if (iLeft < iR) {
            quickSort(pSortBuffer, iLeft, iR, uSeed);
        }
        if (iL < iRight) {
            quickSort(pSortBuffer, iL, iRight, uSeed);
        }
    }

    static void buildNeighborsFast(TriInfo pTriInfos[], Edge[] pEdges, final int piTriListIn[], final int iNrTrianglesIn) {
        // build array of edges
        long uSeed = INTERNAL_RND_SORT_SEED;        // could replace with a random seed?
        
        for (int f = 0; f < iNrTrianglesIn; f++) {
            for (int i = 0; i < 3; i++) {
                final int i0 = piTriListIn[f * 3 + i];
                final int i1 = piTriListIn[f * 3 + (i < 2 ? (i + 1) : 0)];
                pEdges[f * 3 + i] = new Edge();
                pEdges[f * 3 + i].setI0(i0 < i1 ? i0 : i1);      // put minimum index in i0
                pEdges[f * 3 + i].setI1(!(i0 < i1) ? i0 : i1);    // put maximum index in i1
                pEdges[f * 3 + i].setF(f);              // record face number
            }
        }

        // sort over all edges by i0, this is the pricy one.
        quickSortEdges(pEdges, 0, iNrTrianglesIn * 3 - 1, 0, uSeed);  // sort channel 0 which is i0

        // sub sort over i1, should be fast.
        // could replace this with a 64 bit int sort over (i0,i1)
        // with i0 as msb in the quicksort call above.
        int iEntries = iNrTrianglesIn * 3;
        int iCurStartIndex = 0;
        for (int i = 1; i < iEntries; i++) {
            if (pEdges[iCurStartIndex].getI0() != pEdges[i].getI0()) {
                final int iL = iCurStartIndex;
                final int iR = i - 1;
                //final int iElems = i-iL;
                iCurStartIndex = i;
                quickSortEdges(pEdges, iL, iR, 1, uSeed);  // sort channel 1 which is i1
            }
        }

        // sub sort over f, which should be fast.
        // this step is to remain compliant with BuildNeighborsSlow() when
        // more than 2 triangles use the same edge (such as a butterfly topology).
        iCurStartIndex = 0;
        for (int i = 1; i < iEntries; i++) {
            if (pEdges[iCurStartIndex].getI0() != pEdges[i].getI0() || pEdges[iCurStartIndex].getI1() != pEdges[i].getI1()) {
                final int iL = iCurStartIndex;
                final int iR = i - 1;
                //final int iElems = i-iL;
                iCurStartIndex = i;
                quickSortEdges(pEdges, iL, iR, 2, uSeed);  // sort channel 2 which is f
            }
        }

        // pair up, adjacent triangles
        for (int i = 0; i < iEntries; i++) {
            final int i0 = pEdges[i].getI0();
            final int i1 = pEdges[i].getI1();
            final int g = pEdges[i].getF();
            boolean bUnassigned_A;

            int[] i0_A = new int[1];
            int[] i1_A = new int[1];
            int[] edgenum_A = new int[1];
            int[] edgenum_B = new int[1];
            //int edgenum_B=0;  // 0,1 or 2
            int[] triList = new int[3];
            System.arraycopy(piTriListIn, g * 3, triList, 0, 3);
            getEdge(i0_A, i1_A, edgenum_A, triList, i0, i1);  // resolve index ordering and edge_num
            bUnassigned_A = pTriInfos[g].faceNeighbors[edgenum_A[0]] == -1;

            if (bUnassigned_A) {
                // get true index ordering
                int j = i + 1, t;
                boolean bNotFound = true;
                while (j < iEntries && i0 == pEdges[j].getI0() && i1 == pEdges[j].getI1() && bNotFound) {
                    boolean bUnassigned_B;
                    int[] i0_B = new int[1];
                    int[] i1_B = new int[1];
                    t = pEdges[j].getF();
                    // flip i0_B and i1_B
                    System.arraycopy(piTriListIn, t * 3, triList, 0, 3);
                    getEdge(i1_B, i0_B, edgenum_B, triList, pEdges[j].getI0(), pEdges[j].getI1());  // resolve index ordering and edge_num
                    //assert(!(i0_A==i1_B && i1_A==i0_B));
                    bUnassigned_B = pTriInfos[t].faceNeighbors[edgenum_B[0]] == -1;
                    if (i0_A[0] == i0_B[0] && i1_A[0] == i1_B[0] && bUnassigned_B) {
                        bNotFound = false;
                    } else {
                        ++j;
                    }
                }

                if (!bNotFound) {
                    int t2 = pEdges[j].getF();
                    pTriInfos[g].faceNeighbors[edgenum_A[0]] = t2;
                    //assert(pTriInfos[t].FaceNeighbors[edgenum_B]==-1);
                    pTriInfos[t2].faceNeighbors[edgenum_B[0]] = g;
                }
            }
        }
    }

    static void buildNeighborsSlow(TriInfo pTriInfos[], final int piTriListIn[], final int iNrTrianglesIn) {
        
        for (int f = 0; f < iNrTrianglesIn; f++) {
            for (int i = 0; i < 3; i++) {
                // if unassigned
                if (pTriInfos[f].faceNeighbors[i] == -1) {
                    final int i0_A = piTriListIn[f * 3 + i];
                    final int i1_A = piTriListIn[f * 3 + (i < 2 ? (i + 1) : 0)];

                    // search for a neighbor
                    boolean bFound = false;
                    int t = 0, j = 0;
                    while (!bFound && t < iNrTrianglesIn) {
                        if (t != f) {
                            j = 0;
                            while (!bFound && j < 3) {
                                // in rev order
                                final int i1_B = piTriListIn[t * 3 + j];
                                final int i0_B = piTriListIn[t * 3 + (j < 2 ? (j + 1) : 0)];
                                //assert(!(i0_A==i1_B && i1_A==i0_B));
                                if (i0_A == i0_B && i1_A == i1_B) {
                                    bFound = true;
                                } else {
                                    ++j;
                                }
                            }
                        }

                        if (!bFound) {
                            ++t;
                        }
                    }

                    // assign neighbors
                    if (bFound) {
                        pTriInfos[f].faceNeighbors[i] = t;
                        //assert(pTriInfos[t].FaceNeighbors[j]==-1);
                        pTriInfos[t].faceNeighbors[j] = f;
                    }
                }
            }
        }
    }

    static void quickSortEdges(Edge[] pSortBuffer, int iLeft, int iRight, final int channel, long uSeed) {
        // early out
        Edge sTmp;
        final int iElems = iRight - iLeft + 1;
        if (iElems < 2) {
            return;
        } else if (iElems == 2) {
            if (pSortBuffer[iLeft].array[channel] > pSortBuffer[iRight].array[channel]) {
                sTmp = pSortBuffer[iLeft];
                pSortBuffer[iLeft] = pSortBuffer[iRight];
                pSortBuffer[iRight] = sTmp;
            }
            return;
        }

        // Random
        long t = uSeed & 31;
        t = (uSeed << t) | (uSeed >> (32 - t));
        uSeed = uSeed + t + 3;
        // Random end
        
        uSeed = uSeed & 0xffffffffL;

        int iL = iLeft;
        int iR = iRight;
        int n = (iR - iL) + 1;
        assert (n >= 0);
        int index = (int) (uSeed % n);

        int iMid = pSortBuffer[index + iL].array[channel];

        do {
            while (pSortBuffer[iL].array[channel] < iMid) {
                ++iL;
            }
            while (pSortBuffer[iR].array[channel] > iMid) {
                --iR;
            }

            if (iL <= iR) {
                sTmp = pSortBuffer[iL];
                pSortBuffer[iL] = pSortBuffer[iR];
                pSortBuffer[iR] = sTmp;
                ++iL;
                --iR;
            }
        } while (iL <= iR);

        if (iLeft < iR) {
            quickSortEdges(pSortBuffer, iLeft, iR, channel, uSeed);
        }
        if (iL < iRight) {
            quickSortEdges(pSortBuffer, iL, iRight, channel, uSeed);
        }
    }

// resolve ordering and edge number
    static void getEdge(int[] i0_out, int[] i1_out, int[] edgenum_out, final int[] indices, final int i0_in, final int i1_in) {
        edgenum_out[0] = -1;

        // test if first index is on the edge
        if (indices[0] == i0_in || indices[0] == i1_in) {
            // test if second index is on the edge
            if (indices[1] == i0_in || indices[1] == i1_in) {
                edgenum_out[0] = 0;  // first edge
                i0_out[0] = indices[0];
                i1_out[0] = indices[1];
            } else {
                edgenum_out[0] = 2;  // third edge
                i0_out[0] = indices[2];
                i1_out[0] = indices[0];
            }
        } else {
            // only second and third index is on the edge
            edgenum_out[0] = 1;  // second edge
            i0_out[0] = indices[1];
            i1_out[0] = indices[2];
        }
    }

    static void degenPrologue(TriInfo pTriInfos[], int piTriList_out[], final int iNrTrianglesIn, final int iTotTris) {
        
        // locate quads with only one good triangle
        int t = 0;
        while (t < (iTotTris - 1)) {
            final int iFO_a = pTriInfos[t].orgFaceNumber;
            final int iFO_b = pTriInfos[t + 1].orgFaceNumber;
            if (iFO_a == iFO_b) {
                // this is a quad
                final boolean bIsDeg_a = (pTriInfos[t].flag & MARK_DEGENERATE) != 0;
                final boolean bIsDeg_b = (pTriInfos[t + 1].flag & MARK_DEGENERATE) != 0;
                //TODO nehon : Check this in detail as this operation is utterly strange
                if ((bIsDeg_a ^ bIsDeg_b) != false) {
                    pTriInfos[t].flag |= QUAD_ONE_DEGEN_TRI;
                    pTriInfos[t + 1].flag |= QUAD_ONE_DEGEN_TRI;
                }
                t += 2;
            } else {
                ++t;
            }
        }

        // reorder list so all degen triangles are moved to the back
        // without reordering the good triangles
        int iNextGoodTriangleSearchIndex = 1;
        t = 0;
        boolean bStillFindingGoodOnes = true;
        while (t < iNrTrianglesIn && bStillFindingGoodOnes) {
            final boolean bIsGood = (pTriInfos[t].flag & MARK_DEGENERATE) == 0;
            if (bIsGood) {
                if (iNextGoodTriangleSearchIndex < (t + 2)) {
                    iNextGoodTriangleSearchIndex = t + 2;
                }
            } else {                
                // search for the first good triangle.
                boolean bJustADegenerate = true;
                while (bJustADegenerate && iNextGoodTriangleSearchIndex < iTotTris) {
                    final boolean bIsGood2 = (pTriInfos[iNextGoodTriangleSearchIndex].flag & MARK_DEGENERATE) == 0;
                    if (bIsGood2) {
                        bJustADegenerate = false;
                    } else {
                        ++iNextGoodTriangleSearchIndex;
                    }
                }

                int t0 = t;
                int t1 = iNextGoodTriangleSearchIndex;
                ++iNextGoodTriangleSearchIndex;
                assert (iNextGoodTriangleSearchIndex > (t + 1));

                // swap triangle t0 and t1
                if (!bJustADegenerate) {                    
                    for (int i = 0; i < 3; i++) {
                        final int index = piTriList_out[t0 * 3 + i];
                        piTriList_out[t0 * 3 + i] = piTriList_out[t1 * 3 + i];
                        piTriList_out[t1 * 3 + i] = index;
                    }
                    {
                        final TriInfo tri_info = pTriInfos[t0];
                        pTriInfos[t0] = pTriInfos[t1];
                        pTriInfos[t1] = tri_info;
                    }
                } else {
                    bStillFindingGoodOnes = false;  // this is not supposed to happen
                }
            }

            if (bStillFindingGoodOnes) {
                ++t;
            }
        }

        assert (bStillFindingGoodOnes);  // code will still work.
        assert (iNrTrianglesIn == t);
    }

    static void DegenEpilogue(TSpace psTspace[], TriInfo pTriInfos[], int piTriListIn[], final MikkTSpaceContext mikkTSpace, final int iNrTrianglesIn, final int iTotTris) {
        
        // deal with degenerate triangles
        // punishment for degenerate triangles is O(N^2)
        for (int t = iNrTrianglesIn; t < iTotTris; t++) {
            // degenerate triangles on a quad with one good triangle are skipped
            // here but processed in the next loop
            final boolean bSkip = (pTriInfos[t].flag & QUAD_ONE_DEGEN_TRI) != 0;

            if (!bSkip) {
                for (int i = 0; i < 3; i++) {
                    final int index1 = piTriListIn[t * 3 + i];
                    // search through the good triangles
                    boolean bNotFound = true;
                    int j = 0;
                    while (bNotFound && j < (3 * iNrTrianglesIn)) {
                        final int index2 = piTriListIn[j];
                        if (index1 == index2) {
                            bNotFound = false;
                        } else {
                            ++j;
                        }
                    }

                    if (!bNotFound) {
                        final int iTri = j / 3;
                        final int iVert = j % 3;
                        final int iSrcVert = pTriInfos[iTri].vertNum[iVert];
                        final int iSrcOffs = pTriInfos[iTri].tSpacesOffs;
                        final int iDstVert = pTriInfos[t].vertNum[i];
                        final int iDstOffs = pTriInfos[t].tSpacesOffs;

                        // copy tspace
                        psTspace[iDstOffs + iDstVert] = psTspace[iSrcOffs + iSrcVert];
                    }
                }
            }
        }

        // deal with degenerate quads with one good triangle
        for (int t = 0; t < iNrTrianglesIn; t++) {
            // this triangle belongs to a quad where the
            // other triangle is degenerate
            if ((pTriInfos[t].flag & QUAD_ONE_DEGEN_TRI) != 0) {
               
                byte[] pV = pTriInfos[t].vertNum;
                int iFlag = (1 << pV[0]) | (1 << pV[1]) | (1 << pV[2]);
                int iMissingIndex = 0;
                if ((iFlag & 2) == 0) {
                    iMissingIndex = 1;
                } else if ((iFlag & 4) == 0) {
                    iMissingIndex = 2;
                } else if ((iFlag & 8) == 0) {
                    iMissingIndex = 3;
                }

                int iOrgF = pTriInfos[t].orgFaceNumber;
                Vector3f vDstP = getPosition(mikkTSpace, makeIndex(iOrgF, iMissingIndex));
                boolean bNotFound = true;
                int i = 0;
                while (bNotFound && i < 3) {
                    final int iVert = pV[i];
                    final Vector3f vSrcP = getPosition(mikkTSpace, makeIndex(iOrgF, iVert));
                    if (vSrcP.equals(vDstP)) {
                        final int iOffs = pTriInfos[t].tSpacesOffs;
                        psTspace[iOffs + iMissingIndex] = psTspace[iOffs + iVert];
                        bNotFound = false;
                    } else {
                        ++i;
                    }
                }
                assert (!bNotFound);
            }
        }

    }    

    /**
     * SubGroup inner class
     */
    private static class SubGroup {
        int nrFaces;
        int[] triMembers;
    }

    private static class Group {
        int nrFaces;
        List<Integer> faceIndices = new ArrayList<Integer>();
        int vertexRepresentitive;
        boolean orientPreservering;
    }

    private static class TriInfo {

        int[] faceNeighbors = new int[3];
        Group[] assignedGroup = new Group[3];

        // normalized first order face derivatives
        Vector3f os = new Vector3f();
        Vector3f ot = new Vector3f();
        float magS, magT;  // original magnitudes

        // determines if the current and the next triangle are a quad.
        int orgFaceNumber;
        int flag, tSpacesOffs;
        byte[] vertNum = new byte[4];
    }

    private static class TSpace {

        Vector3f os = new Vector3f();
        float magS;
        Vector3f ot = new Vector3f();
        float magT;
        int counter;  // this is to average back into quads.
        boolean orient;
        
        void set(TSpace ts){
            os.set(ts.os);
            magS = ts.magS;
            ot.set(ts.ot);
            magT = ts.magT;
            counter = ts.counter;
            orient = ts.orient;
        }
    }

    private static class TmpVert {

        float vert[] = new float[3];
        int index;
    }

    private static class Edge {

        void setI0(int i){            
            array[0] = i;
        }
        
        void setI1(int i){            
            array[1] = i;
        }
        
        void setF(int i){            
            array[2] = i;
        }
        
        int getI0(){            
            return array[0];
        }
        
        int getI1(){            
            return array[1];
        }
        
        int getF(){            
            return array[2];
        }
        
        int[] array = new int[3];
    }

}
