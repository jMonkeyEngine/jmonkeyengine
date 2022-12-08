/*
 * Copyright (c) 2009-2022 jMonkeyEngine
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
package com.jme3.awt;

import com.jme3.asset.AssetNotFoundException;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeSystem;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import javax.swing.*;

/**
 * <code>SettingsDialog</code> displays a Swing dialog box to interactively
 * configure the <code>AppSettings</code> of a desktop application before
 * <code>start()</code> is invoked.
 *
 * The <code>AppSettings</code> instance to be configured is passed to the
 * constructor.
 *
 * @see AppSettings
 * @author Mark Powell
 * @author Eric Woroshow
 * @author Joshua Slack - reworked for proper use of GL commands.
 */
public final class AWTSettingsDialog extends JFrame {

    public static interface SelectionListener {

        public void onSelection(int selection);
    }

    private static final Logger logger = Logger.getLogger(AWTSettingsDialog.class.getName());
    private static final long serialVersionUID = 1L;
    public static final int NO_SELECTION = 0, APPROVE_SELECTION = 1, CANCEL_SELECTION = 2;

    // Resource bundle for i18n.
    ResourceBundle resourceBundle = ResourceBundle.getBundle("com.jme3.app/SettingsDialog");

    // the instance being configured
    private final AppSettings source;

    // Title Image
    private URL imageFile = null;
    // Array of supported display modes
    private DisplayMode[] modes = null;
    private static final DisplayMode[] windowDefaults = new DisplayMode[] {
        new DisplayMode(1024, 768, 24, 60),
        new DisplayMode(1280, 720, 24, 60),
        new DisplayMode(1280, 1024, 24, 60),
        new DisplayMode(1440, 900, 24, 60),
        new DisplayMode(1680, 1050, 24, 60),
    };
    private DisplayMode[] windowModes = null;

    // UI components
    private JCheckBox vsyncBox = null;
    private JCheckBox gammaBox = null;
    private JCheckBox fullscreenBox = null;
    private JComboBox<String> displayResCombo = null;
    private JComboBox<String> colorDepthCombo = null;
    private JComboBox<String> displayFreqCombo = null;
    private JComboBox<String> antialiasCombo = null;
    private JLabel icon = null;
    private int selection = 0;
    private SelectionListener selectionListener = null;

    private int minWidth = 0;
    private int minHeight = 0;

    public static boolean showDialog(AppSettings sourceSettings) {
        return showDialog(sourceSettings, true);
    }

    public static boolean showDialog(AppSettings sourceSettings, boolean loadSettings) {
        String iconPath = sourceSettings.getSettingsDialogImage();
        final URL iconUrl = JmeSystem.class.getResource(iconPath.startsWith("/") ? iconPath : "/" + iconPath);
        if (iconUrl == null) {
            throw new AssetNotFoundException(sourceSettings.getSettingsDialogImage());
        }
        return showDialog(sourceSettings, iconUrl, loadSettings);
    }

    public static boolean showDialog(AppSettings sourceSettings, String imageFile, boolean loadSettings) {
        return showDialog(sourceSettings, getURL(imageFile), loadSettings);
    }

    public static boolean showDialog(AppSettings sourceSettings, URL imageFile, boolean loadSettings) {
        if (SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Cannot run from EDT");
        }
        if (GraphicsEnvironment.isHeadless()) {
            throw new IllegalStateException("Cannot show dialog in headless environment");
        }

        AppSettings settings = new AppSettings(false);
        settings.copyFrom(sourceSettings);

        Object lock = new Object();
        AtomicBoolean done = new AtomicBoolean();
        AtomicInteger result = new AtomicInteger();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                final SelectionListener selectionListener = new SelectionListener() {
                    @Override
                    public void onSelection(int selection) {
                        synchronized (lock) {
                            done.set(true);
                            result.set(selection);
                            lock.notifyAll();
                        }
                    }
                };
                AWTSettingsDialog dialog = new AWTSettingsDialog(settings, imageFile, loadSettings);
                dialog.setSelectionListener(selectionListener);
                dialog.showDialog();
            }
        });
        synchronized (lock) {
            while (!done.get()) {
                try {
                    lock.wait();
                } catch (InterruptedException ex) {
                }
            }
        }

        sourceSettings.copyFrom(settings);

        return result.get() == AWTSettingsDialog.APPROVE_SELECTION;
    }

    /**
     * Instantiate a <code>SettingsDialog</code> for the primary display.
     *
     * @param source
     *            the <code>AppSettings</code> (not null)
     * @param imageFile
     *            the image file to use as the title of the dialog;
     *            <code>null</code> will result in to image being displayed
     * @param loadSettings
     *            if true, copy the settings, otherwise merge them
     * @throws IllegalArgumentException
     *             if the source is <code>null</code>
     */
    protected AWTSettingsDialog(AppSettings source, String imageFile, boolean loadSettings) {
        this(source, getURL(imageFile), loadSettings);
    }

    /**
     * /** Instantiate a <code>SettingsDialog</code> for the primary display.
     *
     * @param source
     *            the <code>AppSettings</code> object (not null)
     * @param imageFile
     *            the image file to use as the title of the dialog;
     *            <code>null</code> will result in to image being displayed
     * @param loadSettings
     *            if true, copy the settings, otherwise merge them
     * @throws IllegalArgumentException
     *             if the source is <code>null</code>
     */
    protected AWTSettingsDialog(AppSettings source, URL imageFile, boolean loadSettings) {
        if (source == null) {
            throw new IllegalArgumentException("Settings source cannot be null");
        }

        this.source = source;
        this.imageFile = imageFile;

        // setModal(true);
        setAlwaysOnTop(true);
        setResizable(false);

        AppSettings registrySettings = new AppSettings(true);

        String appTitle;
        if (source.getTitle() != null) {
            appTitle = source.getTitle();
        } else {
            appTitle = registrySettings.getTitle();
        }

        minWidth = source.getMinWidth();
        minHeight = source.getMinHeight();

        try {
            registrySettings.load(appTitle);
        } catch (BackingStoreException ex) {
            logger.log(Level.WARNING, "Failed to load settings", ex);
        }

        if (loadSettings) {
            source.copyFrom(registrySettings);
        } else if (!registrySettings.isEmpty()) {
            source.mergeFrom(registrySettings);
        }

        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

        modes = device.getDisplayModes();
        Arrays.sort(modes, new DisplayModeSorter());

        DisplayMode[] merged = new DisplayMode[modes.length + windowDefaults.length];

        int wdIndex = 0;
        int dmIndex = 0;
        int mergedIndex;

        for (mergedIndex = 0; mergedIndex < merged.length && (wdIndex < windowDefaults.length || dmIndex < modes.length); mergedIndex++) {

            if (dmIndex >= modes.length) {
                merged[mergedIndex] = windowDefaults[wdIndex++];
            } else if (wdIndex >= windowDefaults.length) {
                merged[mergedIndex] = modes[dmIndex++];
            } else if (modes[dmIndex].getWidth() < windowDefaults[wdIndex].getWidth()) {
                merged[mergedIndex] = modes[dmIndex++];
            } else if (modes[dmIndex].getWidth() == windowDefaults[wdIndex].getWidth()) {
                if (modes[dmIndex].getHeight() < windowDefaults[wdIndex].getHeight()) {
                    merged[mergedIndex] = modes[dmIndex++];
                } else if (modes[dmIndex].getHeight() == windowDefaults[wdIndex].getHeight()) {
                    merged[mergedIndex] = modes[dmIndex++];
                    wdIndex++;
                } else {
                    merged[mergedIndex] = windowDefaults[wdIndex++];
                }
            } else {
                merged[mergedIndex] = windowDefaults[wdIndex++];
            }
        }

        if (merged.length == mergedIndex) {
            windowModes = merged;
        } else {
            windowModes = Arrays.copyOfRange(merged, 0, mergedIndex);
        }

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

    public int getMinWidth() {
        return minWidth;
    }

    public void setMinWidth(int minWidth) {
        this.minWidth = minWidth;
    }

    public int getMinHeight() {
        return minHeight;
    }

    public void setMinHeight(int minHeight) {
        this.minHeight = minHeight;
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
        } catch (MalformedURLException e) {
            logger.log(Level.WARNING, "Couldn’t read from file '" + image + "'", e);
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
        pack(); // Resize to accommodate the new image
        setLocationRelativeTo(null); // put in center
    }

    /**
     * <code>showDialog</code> sets this dialog as visible, and brings it to the
     * front.
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
        GridBagConstraints gbc;

        JPanel mainPanel = new JPanel(new GridBagLayout());

        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                setUserSelection(CANCEL_SELECTION);
                dispose();
            }
        });

        if (source.getIcons() != null) {
            safeSetIconImages(Arrays.asList((BufferedImage[]) source.getIcons()));
        }

        setTitle(MessageFormat.format(resourceBundle.getString("frame.title"), source.getTitle()));

        // The buttons...
        JButton ok = new JButton(resourceBundle.getString("button.ok"));
        JButton cancel = new JButton(resourceBundle.getString("button.cancel"));

        icon = new JLabel(imageFile != null ? new ImageIcon(imageFile) : null);

        KeyListener aListener = new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (verifyAndSaveCurrentSelection()) {
                        setUserSelection(APPROVE_SELECTION);
                        dispose();
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    setUserSelection(CANCEL_SELECTION);
                    dispose();
                }
            }
        };

        displayResCombo = setUpResolutionChooser();
        displayResCombo.addKeyListener(aListener);
        colorDepthCombo = new JComboBox<>();
        colorDepthCombo.addKeyListener(aListener);
        displayFreqCombo = new JComboBox<>();
        displayFreqCombo.addKeyListener(aListener);
        antialiasCombo = new JComboBox<>();
        antialiasCombo.addKeyListener(aListener);
        fullscreenBox = new JCheckBox(resourceBundle.getString("checkbox.fullscreen"));
        fullscreenBox.setSelected(source.isFullscreen());
        fullscreenBox.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                updateResolutionChoices();
            }
        });
        vsyncBox = new JCheckBox(resourceBundle.getString("checkbox.vsync"));
        vsyncBox.setSelected(source.isVSync());

        gammaBox = new JCheckBox(resourceBundle.getString("checkbox.gamma"));
        gammaBox.setSelected(source.isGammaCorrection());

        gbc = new GridBagConstraints();
        gbc.weightx = 0.5;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.EAST;
        mainPanel.add(fullscreenBox, gbc);
        gbc = new GridBagConstraints();
        gbc.weightx = 0.5;
        // gbc.insets = new Insets(4, 16, 0, 4);
        gbc.gridx = 2;
        // gbc.gridwidth = 2;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.EAST;
        mainPanel.add(vsyncBox, gbc);
        gbc = new GridBagConstraints();
        gbc.weightx = 0.5;
        gbc.gridx = 3;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        mainPanel.add(gammaBox, gbc);

        gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.weightx = 0.5;
        mainPanel.add(new JLabel(resourceBundle.getString("label.resolutions")), gbc);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.WEST;
        mainPanel.add(displayResCombo, gbc);
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 16, 4, 4);
        gbc.gridx = 2;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.EAST;
        mainPanel.add(new JLabel(resourceBundle.getString("label.colordepth")), gbc);
        gbc = new GridBagConstraints();
        gbc.weightx = 0.5;
        gbc.gridx = 3;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.WEST;
        mainPanel.add(colorDepthCombo, gbc);
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.weightx = 0.5;
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.EAST;
        mainPanel.add(new JLabel(resourceBundle.getString("label.refresh")), gbc);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.WEST;
        mainPanel.add(displayFreqCombo, gbc);
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 16, 4, 4);
        gbc.gridx = 2;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.EAST;
        mainPanel.add(new JLabel(resourceBundle.getString("label.antialias")), gbc);
        gbc = new GridBagConstraints();
        gbc.weightx = 0.5;
        gbc.gridx = 3;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.WEST;
        mainPanel.add(antialiasCombo, gbc);

        // Set the button action listeners. Cancel disposes without saving, OK
        // saves.
        ok.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (verifyAndSaveCurrentSelection()) {
                    setUserSelection(APPROVE_SELECTION);
                    dispose();

                    // System.gc() should be called to prevent "X Error of
                    // failed request: RenderBadPicture (invalid Picture
                    // parameter)"
                    // on Linux when using AWT/Swing + GLFW.
                    // For more info see:
                    // https://github.com/LWJGL/lwjgl3/issues/149,
                    // https://hub.jmonkeyengine.org/t/experimenting-lwjgl3/37275
                    System.gc();
                    System.gc();
                }
            }
        });

        cancel.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                setUserSelection(CANCEL_SELECTION);
                dispose();
            }
        });

        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.gridy = 4;
        gbc.anchor = GridBagConstraints.EAST;
        mainPanel.add(ok, gbc);
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 16, 4, 4);
        gbc.gridx = 2;
        gbc.gridwidth = 2;
        gbc.gridy = 4;
        gbc.anchor = GridBagConstraints.WEST;
        mainPanel.add(cancel, gbc);

        if (icon != null) {
            gbc = new GridBagConstraints();
            gbc.gridwidth = 4;
            mainPanel.add(icon, gbc);
        }

        this.getContentPane().add(mainPanel);

        pack();

        mainPanel.getRootPane().setDefaultButton(ok);
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                // Fill in the combos once the window has opened so that the
                // insets can be read.
                // The assumption is made that the settings window and the
                // display window will have the
                // same insets as that is used to resize the "full screen
                // windowed" mode appropriately.
                updateResolutionChoices();
                if (source.getWidth() != 0 && source.getHeight() != 0) {
                    displayResCombo.setSelectedItem(source.getWidth() + " x " + source.getHeight());
                } else {
                    displayResCombo.setSelectedIndex(displayResCombo.getItemCount() - 1);
                }

                updateAntialiasChoices();
                colorDepthCombo.setSelectedItem(source.getBitsPerPixel() + " bpp");
            }
        });

    }

    /*
     * Access JDialog.setIconImages by reflection in case we're running on JRE <
     * 1.6
     */
    private void safeSetIconImages(List<? extends Image> icons) {
        try {
            // Due to Java bug 6445278, we try to set icon on our shared owner
            // frame first.
            // Otherwise, our alt-tab icon will be the Java default under
            // Windows.
            Window owner = getOwner();
            if (owner != null) {
                Method setIconImages = owner.getClass().getMethod("setIconImages", List.class);
                setIconImages.invoke(owner, icons);
                return;
            }

            Method setIconImages = getClass().getMethod("setIconImages", List.class);
            setIconImages.invoke(this, icons);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error setting icon images", e);
        }
    }

    /**
     * <code>verifyAndSaveCurrentSelection</code> first verifies that the
     * display mode is valid for this system, and then saves the current
     * selection to the backing store.
     * 
     * @return if the selection is valid
     */
    private boolean verifyAndSaveCurrentSelection() {
        String display = (String) displayResCombo.getSelectedItem();
        boolean fullscreen = fullscreenBox.isSelected();
        boolean vsync = vsyncBox.isSelected();
        boolean gamma = gammaBox.isSelected();

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
        if (aaString.equals(resourceBundle.getString("antialias.disabled"))) {
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

        boolean valid = false;

        // test valid display mode when going full screen
        if (!fullscreen) {
            valid = true;
        } else {
            GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            valid = device.isFullScreenSupported();
        }

        if (valid) {
            // use the AppSettings class to save it to backing store
            source.setWidth(width);
            source.setHeight(height);
            source.setBitsPerPixel(depth);
            source.setFrequency(freq);
            source.setFullscreen(fullscreen);
            source.setVSync(vsync);
            source.setGammaCorrection(gamma);
            // source.setRenderer(renderer);
            source.setSamples(multisample);

            String appTitle = source.getTitle();

            try {
                source.save(appTitle);
            } catch (BackingStoreException ex) {
                logger.log(Level.WARNING, "Failed to save setting changes", ex);
            }
        } else {
            showError(this, resourceBundle.getString("error.unsupportedmode"));
        }

        return valid;
    }

    /**
     * <code>setUpChooser</code> retrieves all available display modes and
     * places them in a <code>JComboBox</code>. The resolution specified by
     * AppSettings is used as the default value.
     * 
     * @return the combo box of display modes.
     */
    private JComboBox<String> setUpResolutionChooser() {
        JComboBox<String> resolutionBox = new JComboBox<>();

        resolutionBox.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                updateDisplayChoices();
            }
        });

        return resolutionBox;
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
        colorDepthCombo.setModel(new DefaultComboBoxModel<>(depths));
        colorDepthCombo.setSelectedItem(colorDepth);
        // grab available frequencies
        String[] freqs = getFrequencies(resolution, modes);
        displayFreqCombo.setModel(new DefaultComboBoxModel<>(freqs));
        // Try to reset freq
        displayFreqCombo.setSelectedItem(displayFreq);

        if (!displayFreqCombo.getSelectedItem().equals(displayFreq)) {
            // Cannot find saved frequency in available frequencies.
            // Choose the closest one to 60 Hz.
            displayFreqCombo.setSelectedItem(getBestFrequency(resolution, modes));
        }
    }

    /**
     * <code>updateResolutionChoices</code> updates the available resolutions
     * list to match the currently selected window mode (fullscreen or
     * windowed). It then sets up a list of standard options (if windowed) or
     * calls <code>updateDisplayChoices</code> (if fullscreen).
     */
    private void updateResolutionChoices() {
        if (!fullscreenBox.isSelected()) {
            displayResCombo.setModel(new DefaultComboBoxModel<>(getWindowedResolutions(windowModes)));
            if (displayResCombo.getItemCount() > 0) {
                displayResCombo.setSelectedIndex(displayResCombo.getItemCount() - 1);
            }
            colorDepthCombo.setModel(new DefaultComboBoxModel<>(new String[] { "24 bpp", "16 bpp" }));
            displayFreqCombo.setModel(new DefaultComboBoxModel<>(new String[] { resourceBundle.getString("refresh.na") }));
            displayFreqCombo.setEnabled(false);
        } else {
            displayResCombo.setModel(new DefaultComboBoxModel<>(getResolutions(modes, Integer.MAX_VALUE, Integer.MAX_VALUE)));
            if (displayResCombo.getItemCount() > 0) {
                displayResCombo.setSelectedIndex(displayResCombo.getItemCount() - 1);
            }
            displayFreqCombo.setEnabled(true);
            updateDisplayChoices();
        }
    }

    private void updateAntialiasChoices() {
        // maybe in the future will add support for determining this info
        // through PBuffer
        String[] choices = new String[] { resourceBundle.getString("antialias.disabled"), "2x", "4x", "6x", "8x", "16x" };
        antialiasCombo.setModel(new DefaultComboBoxModel<>(choices));
        antialiasCombo.setSelectedItem(choices[Math.min(source.getSamples() / 2, 5)]);
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
            logger.log(Level.WARNING, "Invalid file name '" + file + "'", e);
        }
        return url;
    }

    private static void showError(java.awt.Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Returns every unique resolution from an array of
     * <code>DisplayMode</code>s where the resolution is greater than the
     * configured minimums.
     */
    private String[] getResolutions(DisplayMode[] modes, int heightLimit, int widthLimit) {
        Insets insets = getInsets();
        heightLimit -= insets.top + insets.bottom;
        widthLimit -= insets.left + insets.right;

        Set<String> resolutions = new LinkedHashSet<>(modes.length);
        for (DisplayMode mode : modes) {
            int height = mode.getHeight();
            int width = mode.getWidth();
            if (width >= minWidth && height >= minHeight) {
                if (height >= heightLimit) {
                    height = heightLimit;
                }
                if (width >= widthLimit) {
                    width = widthLimit;
                }

                String res = width + " x " + height;
                resolutions.add(res);
            }
        }

        return resolutions.toArray(new String[0]);
    }

    /**
     * Returns every unique resolution from an array of
     * <code>DisplayMode</code>s where the resolution is greater than the
     * configured minimums and the height is less than the current screen
     * resolution.
     */
    private String[] getWindowedResolutions(DisplayMode[] modes) {
        int maxHeight = 0;
        int maxWidth = 0;

        for (DisplayMode mode : modes) {
            if (maxHeight < mode.getHeight()) {
                maxHeight = mode.getHeight();
            }
            if (maxWidth < mode.getWidth()) {
                maxWidth = mode.getWidth();
            }
        }

        return getResolutions(modes, maxHeight, maxWidth);
    }

    /**
     * Returns every possible bit depth for the given resolution.
     */
    private static String[] getDepths(String resolution, DisplayMode[] modes) {
        List<String> depths = new ArrayList<>(4);
        for (DisplayMode mode : modes) {
            int bitDepth = mode.getBitDepth();
            if (bitDepth == DisplayMode.BIT_DEPTH_MULTI) {
                continue;
            }
            // Filter out all bit depths lower than 16 - Java incorrectly
            // reports
            // them as valid depths though the monitor does not support them
            if (bitDepth < 16 && bitDepth > 0) {
                continue;
            }
            String res = mode.getWidth() + " x " + mode.getHeight();
            if (!res.equals(resolution)) {
                continue;
            }
            String depth = bitDepth + " bpp";
            if (!depths.contains(depth)) {
                depths.add(depth);
            }
        }

        if (depths.isEmpty()) {
            // add some default depths, possible system is multi-depth
            // supporting
            depths.add("24 bpp");
        }

        return depths.toArray(new String[0]);
    }

    /**
     * Returns every possible refresh rate for the given resolution.
     */
    private static String[] getFrequencies(String resolution, DisplayMode[] modes) {
        List<String> freqs = new ArrayList<>(4);
        for (DisplayMode mode : modes) {
            String res = mode.getWidth() + " x " + mode.getHeight();
            String freq;
            if (mode.getRefreshRate() == DisplayMode.REFRESH_RATE_UNKNOWN) {
                freq = "???";
            } else {
                freq = mode.getRefreshRate() + " Hz";
            }
            if (res.equals(resolution) && !freqs.contains(freq)) {
                freqs.add(freq);
            }
        }

        return freqs.toArray(new String[0]);
    }

    /**
     * Chooses the closest frequency to 60 Hz.
     * 
     * @param resolution
     * @param modes
     * @return
     */
    private static String getBestFrequency(String resolution, DisplayMode[] modes) {
        int closest = Integer.MAX_VALUE;
        int desired = 60;
        for (DisplayMode mode : modes) {
            String res = mode.getWidth() + " x " + mode.getHeight();
            int freq = mode.getRefreshRate();
            if (freq != DisplayMode.REFRESH_RATE_UNKNOWN && res.equals(resolution)) {
                if (Math.abs(freq - desired) < Math.abs(closest - desired)) {
                    closest = mode.getRefreshRate();
                }
            }
        }

        if (closest != Integer.MAX_VALUE) {
            return closest + " Hz";
        } else {
            return null;
        }
    }

    /**
     * Utility class for sorting <code>DisplayMode</code>s. Sorts by resolution,
     * then bit depth, and then finally refresh rate.
     */
    private class DisplayModeSorter implements Comparator<DisplayMode> {

        /**
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        @Override
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
