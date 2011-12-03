package com.jme3.font;

import com.jme3.math.ColorRGBA;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Contains the color information tagged in a text string
 * Format: \#rgb#
 *         \#rgba#
 *         \#rrggbb#
 *         \#rrggbbaa#
 * @author YongHoon
 */
class ColorTags {
    private static final Pattern colorPattern = Pattern.compile("\\\\#([0-9a-fA-F]{8})#|\\\\#([0-9a-fA-F]{6})#|" +
    		                                                    "\\\\#([0-9a-fA-F]{4})#|\\\\#([0-9a-fA-F]{3})#");
    private LinkedList<Range> colors = new LinkedList<Range>();
    private String text;

    ColorTags() { }

    ColorTags(String seq) {
        setText(seq);
    }
    
    /**
     * @return text without color tags
     */
    String getPlainText() {
        return text;
    }
    
    LinkedList<Range> getTags() {
        return colors;
    }

    void setText(final String charSeq) {
        colors.clear();
        if (charSeq == null) {
            return;
        }
        Matcher m = colorPattern.matcher(charSeq);
        if (m.find()) {
            StringBuilder builder = new StringBuilder(charSeq.length()-7);
            int startIndex = 0;
            do {
                String colorStr = null;
                for (int i = 1; i <= 4 && colorStr==null; i++) {
                    colorStr = m.group(i);
                }
                builder.append(charSeq.subSequence(startIndex, m.start()));
                Range range = new Range(builder.length(), colorStr);
                startIndex = m.end();
                colors.add(range);
            } while (m.find());
            builder.append(charSeq.subSequence(startIndex, charSeq.length()));
            text = builder.toString();
        } else {
            text = charSeq;
        }
    }
    
    class Range {
        int start;
        ColorRGBA color;
        Range(int start, String colorStr) {
            this.start = start;
            this.color = new ColorRGBA();
            if (colorStr.length() >= 6) {
                color.set(Integer.parseInt(colorStr.subSequence(0,2).toString(), 16) / 255f,
                          Integer.parseInt(colorStr.subSequence(2,4).toString(), 16) / 255f,
                          Integer.parseInt(colorStr.subSequence(4,6).toString(), 16) / 255f,
                          1);
                if (colorStr.length() == 8) {
                    color.a = Integer.parseInt(colorStr.subSequence(6,8).toString(), 16) / 255f;
                }
            } else {
                color.set(Integer.parseInt(Character.toString(colorStr.charAt(0)), 16) / 15f,
                          Integer.parseInt(Character.toString(colorStr.charAt(1)), 16) / 15f,
                          Integer.parseInt(Character.toString(colorStr.charAt(2)), 16) / 15f,
                          1);
                if (colorStr.length() == 4) {
                    color.a = Integer.parseInt(Character.toString(colorStr.charAt(3)), 16) / 15f;
                }
            }
            
        }
    }
}
