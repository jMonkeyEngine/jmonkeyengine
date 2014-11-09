package jme3test.android;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import com.jme3.app.AndroidHarness;

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
        
        exitDialogTitle = "Close Demo?";
        exitDialogMessage = "Press Yes";
                        
        screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        
        super.onCreate(savedInstanceState);                
    }

}
