package jme3test.android;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.*;
import java.util.ArrayList;
import java.util.List;

public class DemoMainActivity extends Activity {

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);       
        
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                      
        final Intent myIntent = new Intent(DemoMainActivity.this, DemoAndroidHarness.class);
        
        //Next create the bundle and initialize it
        final Bundle bundle = new Bundle();


        final Spinner spinnerConfig = (Spinner) findViewById(R.id.spinnerConfig);
        ArrayAdapter<CharSequence> adapterDropDownConfig = ArrayAdapter.createFromResource(
                this, R.array.eglconfig_array, android.R.layout.simple_spinner_item);
        adapterDropDownConfig.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerConfig.setAdapter(adapterDropDownConfig);

        
        spinnerConfig.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override           
            public void onItemSelected(AdapterView<?> parent,
                    View view, int pos, long id) {
                  Toast.makeText(parent.getContext(), "Set EGLConfig " +
                      parent.getItemAtPosition(pos).toString(), Toast.LENGTH_LONG).show();
                  //Add the parameters to bundle as
                  bundle.putString("EGLCONFIG", parent.getItemAtPosition(pos).toString()); 
            }

            public void onNothingSelected(AdapterView parent) {
                  // Do nothing.
            }
        });
        
        
        final Spinner spinnerLogging = (Spinner) findViewById(R.id.spinnerLogging);
        ArrayAdapter<CharSequence> adapterDropDownLogging = ArrayAdapter.createFromResource(
                this, R.array.logging_array, android.R.layout.simple_spinner_item);
        adapterDropDownLogging.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLogging.setAdapter(adapterDropDownLogging);

        
        spinnerLogging.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override           
            public void onItemSelected(AdapterView<?> parent,
                    View view, int pos, long id) {
                  Toast.makeText(parent.getContext(), "Set Logging " +
                      parent.getItemAtPosition(pos).toString(), Toast.LENGTH_LONG).show();
                                    
                  //Add the parameters to bundle as
                  bundle.putBoolean("VERBOSE", parent.getItemAtPosition(pos).toString().equals("Verbose"));
            }

            public void onNothingSelected(AdapterView parent) {
                  // Do nothing.
            }
        });
        
        
        ListView list = (ListView) findViewById(R.id.ListView01);
        list.setClickable(true);
 
        final List<DemoLaunchEntry> listDemos = new ArrayList<DemoLaunchEntry>();
        
        listDemos.add(new DemoLaunchEntry("jme3test.android.SimpleTexturedTest", "An field of textured boxes rotating"));
        listDemos.add(new DemoLaunchEntry("jme3test.android.TestSkyLoadingLagoon", "Sky box demonstration with jpg"));
        listDemos.add(new DemoLaunchEntry("jme3test.android.TestSkyLoadingPrimitives", "Sky box demonstration with png"));
        listDemos.add(new DemoLaunchEntry("jme3test.android.TestBumpModel", "Shows a bump mapped well with a moving light"));        
        listDemos.add(new DemoLaunchEntry("jme3test.android.TestNormalMapping", "Shows a normal mapped sphere"));
        listDemos.add(new DemoLaunchEntry("jme3test.android.TestUnshadedModel", "Shows an unshaded model of the sphere"));
        listDemos.add(new DemoLaunchEntry("jme3test.android.TestMovingParticle", "Demonstrates particle effects"));        
        listDemos.add(new DemoLaunchEntry("jme3test.android.TestAmbient", "Positional sound - You sit in a dark cave under a waterfall"));
        
        //listDemos.add(new DemoLaunchEntry("jme3test.effect.TestParticleEmitter", ""));
        //listDemos.add(new DemoLaunchEntry("jme3test.effect.TestPointSprite", ""));
        //listDemos.add(new DemoLaunchEntry("jme3test.light.TestLightRadius", ""));
        listDemos.add(new DemoLaunchEntry("jme3test.android.TestMotionPath", "Shows cinematics - see a teapot on its journey - model loading needs a long time - just let it load, looks like freezed"));
        //listDemos.add(new DemoLaunchEntry("com.jme3.androiddemo.TestSimpleWater", "Post processors - not working correctly due to missing framebuffer support, looks interresting :)"));
        //listDemos.add(new DemoLaunchEntry("jme3test.model.TestHoverTank", ""));
        //listDemos.add(new DemoLaunchEntry("jme3test.niftygui.TestNiftyGui", ""));
        //listDemos.add(new DemoLaunchEntry("com.jme3.androiddemo.TestNiftyGui", ""));

        
        DemoLaunchAdapter adapterList = new DemoLaunchAdapter(this, listDemos);

        list.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View view, int position, long index) {
                System.out.println("onItemClick");                               
                showToast(listDemos.get(position).getName());
                 

                //Add the parameters to bundle as
                bundle.putString("APPCLASSNAME", listDemos.get(position).getName());

                //Add this bundle to the intent
                myIntent.putExtras(bundle);

                //Start the JME3 app harness activity                
                DemoMainActivity.this.startActivity(myIntent);

            }
        });

        list.setAdapter(adapterList);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}

