/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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
package com.jme3.system;

import com.jme3.renderer.Renderer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class AppSettings extends HashMap<String, Object> {

    private static final AppSettings defaults = new AppSettings(false);
    public static final String LWJGL_OPENGL1 = "LWJGL-OPENGL1",
                               LWJGL_OPENGL2 = "LWJGL-OpenGL2",
                               LWJGL_OPENGL3 = "LWJGL-OpenGL3",
                               LWJGL_OPENGL_ANY = "LWJGL-OpenGL-Any",
                               JOGL = "JOGL",
                               NULL = "NULL";
    public static final String LWJGL_OPENAL = "LWJGL";
    private String settingsDialogImage = "/com/jme3/app/Monkey.png";

    static {
        defaults.put("Width", 640);
        defaults.put("Height", 480);
        defaults.put("BitsPerPixel", 24);
        defaults.put("Frequency", 60);
        defaults.put("DepthBits", 24);
        defaults.put("StencilBits", 0);
        defaults.put("Samples", 0);
        defaults.put("Fullscreen", false);
        defaults.put("Title", "jMonkey Engine 3.0");
        defaults.put("Renderer", LWJGL_OPENGL2);
        defaults.put("AudioRenderer", LWJGL_OPENAL);
        defaults.put("DisableJoysticks", true);
        defaults.put("UseInput", true);
        defaults.put("VSync", false);
        defaults.put("FrameRate", -1);
      //  defaults.put("Icons", null);

        // disable these settings to benchmark speed
//        defaults.put("VSync", true);
//        defaults.put("FrameRate", 60);
    }

    /**
     * Create Application settings
     * use loadDefault=true, to load jME default values.
     * use false if you want to change some settings but you would like the application to remind other settings from previous launches
     * @param loadDefaults
     */
    public AppSettings(boolean loadDefaults) {
        if (loadDefaults) {
            putAll(defaults);
        }
    }

    public void copyFrom(AppSettings other) {
        this.putAll(other);
    }

    public void mergeFrom(AppSettings other) {
        for (String key : other.keySet()) {
            if (get(key) == null) {
                put(key, other.get(key));
            }
        }
    }

    public void load(InputStream in) throws IOException {
        Properties props = new Properties();
        props.load(in);
        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            String key = (String) entry.getKey();
            String val = (String) entry.getValue();
            if (val != null) {
                val = val.trim();
            }
            if (key.endsWith("(int)")) {
                key = key.substring(0, key.length() - 5);
                int iVal = Integer.parseInt(val);
                putInteger(key, iVal);
            } else if (key.endsWith("(string)")) {
                putString(key.substring(0, key.length() - 8), val);
            } else if (key.endsWith("(bool)")) {
                boolean bVal = Boolean.parseBoolean(val);
                putBoolean(key.substring(0, key.length() - 6), bVal);
            } else {
                throw new IOException("Cannot parse key: " + key);
            }
        }
    }

    public void save(OutputStream out) throws IOException {
        Properties props = new Properties();
        for (Map.Entry<String, Object> entry : entrySet()) {
            Object val = entry.getValue();
            String type;
            if (val instanceof Integer) {
                type = "(int)";
            } else if (val instanceof String) {
                type = "(string)";
            } else if (val instanceof Boolean) {
                type = "(bool)";
            } else {
                throw new UnsupportedEncodingException();
            }
            props.setProperty(entry.getKey() + type, val.toString());
        }
        props.store(out, "jME3 AppSettings");
    }

    public void load(String preferencesKey) throws BackingStoreException {
        Preferences prefs = Preferences.userRoot().node(preferencesKey);
        String[] keys = prefs.keys();
        if (keys != null) {
            for (String key : keys) {
                Object defaultValue = defaults.get(key);
                if (defaultValue instanceof Integer) {
                    put(key, prefs.getInt(key, (Integer) defaultValue));
                } else if (defaultValue instanceof String) {
                    put(key, prefs.get(key, (String) defaultValue));
                } else if (defaultValue instanceof Boolean) {
                    put(key, prefs.getBoolean(key, (Boolean) defaultValue));
                }
            }
        }
    }

    public void save(String preferencesKey) throws BackingStoreException {
        Preferences prefs = Preferences.userRoot().node(preferencesKey);
        for (String key : keySet()) {         
            prefs.put(key, get(key).toString());
        }
    }

    public int getInteger(String key) {
        Integer i = (Integer) get(key);
        if (i == null) {
            return 0;
        }

        return i.intValue();
    }

    public boolean getBoolean(String key) {
        Boolean b = (Boolean) get(key);
        if (b == null) {
            return false;
        }

        return b.booleanValue();
    }

    public String getString(String key) {
        String s = (String) get(key);
        if (s == null) {
            return null;
        }

        return s;
    }

    public void putInteger(String key, int value) {
        put(key, Integer.valueOf(value));
    }

    public void putBoolean(String key, boolean value) {
        put(key, Boolean.valueOf(value));
    }

    public void putString(String key, String value) {
        put(key, value);
    }

    /**
     * @param frameRate The frame-rate is the upper limit on how high
     * the application's frames-per-second can go.
     * (Default: -1 no frame rate limit imposed)
     */
    public void setFrameRate(int frameRate) {
        putInteger("FrameRate", frameRate);
    }

    /**
     * @param use If true, the application will initialize and use input.
     * Set to false for headless applications that do not require keyboard
     * or mouse input.
     * (Default: true)
     */
    public void setUseInput(boolean use) {
        putBoolean("UseInput", use);
    }

    /**
     * @param use If true, the application will initialize and use joystick
     * input. Set to false if no joystick input is desired.
     * (Default: false)
     */
    public void setUseJoysticks(boolean use) {
        putBoolean("DisableJoysticks", !use);
    }

    /**
     * Set the graphics renderer to use, one of:<br>
     * <ul>
     * <li>AppSettings.LWJGL_OPENGL1 - Force OpenGL1.1 compatability</li>
     * <li>AppSettings.LWJGL_OPENGL2 - Force OpenGL2 compatability</li>
     * <li>AppSettings.LWJGL_OPENGL3 - Force OpenGL3.3 compatability</li>
     * <li>AppSettings.LWJGL_OPENGL_ANY - Choose an appropriate 
     * OpenGL version based on system capabilities</li>
     * <li>null - Disable graphics rendering</li>
     * </ul>
     * @param renderer The renderer to set
     * (Default: AppSettings.LWJGL_OPENGL2)
     */
    public void setRenderer(String renderer) {
        putString("Renderer", renderer);
    }

    /**
     * Set a custom graphics renderer to use. The class should implement 
     * the {@link Renderer} interface.
     * @param clazz The custom graphics renderer class.
     * (Default: not set)
     */
    public void setCustomRenderer(Class clazz){
        put("Renderer", "CUSTOM" + clazz.getName());
    }

    /**
     * Set the audio renderer to use. One of:<br>
     * <ul>
     * <li>AppSettings.LWJGL_OPENAL - Default for LWJGL</li>
     * <li>null - Disable audio</li>
     * </ul>
     * @param audioRenderer 
     * (Default: LWJGL)
     */
    public void setAudioRenderer(String audioRenderer) {
        putString("AudioRenderer", audioRenderer);
    }

    /**
     * @param value the width for the rendering display.
     * (Default: 640)
     */
    public void setWidth(int value) {
        putInteger("Width", value);
    }

    /**
     * @param value the height for the rendering display.
     * (Default: 480)
     */
    public void setHeight(int value) {
        putInteger("Height", value);
    }

    /**
     * Set the resolution for the rendering display
     * @param width The width
     * @param height The height
     * (Default: 640x480)
     */
    public void setResolution(int width, int height) {
        setWidth(width);
        setHeight(height);
    }

    /**
     * Set the frequency, also known as refresh rate, for the 
     * rendering display.
     * @param value The frequency
     * (Default: 60)
     */
    public void setFrequency(int value) {
        putInteger("Frequency", value);
    }

    /**
     * Set the bits per pixel for the display. Appropriate
     * values are 16 for RGB565 color format, or 24 for RGB8 color format.
     * 
     * @param value The bits per pixel to set
     * (Default: 24)
     */
    public void setBitsPerPixel(int value) {
        putInteger("BitsPerPixel", value);
    }

    /**
     * Set the number of samples per pixel. A value of 1 indicates
     * each pixel should be single-sampled, higher values indicate
     * a pixel should be multi-sampled.
     * 
     * @param value The number of samples
     * (Default: 1)
     */
    public void setSamples(int value) {
        putInteger("Samples", value);
    }

    /**
     * @param title The title of the rendering display
     * (Default: jMonkeyEngine 3.0)
     */
    public void setTitle(String title) {
        putString("Title", title);
    }

    /**
     * @param value true to enable full-screen rendering, false to render in a window
     * (Default: false)
     */
    public void setFullscreen(boolean value) {
        putBoolean("Fullscreen", value);
    }

    /**
     * Set to true to enable vertical-synchronization, limiting and synchronizing
     * every frame rendered to the monitor's refresh rate.
     * @param value 
     * (Default: false)
     */
    public void setVSync(boolean value) {
        putBoolean("VSync", value);
    }
    
    /**
     * Enable 3D stereo.
     * <p>This feature requires hardware support from the GPU driver. 
     * See: http://en.wikipedia.org/wiki/Quad_buffering<br>
     * Once enabled, filters or scene processors that handle 3D stereo rendering
     * could use this feature to render using hardware 3D stereo.</p>
     * (Default: false)
     */
    public void setStereo3D(boolean value){
        putBoolean("Stereo3D", value);
    }

    /**
     * Sets the application icons to be used, with the most preferred first.
     * For Windows you should supply at least one 16x16 icon and one 32x32. The former is used for the title/task bar,
     * the latter for the alt-tab icon.
     * Linux (and similar platforms) expect one 32x32 icon.
     * Mac OS X should be supplied one 128x128 icon.
     * <br/>
     * The icon is used for the settings window, and the LWJGL render window. Not currently supported for JOGL.
     * Note that a bug in Java 6 (bug ID 6445278, currently hidden but available in Google cache) currently prevents
     * the icon working for alt-tab on the settings dialog in Windows.
     *
     * @param value An array of BufferedImages to use as icons.
     * (Default: not set)
     */
    public void setIcons(Object[] value) {
        put("Icons", value);
    }

    public int getFrameRate() {
        return getInteger("FrameRate");
    }

    public boolean useInput() {
        return getBoolean("UseInput");
    }

    public String getRenderer() {
        return getString("Renderer");
    }

    public int getWidth() {
        return getInteger("Width");
    }

    public int getHeight() {
        return getInteger("Height");
    }

    public int getBitsPerPixel() {
        return getInteger("BitsPerPixel");
    }

    public int getFrequency() {
        return getInteger("Frequency");
    }

    public int getDepthBits() {
        return getInteger("DepthBits");
    }

    public int getStencilBits() {
        return getInteger("StencilBits");
    }

    public int getSamples() {
        return getInteger("Samples");
    }

    public String getTitle() {
        return getString("Title");
    }

    public boolean isVSync() {
        return getBoolean("VSync");
    }

    public boolean isFullscreen() {
        return getBoolean("Fullscreen");
    }

    public boolean useJoysticks() {
        return !getBoolean("DisableJoysticks");
    }

    public String getAudioRenderer() {
        return getString("AudioRenderer");
    }
    
    public boolean useStereo3D(){
        return getBoolean("Stereo3D");  
    }

    public Object[] getIcons() {
        return (Object[]) get("Icons");
    }

    public void setSettingsDialogImage(String path) {
        settingsDialogImage = path;
    }

    public String getSettingsDialogImage() {
        return settingsDialogImage;
    }
}
