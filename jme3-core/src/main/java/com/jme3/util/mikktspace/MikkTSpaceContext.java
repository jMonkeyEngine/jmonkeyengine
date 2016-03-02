/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.util.mikktspace;

/**
 *
 * @author Nehon
 */
public interface MikkTSpaceContext {

    /**
     * Returns the number of faces (triangles/quads) on the mesh to be
     * processed.
     *
     * @return
     */
    public int getNumFaces();

    /**
     * Returns the number of vertices on face number iFace iFace is a number in
     * the range {0, 1, ..., getNumFaces()-1}
     *
     * @param face
     * @return
     */
    public int getNumVerticesOfFace(int face);

    /**
     * returns the position/normal/texcoord of the referenced face of vertex
     * number iVert. iVert is in the range {0,1,2} for triangles and {0,1,2,3}
     * for quads.
     *
     * @param posOut
     * @param face
     * @param vert
     */
    public void getPosition(float posOut[], int face, int vert);

    public void getNormal(float normOut[], int face, int vert);

    public void getTexCoord(float texOut[], int face, int vert);

    /**
     * The call-backsetTSpaceBasic() is sufficient for basic normal mapping.
     * This function is used to return the tangent and sign to the application.
     * tangent is a unit length vector. For normal maps it is sufficient to use
     * the following simplified version of the bitangent which is generated at
     * pixel/vertex level.
     *
     * bitangent = fSign * cross(vN, tangent);
     *
     * Note that the results are returned unindexed. It is possible to generate
     * a new index list But averaging/overwriting tangent spaces by using an
     * already existing index list WILL produce INCRORRECT results. DO NOT! use
     * an already existing index list.
     *
     * @param tangent
     * @param sign
     * @param face
     * @param vert
     */
    public void setTSpaceBasic(float tangent[], float sign, int face, int vert);

    /**
     * This function is used to return tangent space results to the application.
     * tangent and biTangent are unit length vectors and fMagS and fMagT are
     * their true magnitudes which can be used for relief mapping effects.
     *
     * biTangent is the "real" bitangent and thus may not be perpendicular to
     * tangent. However, both are perpendicular to the vertex normal. For normal
     * maps it is sufficient to use the following simplified version of the
     * bitangent which is generated at pixel/vertex level.
     *
     * <pre>
     * fSign = bIsOrientationPreserving ? 1.0f : (-1.0f);
     * bitangent = fSign * cross(vN, tangent);
     * </pre>
     *
     * Note that the results are returned unindexed. It is possible to generate
     * a new index list. But averaging/overwriting tangent spaces by using an
     * already existing index list WILL produce INCRORRECT results. DO NOT! use
     * an already existing index list.
     *
     * @param tangent
     * @param biTangent
     * @param magS
     * @param magT
     * @param isOrientationPreserving
     * @param face
     * @param vert
     */
    void setTSpace(float tangent[], float biTangent[], float magS, float magT,
            boolean isOrientationPreserving, int face, int vert);
}
