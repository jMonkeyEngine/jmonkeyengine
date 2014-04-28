/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.materialdefinition.fileStructure.leaves;

/**
 *
 * @author Nehon
 */
public abstract class MappingBlock extends LeafStatement {

    protected String leftVar;
    protected String rightVar;
    protected String leftVarSwizzle;
    protected String rightVarSwizzle;
    protected String leftNameSpace;
    protected String rightNameSpace;
    protected String condition;

    public MappingBlock(int lineNumber, String line) {
        super(lineNumber, line);
    }

    protected abstract void updateLine();

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        String old = this.condition;
        if (condition.trim().length() == 0 || condition.equals("<null value>")) {
            condition = null;
        }
        this.condition = condition;
        updateLine();
        fire("condition", old, condition);
    }

    public String getLeftVar() {
        return leftVar;
    }

    public void setLeftVar(String leftVar) {
        this.leftVar = leftVar;
        updateLine();
    }

    public String getRightVar() {
        return rightVar;
    }

    public void setRightVar(String rightVar) {
        this.rightVar = rightVar;
        updateLine();
    }

    public String getRightNameSpace() {
        return rightNameSpace;
    }

    public void setRightNameSpace(String rightnameSpace) {
        String old = this.rightNameSpace;
        this.rightNameSpace = rightnameSpace;
        updateLine();
        fire("rightNameSpace", old, rightnameSpace);
    }

    public String getLeftVarSwizzle() {
        return leftVarSwizzle;
    }

    public void setLeftVarSwizzle(String leftVarSwizzle) {
        String old = this.leftVarSwizzle;
        if (leftVarSwizzle.trim().length() == 0) {
            leftVarSwizzle = null;
        }
        this.leftVarSwizzle = leftVarSwizzle;
        updateLine();
        fire("leftVarSwizzle", old, leftVarSwizzle);
    }

    public String getRightVarSwizzle() {
        return rightVarSwizzle;
    }

    public void setRightVarSwizzle(String rightVarSwizzle) {
        String old = this.rightVarSwizzle;
        this.rightVarSwizzle = rightVarSwizzle;
        if (rightVarSwizzle.trim().length() == 0) {
            rightVarSwizzle = null;
        }
        this.rightVarSwizzle = rightVarSwizzle;
        updateLine();
        fire("rightVarSwizzle", old, rightVarSwizzle);
    }

    public String getLeftNameSpace() {
        return leftNameSpace;
    }

    public void setLeftNameSpace(String leftNameSpace) {
        String old = this.leftNameSpace;
        this.leftNameSpace = leftNameSpace;
        updateLine();
        fire("leftNameSpace", old, leftNameSpace);
    }
  
    
    
}
