/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.light;

import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.material.logic.TiledRenderGrid;
import com.jme3.math.Matrix4f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.renderer.Camera;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 *
 * @author codex
 */
public class LightFrustum {
    
    private float left, right, top, bottom;
    
    private Matrix4f vp;
    private float camLeftCoeff = -1;
    private float camTopCoeff = -1;
    private float viewPortWidth = -1;
    private float viewPortHeight = -1;
    private final float[] matArray1 = new float[16];
    private final Vector3f tempVec3 = new Vector3f();
    private final Vector3f tempVec3_2 = new Vector3f();
    private final Vector4f tempVec4 = new Vector4f();
    private final Vector4f tempvec4_2 = new Vector4f();
    private final Vector4f tempVec4_3 = new Vector4f();
    private final Vector4f camUp = new Vector4f();
    private final Vector4f camLeft = new Vector4f();
    private final Vector4f lightLeft = new Vector4f();
    private final Vector4f lightUp = new Vector4f();
    private final Vector4f lightCenter = new Vector4f();
    
    public LightFrustum calculateCamera(Camera cam) {
        viewPortWidth = cam.getWidth() * 0.5f;
        viewPortHeight = cam.getHeight() * 0.5f;
        vp = cam.getViewProjectionMatrix();
        Matrix4f v = cam.getViewMatrix();
        v.get(matArray1);
        tempVec3.set(matArray1[0], matArray1[1], matArray1[2]);
        camLeftCoeff = 1.0f / cam.getWorldPlane(1).getNormal().dot(tempVec3);
        tempVec3.set(matArray1[4], matArray1[5], matArray1[6]);
        camTopCoeff = 1.0f / cam.getWorldPlane(2).getNormal().dot(tempVec3);
        camLeft.set(matArray1[0], matArray1[1], matArray1[2], -1.0f).multLocal(-1.0f);
        camUp.set(matArray1[4], matArray1[5], matArray1[6], 1.0f);
        return this;
    }
    
    public LightFrustum fromLight(Light l) {
        switch (l.getType()) {
            case Directional:
                return fromDirectional((DirectionalLight)l);
            case Point:
                return fromPoint((PointLight)l);
            case Spot:
                return fromSpot((SpotLight)l);
            case Ambient:
                return fromAmbient((AmbientLight)l);
            default:
                throw new UnsupportedOperationException("Light type "+l.getType()+" is not supported.");
        }
    }
    public LightFrustum fromDirectional(DirectionalLight dl) {
        return fullscreen();
    }
    public LightFrustum fromPoint(PointLight pl) {
        
        float r = pl.getRadius();
        if(r <= 0)return null;
        float lr = r * camLeftCoeff;
        float tr = r * camTopCoeff;
        tempVec4.set(pl.getPosition().x, pl.getPosition().y, pl.getPosition().z, 1.0f);
        Vector4f center = tempVec4;
        tempvec4_2.w = 1.0f;
        tempVec4_3.w = 1.0f;

        camLeft.mult(lr, tempvec4_2);
        tempvec4_2.addLocal(center);
        Vector4f lightFrustumLeft = tempvec4_2;
        lightFrustumLeft.w = 1.0f;

        camUp.mult(tr, tempVec4_3);
        tempVec4_3.addLocal(center);
        Vector4f lightFrustumUp = tempVec4_3;
        lightFrustumUp.w = 1.0f;

        vp.mult(lightFrustumLeft, lightLeft);
        vp.mult(lightFrustumUp, lightUp);
        vp.mult(center, lightCenter);

        lightLeft.x /= lightLeft.w;
        lightLeft.y /= lightLeft.w;

        lightUp.x /= lightUp.w;
        lightUp.y /= lightUp.w;

        lightCenter.x /= lightCenter.w;
        lightCenter.y /= lightCenter.w;

        lightLeft.x = viewPortWidth * (1.0f + lightLeft.x);
        lightUp.x = viewPortWidth * (1.0f + lightUp.x);
        lightCenter.x = viewPortWidth * (1.0f + lightCenter.x);

        lightLeft.y = viewPortHeight * (1.0f - lightLeft.y);
        lightUp.y = viewPortHeight * (1.0f - lightUp.y);
        lightCenter.y = viewPortHeight * (1.0f - lightCenter.y);

        // light frustum rect
        float lw = Math.abs(lightLeft.x - lightCenter.x);
        float lh = Math.abs(lightCenter.y - lightUp.y);
        float l, b;
        if(lightCenter.z < -lightCenter.w){
            l = -lightCenter.x - lw;
            b = -lightCenter.y + lh;
        } else {
            l = lightCenter.x - lw;
            b = lightCenter.y + lh;
        }
        b = viewPortHeight * 2.0f - b;
        this.left = l;
        this.right = lw * 2.0f + l;
        this.top = lh * 2.0f + b;
        this.bottom = b;
        
        return this;
        
    }
    public LightFrustum fromSpot(SpotLight sl) {
        return fullscreen();
    }
    public LightFrustum fromAmbient(AmbientLight al) {
        return fullscreen();
    }
    public LightFrustum fullscreen() {
        left = bottom = 0;
        right = viewPortWidth;
        top = viewPortHeight;
        return this;
    }
    
    public void write(ArrayList<LinkedList<Integer>> tileIndices, TiledRenderGrid tileInfo, int lightIndex) {
        int width = tileInfo.getGridWidth();
        int numTiles = tileInfo.getNumTiles();
        int tileLeft = (int)Math.max(Math.floor(left / tileInfo.getTileSize()), 0);
        int tileRight = (int)Math.min(Math.ceil(right / tileInfo.getTileSize()), width);
        int tileBottom = (int)Math.max(Math.floor(bottom / tileInfo.getTileSize()), 0);
        int tileTop = (int)Math.min(Math.ceil(top / tileInfo.getTileSize()), tileInfo.getGridHeight());
        for (int b = tileBottom; b < tileTop; b++) {
            int base = b*width;
            for (int l = tileLeft; l < tileRight; l++) {
                int tileId = l+base;
                if (tileId >= 0 && tileId < numTiles) {
                    tileIndices.get(tileId).add(lightIndex);
                }
            }
        }
    }
    
}
