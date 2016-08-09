/*
 * Copyright (c) 2009-2016 jMonkeyEngine
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
package jme3test.opencl;

import com.jme3.app.SimpleApplication;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.opencl.*;
import com.jme3.system.AppSettings;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.NiftyEventSubscriber;
import de.lessvoid.nifty.controls.*;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Sebastian Weiss
 */
public class TestContextSwitching extends SimpleApplication implements ScreenController {
    private static final Logger LOG = Logger.getLogger(TestContextSwitching.class.getName());
    
    private Nifty nifty;
    private Label infoLabel;
    private Button applyButton;
    private ListBox<String> platformListBox;
    private ListBox<String> deviceListBox;
    
    private static String selectedPlatform;
    private static String selectedDevice;
    private Context clContext;
    private static List<? extends Platform> availabePlatforms;
    private Buffer testBuffer;
    private boolean bufferCreated;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        new TestContextSwitching().start();
    }

    public TestContextSwitching() {
        AppSettings settings = new AppSettings(true);
        settings.setOpenCLSupport(true);
        settings.setVSync(true);
        settings.setWidth(800);
        settings.setHeight(600);
        settings.setOpenCLPlatformChooser(CustomPlatformChooser.class);
        //settings.setRenderer(AppSettings.JOGL_OPENGL_FORWARD_COMPATIBLE);
        
        setSettings(settings);
        setShowSettings(false);
    }

    @Override
    public void simpleInitApp() {
        
        clContext = null;
        
        NiftyJmeDisplay niftyDisplay = NiftyJmeDisplay.newNiftyJmeDisplay(
                assetManager,
                inputManager,
                audioRenderer,
                guiViewPort);
        nifty = niftyDisplay.getNifty();
        nifty.fromXml("jme3test/opencl/ContextSwitchingScreen.xml", "Screen", this);
        guiViewPort.addProcessor(niftyDisplay);
        inputManager.setCursorVisible(true);
        flyCam.setEnabled(false);
    }

    @Override
    public void simpleUpdate(float tpf) {
        if (applyButton != null) {
            updateInfos();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void bind(Nifty nifty, Screen screen) {
        applyButton = screen.findNiftyControl("ApplyButton", Button.class);
        platformListBox = screen.findNiftyControl("PlatformListBox", ListBox.class);
        deviceListBox = screen.findNiftyControl("DeviceListBox", ListBox.class);
        infoLabel = screen.findNiftyControl("InfoLabel", Label.class);
        
        updateInfos();
        
        platformListBox.clear();
        for (Platform p : availabePlatforms) {
            platformListBox.addItem(p.getName());
        }
        platformListBox.selectItem(selectedPlatform);
        changePlatform(selectedPlatform);
    }
    
    private void updateInfos() {
        
        if (testBuffer == null && clContext != null && !bufferCreated) {
            try {
                testBuffer = clContext.createBuffer(1024).register();
                LOG.info("Test buffer created");
            } catch (OpenCLException ex) {
                LOG.log(Level.SEVERE, "Unable to create buffer", ex);
            }
            bufferCreated = true;
        }
        
        Context c = context.getOpenCLContext();
        if (c == clContext) {
            return;
        }
        clContext = c;
        LOG.info("context changed");
        testBuffer = null;
        bufferCreated = false;
        StringBuilder text = new StringBuilder();
        text.append("Current context:\n");
        text.append("  Platform: ").append(clContext.getDevices().get(0).getPlatform().getName()).append("\n");
        text.append("  Device: ").append(clContext.getDevices().get(0).getName()).append("\n");
        text.append("  Profile: ").append(clContext.getDevices().get(0).getProfile()).append("\n");
        text.append("  Memory: ").append(clContext.getDevices().get(0).getGlobalMemorySize()).append(" B\n");
        text.append("  Compute Units: ").append(clContext.getDevices().get(0).getComputeUnits()).append("\n");
        infoLabel.setText(text.toString());
    }

    @NiftyEventSubscriber(id="ApplyButton")
    public void onButton(String id, ButtonClickedEvent event) {
        LOG.log(Level.INFO, "Change context: platorm={0}, device={1}", new Object[]{selectedPlatform, selectedDevice});
        restart();
    }
    
    private void changePlatform(String platform) {
        selectedPlatform = platform;
        Platform p = null;
        for (Platform p2 : availabePlatforms) {
            if (p2.getName().equals(selectedPlatform)) {
                p = p2;
                break;
            }
        }
        deviceListBox.clear();
        if (p == null) {
            return;
        }
        for (Device d : p.getDevices()) {
            deviceListBox.addItem(d.getName());
        }
        deviceListBox.selectItem(selectedDevice);
    }
    
    @NiftyEventSubscriber(id="PlatformListBox")
    public void onPlatformChanged(String id, ListBoxSelectionChangedEvent<String> event) {
        String p = event.getSelection().isEmpty() ? null : event.getSelection().get(0);
        LOG.log(Level.INFO, "Selected platform changed to {0}", p);
        selectedPlatform = p;
        changePlatform(p);
    }
    
    @NiftyEventSubscriber(id="DeviceListBox")
    public void onDeviceChanged(String id, ListBoxSelectionChangedEvent<String> event) {
        String d = event.getSelection().isEmpty() ? null : event.getSelection().get(0);
        LOG.log(Level.INFO, "Selected device changed to {0}", d);
        selectedDevice = d;
    }
    
    @Override
    public void onStartScreen() {
        
    }

    @Override
    public void onEndScreen() {
        
    }
    
    public static class CustomPlatformChooser implements PlatformChooser {

        public CustomPlatformChooser() {}
        
        @Override
        public List<? extends Device> chooseDevices(List<? extends Platform> platforms) {
            availabePlatforms = platforms;
            
            Platform platform = null;
            for (Platform p : platforms) {
                if (p.getName().equals(selectedPlatform)) {
                    platform = p;
                    break;
                }
            }
            if (platform == null) {
                platform = platforms.get(0);
            }
            selectedPlatform = platform.getName();
            
            Device device = null;
            for (Device d : platform.getDevices()) {
                if (d.getName().equals(selectedDevice)) {
                    device = d;
                    break;
                }
            }
            if (device == null) {
                for (Device d : platform.getDevices()) {
                    if (d.getDeviceType() == Device.DeviceType.GPU) {
                        device = d;
                        break;
                    }
                }
            }
            if (device == null) {
                device = platform.getDevices().get(0);
            }
            selectedDevice = device.getName();
            
            return Collections.singletonList(device);
        }
        
    }
}
