/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.materialdefinition.navigator.node;

import com.jme3.gde.materialdefinition.fileStructure.MatDefBlock;
import com.jme3.gde.materialdefinition.fileStructure.leaves.MatParamBlock;
import com.jme3.gde.materialdefinition.navigator.node.properties.MatParamProperty;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import javax.swing.Action;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Sheet;
import org.openide.util.Lookup;

/**
 *
 * @author Nehon
 */
public class AbstractMatDefNode extends AbstractNode {

    protected Lookup lookup;

    public AbstractMatDefNode(Children children, Lookup lookup) {
        super(children, lookup);
        this.lookup = lookup;
        MatDefBlock block = lookup.lookup(MatDefBlock.class);
        block.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals(MatDefBlock.ADD_MAT_PARAM) || evt.getPropertyName().equals(MatDefBlock.REMOVE_MAT_PARAM)) {
                    setSheet(createSheet());
                    firePropertySetsChange(null, null);
                }
            }
        });

    }

    
    @Override
    public Action[] getActions(boolean popup) {
        return new Action[]{};
    }
      
    @Override
    protected Sheet createSheet() {
        Sheet sheet = super.createSheet();
        MatDefBlock def = lookup.lookup(MatDefBlock.class);
        List<MatParamBlock> params = def.getMatParams();

        Sheet.Set set = new Sheet.Set();
        set.setName("MaterialParameters");
        set.setDisplayName("Material Parameters");
        for (MatParamBlock matParamBlock : params) {
            set.put(MatParamProperty.makeProperty(matParamBlock, lookup));
        }

        sheet.put(set);

        return sheet;

    }
}
