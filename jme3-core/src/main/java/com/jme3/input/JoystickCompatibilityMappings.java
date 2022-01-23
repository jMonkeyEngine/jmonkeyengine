/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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
package com.jme3.input;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Provides compatibility mapping to different joysticks
 * that both report their name in a unique way and require
 * remapping to achieve a proper default layout.
 *
 * <p>All mappings MUST be defined before the joystick support
 * has been initialized in the InputManager.</p>
 *
 * @author Paul Speed
 * @author Markil3
 */
public class JoystickCompatibilityMappings {

    private static final Logger logger = Logger.getLogger(JoystickCompatibilityMappings.class.getName());

    // List of resource paths to check for the joystick-mapping.properties
    // files.
    private static String[] searchPaths = {"joystick-mapping.properties"};

    private static Map<String, Map<String, String>> joystickMappings = new HashMap<String, Map<String, String>>();
    private static Map<String, Map<String, AxisData>> axisMappings = new HashMap<String, Map<String, AxisData>>();
    private static Map<JoystickAxis, float[]> axisRangeMappings = new HashMap<>();
    private static Map<String, Map<String, String>> buttonMappings = new HashMap<String, Map<String, String>>();

    // Remaps names by regex.
    final private static Map<Pattern, String> nameRemappings = new HashMap<>();
    final private static Map<String, String> nameCache = new HashMap<>();

    static {
        loadDefaultMappings();
    }

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private JoystickCompatibilityMappings() {
    }

    protected static Map<String, String> getMappings(String joystickName, boolean create) {
        Map<String, String> result = joystickMappings.get(joystickName.trim());
        if (result == null && create) {
            result = new HashMap<String, String>();
            joystickMappings.put(joystickName.trim(), result);
        }
        return result;
    }

    /**
     * Obtains mappings specific to the joystick axis
     *
     * @param joystickName - The name of the joystick type to obtain mappings for.
     * @param create       - If there are no mappings present and this parameter is true, then a new entry for this joystick is created.
     * @return The various axis remappings for the requested joystick, or null of there are none.
     */
    private static Map<String, AxisData> getAxisMappings(String joystickName, boolean create) {
        Map<String, AxisData> result = axisMappings.get(joystickName.trim());
        if (result == null && create) {
            result = new HashMap<String, AxisData>();
            axisMappings.put(joystickName.trim(), result);
        }
        return result;
    }

    /**
     * Obtains mappings specific to the joystick buttons
     *
     * @param joystickName - The name of the joystick type to obtain mappings for.
     * @param create       - If there are no mappings present and this parameter is true, then a new entry for this joystick is created.
     * @return The various button remappings for the requested joystick, or null of there are none.
     */
    protected static Map<String, String> getButtonMappings(String joystickName, boolean create) {
        Map<String, String> result = buttonMappings.get(joystickName.trim());
        if (result == null && create) {
            result = new HashMap<String, String>();
            buttonMappings.put(joystickName.trim(), result);
        }
        return result;
    }

    /**
     * This method will take a "raw" axis value from the system and rescale it based on what the remapper has specified. For example, if the remapper specified an axis to be scaled to [0.0,1.0], then a raw value of -0.5 would be converted to 0.25.
     *
     * @param axis         - The axis to remap.
     * @param currentValue - The raw value the system is outputting, on a scale of -1.0 to 1.0.
     * @return The new value that will be provided to listeners, on a scale specified by the remappings file.
     */
    public static float remapAxisRange(JoystickAxis axis, float currentValue) {
        String joyName = axis.getJoystick().getName();
        Map<String, AxisData> map;
        float[] range = axisRangeMappings.get(axis);
        if (range == null) {
            map = getAxisMappings(joyName, false);
            if (map != null && map.containsKey(axis.getName())) {
                range = map.get(axis.getName()).range;
                axisRangeMappings.put(axis, range);
            } else {
                // Try the normalized name
                joyName = getNormalizedName(joyName);
                if (joyName != null) {
                    map = getAxisMappings(joyName, false);
                    if (map != null && map.containsKey(axis.getName())) {
                        range = map.get(axis.getName()).range;
                        axisRangeMappings.put(axis, range);
                    }
                }
            }
        }
        if (range == null) {
            axisRangeMappings.put(axis, new float[0]);
            return currentValue;
        }

        /*
         * If we have an array of size 0, that means we have acknowledged this axis (so we don't
         * need to go searching for it every tick), but that there is no remapping.
         */
        if (range.length == 0) {
            return currentValue;
        }

        return (currentValue + range[1] + range[0]) * ((range[1] - range[0]) / 2);
    }

    /**
     * Takes the original name of an axis, specifically, and returns the new name it will function under.
     *
     * @param joystickName - The joystick type the axis comes from.
     * @param componentId  - The system-provided name for the axis.
     * @return The new name for the axis, or just componentId if no remapping was provided.
     */
    public static String remapAxis(String joystickName, String componentId) {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "remapAxis({0}, {1})", new Object[]{joystickName, componentId});
        }

        // Always try the specific name first.
        joystickName = joystickName.trim();
        Map map = getAxisMappings(joystickName, false);
        if (map != null && map.containsKey(componentId)) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "returning remapped axis:{0}", map.get(componentId));
            }
            return ((AxisData) map.get(componentId)).name;
        }

        map = getMappings(joystickName, false);
        if (map != null && map.containsKey(componentId)) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "returning remapped axis:{0}", map.get(componentId));
            }
            return ((String) map.get(componentId));
        }

        // Try the normalized name
        joystickName = getNormalizedName(joystickName);
        logger.log(Level.FINE, "normalized joystick name:{0}", joystickName);
        if (joystickName == null) {
            return componentId;
        }

        map = getAxisMappings(joystickName, false);
        if (map != null && map.containsKey(componentId)) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "returning remapped:{0}", map.get(componentId));
            }
            return ((AxisData) map.get(componentId)).name;
        }

        map = getMappings(joystickName, false);
        if (map != null && map.containsKey(componentId)) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "returning remapped:{0}", map.get(componentId));
            }
            return ((String) map.get(componentId));
        }

        return componentId;
    }

    /**
     * Takes the original name of a button, specifically, and returns the new name it will function under.
     *
     * @param joystickName - The joystick type the axis comes from.
     * @param componentId  - The system-provided name for the button.
     * @return The new name for the button, or just componentId if no remapping was provided.
     */
    public static String remapButton(String joystickName, String componentId) {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "remapAxis({0}, {1})", new Object[]{joystickName, componentId});
        }

        // Always try the specific name first.
        joystickName = joystickName.trim();
        Map<String, String> map = getButtonMappings(joystickName, false);
        if (map != null && map.containsKey(componentId)) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "returning remapped axis:{0}", map.get(componentId));
            }
            return map.get(componentId);
        }

        map = getMappings(joystickName, false);
        if (map != null && map.containsKey(componentId)) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "returning remapped axis:{0}", map.get(componentId));
            }
            return map.get(componentId);
        }

        // Try the normalized name
        joystickName = getNormalizedName(joystickName);
        logger.log(Level.FINE, "normalized joystick name:{0}", joystickName);
        if (joystickName == null) {
            return componentId;
        }

        map = getButtonMappings(joystickName, false);
        if (map != null && map.containsKey(componentId)) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "returning remapped:{0}", map.get(componentId));
            }
            return map.get(componentId);
        }

        map = getMappings(joystickName, false);
        if (map != null && map.containsKey(componentId)) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "returning remapped:{0}", map.get(componentId));
            }
            return map.get(componentId);
        }

        return componentId;
    }

    /**
     * Returns the remapped version of the axis/button name if there
     * is a mapping for it otherwise it returns the original name.
     *
     * @param joystickName which joystick (not null)
     * @param componentId the unmapped axis/button name
     * @return the resulting axis/button name
     */
    public static String remapComponent(String joystickName, String componentId) {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "remapComponent({0}, {1})", new Object[]{joystickName, componentId});
        }

        // Always try the specific name first.
        joystickName = joystickName.trim();
        Map<String, String> map = getMappings(joystickName, false);
        if (map != null && map.containsKey(componentId)) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "returning remapped:{0}", map.get(componentId));
            }
            return map.get(componentId);
        }
        // Try the normalized name
        joystickName = getNormalizedName(joystickName);
        logger.log(Level.FINE, "normalized joystick name:{0}", joystickName);
        if (joystickName == null) {
            return componentId;
        }
        map = getMappings(joystickName, false);
        if (map == null) {
            return componentId;
        }
        if (!map.containsKey(componentId)) {
            return componentId;
        }
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "returning remapped:{0}", map.get(componentId));
        }
        return map.get(componentId);
    }

    /**
     * Returns a set of Joystick button name remappings if they exist otherwise
     * it returns an empty map.
     *
     * @param joystickName which joystick (not null)
     * @return an unmodifiable map
     */
    public static Map<String, String> getJoystickButtonMappings(String joystickName) {
        Map<String, String> result = getButtonMappings(joystickName.trim(), false);
        if (result == null)
            return Collections.emptyMap();
        return Collections.unmodifiableMap(result);
    }

    /**
     * Returns a set of Joystick axis/button name remappings if they exist otherwise
     * it returns an empty map.
     *
     * @param joystickName which joystick (not null)
     * @return an unmodifiable map
     */
    public static Map<String, String> getJoystickMappings(String joystickName) {
        Map<String, String> result = getMappings(joystickName.trim(), false);
        if (result == null)
            return Collections.emptyMap();
        return Collections.unmodifiableMap(result);
    }

    /**
     * Adds a single Joystick axis or button remapping based on the
     * joystick's name and axis/button name.  The "remap" value will be
     * used instead.
     *
     * @param stickName which joystick (not null)
     * @param sourceComponentId the name to be remapped
     * @param remapId the remapped name
     */
    public static void addAxisMapping(String stickName, String sourceComponentId, String remapId) {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "addAxisMapping({0}, {1}, {2})",
                    new Object[]{stickName, sourceComponentId, remapId});
        }
        getAxisMappings(stickName, true).put(sourceComponentId, new AxisData(remapId, new float[0]));
    }

    /**
     * Adds a single Joystick axis or button remapping based on the
     * joystick's name and axis/button name.  The "remap" value will be
     * used instead.
     *
     * @param stickName which joystick (not null)
     * @param sourceComponentId the name to be remapped
     * @param remapId the remapped name
     * @param range the desired range (not null, exactly 2 elements)
     */
    public static void addAxisMapping(String stickName, String sourceComponentId, String remapId, float[] range) {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "addAxisMapping({0}, {1}, {2})",
                    new Object[]{stickName, sourceComponentId, remapId});
        }
        if (range.length != 2) {
            throw new IllegalArgumentException("The range must have exactly 2 elements");
        }
        getAxisMappings(stickName, true).put(sourceComponentId, new AxisData(remapId, range));
    }

    /**
     * Adds a single Joystick axis or button remapping based on the
     * joystick's name and axis/button name.  The "remap" value will be
     * used instead.
     *
     * @param stickName which joystick (not null)
     * @param sourceComponentId the name to be remapped
     * @param remapId the remapped name
     */
    public static void addButtonMapping(String stickName, String sourceComponentId, String remapId) {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "addButtonMapping({0}, {1}, {2})",
                    new Object[]{stickName, sourceComponentId, remapId});
        }
        getButtonMappings(stickName, true).put(sourceComponentId, remapId);
    }

    /**
     * Adds a single Joystick axis or button remapping based on the
     * joystick's name and axis/button name.  The "remap" value will be
     * used instead.
     *
     * @param stickName which joystick (not null)
     * @param sourceComponentId the name to be remapped
     * @param remapId the remapped name
     */
    public static void addMapping(String stickName, String sourceComponentId, String remapId) {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "addMapping({0}, {1}, {2})",
                    new Object[]{stickName, sourceComponentId, remapId});
        }
        getMappings(stickName, true).put(sourceComponentId, remapId);
    }

    /**
     * Adds a preconfigured set of mappings in Properties object
     * form where the names are dot notation
     * "axis"/"button"/"". "joystick"."axis/button name"
     * and the values are the remapped component name.  This calls
     * addMapping(stickName, sourceComponent, remap) for every property
     * that it is able to parse.
     *
     * @param p (not null)
     */
    public static void addMappings(Properties p) {
        final String AXIS_LABEL = "axis";
        final String BUTTON_LABEL = "button";

        float[] range;
        int lBracketIndex, rBracketIndex, commaIndex;

        for (Map.Entry<Object, Object> e : p.entrySet()) {
            range = null;
            String key = String.valueOf(e.getKey()).trim();

            int firstSplit = key.indexOf('.');
            int split = key.lastIndexOf('.');
            if (split < 0) {
                logger.log(Level.WARNING, "Skipping mapping:{0}", e);
                continue;
            }

            String type;
            if (firstSplit >= 0 && firstSplit != split) {
                type = key.substring(0, firstSplit).trim();
                if (!type.equals(AXIS_LABEL) && !type.equals(BUTTON_LABEL)) {
                    /*
                     * In this case, the "type" is probably a part of the
                     * joystick name.
                     */
                    firstSplit = -1;
                    type = "";
                }
            } else {
                firstSplit = -1;
                type = "";
            }
            String stick = key.substring(firstSplit + 1, split).trim();
            String component = key.substring(split + 1).trim();
            String value = String.valueOf(e.getValue()).trim();
            if ("regex".equals(component)) {
                // It's a name remapping
                addJoystickNameRegex(value, stick);
            }
            if ((lBracketIndex = value.indexOf('[')) > 0) {
                /*
                 * This means that there is an axis range.
                 */
                range = new float[2];
                rBracketIndex = value.indexOf(']');
                commaIndex = value.indexOf(',');
                if (rBracketIndex > -1 && commaIndex > -1) {
                    try {
                        range[0] = Float.parseFloat(value.substring(lBracketIndex + 1, commaIndex).trim());
                        range[1] = Float.parseFloat(value.substring(commaIndex + 1, rBracketIndex).trim());
                        value = value.substring(0, lBracketIndex).trim();
                        type = AXIS_LABEL;
                    } catch (NumberFormatException nfe) {
                        logger.log(Level.SEVERE, "Could not parse axis range \"" + value.substring(lBracketIndex) + "\"", nfe);
                    }
                }
            }
            switch (type) {
                case AXIS_LABEL:
                    if (range == null) {
                        addAxisMapping(stick, component, value);
                    } else {
                        addAxisMapping(stick, component, value, range);
                    }
                    break;
                case BUTTON_LABEL:
                    addButtonMapping(stick, component, value);
                    break;
                default:
                    addMapping(stick, component, value);
            }
        }
    }

    /**
     * Maps a regular expression to a normalized name for that joystick.
     *
     * @param regex the regular expression to be matched
     * @param name the remapped name
     */
    public static void addJoystickNameRegex(String regex, String name) {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "addJoystickNameRegex({0}, {1})", new Object[]{regex, name});
        }
        nameRemappings.put(Pattern.compile(regex), name);
    }

    protected static String getNormalizedName(String name) {
        String result = nameCache.get(name);
        if (result != null) {
            return result;
        }
        for (Map.Entry<Pattern, String> e : nameRemappings.entrySet()) {
            Pattern p = e.getKey();
            Matcher m = p.matcher(name);
            if (m.matches()) {
                nameCache.put(name, e.getValue());
                return e.getValue();
            }
        }
        return null;
    }

    /**
     * Loads a set of compatibility mappings from the property file
     * specified by the given URL.
     *
     * @param u the URL of the properties file (not null)
     * @throws IOException if an I/O exception occurs
     */
    public static void loadMappingProperties(URL u) throws IOException {
        logger.log(Level.FINE, "Loading mapping properties:{0}", u);
        InputStream in = u.openStream();
        try {
            Properties p = new Properties();
            p.load(in);
            addMappings(p);
        } finally {
            in.close();
        }
    }

    protected static void loadMappings(ClassLoader cl, String path) throws IOException {
        logger.log(Level.FINE, "Searching for mappings for path:{0}", path);
        for (Enumeration<URL> en = cl.getResources(path); en.hasMoreElements(); ) {
            URL u = en.nextElement();
            try {
                loadMappingProperties(u);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error loading:" + u, e);
            }
        }

    }

    /**
     * Loads the default compatibility mappings by looking for
     * joystick-mapping.properties files on the classpath.
     */
    protected static void loadDefaultMappings() {
        for (String s : searchPaths) {
            try {
                loadMappings(JoystickCompatibilityMappings.class.getClassLoader(), s);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error searching resource path:{0}", s);
            }
        }
    }

    private static class AxisData {
        String name;
        float[] range;

        AxisData(String name, float[] range) {
            this.name = name;
            this.range = range;
        }
    }
}
