/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.core.editor;

import com.jme3.gde.core.assets.AssetData;
import com.jme3.gde.core.assets.SpatialAssetDataObject;
import com.jme3.light.DirectionalLight;
import com.jme3.scene.Spatial;
import com.jme3.system.awt.AwtPanel;
import java.util.concurrent.Callable;
import org.openide.cookies.CloseCookie;
import org.openide.cookies.OpenCookie;
import org.openide.loaders.OpenSupport;
import org.openide.windows.CloneableTopComponent;

/**
 *
 * @author normenhansen
 */
public class SpatialAssetOpenSupport extends OpenSupport implements OpenCookie, CloseCookie {

    SpatialAssetDataObject dataObject;
    SceneApplication app;
    SceneEditorTopComponent tc;

    public SpatialAssetOpenSupport(SpatialAssetDataObject dataObject) {
        super(dataObject.getPrimaryEntry());
        this.dataObject = dataObject;
    }

    protected CloneableTopComponent createCloneableTopComponent() {
        if (tc == null) {
            tc = new SceneEditorTopComponent();
        }
        return tc;
    }

    @Override
    public void open() {
        super.open();
        if (app == null) {
            app = new SceneApplication();
            tc.setDataObject(dataObject);
            Thread t = new Thread(new Runnable() {
                public void run() {
                    app.start();
                    //enqueue to wait until started
                    app.enqueue(new Callable<Void>() {
                        public Void call() throws Exception {
                            //run on EDT
                            java.awt.EventQueue.invokeLater(new Runnable() {
                                public void run() {
                                    AwtPanel panel = app.getPanel();
                                    tc.getScenePanel().add(panel);
                                    tc.repaint();
                                }
                            });
                            Spatial spat = (Spatial) dataObject.getLookup().lookup(AssetData.class).loadAsset();
                            if (spat != null) {
                                app.getRootNode().attachChild(spat);
                                app.getRootNode().addLight(new DirectionalLight());
                            }
                            dataObject.getLookupContents().add(app);
//                            progressHandle.finish();
                            return null;
                        }
                    });
                }
            });
            t.start();
        }
    }

    @Override
    public boolean close() {
        boolean close = super.close();
        if (close && app != null) {
            app.stop();
            java.awt.EventQueue.invokeLater(new Runnable() {
                public void run() {
                    tc.getScenePanel().removeAll();
                }
            });
            dataObject.getLookupContents().remove(app);
            app = null;
        }
        return close;
    }

    @Override
    protected boolean canClose() {
        return super.canClose();
    }
    
}
