/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jme3tools.navigation;

import com.jme3.math.Vector3f;
import java.text.DecimalFormat;


/**
 * A representation of the actual map in terms of lat/long and x,y,z co-ordinates.
 * The Map class contains various helper methods such as methods for determining
 * the world unit positions for lat/long coordinates and vice versa. This map projection
 * does not handle screen/pixel coordinates.
 *
 * @author Benjamin Jakobus (thanks to Cormac Gebruers)
 * @version 1.0
 * @since 1.0
 */
public class MapModel3D {

    /* The number of radians per degree */
    private final static double RADIANS_PER_DEGREE = 57.2957;

    /* The number of degrees per radian */
    private final static double DEGREES_PER_RADIAN = 0.0174532925;

    /* The map's width in longitude */
    public final static int DEFAULT_MAP_WIDTH_LONGITUDE = 360;

    /* The top right hand corner of the map */
    private Position centre;

    /* The x and y co-ordinates for the viewport's centre */
    private int xCentre;
    private int zCentre;

    /* The width (in world units (wu)) of the viewport holding the map */
    private int worldWidth;

    /* The viewport height in pixels */
    private int worldHeight;

    /* The number of minutes that one pixel represents */
    private double minutesPerWorldUnit;

    /**
     * Constructor.
     * 
     * @param worldWidth         The world unit width the map's area
     * @since 1.0
     */
    public MapModel3D(int worldWidth) {
        try {
            this.centre = new Position(0, 0);
        } catch (InvalidPositionException e) {
            e.printStackTrace();
        }

        this.worldWidth = worldWidth;

        // Calculate the number of minutes that one pixel represents along the longitude
        calculateMinutesPerWorldUnit(DEFAULT_MAP_WIDTH_LONGITUDE);

        // Calculate the viewport height based on its width and the number of degrees (85)
        // in our map
        worldHeight = ((int) NavCalculator.computeDMPClarkeSpheroid(0, 85) / (int) minutesPerWorldUnit) * 2;

        // Determine the map's x,y centre
        xCentre = 0;
        zCentre = 0;
//        xCentre = worldWidth / 2;
//        zCentre = worldHeight / 2;
    }

    /**
     * Returns the height of the viewport in pixels.
     *
     * @return          The height of the viewport in pixels.
     * @since 1.0
     */
    public int getWorldHeight() {
        return worldHeight;
    }

    /**
     * Calculates the number of minutes per pixels using a given
     * map width in longitude.
     *
     * @param mapWidthInLongitude               The map's with in degrees of longitude.
     * @since 1.0
     */
    public void calculateMinutesPerWorldUnit(double mapWidthInLongitude) {
        // Multiply mapWidthInLongitude by 60 to convert it to minutes.
        minutesPerWorldUnit = (mapWidthInLongitude * 60) / (double) worldWidth;
    }

    /**
     * Returns the width of the viewport in pixels.
     *
     * @return              The width of the viewport in pixels.
     * @since 1.0
     */
    public int getWorldWidth() {
        return worldWidth;
    }

    /**
     * Sets the world's desired width.
     *
     * @param viewportWidth     The world's desired width in WU.
     * @since 1.0
     */
    public void setWorldWidth(int viewportWidth) {
        this.worldWidth = viewportWidth;
    }

     /**
     * Sets the world's desired height.
     *
     * @param viewportHeight     The world's desired height in WU.
     * @since 1.0
     */
    public void setWorldHeight(int viewportHeight) {
        this.worldHeight = viewportHeight;
    }

    /**
     * Sets the map's centre.
     *
     * @param centre            The <code>Position</code> denoting the map's
     *                          desired centre.
     * @since 1.0
     */
    public void setCentre(Position centre) {
        this.centre = centre;
    }

    /**
     * Returns the number of minutes there are per WU.
     *
     * @return                  The number of minutes per WU.
     * @since 1.0
     */
    public double getMinutesPerWu() {
        return minutesPerWorldUnit;
    }

    /**
     * Returns the meters per WU.
     *
     * @return                  The meters per WU.
     * @since 1.0
     */
    public double getMetersPerWu() {
        return 1853 * minutesPerWorldUnit;
    }

    /**
     * Converts a latitude/longitude position into a WU coordinate.
     *
     * @param position          The <code>Position</code> to convert.
     * @return                  The <code>Point</code> a pixel coordinate.
     * @since 1.0
     */
    public Vector3f toWorldUnit(Position position) {
        // Get the difference between position and the centre for calculating
        // the position's longitude translation
        double distance = NavCalculator.computeLongDiff(centre.getLongitude(),
                position.getLongitude());

        // Use the difference from the centre to calculate the pixel x co-ordinate
        double distanceInPixels = (distance / minutesPerWorldUnit);

        // Use the difference in meridional parts to calculate the pixel y co-ordinate
        double dmp = NavCalculator.computeDMPClarkeSpheroid(centre.getLatitude(),
                position.getLatitude());

        int x = 0;
        int z = 0;

        if (centre.getLatitude() == position.getLatitude()) {
            z = zCentre;
        }
        if (centre.getLongitude() == position.getLongitude()) {
            x = xCentre;
        }

        // Distinguish between northern and southern hemisphere for latitude calculations
        if (centre.getLatitude() > 0 && position.getLatitude() > centre.getLatitude()) {
            // Centre is north. Position is north of centre
            z = zCentre - (int) ((dmp) / minutesPerWorldUnit);
        } else if (centre.getLatitude() > 0 && position.getLatitude() < centre.getLatitude()) {
            // Centre is north. Position is south of centre
            z = zCentre + (int) ((dmp) / minutesPerWorldUnit);
        } else if (centre.getLatitude() < 0 && position.getLatitude() > centre.getLatitude()) {
            // Centre is south. Position is north of centre
            z = zCentre - (int) ((dmp) / minutesPerWorldUnit);
        } else if (centre.getLatitude() < 0 && position.getLatitude() < centre.getLatitude()) {
            // Centre is south. Position is south of centre
            z = zCentre + (int) ((dmp) / minutesPerWorldUnit);
        } else if (centre.getLatitude() == 0 && position.getLatitude() > centre.getLatitude()) {
            // Centre is at the equator. Position is north of the equator
            z = zCentre - (int) ((dmp) / minutesPerWorldUnit);
        } else if (centre.getLatitude() == 0 && position.getLatitude() < centre.getLatitude()) {
            // Centre is at the equator. Position is south of the equator
            z = zCentre + (int) ((dmp) / minutesPerWorldUnit);
        }

        // Distinguish between western and eastern hemisphere for longitude calculations
        if (centre.getLongitude() < 0 && position.getLongitude() < centre.getLongitude()) {
            // Centre is west. Position is west of centre
            x = xCentre - (int) distanceInPixels;
        } else if (centre.getLongitude() < 0 && position.getLongitude() > centre.getLongitude()) {
            // Centre is west. Position is south of centre
            x = xCentre + (int) distanceInPixels;
        } else if (centre.getLongitude() > 0 && position.getLongitude() < centre.getLongitude()) {
            // Centre is east. Position is west of centre
            x = xCentre - (int) distanceInPixels;
        } else if (centre.getLongitude() > 0 && position.getLongitude() > centre.getLongitude()) {
            // Centre is east. Position is east of centre
            x = xCentre + (int) distanceInPixels;
        } else if (centre.getLongitude() == 0 && position.getLongitude() > centre.getLongitude()) {
            // Centre is at the equator. Position is east of centre
            x = xCentre + (int) distanceInPixels;
        } else if (centre.getLongitude() == 0 && position.getLongitude() < centre.getLongitude()) {
            // Centre is at the equator. Position is west of centre
            x = xCentre - (int) distanceInPixels;
        }

        // Distinguish between northern and southern hemisphere for longitude calculations
        return new Vector3f(x, 0, z);
    }

    /**
     * Converts a world position into a Mercator position.
     *
     * @param posVec                     <code>Vector</code> containing the world unit 
     *                              coordinates that are to be converted into
     *                              longitude / latitude coordinates.
     * @return                      The resulting <code>Position</code> in degrees of
     *                              latitude and longitude.
     * @since 1.0
     */
    public Position toPosition(Vector3f posVec) {
        double lat, lon;
        Position pos = null;
        try {
            Vector3f worldCentre = toWorldUnit(new Position(0, 0));

            // Get the difference between position and the centre
            double xDistance = difference(xCentre, posVec.getX());
            double yDistance = difference(worldCentre.getZ(), posVec.getZ());
            double lonDistanceInDegrees = (xDistance * minutesPerWorldUnit) / 60;
            double mp = (yDistance * minutesPerWorldUnit);
            // If we are zoomed in past a certain point, then use linear search.
            // Otherwise use binary search
            if (getMinutesPerWu() < 0.05) {
                lat = findLat(mp, getCentre().getLatitude());
                if (lat == -1000) {
                    System.out.println("lat: " + lat);
                }
            } else {
                lat = findLat(mp, 0.0, 85.0);
            }
            lon = (posVec.getX() < xCentre ? centre.getLongitude() - lonDistanceInDegrees
                    : centre.getLongitude() + lonDistanceInDegrees);

            if (posVec.getZ() > worldCentre.getZ()) {
                lat = -1 * lat;
            }
            if (lat == -1000 || lon == -1000) {
                return pos;
            }
            pos = new Position(lat, lon);
        } catch (InvalidPositionException ipe) {
            ipe.printStackTrace();
        }
        return pos;
    }

    /**
     * Calculates difference between two points on the map in WU.
     *
     * @param a                     
     * @param b
     * @return difference           The difference between a and b in WU.
     * @since 1.0
     */
    private double difference(double a, double b) {
        return Math.abs(a - b);
    }

    /**
     * Defines the centre of the map in pixels.
     *
     * @param posVec             <code>Vector3f</code> object denoting the map's new centre.
     * @since 1.0
     */
    public void setCentre(Vector3f posVec) {
        try {
            Position newCentre = toPosition(posVec);
            if (newCentre != null) {
                centre = newCentre;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the WU (x,y,z) centre of the map.
     * 
     * @return              <code>Vector3f</code> object marking the map's (x,y) centre.
     * @since 1.0
     */
    public Vector3f getCentreWu() {
        return new Vector3f(xCentre, 0, zCentre);
    }

    /**
     * Returns the <code>Position</code> centre of the map.
     *
     * @return              <code>Position</code> object marking the map's (lat, long)
     *                      centre.
     * @since 1.0
     */
    public Position getCentre() {
        return centre;
    }

    /**
     * Uses binary search to find the latitude of a given MP.
     *
     * @param mp                Maridian part whose latitude to determine.
     * @param low               Minimum latitude bounds.
     * @param high              Maximum latitude bounds.
     * @return                  The latitude of the MP value
     * @since 1.0
     */
    private double findLat(double mp, double low, double high) {
        DecimalFormat form = new DecimalFormat("#.####");
        mp = Math.round(mp);
        double midLat = (low + high) / 2.0;
        // ctr is used to make sure that with some
        // numbers which can't be represented exactly don't inifitely repeat
        double guessMP = NavCalculator.computeDMPClarkeSpheroid(0, (float) midLat);

        while (low <= high) {
            if (guessMP == mp) {
                return midLat;
            } else {
                if (guessMP > mp) {
                    high = midLat - 0.0001;
                } else {
                    low = midLat + 0.0001;
                }
            }

            midLat = Double.valueOf(form.format(((low + high) / 2.0)));
            guessMP = NavCalculator.computeDMPClarkeSpheroid(0, (float) midLat);
            guessMP = Math.round(guessMP);
        }
        return -1000;
    }

    /**
     * Uses linear search to find the latitude of a given MP.
     *
     * @param mp                The meridian part for which to find the latitude.
     * @param previousLat       The previous latitude. Used as a upper / lower bound.
     * @return                  The latitude of the MP value.
     * @since 1.0
     */
    private double findLat(double mp, double previousLat) {
        DecimalFormat form = new DecimalFormat("#.#####");
        mp = Double.parseDouble(form.format(mp));
        double guessMP;
        for (double lat = previousLat - 0.25; lat < previousLat + 1; lat += 0.00001) {
            guessMP = NavCalculator.computeDMPClarkeSpheroid(0, lat);
            guessMP = Double.parseDouble(form.format(guessMP));
            if (guessMP == mp || Math.abs(guessMP - mp) < 0.05) {
                return lat;
            }
        }
        return -1000;
    }
}
