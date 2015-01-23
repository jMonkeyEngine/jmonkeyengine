/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.shadernodedefinition.wizard;

import com.jme3.gde.core.assets.ProjectAssetManager;
import java.awt.Component;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.event.ChangeListener;
import javax.xml.transform.Templates;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.api.templates.TemplateRegistration;
import org.netbeans.api.templates.TemplateRegistrations;
import org.netbeans.spi.project.ui.templates.support.Templates;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.util.NbBundle.Messages;

// TODO define position attribute
@TemplateRegistrations({
@TemplateRegistration(folder = "Material", content = "../SNDefTemplate.j3sn", displayName = "#SNDefWizardIterator_displayName", iconBase = "com/jme3/gde/materialdefinition/icons/node.png", description = "../sNDef.html", scriptEngine = "freemarker"),
@TemplateRegistration(folder = "Material", content = "../ShaderNodeSource", scriptEngine = "freemarker")
})
@Messages("SNDefWizardIterator_displayName=Shader Node Definition")
public final class SNDefWizardIterator implements WizardDescriptor.InstantiatingIterator<WizardDescriptor> {

    private int index;

    private WizardDescriptor wizard;
    private List<WizardDescriptor.Panel<WizardDescriptor>> panels;

    private List<WizardDescriptor.Panel<WizardDescriptor>> getPanels() {
        if (panels == null) {
            panels = new ArrayList<WizardDescriptor.Panel<WizardDescriptor>>();
            // Change to default new file panel and add our panel at bottom
            Project p = Templates.getProject(wizard);
            SourceGroup[] groups = ProjectUtils.getSources(p).getSourceGroups(Sources.TYPE_GENERIC);

            // SimpleTargetChooser is the default new file panel,
            // Add our panel at the bottom
            WizardDescriptor.Panel<WizardDescriptor> advNewFilePanel = Templates.buildSimpleTargetChooser(p, groups).create();
            panels.add(advNewFilePanel);
            panels.add(new SNDefWizardPanel1());
            panels.add(new SNDefWizardPanel2("Inputs"));
            panels.add(new SNDefWizardPanel2("Outputs"));
            String[] steps = createSteps();
            for (int i = 0; i < panels.size(); i++) {
                Component c = panels.get(i).getComponent();
                if (steps[i] == null) {
                    // Default step name to component name of panel. Mainly
                    // useful for getting the name of the target chooser to
                    // appear in the list of steps.
                    steps[i] = c.getName();
                }
                if (c instanceof JComponent) { // assume Swing components
                    JComponent jc = (JComponent) c;
                    jc.putClientProperty(WizardDescriptor.PROP_CONTENT_SELECTED_INDEX, i);
                    jc.putClientProperty(WizardDescriptor.PROP_CONTENT_DATA, steps);
                    jc.putClientProperty(WizardDescriptor.PROP_AUTO_WIZARD_STYLE, true);
                    jc.putClientProperty(WizardDescriptor.PROP_CONTENT_DISPLAYED, true);
                    jc.putClientProperty(WizardDescriptor.PROP_CONTENT_NUMBERED, true);
                }
            }
        }
        return panels;
    }

    @Override
    public Set<?> instantiate() throws IOException {

        FileObject createdFile = null;

        // Read Title from wizard 
        // String HtmlTitle = (String) wizard.getProperty(SNDefWizardPanel1.TITLE);
        // FreeMarker Template will get its variables from HashMap.
        // HashMap key is the variable name.
        SNDefVisualPanel1 panel1 = ((SNDefWizardPanel1) panels.get(1)).getComponent();
        SNDefVisualPanel2 panel2 = ((SNDefWizardPanel2) panels.get(2)).getComponent();
        SNDefVisualPanel2 panel3 = ((SNDefWizardPanel2) panels.get(3)).getComponent();

        Map args = new HashMap();
        args.put("defName", panel1.getDefName());
        args.put("defType", panel1.getDefType());
        args.put("description", panel1.getDefDescription());
        args.put("inputParams", panel2.getData());
        args.put("outputParams", panel3.getData());

        //Get the template and convert it:
        FileObject tplSnd = Templates.getTemplate(wizard);
        FileObject tplShd = tplSnd.getParent().getChildren()[1];
        
        DataObject templateSnd = DataObject.find(tplSnd);
        DataObject templateShd = DataObject.find(tplShd);

        
        
        //Get the package:
        FileObject dir = Templates.getTargetFolder(wizard);
        DataFolder df = DataFolder.findFolder(dir);

        ProjectAssetManager assetManager = new ProjectAssetManager(Templates.getProject(wizard),"assets");
        
        //Get the class:
        String targetName = Templates.getTargetName(wizard);
        String shaderName = targetName;
        if (panel1.getDefType().equals("Fragment")) {
            shaderName += ".frag";
        } else if (panel1.getDefType().equals("Vertex")) {
            shaderName += ".vert";
        } else {
            shaderName += ".frag";
        }
        
        args.put("shaderSnippet",assetManager.getRelativeAssetPath(dir.getPath()+"/"+shaderName));

        //Define the template from the above,
        //passing the package, the file name, and the map of strings to the template:
       // DataObject dobj = templateSnd.createFromTemplate(df, targetName, args);
        
       
        DataObject sobj = templateShd.createFromTemplate(df, shaderName, args);
 DataObject dobj = templateSnd.createFromTemplate(df, targetName, args);
        //Obtain a FileObject:
        createdFile = dobj.getPrimaryFile();

        // Return the created file.
        return Collections.singleton(createdFile);
    }

    @Override
    public void initialize(WizardDescriptor wizard) {
        this.wizard = wizard;
    }

    @Override
    public void uninitialize(WizardDescriptor wizard) {
        panels = null;
    }

    @Override
    public WizardDescriptor.Panel<WizardDescriptor> current() {
        return getPanels().get(index);
    }

    @Override
    public String name() {
        return index + 1 + ". from " + getPanels().size();
    }

    @Override
    public boolean hasNext() {
        return index < getPanels().size() - 1;
    }

    @Override
    public boolean hasPrevious() {
        return index > 0;
    }

    @Override
    public void nextPanel() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        index++;
    }

    @Override
    public void previousPanel() {
        if (!hasPrevious()) {
            throw new NoSuchElementException();
        }
        index--;
    }

    // If nothing unusual changes in the middle of the wizard, simply:
    @Override
    public void addChangeListener(ChangeListener l) {
    }

    @Override
    public void removeChangeListener(ChangeListener l) {
    }
    // If something changes dynamically (besides moving between panels), e.g.
    // the number of panels changes in response to user input, then use
    // ChangeSupport to implement add/removeChangeListener and call fireChange
    // when needed

    // You could safely ignore this method. Is is here to keep steps which were
    // there before this wizard was instantiated. It should be better handled
    // by NetBeans Wizard API itself rather than needed to be implemented by a
    // client code.
    private String[] createSteps() {
        String[] beforeSteps = (String[]) wizard.getProperty("WizardPanel_contentData");
        assert beforeSteps != null : "This wizard may only be used embedded in the template wizard";
        String[] res = new String[(beforeSteps.length - 1) + panels.size()];
        for (int i = 0; i < res.length; i++) {
            if (i < (beforeSteps.length - 1)) {
                res[i] = beforeSteps[i];
            } else {
                res[i] = panels.get(i - beforeSteps.length + 1).getComponent().getName();
            }
        }
        return res;
    }

}
