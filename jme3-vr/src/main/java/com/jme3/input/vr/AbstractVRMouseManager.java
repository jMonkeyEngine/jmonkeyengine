package com.jme3.input.vr;

import java.util.logging.Logger;

import org.lwjgl.glfw.GLFW;

import com.jme3.app.VREnvironment;
import com.jme3.input.MouseInput;
import com.jme3.input.lwjgl.GlfwMouseInputVR;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.Vector2f;
import com.jme3.system.AppSettings;
import com.jme3.system.lwjgl.LwjglWindow;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.ui.Picture;

/**
 * An abstract implementation of a {@link VRMouseManager}. This class should be overridden by specific hardware implementation of VR devices.
 * @author Julien Seinturier - COMEX SA - <a href="http://www.seinturier.fr">http://www.seinturier.fr</a>
 *
 */
public abstract class AbstractVRMouseManager implements VRMouseManager {
    private static final Logger logger = Logger.getLogger(AbstractVRMouseManager.class.getName());

    private VREnvironment environment = null;

    private boolean vrMouseEnabled = true;
    private boolean mouseAttached = false;
    private Picture mouseImage;
    private int recentCenterCount = 0;

    protected final Vector2f cursorPos = new Vector2f();

    private float ySize, sensitivity = 8f, acceleration = 2f;

    private boolean thumbstickMode;
    private float moveScale = 1f;

    /**
     * Create a new AbstractVRMouseManager attached to the given {@link VREnvironment VR environment}.
     * @param environment the {@link VREnvironment VR environment} that this manager is attached to.
     */
    public AbstractVRMouseManager(VREnvironment environment) {
        this.environment = environment;
    }

    @Override
    public void initialize() {
        logger.config("Initializing VR mouse manager.");

        // load default mouse image
        mouseImage = new Picture("mouse");
        setImage("Common/Util/mouse.png");
        // hide default cursor by making it invisible

        MouseInput mi = environment.getApplication().getContext().getMouseInput();
        if( mi instanceof GlfwMouseInputVR ){
            ((GlfwMouseInputVR)mi).hideActiveCursor();
        }
        centerMouse();

        logger.config("Initialized VR mouse manager [SUCCESS]");
    }

    @Override
    public VREnvironment getVREnvironment() {
      return environment;
    }

    @Override
    public void setVRMouseEnabled(boolean enabled) {
        vrMouseEnabled = enabled;
    }

    @Override
    public void setThumbstickMode(boolean set) {
        thumbstickMode = set;
    }

    @Override
    public boolean isThumbstickMode() {
        return thumbstickMode;
    }

    @Override
    public void setSpeed(float sensitivity, float acceleration) {
        this.sensitivity = sensitivity;
        this.acceleration = acceleration;
    }

    @Override
    public float getSpeedSensitivity() {
        return sensitivity;
    }

    @Override
    public float getSpeedAcceleration() {
        return acceleration;
    }

    @Override
    public float getMouseMoveScale() {
        return moveScale;
    }

    @Override
    public void setMouseMoveScale(float set) {
        moveScale = set;
    }

    @Override
    public void setImage(String texture) {
        if (environment != null){

            if (environment.getApplication() != null){
                if( environment.isInVR() == false ){
                    Texture tex = environment.getApplication().getAssetManager().loadTexture(texture);
                    mouseImage.setTexture(environment.getApplication().getAssetManager(), (Texture2D)tex, true);
                    ySize = tex.getImage().getHeight();
                    mouseImage.setHeight(ySize);
                    mouseImage.setWidth(tex.getImage().getWidth());
                    mouseImage.getMaterial().getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
                    mouseImage.getMaterial().getAdditionalRenderState().setDepthWrite(false);
                } else {
                    Texture tex = environment.getApplication().getAssetManager().loadTexture(texture);
                    mouseImage.setTexture(environment.getApplication().getAssetManager(), (Texture2D)tex, true);
                    ySize = tex.getImage().getHeight();
                    mouseImage.setHeight(ySize);
                    mouseImage.setWidth(tex.getImage().getWidth());
                    mouseImage.getMaterial().getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
                    mouseImage.getMaterial().getAdditionalRenderState().setDepthWrite(false);
                }
            } else {
                throw new IllegalStateException("This VR environment is not attached to any application.");
            }

        } else {
          throw new IllegalStateException("This VR view manager is not attached to any VR environment.");
        }
    }


    @Override
    public Vector2f getCursorPosition() {

        if (environment != null){
            if (environment.getApplication() != null){
                 if( environment.isInVR() ) {
                        return cursorPos;
                    }

                    return environment.getApplication().getInputManager().getCursorPosition();
            } else {
                throw new IllegalStateException("This VR environment is not attached to any application.");
            }
        } else {
            throw new IllegalStateException("This VR view manager is not attached to any VR environment.");
        }
    }

    @Override
    public void centerMouse() {
        if (environment != null){
            if (environment.getApplication() != null){
                // set mouse in center of the screen if newly added
                Vector2f size = environment.getVRGUIManager().getCanvasSize();
                MouseInput mi = environment.getApplication().getContext().getMouseInput();
                AppSettings as = environment.getApplication().getContext().getSettings();
                if( mi instanceof GlfwMouseInputVR ) ((GlfwMouseInputVR)mi).setCursorPosition((int)(as.getWidth() / 2f), (int)(as.getHeight() / 2f));
                if( environment.isInVR() ) {
                    cursorPos.x = size.x / 2f;
                    cursorPos.y = size.y / 2f;
                    recentCenterCount = 2;
                }
            } else {
                throw new IllegalStateException("This VR environment is not attached to any application.");
            }
        } else {
            throw new IllegalStateException("This VR view manager is not attached to any VR environment.");
        }
    }

    @Override
    public void update(float tpf) {
        // if we are showing the cursor, add our picture as it
        if( vrMouseEnabled && environment.getApplication().getInputManager().isCursorVisible() ) {
            if(!mouseAttached) {
                mouseAttached = true;
                environment.getApplication().getGuiViewPort().attachScene(mouseImage);
                centerMouse();
                // the "real" mouse pointer should stay hidden
                if (environment.getApplication().getContext() instanceof LwjglWindow){
                    GLFW.glfwSetInputMode(((LwjglWindow)environment.getApplication().getContext()).getWindowHandle(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
                }
            }
            // handle mouse movements, which may be in addition to (or exclusive from) tracked movement
            MouseInput mi = environment.getApplication().getContext().getMouseInput();
            if( mi instanceof GlfwMouseInputVR ) {
                if( recentCenterCount <= 0 ) {
                    //Vector2f winratio = VRGuiManager.getCanvasToWindowRatio();
                    cursorPos.x += ((GlfwMouseInputVR)mi).getLastDeltaX();// * winratio.x;
                    cursorPos.y += ((GlfwMouseInputVR)mi).getLastDeltaY();// * winratio.y;
                    if( cursorPos.x < 0f ) cursorPos.x = 0f;
                    if( cursorPos.y < 0f ) cursorPos.y = 0f;
                    if( cursorPos.x > environment.getVRGUIManager().getCanvasSize().x ) cursorPos.x = environment.getVRGUIManager().getCanvasSize().x;
                    if( cursorPos.y > environment.getVRGUIManager().getCanvasSize().y ) cursorPos.y = environment.getVRGUIManager().getCanvasSize().y;
                } else recentCenterCount--;
                ((GlfwMouseInputVR)mi).clearDeltas();
            }
            // ok, update the cursor graphic position
            Vector2f currentPos = getCursorPosition();
            mouseImage.setLocalTranslation(currentPos.x, currentPos.y - ySize, environment.getVRGUIManager().getGuiDistance() + 1f);

            mouseImage.updateGeometricState();

        } else if(mouseAttached) {
            mouseAttached = false;
            environment.getApplication().getGuiViewPort().detachScene(mouseImage);

            // Use the setCursorVisible implementation to show the cursor again, depending on the state of cursorVisible
            boolean cursorVisible = environment.getApplication().getInputManager().isCursorVisible();
            environment.getApplication().getContext().getMouseInput().setCursorVisible(cursorVisible);
        }
    }
}
