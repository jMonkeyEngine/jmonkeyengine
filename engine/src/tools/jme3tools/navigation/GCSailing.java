/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jme3tools.navigation;

/**
 * A utility class to package up a great circle sailing.
 * 
 * @author Benjamin Jakobus, based on JMarine (by Cormac Gebruers and Benjamin
 *          Jakobus)
 *
 * @version 1.0
 * @since 1.0
 */
public class GCSailing {

    private int[] courses;
    private float[] distancesNM;

    public GCSailing(int[] pCourses, float[] pDistancesNM) {
        courses = pCourses;
        distancesNM = pDistancesNM;
    }

    public int[] getCourses() {
        return courses;
    }

    public float[] getDistancesNM() {
        return distancesNM;
    }
}
