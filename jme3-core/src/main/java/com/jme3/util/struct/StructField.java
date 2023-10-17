package com.jme3.util.struct;

/**
 * A field of a struct
 * 
 * @author Riccardo Balbo
 */
public abstract class StructField<T> {

    private int position;
    protected T value;
    protected boolean isUpdateNeeded = true;
    private String name;
    private int depth = 0;
    private int group = 0;

    protected StructField(int position, String name, T value) {
        this.position = position;
        this.value = value;
        this.name = name;
    }

    /**
     * Get depth of the field
     * 
     * @return
     */
    public int getDepth() {
        return depth;
    }

    /**
     * Get the group to which this field belongs (eg. a parent struct)
     * 
     * @return id of the group
     */
    public int getGroup() {
        return group;
    }

    void setGroup(int group) {
        this.group = group;
    }

    void setDepth(int depth) {
        this.depth = depth;
    }

    void setPosition(int position) {
        this.position = position;
    }

    /**
     * Get position inside the struct
     * 
     * @return position inside the struct
     */
    public int getPosition() {
        return position;
    }

    /**
     * Get value of this field
     * 
     * @return value
     */
    public T getValue() {
        return value;
    }

    /**
     * Check if field needs update
     * 
     * @return
     */
    public boolean isUpdateNeeded() {
        return isUpdateNeeded;
    }

    /**
     * Clear update needed used internally
     */
    public void clearUpdateNeeded() {
        isUpdateNeeded = false;
    }

    /**
     * Get simple name of the field
     * 
     * @return
     */
    public String getName() {
        String friendlyName;
        if (name != null) friendlyName = name;
        else friendlyName = value.getClass().getSimpleName();
        return friendlyName;
    }

    @Override
    public String toString() {
        return "StructField[" + getName() + "] = " + value.toString();
    }

}