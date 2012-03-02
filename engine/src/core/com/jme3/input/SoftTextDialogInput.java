package com.jme3.input;

import com.jme3.input.controls.SoftTextDialogInputListener;

public interface SoftTextDialogInput {

    public static int TEXT_ENTRY_DIALOG = 0;
    public static int NUMERIC_ENTRY_DIALOG = 1;
    public static int NUMERIC_KEYPAD_DIALOG = 2;

    public void requestDialog(int id, String title, String initialValue, SoftTextDialogInputListener listener);

}
