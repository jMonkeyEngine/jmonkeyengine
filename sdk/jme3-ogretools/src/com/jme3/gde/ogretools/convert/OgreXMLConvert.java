/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.ogretools.convert;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.netbeans.api.progress.ProgressHandle;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.NotifyDescriptor.Confirmation;
import org.openide.util.Exceptions;
import org.openide.util.Utilities;

/**
 *
 * @author normenhansen
 */
public class OgreXMLConvert {

    static final int BUFFER = 2048;
    public static String osx_path = System.getProperty("netbeans.user") + "/ogretools/";
    public static String windows_path = System.getProperty("netbeans.user") + "\\ogretools\\";
    public static String linux_path = System.getProperty("netbeans.user") + "/.ogretools/";

    public boolean doConvert(OgreXMLConvertOptions options, ProgressHandle handle) {
        if (!checkTools()) {
            return false;
        }
        String[] cmdOptions = getCommandString(options);
        Process proc = null;
        if (!options.isBinaryFile()) {
            handle.progress("Optimizing Mesh / Creating LOD meshes");
            //convert to binary + modify
            try {
                proc = Runtime.getRuntime().exec(cmdOptions);
                OutputReader outReader = new OutputReader(proc.getInputStream());
                outReader.setProgress(handle);
                OutputReader errReader = new OutputReader(proc.getErrorStream());
                errReader.setProgress(handle);
                outReader.start();
                errReader.start();
                try {
                    proc.waitFor();
                } catch (InterruptedException ex) {
                    Exceptions.printStackTrace(ex);
                }
                if (proc.exitValue() != 0) {
                    return false;
                }
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
                cleanUp(options);
                return false;
            }
        }
        handle.progress("Converting Binary Mesh");
        //convert back to xml
        cmdOptions = getBackCommandString(options);
        try {
            proc = Runtime.getRuntime().exec(cmdOptions);
            OutputReader outReader = new OutputReader(proc.getInputStream());
            outReader.setProgress(handle);
            OutputReader errReader = new OutputReader(proc.getErrorStream());
            errReader.setProgress(handle);
            outReader.start();
            errReader.start();
            try {
                proc.waitFor();
            } catch (InterruptedException ex) {
                Exceptions.printStackTrace(ex);
            }
            if (proc.exitValue() != 0) {
                return false;
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
            cleanUp(options);
            return false;
        }
        cleanUp(options);
        return true;
    }

    private void cleanUp(OgreXMLConvertOptions options) {
        if (!options.isBinaryFile()) {
            File file = new File(options.getBinaryFileName());
            if (file.exists()) {
                file.delete();
            }
        }
    }

    private String[] getBackCommandString(OgreXMLConvertOptions options) {
        ArrayList<String> strings = new ArrayList<String>();
        if (Utilities.isWindows()) {
            strings.add(windows_path + "OgreXMLConverter.exe");
            strings.add("-log");
            strings.add(windows_path + "OgreXMLConverter.log");
        } else if (Utilities.isMac()) {
            strings.add(osx_path + "bin/OgreXMLConverter");
            strings.add("-log");
            strings.add(osx_path + "OgreXMLConverter.log");
        } else {
            strings.add(linux_path + "OgreXMLConverter");
            strings.add("-log");
            strings.add(linux_path + "OgreXMLConverter.log");
        }

        strings.add(options.getBinaryFileName());
        strings.add(options.getDestFile());

        return strings.toArray(new String[strings.size()]);
    }

    private String[] getCommandString(OgreXMLConvertOptions options) {
        ArrayList<String> strings = new ArrayList<String>();
        if (Utilities.isWindows()) {
            strings.add(windows_path + "OgreXMLConverter.exe");
            strings.add("-log");
            strings.add(windows_path + "OgreXMLConverter.log");
        } else if (Utilities.isMac()) {
            strings.add(osx_path + "bin/OgreXMLConverter");
            strings.add("-log");
            strings.add(osx_path + "OgreXMLConverter.log");
        } else {
            strings.add(linux_path + "OgreXMLConverter");
            strings.add("-log");
            strings.add(linux_path + "OgreXMLConverter.log");
        }

        strings.add("-gl");

        if (options.isGenerateTangents()) {
            strings.add("-t");
        }
        if (!options.isGenerateEdgeLists()) {
            strings.add("-e");
        }
        if (options.getLodLevels() > 0) {
            strings.add("-l");
            strings.add(options.getLodLevels() + "");
//            strings.add("-v");
//            strings.add(options.getLodValue() + "");
            strings.add("-p");
            strings.add(options.getLodPercent() + "");
//            strings.add("-s");
//            strings.add(options.getLodStrategy());
        }

        strings.add(options.getSourceFile());
        strings.add(options.getBinaryFileName());

        return strings.toArray(new String[strings.size()]);
    }

    private boolean checkTools() {
        if (Utilities.isWindows()) {
            File file = new File(windows_path + "OgreXMLConverter.exe");
            if (!file.exists()) {
                return extractToolsWindows();
            }
        } else if (Utilities.isMac()) {
            File file = new File(osx_path + "bin/OgreXMLConverter");
            if (!file.exists()) {
                return extractToolsOSX();
            }
        } else {
            File file = new File(linux_path + "OgreXMLConverter");
            if (!file.exists()) {
                return extractToolsLinux();
            }
        }
        return true;
    }

    private boolean extractToolsOSX() {
        File path = new File(osx_path);
        if (!path.exists()) {
            path.mkdirs();
        }

        try {
            BufferedInputStream in = new BufferedInputStream(getClass().getResourceAsStream("/com/jme3/gde/ogretools/convert/OgreTools-Mac-Intel.zip"));
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(osx_path + "tools.zip"));
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
                osx_path + "tools.zip",
                "-d",
                osx_path
            };
            Process p = Runtime.getRuntime().exec(cmdStrings);
            OutputReader outReader = new OutputReader(p.getInputStream());
            OutputReader errReader = new OutputReader(p.getErrorStream());
            outReader.start();
            errReader.start();
            p.waitFor();
            File zipFile = new File(osx_path + "tools.zip");
            zipFile.delete();
            if (p.exitValue() != 0) {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Confirmation msg = new NotifyDescriptor.Confirmation(
                    "Error extracting OgreTools!",
                    NotifyDescriptor.DEFAULT_OPTION,
                    NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(msg);
            return false;
        }
        return true;
    }

    private boolean extractToolsWindows() {
        File path = new File(windows_path);
        if (!path.exists()) {
            path.mkdirs();
        }

        try {
            File scriptsFolderFile = new File(windows_path);
            if (!scriptsFolderFile.exists()) {
                Confirmation msg = new NotifyDescriptor.Confirmation(
                        "Error extracting OgreTools!",
                        NotifyDescriptor.DEFAULT_OPTION,
                        NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(msg);
                return false;
            }
            BufferedOutputStream dest = null;
            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(getClass().getResourceAsStream("/com/jme3/gde/ogretools/convert/OgreTools-Windows.zip")));
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                int count;
                byte data[] = new byte[BUFFER];
                if (entry.getName().contains(".svn") || entry.getName().contains(".DS_Store")) {
                    continue;
                }
                if (entry.isDirectory()) {
                    File dir = new File(windows_path + File.separator + entry.getName());
                    dir.mkdirs();
                    continue;
                }
                // write the files to the disk
                FileOutputStream fos = new FileOutputStream(windows_path + File.separator + entry.getName());
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
            Confirmation msg = new NotifyDescriptor.Confirmation(
                    "Error extracting OgreXMLTools:\n" + ex.toString(),
                    NotifyDescriptor.DEFAULT_OPTION,
                    NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(msg);
            Exceptions.printStackTrace(ex);
            return false;
        }

        return true;
    }

    private boolean extractToolsLinux() {
        File path = new File(linux_path);
        if (!path.exists()) {
            path.mkdirs();
        }

        try {
            File scriptsFolderFile = new File(linux_path);
            if (!scriptsFolderFile.exists()) {
                Confirmation msg = new NotifyDescriptor.Confirmation(
                        "Error extracting OgreTools!",
                        NotifyDescriptor.DEFAULT_OPTION,
                        NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(msg);
                return false;
            }
            BufferedOutputStream dest = null;
            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(getClass().getResourceAsStream("/com/jme3/gde/ogretools/convert/OgreTools-Linux.zip")));
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                int count;
                byte data[] = new byte[BUFFER];
                if (entry.getName().contains(".svn") || entry.getName().contains(".DS_Store")) {
                    continue;
                }
                if (entry.isDirectory()) {
                    File dir = new File(linux_path + File.separator + entry.getName());
                    dir.mkdirs();
                    continue;
                }
                // write the files to the disk
                FileOutputStream fos = new FileOutputStream(linux_path + File.separator + entry.getName());
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
            Confirmation msg = new NotifyDescriptor.Confirmation(
                    "Error extracting OgreXMLTools:\n" + ex.toString(),
                    NotifyDescriptor.DEFAULT_OPTION,
                    NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(msg);
            Exceptions.printStackTrace(ex);
            return false;
        }
        return true;
    }
}
