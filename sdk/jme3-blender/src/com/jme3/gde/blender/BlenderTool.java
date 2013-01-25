/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.blender;

import com.jme3.math.Vector3f;
import java.awt.Frame;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.modules.InstalledFileLocator;
import org.openide.util.Exceptions;
import org.openide.util.Utilities;
import org.openide.windows.WindowManager;

/**
 *
 * @author normenhansen
 */
public class BlenderTool {

    private static final Logger logger = Logger.getLogger(BlenderTool.class.getName());
    private static boolean running = false;
    private static Window blenderWindow = null;

    private static String getBlenderExeName() {
        if (Utilities.isWindows()) {
            return "blender.exe";
        } else {
            return "blender";
        }
    }

    public static File getBlenderExecutable() {
        File blender = InstalledFileLocator.getDefault().locate("../blender/" + getBlenderExeName(), null, false);
        if (blender == null) {
            DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message("Error finding Blender executable at\n" + blender.getPath()));
            logger.log(Level.SEVERE, "Error finding Blender executable at {0}", blender.getPath());
        }
        return blender;
    }

    public static File getBlenderSettingsFolder() {
        File blender = InstalledFileLocator.getDefault().locate("../blender/2.64", null, false);
        if (blender == null) {
            DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message("Error finding Blender settings at\n" + blender.getPath()));
            logger.log(Level.SEVERE, "Error finding Blender settings at {0}", blender.getPath());
        }
        return blender;
    }

    public static File getBlenderRootFolder() {
//        File appFolder = InstalledFileLocator.getDefault().locate("bin", null, false).getParentFile().getParentFile();
//        if (appFolder != null) {
//            DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message(appFolder.toString()));
//        }
        File blender = InstalledFileLocator.getDefault().locate("../blender", null, false);
        if (blender == null) {
            DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message("Error finding Blender root folder at\n" + blender.getPath()));
            logger.log(Level.SEVERE, "Error finding Blender root folder at {0}", blender.getPath());
        }
        return blender;

    }

    public static boolean openInBlender(FileObject file) {
        String path = file.getPath().replace("/", File.separator);
        return runBlender(path, true);
    }

    private static void setBlendWin(Window win) {
        blenderWindow = win;
    }

    public static boolean blenderToFront() {
        Window win = blenderWindow;
        if (win != null) {
            logger.log(Level.INFO, "Request focus of Blender window {0}", win);
            win.requestFocus();
            return true;
        }
        return false;
    }

    public static boolean runBlender(final String options, boolean async) {
        logger.log(Level.INFO, "Try running blender with options {0}", options);
        if (running) {
            logger.log(Level.INFO, "Blender seems to be running");
            return false;
        }
        blenderWindow = null;
        running = true;
        //TODO: wtf, for some reason i cannot access AtomicBoolean..
        final Vector3f v = new Vector3f(0, 0, 0);
        final File exe = getBlenderExecutable();
        if (exe == null) {
            logger.log(Level.SEVERE, "Could not find blender executable!");
            running = false;
            return false;
        }
        final Frame mainWin = WindowManager.getDefault().getMainWindow();
        assert (mainWin != null);
        logger.log(Level.INFO, "Adding focus listener to window {0}", mainWin);
//        mainWin.addWindowFocusListener(new WindowFocusListener() {
//            public void windowGainedFocus(WindowEvent e) {
//            }
//
//            public void windowLostFocus(WindowEvent e) {
//                Window blendWin = e.getOppositeWindow();
//                logger.log(Level.INFO, "Lost focus to window {0}, use as Blender window", blendWin);
//                setBlendWin(blendWin);
//                mainWin.removeWindowFocusListener(this);
//                logger.log(Level.INFO, "Remove focus listener from window {0}", mainWin);
//            }
//        });
        mainWin.setState(Frame.ICONIFIED);
        Runnable r = new Runnable() {
            public void run() {
                try {
                    String command = null;
                    if (options != null) {
                        command = exe.getAbsolutePath() + " " + options;
                    } else {
                        command = exe.getAbsolutePath();
                    }
                    Process proc = Runtime.getRuntime().exec(command);
                    OutputReader outReader = new OutputReader(proc.getInputStream());
                    OutputReader errReader = new OutputReader(proc.getErrorStream());
                    outReader.start();
                    errReader.start();
                    try {
                        proc.waitFor();
                    } catch (InterruptedException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                    if (proc.exitValue() != 0) {
                        v.x = 1;
                    }
                    java.awt.EventQueue.invokeLater(new Runnable() {
                        public void run() {
                            mainWin.setState(Frame.NORMAL);
                        }
                    });
                    blenderWindow = null;
                    running = false;
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                    v.x = 1;
                    java.awt.EventQueue.invokeLater(new Runnable() {
                        public void run() {
                            mainWin.setState(Frame.NORMAL);
                        }
                    });
                    blenderWindow = null;
                    running = false;
                }
            }
        };
        if (async) {
            new Thread(r).start();
        } else {
            r.run();
        }
        if (v.x != 1) {
            return true;
        } else {
            return false;
        }
    }

    public static void runBlender() {
        if (!runBlender(null, true)) {
            logger.log(Level.INFO, "Could not run blender, already running? Trying to focus window.");
            if (!blenderToFront()) {
                logger.log(Level.INFO, "Could not bring blender to front.");
            } else {
                logger.log(Level.INFO, "Requested Blender window focus.");
            }
        }
    }
}
