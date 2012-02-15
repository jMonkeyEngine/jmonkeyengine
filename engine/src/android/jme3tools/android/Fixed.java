package jme3tools.android;

import java.util.Random;

/**
 *	Fixed point maths class. This can be tailored for specific needs by
 *	changing the bits allocated to the 'fraction' part (see <code>FIXED_POINT
 *	</code>, which would also require <code>SIN_PRECALC</code> and <code>
 *	COS_PRECALC</code> updating).
 *
 *  <p><a href="http://blog.numfum.com/2007/09/java-fixed-point-maths.html">
 *  http://blog.numfum.com/2007/09/java-fixed-point-maths.html</a></p>
 *
 *	@version 1.0
 *	@author CW
 * 
 * @deprecated Most devices with OpenGL ES 2.0 have an FPU. Please use
 * floats instead of this class for decimal math.
 */
@Deprecated
public final class Fixed {

    /**
     *	Number of bits used for 'fraction'.
     */
    public static final int FIXED_POINT = 16;
    /**
     *	Decimal one as represented by the Fixed class.
     */
    public static final int ONE = 1 << FIXED_POINT;
    /**
     *	Half in fixed point.
     */
    public static final int HALF = ONE >> 1;
    /**
     *	Quarter circle resolution for trig functions (should be a power of
     *	two). This is the number of discrete steps in 90 degrees.
     */
    public static final int QUARTER_CIRCLE = 64;
    /**
     *	Mask used to limit angles to one revolution. If a quarter circle is 64
     * (i.e. 90 degrees is broken into 64 steps) then the mask is 255.
     */
    public static final int FULL_CIRCLE_MASK = QUARTER_CIRCLE * 4 - 1;
    /**
     *	The trig table is generated at a higher precision than the typical
     *	16.16 format used for the rest of the fixed point maths. The table
     *	values are then shifted to match the actual fixed point used.
     */
    private static final int TABLE_SHIFT = 30;
    /**
     *	Equivalent to: sin((2 * PI) / (QUARTER_CIRCLE * 4))
     *	<p>
     *	Note: if either QUARTER_CIRCLE or TABLE_SHIFT is changed this value
     *	will need recalculating (put the above formular into a calculator set
     *	radians, then shift the result by <code>TABLE_SHIFT</code>).
     */
    private static final int SIN_PRECALC = 26350943;
    /**
     *	Equivalent to: cos((2 * PI) / (QUARTER_CIRCLE * 4)) * 2
     *
     *	Note: if either QUARTER_CIRCLE or TABLE_SHIFT is changed this value
     *	will need recalculating ((put the above formular into a calculator set
     *	radians, then shift the result by <code>TABLE_SHIFT</code>).
     */
    private static final int COS_PRECALC = 2146836866;
    /**
     *	One quarter sine wave as fixed point values.
     */
    private static final int[] SINE_TABLE = new int[QUARTER_CIRCLE + 1];
    /**
     *	Scale value for indexing ATAN_TABLE[].
     */
    private static final int ATAN_SHIFT;
    /**
     *	Reverse atan lookup table.
     */
    private static final byte[] ATAN_TABLE;
    /**
     *	ATAN_TABLE.length
     */
    private static final int ATAN_TABLE_LEN;

    /*
     *	Generates the tables and fills in any remaining static ints.
     */
    static {
        // Generate the sine table using recursive synthesis.
        SINE_TABLE[0] = 0;
        SINE_TABLE[1] = SIN_PRECALC;
        for (int n = 2; n < QUARTER_CIRCLE + 1; n++) {
            SINE_TABLE[n] = (int) (((long) SINE_TABLE[n - 1] * COS_PRECALC) >> TABLE_SHIFT) - SINE_TABLE[n - 2];
        }
        // Scale the values to the fixed point format used.
        for (int n = 0; n < QUARTER_CIRCLE + 1; n++) {
            SINE_TABLE[n] = SINE_TABLE[n] + (1 << (TABLE_SHIFT - FIXED_POINT - 1)) >> TABLE_SHIFT - FIXED_POINT;
        }

        // Calculate a shift used to scale atan lookups
        int rotl = 0;
        int tan0 = tan(0);
        int tan1 = tan(1);
        while (rotl < 32) {
            if ((tan1 >>= 1) > (tan0 >>= 1)) {
                rotl++;
            } else {
                break;
            }
        }
        ATAN_SHIFT = rotl;
        // Create the a table of tan values
        int[] lut = new int[QUARTER_CIRCLE];
        for (int n = 0; n < QUARTER_CIRCLE; n++) {
            lut[n] = tan(n) >> rotl;
        }
        ATAN_TABLE_LEN = lut[QUARTER_CIRCLE - 1];
        // Then from the tan values create a reverse lookup
        ATAN_TABLE = new byte[ATAN_TABLE_LEN];
        for (byte n = 0; n < QUARTER_CIRCLE - 1; n++) {
            int min = lut[n];
            int max = lut[n + 1];
            for (int i = min; i < max; i++) {
                ATAN_TABLE[i] = n;
            }
        }
    }
    /**
     *	How many decimal places to use when converting a fixed point value to
     *	a decimal string.
     *
     *	@see #toString
     */
    private static final int STRING_DECIMAL_PLACES = 2;
    /**
     *	Value to add in order to round down a fixed point number when
     *	converting to a string.
     */
    private static final int STRING_DECIMAL_PLACES_ROUND;

    static {
        int i = 10;
        for (int n = 1; n < STRING_DECIMAL_PLACES; n++) {
            i *= i;
        }
        if (STRING_DECIMAL_PLACES == 0) {
            STRING_DECIMAL_PLACES_ROUND = ONE / 2;
        } else {
            STRING_DECIMAL_PLACES_ROUND = ONE / (2 * i);
        }
    }
    /**
     *	Random number generator. The standard <code>java.utll.Random</code> is
     *	used since it is available to both J2ME and J2SE. If a guaranteed
     *	sequence is required this would not be adequate.
     */
    private static Random rng = null;

    /**
     *	Fixed can't be instantiated.
     */
    private Fixed() {
    }

    /**
     * Returns an integer as a fixed point value.
     */
    public static int intToFixed(int n) {
        return n << FIXED_POINT;
    }

    /**
     * Returns a fixed point value as a float.
     */
    public static float fixedToFloat(int i) {
        float fp = i;
        fp = fp / ((float) ONE);
        return fp;
    }

    /**
     * Returns a float as a fixed point value.
     */
    public static int floatToFixed(float fp) {
        return (int) (fp * ((float) ONE));
    }

    /**
     *	Converts a fixed point value into a decimal string.
     */
    public static String toString(int n) {
        StringBuffer sb = new StringBuffer(16);
        sb.append((n += STRING_DECIMAL_PLACES_ROUND) >> FIXED_POINT);
        sb.append('.');
        n &= ONE - 1;
        for (int i = 0; i < STRING_DECIMAL_PLACES; i++) {
            n *= 10;
            sb.append((n / ONE) % 10);
        }
        return sb.toString();
    }

    /**
     *	Multiplies two fixed point values and returns the result.
     */
    public static int mul(int a, int b) {
        return (int) ((long) a * (long) b >> FIXED_POINT);
    }

    /**
     *	Divides two fixed point values and returns the result.
     */
    public static int div(int a, int b) {
        return (int) (((long) a << FIXED_POINT * 2) / (long) b >> FIXED_POINT);
    }

    /**
     *	Sine of an angle.
     *
     *	@see #QUARTER_CIRCLE
     */
    public static int sin(int n) {
        n &= FULL_CIRCLE_MASK;
        if (n < QUARTER_CIRCLE * 2) {
            if (n < QUARTER_CIRCLE) {
                return SINE_TABLE[n];
            } else {
                return SINE_TABLE[QUARTER_CIRCLE * 2 - n];
            }
        } else {
            if (n < QUARTER_CIRCLE * 3) {
                return -SINE_TABLE[n - QUARTER_CIRCLE * 2];
            } else {
                return -SINE_TABLE[QUARTER_CIRCLE * 4 - n];
            }
        }
    }

    /**
     *	Cosine of an angle.
     *
     *	@see #QUARTER_CIRCLE
     */
    public static int cos(int n) {
        n &= FULL_CIRCLE_MASK;
        if (n < QUARTER_CIRCLE * 2) {
            if (n < QUARTER_CIRCLE) {
                return SINE_TABLE[QUARTER_CIRCLE - n];
            } else {
                return -SINE_TABLE[n - QUARTER_CIRCLE];
            }
        } else {
            if (n < QUARTER_CIRCLE * 3) {
                return -SINE_TABLE[QUARTER_CIRCLE * 3 - n];
            } else {
                return SINE_TABLE[n - QUARTER_CIRCLE * 3];
            }
        }
    }

    /**
     *	Tangent of an angle.
     *
     *	@see #QUARTER_CIRCLE
     */
    public static int tan(int n) {
        return div(sin(n), cos(n));
    }

    /**
     *	Returns the arc tangent of an angle.
     */
    public static int atan(int n) {
        n = n + (1 << (ATAN_SHIFT - 1)) >> ATAN_SHIFT;
        if (n < 0) {
            if (n <= -ATAN_TABLE_LEN) {
                return -(QUARTER_CIRCLE - 1);
            }
            return -ATAN_TABLE[-n];
        } else {
            if (n >= ATAN_TABLE_LEN) {
                return QUARTER_CIRCLE - 1;
            }
            return ATAN_TABLE[n];
        }
    }

    /**
     *	Returns the polar angle of a rectangular coordinate.
     */
    public static int atan(int x, int y) {
        int n = atan(div(x, abs(y) + 1)); // kludge to prevent ArithmeticException
        if (y > 0) {
            return n;
        }
        if (y < 0) {
            if (x < 0) {
                return -QUARTER_CIRCLE * 2 - n;
            }
            if (x > 0) {
                return QUARTER_CIRCLE * 2 - n;
            }
            return QUARTER_CIRCLE * 2;
        }
        if (x > 0) {
            return QUARTER_CIRCLE;
        }
        return -QUARTER_CIRCLE;
    }

    /**
     *	Rough calculation of the hypotenuse. Whilst not accurate it is very fast.
     *	<p>
     *	Derived from a piece in Graphics Gems.
     */
    public static int hyp(int x1, int y1, int x2, int y2) {
        if ((x2 -= x1) < 0) {
            x2 = -x2;
        }
        if ((y2 -= y1) < 0) {
            y2 = -y2;
        }
        return x2 + y2 - (((x2 > y2) ? y2 : x2) >> 1);
    }

    /**
     *	Fixed point square root.
     *	<p>
     *	Derived from a 1993 Usenet algorithm posted by Christophe Meessen.
     */
    public static int sqrt(int n) {
        if (n <= 0) {
            return 0;
        }
        long sum = 0;
        int bit = 0x40000000;
        while (bit >= 0x100) { // lower values give more accurate results
            long tmp = sum | bit;
            if (n >= tmp) {
                n -= tmp;
                sum = tmp + bit;
            }
            bit >>= 1;
            n <<= 1;
        }
        return (int) (sum >> 16 - (FIXED_POINT / 2));
    }

    /**
     *	Returns the absolute value.
     */
    public static int abs(int n) {
        return (n < 0) ? -n : n;
    }

    /**
     *	Returns the sign of a value, -1 for negative numbers, otherwise 1.
     */
    public static int sgn(int n) {
        return (n < 0) ? -1 : 1;
    }

    /**
     *	Returns the minimum of two values.
     */
    public static int min(int a, int b) {
        return (a < b) ? a : b;
    }

    /**
     *	Returns the maximum of two values.
     */
    public static int max(int a, int b) {
        return (a > b) ? a : b;
    }

    /**
     *	Clamps the value n between min and max.
     */
    public static int clamp(int n, int min, int max) {
        return (n < min) ? min : (n > max) ? max : n;
    }

    /**
     *	Wraps the value n between 0 and the required limit.
     */
    public static int wrap(int n, int limit) {
        return ((n %= limit) < 0) ? limit + n : n;
    }

    /**
     *	Returns the nearest int to a fixed point value. Equivalent to <code>
     *	Math.round()</code> in the standard library.
     */
    public static int round(int n) {
        return n + HALF >> FIXED_POINT;
    }

    /**
     *	Returns the nearest int rounded down from a fixed point value.
     *	Equivalent to <code>Math.floor()</code> in the standard library.
     */
    public static int floor(int n) {
        return n >> FIXED_POINT;
    }

    /**
     *	Returns the nearest int rounded up from a fixed point value.
     *	Equivalent to <code>Math.ceil()</code> in the standard library.
     */
    public static int ceil(int n) {
        return n + (ONE - 1) >> FIXED_POINT;
    }

    /**
     *	Returns a fixed point value greater than or equal to decimal 0.0 and
     *	less than 1.0 (in 16.16 format this would be 0 to 65535 inclusive).
     */
    public static int rand() {
        if (rng == null) {
            rng = new Random();
        }
        return rng.nextInt() >>> (32 - FIXED_POINT);
    }

    /**
     *	Returns a random number between 0 and <code>n</code> (exclusive).
     */
    public static int rand(int n) {
        return (rand() * n) >> FIXED_POINT;
    }
}