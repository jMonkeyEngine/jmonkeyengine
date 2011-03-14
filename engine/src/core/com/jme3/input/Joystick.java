package com.jme3.input;

import com.jme3.input.controls.JoyAxisTrigger;
import com.jme3.input.controls.JoyButtonTrigger;

public final class Joystick {

    private InputManager inputManager;
    private JoyInput joyInput;
    private int joyId;
    private int buttonCount;
    private int axisCount;
    private int axisXIndex, axisYIndex;
    private String name;

    public Joystick(InputManager inputManager, JoyInput joyInput,
                    int joyId, String name, int buttonCount, int axisCount,
                    int xAxis, int yAxis){
        this.inputManager = inputManager;
        this.joyInput = joyInput;
        this.joyId = joyId;
        this.name = name;
        this.buttonCount = buttonCount;
        this.axisCount = axisCount;

        this.axisXIndex = xAxis;
        this.axisYIndex = yAxis;
    }

    public void rumble(float amount){
        joyInput.setJoyRumble(joyId, amount);
    }

    public void assignButton(String mappingName, int buttonId){
        if (buttonId < 0 || buttonId >= buttonCount)
            throw new IllegalArgumentException();

        inputManager.addMapping(mappingName, new JoyButtonTrigger(joyId, buttonId));
    }

    public void assignAxis(String positiveMapping, String negativeMapping, int axisId){
        inputManager.addMapping(positiveMapping, new JoyAxisTrigger(joyId, axisId, false));
        inputManager.addMapping(negativeMapping, new JoyAxisTrigger(joyId, axisId, true));
    }

    public int getXAxisIndex(){
        return axisXIndex;
    }

    public int getYAxisIndex(){
        return axisYIndex;
    }

    public int getAxisCount() {
        return axisCount;
    }

    public int getButtonCount() {
        return buttonCount;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString(){
        return "Joystick[name=" + name + ", id=" + joyId + ", buttons=" + buttonCount
                                + ", axes=" + axisCount + "]";
    }

}
