/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.angelfont;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 *
 * @author normenhansen
 */
public abstract class FontCreator {

    private static final Color OPAQUE_WHITE = new Color(0xFFFFFFFF, true);
    private static final Color TRANSPARENT_BLACK = new Color(0x00000000, true);

    public FontCreator() {
    }

    public static BufferedImage buildFont(String fontName) {
        return buildFont(fontName, 512);
    }

    public static BufferedImage buildFont(String fontName, int bitmapSize) {
        return buildFont(fontName, bitmapSize, 63);
    }

    public static BufferedImage buildFont(String fontName, int bitmapSize, int offset) {
        BufferedImage fontImage;
        Font font;

        boolean sizeFound = false;
        boolean directionSet = false;
        int delta = 0;
        int fontSize = 24;

        String charLocs = "";

        /*
         * To find out how much space a Font takes, you need to use a the
         * FontMetrics class. To get the FontMetrics, you need to get it from a
         * Graphics context. A Graphics context is only available from a
         * displayable surface, ie any class that subclasses Component or any
         * Image. First the font is set on a Graphics object. Then get the
         * FontMetrics and find out the width and height of the widest character
         * (W). Then take the largest of the 2 values and find the maximum size
         * font that will fit in the size allocated.
         */
        while (!sizeFound) {
            font = new Font(fontName, Font.PLAIN, fontSize); // Font Name
            // use BufferedImage.TYPE_4BYTE_ABGR to allow alpha blending
            fontImage = new BufferedImage(bitmapSize, bitmapSize,
                    BufferedImage.TYPE_4BYTE_ABGR);
            Graphics2D g = (Graphics2D) fontImage.getGraphics();
            g.setFont(font);
            FontMetrics fm = g.getFontMetrics();
            int width = fm.stringWidth("W");
            int height = fm.getHeight();
            int lineWidth = (width > height) ? width * 16 : height * 16;
            if (!directionSet) {
                if (lineWidth > bitmapSize) {
                    delta = -2;
                } else {
                    delta = 2;
                }
                directionSet = true;
            }
            if (delta > 0) {
                if (lineWidth < bitmapSize) {
                    fontSize += delta;
                } else {
                    sizeFound = true;
                    fontSize -= delta;
                }
            } else if (delta < 0) {
                if (lineWidth > bitmapSize) {
                    fontSize += delta;
                } else {
                    sizeFound = true;
                    fontSize -= delta;
                }
            }
        }

        /*
         * Now that a font size has been determined, create the final image, set
         * the font and draw the standard/extended ASCII character set for that
         * font.
         */
        font = new Font(fontName, Font.BOLD, fontSize); // Font Name
        // use BufferedImage.TYPE_4BYTE_ABGR to allow alpha blending
        fontImage = new BufferedImage(bitmapSize, bitmapSize,
                BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g = (Graphics2D) fontImage.getGraphics();
        g.setFont(font);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(OPAQUE_WHITE);
        g.setBackground(TRANSPARENT_BLACK);

        FontMetrics fm = g.getFontMetrics();

        for (int i = 0; i < 256; i++) {
            int x = i % 16;
            int y = i / 16;
            char ch[] = {(char) i};
            String temp = new String(ch);
            Rectangle2D bounds = fm.getStringBounds(temp, g);

            int xPos = (int) ((x * 32) + (16 - (bounds.getWidth() / 2)));
            int yPos = (y * 32) + fm.getAscent() - offset;
            g.drawString(temp, xPos, yPos);
            //TODO: AngelFont support!
//            g.setColor(Color.BLUE);
//            g.drawRect(xPos, yPos-(int)bounds.getHeight(), (int)bounds.getWidth(), (int)bounds.getHeight()+fm.getDescent());
//            g.setColor(Color.WHITE);
//
//            charLocs=charLocs+
//                    "char id="+i+
//                    "    x="+xPos +
//                    "    y="+(yPos-(int)bounds.getHeight()-fm.getAscent())+
//                    "    width="+(int)bounds.getWidth()+
//                    "    height="+(int)bounds.getHeight()+
//                    "    xoffset=0" +
//                    "    yoffset=0" +
//                    "    xadvance=0" +
//                    "    page=0" +
//                    "    chnl=15\n";
        }
//        System.out.println(charLocs);
        return fontImage;
    }
}
