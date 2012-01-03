package jme3tools.navigation;
import java.awt.Point;
import java.text.DecimalFormat;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


/**
 * A representation of the actual map in terms of lat/long and x,y co-ordinates.
 * The Map class contains various helper methods such as methods for determining
 * the pixel positions for lat/long co-ordinates and vice versa.
 *
 * @author Cormac Gebruers
 * @author Benjamin Jakobus
 * @version 1.0
 * @since 1.0
 */
public class MapModel2D {

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
    private int yCentre;

    /* The width (in pixels) of the viewport holding the map */
    private int viewportWidth;

    /* The viewport height in pixels */
    private int viewportHeight;

    /* The number of minutes that one pixel represents */
    private double minutesPerPixel;

    /**
     * Constructor
     * @param viewportWidth the pixel width of the viewport (component) in which
     *        the map is displayed
     * @since 1.0
     */
    public MapModel2D(int viewportWidth) {
        try {
            this.centre = new Position(0, 0);
        } catch (InvalidPositionException e) {
            e.printStackTrace();
        }

        this.viewportWidth = viewportWidth;

        // Calculate the number of minutes that one pixel represents along the longitude
        calculateMinutesPerPixel(DEFAULT_MAP_WIDTH_LONGITUDE);

        // Calculate the viewport height based on its width and the number of degrees (85)
        // in our map
        viewportHeight = ((int) NavCalculator.computeDMPClarkeSpheroid(0, 85) / (int) minutesPerPixel) * 2;
//        viewportHeight = viewportWidth; // REMOVE!!!
        // Determine the map's x,y centre
        xCentre = viewportWidth / 2;
        yCentre = viewportHeight / 2;
    }

    /**
     * Returns the height of the viewport in pixels
     * @return the height of the viewport in pixels
     * @since 0.1
     */
    public int getViewportPixelHeight() {
        return viewportHeight;
    }

    /**
     * Calculates the number of minutes per pixels using a given
     * map width in longitude
     * @param mapWidthInLongitude
     * @since 1.0
     */
    public void calculateMinutesPerPixel(double mapWidthInLongitude) {
        minutesPerPixel = (mapWidthInLongitude * 60) / (double) viewportWidth;
    }

    /**
     * Returns the width of the viewport in pixels
     * @return the width of the viewport in pixels
     * @since 0.1
     */
    public int getViewportPixelWidth() {
        return viewportWidth;
    }

    public void setViewportWidth(int viewportWidth) {
        this.viewportWidth = viewportWidth;
    }

    public void setViewportHeight(int viewportHeight) {
        this.viewportHeight = viewportHeight;
    }

    public void setCentre(Position centre) {
        this.centre = centre;
    }

    /**
     * Returns the number of minutes there are per pixel
     * @return the number of minutes per pixel
     * @since 1.0
     */
    public double getMinutesPerPixel() {
        return minutesPerPixel;
    }

    public double getMetersPerPixel() {
        return 1853 * minutesPerPixel;
    }

    public void setMinutesPerPixel(double minutesPerPixel) {
        this.minutesPerPixel = minutesPerPixel;
    }

    /**
     * Converts a latitude/longitude position into a pixel co-ordinate
     * @param position the position to convert
     * @return {@code Point} a pixel co-ordinate
     * @since 1.0
     */
    public Point toPixel(Position position) {
        // Get the distance between position and the centre for calculating
        // the position's longitude translation
        double distance = NavCalculator.computeLongDiff(centre.getLongitude(),
                position.getLongitude());

        // Use the distance from the centre to calculate the pixel x co-ordinate
        double distanceInPixels = (distance / minutesPerPixel);

        // Use the difference in meridional parts to calculate the pixel y co-ordinate
        double dmp = NavCalculator.computeDMPClarkeSpheroid(centre.getLatitude(),
                position.getLatitude());

        int x = 0;
        int y = 0;

        if (centre.getLatitude() == position.getLatitude()) {
            y = yCentre;
        }
        if (centre.getLongitude() == position.getLongitude()) {
            x = xCentre;
        }

        // Distinguish between northern and southern hemisphere for latitude calculations
        if (centre.getLatitude() > 0 && position.getLatitude() > centre.getLatitude()) {
            // Centre is north. Position is north of centre
            y = yCentre + (int) ((dmp) / minutesPerPixel);
        } else if (centre.getLatitude() > 0 && position.getLatitude() < centre.getLatitude()) {
            // Centre is north. Position is south of centre
            y = yCentre - (int) ((dmp) / minutesPerPixel);
        } else if (centre.getLatitude() < 0 && position.getLatitude() > centre.getLatitude()) {
            // Centre is south. Position is north of centre
            y = yCentre + (int) ((dmp) / minutesPerPixel);
        } else if (centre.getLatitude() < 0 && position.getLatitude() < centre.getLatitude()) {
            // Centre is south. Position is south of centre
            y = yCentre - (int) ((dmp) / minutesPerPixel);
        } else if (centre.getLatitude() == 0 && position.getLatitude() > centre.getLatitude()) {
            // Centre is at the equator. Position is north of the equator
            y = yCentre + (int) ((dmp) / minutesPerPixel);
        } else if (centre.getLatitude() == 0 && position.getLatitude() < centre.getLatitude()) {
            // Centre is at the equator. Position is south of the equator
            y = yCentre - (int) ((dmp) / minutesPerPixel);
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

        // Distinguish between northern and souterhn hemisphere for longitude calculations
        return new Point(x, y);
    }

    /**
     * Converts a pixel position into a mercator position
     * @param p {@link Point} object that you wish to convert into
     *        longitude / latiude
     * @return the converted {@code Position} object
     * @since 1.0
     */
    public Position toPosition(Point p) {
        double lat, lon;
        Position pos = null;
        try {
            Point pixelCentre = toPixel(new Position(0, 0));

            // Get the distance between position and the centre
            double xDistance = distance(xCentre, p.getX());
            double yDistance = distance(pixelCentre.getY(), p.getY());
            double lonDistanceInDegrees = (xDistance * minutesPerPixel) / 60;
            double mp = (yDistance * minutesPerPixel);
            // If we are zoomed in past a certain point, then use linear search.
            // Otherwise use binary search
            if (getMinutesPerPixel() < 0.05) {
                lat = findLat(mp, getCentre().getLatitude());
                if (lat == -1000) {
                    System.out.println("lat: " + lat);
                }
            } else {
                lat = findLat(mp, 0.0, 85.0);
            }
            lon = (p.getX() < xCentre ? centre.getLongitude() - lonDistanceInDegrees
                    : centre.getLongitude() + lonDistanceInDegrees);

            if (p.getY() > pixelCentre.getY()) {
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
     * Calculates distance between two points on the map in pixels
     * @param a
     * @param b
     * @return distance the distance between a and b in pixels
     * @since 1.0
     */
    private double distance(double a, double b) {
        return Math.abs(a - b);
    }

    /**
     * Defines the centre of the map in pixels
     * @param p <code>Point</code> object denoting the map's new centre
     * @since 1.0
     */
    public void setCentre(Point p) {
        try {
            Position newCentre = toPosition(p);
            if (newCentre != null) {
                centre = newCentre;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets the map's xCentre
     * @param xCentre
     * @since 1.0
     */
    public void setXCentre(int xCentre) {
        this.xCentre = xCentre;
    }

    /**
     * Sets the map's yCentre
     * @param yCentre
     * @since 1.0
     */
    public void setYCentre(int yCentre) {
        this.yCentre = yCentre;
    }

    /**
     * Returns the pixel (x,y) centre of the map
     * @return {@link Point) object marking the map's (x,y) centre
     * @since 1.0
     */
    public Point getPixelCentre() {
        return new Point(xCentre, yCentre);
    }

    /**
     * Returns the {@code Position} centre of the map
     * @return {@code Position} object marking the map's (lat, long) centre
     * @since 1.0
     */
    public Position getCentre() {
        return centre;
    }

    /**
     * Uses binary search to find the latitude of a given MP.
     *
     * @param mp maridian part
     * @param low
     * @param high
     * @return the latitude of the MP value
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
     * Uses linear search to find the latitude of a given MP
     * @param mp the meridian part for which to find the latitude
     * @param previousLat the previous latitude. Used as a upper / lower bound
     * @return the latitude of the MP value
     */
    private double findLat(double mp, double previousLat) {
        DecimalFormat form = new DecimalFormat("#.#####");
        mp = Double.parseDouble(form.format(mp));
        double guessMP;
        for (double lat = previousLat - 0.25; lat < previousLat + 1; lat += 0.00001) {
            guessMP = NavCalculator.computeDMPClarkeSpheroid(0, lat);
            guessMP = Double.parseDouble(form.format(guessMP));
            if (guessMP == mp || Math.abs(guessMP - mp) < 0.001) {
                return lat;
            }
        }
        return -1000;
    }
}
