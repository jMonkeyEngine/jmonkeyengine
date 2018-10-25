package com.jme3.input.vr;

import com.jme3.app.VRAppState;
import com.jme3.app.VREnvironment;
import com.jme3.app.state.AppState;
import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.texture.Texture2D;

/**
 * A VR view manager. This interface describes methods that enable to submit 3D views to the VR compositor.
 * @author reden - phr00t - https://github.com/phr00t
 * @author Julien Seinturier - COMEX SA - <a href="http://www.seinturier.fr">http://www.seinturier.fr</a>
 */
public interface VRViewManager {

    /**
     * The name of the left view.
     */
    public final static String LEFT_VIEW_NAME = "Left View";
    
    /**
     * The name of the right view.
     */
    public final static String RIGHT_VIEW_NAME = "Right View";
    
    /**
     * Get the {@link Camera camera} attached to the left eye.
     * @return the {@link Camera camera} attached to the left eye.
     * @see #getRightCamera()
     */
    public Camera getLeftCamera();
    
    /**
     * Get the {@link Camera camera} attached to the right eye.
     * @return the {@link Camera camera} attached to the right eye.
     * @see #getLeftCamera()
     */
    public Camera getRightCamera();
    
    /**
     * Get the {@link ViewPort viewport} attached to the left eye.
     * @return the {@link ViewPort viewport} attached to the left eye.
     * @see #getRightViewPort()
     */
    public ViewPort getLeftViewPort();
    
    
    /**
     * Get the {@link ViewPort viewport} attached to the right eye.
     * @return the {@link ViewPort viewport} attached to the right eye.
     * @see #getLeftViewPort()
     */
    public ViewPort getRightViewPort();
    
    /**
     * Get the {@link ViewPort view port} attached to the mirror display.
     * @return the view port attached to the mirror display.
     * @see #getLeftViewPort()
     * @see #getRightViewPort()
     */
    public ViewPort getMirrorViewPort();
    
    /**
     * Get the texture attached to the left eye.
     * @return the texture attached to the left eye.
     * @see #getRightTexture()
     */
    public Texture2D getLeftTexture();
    
    /**
     * Get the texture attached to the right eye.
     * @return the texture attached to the right eye.
     * @see #getLeftTexture()
     */
    public Texture2D getRightTexture();

    /**
     * Get the depth texture attached to the left eye.
     * @return the texture attached to the left eye.
     * @see #getRightTexture()
     */
    public Texture2D getLeftDepth();
    
    /**
     * Get the depth texture attached to the right eye.
     * @return the texture attached to the right eye.
     * @see #getLeftTexture()
     */
    public Texture2D getRightDepth();
    
    /**
     * Get the {@link FilterPostProcessor filter post processor} attached to the left eye.
     * @return the {@link FilterPostProcessor filter post processor} attached to the left eye.
     * @see #getRightPostProcessor()
     */
    public FilterPostProcessor getLeftPostProcessor();
    
    /**
     * Get the {@link FilterPostProcessor filter post processor} attached to the right eye.
     * @return the {@link FilterPostProcessor filter post processor} attached to the right eye.
     * @see #getLeftPostProcessor()
     */
    public FilterPostProcessor getRightPostProcessor();
    
    /**
     * Get the resolution multiplier.
     * @return the resolution multiplier.
     * @see #setResolutionMultiplier(float)
     */
    public float getResolutionMuliplier();
    
    /**
     * Set the resolution multiplier.
     * @param resMult the resolution multiplier.
     * @see #getResolutionMuliplier()
     */
    public void setResolutionMultiplier(float resMult);
    
    /**
     * Get the height adjustment to apply to the cameras before rendering.
     * @return the height adjustment to apply to the cameras before rendering.
     * @see #setHeightAdjustment(float)
     */
    public float getHeightAdjustment();
    
    /**
     * Set the height adjustment to apply to the cameras before rendering.
     * @param amount the height adjustment to apply to the cameras before rendering.
     * @see #getHeightAdjustment()
     */
    public void setHeightAdjustment(float amount);
    
    /**
     * Get the {@link VREnvironment VR environment} to which the view manager is attached.
     * @return the {@link VREnvironment VR environment} to which the view manager is attached.
     */
    public VREnvironment getVREnvironment();
    
    /**
     * Initialize the VR view manager. This method should be called after the attachment of a {@link VREnvironment VR environment} to an application.
     */
    public void initialize();
    
    /**
     * Update the VR view manager. 
     * This method is called by the attached {@link VRAppState app state} and should not be called manually.
     * @param tpf the time per frame.
     */
    public void update(float tpf);
    
    /**
     * This method contains action to be done during the rendering phase. 
     * This method should be called for example from the {@link com.jme3.app.state.AppState#render(com.jme3.renderer.RenderManager) render} method of an {@link com.jme3.app.state.AppState app state}.
     * @see #postRender()
     */
    public void render();
    
    /**
     * Send the rendering result as textures to the two eyes. 
     * This method should be called after all the rendering operations 
     * (for example at the end of the {@link AppState#postRender() postRender()} method of the attached app state.)
     * @see #preRender()
     */
    public void postRender();
    
    /**
     * Handles moving filters from the main view to each eye.
     */
    public void moveScreenProcessingToEyes();
}
