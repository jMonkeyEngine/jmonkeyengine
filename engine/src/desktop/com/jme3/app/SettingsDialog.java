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
package com.jme3.app;

import com.jme3.system.AppSettings;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import javax.swing.*;

/**
 * <code>PropertiesDialog</code> provides an interface to make use of the
 * <code>GameSettings</code> class. The <code>GameSettings</code> object
 * is still created by the client application, and passed during construction.
 * 
 * @see com.jme.system.GameSettings
 * @author Mark Powell
 * @author Eric Woroshow
 * @author Joshua Slack - reworked for proper use of GL commands.
 * @version $Id: LWJGLPropertiesDialog.java 4131 2009-03-19 20:15:28Z blaine.dev $
 */
public final class SettingsDialog extends JDialog {

    public static interface SelectionListener {

        public void onSelection(int selection);
    }
    private static final Logger logger = Logger.getLogger(SettingsDialog.class.getName());
    private static final long serialVersionUID = 1L;
    public static final int NO_SELECTION = 0,
            APPROVE_SELECTION = 1,
            CANCEL_SELECTION = 2;
    // connection to properties file.
    private final AppSettings source;
    // Title Image
    private URL imageFile = null;
    // Array of supported display modes
    private DisplayMode[] modes = null;
    // Array of windowed resolutions
    private String[] windowedResolutions = {"320 x 240", "640 x 480", "800 x 600",
        "1024 x 768", "1152 x 864", "1280 x 720"};
    // UI components
    private JCheckBox vsyncBox = null;
    private JCheckBox fullscreenBox = null;
    private JComboBox displayResCombo = null;
    private JComboBox colorDepthCombo = null;
    private JComboBox displayFreqCombo = null;
//    private JComboBox rendererCombo = null;
    private JComboBox antialiasCombo = null;
    private JLabel icon = null;
    private int selection = 0;
    private SelectionListener selectionListener = null;

    /**
     * Constructor for the <code>PropertiesDialog</code>. Creates a
     * properties dialog initialized for the primary display.
     *
     * @param source
     *            the <code>AppSettings</code> object to use for working with
     *            the properties file.
     * @param imageFile
     *            the image file to use as the title of the dialog;
     *            <code>null</code> will result in to image being displayed
     * @throws NullPointerException
     *             if the source is <code>null</code>
     */
    public SettingsDialog(AppSettings source, String imageFile, boolean loadSettings) {
        this(source, getURL(imageFile), loadSettings);
    }

    /**
     * Constructor for the <code>PropertiesDialog</code>. Creates a
     * properties dialog initialized for the primary display.
     * 
     * @param source
     *            the <code>GameSettings</code> object to use for working with
     *            the properties file.
     * @param imageFile
     *            the image file to use as the title of the dialog;
     *            <code>null</code> will result in to image being displayed
     * @param loadSettings 
     * @throws JmeException
     *             if the source is <code>null</code>
     */
    public SettingsDialog(AppSettings source, URL imageFile, boolean loadSettings) {
        if (source == null) {
            throw new NullPointerException("Settings source cannot be null");
        }

        this.source = source;
        this.imageFile = imageFile;

//        setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        setModal(true);

        AppSettings registrySettings = new AppSettings(true);

        String appTitle;
        if(source.getTitle()!=null){
            appTitle = source.getTitle();
        }else{
           appTitle = registrySettings.getTitle();
        }
        try {
            registrySettings.load(appTitle);
        } catch (BackingStoreException ex) {
            logger.log(Level.WARNING,
                    "Failed to load settings", ex);
        }

        if (loadSettings) {
            source.copyFrom(registrySettings);
        } else if(!registrySettings.isEmpty()) {
            source.mergeFrom(registrySettings);
        }

        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

        modes = device.getDisplayModes();
        Arrays.sort(modes, new DisplayModeSorter());

        createUI();
    }

    public void setSelectionListener(SelectionListener sl) {
        this.selectionListener = sl;
    }

    public int getUserSelection() {
        return selection;
    }

    private void setUserSelection(int selection) {
        this.selection = selection;
        selectionListener.onSelection(selection);
    }

    /**
     * <code>setImage</code> sets the background image of the dialog.
     * 
     * @param image
     *            <code>String</code> representing the image file.
     */
    public void setImage(String image) {
        try {
            URL file = new URL("file:" + image);
            setImage(file);
            // We can safely ignore the exception - it just means that the user
            // gave us a bogus file
        } catch (MalformedURLException e) {
        }
    }

    /**
     * <code>setImage</code> sets the background image of this dialog.
     * 
     * @param image
     *            <code>URL</code> pointing to the image file.
     */
    public void setImage(URL image) {
        icon.setIcon(new ImageIcon(image));
        pack(); // Resize to accomodate the new image
        setLocationRelativeTo(null); // put in center
    }

    /**
     * <code>showDialog</code> sets this dialog as visble, and brings it to
     * the front.
     */
    public void showDialog() {
        setLocationRelativeTo(null);
        setVisible(true);
        toFront();
    }

    /**
     * <code>init</code> creates the components to use the dialog.
     */
    private void createUI() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            logger.warning("Could not set native look and feel.");
        }

        addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                setUserSelection(CANCEL_SELECTION);
                dispose();
            }
        });

        if (source.getIcons() != null) {
            safeSetIconImages( (List<BufferedImage>) Arrays.asList((BufferedImage[]) source.getIcons()) );
        }

        setTitle("Select Display Settings");

        // The panels...
        JPanel mainPanel = new JPanel();
        JPanel centerPanel = new JPanel();
        JPanel optionsPanel = new JPanel();
        JPanel buttonPanel = new JPanel();
        // The buttons...
        JButton ok = new JButton("Ok");
        JButton cancel = new JButton("Cancel");

        icon = new JLabel(imageFile != null ? new ImageIcon(imageFile) : null);

        mainPanel.setLayout(new BorderLayout());

        centerPanel.setLayout(new BorderLayout());

        KeyListener aListener = new KeyAdapter() {

            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (verifyAndSaveCurrentSelection()) {
                        setUserSelection(APPROVE_SELECTION);
                        dispose();
                    }
                }
                else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    setUserSelection(CANCEL_SELECTION);
                    dispose();
                }
            }
        };

        displayResCombo = setUpResolutionChooser();
        displayResCombo.addKeyListener(aListener);
        colorDepthCombo = new JComboBox();
        colorDepthCombo.addKeyListener(aListener);
        displayFreqCombo = new JComboBox();
        displayFreqCombo.addKeyListener(aListener);
        antialiasCombo = new JComboBox();
        antialiasCombo.addKeyListener(aListener);
        fullscreenBox = new JCheckBox("Fullscreen?");
        fullscreenBox.setSelected(source.isFullscreen());
        fullscreenBox.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                updateResolutionChoices();
            }
        });
        vsyncBox = new JCheckBox("VSync?");
        vsyncBox.setSelected(source.isVSync());
//        rendererCombo = setUpRendererChooser();
//        rendererCombo.addKeyListener(aListener);

       

        updateResolutionChoices();
        updateAntialiasChoices();
        displayResCombo.setSelectedItem(source.getWidth() + " x " + source.getHeight());
        colorDepthCombo.setSelectedItem(source.getBitsPerPixel() + " bpp");

        optionsPanel.add(displayResCombo);
        optionsPanel.add(colorDepthCombo);
        optionsPanel.add(displayFreqCombo);
        optionsPanel.add(antialiasCombo);
        optionsPanel.add(fullscreenBox);
        optionsPanel.add(vsyncBox);
//        optionsPanel.add(rendererCombo);

        // Set the button action listeners. Cancel disposes without saving, OK
        // saves.
        ok.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (verifyAndSaveCurrentSelection()) {
                    setUserSelection(APPROVE_SELECTION);
                    dispose();
                }
            }
        });

        cancel.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                setUserSelection(CANCEL_SELECTION);
                dispose();
            }
        });

        buttonPanel.add(ok);
        buttonPanel.add(cancel);

        if (icon != null) {
            centerPanel.add(icon, BorderLayout.NORTH);
        }
        centerPanel.add(optionsPanel, BorderLayout.SOUTH);

        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        this.getContentPane().add(mainPanel);

        pack();
    }

    /* Access JDialog.setIconImages by reflection in case we're running on JRE < 1.6 */
    private void safeSetIconImages(List<? extends Image> icons) {
        try {
            // Due to Java bug 6445278, we try to set icon on our shared owner frame first.
            // Otherwise, our alt-tab icon will be the Java default under Windows.
            Window owner = getOwner();
            if (owner != null) {
                Method setIconImages = owner.getClass().getMethod("setIconImages", List.class);
                setIconImages.invoke(owner, icons);
                return;
            }

            Method setIconImages = getClass().getMethod("setIconImages", List.class);
            setIconImages.invoke(this, icons);
        } catch (Exception e) {
            return;
        }
    }

    /**
     * <code>verifyAndSaveCurrentSelection</code> first verifies that the
     * display mode is valid for this system, and then saves the current
     * selection as a properties.cfg file.
     * 
     * @return if the selection is valid
     */
    private boolean verifyAndSaveCurrentSelection() {
        String display = (String) displayResCombo.getSelectedItem();
        boolean fullscreen = fullscreenBox.isSelected();
        boolean vsync = vsyncBox.isSelected();

        int width = Integer.parseInt(display.substring(0, display.indexOf(" x ")));
        display = display.substring(display.indexOf(" x ") + 3);
        int height = Integer.parseInt(display);

        String depthString = (String) colorDepthCombo.getSelectedItem();
        int depth = -1;
        if (depthString.equals("???")) {
            depth = 0;
        } else {
            depth = Integer.parseInt(depthString.substring(0, depthString.indexOf(' ')));
        }

        String freqString = (String) displayFreqCombo.getSelectedItem();
        int freq = -1;
        if (fullscreen) {
            if (freqString.equals("???")) {
                freq = 0;
            } else {
                freq = Integer.parseInt(freqString.substring(0, freqString.indexOf(' ')));
            }
        }

        String aaString = (String) antialiasCombo.getSelectedItem();
        int multisample = -1;
        if (aaString.equals("Disabled")) {
            multisample = 0;
        } else {
            multisample = Integer.parseInt(aaString.substring(0, aaString.indexOf('x')));
        }

        // FIXME: Does not work in Linux
        /*
         * if (!fullscreen) { //query the current bit depth of the desktop int
         * curDepth = GraphicsEnvironment.getLocalGraphicsEnvironment()
         * .getDefaultScreenDevice().getDisplayMode().getBitDepth(); if (depth >
         * curDepth) { showError(this,"Cannot choose a higher bit depth in
         * windowed " + "mode than your current desktop bit depth"); return
         * false; } }
         */

        String renderer = "LWJGL-OpenGL2";//(String) rendererCombo.getSelectedItem();

        boolean valid = false;

        // test valid display mode when going full screen
        if (!fullscreen) {
            valid = true;
        } else {
            GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            valid = device.isFullScreenSupported();
        }

        if (valid) {
            //use the GameSettings class to save it.
            source.setWidth(width);
            source.setHeight(height);
            source.setBitsPerPixel(depth);
            source.setFrequency(freq);
            source.setFullscreen(fullscreen);
            source.setVSync(vsync);
            //source.setRenderer(renderer);
            source.setSamples(multisample);

            String appTitle = source.getTitle();

            try {
                source.save(appTitle);
            } catch (BackingStoreException ex) {
                logger.log(Level.WARNING,
                        "Failed to save setting changes", ex);
            }
        } else {
            showError(
                    this,
                    "Your monitor claims to not support the display mode you've selected.\n"
                    + "The combination of bit depth and refresh rate is not supported.");
        }

        return valid;
    }

    /**
     * <code>setUpChooser</code> retrieves all available display modes and
     * places them in a <code>JComboBox</code>. The resolution specified by
     * GameSettings is used as the default value.
     * 
     * @return the combo box of display modes.
     */
    private JComboBox setUpResolutionChooser() {
        String[] res = getResolutions(modes);
        JComboBox resolutionBox = new JComboBox(res);

        resolutionBox.setSelectedItem(source.getWidth() + " x "
                + source.getHeight());
        resolutionBox.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                updateDisplayChoices();
            }
        });

        return resolutionBox;
    }

    /**
     * <code>setUpRendererChooser</code> sets the list of available renderers.
     * Data is obtained from the <code>DisplaySystem</code> class. The
     * renderer specified by GameSettings is used as the default value.
     * 
     * @return the list of renderers.
     */
    private JComboBox setUpRendererChooser() {
        String modes[] = {"NULL", "JOGL-OpenGL1", "LWJGL-OpenGL2", "LWJGL-OpenGL3", "LWJGL-OpenGL3.1"};
        JComboBox nameBox = new JComboBox(modes);
        nameBox.setSelectedItem(source.getRenderer());
        return nameBox;
    }

    /**
     * <code>updateDisplayChoices</code> updates the available color depth and
     * display frequency options to match the currently selected resolution.
     */
    private void updateDisplayChoices() {
        if (!fullscreenBox.isSelected()) {
            // don't run this function when changing windowed settings
            return;
        }
        String resolution = (String) displayResCombo.getSelectedItem();
        String colorDepth = (String) colorDepthCombo.getSelectedItem();
        if (colorDepth == null) {
            colorDepth = source.getBitsPerPixel() + " bpp";
        }
        String displayFreq = (String) displayFreqCombo.getSelectedItem();
        if (displayFreq == null) {
            displayFreq = source.getFrequency() + " Hz";
        }

        // grab available depths
        String[] depths = getDepths(resolution, modes);
        colorDepthCombo.setModel(new DefaultComboBoxModel(depths));
        colorDepthCombo.setSelectedItem(colorDepth);
        // grab available frequencies
        String[] freqs = getFrequencies(resolution, modes);
        displayFreqCombo.setModel(new DefaultComboBoxModel(freqs));
        // Try to reset freq
        displayFreqCombo.setSelectedItem(displayFreq);
    }

    /**
     * <code>updateResolutionChoices</code> updates the available resolutions
     * list to match the currently selected window mode (fullscreen or
     * windowed). It then sets up a list of standard options (if windowed) or
     * calls <code>updateDisplayChoices</code> (if fullscreen).
     */
    private void updateResolutionChoices() {
        if (!fullscreenBox.isSelected()) {
            displayResCombo.setModel(new DefaultComboBoxModel(
                    windowedResolutions));
            colorDepthCombo.setModel(new DefaultComboBoxModel(new String[]{
                        "24 bpp", "16 bpp"}));
            displayFreqCombo.setModel(new DefaultComboBoxModel(
                    new String[]{"n/a"}));
            displayFreqCombo.setEnabled(false);
        } else {
            displayResCombo.setModel(new DefaultComboBoxModel(
                    getResolutions(modes)));
            displayFreqCombo.setEnabled(true);
            updateDisplayChoices();
        }
    }

    private void updateAntialiasChoices() {
        // maybe in the future will add support for determining this info
        // through pbuffer
        String[] choices = new String[]{"Disabled", "2x", "4x", "6x", "8x", "16x"};
        antialiasCombo.setModel(new DefaultComboBoxModel(choices));
        antialiasCombo.setSelectedItem(choices[Math.min(source.getSamples()/2,5)]);
    }

    //
    // Utility methods
    //
    /**
     * Utility method for converting a String denoting a file into a URL.
     * 
     * @return a URL pointing to the file or null
     */
    private static URL getURL(String file) {
        URL url = null;
        try {
            url = new URL("file:" + file);
        } catch (MalformedURLException e) {
        }
        return url;
    }

    private static void showError(java.awt.Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error",
                JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Returns every unique resolution from an array of <code>DisplayMode</code>s.
     */
    private static String[] getResolutions(DisplayMode[] modes) {
        ArrayList<String> resolutions = new ArrayList<String>(modes.length);
        for (int i = 0; i < modes.length; i++) {
            String res = modes[i].getWidth() + " x " + modes[i].getHeight();
            if (!resolutions.contains(res)) {
                resolutions.add(res);
            }
        }

        String[] res = new String[resolutions.size()];
        resolutions.toArray(res);
        return res;
    }

    /**
     * Returns every possible bit depth for the given resolution.
     */
    private static String[] getDepths(String resolution, DisplayMode[] modes) {
        ArrayList<String> depths = new ArrayList<String>(4);
        for (int i = 0; i < modes.length; i++) {
            // Filter out all bit depths lower than 16 - Java incorrectly
            // reports
            // them as valid depths though the monitor does not support them
            if (modes[i].getBitDepth() < 16 && modes[i].getBitDepth() > 0) {
                continue;
            }

            String res = modes[i].getWidth() + " x " + modes[i].getHeight();
            String depth = modes[i].getBitDepth() + " bpp";
            if (res.equals(resolution) && !depths.contains(depth)) {
                depths.add(depth);
            }
        }

        if (depths.size() == 1 && depths.contains("-1 bpp")) {
            // add some default depths, possible system is multi-depth supporting
            depths.clear();
            depths.add("24 bpp");
        }

        String[] res = new String[depths.size()];
        depths.toArray(res);
        return res;
    }

    /**
     * Returns every possible refresh rate for the given resolution.
     */
    private static String[] getFrequencies(String resolution,
            DisplayMode[] modes) {
        ArrayList<String> freqs = new ArrayList<String>(4);
        for (int i = 0; i < modes.length; i++) {
            String res = modes[i].getWidth() + " x " + modes[i].getHeight();
            String freq;
            if (modes[i].getRefreshRate() == DisplayMode.REFRESH_RATE_UNKNOWN) {
                freq = "???";
            } else {
                freq = modes[i].getRefreshRate() + " Hz";
            }

            if (res.equals(resolution) && !freqs.contains(freq)) {
                freqs.add(freq);
            }
        }

        String[] res = new String[freqs.size()];
        freqs.toArray(res);
        return res;
    }

    /**
     * Utility class for sorting <code>DisplayMode</code>s. Sorts by
     * resolution, then bit depth, and then finally refresh rate.
     */
    private class DisplayModeSorter implements Comparator<DisplayMode> {

        /**
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        public int compare(DisplayMode a, DisplayMode b) {
            // Width
            if (a.getWidth() != b.getWidth()) {
                return (a.getWidth() > b.getWidth()) ? 1 : -1;
            }
            // Height
            if (a.getHeight() != b.getHeight()) {
                return (a.getHeight() > b.getHeight()) ? 1 : -1;
            }
            // Bit depth
            if (a.getBitDepth() != b.getBitDepth()) {
                return (a.getBitDepth() > b.getBitDepth()) ? 1 : -1;
            }
            // Refresh rate
            if (a.getRefreshRate() != b.getRefreshRate()) {
                return (a.getRefreshRate() > b.getRefreshRate()) ? 1 : -1;
            }
            // All fields are equal
            return 0;
        }
    }
}
