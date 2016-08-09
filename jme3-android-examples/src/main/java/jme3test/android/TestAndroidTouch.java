package jme3test.android;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.font.Rectangle;
import com.jme3.input.*;
import com.jme3.input.controls.*;
import com.jme3.input.event.*;
import com.jme3.math.ColorRGBA;
import com.jme3.ui.Picture;

import java.text.NumberFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Test for Android Touch Input integration into jME3.
 *
 * @author iwgeric
 */
public class TestAndroidTouch extends SimpleApplication {
    private static final Logger logger = Logger.getLogger(TestAndroidTouch.class.getSimpleName());

    private Picture picMouseBackground;
    private Picture picMouseLeftButton;
    private Picture picMouseDisabled;
    private BitmapText textMouseAnalog;
    private BitmapText textMouseLabel;
    private BitmapText textMouseLocation;
    private BitmapText textCursorLocation;
    private BitmapText textKeyPressed;
    private BitmapText textPhoneLabel;
    private BitmapText textPhoneLocation;

    private Picture picPhone;

    private String touchMapping = "touch";

    private String mappingKeyPrefix = "key-";
    private String mappingMouseLeft = "mouse left";
    private String mappingMouseXNeg = "mouse x neg";
    private String mappingMouseXPos = "mouse x pos";
    private String mappingMouseYNeg = "mouse y neg";
    private String mappingMouseYPos = "mouse y pos";

    private TouchListener touchListener = new MyTouchListener();
    private ActionListener actionListener = new MyActionListener();
    private AnalogListener analogListener = new MyAnalogListener();
    private RawInputListener rawInputListener = new MyRawInputListener();

    private NumberFormat analogFormat = NumberFormat.getNumberInstance();
    private NumberFormat locationFormat = NumberFormat.getNumberInstance();

    @Override
    public void simpleInitApp() {

        getViewPort().setBackgroundColor(ColorRGBA.White);

        analogFormat.setMaximumFractionDigits(3);
        analogFormat.setMinimumFractionDigits(3);
        locationFormat.setMaximumFractionDigits(0);
        locationFormat.setMinimumFractionDigits(0);

        // Setup list of triggers based on different keyboard key codes. For Android, the soft keyboard key events
        // are translated into jme key events.
        int[] keyCodes = new int[] {
                KeyInput.KEY_0, KeyInput.KEY_1, KeyInput.KEY_2, KeyInput.KEY_3, KeyInput.KEY_4, KeyInput.KEY_5,
                KeyInput.KEY_6, KeyInput.KEY_7, KeyInput.KEY_8, KeyInput.KEY_9, KeyInput.KEY_DECIMAL, KeyInput.KEY_PERIOD,
                KeyInput.KEY_A, KeyInput.KEY_B, KeyInput.KEY_C, KeyInput.KEY_D, KeyInput.KEY_E, KeyInput.KEY_F,
                KeyInput.KEY_G, KeyInput.KEY_H, KeyInput.KEY_I, KeyInput.KEY_J, KeyInput.KEY_K, KeyInput.KEY_L,
                KeyInput.KEY_M, KeyInput.KEY_N, KeyInput.KEY_O, KeyInput.KEY_P, KeyInput.KEY_Q, KeyInput.KEY_R,
                KeyInput.KEY_S, KeyInput.KEY_T, KeyInput.KEY_U, KeyInput.KEY_V, KeyInput.KEY_W, KeyInput.KEY_X,
                KeyInput.KEY_Y, KeyInput.KEY_Z, KeyInput.KEY_CAPITAL, KeyInput.KEY_LSHIFT, KeyInput.KEY_RSHIFT,
                KeyInput.KEY_UP, KeyInput.KEY_DOWN, KeyInput.KEY_LEFT, KeyInput.KEY_RIGHT
        };

        for (int idx=0; idx<keyCodes.length; idx++) {
            String keyMapping = mappingKeyPrefix + KeyNames.getName(keyCodes[idx]);
            inputManager.addMapping(keyMapping, new KeyTrigger(keyCodes[idx]));
            inputManager.addListener(actionListener, keyMapping);
            logger.log(Level.INFO, "Adding key mapping: {0}", keyMapping);
        }

        // setup InputManager to trigger our listeners when the various triggers are received.

        // Touch inputs are all sent to the TouchTrigger.  To have one mapping for all touch events, use TouchInput.ALL.
        inputManager.addMapping(touchMapping, new TouchTrigger(TouchInput.ALL));
        inputManager.addListener(touchListener, touchMapping);

        // If inputManager.isSimulateMouse = true, touch events will be translated into Mouse Button and Axis events.
        // To enable this, call inputManager.setSimulateMouse(true).
        inputManager.addMapping(mappingMouseLeft, new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(actionListener, mappingMouseLeft);

        inputManager.addMapping(mappingMouseXNeg, new MouseAxisTrigger(MouseInput.AXIS_X, true));
        inputManager.addMapping(mappingMouseXPos, new MouseAxisTrigger(MouseInput.AXIS_X, false));
        inputManager.addMapping(mappingMouseYNeg, new MouseAxisTrigger(MouseInput.AXIS_Y, true));
        inputManager.addMapping(mappingMouseYPos, new MouseAxisTrigger(MouseInput.AXIS_Y, false));
        inputManager.addListener(analogListener, mappingMouseXNeg, mappingMouseXPos, mappingMouseYNeg, mappingMouseYPos);

        // add raw input listener to inputManager
        inputManager.addRawInputListener(rawInputListener);

        float mouseSize = (settings.getWidth() >= settings.getHeight())? settings.getHeight()/2f: settings.getWidth()/2f;

        picMouseBackground = new Picture("Mouse Background");
        picMouseBackground.setImage(assetManager, "mouse_none.png", true);
        picMouseBackground.setWidth(mouseSize);
        picMouseBackground.setHeight(mouseSize);
        picMouseBackground.setLocalTranslation(settings.getWidth()-mouseSize, 0f, 0f);

        picMouseLeftButton = new Picture("Mouse Button Left");
        picMouseLeftButton.setImage(assetManager, "mouse_left.png", true);
        picMouseLeftButton.setWidth(mouseSize);
        picMouseLeftButton.setHeight(mouseSize);
        picMouseLeftButton.setLocalTranslation(settings.getWidth()-mouseSize, 0f, 1f);

        picMouseDisabled = new Picture("Mouse Disabled");
        picMouseDisabled.setImage(assetManager, "mouse_disabled.png", true);
        picMouseDisabled.setWidth(mouseSize);
        picMouseDisabled.setHeight(mouseSize);
        picMouseDisabled.setLocalTranslation(settings.getWidth()-mouseSize, 0f, 1f);

        float phoneSize = (settings.getWidth() >= settings.getHeight())? settings.getHeight()/2f: settings.getWidth()/2f;

        // preload images to send data to gpu to avoid hesitations during run time the first time the image is displayed
        renderManager.preloadScene(picMouseBackground);
        renderManager.preloadScene(picMouseLeftButton);
        renderManager.preloadScene(picMouseDisabled);

        guiNode.attachChild(picMouseBackground);
        if (inputManager.isSimulateMouse()) {
            picMouseDisabled.removeFromParent();
        } else {
            guiNode.attachChild(picMouseDisabled);
        }

        textMouseLabel = new BitmapText(guiFont, false);
        textMouseLabel.setSize(mouseSize/10f);
        textMouseLabel.setColor(ColorRGBA.Blue);
        textMouseLabel.setBox(new Rectangle(0f, 0f, mouseSize, mouseSize/5f));
        textMouseLabel.setAlignment(BitmapFont.Align.Center);
        textMouseLabel.setVerticalAlignment(BitmapFont.VAlign.Bottom);
        textMouseLabel.setText("Mouse Analog\nand Position");
        textMouseLabel.setLocalTranslation(settings.getWidth()-mouseSize, mouseSize*1.25f, 1f);
        guiNode.attachChild(textMouseLabel);

        textMouseAnalog = new BitmapText(guiFont, false);
        textMouseAnalog.setSize(mouseSize/10f);
        textMouseAnalog.setColor(ColorRGBA.Blue);
        textMouseAnalog.setBox(new Rectangle(0f, 0f, mouseSize, mouseSize/10f));
        textMouseAnalog.setAlignment(BitmapFont.Align.Center);
        textMouseAnalog.setVerticalAlignment(BitmapFont.VAlign.Center);
        textMouseAnalog.setText("0.000, 0.000");
        textMouseAnalog.setLocalTranslation(settings.getWidth()-mouseSize, mouseSize/2f, 2f);
        guiNode.attachChild(textMouseAnalog);

        textMouseLocation = new BitmapText(guiFont, false);
        textMouseLocation.setSize(mouseSize/10f);
        textMouseLocation.setColor(ColorRGBA.Blue);
        textMouseLocation.setBox(new Rectangle(0f, 0f, mouseSize, mouseSize/10f));
        textMouseLocation.setAlignment(BitmapFont.Align.Center);
        textMouseLocation.setVerticalAlignment(BitmapFont.VAlign.Center);
        textMouseLocation.setText("0, 0");
        textMouseLocation.setLocalTranslation(settings.getWidth()-mouseSize, mouseSize/2f-mouseSize/10f, 2f);
        guiNode.attachChild(textMouseLocation);

        textCursorLocation = new BitmapText(guiFont, false);
        textCursorLocation.setSize(mouseSize/10f);
        textCursorLocation.setColor(ColorRGBA.Blue);
        textCursorLocation.setBox(new Rectangle(0f, 0f, mouseSize, mouseSize/10f));
        textCursorLocation.setAlignment(BitmapFont.Align.Center);
        textCursorLocation.setVerticalAlignment(BitmapFont.VAlign.Center);
        textCursorLocation.setText("0, 0");
        textCursorLocation.setLocalTranslation(settings.getWidth()-mouseSize, mouseSize/2f-mouseSize/10f*2f, 2f);
        guiNode.attachChild(textCursorLocation);

        textKeyPressed = new BitmapText(guiFont, false);
        textKeyPressed.setSize(mouseSize/10f);
        textKeyPressed.setColor(ColorRGBA.Blue);
        textKeyPressed.setBox(new Rectangle(0f, 0f, settings.getWidth(), mouseSize/10f));
        textKeyPressed.setAlignment(BitmapFont.Align.Center);
        textKeyPressed.setVerticalAlignment(BitmapFont.VAlign.Top);
        textKeyPressed.setText("Last Key Pressed: None");
        textKeyPressed.setLocalTranslation(0f, settings.getHeight()-mouseSize/10f, 2f);
        guiNode.attachChild(textKeyPressed);

        picPhone = new Picture("Phone");
        picPhone.setImage(assetManager, "phone_landscape.png", true);
        picPhone.setWidth(phoneSize);
        picPhone.setHeight(phoneSize);
        picPhone.setLocalTranslation(picMouseBackground.getLocalTranslation().x - phoneSize, 0f, 1f);
        guiNode.attachChild(picPhone);

        textPhoneLocation = new BitmapText(guiFont, false);
        textPhoneLocation.setSize(phoneSize/10f);
        textPhoneLocation.setColor(ColorRGBA.White);
        textPhoneLocation.setBox(new Rectangle(0f, 0f, phoneSize, phoneSize/10f));
        textPhoneLocation.setAlignment(BitmapFont.Align.Center);
        textPhoneLocation.setVerticalAlignment(BitmapFont.VAlign.Center);
        textPhoneLocation.setText("0, 0");
        textPhoneLocation.setLocalTranslation(picMouseBackground.getLocalTranslation().x - phoneSize, phoneSize*0.5f, 2f);
        guiNode.attachChild(textPhoneLocation);

        textPhoneLabel = new BitmapText(guiFont, false);
        textPhoneLabel.setSize(phoneSize/10f);
        textPhoneLabel.setColor(ColorRGBA.Blue);
        textPhoneLabel.setBox(new Rectangle(0f, 0f, phoneSize, phoneSize/10f));
        textPhoneLabel.setAlignment(BitmapFont.Align.Center);
        textPhoneLabel.setVerticalAlignment(BitmapFont.VAlign.Bottom);
        textPhoneLabel.setText("Touch Location");
        textPhoneLabel.setLocalTranslation(picMouseBackground.getLocalTranslation().x - phoneSize, picPhone.getLocalTranslation().y + phoneSize*0.75f, 1f);
        guiNode.attachChild(textPhoneLabel);

        renderManager.preloadScene(picPhone);
    }

    private class MyTouchListener implements TouchListener {

        @Override
        public void onTouch(String name, TouchEvent event, float tpf) {
            String touchEvent = event.toString();

            logger.log(Level.INFO, "TouchListenerEvent: {0}", touchEvent);

            switch (event.getType()) {
                case DOWN:
                case UP:
                case MOVE:
                case SCROLL:
                    textPhoneLocation.setText(
                            String.valueOf(locationFormat.format(event.getX())) + ", " +
                                    String.valueOf(locationFormat.format(event.getY())));
                    break;
                default:
            }

        }
    }

    protected class MyActionListener implements ActionListener {

        @Override
        public void onAction(String name, boolean isPressed, float tpf) {
            logger.log(Level.INFO, "ActionListenerEvent[name:{0}, pressed: {1}, tpf: {2}",
                    new Object[]{name, isPressed, tpf});

            if (name.equalsIgnoreCase(mappingMouseLeft)) {
                if (isPressed) {
                    guiNode.attachChild(picMouseLeftButton);
                } else {
                    picMouseLeftButton.removeFromParent();
                }
                textCursorLocation.setText(
                        String.valueOf(locationFormat.format(inputManager.getCursorPosition().getX())) + ", " +
                                String.valueOf(locationFormat.format(inputManager.getCursorPosition().getY())));
            } else if (name.startsWith(mappingKeyPrefix)) {
                logger.log(Level.INFO, "key event name: {0}, pressed: {1}", new Object[]{name, isPressed});
                if (isPressed) {
                    textKeyPressed.setText("Last Key Pressed: " + name.substring(mappingKeyPrefix.length(), name.length()));
                } else {
//                    textKeyPressed.setText("Key Pressed: None");
                }
            }
        }

    }


    protected class MyAnalogListener implements AnalogListener {
        float lastValueX = 0;
        float lastValueY = 0;

        @Override
        public void onAnalog(String name, float value, float tpf) {
            logger.log(Level.INFO, "AnalogListenerEvent[name:{0}, value: {1}, tpf: {2}",
                    new Object[]{name, value, tpf});
            if (name.equalsIgnoreCase(mappingMouseXPos)) {
                setValueX(value);
            } else if (name.equalsIgnoreCase(mappingMouseXNeg)) {
                    setValueX(-value);
            } else if (name.equalsIgnoreCase(mappingMouseYPos)) {
                setValueY(value);
            } else if (name.equalsIgnoreCase(mappingMouseYNeg)) {
                setValueY(-value);
            }
        }

        public void setValueX(float x) {
            lastValueX = x;
            textMouseAnalog.setText(
                    String.valueOf(analogFormat.format(lastValueX)) + ", " + String.valueOf(analogFormat.format(lastValueY)));
            textCursorLocation.setText(
                    String.valueOf(locationFormat.format(inputManager.getCursorPosition().getX())) + ", " +
                            String.valueOf(locationFormat.format(inputManager.getCursorPosition().getY())));
        }
        public void setValueY(float y) {
            lastValueY = y;
            textMouseAnalog.setText(
                    String.valueOf(analogFormat.format(lastValueX)) + ", " + String.valueOf(analogFormat.format(lastValueY)));
            textCursorLocation.setText(
                    String.valueOf(locationFormat.format(inputManager.getCursorPosition().getX())) + ", " +
                            String.valueOf(locationFormat.format(inputManager.getCursorPosition().getY())));
        }
    }

    protected class MyRawInputListener implements RawInputListener {

        @Override
        public void beginInput() {
//            logger.log(Level.INFO, "RawInputListenerEvent: BeginInput");
        }

        @Override
        public void endInput() {
//            logger.log(Level.INFO, "RawInputListenerEvent: EndInput");
        }

        @Override
        public void onJoyAxisEvent(JoyAxisEvent event) {
            logger.log(Level.INFO, "RawInputListenerEvent: {0}", event);
        }

        @Override
        public void onJoyButtonEvent(JoyButtonEvent event) {
            logger.log(Level.INFO, "RawInputListenerEvent: {0}", event);
        }

        @Override
        public void onMouseMotionEvent(MouseMotionEvent event) {
            logger.log(Level.INFO, "RawInputListenerEvent: {0}", event);
            textMouseLocation.setText(
                    String.valueOf(locationFormat.format(event.getX())) + ", " + String.valueOf(locationFormat.format(event.getY())));
        }

        @Override
        public void onMouseButtonEvent(MouseButtonEvent event) {
            logger.log(Level.INFO, "RawInputListenerEvent: {0}", event);
            textMouseLocation.setText(
                    String.valueOf(locationFormat.format(event.getX())) + ", " + String.valueOf(locationFormat.format(event.getY())));
        }

        @Override
        public void onKeyEvent(KeyInputEvent event) {
            logger.log(Level.INFO, "RawInputListenerEvent: {0}", event);
        }

        @Override
        public void onTouchEvent(TouchEvent event) {
            logger.log(Level.INFO, "RawInputListenerEvent: {0}", event);
        }
    }
}
