/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jme3.netbeans.plaf.darkmonkey;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.awt.image.BandedSampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferFloat;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

/**
 * I figured it would be best to have a Utilities type class to store the
 * Methods I commonly use. Resources, registering stuff, resolving, transforming
 * and so on...
 *
 * @author charles
 */
public class DMUtils {

    /**
     * <p>
     * This method loads a picture from a relative path string. The relative
     * path's root directory is understood to be inside of a jar... and in
     * relation to the package of the referring Object instance.
     * </p>
     * <p>
     * For example: if the object is an instance of
     * org.jme3.netbeans.plaf.darkmonkey.DarkMonkeyIconFactory.class, and the
     * string is "icons/MyCloseIcon.png", it will attempt to load
     * org/jme3/netbeans/plaf/darkmonkey/icons/MyCloseIcon.png from
     * DarkMonkeyIconFactory's jar file.
     * </p>
     * It will print a stack trace if you get the relative path wrong.
     *
     * @param refObj - Reference Object(Object) - meant for a standard 'this'
     * call, though any Instantiated class can be used. This is part of a
     * workaround for Netbean's multiple class loader system.
     * @param fileName - File Name(String) - the path to an image relative to
     * the Reference Object's location in a jar file.
     * @return BufferedImage - Freshly converted from the image file found at
     * the location.
     */
    public static BufferedImage loadImagefromJar(Object refObj, String fileName) {
        BufferedImage bi = null;
        try {
            bi = ImageIO.read(refObj.getClass().getResourceAsStream(fileName));
        } catch (IOException e) {
            // File is probably referenced wrong or "mispleled"... lol.
            e.printStackTrace();
        }
        return bi;
    }

    /**<p>
     * This utility method is designed to Load OpenType/TrueType fonts into the 
     * current Runtime Environment without installing them to the OS.  It takes 
     * the base path of the refObj and loads font files located relative to it.
     * It checks to make sure that the fonts are not already installed in the system
     * OS, first.  If they are already installed, it does nothing more.
     * </p><p>
     * Typical Usage - DMUtils.loadFontsFromJar(this, someFontFiles);<br/>
     * and then someFontFiles[0] would contain something like "myfonts/DisFontPlain.ttf"
     * </p>
     * @param refObj - Object - Usually just a *this*, but useful for a multiClassLoader
     * type situation.
     * @param fileNames - String[] - an array of {relative path + filename} strings for loading
     *  TrueType or OpenType fonts
     */
    public static void loadFontsFromJar(Object refObj, String[] fileNames) {
        //first, we grab ahold of what all fonts are in the JRE's system
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Font[] fontsListing = ge.getAllFonts();

        /* // this can be uncommented if you want to see all the fonts in the JRE
         for (Font fontInListing : fontsListing) {
         System.out.println(fontInListing.getFontName() + " : " + fontInListing.getFamily());
         }
         */

        // Then we go and process the incoming streams
        InputStream inStream;
        Font checkFont;
        try {
            toNextFileName:
            for (String fileName : fileNames) {// load up the fileName to process...
                checkFont = Font.createFont(Font.TRUETYPE_FONT, refObj.getClass().getResourceAsStream(fileName));
                for (Font fontInListing : fontsListing) {// check if it's already on the list
                    if (fontInListing.getFontName().equals(checkFont.getFontName())) {
                        continue toNextFileName; //head to the next file if we find it...
                    }
                }
                ge.registerFont(checkFont);// and register it if we don't....
            }
        } catch (FontFormatException | IOException e) {
            // a File is probably referenced wrong or "mispleled"... lol.
            // you can alternativly send a single String for debugging purposes
            e.printStackTrace();
        }
        
    }

    /**
     * This method transforms the inputed BufferedImage by the supplied Color[].
     * The behavior treats the Color[] as Ordered Passes A, R, G, B for .length
     * 4 or more. It treats it as Ordered Passes R, G, B only for .length 3 or
     * less.
     *
     * @param colorSet Color[] - that processes [1..4] up to four palette
     * colors. 3 or less uses R,G,B passes only. 4 uses A,R,G,B and ignores
     * anything more.
     * @param clearToColorRequested - Color - A color to Blend with the First
     * Translucent Pass - Optional
     * @param argbMappedBufferedImage - BufferedImage - The image containing
     * Channels as Alpha for the Palette
     * @return BufferedImage - a new BufferedImage() transformed by the palette.
     */
    public static BufferedImage paletteSwapARGB8(Color[] colorSet, Color clearToColorRequested, BufferedImage argbMappedBufferedImage) {
        if (argbMappedBufferedImage == null) {
            return null; //S.E.P.
        }
        final Color BLACK_NO_ALPHA = new Color(0x00000000);
        final Color WHITE_NO_ALPHA = new Color(0x00FFFFFF);
        final int ALPHA = 3; // this is some static mapping for...
        final int RED = 0; // readability in the following...
        final int GREEN = 1; // Magic code section of band processing.
        final int BLUE = 2;
        final int[] orderedBands = {ALPHA, RED, GREEN, BLUE};
        //first we prep a cmap with blank passes and 
        Color[] cMap = {BLACK_NO_ALPHA, BLACK_NO_ALPHA, BLACK_NO_ALPHA, BLACK_NO_ALPHA};
        boolean clearColorFound = false;
        Color clearToColor = BLACK_NO_ALPHA;
        if (colorSet != null) {  //if we get a null colorSet... it's all mapped to clear.
            if (colorSet.length > cMap.length) { // if colorSet is more than 4, we only proces  up to 4
                for (int i = 0; i < cMap.length; i++) {
                    if (colorSet[i] != null) {
                        if (!clearColorFound) {
                            clearColorFound = true;
                            clearToColor = colorSet[i];
                        }
                        cMap[orderedBands[i]] = colorSet[i]; // and finally, if any of the Colors are null... invisible pass...
                    }
                }
            } else {
                int startOffset = 0;
                if (colorSet.length < 4) // if less than standard size, assume RGB model
                {
                    startOffset++; // and "blank" the alpha color pass.
                }
                for (int i = 0; i < colorSet.length; i++) {
                    if (colorSet[i] != null) {
                        if (!clearColorFound) {
                            clearColorFound = true;
                            clearToColor = colorSet[i];
                        }
                        cMap[orderedBands[i + startOffset]] = colorSet[i];
                    }
                }
            }
        }

        // finally adjust the clearToColor if one was requested
        if (clearToColorRequested != null) {
            clearToColor = clearToColorRequested;
        }
        //Next we'll switch to Rasters to easily handle floating point precision
        // operations upon the individual channels.

        WritableRaster outRaster, inRaster;
        int w = argbMappedBufferedImage.getWidth();
        int h = argbMappedBufferedImage.getHeight();
        BandedSampleModel inSM = new BandedSampleModel(DataBuffer.TYPE_FLOAT, w, h, 4);
        DataBufferFloat inDBF = new DataBufferFloat((w * h), 4);//4 banks, and total size 
        inRaster = Raster.createWritableRaster(inSM, inDBF, null); // that null just means point 0, 0 (top/left)
        outRaster = inRaster.createCompatibleWritableRaster(w, h);
        float[] cMaptoFlArray, outColortoFlArray, clearColortoFlArray;
        float inBandAsAlpha;
        Color paletteColor;
        // now we convert from W/E the argbMappedBufferedImage's format to 
        // our normalized [0f..1f] RGBA raster
        outColortoFlArray = new float[]{0f, 0f, 0f, 0f}; // or new float[4]... w/e
        clearColortoFlArray = clearToColor.getRGBComponents(new float[4]);
        clearColortoFlArray[ALPHA] = 0f;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int packedPixel = argbMappedBufferedImage.getRGB(x, y);
                int testing;
                float ftesting;
                //outColortoFlArray[ALPHA] = (((packedPixel >> 24) & 0xFF) / 255);
                testing = packedPixel;
                testing = testing >> 24;
                testing = testing & 0xFF;
                ftesting = testing;
                ftesting = ftesting / 255;
                outColortoFlArray[ALPHA] = ftesting;

                //outColortoFlArray[RED]   = (((packedPixel >> 16) & 0xFF) / 255);
                testing = packedPixel;
                testing = testing >> 16;
                testing = testing & 0xFF;
                ftesting = testing;
                ftesting = ftesting / 255;
                outColortoFlArray[RED] = ftesting;

                //outColortoFlArray[GREEN] = (((packedPixel >>  8) & 0xFF) / 255);
                testing = packedPixel;
                testing = testing >> 8;
                testing = testing & 0xFF;
                ftesting = testing;
                ftesting = ftesting / 255;
                outColortoFlArray[GREEN] = ftesting;

                //outColortoFlArray[BLUE]  = ( (packedPixel & 0xFF)        / 255);
                testing = packedPixel;
                testing = testing & 0xFF;
                ftesting = testing;
                ftesting = ftesting / 255;
                outColortoFlArray[BLUE] = ftesting;

                inRaster.setPixel(x, y, outColortoFlArray);
                outRaster.setPixel(x, y, clearColortoFlArray);
            }
        }
        // next, we process all bands in order - a "band" being one channel of A,R,G,B.
        // as each band is processed the outRaster keeps getting "resampled" to apply
        // the next band properly. all values are considered normalized [0f..1f]
        for (int band : orderedBands) {
            paletteColor = cMap[band];
            cMaptoFlArray = paletteColor.getRGBComponents(new float[4]);// this nullifies translucency
            if (paletteColor != BLACK_NO_ALPHA) {
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        //inBandAsAlpha = inRaster.getSample(x, y, band);
                        inBandAsAlpha = inRaster.getSampleFloat(x, y, band);
                        outColortoFlArray = outRaster.getPixel(x, y, new float[4]);
                        outColortoFlArray[RED] = (outColortoFlArray[RED]
                                * (1f - (inBandAsAlpha * cMaptoFlArray[ALPHA])))
                                + (cMaptoFlArray[RED] * (inBandAsAlpha * cMaptoFlArray[ALPHA]));
                        outColortoFlArray[GREEN] = (outColortoFlArray[GREEN]
                                * (1f - (inBandAsAlpha * cMaptoFlArray[ALPHA])))
                                + (cMaptoFlArray[GREEN] * (inBandAsAlpha * cMaptoFlArray[ALPHA]));
                        outColortoFlArray[BLUE] = (outColortoFlArray[BLUE]
                                * (1f - (inBandAsAlpha * cMaptoFlArray[ALPHA])))
                                + (cMaptoFlArray[BLUE] * (inBandAsAlpha * cMaptoFlArray[ALPHA]));

                        outColortoFlArray[ALPHA] = (outColortoFlArray[ALPHA]
                                * (1f - (inBandAsAlpha * cMaptoFlArray[ALPHA])))
                                + (cMaptoFlArray[ALPHA] * (inBandAsAlpha * cMaptoFlArray[ALPHA]));

                        outRaster.setPixel(x, y, outColortoFlArray);
                    }
                }
            }
        }

        //then we convert n' ship
        BufferedImage returnBI = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                outColortoFlArray = outRaster.getPixel(x, y, new float[4]);
                int packedColor = ((int) (outColortoFlArray[ALPHA] * 255f) << 24)
                        | ((int) (outColortoFlArray[RED] * 255f) << 16)
                        | ((int) (outColortoFlArray[GREEN] * 255f) << 8)
                        | ((int) (outColortoFlArray[BLUE] * 255f));
                returnBI.setRGB(x, y, packedColor);
            }
        }

        return returnBI;
    }

    public static BufferedImage paletteSwapARGB8(Color[] colorSet, BufferedImage argbMappedBufferedImage) {

        return paletteSwapARGB8(colorSet, null, argbMappedBufferedImage);
    }

}
