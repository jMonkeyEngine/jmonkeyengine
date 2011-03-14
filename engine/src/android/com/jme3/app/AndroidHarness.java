package com.jme3.app;

import android.app.Activity;
import android.content.res.Resources;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import com.jme3.R;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeSystem;
import com.jme3.system.android.OGLESContext;

/**
 *
 * @author Kirill
 */
public class AndroidHarness extends Activity {

    private OGLESContext ctx;
    private GLSurfaceView view;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        JmeSystem.setResources(getResources());

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN);

        AppSettings settings = new AppSettings(true);

//        String appClass = getResources().getString(R.string.jme3_appclass);
        String appClass = "jme3test.android.Test";
        Application app = null;
        try{
            Class<? extends Application> clazz = (Class<? extends Application>) Class.forName(appClass);
            app = clazz.newInstance();
        }catch (Exception ex){
            ex.printStackTrace();
        }

        app.setSettings(settings);
        app.start();

        ctx = (OGLESContext) app.getContext();
        view = ctx.createView(this);
   		setContentView(view);
    }

    @Override
    protected void onResume() {
        super.onResume();
        view.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        view.onPause();
    }

//    @Override
//    protected void onDestroy(){
//        super.onDestroy();

//        Debug.stopMethodTracing();
//    }

}
