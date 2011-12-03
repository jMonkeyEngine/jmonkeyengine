package com.jme3.font;

import com.jme3.math.ColorRGBA;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * LetterQuad contains the position, color, uv texture information for a character in text.
 * @author YongHoon
 */
class LetterQuad {
    private static final Rectangle UNBOUNDED = new Rectangle(0, 0, Float.MAX_VALUE, Float.MAX_VALUE);
    private static final float LINE_DIR = -1;

    private final BitmapFont font;
    private final char c;
    private final int index;
    private int style;

    private BitmapCharacter bitmapChar = null;
    private float x0 = Integer.MIN_VALUE;
    private float y0 = Integer.MIN_VALUE;
    private float width = Integer.MIN_VALUE;
    private float height = Integer.MIN_VALUE;
    private float xAdvance = 0;
    private float u0;
    private float v0;
    private float u1;
    private float v1;
    private float lineY;
    private boolean eol;

    private LetterQuad previous;
    private LetterQuad next;
    private int colorInt = 0xFFFFFFFF;

    private boolean rightToLeft;
    private float alignX;
    private float alignY;
    private float sizeScale = 1;
    
    /**
     * create head / tail
     * @param font
     * @param rightToLeft
     */
    protected LetterQuad(BitmapFont font, boolean rightToLeft) {
        this.font = font;
        this.c = Character.MIN_VALUE;
        this.rightToLeft = rightToLeft;
        this.index = -1;
        setBitmapChar(null);
    }

    /**
     * create letter and append to previous LetterQuad
     * 
     * @param c
     * @param prev previous character
     */
    protected LetterQuad(char c, LetterQuad prev) {
        this.font = prev.font;
        this.rightToLeft = prev.rightToLeft;
        this.c = c;
        this.index = prev.index+1;
        this.eol = isLineFeed();
        setBitmapChar(c);
        prev.insert(this);
    }
    
    LetterQuad addNextCharacter(char c) {
        LetterQuad n = new LetterQuad(c, this);
        return n;
    }

    BitmapCharacter getBitmapChar() {
        return bitmapChar;
    }
    
    char getChar() {
        return c;
    }
    
    int getIndex() {
        return index;
    }

    private Rectangle getBound(StringBlock block) {
        if (block.getTextBox() != null) {
            return block.getTextBox();
        }
        return UNBOUNDED;
    }

    LetterQuad getPrevious() {
        return previous;
    }

    LetterQuad getNext() {
        return next;
    }

    public float getU0() {
        return u0;
    }

    float getU1() {
        return u1;
    }

    float getV0() {
        return v0;
    }

    float getV1() {
        return v1;
    }
    
    boolean isInvalid() {
        return x0 == Integer.MIN_VALUE;
    }

    boolean isInvalid(StringBlock block) {
        return isInvalid(block, 0);
    }
    
    boolean isInvalid(StringBlock block, float gap) {
        if (isHead() || isTail())
            return false;
        if (x0 == Integer.MIN_VALUE || y0 == Integer.MIN_VALUE) {
            return true;
        }
        Rectangle bound = block.getTextBox();
        if (bound == null) {
            return false;
        }
        return x0 > 0 && bound.x+bound.width-gap < getX1();
    }
    
    float getX0() {
        return x0;
    }

    float getX1() {
        return x0+width;
    }
    
    float getNextX() {
        return x0+xAdvance;
    }
    
    float getNextLine() {
        return lineY+LINE_DIR*font.getCharSet().getLineHeight() * sizeScale;
    }

    float getY0() {
        return y0;
    }

    float getY1() {
        return y0-height;
    }
    
    float getWidth() {
        return width;
    }
    
    float getHeight() {
        return height;
    }

    void insert(LetterQuad ins) {
        LetterQuad n = next;
        next = ins;
        ins.next = n;
        ins.previous = this;
        n.previous = ins;
    }
    
    void invalidate() {
        eol = isLineFeed();
        setBitmapChar(font.getCharSet().getCharacter(c, style));
    }

    boolean isTail() {
        return next == null;
    }

    boolean isHead() {
        return previous == null;
    }

    /**
     * @return next letter
     */
    LetterQuad remove() {
        this.previous.next = next;
        this.next.previous = previous;
        return next;
    }

    void setPrevious(LetterQuad before) {
        this.previous = before;
    }
    
    void setStyle(int style) {
        this.style = style;
        invalidate();
    }
    
    void setColor(ColorRGBA color) {
        this.colorInt = color.asIntRGBA();
        invalidate();
    }

    void setBitmapChar(char c) {
        BitmapCharacterSet charSet = font.getCharSet();
        BitmapCharacter bm = charSet.getCharacter(c, style);
        setBitmapChar(bm);
    }
    
    void setBitmapChar(BitmapCharacter bitmapChar) {
        x0 = Integer.MIN_VALUE;
        y0 = Integer.MIN_VALUE;
        width = Integer.MIN_VALUE;
        height = Integer.MIN_VALUE;
        alignX = 0;
        alignY = 0;
        
        BitmapCharacterSet charSet = font.getCharSet();
        this.bitmapChar = bitmapChar;
        if (bitmapChar != null) {
            u0 = (float) bitmapChar.getX() / charSet.getWidth();
            v0 = (float) bitmapChar.getY() / charSet.getHeight();
            u1 = u0 + (float) bitmapChar.getWidth() / charSet.getWidth();
            v1 = v0 + (float) bitmapChar.getHeight() / charSet.getHeight();
        } else {
            u0 = 0;
            v0 = 0;
            u1 = 0;
            v1 = 0;
        }
    }

    void setNext(LetterQuad next) {
        this.next = next;
    }

    void update(StringBlock block) {
        final float[] tabs = block.getTabPosition();
        final float tabWidth = block.getTabWidth();
        final Rectangle bound = getBound(block);
        sizeScale = block.getSize() / font.getCharSet().getRenderedSize();
        lineY = computeLineY(block);

        if (isHead()) {
            x0 = getBound(block).x;
            y0 = lineY;
            width = 0;
            height = 0;
            xAdvance = 0;
        } else if (isTab()) {
            x0 = previous.getNextX();
            width = tabWidth;
            y0 = lineY;
            height = 0;
            if (tabs != null && x0 < tabs[tabs.length-1]) {
                for (int i = 0; i < tabs.length-1; i++) {
                    if (x0 > tabs[i] && x0 < tabs[i+1]) {
                        width = tabs[i+1] - x0;
                    }
                }
            }
            xAdvance = width;
        } else if (bitmapChar == null) {
            x0 = getPrevious().getX1();
            y0 = lineY;
            width = 0;
            height = 0;
            xAdvance = 0;
        } else {
            float xOffset = bitmapChar.getXOffset() * sizeScale;
            float yOffset = bitmapChar.getYOffset() * sizeScale;
            xAdvance = bitmapChar.getXAdvance() * sizeScale;
            width = bitmapChar.getWidth() * sizeScale;
            height = bitmapChar.getHeight() * sizeScale;
            float incrScale = rightToLeft ? -1f : 1f;
            float kernAmount = 0f;

            if (previous.isHead() || previous.eol) {
                x0 = bound.x;
                
                // The first letter quad will be drawn right at the first
                // position... but it does not offset by the characters offset
                // amount.  This means that we've potentially accumulated extra
                // pixels and the next letter won't get drawn far enough unless
                // we add this offset back into xAdvance.. by subtracting it.
                // This is the same thing that's done below because we've
                // technically baked the offset in just like below.  It doesn't
                // look like it at first glance so I'm keeping it separate with
                // this comment.
                xAdvance -= xOffset * incrScale; 
                
            } else {
                x0 = previous.getNextX() + xOffset * incrScale;
                
                // Since x0 will have offset baked into it then we
                // need to counteract that in xAdvance.  This is better
                // than removing it in getNextX() because we also need
                // to take kerning into account below... which will also
                // get baked in.
                // Without this, getNextX() will return values too far to
                // the left, for example.
                xAdvance -= xOffset * incrScale; 
            }
            y0 = lineY + LINE_DIR*yOffset;

            // Adjust for kerning
            BitmapCharacter lastChar = previous.getBitmapChar();
            if (lastChar != null && block.isKerning()) {
                kernAmount = lastChar.getKerning(c) * sizeScale;
                x0 += kernAmount * incrScale;
                
                // Need to unbake the kerning from xAdvance since it
                // is baked into x0... see above.
                //xAdvance -= kernAmount * incrScale;
                // No, kerning is an inter-character spacing and _does_ affect
                // all subsequent cursor positions. 
            }
        }
        if (isEndOfLine()) {
            xAdvance = bound.x-x0;
        }
    }
    
    /**
     * add temporary linewrap indicator
     */
    void setEndOfLine() {
        this.eol = true;
    }
    
    boolean isEndOfLine() {
        return eol;
    }
    
    boolean isLineWrap() {
        return !isHead() && !isTail() && bitmapChar == null && c == Character.MIN_VALUE;
    }
    
    private float computeLineY(StringBlock block) {
        if (isHead()) {
            return getBound(block).y;
        } else if (previous.eol) {
            return previous.getNextLine();
        } else {
            return previous.lineY;
        }
    }

    
    boolean isLineStart() {
        return x0 == 0 || (previous != null && previous.eol);
    }
    
    boolean isBlank() {
        return c == ' ' || isTab();
    }
    
    public void storeToArrays(float[] pos, float[] tc, short[] idx, byte[] colors, int quadIdx){
        float x = x0+alignX;
        float y = y0-alignY;
        float xpw = x+width;
        float ymh = y-height;

        pos[0] = x;   pos[1]  = y;   pos[2]  = 0;
        pos[3] = x;   pos[4]  = ymh; pos[5]  = 0;
        pos[6] = xpw; pos[7]  = ymh; pos[8]  = 0;
        pos[9] = xpw; pos[10] = y;   pos[11] = 0;

        float v0 = 1f - this.v0;
        float v1 = 1f - this.v1;

        tc[0] = u0; tc[1] = v0;
        tc[2] = u0; tc[3] = v1;
        tc[4] = u1; tc[5] = v1;
        tc[6] = u1; tc[7] = v0;

        colors[3] = (byte) (colorInt & 0xff);
        colors[2] = (byte) ((colorInt >> 8) & 0xff);
        colors[1] = (byte) ((colorInt >> 16) & 0xff);
        colors[0] = (byte) ((colorInt >> 24) & 0xff);
        System.arraycopy(colors, 0, colors, 4,  4);
        System.arraycopy(colors, 0, colors, 8,  4);
        System.arraycopy(colors, 0, colors, 12, 4);

        short i0 = (short) (quadIdx * 4);
        short i1 = (short) (i0 + 1);
        short i2 = (short) (i0 + 2);
        short i3 = (short) (i0 + 3);

        idx[0] = i0; idx[1] = i1; idx[2] = i2;
        idx[3] = i0; idx[4] = i2; idx[5] = i3;
    }
    
    public void appendPositions(FloatBuffer fb){
        float sx = x0+alignX;
        float sy = y0-alignY;
        float ex = sx+width;
        float ey = sy-height;
        // NOTE: subtracting the height here
        // because OGL's Ortho origin is at lower-left
        fb.put(sx).put(sy).put(0f);
        fb.put(sx).put(ey).put(0f);
        fb.put(ex).put(ey).put(0f);
        fb.put(ex).put(sy).put(0f);
    }

    public void appendPositions(ShortBuffer sb){
        final float x1 = getX1();
        final float y1 = getY1();
        short x = (short) x0;
        short y = (short) y0;
        short xpw = (short) (x1);
        short ymh = (short) (y1);
        
        sb.put(x).put(y).put((short)0);
        sb.put(x).put(ymh).put((short)0);
        sb.put(xpw).put(ymh).put((short)0);
        sb.put(xpw).put(y).put((short)0);
    }

    public void appendTexCoords(FloatBuffer fb){
        // flip coords to be compatible with OGL
        float v0 = 1 - this.v0;
        float v1 = 1 - this.v1;

        // upper left
        fb.put(u0).put(v0);
        // lower left
        fb.put(u0).put(v1);
        // lower right
        fb.put(u1).put(v1);
        // upper right
        fb.put(u1).put(v0);
    }

    public void appendColors(ByteBuffer bb){
        bb.putInt(colorInt);
        bb.putInt(colorInt);
        bb.putInt(colorInt);
        bb.putInt(colorInt);
    }

    public void appendIndices(ShortBuffer sb, int quadIndex){
        // each quad has 4 indices
        short v0 = (short) (quadIndex * 4);
        short v1 = (short) (v0 + 1);
        short v2 = (short) (v0 + 2);
        short v3 = (short) (v0 + 3);

        sb.put(v0).put(v1).put(v2);
        sb.put(v0).put(v2).put(v3);
//        sb.put(new short[]{ v0, v1, v2,
//                            v0, v2, v3 });
    }


    @Override
    public String toString() {
        return String.valueOf(c);
    }

    void setAlignment(float alignX, float alignY) {
        this.alignX = alignX;
        this.alignY = alignY;
    }

    float getAlignX() {
        return alignX;
    }

    float getAlignY() {
        return alignY;
    }

    boolean isLineFeed() {
        return c == '\n';
    }
    
    boolean isTab() {
        return c == '\t';
    }
    
}
