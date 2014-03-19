package com.jme3.gde.assetpack.project;

import com.jme3.gde.assetpack.XmlHelper;
import com.jme3.gde.assetpack.browser.nodes.AssetPackBrowserFolder;
import com.jme3.gde.assetpack.project.properties.GeneralSettingsPanel;
import com.jme3.gde.assetpack.project.properties.LicensePanel;
import com.jme3.gde.core.assets.ProjectAssetManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.DeleteOperationImplementation;
import org.netbeans.spi.project.CopyOperationImplementation;
import org.netbeans.spi.project.ProjectState;
import org.netbeans.spi.project.ui.CustomizerProvider;
import org.netbeans.spi.project.ui.LogicalViewProvider;
import org.netbeans.spi.project.ui.support.DefaultProjectOperations;
import org.netbeans.spi.project.ui.support.ProjectCustomizer;
import org.netbeans.spi.project.ui.support.ProjectCustomizer.Category;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.openide.xml.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

@SuppressWarnings("unchecked")
public class AssetPackProject implements Project {

    private final FileObject projectDir;
    private LogicalViewProvider logicalView = new AssetPackProjectLogicalView(this);
    private String assetsFolder = "assets";
    private final ProjectState state;
    private Lookup lkp;
    private ProjectAssetManager projectAssetManager;
    private Document configuration;
    private AssetPackProjectCustomizer projectCustomizer;
    private AssetPackBrowserFolder assetPackFolder;

    public AssetPackProject(FileObject projectDir, ProjectState state) {
        this.projectDir = projectDir;
        this.state = state;
        projectAssetManager = new ProjectAssetManager(this, assetsFolder);
        loadProjectFile();
        projectCustomizer = new AssetPackProjectCustomizer(this);
    }

    @Override
    public FileObject getProjectDirectory() {
        return projectDir;
    }

    private FileObject getConfigFile() {
        FileObject folder = projectDir;
        FileObject file = folder.getFileObject(AssetPackProjectFactory.CONFIG_NAME);
        if (file == null) {
            try {
                return folder.createData(AssetPackProjectFactory.CONFIG_NAME);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        return file;
    }

    private void loadProjectFile() {
        InputStream in = null;
        try {
            FileObject file = getConfigFile();
            in = file.getInputStream();
            configuration = XMLUtil.parse(new InputSource(in), false, false, null, null);
            in.close();
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        } finally {
        }
    }

    public FileObject getAssetsFolder() {
        FileObject result =
                projectDir.getFileObject(assetsFolder);
        try {
            if (result == null) {
                result = projectDir.createFolder(assetsFolder);
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return result;
    }

    public String getDescription() {
        Element properties = null;
        properties = XmlHelper.findChildElement(configuration.getDocumentElement(), "description");
        if (properties == null) {
            return "";
        }
        return properties.getTextContent().trim();
    }

    public void setDescription(String description) {
        Element properties = null;
        properties = XmlHelper.findChildElement(configuration.getDocumentElement(), "description");
        if (properties == null) {
            return;
        }
        properties.setTextContent(description);
    }

    public String getLicense() {
        Element properties = null;
        properties = XmlHelper.findChildElement(configuration.getDocumentElement(), "license");
        if (properties == null) {
            return "";
        }
        return properties.getTextContent().trim();
    }

    public void setLicense(String description) {
        Element properties = null;
        properties = XmlHelper.findChildElement(configuration.getDocumentElement(), "license");
        if (properties == null) {
            properties = configuration.createElement("license");
            configuration.appendChild(properties);
        }
        properties.setTextContent(description);
    }

    public String getProjectName() {
        return configuration.getDocumentElement().getAttribute("name");
    }

    public void setProjectName(String name) {
        configuration.getDocumentElement().setAttribute("name", name);
    }

    public String getDistributorName() {
        return configuration.getDocumentElement().getAttribute("distributor");
    }

    public void setDistributorName(String name) {
        configuration.getDocumentElement().setAttribute("distributor", name);
    }

    public String getVersion() {
        return configuration.getDocumentElement().getAttribute("version");
    }

    public void setVersion(String name) {
        configuration.getDocumentElement().setAttribute("version", name);
    }

    public void saveSettings() {
        FileLock lock = null;
        try {
            FileObject file = getConfigFile();
            lock = file.lock();
            OutputStream out = file.getOutputStream(lock);
            XMLUtil.write(configuration, out, "UTF-8");
            if (out != null) {
                out.close();

            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } finally {
            if (lock != null) {
                lock.releaseLock();
            }
        }
    }

    public Element getProjectAssets() {
        return XmlHelper.findChildElement(configuration.getDocumentElement(), "assets");
    }

    public Element getProjectDescription() {
        return XmlHelper.findChildElement(configuration.getDocumentElement(), "description");
    }

    public Element getProjectLicense() {
        return XmlHelper.findChildElement(configuration.getDocumentElement(), "license");
    }

    @Override
    public Lookup getLookup() {
        if (lkp == null) {
            lkp = Lookups.fixed(new Object[]{
                        this, //project spec requires a project be in its own lookup
                        state, //allow outside code to mark the project as needing saving
                        logicalView, //Logical view of project implementation
                        projectAssetManager,
                        projectCustomizer,
                        new ActionProviderImpl(), //Provides standard actions like Build and Clean
                        new DemoDeleteOperation(),
                        new DemoCopyOperation(this),
                        new Info(), //Project information implementation
                    });
        }
        return lkp;
    }

    public ProjectAssetManager getProjectAssetManager() {
        return projectAssetManager;
    }

    public AssetPackProjectCustomizer getProjectCustomizer() {
        return projectCustomizer;
    }

    /**
     * @return the configuration
     */
    public Document getConfiguration() {
        return configuration;
    }

    public AssetPackBrowserFolder getAssetPackFolder() {
        return assetPackFolder;
    }

    public AssetPackBrowserFolder setAssetPackFolder(AssetPackBrowserFolder folder) {
        assetPackFolder = folder;
        return assetPackFolder;
    }

//    public void showProjectCustomizer(){
//        ProjectCustomizer.createCustomizerDialog("com-jme3-gde-assetpack", this.getLookup(), "General", null, HelpCtx.DEFAULT_HELP).setVisible(true);
//    }
    private final class ActionProviderImpl implements ActionProvider {

        private String[] supported = new String[]{
            ActionProvider.COMMAND_DELETE,
            ActionProvider.COMMAND_COPY,};

        @Override
        public String[] getSupportedActions() {
            return supported;
        }

        @Override
        public void invokeAction(String string, Lookup lookup) throws IllegalArgumentException {
            if (string.equalsIgnoreCase(ActionProvider.COMMAND_DELETE)) {
                DefaultProjectOperations.performDefaultDeleteOperation(AssetPackProject.this);
            }
            if (string.equalsIgnoreCase(ActionProvider.COMMAND_COPY)) {
                DefaultProjectOperations.performDefaultCopyOperation(AssetPackProject.this);
            }
        }

        @Override
        public boolean isActionEnabled(String command, Lookup lookup) throws IllegalArgumentException {
            if ((command.equals(ActionProvider.COMMAND_DELETE))) {
                return true;
            } else if ((command.equals(ActionProvider.COMMAND_COPY))) {
                return true;
            } else {
                throw new IllegalArgumentException(command);
            }
        }
    }

    private final class DemoDeleteOperation implements DeleteOperationImplementation {

        public void notifyDeleting() throws IOException {
        }

        public void notifyDeleted() throws IOException {
        }

        public List<FileObject> getMetadataFiles() {
            List<FileObject> dataFiles = new ArrayList<FileObject>();
            return dataFiles;
        }

        public List<FileObject> getDataFiles() {
            List<FileObject> dataFiles = new ArrayList<FileObject>();
            return dataFiles;
        }
    }

    private final class DemoCopyOperation implements CopyOperationImplementation {

        private final AssetPackProject project;
        private final FileObject projectDir;

        public DemoCopyOperation(AssetPackProject project) {
            this.project = project;
            this.projectDir = project.getProjectDirectory();
        }

        public List<FileObject> getMetadataFiles() {
            return Collections.EMPTY_LIST;
        }

        public List<FileObject> getDataFiles() {
            return Collections.EMPTY_LIST;
        }

        public void notifyCopying() throws IOException {
        }

        public void notifyCopied(Project arg0, File arg1, String arg2) throws IOException {
        }
    }

    private final class AssetPackProjectCustomizer implements CustomizerProvider, ProjectCustomizer.CategoryComponentProvider, ActionListener {

        private Category[] propertyCategories = new Category[]{
            Category.create("General", "General", null),
            Category.create("License", "License", null),};
        AssetPackProject project;
        GeneralSettingsPanel panel1;
        LicensePanel panel2;

        public AssetPackProjectCustomizer(AssetPackProject project) {
            this.project = project;
        }

        public void showCustomizer() {
            ProjectCustomizer.createCustomizerDialog(propertyCategories, this, "General", this, new HelpCtx("sdk.asset_packs")).setVisible(true);
        }

        public JComponent create(Category category) {
            if (category.getName().equals("General")) {
                panel1 =
                        new GeneralSettingsPanel(project);
                return panel1;
            } else if (category.getName().equals("License")) {
                panel2 =
                        new LicensePanel(project);
                return panel2;
            } else {
                return new JPanel();
            }

        }

        public void actionPerformed(ActionEvent e) {
            panel1.actionPerformed(null);
            panel2.actionPerformed(null);
            saveSettings();
        }
    }

    /** Implementation of project system's ProjectInformation class */
    private final class Info implements ProjectInformation {

        @Override
        public Icon getIcon() {
            return new ImageIcon(ImageUtilities.loadImage(
                    "com/jme3/gde/assetpack/icons/assetpack.png"));
        }

        @Override
        public String getName() {
            return getProjectName();
        }

        @Override
        public String getDisplayName() {
            return getProjectName();
        }

        @Override
        public void addPropertyChangeListener(PropertyChangeListener pcl) {
            //do nothing, won't change
        }

        @Override
        public void removePropertyChangeListener(PropertyChangeListener pcl) {
            //do nothing, won't change
        }

        @Override
        public Project getProject() {
            return AssetPackProject.this;
        }
    }
}
