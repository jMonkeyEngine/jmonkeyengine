/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.materialdefinition.fileStructure;

import com.jme3.util.blockparser.Statement;

/**
 *
 * @author Nehon
 */
public class FragmentShaderNodesBlock extends ShaderNodesBlock {

    public FragmentShaderNodesBlock(int lineNumber, String line) {
        super(lineNumber, line);
    }

    public FragmentShaderNodesBlock(Statement sta) {
        super(sta);
    }
}
