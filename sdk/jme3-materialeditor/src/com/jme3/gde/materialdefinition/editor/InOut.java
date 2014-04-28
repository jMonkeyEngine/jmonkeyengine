/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.materialdefinition.editor;

import com.jme3.gde.materialdefinition.fileStructure.leaves.InputMappingBlock;
import com.jme3.gde.materialdefinition.fileStructure.leaves.OutputMappingBlock;

/**
 *
 * @author Nehon
 */
public interface InOut {

    public String getName();

    public void addInputMapping(InputMappingBlock block);

    public void removeInputMapping(InputMappingBlock block);

    public void addOutputMapping(OutputMappingBlock block);

    public void removeOutputMapping(OutputMappingBlock block);
}
