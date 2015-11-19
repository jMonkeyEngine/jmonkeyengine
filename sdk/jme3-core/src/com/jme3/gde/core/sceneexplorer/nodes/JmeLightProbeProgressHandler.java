 
package com.jme3.gde.core.sceneexplorer.nodes;

import com.jme3.environment.generation.JobProgressAdapter;
import com.jme3.light.LightProbe;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;

/**
 *
 * @author Nehon
 */


public class JmeLightProbeProgressHandler extends JobProgressAdapter<LightProbe> {
    
    int lastProgress;
    
    ProgressHandle handle = ProgressHandleFactory.createHandle("Generating environment maps");
    
    @Override
    public void start() {
        handle.start(100);
    }
    
    @Override
    public void progress(double value) {
        lastProgress = (int) (value * 100);
        handle.progress(lastProgress);
    }
    
    @Override
    public void step(String message) {
        handle.progress(message, lastProgress);
    }
    
    @Override
    public void done(LightProbe t) {
        handle.finish();
    }
    
}
