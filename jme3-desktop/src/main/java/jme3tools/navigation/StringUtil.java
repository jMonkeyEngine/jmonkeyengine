/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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
package jme3tools.navigation;

import java.util.regex.Pattern;

/**
 * A collection of String utilities.
 *
 * @author Benjamin Jakobus
 * @version 1.0
 */
public class StringUtil {

    /**
     * Splits a newline (\n) delimited string into an array of strings
     *
     * @param str the string to split up
     * @param delimiter the delimiter to use in splitting
     * @return an array of String objects equivalent to str
     */
    public String[] splitDelimitedStr(String str, String delimiter) {
        Pattern pttn = Pattern.compile(delimiter);
        return pttn.split(str);
    }

    /**
     * Right aligns a long number with spaces for printing
     *
     * @param num the number to be aligned
     * @param totalLen the total length of the padded string
     * @return the padded number
     */
    public String padNum(long num, int totalLen) {
        String numStr = Long.toString(num);
        int len = totalLen - numStr.length();
        String pads = "";
        for (int i = 0; i < len; i++) {
            pads += " ";
        }
        return pads + numStr;
    }

    /**
     * Right aligns a long number with zeros for printing
     *
     * @param num the number to be aligned
     * @param totalLen the total length of the padded string
     * @return the padded number
     */
    public String padNumZero(long num, int totalLen) {
        String numStr = Long.toString(num);
        int len = totalLen - numStr.length();
        String pads = "";
        for (int i = 0; i < len; i++) {
            pads += "0";
        }
        return pads + numStr;
    }

    /**
     * Right aligns an integer number with spaces for printing
     *
     * @param num the number to be aligned
     * @param totalLen the total length of the padded string
     * @return the padded number
     */
    public String padNum(int num, int totalLen) {
        String numStr = Integer.toString(num);
        int len = totalLen - numStr.length();
        String pads = "";
        for (int i = 0; i < len; i++) {
            pads += " ";
        }
        return pads + numStr;
    }

    /**
     * Right aligns an integer number with zeros for printing
     *
     * @param num the number to be aligned
     * @param totalLen the total length of the padded string
     * @return the padded number
     */
    public String padNumZero(int num, int totalLen) {
        String numStr = Integer.toString(num);
        int len = totalLen - numStr.length();
        String pads = "";
        for (int i = 0; i < len; i++) {
            pads += "0";
        }
        return pads + numStr;
    }

    /**
     * Right aligns a double number with spaces for printing
     *
     * @param num the number to be aligned
     * @param wholeLen the total length of the padded string
     * @return the padded number
     */
    public String padNum(double num, int wholeLen, int decimalPlaces) {
        String numStr = Double.toString(num);
        int dpLoc = numStr.indexOf(".");

        int len = wholeLen - dpLoc;
        String pads = "";
        for (int i = 0; i < len; i++) {
            pads += " ";
        }

        numStr = pads + numStr;

        dpLoc = numStr.indexOf(".");

        if (dpLoc + 1 + decimalPlaces > numStr.substring(dpLoc).length()) {
            return numStr;
        }
        return numStr.substring(0, dpLoc + 1 + decimalPlaces);
    }

    /**
     * Right aligns a double number with zeros for printing
     *
     * @param num the number to be aligned
     * @param wholeLen the total length of the padded string
     * @return the padded number
     */
    public String padNumZero(double num, int wholeLen, int decimalPlaces) {
        String numStr = Double.toString(num);
        int dpLoc = numStr.indexOf(".");

        int len = wholeLen - dpLoc;
        String pads = "";
        for (int i = 0; i < len; i++) {
            pads += "0";
        }

        numStr = pads + numStr;

        dpLoc = numStr.indexOf(".");

        if (dpLoc + 1 + decimalPlaces > numStr.substring(dpLoc).length()) {
            return numStr;
        }
        return numStr.substring(0, dpLoc + 1 + decimalPlaces);
    }

    /**
     * Right aligns a float number with spaces for printing
     *
     * @param num the number to be aligned
     * @param wholeLen the total length of the padded string
     * @return the padded number
     */
    public String padNum(float num, int wholeLen, int decimalPlaces) {
        String numStr = Float.toString(num);
        int dpLoc = numStr.indexOf(".");

        int len = wholeLen - dpLoc;
        String pads = "";
        for (int i = 0; i < len; i++) {
            pads += " ";
        }

        numStr = pads + numStr;

        dpLoc = numStr.indexOf(".");

        if (dpLoc + 1 + decimalPlaces > numStr.substring(dpLoc).length()) {
            return numStr;
        }
        return numStr.substring(0, dpLoc + 1 + decimalPlaces);
    }

    /**
     * Right aligns a float number with zeros for printing
     *
     * @param num the number to be aligned
     * @param wholeLen the total length of the padded string
     * @return the padded number
     */
    public String padNumZero(float num, int wholeLen, int decimalPlaces) {
        String numStr = Float.toString(num);
        int dpLoc = numStr.indexOf(".");

        int len = wholeLen - dpLoc;
        String pads = "";

        if (numStr.charAt(0) == '-') {
            len += 1;
            for (int i = 0; i < len; i++) {
                pads += "0";
            }
            pads = "-" + pads;
            numStr = pads + numStr.substring(1);
        } else {
            for (int i = 0; i < len; i++) {
                pads += "0";
            }
            numStr = pads + numStr;
        }

        dpLoc = numStr.indexOf(".");
        int length = numStr.substring(dpLoc).length();
        while (length < decimalPlaces) {
            numStr += "0";
        }
        return numStr;

    }

    /**
     * Right aligns a {@link String} with zeros for printing
     *
     * @param input the String to be aligned
     * @param wholeLen the total length of the padded string
     * @return the padded number
     */
    public String padStringRight(String input, int wholeLen) {
        for (int i = input.length(); i < wholeLen; i++) {
            input += " ";
        }
        return input;
    }

    /**
     * @param arr a boolean array to be represented as a string
     * @return the array as a string
     */
    public String boolArrToStr(boolean[] arr) {
        String output = "";
        for (int i = 0; i < arr.length; i++) {
            if (arr[i]) {
                output += "1";
            } else {
                output += "0";
            }
        }
        return output;
    }

    /**
     * Formats a double nicely for printing: THIS DOES NOT ROUND!!!!
     * @param num the double to be turned into a pretty string
     * @return the pretty string
     */
    public String prettyNum(double num) {
        String numStr = (new Double(num)).toString();

        while (numStr.length() < 4) {
            numStr += "0";
        }

        numStr = numStr.substring(0, numStr.indexOf(".") + 3);
        return numStr;
    }
}
