/*
 *  Copyright (c) 2009-2010 jMonkeyEngine
 *  All rights reserved.
 * 
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are
 *  met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 
 *  * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *  TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *  PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 *  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.gde.core.j2seproject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ant.AntBuildExtender;
import org.netbeans.api.project.libraries.LibraryManager;
import org.netbeans.modules.java.j2seproject.J2SEProject;
import org.netbeans.modules.java.j2seproject.J2SEProjectUtil;
import org.netbeans.modules.java.j2seproject.api.J2SEProjectConfigurations;
import org.netbeans.spi.project.support.ant.EditableProperties;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;

/**
 * A ProjectExtensionManager allows extending jMonkeyPlatform projects with ant
 * tasks.
 *
 * @author normenhansen
 */
public class ProjectExtensionManager {

    private static final Logger logger = Logger.getLogger(ProjectExtensionManager.class.getName());
    private String extensionName;
    private String extensionVersion;
    private String extensionTargets;
    private String[] extensionDependencies;
    private String antTaskLibrary;
    private URL zipFile;
    private static final String resourcesFolder = "resources";

    /**
     * Allows extending ant based projects
     *
     * @param extensionName Name of the extension
     * @param extensionVersion Version of the extension (impl file is recreated
     * when this changes)
     * @param extensionTargets String that contains the whole target part of the
     * xxx-impl.xml
     * @param extensionDependencies String array with targets and dependencies,
     * has to be multiple of two. First item target, second item dependency
     * third target etc.
     * @param antTaskLibrary Name of a library that is added to the project for
     * loading in the ant task, accessible via ${libs.MyLibraryName.classpath}.
     */
    public ProjectExtensionManager(String extensionName, String extensionVersion, String extensionTargets, String[] extensionDependencies, String antTaskLibrary) {
        this.extensionName = extensionName;
        this.extensionVersion = extensionVersion;
        this.extensionTargets = extensionTargets;
        this.extensionDependencies = extensionDependencies;
    }

    public ProjectExtensionManager(String extensionName, String extensionVersion, String extensionTargets, String[] extensionDependencies) {
        this.extensionName = extensionName;
        this.extensionVersion = extensionVersion;
        this.extensionTargets = extensionTargets;
        this.extensionDependencies = extensionDependencies;
    }

    public ProjectExtensionManager(String extensionName, String extensionVersion, String[] extensionDependencies) {
        this.extensionName = extensionName;
        this.extensionVersion = extensionVersion;
        this.extensionDependencies = extensionDependencies;
    }

    public ProjectExtensionManager(String extensionName, String extensionVersion, String extensionTargets) {
        this.extensionName = extensionName;
        this.extensionVersion = extensionVersion;
        this.extensionTargets = extensionTargets;
    }

    public ProjectExtensionManager(String extensionName, String extensionVersion) {
        this.extensionName = extensionName;
        this.extensionVersion = extensionVersion;
    }

    /**
     * Checks if the extension exists and creates it if not
     *
     * @param proj
     */
    public void checkExtension(Project proj) {
        logger.log(Level.FINE, "Checking extension..");
        if (!(proj instanceof J2SEProject)) {
            logger.log(Level.WARNING, "Trying to load Assets Properties from non-asset project");
            return;
        }

        FileObject projDir = proj.getProjectDirectory();
        final FileObject buildXmlFO = J2SEProjectUtil.getBuildXml((J2SEProject) proj);
        if (buildXmlFO == null) {
            logger.log(Level.WARNING, "The project build script does not exist, the project cannot be extended by jMP.");
            return;
        }
        FileObject assetsBuildFile = getImplFile(projDir, true);
        AntBuildExtender extender = proj.getLookup().lookup(AntBuildExtender.class);
        if (extender != null) {
            assert assetsBuildFile != null;
            if (extender.getExtension(extensionName) == null) {
                logger.log(Level.FINE, "Creating extension {0}", extensionName);
                AntBuildExtender.Extension ext = extender.addExtension(extensionName, assetsBuildFile);
                if (extensionDependencies != null) {
                    for (int i = 0; i < extensionDependencies.length; i += 2) {
                        String target = extensionDependencies[i];
                        String extension = extensionDependencies[i + 1];
                        ext.addDependency(target, extension);
                    }
                }
//                try {
//                    ProjectManager.getDefault().saveProject(proj);
//                } catch (IOException ex) {
//                    Exceptions.printStackTrace(ex);
//                }
                addAntTaskLibrary(proj, antTaskLibrary);
                try {
                    addZipContents(proj.getProjectDirectory());
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        } else {
            logger.log(Level.WARNING, "Trying to include assets build snippet in project type that doesn't support AntBuildExtender API contract.");
        }
    }

    /**
     * Removes the extension including all files and libraries
     *
     * @param proj
     */
    public void removeExtension(Project proj) {
        if (!(proj instanceof J2SEProject)) {
            logger.log(Level.WARNING, "Trying to load Assets Properties from non-asset project");
            return;
        }

        FileObject projDir = proj.getProjectDirectory();
        final FileObject buildXmlFO = J2SEProjectUtil.getBuildXml((J2SEProject) proj);
        if (buildXmlFO == null) {
            logger.log(Level.WARNING, "The project build script does not exist, the project cannot be extended by jMP.");
            return;
        }
        AntBuildExtender extender = proj.getLookup().lookup(AntBuildExtender.class);
        if (extender != null) {
            if (extender.getExtension(extensionName) != null) {
                logger.log(Level.FINE, "Removing extension {0}", extensionName);
                extender.removeExtension(extensionName);
                try {
                    FileObject assetsBuildFile = getImplFile(projDir, false);
                    if (assetsBuildFile != null) {
                        logger.log(Level.FINE, "Deleting {0}", assetsBuildFile.getNameExt());
                        assetsBuildFile.delete();
                    }
                    logger.log(Level.FINE, "Saving project {0}", proj.getProjectDirectory().getName());
//                    ProjectManager.getDefault().saveProject(proj);
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
                removeAntTaskLibrary(proj, antTaskLibrary);
                try {
                    removeZipContents(proj.getProjectDirectory());
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        } else {
            logger.log(Level.WARNING, "Trying to include assets build snippet in project type that doesn't support AntBuildExtender API contract.");
        }
    }

    public void setExtensionDependencies(String[] extensionDependencies) {
        this.extensionDependencies = extensionDependencies;
    }

    public void setExtensionTargets(String extensionTargets) {
        this.extensionTargets = extensionTargets;
    }

    /**
     * Sets the name of a library that will be added for loading with the ant
     * task. If set to null no library is added (default).
     *
     * @param antTaskLibrary
     */
    public void setAntTaskLibrary(String antTaskLibrary) {
        this.antTaskLibrary = antTaskLibrary;
    }

    public void setDataZip(String url) {
        try {
            this.zipFile = new URL(url);
        } catch (MalformedURLException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    public void setDataZipUrl(URL url) {
        this.zipFile = url;
    }

    /**
     * Loads ant targets from a file in the classpath
     *
     * @param path
     */
    public void loadTargets(String path) {
        try {
            LineNumberReader in = new LineNumberReader(new InputStreamReader(new URL(path).openStream()));
            StringWriter out = new StringWriter();
            String line = in.readLine();
            while (line != null) {
                out.write(line + "\n");
                line = in.readLine();
            }
            in.close();
            out.close();
            extensionTargets = out.toString();
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

    }

    /**
     * Adds a run configuration (works direct, no removing)
     *
     * @param project
     * @param name
     * @param properties
     */
    public void addRunConfiguration(Project project, String name, Properties properties) {
        try {
            J2SEProjectConfigurations.createConfigurationFiles(project, name, properties, null);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IllegalArgumentException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    /**
     * Adds a run configuration (works direct, no removing)
     *
     * @param project
     * @param name File name of the configuration
     * @param label Label in the dropdown box
     * @param runTarget Target for "run"
     * @param debugTarget Target for "debug"
     */
    public void addRunConfiguration(Project project, String name, String label, String runTarget, String debugTarget, String cleanTarget) {
        try {
            EditableProperties properties = new EditableProperties(true);
            properties.setProperty("$label", label);
            properties.setProperty("$target.run", runTarget);
            properties.setProperty("$target.debug", debugTarget);
            properties.setProperty("$target.clean", cleanTarget);
            J2SEProjectConfigurations.createConfigurationFiles(project, name, properties, null);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IllegalArgumentException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    /**
     * Adds a run configuration (works direct, no removing)
     *
     * @param project
     * @param name File name of the configuration
     * @param label Label in the dropdown box
     * @param runTarget Target for "run"
     * @param debugTarget Target for "debug"
     */
    public void addRunConfiguration(Project project, String name, String label, String runTarget, String debugTarget) {
        try {
            EditableProperties properties = new EditableProperties(true);
            properties.setProperty("$label", label);
            properties.setProperty("$target.run", runTarget);
            properties.setProperty("$target.debug", debugTarget);
            J2SEProjectConfigurations.createConfigurationFiles(project, name, properties, null);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IllegalArgumentException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    /**
     * Adds a run configuration (works direct, no removing)
     *
     * @param project
     * @param name File name of the configuration
     * @param label Label in the dropdown box
     * @param runTarget Target for "run"
     */
    public void addRunConfiguration(Project project, String name, String label, String runTarget) {
        try {
            EditableProperties properties = new EditableProperties(true);
            properties.setProperty("$label", label);
            properties.setProperty("$target.run", runTarget);
            J2SEProjectConfigurations.createConfigurationFiles(project, name, properties, null);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IllegalArgumentException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private FileObject getImplFile(FileObject projDir, boolean create) {
        FileObject assetsImpl = projDir.getFileObject("nbproject/" + extensionName + "-impl.xml");
        if (assetsImpl == null) {
            logger.log(Level.FINE, "No extension file {0}-impl.xml found", extensionName);
            if (create) {
                logger.log(Level.FINE, "Creating extension file {0}-impl.xml", extensionName);
                assetsImpl = createImplFile(projDir);
            }
        } else {
            logger.log(Level.FINE, "Found extension file {0}-impl.xml", extensionName);
            try {
                if (create && !assetsImpl.asLines().get(1).startsWith("<!--" + extensionName + "-impl.xml " + extensionVersion + "-->")) {
                    logger.log(Level.FINE, "Updating extension file {0}-impl.xml", extensionName);
                    assetsImpl.delete();
                    logger.log(Level.FINE, "Deleted extension file {0}-impl.xml", extensionName);
                    assetsImpl = createImplFile(projDir);
                    logger.log(Level.FINE, "Recreated extension file {0}-impl.xml", extensionName);
                }
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        return assetsImpl;
    }

    private FileObject createImplFile(FileObject projDir) {
        FileLock lock = null;
        FileObject file = null;
        try {
            file = projDir.getFileObject("nbproject").createData(extensionName + "-impl.xml");
            lock = file.lock();
            OutputStreamWriter out = new OutputStreamWriter(file.getOutputStream(lock));
            out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            out.write("<!--" + extensionName + "-impl.xml " + extensionVersion + "-->\n");
            out.write("<project name=\"" + extensionName + "-impl\" basedir=\"..\">\n");
            if (extensionTargets != null) {
                out.write(extensionTargets);
            }
            out.write("</project>\n");
            out.close();
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } finally {
            if (lock != null) {
                lock.releaseLock();
            }
        }
        return file;
    }

    private void addAntTaskLibrary(Project proj, String name) {
        if (name == null) {
            return;
        }
        try {
            AntBuildExtender extender = proj.getLookup().lookup(AntBuildExtender.class);
            if (extender != null) {
                LibraryManager.getDefault();
                extender.addLibrary(LibraryManager.getDefault().getLibrary(name));
//                ProjectManager.getDefault().saveProject(proj);
            }

        } catch (Exception e) {
            Exceptions.printStackTrace(e);
        }
    }

    private void removeAntTaskLibrary(Project proj, String name) {
        if (name == null) {
            return;
        }
        try {
            AntBuildExtender extender = proj.getLookup().lookup(AntBuildExtender.class);
            if (extender != null) {
                LibraryManager.getDefault();
                extender.removeLibrary(LibraryManager.getDefault().getLibrary(name));
//                ProjectManager.getDefault().saveProject(proj);
            }
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private void addZipContents(FileObject projectRoot) throws IOException {
        if (zipFile == null) {
            return;
        }
        InputStream in = zipFile.openStream();
        try {
            ZipInputStream str = new ZipInputStream(in);
            ZipEntry entry;
            while ((entry = str.getNextEntry()) != null) {
                String fileName = resourcesFolder + "/" + extensionName + "/" + entry.getName();
                if (entry.isDirectory()) {
                    //XXX: deleting old (beta) files here
                    FileObject fo = projectRoot.getFileObject(entry.getName());
                    if (fo != null && entry.getSize() != -1 && entry.getSize() == fo.getSize()) {
                        logger.log(Level.FINE, "Deleting old folder {0}", fo.getNameExt());
                        fo.delete();
                    }
                    FileUtil.createFolder(projectRoot, fileName);
                } else {
                    //XXX: deleting old (beta) files here
                    FileObject fo = projectRoot.getFileObject(entry.getName());
                    if (fo != null && !fo.equals(projectRoot)) {
                        logger.log(Level.FINE, "Deleting old file {0}", fo.getNameExt());
                        fo.delete();
                    }
                    fo = projectRoot.getFileObject(fileName);
                    if (fo == null) {
                        fo = FileUtil.createData(projectRoot, fileName);
                        writeFile(str, fo);
                    } else {
                        logger.log(Level.FINE, "Not overwriting existing file {0}", fo.getNameExt());
                    }
                }
            }
        } finally {
            in.close();
        }
    }

    private void removeZipContents(FileObject projectRoot) throws IOException {
        if (zipFile == null) {
            return;
        }
        InputStream in = zipFile.openStream();
        boolean kept = false;
        try {
            ZipInputStream str = new ZipInputStream(in);
            ZipEntry entry;
            while ((entry = str.getNextEntry()) != null) {
                //XXX: deleting old (beta) files here
                FileObject old = projectRoot.getFileObject(entry.getName());
                if (old != null && !old.equals(projectRoot)) {
                    logger.log(Level.FINE, "Deleting old file {0}", old.getNameExt());
                    if (entry.getSize() != -1 && entry.getSize() == old.getSize()) {
                        old.delete();
                    }
                }
                String fileName = resourcesFolder + "/" + extensionName + "/" + entry.getName();
                FileObject obj = projectRoot.getFileObject(fileName);
                if (obj != null && !obj.equals(projectRoot) && !obj.isFolder()) {
                    if (entry.getSize() != -1 && entry.getSize() == obj.getSize()) {
                        logger.log(Level.FINE, "Deleting file {0}", obj.getNameExt());
                        obj.delete();
                    } else {
                        kept = true;
                        logger.log(Level.FINE, "Keeping file {0}", obj.getNameExt());
                    }
                }
            }
        } finally {
            in.close();
        }
        FileObject folder = projectRoot.getFileObject(resourcesFolder + "/" + extensionName);
        if (folder != null && !kept) {
            folder.delete();
        }
        FileObject resourceFolder = projectRoot.getFileObject(resourcesFolder);
        if (resourceFolder != null && resourceFolder.getChildren().length == 0) {
            resourceFolder.delete();
        }
    }

    private void writeFile(ZipInputStream str, FileObject fo) throws IOException {
        OutputStream out = fo.getOutputStream();
        try {
            logger.log(Level.FINE, "Creating file " + fo.getNameExt());
            FileUtil.copy(str, out);
        } finally {
            out.close();
        }
    }
}
