/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.blender;

import com.jme3.math.Vector3f;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.modules.InstalledFileLocator;
import org.openide.util.Exceptions;
import org.openide.util.Utilities;

/**
 *
 * @author normenhansen
 */
public class BlenderTool {

    private static final Logger logger = Logger.getLogger(BlenderTool.class.getName());
    private static boolean running = false;
//    private static AtomicBoolean running = new AtomicBoolean(false);

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
            DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message("Error finding Blender!"));
        }
        return blender;
    }

    public static File getBlenderSettingsFolder() {
        File blender = InstalledFileLocator.getDefault().locate("../blender/2.64", null, false);
        if (blender == null) {
            DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message("Error finding Blender!"));
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
            DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message("Error finding Blender!"));
        }
        return blender;

    }
    
    public static boolean openInBlender(FileObject file){
        String path = file.getPath().replace("/", File.separator);
        return runBlender(path, true);
    }

    public static boolean runBlender(final String options, boolean async) {
        if (running) {
            return false;
        }
        running = true;
        //TODO: wtf, for some reason i cannot access AtomicBoolean..
        final Vector3f v = new Vector3f(0, 0, 0);
        final File exe = getBlenderExecutable();
        if (exe == null) {
            return false;
        }
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
//            outReader.setProgress(handle);
                    OutputReader errReader = new OutputReader(proc.getErrorStream());
//            errReader.setProgress(handle);
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
                    running = false;
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                    v.x = 1;
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
        if(!runBlender(null, true)){
            DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message("Error running Blender."));
        }
    }
}
