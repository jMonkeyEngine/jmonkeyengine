/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.nmgen.wizard;

import com.jme3.bounding.BoundingBox;
import com.jme3.gde.core.sceneexplorer.nodes.actions.AbstractNewSpatialWizardAction;
import com.jme3.gde.core.sceneexplorer.nodes.actions.NewSpatialAction;
import com.jme3.gde.nmgen.NavMeshGenerator;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.terrain.Terrain;
import java.awt.Component;
import java.awt.Dialog;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JComponent;
import jme3tools.optimize.GeometryBatchFactory;
import org.critterai.nmgen.IntermediateData;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.DialogDisplayer;
import org.openide.WizardDescriptor;

@org.openide.util.lookup.ServiceProvider(service = NewSpatialAction.class)
public final class NewNavMeshWizardAction extends AbstractNewSpatialWizardAction {

    private WizardDescriptor.Panel[] panels;
    private NavMeshGenerator generator;

    public NewNavMeshWizardAction() {
        name = "NavMesh..";
    }

    @Override
    protected Object showWizard(org.openide.nodes.Node node) {
        WizardDescriptor wizardDescriptor = new WizardDescriptor(getPanels());
        // {0} will be replaced by WizardDesriptor.Panel.getComponent().getName()
        wizardDescriptor.setTitleFormat(new MessageFormat("{0}"));
        wizardDescriptor.setTitle("Create NavMesh");
        if (generator == null)
            generator = new NavMeshGenerator();
        wizardDescriptor.putProperty("generator", generator);

        Dialog dialog = DialogDisplayer.getDefault().createDialog(wizardDescriptor);
        dialog.setVisible(true);
        dialog.toFront();
        boolean cancelled = wizardDescriptor.getValue() != WizardDescriptor.FINISH_OPTION;
        if (!cancelled) {
            return wizardDescriptor;
        }
        return null;
    }

    @Override
    protected Spatial doCreateSpatial(com.jme3.scene.Node rootNode, Object configuration) {
        if (configuration == null) {
            return null;
        }
        ProgressHandle progressHandle = ProgressHandleFactory.createHandle("Generating NavMesh");
        progressHandle.start();
        final Geometry navMesh = new Geometry("NavMesh");
        try {
            //TODO: maybe offload to other thread..
            WizardDescriptor wizardDescriptor = (WizardDescriptor) configuration;

            NavMeshGenerator generator = (NavMeshGenerator) wizardDescriptor.getProperty("generator");
            IntermediateData id = new IntermediateData();

            generator.setIntermediateData(null);

            Mesh mesh = new Mesh();

            GeometryBatchFactory.mergeGeometries(findGeometries(rootNode, new LinkedList<Geometry>(), generator, rootNode), mesh);
            Mesh optiMesh = generator.optimize(mesh);
            if(optiMesh == null) return null;

            Material material = new Material(pm, "Common/MatDefs/Misc/Unshaded.j3md");
            material.getAdditionalRenderState().setWireframe(true);
            material.setColor("Color", ColorRGBA.Green);
            navMesh.setMaterial(material);
            navMesh.setMesh(optiMesh);
            navMesh.setCullHint(CullHint.Always);
            navMesh.setModelBound(new BoundingBox());
        } finally {
            progressHandle.finish();
        }

        return navMesh;
    }

    private List<Geometry> findGeometries(Node node, List<Geometry> geoms, NavMeshGenerator generator, Node originalRoot) {
        if (node instanceof Terrain) {
            Terrain terr = (Terrain)node;
            Mesh merged = generator.terrain2mesh(terr);
            Geometry g = new Geometry("mergedTerrain");
            g.setMesh(merged);
            if (node != originalRoot) {
                g.setLocalScale(((Node)terr).getLocalScale());
                g.setLocalTranslation(((Node)terr).getLocalTranslation());
            }
            geoms.add(g);
            return geoms;
        }
        
        for (Iterator<Spatial> it = node.getChildren().iterator(); it.hasNext();) {
            Spatial spatial = it.next();
            if (spatial instanceof Geometry) {
                geoms.add((Geometry) spatial);
            } else if (spatial instanceof Node) {
                findGeometries((Node) spatial, geoms, generator, originalRoot);
            }
        }
        return geoms;
    }

    /**
     * Initialize panels representing individual wizard's steps and sets
     * various properties for them influencing wizard appearance.
     */
    private WizardDescriptor.Panel[] getPanels() {
        if (panels == null) {
            panels = new WizardDescriptor.Panel[]{
                new NewNavMeshWizardPanel1()
            };
            String[] steps = new String[panels.length];
            for (int i = 0; i < panels.length; i++) {
                Component c = panels[i].getComponent();
                // Default step name to component name of panel. Mainly useful
                // for getting the name of the target chooser to appear in the
                // list of steps.
                steps[i] = c.getName();
                if (c instanceof JComponent) { // assume Swing components
                    JComponent jc = (JComponent) c;
                    // Sets step number of a component
                    // TODO if using org.openide.dialogs >= 7.8, can use WizardDescriptor.PROP_*:
                    jc.putClientProperty("WizardPanel_contentSelectedIndex", new Integer(i));
                    // Sets steps names for a panel
                    jc.putClientProperty("WizardPanel_contentData", steps);
                    // Turn on subtitle creation on each step
                    jc.putClientProperty("WizardPanel_autoWizardStyle", Boolean.TRUE);
                    // Show steps on the left side with the image on the background
                    jc.putClientProperty("WizardPanel_contentDisplayed", Boolean.TRUE);
                    // Turn on numbering of all steps
                    jc.putClientProperty("WizardPanel_contentNumbered", Boolean.TRUE);
                }
            }
        }
        return panels;
    }
}
