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

package com.jme3.collision;

import com.jme3.math.*;

/**
 * No longer public ..
 *
 * @author Kirill Vainer
 */
@Deprecated
class SweepSphere implements Collidable {

    private Vector3f velocity = new Vector3f();
    private Vector3f center = new Vector3f();
    private Vector3f dimension = new Vector3f();
    private Vector3f invDim = new Vector3f();

    private final Triangle scaledTri = new Triangle();
    private final Plane triPlane = new Plane();
    private final Vector3f temp1 = new Vector3f(),
                           temp2 = new Vector3f(),
                           temp3 = new Vector3f();
    private final Vector3f sVelocity = new Vector3f(),
                           sCenter = new Vector3f();

    public Vector3f getCenter() {
        return center;
    }

    public void setCenter(Vector3f center) {
        this.center.set(center);
    }

    public Vector3f getDimension() {
        return dimension;
    }

    public void setDimension(Vector3f dimension) {
        this.dimension.set(dimension);
        this.invDim.set(1,1,1).divideLocal(dimension);
    }

    public void setDimension(float x, float y, float z){
        this.dimension.set(x,y,z);
        this.invDim.set(1,1,1).divideLocal(dimension);
    }

    public void setDimension(float dim){
        this.dimension.set(dim, dim, dim);
        this.invDim.set(1,1,1).divideLocal(dimension);
    }

    public Vector3f getVelocity() {
        return velocity;
    }

    public void setVelocity(Vector3f velocity) {
        this.velocity.set(velocity);
    }

    private boolean pointsOnSameSide(Vector3f p1, Vector3f p2, Vector3f line1, Vector3f line2) {
        // V1 = (line2 - line1) x (p1    - line1)
        // V2 = (p2    - line1) x (line2 - line1)

        temp1.set(line2).subtractLocal(line1);
        temp3.set(temp1);
        temp2.set(p1).subtractLocal(line1);
        temp1.crossLocal(temp2);

        temp2.set(p2).subtractLocal(line1);
        temp3.crossLocal(temp2);

        // V1 . V2 >= 0
        return temp1.dot(temp3) >= 0;
    }

    private boolean isPointInTriangle(Vector3f point, AbstractTriangle tri) {
            if (pointsOnSameSide(point, tri.get1(), tri.get2(), tri.get3())
             && pointsOnSameSide(point, tri.get2(), tri.get1(), tri.get3())
             && pointsOnSameSide(point, tri.get3(), tri.get1(), tri.get2()))
                    return true;
            return false;
    }

    private static float getLowestRoot(float a, float b, float c, float maxR) {
        float determinant = b * b - 4f * a * c;
        if (determinant < 0){
            return Float.NaN;
        }

        float sqrtd = FastMath.sqrt(determinant);
        float r1 = (-b - sqrtd) / (2f * a);
        float r2 = (-b + sqrtd) / (2f * a);

        if (r1 > r2){
            float temp = r2;
            r2 = r1;
            r1 = temp;
        }

        if (r1 > 0 && r1 < maxR){
            return r1;
        }

        if (r2 > 0 && r2 < maxR){
            return r2;
        }

        return Float.NaN;
    }

    private float collideWithVertex(Vector3f sCenter, Vector3f sVelocity,
                                    float velocitySquared, Vector3f vertex, float t) {
        // A = velocity * velocity
        // B = 2 * (velocity . (center - vertex));
        // C = ||vertex - center||^2 - 1f;

        temp1.set(sCenter).subtractLocal(vertex);
        float a = velocitySquared;
        float b = 2f * sVelocity.dot(temp1);
        float c = temp1.negateLocal().lengthSquared() - 1f;
        float newT = getLowestRoot(a, b, c, t);

//        float A = velocitySquared;
//        float B = sCenter.subtract(vertex).dot(sVelocity) * 2f;
//        float C = vertex.subtract(sCenter).lengthSquared() - 1f;
//
//        float newT = getLowestRoot(A, B, C, Float.MAX_VALUE);
//        if (newT > 1.0f)
//            newT = Float.NaN;
        
        return newT;
    }

    private float collideWithSegment(Vector3f sCenter,
                                     Vector3f sVelocity,
                                     float velocitySquared,
                                     Vector3f l1,
                                     Vector3f l2,
                                     float t,
                                     Vector3f store) {
        Vector3f edge = temp1.set(l2).subtractLocal(l1);
        Vector3f base = temp2.set(l1).subtractLocal(sCenter);

        float edgeSquared = edge.lengthSquared();
        float baseSquared = base.lengthSquared();

        float EdotV = edge.dot(sVelocity);
        float EdotB = edge.dot(base);

        float a = (edgeSquared * -velocitySquared) + EdotV * EdotV;
        float b = (edgeSquared * 2f * sVelocity.dot(base))
                - (2f * EdotV * EdotB);
        float c = (edgeSquared * (1f - baseSquared)) + EdotB * EdotB;
        float newT = getLowestRoot(a, b, c, t);
        if (!Float.isNaN(newT)){
            float f = (EdotV * newT - EdotB) / edgeSquared;
            if (f >= 0f && f < 1f){
                store.scaleAdd(f, edge, l1);
                return newT;
            }
        }
        return Float.NaN;
    }

    private CollisionResult collideWithTriangle(AbstractTriangle tri){
        // scale scaledTriangle based on dimension
        scaledTri.get1().set(tri.get1()).multLocal(invDim);
        scaledTri.get2().set(tri.get2()).multLocal(invDim);
        scaledTri.get3().set(tri.get3()).multLocal(invDim);
//        Vector3f sVelocity = velocity.mult(invDim);
//        Vector3f sCenter = center.mult(invDim);
        velocity.mult(invDim, sVelocity);
        center.mult(invDim, sCenter);

        triPlane.setPlanePoints(scaledTri);

        float normalDotVelocity = triPlane.getNormal().dot(sVelocity);
        // XXX: sVelocity.normalize() needed?
        // back facing scaledTriangles not considered
        if (normalDotVelocity > 0f)
            return null;

        float t0, t1;
        boolean embedded = false;

        float signedDistanceToPlane = triPlane.pseudoDistance(sCenter);
        if (normalDotVelocity == 0.0f){
            // we are travelling exactly parrallel to the plane
            if (FastMath.abs(signedDistanceToPlane) >= 1.0f){
                // no collision possible
                return null;
            }else{
                // we are embedded
                t0 = 0;
                t1 = 1;
                embedded = true;
                System.out.println("EMBEDDED");
                return null;
            }
        }else{
            t0 = (-1f - signedDistanceToPlane) / normalDotVelocity;
            t1 = ( 1f - signedDistanceToPlane) / normalDotVelocity;

            if (t0 > t1){
                float tf = t1;
                t1 = t0;
                t0 = tf;
            }

            if (t0 > 1.0f || t1 < 0.0f){
                // collision is out of this sVelocity range
                return null;
            }

            // clamp the interval to [0, 1]
            t0 = Math.max(t0, 0.0f);
            t1 = Math.min(t1, 1.0f);
        }

        boolean foundCollision = false;
        float minT = 1f;

        Vector3f contactPoint = new Vector3f();
        Vector3f contactNormal = new Vector3f();
  
//        if (!embedded){
            // check against the inside of the scaledTriangle
            // contactPoint = sCenter - p.normal + t0 * sVelocity
            contactPoint.set(sVelocity);
            contactPoint.multLocal(t0);
            contactPoint.addLocal(sCenter);
            contactPoint.subtractLocal(triPlane.getNormal());

            // test to see if the collision is on a scaledTriangle interior
            if (isPointInTriangle(contactPoint, scaledTri) && !embedded){
                foundCollision = true;

                minT = t0;

                // scale collision point back into R3
                contactPoint.multLocal(dimension);
                contactNormal.set(velocity).multLocal(t0);
                contactNormal.addLocal(center);
                contactNormal.subtractLocal(contactPoint).normalizeLocal();

//                contactNormal.set(triPlane.getNormal());
                
                CollisionResult result = new CollisionResult();
                result.setContactPoint(contactPoint);
                result.setContactNormal(contactNormal);
                result.setDistance(minT * velocity.length());
                return result;
            }
//        }

        float velocitySquared = sVelocity.lengthSquared();

        Vector3f v1 = scaledTri.get1();
        Vector3f v2 = scaledTri.get2();
        Vector3f v3 = scaledTri.get3();

        // vertex 1
        float newT;
        newT = collideWithVertex(sCenter, sVelocity, velocitySquared, v1, minT);
        if (!Float.isNaN(newT)){
            minT = newT;
            contactPoint.set(v1);
            foundCollision = true;
        }

        // vertex 2
        newT = collideWithVertex(sCenter, sVelocity, velocitySquared, v2, minT);
        if (!Float.isNaN(newT)){
            minT = newT;
            contactPoint.set(v2);
            foundCollision = true;
        }

        // vertex 3
        newT = collideWithVertex(sCenter, sVelocity, velocitySquared, v3, minT);
        if (!Float.isNaN(newT)){
            minT = newT;
            contactPoint.set(v3);
            foundCollision = true;
        }

        // edge 1-2
        newT = collideWithSegment(sCenter, sVelocity, velocitySquared, v1, v2, minT, contactPoint);
        if (!Float.isNaN(newT)){
            minT = newT;
            foundCollision = true;
        }

        // edge 2-3
        newT = collideWithSegment(sCenter, sVelocity, velocitySquared, v2, v3, minT, contactPoint);
        if (!Float.isNaN(newT)){
            minT = newT;
            foundCollision = true;
        }

        // edge 3-1
        newT = collideWithSegment(sCenter, sVelocity, velocitySquared, v3, v1, minT, contactPoint);
        if (!Float.isNaN(newT)){
            minT = newT;
            foundCollision = true;
        }

        if (foundCollision){
            // compute contact normal based on minimum t
            contactPoint.multLocal(dimension);
            contactNormal.set(velocity).multLocal(t0);
            contactNormal.addLocal(center);
            contactNormal.subtractLocal(contactPoint).normalizeLocal();

            CollisionResult result = new CollisionResult();
            result.setContactPoint(contactPoint);
            result.setContactNormal(contactNormal);
            result.setDistance(minT * velocity.length());

            return result;
        }else{
            return null;
        }
    }

    public CollisionResult collideWithSweepSphere(SweepSphere other){
        temp1.set(velocity).subtractLocal(other.velocity);
        temp2.set(center).subtractLocal(other.center);
        temp3.set(dimension).addLocal(other.dimension);
        // delta V
        // delta C
        // Rsum

        float a = temp1.lengthSquared();
        float b = 2f * temp1.dot(temp2);
        float c = temp2.lengthSquared() - temp3.getX() * temp3.getX();
        float t = getLowestRoot(a, b, c, 1);

        // no collision
        if (Float.isNaN(t))
            return null;

        CollisionResult result = new CollisionResult();
        result.setDistance(velocity.length() * t);

        temp1.set(velocity).multLocal(t).addLocal(center);
        temp2.set(other.velocity).multLocal(t).addLocal(other.center);
        temp3.set(temp2).subtractLocal(temp1);
        // temp3 contains center to other.center vector

        // normalize it to get normal
        temp2.set(temp3).normalizeLocal();
        result.setContactNormal(new Vector3f(temp2));

        // temp3 is contact point
        temp3.set(temp2).multLocal(dimension).addLocal(temp1);
        result.setContactPoint(new Vector3f(temp3));
        
        return result;
    }

    public static void main(String[] args){
        SweepSphere ss = new SweepSphere();
        ss.setCenter(Vector3f.ZERO);
        ss.setDimension(1);
        ss.setVelocity(new Vector3f(10, 10, 10));

        SweepSphere ss2 = new SweepSphere();
        ss2.setCenter(new Vector3f(5, 5, 5));
        ss2.setDimension(1);
        ss2.setVelocity(new Vector3f(-10, -10, -10));

        CollisionResults cr = new CollisionResults();
        ss.collideWith(ss2, cr);
        if (cr.size() > 0){
            CollisionResult c = cr.getClosestCollision();
            System.out.println("D = "+c.getDistance());
            System.out.println("P = "+c.getContactPoint());
            System.out.println("N = "+c.getContactNormal());
        }
    }

    public int collideWith(Collidable other, CollisionResults results)
            throws UnsupportedCollisionException {
        if (other instanceof AbstractTriangle){
            AbstractTriangle tri = (AbstractTriangle) other;
            CollisionResult result = collideWithTriangle(tri);
            if (result != null){
                results.addCollision(result);
                return 1;
            }
            return 0;
        }else if (other instanceof SweepSphere){
            SweepSphere sph = (SweepSphere) other;

            CollisionResult result = collideWithSweepSphere(sph);
            if (result != null){
                results.addCollision(result);
                return 1;
            }
            return 0;
        }else{
            throw new UnsupportedCollisionException();
        }
    }

}
