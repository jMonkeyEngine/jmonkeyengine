package jme3test.app.state.testsimpleappstate;

import com.jme3.app.state.SimpleAppState;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;

public class ExampleSimpleAppStateInput extends SimpleAppState<TestSimpleAppState> implements ActionListener{

    @Override
    protected void onInit(){
        getInputManager().addMapping("toggleState", new KeyTrigger(KeyInput.KEY_P));
        getInputManager().addMapping("addState", new KeyTrigger(KeyInput.KEY_A));
        getInputManager().addMapping("removeState", new KeyTrigger(KeyInput.KEY_R));
        getInputManager().addListener(this, "toggleState", "addState", "removeState");
        
        BitmapText hudText = new BitmapText(getApplication().getGuiFont(), false);
        hudText.setSize(getApplication().getGuiFont().getCharSet().getRenderedSize());
        hudText.setText("P - Toggle whether or not the state is enabled\nA - Add the state\nR - Remove the state");
        hudText.setLocalTranslation(300, hudText.getLineHeight() * 3, 0);
        getGuiNode().attachChild(hudText);
    }
    
    @Override
    protected void onDeinit(){
        getInputManager().deleteMapping("toggleState");
        getInputManager().deleteMapping("addState");
        getInputManager().deleteMapping("removeState");
        getInputManager().removeListener(this);
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf){        
        if(isPressed){
            ExampleSimpleAppStateRotatingCube otherState = getState(ExampleSimpleAppStateRotatingCube.class);

            if(name.equals("toggleState")){
                if(otherState != null){ //if it is currently removed
                    otherState.setEnabled(!otherState.isEnabled());
                }
            }else if(name.equals("addState")){
                getStateManager().attach(new ExampleSimpleAppStateRotatingCube());
            }else{
                getStateManager().detach(otherState);
            }
        }
    }
    
}
