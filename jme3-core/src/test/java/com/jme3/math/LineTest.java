package com.jme3.math;

import org.junit.Test;
import static org.junit.Assert.*;

public class LineTest {

    public Vector3f findNearestPoint(Line line, Vector3f point){

        /*
         * The line's information
         */
        float x = line.getOrigin().x;
        float y = line.getOrigin().y;
        float z = line.getOrigin().z;
        float vecX = line.getDirection().x - line.getOrigin().x;
        float vecY = line.getDirection().y - line.getOrigin().y;
        float vecZ = line.getDirection().z - line.getOrigin().z;

        /*
         * The dot product of the vector(the nearest point on the line with parameter to the specific point) and the direction of the line should be 0.
         */
        float con = vecX*(x-point.x) + vecY*(y-point.y) + vecZ*(z-point.z);
        float parameter = -con/(vecX*vecX + vecY*vecY + vecZ*vecZ);
        return new Vector3f(x+vecX*parameter,y+vecY*parameter,z+vecZ*parameter);

    }

    public float distanceByVectorEquation(Line line, Vector3f point){
        Vector3f nearestPoint = findNearestPoint(line,point);
        float squaredDis = (nearestPoint.x-point.x)*(nearestPoint.x-point.x)+(nearestPoint.y-point.y)*(nearestPoint.y-point.y)+(nearestPoint.z-point.z)*(nearestPoint.z-point.z);
        return FastMath.sqrt(squaredDis);
    }

    public float distanceByCrossProduct(Line line, Vector3f point) {

        Vector3f u = new Vector3f(line.getDirection().x - line.getOrigin().x,
                line.getDirection().y - line.getOrigin().y,
                line.getDirection().z - line.getOrigin().z);

        Vector3f pq = new Vector3f(point.x - line.getOrigin().x,
                point.y - line.getOrigin().y,
                point.z - line.getOrigin().z);

        float distance =  pq.cross(u).length() / u.length();

        return distance;
    }

    /**
     * To test the distance of the line and the specific point
     *
     */
    @Test
    public void testDistance() {

        /*
         * Setup the tested point and line.
         */
        Vector3f origin = new Vector3f(0,70,0);
        Vector3f direction = new Vector3f(1,8,0);
        Vector3f point = new Vector3f(32,1,8);

        Line line = new Line();
        line.setOrigin(origin);
        line.setDirection(direction);

        /*
         * Assert origin and direction are set properly.
         */
        assert origin == line.getOrigin();
        assert direction == line.getDirection();

        /*
         * Get distance by different ways.
         */
        float disByLineClass = line.distance(point); // Get the distance by Line class
        float distanceByVectorEquation = distanceByVectorEquation(line,point); // Get the distance by vector equation in the above method
        float distanceByCrossProduct = distanceByCrossProduct(line, point);

        /*
         * Verify that the distance that calculated by different methods are the same.
         */
        assertEquals(disByLineClass,distanceByVectorEquation,0.01f);
        assertEquals(disByLineClass,distanceByCrossProduct,0.01f);

    }

}
