package com.jme3.app;

import java.util.logging.Logger;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import com.jme3.app.Application;
import com.jme3.input.android.AndroidInput;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeSystem;
import com.jme3.system.android.OGLESContext;


/**
 * <code>AndroidHarness</code> wraps a jme application object and runs it on Android
 * @author Kirill
 * @author larynx
 */
public class AndroidHarness extends Activity implements DialogInterface.OnClickListener
{
    protected final static Logger logger = Logger.getLogger(AndroidHarness.class.getName());
    
    protected OGLESContext ctx;
    protected GLSurfaceView view;
    
    protected String appClass = "jme3test.android.Test";
    protected Application app = null;
    
    protected boolean debug = false;

    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);

        JmeSystem.setResources(getResources());

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN);

        AppSettings settings = new AppSettings(true);
        AndroidInput input = new AndroidInput(this);
        
        

        // Create application instance
        try{
            @SuppressWarnings("unchecked")
            Class<? extends Application> clazz = (Class<? extends Application>) Class.forName(appClass);
            app = clazz.newInstance();
        }catch (Exception ex){
            ex.printStackTrace();
        }

        app.setSettings(settings);
        app.start();    
        ctx = (OGLESContext) app.getContext();
        if (debug)
        {
            view = ctx.createView(input, GLSurfaceView.DEBUG_CHECK_GL_ERROR | GLSurfaceView.DEBUG_LOG_GL_CALLS);
        }
        else
        {
            view = ctx.createView(input);
        }
   		setContentView(view);      
    }


    @Override
    protected void onRestart(){
        super.onRestart(); 
        app.restart();
        logger.info("onRestart");
    }
    

    @Override
    protected void onStart(){
        super.onStart();
        logger.info("onStart");
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        view.onResume();
        logger.info("onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        view.onPause();
        logger.info("onPause");
    }
    
    @Override
    protected void onStop(){
        super.onStop();
        logger.info("onStop");
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();        
        app.stop();
        logger.info("onDestroy");
    }

    
    /**
     * Called when an error has occured. This is typically
     * invoked when an uncought exception is thrown in the render thread.
     * @param errorMsg The error message, if any, or null.
     * @param t Throwable object, or null.
     */
    public void handleError(final String errorMsg, final Throwable t)
    {
        
        String s = "";
        if (t != null && t.getStackTrace() != null)
        {
            for (StackTraceElement ste: t.getStackTrace())
            {
                s +=  ste.getClassName() + "." + ste.getMethodName() + "(" + + ste.getLineNumber() + ") ";
            }
        }                
        
        final String sTrace = s;
        
        logger.severe(t != null ? t.toString() : "Failed");
        logger.severe((errorMsg != null ? errorMsg + ": " : "") + sTrace);
        
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() 
            {                                
                AlertDialog dialog = new AlertDialog.Builder(AndroidHarness.this)
               // .setIcon(R.drawable.alert_dialog_icon)
                .setTitle(t != null ? t.toString() : "Failed")
                .setPositiveButton("Kill", AndroidHarness.this)
                .setMessage((errorMsg != null ? errorMsg + ": " : "") + sTrace)
                .create();    
                dialog.show();                
            }
        });
        
    }

    
    /**
     * Called by the android alert dialog, terminate the activity and OpenGL rendering
     * @param dialog
     * @param whichButton
     */
    public void onClick(DialogInterface dialog, int whichButton) 
    {
        app.stop();
        this.finish();
    }
}
