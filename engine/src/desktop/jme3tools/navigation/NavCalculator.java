/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jme3tools.navigation;



/**
 * A utlity class for performing position calculations
 *
 * @author Benjamin Jakobus, based on JMarine (by Cormac Gebruers and Benjamin
 *          Jakobus)
 * @version 1.0
 * @since 1.0
 */
public class NavCalculator {

    private double distance;
    private double trueCourse;

    /* The earth's radius in meters */
    public static final int WGS84_EARTH_RADIUS = 6378137;
    private String strCourse;

    /* The sailing calculation type */
    public static final int MERCATOR = 0;
    public static final int GC = 1;

    /* The degree precision to use for courses */
    public static final int RL_CRS_PRECISION = 1;

    /* The distance precision to use for distances */
    public static final int RL_DIST_PRECISION = 1;
    public static final int METERS_PER_MINUTE = 1852;

    /**
     * Constructor
     * @param P1
     * @param P2
     * @param calcType
     * @since 1.0
     */
    public NavCalculator(Position P1, Position P2, int calcType) {
        switch (calcType) {
            case MERCATOR:
                mercatorSailing(P1, P2);
                break;
            case GC:
                greatCircleSailing(P1, P2);
                break;
        }
    }

    /**
     * Constructor
     * @since 1.0
     */
    public NavCalculator() {
    }

    /**
     * Determines a great circle track between two positions
     * @param p1 origin position
     * @param p2 destination position
     */
    public GCSailing greatCircleSailing(Position p1, Position p2) {
        return new GCSailing(new int[0], new float[0]);
    }

    /**
     * Determines a Rhumb Line course and distance between two points
     * @param p1 origin position
     * @param p2 destination position
     */
    public RLSailing rhumbLineSailing(Position p1, Position p2) {
        RLSailing rl = mercatorSailing(p1, p2);
        return rl;
    }

    /**
     * Determines the rhumb line course  and distance between two positions
     * @param p1 origin position
     * @param p2 destination position
     */
    public RLSailing mercatorSailing(Position p1, Position p2) {

        double dLat = computeDLat(p1.getLatitude(), p2.getLatitude());
        //plane sailing...
        if (dLat == 0) {
            RLSailing rl = planeSailing(p1, p2);
            return rl;
        }

        double dLong = computeDLong(p1.getLongitude(), p2.getLongitude());
        double dmp = (float) computeDMPClarkeSpheroid(p1.getLatitude(), p2.getLatitude());

        trueCourse = (float) Math.toDegrees(Math.atan(dLong / dmp));
        double degCrs = convertCourse((float) trueCourse, p1, p2);
        distance = (float) Math.abs(dLat / Math.cos(Math.toRadians(trueCourse)));

        RLSailing rl = new RLSailing(degCrs, (float) distance);
        trueCourse = rl.getCourse();
        strCourse = (dLat < 0 ? "S" : "N");
        strCourse += " " + trueCourse;
        strCourse += " " + (dLong < 0 ? "W" : "E");
        return rl;

    }

    /**
     * Calculate a plane sailing situation - i.e. where Lats are the same 
     * @param p1
     * @param p2
     * @return
     * @since 1.0
     */
    public RLSailing planeSailing(Position p1, Position p2) {
        double dLong = computeDLong(p1.getLongitude(), p2.getLongitude());

        double sgnDLong = 0 - (dLong / Math.abs(dLong));
        if (Math.abs(dLong) > 180 * 60) {
            dLong = (360 * 60 - Math.abs(dLong)) * sgnDLong;
        }

        double redist = 0;
        double recourse = 0;
        if (p1.getLatitude() == 0) {
            redist = Math.abs(dLong);
        } else {
            redist = Math.abs(dLong * (float) Math.cos(p1.getLatitude() * 2 * Math.PI / 360));
        }
        recourse = (float) Math.asin(0 - sgnDLong);
        recourse = recourse * 360 / 2 / (float) Math.PI;

        if (recourse < 0) {
            recourse = recourse + 360;
        }
        return new RLSailing(recourse, redist);
    }

    /**
     * Converts a course from cardinal XddY to ddd notation
     * @param tc
     * @param p1 position one
     * @param p2 position two
     * @return
     * @since 1.0
     */
    public static double convertCourse(float tc, Position p1, Position p2) {

        double dLat = p1.getLatitude() - p2.getLatitude();
        double dLong = p1.getLongitude() - p2.getLongitude();
        //NE
        if (dLong >= 0 & dLat >= 0) {
            return Math.abs(tc);
        }

        //SE
        if (dLong >= 0 & dLat < 0) {
            return 180 - Math.abs(tc);
        }

        //SW
        if (dLong < 0 & dLat < 0) {
            return 180 + Math.abs(tc);
        }

        //NW
        if (dLong < 0 & dLat >= 0) {
            return 360 - Math.abs(tc);
        }
        return -1;
    }

    /**
     * Getter method for the distance between two points
     * @return distance
     * @since 1.0
     */
    public double getDistance() {
        return distance;
    }

    /**
     * Getter method for the true course
     * @return true course
     * @since 1.0
     */
    public double getTrueCourse() {
        return trueCourse;
    }

    /**
     * Getter method for the true course
     * @return true course
     * @since 1.0
     */
    public String getStrCourse() {
        return strCourse;
    }

    /**
     * Computes the difference in meridional parts for two latitudes in minutes
     * (based on Clark 1880 spheroid)
     * @param lat1
     * @param lat2
     * @return difference in minutes
     * @since 1.0
     */
    public static double computeDMPClarkeSpheroid(double lat1, double lat2) {
        double absLat1 = Math.abs(lat1);
        double absLat2 = Math.abs(lat2);

        double m1 = (7915.704468 * (Math.log(Math.tan(Math.toRadians(45
                + (absLat1 / 2)))) / Math.log(10))
                - 23.268932 * Math.sin(Math.toRadians(absLat1))
                - 0.052500 * Math.pow(Math.sin(Math.toRadians(absLat1)), 3)
                - 0.000213 * Math.pow(Math.sin(Math.toRadians(absLat1)), 5));

        double m2 = (7915.704468 * (Math.log(Math.tan(Math.toRadians(45
                + (absLat2 / 2)))) / Math.log(10))
                - 23.268932 * Math.sin(Math.toRadians(absLat2))
                - 0.052500 * Math.pow(Math.sin(Math.toRadians(absLat2)), 3)
                - 0.000213 * Math.pow(Math.sin(Math.toRadians(absLat2)), 5));
        if ((lat1 <= 0 && lat2 <= 0) || (lat1 > 0 && lat2 > 0)) {
            return Math.abs(m1 - m2);
        } else {
            return m1 + m2;
        }
    }

    /**
     * Computes the difference in meridional parts for a perfect sphere between
     * two degrees of latitude
     * @param lat1
     * @param lat2
     * @return difference in meridional parts between lat1 and lat2 in minutes
     * @since 1.0
     */
    public static float computeDMPWGS84Spheroid(float lat1, float lat2) {
        float absLat1 = Math.abs(lat1);
        float absLat2 = Math.abs(lat2);

        float m1 = (float) (7915.7045 * Math.log10(Math.tan(Math.toRadians(45 + (absLat1 / 2))))
                - 23.01358 * Math.sin(absLat1 - 0.05135) * Math.pow(Math.sin(absLat1), 3));

        float m2 = (float) (7915.7045 * Math.log10(Math.tan(Math.toRadians(45 + (absLat2 / 2))))
                - 23.01358 * Math.sin(absLat2 - 0.05135) * Math.pow(Math.sin(absLat2), 3));

        if (lat1 <= 0 & lat2 <= 0 || lat1 > 0 & lat2 > 0) {
            return Math.abs(m1 - m2);
        } else {
            return m1 + m2;
        }
    }

    /**
     * Predicts the position of a target for a given time in the future
     * @param time the number of seconds from now for which to predict the future
     *        position
     * @param speed the miles per minute that the target is traveling
     * @param currentLat the target's current latitude
     * @param currentLong the target's current longitude
     * @param course the target's current course in degrees
     * @return the predicted future position
     * @since 1.0
     */
    public static Position predictPosition(int time, double speed,
            double currentLat, double currentLong, double course) {
        Position futurePosition = null;
        course = Math.toRadians(course);
        double futureLong = currentLong + speed * time * Math.sin(course);
        double futureLat = currentLat + speed * time * Math.cos(course);
        try {
            futurePosition = new Position(futureLat, futureLong);
        } catch (InvalidPositionException ipe) {
            ipe.printStackTrace();
        }
        return futurePosition;

    }

    /**
     * Computes the coordinate of position B relative to an offset given
     * a distance and an angle.
     *
     * @param offset        The offset position.
     * @param bearing       The bearing between the offset and the coordinate
     *                      that you want to calculate.
     * @param distance      The distance, in meters, between the offset
     *                      and point B.
     * @return              The position of point B that is located from
     *                      given offset at given distance and angle.
     * @since 1.0
     */
    public static Position computePosition(Position initialPos, double heading,
            double distance) {
        if (initialPos == null) {
            return null;
        }
        double angle;
        if (heading < 90) {
            angle = heading;
        } else if (heading > 90 && heading < 180) {
            angle = 180 - heading;
        } else if (heading > 180 && heading < 270) {
            angle = heading - 180;
        } else {
            angle = 360 - heading;
        }

        Position newPosition = null;

        // Convert meters into nautical miles
        distance = distance * 0.000539956803;
        angle = Math.toRadians(angle);
        double initialLat = initialPos.getLatitude();
        double initialLong = initialPos.getLongitude();
        double dlat = distance * Math.cos(angle);
        dlat = dlat / 60;
        dlat = Math.abs(dlat);
        double newLat = 0;
        if ((heading > 270 && heading < 360) || (heading > 0 && heading < 90)) {
            newLat = initialLat + dlat;
        } else if (heading < 270 && heading > 90) {
            newLat = initialLat - dlat;
        }
        double meanLat = (Math.abs(dlat) / 2.0) + newLat;
        double dep = (Math.abs(dlat * 60)) * Math.tan(angle);
        double dlong = dep * (1.0 / Math.cos(Math.toRadians(meanLat)));
        dlong = dlong / 60;
        dlong = Math.abs(dlong);
        double newLong;
        if (heading > 180 && heading < 360) {
            newLong = initialLong - dlong;
        } else {
            newLong = initialLong + dlong;
        }

        if (newLong < -180) {
            double diff = Math.abs(newLong + 180);
            newLong = 180 - diff;
        }

        if (newLong > 180) {
            double diff = Math.abs(newLong + 180);
            newLong = (180 - diff) * -1;
        }

        if (heading == 0 || heading == 360 || heading == 180) {
            newLong = initialLong;
            newLat = initialLat + dlat;
        } else if (heading == 90 || heading == 270) {
            newLat = initialLat;
//            newLong = initialLong + dlong; THIS WAS THE ORIGINAL (IT WORKED)
            newLong = initialLong - dlong;
        }
        try {
            newPosition = new Position(newLat,
                    newLong);
        } catch (InvalidPositionException ipe) {
            ipe.printStackTrace();
            System.out.println(newLat + "," + newLong);
        }
        return newPosition;
    }

    /**
     * Computes the difference in Longitude between two positions and assigns the
     * correct sign -westwards travel, + eastwards travel
     * @param lng1
     * @param lng2
     * @return difference in longitude
     * @since 1.0
     */
    public static double computeDLong(double lng1, double lng2) {
        if (lng1 - lng2 == 0) {
            return 0;
        }

        // both easterly
        if (lng1 >= 0 & lng2 >= 0) {
            return -(lng1 - lng2) * 60;
        }
        //both westerly
        if (lng1 < 0 & lng2 < 0) {
            return -(lng1 - lng2) * 60;
        }

        //opposite sides of Date line meridian

        //sum less than 180
        if (Math.abs(lng1) + Math.abs(lng2) < 180) {
            if (lng1 < 0 & lng2 > 0) {
                return -(Math.abs(lng1) + Math.abs(lng2)) * 60;
            } else {
                return Math.abs(lng1) + Math.abs(lng2) * 60;
            }
        } else {
            //sum greater than 180
            if (lng1 < 0 & lng2 > 0) {
                return -(360 - (Math.abs(lng1) + Math.abs(lng2))) * 60;
            } else {
                return (360 - (Math.abs(lng1) + Math.abs(lng2))) * 60;
            }
        }
    }

    /**
     * Computes the difference in Longitude between two positions and assigns the
     * correct sign -westwards travel, + eastwards travel
     * @param lng1
     * @param lng2
     * @return difference in longitude
     * @since 1.0
     */
    public static double computeLongDiff(double lng1, double lng2) {
        if (lng1 - lng2 == 0) {
            return 0;
        }

        // both easterly
        if (lng1 >= 0 & lng2 >= 0) {
            return Math.abs(-(lng1 - lng2) * 60);
        }
        //both westerly
        if (lng1 < 0 & lng2 < 0) {
            return Math.abs(-(lng1 - lng2) * 60);
        }

        if (lng1 == 0) {
            return Math.abs(lng2 * 60);
        }

        if (lng2 == 0) {
            return Math.abs(lng1 * 60);
        }

        return (Math.abs(lng1) + Math.abs(lng2)) * 60;
    }

    /**
     * Compute the difference in latitude between two positions
     * @param lat1
     * @param lat2
     * @return difference in latitude
     * @since 1.0
     */
    public static double computeDLat(double lat1, double lat2) {
        //same side of equator

        //plane sailing
        if (lat1 - lat2 == 0) {
            return 0;
        }

        //both northerly
        if (lat1 >= 0 & lat2 >= 0) {
            return -(lat1 - lat2) * 60;
        }
        //both southerly
        if (lat1 < 0 & lat2 < 0) {
            return -(lat1 - lat2) * 60;
        }

        //opposite sides of equator
        if (lat1 >= 0) {
            //heading south
            return -(Math.abs(lat1) + Math.abs(lat2));
        } else {
            //heading north
            return (Math.abs(lat1) + Math.abs(lat2));
        }
    }

    public static class Quadrant {

        private static final Quadrant FIRST = new Quadrant(1, 1);
        private static final Quadrant SECOND = new Quadrant(-1, 1);
        private static final Quadrant THIRD = new Quadrant(-1, -1);
        private static final Quadrant FOURTH = new Quadrant(1, -1);
        private final int lonMultiplier;
        private final int latMultiplier;

        public Quadrant(final int xMultiplier, final int yMultiplier) {
            this.lonMultiplier = xMultiplier;
            this.latMultiplier = yMultiplier;
        }

        static Quadrant getQuadrant(double degrees, boolean invert) {
            if (invert) {
                if (degrees >= 0 && degrees <= 90) {
                    return FOURTH;
                } else if (degrees > 90 && degrees <= 180) {
                    return THIRD;
                } else if (degrees > 180 && degrees <= 270) {
                    return SECOND;
                }
                return FIRST;
            } else {
                if (degrees >= 0 && degrees <= 90) {
                    return FIRST;
                } else if (degrees > 90 && degrees <= 180) {
                    return SECOND;
                } else if (degrees > 180 && degrees <= 270) {
                    return THIRD;
                }
                return FOURTH;
            }
        }
    }

    /**
     * Converts meters to degrees.
     *
     * @param meters            The meters that you want to convert into degrees.
     * @return                  The degree equivalent of the given meters.
     * @since 1.0
     */
    public static double toDegrees(double meters) {
        return (meters / METERS_PER_MINUTE) / 60;
    }

    /**
     * Computes the bearing between two points.
     * 
     * @param p1
     * @param p2
     * @return
     * @since 1.0
     */
    public static int computeBearing(Position p1, Position p2) {
        int bearing;
        double dLon = computeDLong(p1.getLongitude(), p2.getLongitude());
        double y = Math.sin(dLon) * Math.cos(p2.getLatitude());
        double x = Math.cos(p1.getLatitude()) * Math.sin(p2.getLatitude())
                - Math.sin(p1.getLatitude()) * Math.cos(p2.getLatitude()) * Math.cos(dLon);
        bearing = (int) Math.toDegrees(Math.atan2(y, x));
        return bearing;
    }

    /**
     * Computes the angle between two points.
     *
     * @param p1
     * @param p2
     * @return
     */
    public static int computeAngle(Position p1, Position p2) {
        // cos (adj / hyp)
        double adj = Math.abs(p1.getLongitude() - p2.getLongitude());
        double opp = Math.abs(p1.getLatitude() - p2.getLatitude());
        return (int) Math.toDegrees(Math.atan(opp / adj));

//        int angle = (int)Math.atan2(p2.getLatitude() - p1.getLatitude(),
//                p2.getLongitude() - p1.getLongitude());
        //Actually it's ATan2(dy , dx) where dy = y2 - y1 and dx = x2 - x1, or ATan(dy / dx)
    }

    public static int computeHeading(Position p1, Position p2) {
        int angle = computeAngle(p1, p2);
        // NE
        if (p2.getLongitude() >= p1.getLongitude() && p2.getLatitude() >= p1.getLatitude()) {
            return angle;
        } else if (p2.getLongitude() >= p1.getLongitude() && p2.getLatitude() <= p1.getLatitude()) {
            // SE
            return 90 + angle;
        } else if (p2.getLongitude() <= p1.getLongitude() && p2.getLatitude() <= p1.getLatitude()) {
            // SW
            return 270 - angle;
        } else {
            // NW
            return 270 + angle;
        }
    }

    public static void main(String[] args) {
        try {
            int pos = NavCalculator.computeHeading(new Position(0, 0), new Position(10, -10));
//            System.out.println(pos.getLatitude() + "," + pos.getLongitude());
            System.out.println(pos);
        } catch (Exception e) {
        }





    }
}
