/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jme3tools.navigation;

import java.text.DecimalFormat;

/**
 * Coordinate class. Used to store a coordinate in [DD]D MM.M format.
 *
 * @author Benjamin Jakobus (based on JMarine by Benjamin Jakobus and Cormac Gebruers)
 * @version 1.0
 * @since 1.0
 */
public class Coordinate {

    /* the degree part of the position (+ N/E, -W/S) */
    private int deg;

    /* the decimals of a minute */
    private double minsDecMins;

    /* the coordinate as a decimal*/
    private double decCoordinate;

    /* whether this coordinate is a latitude or a longitude: : LAT==0, LONG==1  */
    private int coOrdinate;

    /* The minutes trailing decimal precision to use for positions */
    public static final int MINPRECISION = 4;
    /* The degrees trailing decimal precision to use for positions */
    public static final int DEGPRECISION = 7;

    /* typeDefs for coOrdinates */
    public static final int LAT = 0;
    public static final int LNG = 1;

    /* typeDefs for quadrant */
    public static final int E = 0;
    public static final int S = 1;
    public static final int W = 2;
    public static final int N = 3;

    /**
     * Constructor
     * 
     * @param deg
     * @param minsDecMins
     * @param coOrdinate
     * @param quad
     * @throws InvalidPositionException
     * @since 1.0
     */
    public Coordinate(int deg, float minsDecMins, int coOrdinate,
            int quad) throws InvalidPositionException {
        buildCoOrdinate(deg, minsDecMins, coOrdinate, quad);
        if (verify()) {
        } else {
            throw new InvalidPositionException();
        }
    }

    /**
     * Constructor
     * @param decCoordinate
     * @param coOrdinate
     * @throws InvalidPositionException
     * @since 1.0
     */
    public Coordinate(double decCoordinate, int coOrdinate) throws InvalidPositionException {
        DecimalFormat form = new DecimalFormat("#.#######");

        this.decCoordinate = decCoordinate;
        this.coOrdinate = coOrdinate;
        if (verify()) {
            deg = new Float(decCoordinate).intValue();
            if (deg < 0) {
                minsDecMins = Double.parseDouble(form.format((Math.abs(decCoordinate) - Math.abs(deg)) * 60));
            } else {
                minsDecMins = Double.parseDouble(form.format((decCoordinate - deg) * 60));
            }
        } else {
            throw new InvalidPositionException();
        }
    }

    /**
     * This constructor takes a coordinate in the ALRS formats i.e
     * 38∞31.64'N for lat, and 28∞19.12'W for long
     * Note: ALRS positions are occasionally written with the decimal minutes
     * apostrophe in the 'wrong' place and with an non CP1252 compliant decimal character.
     * This issue has to be corrected in the source database
     * @param coOrdinate
     * @throws InvalidPositionException
     * @since 1.0
     */
    public Coordinate(String coOrdinate) throws InvalidPositionException {
        //firstly split it into its component parts and dispose of the unneeded characters
        String[] items = coOrdinate.split("°");
        int deg = Integer.valueOf(items[0]);

        items = items[1].split("'");
        float minsDecMins = Float.valueOf(items[0]);
        char quad = items[1].charAt(0);

        switch (quad) {
            case 'N':
                buildCoOrdinate(deg, minsDecMins, Coordinate.LAT, Coordinate.N);
                break;
            case 'S':
                buildCoOrdinate(deg, minsDecMins, Coordinate.LAT, Coordinate.S);
                break;
            case 'E':
                buildCoOrdinate(deg, minsDecMins, Coordinate.LNG, Coordinate.E);
                break;
            case 'W':
                buildCoOrdinate(deg, minsDecMins, Coordinate.LNG, Coordinate.W);
        }
        if (verify()) {
        } else {
            throw new InvalidPositionException();
        }
    }

    /**
     * Prints out a coordinate as a string
     * @return the coordinate in decimal format
     * @since 1.0
     */
    public String toStringDegMin() {
        String str = "";
        String quad = "";
        StringUtil su = new StringUtil();
        switch (coOrdinate) {
            case LAT:
                if (decCoordinate >= 0) {
                    quad = "N";
                } else {
                    quad = "S";
                }
                str = su.padNumZero(Math.abs(deg), 2);
                str += "\u00b0" + su.padNumZero(Math.abs(minsDecMins), 2, MINPRECISION) + "'" + quad;
                break;
            case LNG:
                if (decCoordinate >= 0) {
                    quad = "E";
                } else {
                    quad = "W";
                }
                str = su.padNumZero(Math.abs(deg), 3);
                str += "\u00b0" + su.padNumZero(Math.abs(minsDecMins), 2, MINPRECISION) + "'" + quad;
                break;
        }
        return str;
    }

    /**
     * Prints out a coordinate as a string
     * @return the coordinate in decimal format
     * @since 1.0
     */
    public String toStringDec() {
        StringUtil u = new StringUtil();
        switch (coOrdinate) {
            case LAT:
                return u.padNumZero(decCoordinate, 2, DEGPRECISION);
            case LNG:
                return u.padNumZero(decCoordinate, 3, DEGPRECISION);
        }
        return "error";
    }

    /**
     * Returns the coordinate's decimal value
     * @return float the decimal value of the coordinate
     * @since 1.0
     */
    public double decVal() {
        return decCoordinate;
    }

    /**
     * Determines whether a decimal position is valid
     * @return result of validity test
     * @since 1.0
     */
    private boolean verify() {
        switch (coOrdinate) {
            case LAT:
                if (Math.abs(decCoordinate) > 90.0) {
                    return false;
                }
                break;

            case LNG:
                if (Math.abs(decCoordinate) > 180) {
                    return false;
                }
        }
        return true;
    }

    /**
     * Populate this object by parsing the arguments to the function
     * Placed here to allow multiple constructors to use it
     * @since 1.0
     */
    private void buildCoOrdinate(int deg, float minsDecMins, int coOrdinate,
            int quad) {
        NumUtil nu = new NumUtil();

        switch (coOrdinate) {
            case LAT:
                switch (quad) {
                    case N:
                        this.deg = deg;
                        this.minsDecMins = minsDecMins;
                        this.coOrdinate = coOrdinate;
                        decCoordinate = nu.Round(this.deg + (float) this.minsDecMins / 60, Coordinate.MINPRECISION);
                        break;

                    case S:
                        this.deg = -deg;
                        this.minsDecMins = minsDecMins;
                        this.coOrdinate = coOrdinate;
                        decCoordinate = nu.Round(this.deg - ((float) this.minsDecMins / 60), Coordinate.MINPRECISION);
                }

            case LNG:
                switch (quad) {
                    case E:
                        this.deg = deg;
                        this.minsDecMins = minsDecMins;
                        this.coOrdinate = coOrdinate;
                        decCoordinate = nu.Round(this.deg + ((float) this.minsDecMins / 60), Coordinate.MINPRECISION);
                        break;

                    case W:
                        this.deg = -deg;
                        this.minsDecMins = minsDecMins;
                        this.coOrdinate = coOrdinate;
                        decCoordinate = nu.Round(this.deg - ((float) this.minsDecMins / 60), Coordinate.MINPRECISION);
                }
        }
    }
}
