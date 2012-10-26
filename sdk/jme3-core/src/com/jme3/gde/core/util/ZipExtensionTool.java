/*
 * Copyright (c) 2003-2012 jMonkeyEngine
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
package com.jme3.gde.core.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.Exceptions;
import org.openide.util.Utilities;

/**
 * Tool that extracts zip files specific per platform to the settings directory
 * of the SDK. All zip files have to be in the package defined by packageName
 * and be named like (extensionName)-(os).zip, where (os) can be linux, windows
 * or mac. The suffixes can be changed for extraction so one package can be used
 * for multiple platforms.
 *
 * @author normenhansen
 */
public class ZipExtensionTool {

    static final int BUFFER = 2048;
    private final String settingsFolder = System.getProperty("netbeans.user");
    public String SUFFIX_WIN = "windows";
    public String SUFFIX_LINUX = "linux";
    public String SUFFIX_OSX = "mac";
    private final String packageName;
    private final String extensionName;
    private ProgressHandle progress;

    public ZipExtensionTool(String packageName, String extensionName) {
        this.packageName = "/" + packageName.replace('.', '/');
        this.extensionName = extensionName;
    }

    public void install() {
        ProgressHandle progressHandle = ProgressHandleFactory.createHandle("Installing " + extensionName + " data");
        progressHandle.start();
        if (new File(settingsFolder + File.separator + extensionName).exists()) {
            return;
        }
        if (Utilities.isWindows()) {
            extractToolsJava(packageName + "/" + extensionName + "-" + SUFFIX_WIN + ".zip", settingsFolder + File.separator + extensionName);
        } else if (Utilities.isMac()) {
            extractToolsShell(packageName + "/" + extensionName + "-" + SUFFIX_OSX + ".zip", settingsFolder + File.separator + extensionName);
        } else if (Utilities.isUnix()) {
            extractToolsShell(packageName + "/" + extensionName + "-" + SUFFIX_LINUX + ".zip", settingsFolder + File.separator + extensionName);
        }
        progressHandle.finish();
    }

    private boolean extractToolsShell(String zipPath, String extractionPath) {
        File path = new File(extractionPath);
        if (!path.exists()) {
            path.mkdirs();
        }
        BufferedInputStream in = null;
        BufferedOutputStream out = null;
        URL url = null;
        try {
            String tempFileName = extractionPath + "_tmp.zip";
            System.out.println("ZipPath: " + zipPath);
            url = new URL("nbres:" + zipPath);
            in = new BufferedInputStream(url.openStream());
            out = new BufferedOutputStream(new FileOutputStream(tempFileName));
            int inbyte = in.read();
            while (inbyte != -1) {
                out.write(inbyte);
                inbyte = in.read();
            }
            in.close();
            out.close();
            String[] cmdStrings = new String[]{
                "unzip",
                "-o",
                "-q",
                tempFileName,
                "-d",
                extractionPath
            };
            Process p = Runtime.getRuntime().exec(cmdStrings);
            OutputReader outReader = new OutputReader(p.getInputStream());
            OutputReader errReader = new OutputReader(p.getErrorStream());
            outReader.start();
            errReader.start();
            p.waitFor();
            File zipFile = new File(tempFileName);
            zipFile.delete();
            if (p.exitValue() != 0) {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            NotifyDescriptor.Confirmation msg = new NotifyDescriptor.Confirmation(
                    "Error extracting " + extensionName,
                    NotifyDescriptor.DEFAULT_OPTION,
                    NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(msg);
            return false;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }
        return true;
    }

    private boolean extractToolsJava(String zipPath, String extractionPath) {
        File path = new File(extractionPath);
        if (!path.exists()) {
            path.mkdirs();
        }

        try {
            File scriptsFolderFile = new File(extractionPath);
            if (!scriptsFolderFile.exists()) {
                NotifyDescriptor.Confirmation msg = new NotifyDescriptor.Confirmation(
                        "Error extracting " + extensionName,
                        NotifyDescriptor.DEFAULT_OPTION,
                        NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(msg);
                return false;
            }
            BufferedOutputStream dest = null;
            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(getClass().getResourceAsStream(zipPath)));
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                int count;
                byte data[] = new byte[BUFFER];
                if (entry.getName().contains(".svn") || entry.getName().contains(".DS_Store")) {
                    continue;
                }
                if (entry.isDirectory()) {
                    File dir = new File(extractionPath + File.separator + entry.getName());
                    dir.mkdirs();
                    continue;
                }
                // write the files to the disk
                FileOutputStream fos = new FileOutputStream(extractionPath + File.separator + entry.getName());
                dest = new BufferedOutputStream(fos, BUFFER);
                while ((count = zis.read(data, 0, BUFFER))
                        != -1) {
                    dest.write(data, 0, count);
                }
                dest.flush();
                dest.close();
            }
            zis.close();
        } catch (IOException ex) {
            NotifyDescriptor.Confirmation msg = new NotifyDescriptor.Confirmation(
                    "Error extracting  " + extensionName + ":" + ex.toString(),
                    NotifyDescriptor.DEFAULT_OPTION,
                    NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(msg);
            Exceptions.printStackTrace(ex);
            return false;
        }

        return true;
    }

    /**
     * @param progress the progress to set
     */
    public void setProgress(ProgressHandle progress) {
        this.progress = progress;
    }

    public class OutputReader implements Runnable {

        private Thread thread;
        private BufferedReader in;

        public OutputReader(InputStream in) {
            this.in = new BufferedReader(new InputStreamReader(in));
        }

        public OutputReader(BufferedReader in) {
            this.in = in;
        }

        public void start() {
            thread = new Thread(this);
            thread.start();
        }

        public void run() {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.trim().length() > 0) {
                        if (progress != null) {
                            progress.progress(line);
                        } else {
                            Logger.getLogger(this.getClass().getName()).log(Level.INFO, line);
                        }
                    }
                }
            } catch (Exception e) {
                Exceptions.printStackTrace(e);
            }
        }
    }
}
