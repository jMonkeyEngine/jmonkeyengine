package jme3test.input;

import com.jme3.app.SimpleApplication;
import com.jme3.input.JoyInput;
import com.jme3.input.Joystick;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.system.AppSettings;

public class TestJoystick extends SimpleApplication implements AnalogListener, ActionListener {

    public static void main(String[] args){
        TestJoystick app = new TestJoystick();
        AppSettings settings = new AppSettings(true);
        settings.setUseJoysticks(true);
        app.setSettings(settings);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        Joystick[] joysticks = inputManager.getJoysticks();
        if (joysticks == null)
            throw new IllegalStateException("Cannot find any joysticks!");

        for (int i = 0; i < joysticks.length; i++){
            Joystick joy = joysticks[i];
            System.out.println(joy.toString());
            
            joy.assignAxis("Joy Right",  "Joy Left",  joy.getXAxisIndex());
            joy.assignAxis("Joy Down",   "Joy Up",    joy.getYAxisIndex());
            joy.assignAxis("DPAD Right", "DPAD Left", JoyInput.AXIS_POV_X);
            joy.assignAxis("DPAD Up",    "DPAD Down", JoyInput.AXIS_POV_Y);
            joy.assignButton("Button", 0);
        }

        inputManager.addListener(this, "DPAD Left", "DPAD Right", "DPAD Down", "DPAD Up");
        inputManager.addListener(this, "Joy Left", "Joy Right", "Joy Down", "Joy Up");
        inputManager.addListener(this, "Button");
    }

    public void onAnalog(String name, float isPressed, float tpf) {
        System.out.println(name + " = " + isPressed / tpf);
    }

    public void onAction(String name, boolean isPressed, float tpf) {
        System.out.println(name + " = " + isPressed);
    }

}
