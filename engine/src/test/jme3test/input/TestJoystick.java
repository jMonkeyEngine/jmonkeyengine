package jme3test.input;

import com.jme3.app.SimpleApplication;
import com.jme3.input.JoyInput;
import com.jme3.input.Joystick;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.JoyAxisTrigger;
import com.jme3.system.AppSettings;

public class TestJoystick extends SimpleApplication implements AnalogListener {

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
            
        for (Joystick joy : joysticks){
            System.out.println(joy.toString());
        }

        inputManager.addMapping("DPAD Left", new JoyAxisTrigger(0, JoyInput.AXIS_POV_X, true));
        inputManager.addMapping("DPAD Right", new JoyAxisTrigger(0, JoyInput.AXIS_POV_X, false));
        inputManager.addMapping("DPAD Down", new JoyAxisTrigger(0, JoyInput.AXIS_POV_Y, true));
        inputManager.addMapping("DPAD Up", new JoyAxisTrigger(0, JoyInput.AXIS_POV_Y, false));
        inputManager.addListener(this, "DPAD Left", "DPAD Right", "DPAD Down", "DPAD Up");

        inputManager.addMapping("Joy Left", new JoyAxisTrigger(0, 0, true));
        inputManager.addMapping("Joy Right", new JoyAxisTrigger(0, 0, false));
        inputManager.addMapping("Joy Down", new JoyAxisTrigger(0, 1, true));
        inputManager.addMapping("Joy Up", new JoyAxisTrigger(0, 1, false));
        inputManager.addListener(this, "Joy Left", "Joy Right", "Joy Down", "Joy Up");
    }

    public void onAnalog(String name, float isPressed, float tpf) {
        System.out.println(name + " = " + isPressed);
    }

    public void onAction(String name, boolean isPressed, float tpf) {
        System.out.println(name + " = " + isPressed);
    }

}
