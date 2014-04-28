/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.materialdefinition.fileStructure.leaves;

import com.jme3.material.TechniqueDef;
import com.jme3.util.blockparser.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Nehon
 */
public class LightModeBlock extends LeafStatement {

    protected String lightMode;

    protected LightModeBlock(int lineNumber, String line) {
        super(lineNumber, line);
    }

    public LightModeBlock(Statement sta) {
        this(sta.getLineNumber(), sta.getLine());
        parse(sta);
         updateLine();
    }

    public LightModeBlock(String lightMode) {
        super(0, "");
        this.lightMode = lightMode;
        updateLine();
    }

    private void updateLine() {
        this.line = "LightMode " + lightMode;
    }

    public String getLightMode() {
        return lightMode;
    }

    public void setLightMode(String lightMode) {
        this.lightMode = lightMode;
        updateLine();
    }

    private void parse(Statement sta) {
        try {
            String[] split = sta.getLine().split("\\s");
            lightMode = split[1].trim();            
            
            TechniqueDef.LightMode.valueOf(lightMode);

        } catch (ArrayIndexOutOfBoundsException e) {           
            Logger.getLogger(LightModeBlock.class.getName()).log(Level.WARNING, "Parsing error line " + sta.getLineNumber() + " : " + sta.getLine());
        } catch (IllegalArgumentException ex){
            Logger.getLogger(LightModeBlock.class.getName()).log(Level.WARNING, "Invalid light mode : " + sta.getLineNumber() + " : " + sta.getLine());
        }
    }
}
