package jme3test.android;

/**
 * Name (=appClass) and Description of one demo launch inside the main apk
 * @author larynx
 *
 */
public class DemoLaunchEntry 
{
    private String name;
    private String description;
            
    /**
     * @param name
     * @param description
     */
    public DemoLaunchEntry(String name, String description) {
        super();
        this.name = name;
        this.description = description;
    }
    
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    
    

}
