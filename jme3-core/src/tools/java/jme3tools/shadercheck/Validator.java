package jme3tools.shadercheck;

import com.jme3.shader.Shader;

/**
 * Interface for shader validator tools.
 */ 
public interface Validator {
    
    /**
     * Returns the name of the validation tool
     *
     * @return the name
     */
    public String getName();

    /**
     * Returns true if the tool is installed on the system, false otherwise.
     *
     * @return true if installed, otherwise false
     */
    public boolean isInstalled();

    /**
     * @return the tool version as a String, must return null if the tool
     * is not installed.
     */
    public String getInstalledVersion();
    
    /**
     * Validates the given shader to make sure it follows all requirements
     * of the shader language specified as
     * {@link com.jme3.shader.Shader.ShaderSource#getLanguage()}.
     * The results of the validation will be written into the 
     * results argument.
     * 
     * @param shader The shader to validate
     * @param results The storage for the validation results
     */
    public void validate(Shader shader, StringBuilder results);
    
}
