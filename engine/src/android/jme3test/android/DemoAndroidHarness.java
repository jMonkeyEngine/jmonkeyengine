package jme3test.android;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import com.jme3.app.AndroidHarness;
import com.jme3.system.android.AndroidConfigChooser.ConfigType;

public class DemoAndroidHarness extends AndroidHarness
{
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {        
        // Set the application class to run
        // First Extract the bundle from intent
        Bundle bundle = getIntent().getExtras();

        //Next extract the values using the key as
        appClass = bundle.getString("APPCLASSNAME");                
        
        
        String eglConfig = bundle.getString("EGLCONFIG");
        if (eglConfig.equals("Best"))
        {
            eglConfigType = ConfigType.BEST;
        }
        else if (eglConfig.equals("Legacy"))
        {
            eglConfigType = ConfigType.LEGACY;
        }
        else
        {
            eglConfigType = ConfigType.FASTEST;    
        }
        
        
        if (bundle.getBoolean("VERBOSE"))
        {
            eglConfigVerboseLogging = true;
        }
        else
        {
            eglConfigVerboseLogging = false;
        }
        
        
        exitDialogTitle = "Close Demo?";
        exitDialogMessage = "Press Yes";
                        
        screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        
        super.onCreate(savedInstanceState);                
    }

}
