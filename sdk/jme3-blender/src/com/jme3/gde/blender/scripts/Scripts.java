/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.blender.scripts;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;

/**
 *
 * @author normenhansen
 */
public class Scripts {

    private static final Logger logger = Logger.getLogger(Scripts.class.getName());
    private final static String root = "com/jme3/gde/blender/scripts/";

    public static void copyToFolder(FileObject folder) {
        if (folder == null) {
            logger.log(Level.WARNING, "Got null folder for scripts check");
            return;
        }
        checkScript(folder, "import_3ds.py");
        checkScript(folder, "import_dae.py");
        checkScript(folder, "import_fbx.py");
    }

    private static void checkScript(FileObject folder, String name) {
        FileObject file = folder.getFileObject(name);
        //TODO:check version!
        if (file == null) {
            try {
                InputStream in = null;
                OutputStream out = null;
                try {
                    URL url = new URL("nbres:" + root + name);
                    file = FileUtil.createData(folder, name);
                    in = url.openStream();
                    out = file.getOutputStream();
                    FileUtil.copy(in, out);
                } catch (IOException e) {
                    Exceptions.printStackTrace(e);
                } finally {
                    if (in != null) {
                        in.close();
                    }
                    if (out != null) {
                        out.close();
                    }
                }
                logger.log(Level.INFO, "Extracted script {0}", file.getPath());
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }
}
