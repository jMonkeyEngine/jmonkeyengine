package com.jme3.input.android;

import android.view.KeyEvent;
import android.view.MotionEvent;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.TouchEvent;

/**
 * AndroidTouchInputListener is an inputlistener interface which defines
 * callbacks/events for android touch screens For use with class AndroidInput
 *
 * @author larynx
 *
 */
public interface AndroidTouchInputListener extends RawInputListener {

    public void onTouchEvent(TouchEvent evt);

    public void onMotionEvent(MotionEvent evt);

    public void onAndroidKeyEvent(KeyEvent evt);
}
