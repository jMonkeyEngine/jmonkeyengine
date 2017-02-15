package com.jme3.util;

import com.jme3.app.VREnvironment;
import com.jme3.post.CartoonSSAO;
import com.jme3.post.Filter;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.FilterUtil;
import com.jme3.post.SceneProcessor;
import com.jme3.post.filters.FogFilter;
import com.jme3.post.filters.TranslucentBucketFilter;
import com.jme3.post.ssao.SSAOFilter;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.shadow.VRDirectionalLightShadowRenderer;
import com.jme3.texture.Texture2D;

/**
 * A VR view manager. This class holds methods that enable to submit 3D views to the VR compositor. 
 * System dependent classes should extends from this one.
 * @author Julien Seinturier - (c) 2016 - JOrigin project - <a href="http://www.jorigin.org">http:/www.jorigin.org</a>
 */
public abstract class AbstractVRViewManager implements VRViewManager {

	
	//private static final Logger logger = Logger.getLogger(AbstractVRViewManager.class.getName());

    protected  VREnvironment environment = null;
    
    protected Camera leftCamera;
    protected ViewPort leftViewport;
    protected FilterPostProcessor leftPostProcessor;
    protected Texture2D leftEyeTexture;
    protected Texture2D leftEyeDepth;
    
    protected Camera rightCamera;
    protected ViewPort rightViewport;
    protected FilterPostProcessor rightPostProcessor;
    protected Texture2D rightEyeTexture;
    protected Texture2D rightEyeDepth;
	
    private float resMult = 1f;
    
    private float heightAdjustment;
    
    @Override
    public Camera getLeftCamera() {
        return leftCamera;
    }
    
    @Override
    public Camera getRightCamera() {
        return rightCamera;
    }
    
    @Override
    public ViewPort getLeftViewport() {
        return leftViewport;
    }
    
    @Override
    public ViewPort getRightViewport() {
        return rightViewport;
    }
    
    @Override
    public Texture2D getLeftTexture(){
      return leftEyeTexture;
    }
    
    @Override
    public Texture2D getRightTexture(){
      return rightEyeTexture;
    }
    
    @Override
    public Texture2D getLeftDepth(){
      return leftEyeDepth;
    }
    
    @Override
    public Texture2D getRightDepth(){
      return rightEyeDepth;
    }
    
    @Override
    public FilterPostProcessor getLeftPostProcessor(){
      return leftPostProcessor;
    }
    
    @Override
    public FilterPostProcessor getRightPostProcessor(){
      return rightPostProcessor;
    }
    
    @Override
    public float getResolutionMuliplier() {
        return resMult;
    }
    
    @Override
    public void setResolutionMultiplier(float resMult) {
        this.resMult = resMult;
    }
    
    @Override
    public float getHeightAdjustment() {
        return heightAdjustment;
    }
    
    @Override
    public void setHeightAdjustment(float amount) {
        heightAdjustment = amount;
    }
    
    @Override
    public VREnvironment getVREnvironment(){
    	return environment;
    }
    
    /**
     * Handles moving filters from the main view to each eye
     */
    public void moveScreenProcessingToEyes() {
    	
    	if (environment != null){
            if( getRightViewport() == null ){
            	return;
            }
            
            if (environment.getApplication() != null){
                syncScreenProcessing(environment.getApplication().getViewPort());
                environment.getApplication().getViewPort().clearProcessors();
            } else {
            	throw new IllegalStateException("The VR environment is not attached to any application.");
            }
            

    	} else {
    		throw new IllegalStateException("This VR view manager is not attached to any VR environment.");
    	}
    	

    }
    
    /**
     * Sets the two views to use the list of {@link SceneProcessor processors}.
     * @param sourceViewport the {@link ViewPort viewport} that contains the processors to use.
     */
    public void syncScreenProcessing(ViewPort sourceViewport) {
    	
    	if (environment != null){
    		if(  getRightViewport() == null ){
    			return;
    		}
    		
    		if (environment.getApplication() != null){
    			// setup post processing filters
                if( getRightPostProcessor() == null ) {
                    rightPostProcessor = new FilterPostProcessor(environment.getApplication().getAssetManager());               
                    leftPostProcessor =  new FilterPostProcessor(environment.getApplication().getAssetManager());
                }
                // clear out all filters & processors, to start from scratch
                getRightPostProcessor().removeAllFilters();
                getLeftPostProcessor().removeAllFilters();
                getLeftViewport().clearProcessors();
                getRightViewport().clearProcessors();
                // if we have no processors to sync, don't add the FilterPostProcessor
                if( sourceViewport.getProcessors().isEmpty() ) return;
                // add post processors we just made, which are empty
                getLeftViewport().addProcessor(getLeftPostProcessor());
                getRightViewport().addProcessor(getRightPostProcessor());
                // go through all of the filters in the processors list
                // add them to the left viewport processor & clone them to the right
                for(SceneProcessor sceneProcessor : sourceViewport.getProcessors()) {
                    if (sceneProcessor instanceof FilterPostProcessor) {
                        for(Filter f : ((FilterPostProcessor)sceneProcessor).getFilterList() ) {
                            if( f instanceof TranslucentBucketFilter ) {
                                // just remove this filter, we will add it at the end manually
                                ((FilterPostProcessor)sceneProcessor).removeFilter(f);
                            } else {
                            	getLeftPostProcessor().addFilter(f);
                                // clone to the right
                                Filter f2;
                                if(f instanceof FogFilter){
                                    f2 = FilterUtil.cloneFogFilter((FogFilter)f); 
                                } else if (f instanceof CartoonSSAO ) {
                                    f2 = new CartoonSSAO((CartoonSSAO)f);
                                } else if (f instanceof SSAOFilter){
                                    f2 = FilterUtil.cloneSSAOFilter((SSAOFilter)f);
                                } else if (f instanceof DirectionalLightShadowFilter){
                                    f2 = FilterUtil.cloneDirectionalLightShadowFilter(environment.getApplication().getAssetManager(), (DirectionalLightShadowFilter)f);
                                } else {
                                    f2 = f; // dof, bloom, lightscattering etc.
                                }                    
                                getRightPostProcessor().addFilter(f2);
                            }
                        }
                    } else if (sceneProcessor instanceof VRDirectionalLightShadowRenderer) {
                        // shadow processing
                        // TODO: make right shadow processor use same left shadow maps for performance
                        VRDirectionalLightShadowRenderer dlsr = (VRDirectionalLightShadowRenderer) sceneProcessor;
                        VRDirectionalLightShadowRenderer dlsrRight = dlsr.clone();
                        dlsrRight.setLight(dlsr.getLight());
                        getRightViewport().getProcessors().add(0, dlsrRight);
                        getLeftViewport().getProcessors().add(0, sceneProcessor);
                    }
                }
                // make sure each has a translucent filter renderer
                getLeftPostProcessor().addFilter(new TranslucentBucketFilter());
                getRightPostProcessor().addFilter(new TranslucentBucketFilter());
    		} else {
            	throw new IllegalStateException("The VR environment is not attached to any application.");
            }
    		
            
    	} else {
    		throw new IllegalStateException("This VR view manager is not attached to any VR environment.");
    	}
    	
        
    }
}
