package com.jme3.util;


/**
 * The wrapper for the primitive type {@code int}.
 * <p>
 * As with the specification, this implementation relies on code laid out in <a
 * href="http://www.hackersdelight.org/">Henry S. Warren, Jr.'s Hacker's
 * Delight, (Addison Wesley, 2002)</a> as well as <a
 * href="http://aggregate.org/MAGIC/">The Aggregate's Magic Algorithms</a>.
 *
 * @see java.lang.Number
 * @since 1.1
 */
public final class FastInteger {

    /**
     * Constant for the maximum {@code int} value, 2<sup>31</sup>-1.
     */
    public static final int MAX_VALUE = 0x7FFFFFFF;

    /**
     * Constant for the minimum {@code int} value, -2<sup>31</sup>.
     */
    public static final int MIN_VALUE = 0x80000000;

    /**
     * Constant for the number of bits needed to represent an {@code int} in
     * two's complement form.
     *
     * @since 1.5
     */
    public static final int SIZE = 32;
    
    /*
     * Progressively smaller decimal order of magnitude that can be represented
     * by an instance of Integer. Used to help compute the String
     * representation.
     */
    private static final int[] decimalScale = new int[] { 1000000000, 100000000,
            10000000, 1000000, 100000, 10000, 1000, 100, 10, 1 };
    
    /**
     * Converts the specified integer into its decimal string representation.
     * The returned string is a concatenation of a minus sign if the number is
     * negative and characters from '0' to '9'.
     * 
     * @param value
     *            the integer to convert.
     * @return the decimal string representation of {@code value}.
     */
    public static boolean toCharArray(int value, char[] output) {
        if (value == 0) 
        {
            output[0] = '0';
            output[1] = 0;
            return true;
        }

        // Faster algorithm for smaller Integers
        if (value < 1000 && value > -1000) {

            int positive_value = value < 0 ? -value : value;
            int first_digit = 0;
            if (value < 0) {
                output[0] = '-';
                first_digit++;
            }
            int last_digit = first_digit;
            int quot = positive_value;
            do {
                int res = quot / 10;
                int digit_value = quot - ((res << 3) + (res << 1));
                digit_value += '0';
                output[last_digit++] = (char) digit_value;
                quot = res;
            } while (quot != 0);

            int count = last_digit--;
            do {
                char tmp = output[last_digit];
                output[last_digit--] = output[first_digit];
                output[first_digit++] = tmp;
            } while (first_digit < last_digit);
            output[count] = 0;
            return true;
        }
        if (value == MIN_VALUE) {
            System.arraycopy("-2147483648".toCharArray(), 0, output, 0, 12);
            output[12] = 0;
            return true;
        }


        int positive_value = value < 0 ? -value : value;
        byte first_digit = 0;
        if (value < 0) {
            output[0] = '-';
            first_digit++;
        }
        byte last_digit = first_digit;
        byte count;
        int number;
        boolean start = false;
        for (int i = 0; i < 9; i++) {
            count = 0;
            if (positive_value < (number = decimalScale[i])) {
                if (start) {
                    output[last_digit++] = '0';
                }
                continue;
            }

            if (i > 0) {
                number = (decimalScale[i] << 3);
                if (positive_value >= number) {
                    positive_value -= number;
                    count += 8;
                }
                number = (decimalScale[i] << 2);
                if (positive_value >= number) {
                    positive_value -= number;
                    count += 4;
                }
            }
            number = (decimalScale[i] << 1);
            if (positive_value >= number) {
                positive_value -= number;
                count += 2;
            }
            if (positive_value >= decimalScale[i]) {
                positive_value -= decimalScale[i];
                count++;
            }
            if (count > 0 && !start) {
                start = true;
            }
            if (start) {
                output[last_digit++] = (char) (count + '0');
            }
        }

        output[last_digit++] = (char) (positive_value + '0');
        output[last_digit] = 0;
        count = last_digit--;
        return true;
    }


    /**
     * Determines the highest (leftmost) bit of the specified integer that is 1
     * and returns the bit mask value for that bit. This is also referred to as
     * the Most Significant 1 Bit. Returns zero if the specified integer is
     * zero.
     * 
     * @param i
     *            the integer to examine.
     * @return the bit mask indicating the highest 1 bit in {@code i}.
     * @since 1.5
     */
    public static int highestOneBit(int i) {
        i |= (i >> 1);
        i |= (i >> 2);
        i |= (i >> 4);
        i |= (i >> 8);
        i |= (i >> 16);
        return (i & ~(i >>> 1));
    }

    /**
     * Determines the lowest (rightmost) bit of the specified integer that is 1
     * and returns the bit mask value for that bit. This is also referred
     * to as the Least Significant 1 Bit. Returns zero if the specified integer
     * is zero.
     * 
     * @param i
     *            the integer to examine.
     * @return the bit mask indicating the lowest 1 bit in {@code i}.
     * @since 1.5
     */
    public static int lowestOneBit(int i) {
        return (i & (-i));
    }

    /**
     * Determines the number of leading zeros in the specified integer prior to
     * the {@link #highestOneBit(int) highest one bit}.
     *
     * @param i
     *            the integer to examine.
     * @return the number of leading zeros in {@code i}.
     * @since 1.5
     */
    public static int numberOfLeadingZeros(int i) {
        i |= i >> 1;
        i |= i >> 2;
        i |= i >> 4;
        i |= i >> 8;
        i |= i >> 16;
        return bitCount(~i);
    }

    /**
     * Determines the number of trailing zeros in the specified integer after
     * the {@link #lowestOneBit(int) lowest one bit}.
     *
     * @param i
     *            the integer to examine.
     * @return the number of trailing zeros in {@code i}.
     * @since 1.5
     */
    public static int numberOfTrailingZeros(int i) {
        return bitCount((i & -i) - 1);
    }

    /**
     * Counts the number of 1 bits in the specified integer; this is also
     * referred to as population count.
     *
     * @param i
     *            the integer to examine.
     * @return the number of 1 bits in {@code i}.
     * @since 1.5
     */
    public static int bitCount(int i) {
        i -= ((i >> 1) & 0x55555555);
        i = (i & 0x33333333) + ((i >> 2) & 0x33333333);
        i = (((i >> 4) + i) & 0x0F0F0F0F);
        i += (i >> 8);
        i += (i >> 16);
        return (i & 0x0000003F);
    }

    /**
     * Rotates the bits of the specified integer to the left by the specified
     * number of bits.
     *
     * @param i
     *            the integer value to rotate left.
     * @param distance
     *            the number of bits to rotate.
     * @return the rotated value.
     * @since 1.5
     */
    public static int rotateLeft(int i, int distance) {
        if (distance == 0) {
            return i;
        }
        /*
         * According to JLS3, 15.19, the right operand of a shift is always
         * implicitly masked with 0x1F, which the negation of 'distance' is
         * taking advantage of.
         */
        return ((i << distance) | (i >>> (-distance)));
    }

    /**
     * Rotates the bits of the specified integer to the right by the specified
     * number of bits.
     *
     * @param i
     *            the integer value to rotate right.
     * @param distance
     *            the number of bits to rotate.
     * @return the rotated value.
     * @since 1.5
     */
    public static int rotateRight(int i, int distance) {
        if (distance == 0) {
            return i;
        }
        /*
         * According to JLS3, 15.19, the right operand of a shift is always
         * implicitly masked with 0x1F, which the negation of 'distance' is
         * taking advantage of.
         */
        return ((i >>> distance) | (i << (-distance)));
    }

    /**
     * Reverses the order of the bytes of the specified integer.
     * 
     * @param i
     *            the integer value for which to reverse the byte order.
     * @return the reversed value.
     * @since 1.5
     */
    public static int reverseBytes(int i) {
        int b3 = i >>> 24;
        int b2 = (i >>> 8) & 0xFF00;
        int b1 = (i & 0xFF00) << 8;
        int b0 = i << 24;
        return (b0 | b1 | b2 | b3);
    }

    /**
     * Reverses the order of the bits of the specified integer.
     * 
     * @param i
     *            the integer value for which to reverse the bit order.
     * @return the reversed value.
     * @since 1.5
     */
    public static int reverse(int i) {
        // From Hacker's Delight, 7-1, Figure 7-1
        i = (i & 0x55555555) << 1 | (i >> 1) & 0x55555555;
        i = (i & 0x33333333) << 2 | (i >> 2) & 0x33333333;
        i = (i & 0x0F0F0F0F) << 4 | (i >> 4) & 0x0F0F0F0F;
        return reverseBytes(i);
    }

    /**
     * Returns the value of the {@code signum} function for the specified
     * integer.
     * 
     * @param i
     *            the integer value to check.
     * @return -1 if {@code i} is negative, 1 if {@code i} is positive, 0 if
     *         {@code i} is zero.
     * @since 1.5
     */
    public static int signum(int i) {
        return (i == 0 ? 0 : (i < 0 ? -1 : 1));
    }

    /**
     * Returns a {@code Integer} instance for the specified integer value.
     * <p>
     * If it is not necessary to get a new {@code Integer} instance, it is
     * recommended to use this method instead of the constructor, since it
     * maintains a cache of instances which may result in better performance.
     *
     * @param i
     *            the integer value to store in the instance.
     * @return a {@code Integer} instance containing {@code i}.
     * @since 1.5
     */
    public static Integer valueOf(int i) {
        if (i < -128 || i > 127) {
            return new Integer(i);
        }
        return valueOfCache.CACHE [i+128];

    }

   static class valueOfCache {
        /**
         * <p>
         * A cache of instances used by {@link Integer#valueOf(int)} and auto-boxing.
         */
        static final Integer[] CACHE = new Integer[256];

        static {
            for(int i=-128; i<=127; i++) {
                CACHE[i+128] = new Integer(i);
            }
        }
    }
}
