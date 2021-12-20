/*
 * Copyright (c) 2018-2021 jMonkeyEngine
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions 
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright 
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright 
 *    notice, this list of conditions and the following disclaimer in 
 *    the documentation and/or other materials provided with the 
 *    distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its 
 *    contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT 
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS 
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE 
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR 
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, 
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED 
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
 
package jme3test.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;

import com.jme3.app.DebugKeysAppState;
import com.jme3.app.StatsAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.ScreenshotAppState; 
import com.jme3.bounding.BoundingBox;
import com.jme3.font.BitmapCharacterSet;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.*;
import com.jme3.scene.debug.WireBox;
import com.jme3.scene.shape.*;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.plugins.AWTLoader;

/**
 *
 * @author pspeed42
 */
public class TestBitmapFontLayout extends SimpleApplication {
 
    public static final String SCROLL_UP = "scroll up";
    public static final String SCROLL_DOWN = "scroll down";
    public static final String SCROLL_LEFT = "scroll left";
    public static final String SCROLL_RIGHT = "scroll right";
    public static final String ZOOM_IN = "zoom in";
    public static final String ZOOM_OUT = "zoom out";
    public static final String RESET_ZOOM = "reset zoom";
    public static final String RESET_VIEW = "reset view";
 
    public static final float ZOOM_SPEED = 0.1f;
    public static final float SCROLL_SPEED = 50;
 
    final private Node testRoot = new Node("test root");
    final private Node scrollRoot = new Node("scroll root");
    final private Vector3f scroll = new Vector3f(0, 0, 0);
    final private Vector3f zoom = new Vector3f(0, 0, 0);
    
    public static void main(String[] args){
        TestBitmapFontLayout app = new TestBitmapFontLayout();
        app.start();
    }

    public TestBitmapFontLayout() {
        super(new StatsAppState(), 
              new DebugKeysAppState(),
              new ScreenshotAppState("", System.currentTimeMillis()));
    }

    public static Font loadTtf( String resource ) {
        try {
            return Font.createFont(Font.TRUETYPE_FONT, 
                                   TestBitmapFontLayout.class.getResourceAsStream(resource));            
        } catch( FontFormatException | IOException e ) {
            throw new RuntimeException("Error loading resource:" + resource, e);
        }
    }
    
    private Texture renderAwtFont( TestConfig test, int width, int height, BitmapFont bitmapFont ) {
 
        BitmapCharacterSet charset = bitmapFont.getCharSet();
          
        // Create an image at least as big as our JME text
        System.out.println("Creating image size:" + width + ", " + height);        
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = (Graphics2D)image.getGraphics();
 
        g2.setColor(Color.lightGray);
        g2.fillRect(0, 0, width, height);
        g2.setColor(Color.cyan);
        g2.drawRect(0, 0, width, height);
                
        g2.setColor(new Color(0, 0, 128));
        //g2.drawLine(0, 0, 50, 50);
        //g2.drawLine(xFont, yFont, xFont + 30, yFont);
        //g2.drawLine(xFont, yFont, xFont, yFont + 30);
 
        //g2.drawString("Testing", 0, 10);
 
        Font font = test.awtFont;
        System.out.println("Java font:" + font);
                
        float size = font.getSize2D();
        FontMetrics fm = g2.getFontMetrics(font);
        System.out.println("Java font metrics:" + fm);
        
        String[] lines = test.text.split("\n");
 
        g2.setFont(font);
        g2.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        int y = fm.getLeading() + fm.getMaxAscent();
        for( String s : lines ) {        
            g2.drawString(s, 0, y);            
            y += fm.getHeight();
        }
        
        g2.dispose();
        
        Image jmeImage = new AWTLoader().load(image, true);
        return new Texture2D(jmeImage);
    }
    
    private Node createVisual( TestConfig test ) {
        Node result = new Node(test.name);
        
        // For reasons I have trouble articulating, I want the visual's 0,0,0 to be
        // the same as the JME rendered text.  All other things will then be positioned relative
        // to that.
        // JME BitmapText (currently) renders from what it thinks the top of the letter is
        // down.  The actual bitmap text bounds may extend upwards... so we need to account
        // for that in any labeling we add above it.
        // Thus we add and set up the main test text first.

        BitmapFont bitmapFont = assetManager.loadFont(test.jmeFont);
        BitmapCharacterSet charset = bitmapFont.getCharSet();
        
        System.out.println("Test name:" + test.name);
        System.out.println("Charset line height:" + charset.getLineHeight());
        System.out.println("    base:" + charset.getBase());
        System.out.println("    rendered size:" + charset.getRenderedSize());
        System.out.println("    width:" + charset.getWidth() + " height:" + charset.getHeight());
        
        BitmapText bitmapText = new BitmapText(bitmapFont);
        bitmapText.setText(test.text);
        bitmapText.setColor(ColorRGBA.Black); 
        result.attachChild(bitmapText);
        
        // And force it to update because BitmapText builds itself lazily.
        result.updateLogicalState(0.1f);        
        BoundingBox bb = (BoundingBox)bitmapText.getWorldBound(); 
               
        BitmapText label = new BitmapText(assetManager.loadFont("Interface/Fonts/Default.fnt"));
        label.setText("Test:" + test.name);
        // Move the label up by its own size plus whatever extra headspace
        // that the test text might have... plus a couple pixels of padding.
        float yOffset = Math.max(0, bb.getCenter().y + bb.getYExtent()); 
        label.move(0, label.getSize() + yOffset + 2, 0);
        label.setColor(new ColorRGBA(0, 0.2f, 0, 1f));
        result.attachChild(label);
        

        // Bitmap text won't update itself automatically... it's lazy.
        // That means it won't be able to tell us its bounding volume, etc... so
        // we'll force it to update.
        result.updateLogicalState(0.1f);        
 
        // Add a bounding box visual
        WireBox box = new WireBox(bb.getXExtent(), bb.getYExtent(), bb.getZExtent());
        Geometry geom = new Geometry(test.name + " bounds", box);
        geom.setMaterial(new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md"));
        geom.getMaterial().setColor("Color", ColorRGBA.Red);
        geom.setLocalTranslation(bb.getCenter());        
        result.attachChild(geom);
 
        // Add a box to show 0,0 + font size
        float size = bitmapText.getLineHeight() * 0.5f; 
        box = new WireBox(size, size, 0);
        geom = new Geometry(test.name + " metric", box);
        geom.setMaterial(new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md"));
        geom.getMaterial().setColor("Color", ColorRGBA.Blue);
        geom.setLocalTranslation(size, -size, 0);
        result.attachChild(geom); 

        float yBaseline = -charset.getBase();
        Line line = new Line(new Vector3f(0, yBaseline, 0), new Vector3f(50, yBaseline, 0));
        geom = new Geometry(test.name + " base", line);
        geom.setMaterial(new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md"));
        geom.getMaterial().setColor("Color", ColorRGBA.Green);
        result.attachChild(geom); 
         
        System.out.println("text bb:" + bb);
        // We want the width and the height to cover the whole potential area
        // for the font.  So it can't just be the rendered bounds but must encompass
        // the whole abstract font space... 0, 0, to center + extents.
        //int width = (int)Math.round(bb.getCenter().x + bb.getXExtent());
        //int height = (int)Math.round(-bb.getCenter().y + bb.getYExtent());
        // No, that's not right either because in case like this:
        // text bb:BoundingBox [Center: (142.0, -15.5, 0.0)  xExtent: 142.0  yExtent: 20.5  zExtent: 0.0]
        // We get:
        // Creating image size:284, 36
        // ...when it should be at least 41 high.
        float x1 = bb.getCenter().x - bb.getXExtent();
        float x2 = bb.getCenter().x + bb.getXExtent();
        float y1 = bb.getCenter().y - bb.getYExtent();
        float y2 = bb.getCenter().y + bb.getYExtent();
        System.out.println("xy1:" + x1 + ", " + y1 + "  xy2:" + x2 + ", " + y2);
        int width = Math.round(x2 - Math.min(0, x1));
        int height = Math.round(y2 - Math.min(0, y1)); 
        
        Texture awtText = renderAwtFont(test, width, height, bitmapFont);
        Quad quad = new Quad(width, height);
        geom = new Geometry(test.name + " awt1", quad);
        geom.setMaterial(new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md"));
        geom.getMaterial().setTexture("ColorMap", awtText);
        // Quads render from the lower left corner up
        geom.move(0, -height, 0);
        
        // That quad is now positioned directly over where the bitmap text is.
        // We'll clone it and move one right and one down
        Geometry right = geom.clone();
        right.move(width, 0, 0);
        result.attachChild(right);
        
        Geometry down = geom.clone();
        down.move(0, bb.getCenter().y - bb.getYExtent() - 1, 0);
        result.attachChild(down);                        
        
        return result;
    }

    @Override
    public void simpleInitApp() {
        setPauseOnLostFocus(false);
        setDisplayStatView(false);
        setDisplayFps(false);
        viewPort.setBackgroundColor(ColorRGBA.LightGray);
 
        setupTestScene();
        setupUserInput();
 
        setupInstructionsNote();
    }
    
    protected void setupInstructionsNote() {
        // Add some instructional text
        String instructions = "WASD/Cursor Keys = scroll\n"
                            + "+/- = zoom\n"
                            + "space = reset view\n"
                            + "0 = reset zoom\n";
        BitmapText note = new BitmapText(guiFont);
        note.setText(instructions);
        note.setColor(new ColorRGBA(0, 0.3f, 0, 1));
        note.updateLogicalState(0.1f);
        
        BoundingBox bb = (BoundingBox)note.getWorldBound();        
        
        note.setLocalTranslation(cam.getWidth() - bb.getXExtent() * 2 - 20, 
                                 cam.getHeight() - 20, 10);                
        
        guiNode.attachChild(note);
        
        BitmapText note2 = note.clone();
        note2.setColor(ColorRGBA.Black);
        note2.move(1, -1, -2);
        guiNode.attachChild(note2);

        BitmapText note3 = note.clone();
        note3.setColor(ColorRGBA.White);
        note3.move(-1, 1, -1);
        guiNode.attachChild(note3);
        
    }

    protected void setupTestScene() {  
        String fox = "The quick brown fox jumps over the lazy dog.";
        String loremIpsum = "Lorem ipsum dolor sit amet, consectetur adipiscing elit"; 
        String foxIpsum = fox + "\n" + loremIpsum;
        
        List<TestConfig> tests = new ArrayList<>();
 

        // Note: for some Java fonts we reduce the point size to more closely
        // match the pixel size... other than the Java-rendered fonts from Hiero, it will never
        // be exact because of different font engines. 
        tests.add(new TestConfig("Hiero Java FreeSerif-16-Italic", 
                                 foxIpsum,
                                 "jme3test/font/FreeSerif16I.fnt",
                                 loadTtf("/jme3test/font/FreeSerif.ttf").deriveFont(Font.ITALIC, 16f)));                

        tests.add(new TestConfig("Hiero FreeType FreeSerif-16-Italic", 
                                 foxIpsum,
                                 "jme3test/font/FT-FreeSerif16I.fnt",
                                 loadTtf("/jme3test/font/FreeSerif.ttf").deriveFont(Font.ITALIC, 14f)));                

        tests.add(new TestConfig("Hiero Native FreeSerif-16-Italic", 
                                 foxIpsum,
                                 "jme3test/font/Native-FreeSerif16I.fnt",
                                 loadTtf("/jme3test/font/FreeSerif.ttf").deriveFont(Font.ITALIC, 15f)));                

        tests.add(new TestConfig("AngelCode FreeSerif-16-Italic", 
                                 foxIpsum,
                                 "jme3test/font/BM-FreeSerif16I.fnt",
                                 loadTtf("/jme3test/font/FreeSerif.ttf").deriveFont(Font.ITALIC, 12f)));
                                 // It's actually between 12 and 13 but Java rounds up.
                                                 
        tests.add(new TestConfig("Hiero Padded FreeSerif-16-Italic", 
                                 foxIpsum,
                                 "jme3test/font/FreeSerif16Ipad5555.fnt",
                                 loadTtf("/jme3test/font/FreeSerif.ttf").deriveFont(Font.ITALIC, 16f)));

        tests.add(new TestConfig("AngelCode Padded FreeSerif-16-Italic", 
                                 foxIpsum,
                                 "jme3test/font/BM-FreeSerif16Ipad5555.fnt",
                                 loadTtf("/jme3test/font/FreeSerif.ttf").deriveFont(Font.ITALIC, 12f)));                
                                 // It's actually between 12 and 13 but Java rounds up.

        tests.add(new TestConfig("Hiero FreeSerif-32", 
                                 foxIpsum,
                                 "jme3test/font/FreeSerif32.fnt",
                                 loadTtf("/jme3test/font/FreeSerif.ttf").deriveFont(32f)));                

        tests.add(new TestConfig("AngelCode FreeSerif-32", 
                                 foxIpsum,
                                 "jme3test/font/BM-FreeSerif32.fnt",
                                 loadTtf("/jme3test/font/FreeSerif.ttf").deriveFont(25f)));                

        tests.add(new TestConfig("Hiero FreeSerif-64-Italic", 
                                 foxIpsum,
                                 "jme3test/font/FreeSerif64I.fnt",
                                 loadTtf("/jme3test/font/FreeSerif.ttf").deriveFont(Font.ITALIC, 64f)));                

        tests.add(new TestConfig("AngelCode FreeSerif-64-Italic", 
                                 foxIpsum,
                                 "jme3test/font/BM-FreeSerif64I.fnt",
                                 loadTtf("/jme3test/font/FreeSerif.ttf").deriveFont(Font.ITALIC, 50f)));                


        /*tests.add(new TestConfig("Japanese", 
                                 "\u3042\u3047\u3070\u3090\u309E\u3067\u308A\u3089\n"+
                                 "\u3042\u3047\u3070\u3090\u309E\u3067\u308A\u3089",
                                 "jme3test/font/DJapaSubset.fnt",
                                 loadTtf("/jme3test/font/DroidSansFallback.ttf").deriveFont(32f)));*/

        /*tests.add(new TestConfig("DroidSansMono-32",
                                 "ĂăĄąĔĕĖėχψωӮӯ₴₵₹\n"+
                                 "ĂăĄąĔĕĖėχψωӮӯ₴₵₹",
                                 "jme3test/font/DMono32BI.fnt",
                                 loadTtf("/jme3test/font/DroidSansMono.ttf").deriveFont(32f)));*/

        /*tests.add(new TestConfig("DroidSansMono-32",
                                 "ĂăĄąĔĕĖėχψωӮӯ\n"+
                                 "ĂăĄąĔĕĖėχψωӮӯ",
                                 "jme3test/font/DMono32BI.fnt",
                                 loadTtf("/jme3test/font/DroidSansMono.ttf").deriveFont(Font.BOLD | Font.ITALIC, 32f)));
        */                                         

        // Set up the test root node so that y = 0 is the top of the screen
        testRoot.setLocalTranslation(0, cam.getHeight(), 0);
        testRoot.attachChild(scrollRoot);
        guiNode.attachChild(testRoot);

        float y = 0; //cam.getHeight();
        
        for( TestConfig test : tests ) {
            System.out.println("y:" + y);
                    
            Node vis = createVisual(test);
            
            BoundingBox bb = (BoundingBox)vis.getWorldBound();
            System.out.println("bb:" + bb); 
 
            // Render it relative to y, projecting down
            vis.setLocalTranslation(1 + bb.getCenter().x - bb.getXExtent(), 
                                    y - bb.getCenter().y - bb.getYExtent(), 
                                    0);
            //vis.setLocalTranslation(1, y, 0);                                    
            scrollRoot.attachChild(vis);
            
            // Position to render the next one
            y -= bb.getYExtent() * 2;
            
            // plus 5 pixels of padding
            y -= 5;            
        }
    }        
 
    protected void resetZoom() {
        testRoot.setLocalScale(1, 1, 1);
    }
    
    protected void resetView() {
        resetZoom();
        scrollRoot.setLocalTranslation(0, 0, 0); 
    }
    
    protected void setupUserInput() {

        inputManager.addMapping(SCROLL_UP, new KeyTrigger(KeyInput.KEY_UP),
                                           new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping(SCROLL_DOWN, new KeyTrigger(KeyInput.KEY_DOWN),
                                           new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping(SCROLL_LEFT, new KeyTrigger(KeyInput.KEY_LEFT),
                                           new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping(SCROLL_RIGHT, new KeyTrigger(KeyInput.KEY_RIGHT),
                                           new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping(ZOOM_IN, new KeyTrigger(KeyInput.KEY_ADD), 
                                         new KeyTrigger(KeyInput.KEY_EQUALS),
                                         new KeyTrigger(KeyInput.KEY_Q));
        inputManager.addMapping(ZOOM_OUT, new KeyTrigger(KeyInput.KEY_MINUS),
                                         new KeyTrigger(KeyInput.KEY_SUBTRACT),
                                         new KeyTrigger(KeyInput.KEY_Z));
        inputManager.addMapping(RESET_VIEW, new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping(RESET_ZOOM, new KeyTrigger(KeyInput.KEY_0));

        inputManager.addListener(new KeyStateListener(), 
                                 RESET_VIEW, RESET_ZOOM,
                                 SCROLL_UP, SCROLL_DOWN, SCROLL_LEFT, SCROLL_RIGHT,
                                 ZOOM_IN, ZOOM_OUT);            
    }
 
    @Override
    public void simpleUpdate( float tpf ) {
        if( scroll.lengthSquared() != 0 ) {
            scrollRoot.move(scroll.mult(tpf));
        }
        if( zoom.lengthSquared() != 0 ) {
            Vector3f current = testRoot.getLocalScale();
            testRoot.setLocalScale(current.add(zoom.mult(tpf)));
        }
    }
 
    private class KeyStateListener implements ActionListener {  
        @Override
        public void onAction(String name, boolean value, float tpf) {
            switch( name ) {
                case RESET_VIEW:
                    // Only on the up 
                    if( !value ) {
                        resetView();
                    }                
                    break;
                case RESET_ZOOM:
                    // Only on the up 
                    if( !value ) {
                        resetZoom();
                    }
                    break;
                case ZOOM_IN:
                    if( value ) {
                        zoom.set(ZOOM_SPEED, ZOOM_SPEED, 0);
                    } else {
                        zoom.set(0, 0, 0);
                    } 
                    break;
                case ZOOM_OUT:
                    if( value ) {
                        zoom.set(-ZOOM_SPEED, -ZOOM_SPEED, 0);
                    } else {
                        zoom.set(0, 0, 0);
                    } 
                    break;
                case SCROLL_UP:
                    if( value ) {
                        scroll.set(0, -SCROLL_SPEED, 0);
                    } else {
                        scroll.set(0, 0, 0);
                    } 
                    break;
                case SCROLL_DOWN:
                    if( value ) {
                        scroll.set(0, SCROLL_SPEED, 0);
                    } else {
                        scroll.set(0, 0, 0);
                    } 
                    break;
                case SCROLL_LEFT:
                    if( value ) {
                        scroll.set(SCROLL_SPEED, 0, 0);
                    } else {
                        scroll.set(0, 0, 0);
                    } 
                    break;
                case SCROLL_RIGHT:
                    if( value ) {
                        scroll.set(-SCROLL_SPEED, 0, 0);
                    } else {
                        scroll.set(0, 0, 0);
                    } 
                    break;
            }
        }
    }

    private class TestConfig {
        String name;
        String jmeFont;
        Font awtFont;
        String text;
        
        public TestConfig( String name, String text, String jmeFont, Font awtFont ) {
            this.name = name;
            this.text = text;
            this.jmeFont = jmeFont;
            this.awtFont = awtFont;
        }
    }
}
