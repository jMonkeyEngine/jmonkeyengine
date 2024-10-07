package com.jme3.post;

import com.jme3.asset.AssetManager;
import com.jme3.post.filters.FogFilter;
import com.jme3.post.ssao.SSAOFilter;
import com.jme3.shadow.DirectionalLightShadowFilter;

/**
 *
 * @author Rickard
 * @deprecated The jme3-vr module is deprecated and will be removed in a future version (as it only supports OpenVR).
 *             For new Virtual Reality projects, use user libraries that provide OpenXR support.
 *             See <a href = "https://wiki.jmonkeyengine.org/docs/3.4/core/vr/virtualreality.html">Virtual Reality JME wiki section</a>
 *             for more information.
 */
@Deprecated
public class FilterUtil {
    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private FilterUtil() {
    }

    public static FogFilter cloneFogFilter(FogFilter fogFilter){
        FogFilter filterClone = new FogFilter();
        filterClone.setFogColor(fogFilter.getFogColor());
        filterClone.setFogDensity(fogFilter.getFogDensity());
        filterClone.setFogDistance(fogFilter.getFogDistance());
        filterClone.setName(fogFilter.getName() + " Clone");
        
        return filterClone;
    }

    public static SSAOFilter cloneSSAOFilter(SSAOFilter filter){
        SSAOFilter clone = new SSAOFilter();
        clone.setSampleRadius(filter.getSampleRadius());
        clone.setIntensity(filter.getIntensity());
        clone.setScale(filter.getScale());
        clone.setBias(filter.getBias());
        return clone;
    }
    
    public static DirectionalLightShadowFilter cloneDirectionalLightShadowFilter(AssetManager assetManager, DirectionalLightShadowFilter filter){
        DirectionalLightShadowFilter clone = new DirectionalLightShadowFilter(assetManager, 512, 3);
        clone.setLight(filter.getLight());
        clone.setLambda(filter.getLambda());
        clone.setShadowIntensity(filter.getShadowIntensity());
        clone.setEdgeFilteringMode(filter.getEdgeFilteringMode());
//        clone.setEnabled(filter.isEnabled());
        return clone;
    }
    
}
