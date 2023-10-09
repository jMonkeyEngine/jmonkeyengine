package com.jme3.renderer.renderPass;

import com.jme3.light.LightList;
import com.jme3.renderer.framegraph.FGBindable;
import com.jme3.renderer.framegraph.FGSource;

public class DeferredLightDataSource extends FGSource {
    DeferredLightDataProxy deferredLightDataProxy;
    public DeferredLightDataSource(String name, LightList lightData) {
        super(name);
        deferredLightDataProxy = new DeferredLightDataProxy(lightData);
    }

    @Override
    public void postLinkValidate() {

    }

    @Override
    public FGBindable yieldBindable() {
        return deferredLightDataProxy;
    }

    public static class DeferredLightDataProxy extends FGBindable {
        private LightList lightData;

        public DeferredLightDataProxy(LightList lightData) {
            this.lightData = lightData;
        }

        public LightList getLightData() {
            return lightData;
        }
    }

}
