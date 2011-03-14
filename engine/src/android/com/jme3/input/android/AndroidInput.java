package com.jme3.input.android;

import java.util.List;
import java.util.ArrayList;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;

public class AndroidInput extends GLSurfaceView implements KeyInput, MouseInput {
    
    private RawInputListener listener;
    private int lastX = -1, lastY = -1;

    private static final char[] ANDROID_TO_JME_CHR = {
        0x0,// unknown
        0x0,// soft left
        0x0,// soft right
        0x0,// home
        0x0,// back
        0x0,// call
        0x0,// endcall
        '0',
        '1',
        '2',
        '3',
        '4',
        '5',
        '6',
        '7',
        '8',
        '9',
        '*',
        '#',
        0x0,//dpad_up
        0x0,//dpad_down
        0x0,//dpad_left
        0x0,//dpad_right
        0x0,//dpad_center
        0x0,//volume up
        0x0,//volume down
        0x0,//power
        0x0,//camera
        0x0,//clear
        'a',
        'b',
        'c',
        'd',
        'e',
        'f',
        'g',
        'h',
        'i',
        'j',
        'k',
        'l',
        'm',
        'n',
        'o',
        'p',
        'q',
        'r',
        's',
        't',
        'u',
        'v',
        'w',
        'x',
        'y',
        'z',
        ',',
        '.',

        0x0,//left alt
        0x0,//right alt
        0x0,//left ctrl
        0x0,//right ctrl

//        0x0,//fn
//        0x0,//cap

        '\t',
        ' ',
        0x0,//sym(bol)
        0x0,//explorer
        0x0,//envelope
        '\n',//newline
        0x0,//delete
        '`',
        '-',
        '=',
        '[',
        ']',
        '\\',//backslash
        ';',
        '\'',//apostrophe
        '/',//slash
        '@',//at
        0x0,//num
        0x0,//headset hook
        0x0,//focus
        0x0,
        0x0,//menu
        0x0,//notification
        0x0,//search
        0x0,//media play/pause
        0x0,//media stop
        0x0,//media next
        0x0,//media previous
        0x0,//media rewind
        0x0,//media fastforward
        0x0,//mute
    };

    private static final int[] ANDROID_TO_JME = {
        0x0, // unknown
        0x0, // key code soft left
        0x0, // key code soft right
        KeyInput.KEY_HOME,
        KeyInput.KEY_ESCAPE, // key back
        0x0, // key call
        0x0, // key endcall
        KeyInput.KEY_0,
        KeyInput.KEY_1,
        KeyInput.KEY_2,
        KeyInput.KEY_3,
        KeyInput.KEY_4,
        KeyInput.KEY_5,
        KeyInput.KEY_6,
        KeyInput.KEY_7,
        KeyInput.KEY_8,
        KeyInput.KEY_9,
        KeyInput.KEY_MULTIPLY,
        0x0, // key pound
        KeyInput.KEY_UP,
        KeyInput.KEY_DOWN,
        KeyInput.KEY_LEFT,
        KeyInput.KEY_RIGHT,
        KeyInput.KEY_RETURN, // dpad center
        0x0, // volume up
        0x0, // volume down
        KeyInput.KEY_POWER, // power (?)
        0x0, // camera
        0x0, // clear
        KeyInput.KEY_A,
        KeyInput.KEY_B,
        KeyInput.KEY_C,
        KeyInput.KEY_D,
        KeyInput.KEY_E,
        KeyInput.KEY_F,
        KeyInput.KEY_G,
        KeyInput.KEY_H,
        KeyInput.KEY_I,
        KeyInput.KEY_J,
        KeyInput.KEY_K,
        KeyInput.KEY_L,
        KeyInput.KEY_M,
        KeyInput.KEY_N,
        KeyInput.KEY_O,
        KeyInput.KEY_P,
        KeyInput.KEY_Q,
        KeyInput.KEY_R,
        KeyInput.KEY_S,
        KeyInput.KEY_T,
        KeyInput.KEY_U,
        KeyInput.KEY_V,
        KeyInput.KEY_W,
        KeyInput.KEY_X,
        KeyInput.KEY_Y,
        KeyInput.KEY_Z,
        KeyInput.KEY_COMMA,
        KeyInput.KEY_PERIOD,

        KeyInput.KEY_LMENU,
        KeyInput.KEY_RMENU,

        KeyInput.KEY_LSHIFT,
        KeyInput.KEY_RSHIFT,

//        0x0, // fn
//        0x0, // cap (?)

        KeyInput.KEY_TAB,
        KeyInput.KEY_SPACE,
        0x0, // sym (?) symbol
        0x0, // explorer
        0x0, // envelope
        KeyInput.KEY_RETURN, // newline/enter
        KeyInput.KEY_DELETE,
        KeyInput.KEY_GRAVE,
        KeyInput.KEY_MINUS,
        KeyInput.KEY_EQUALS,

        KeyInput.KEY_LBRACKET,
        KeyInput.KEY_RBRACKET,

        KeyInput.KEY_BACKSLASH,
        KeyInput.KEY_SEMICOLON,
        KeyInput.KEY_APOSTROPHE,
        KeyInput.KEY_SLASH,
        KeyInput.KEY_AT, // at (@)
        KeyInput.KEY_NUMLOCK, //0x0, // num
        0x0, //headset hook
        0x0, //focus
        KeyInput.KEY_ADD,
        KeyInput.KEY_LMETA, //menu
        0x0,//notification
        0x0,//search
        0x0,//media play/pause
        0x0,//media stop
        0x0,//media next
        0x0,//media previous
        0x0,//media rewind
        0x0,//media fastforward
        0x0,//mute
    };

//    private int[] keyMap = {
//        0x0,
//        KeyEvent.KEYCODE_BACK, // ESC key
//
//        KeyEvent.KEYCODE_1,
//        KeyEvent.KEYCODE_2,
//        KeyEvent.KEYCODE_3,
//        KeyEvent.KEYCODE_4,
//        KeyEvent.KEYCODE_5,
//        KeyEvent.KEYCODE_6,
//        KeyEvent.KEYCODE_7,
//        KeyEvent.KEYCODE_8,
//        KeyEvent.KEYCODE_9,
//        KeyEvent.KEYCODE_0,
//        KeyEvent.KEYCODE_MINUS,
//        KeyEvent.KEYCODE_EQUALS,
//        KeyEvent.KEYCODE_BACK,
//        KeyEvent.KEYCODE_TAB,
//        KeyEvent.KEYCODE_Q,
//        KeyEvent.KEYCODE_W,
//        KeyEvent.KEYCODE_E,
//        KeyEvent.KEYCODE_R,
//        KeyEvent.KEYCODE_T,
//        KeyEvent.KEYCODE_Y,
//        KeyEvent.KEYCODE_U,
//        KeyEvent.KEYCODE_I,
//        KeyEvent.KEYCODE_O,
//        KeyEvent.KEYCODE_P,
//        KeyEvent.KEYCODE_LEFT_BRACKET,
//        KeyEvent.KEYCODE_RIGHT_BRACKET,
//        KeyEvent.KEYCODE_ENTER,
//        KeyEvent.KEYCODE_SOFT_LEFT, // Left Ctrl
//        KeyEvent.KEYCODE_A,
//        KeyEvent.KEYCODE_S,
//        KeyEvent.KEYCODE_D,
//        KeyEvent.KEYCODE_F,
//        KeyEvent.KEYCODE_G,
//        KeyEvent.KEYCODE_H,
//        KeyEvent.KEYCODE_J,
//        KeyEvent.KEYCODE_K,
//        KeyEvent.KEYCODE_L,
//        KeyEvent.KEYCODE_SEMICOLON,
//        KeyEvent.KEYCODE_APOSTROPHE,
//        KeyEvent.KEYCODE_GRAVE,
//        KeyEvent.KEYCODE_SHIFT_LEFT,
//        KeyEvent.KEYCODE_BACKSLASH,
//        KeyEvent.KEYCODE_Z,
//        KeyEvent.KEYCODE_X,
//        KeyEvent.KEYCODE_C,
//        KeyEvent.KEYCODE_V,
//        KeyEvent.KEYCODE_B,
//        KeyEvent.KEYCODE_N,
//        KeyEvent.KEYCODE_M,
//
//        KeyEvent.KEYCODE_COMMA,
//        KeyEvent.KEYCODE_PERIOD,
//        KeyEvent.KEYCODE_SLASH,
//        KeyEvent.KEYCODE_SHIFT_RIGHT,
//        KeyEvent.KEYCODE_STAR,
//
//        KeyEvent.KEYCODE_ALT_LEFT,
//        KeyEvent.KEYCODE_SPACE,
//
//        0x0, // no caps lock
//
//        0x0, // F1
//        0x0, // F2
//        0x0, // F3
//        0x0, // F4
//        0x0, // F5
//        0x0, // F6
//        0x0, // F7
//        0x0, // F8
//        0x0, // F9
//        0x0, // F10
//
//        KeyEvent.KEYCODE_NUM,
//        0x0, // scroll lock
//
//        0x0, // numpad7
//        0x0, // numpad8
//        0x0, // numpad9
//
//        KeyEvent.
//    }

    public AndroidInput(Context ctx, AttributeSet attribs){
        super(ctx, attribs);
    }

    public AndroidInput(Context ctx){
        super(ctx);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent){
	int newX = getWidth() - (int) motionEvent.getX();
	int newY = (int) motionEvent.getY();


        switch (motionEvent.getAction()){
            case MotionEvent.ACTION_DOWN:
                MouseButtonEvent btn = new MouseButtonEvent(0, true, newX, newY);
                btn.setTime(motionEvent.getEventTime());
		processEvent(btn);
               // listener.onMouseButtonEvent(btn);
                lastX = -1;
                lastY = -1;
                return true;
            case MotionEvent.ACTION_UP:
                MouseButtonEvent btn2 = new MouseButtonEvent(0, false, newX, newY);
                btn2.setTime(motionEvent.getEventTime());
		processEvent(btn2);
               // listener.onMouseButtonEvent(btn2);
                lastX = -1;
                lastY = -1;
                return true;
            case MotionEvent.ACTION_MOVE:
               // int newX = getWidth() - (int) motionEvent.getX();
               // int newY = (int) motionEvent.getY();
                int dx;
                int dy;
                if (lastX != -1){
                    dx = newX - lastX;
                    dy = newY - lastY;
                }else{
                    dx = 0;
                    dy = 0;
                }
                lastX = newX;
                lastY = newY;
                MouseMotionEvent mot = new MouseMotionEvent(newX, newY, dx, dy, 0, 0);
                mot.setTime(motionEvent.getEventTime());
		processEvent(mot);
                //listener.onMouseMotionEvent(mot);
                try{
                    Thread.sleep(15);
                } catch (InterruptedException ex) {
                }
                return true;
        }
        return false;
    }

    @Override
    public boolean onKeyDown (int keyCode, KeyEvent event) {
        int jmeCode  = ANDROID_TO_JME[keyCode];
        String str =  event.getCharacters();
        char c = str != null && str.length() > 0 ? str.charAt(0) : 0x0;
        KeyInputEvent evt = new KeyInputEvent(jmeCode, c, true, false);
	processEvent(evt);
	//     listener.onKeyEvent(evt);
        return false;
    }

    @Override
    public boolean onKeyUp (int keyCode, KeyEvent event) {
        int jmeCode  = ANDROID_TO_JME[keyCode];
        String str =  event.getCharacters();
        char c = str != null && str.length() > 0 ? str.charAt(0) : 0x0;
        KeyInputEvent evt = new KeyInputEvent(jmeCode, c, false, false);
	processEvent(evt);
        //listener.onKeyEvent(evt);
        return false;
    }

    public void setCursorVisible(boolean visible){
    }

    public int getButtonCount(){
        return 255;
    }

    public void initialize() {
    }

    public void update() {
		generateEvents();
    }

    public void destroy() {
    }

    public boolean isInitialized() {
        return true;
    }

	// XXX: android does not have an Event interface?
	private List<Object> currentEvents = new ArrayList<Object>();

	private final static int MAX_EVENTS = 1024;

	private void processEvent(Object event) {
		synchronized (currentEvents) {
			if (currentEvents.size() < MAX_EVENTS)
				currentEvents.add(event);
		}
	}

	private void generateEvents() {
		synchronized (currentEvents) {
			for (Object event: currentEvents) {
				if (event instanceof MouseButtonEvent) {
              				listener.onMouseButtonEvent((MouseButtonEvent) event);
				} else if (event instanceof MouseMotionEvent) {
					listener.onMouseMotionEvent((MouseMotionEvent) event);
				} else if (event instanceof KeyInputEvent) {
					listener.onKeyEvent((KeyInputEvent) event);
				}
			}
			currentEvents.clear();
		}
	}

    public void setInputListener(RawInputListener listener) {
        this.listener = listener;
    }

    public long getInputTimeNanos() {
        return System.nanoTime();
    }

}
