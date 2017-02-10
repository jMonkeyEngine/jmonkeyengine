/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jmevr.util;

import com.jme3.app.Application;
import com.jme3.app.VRAppState;
import com.jme3.app.VRApplication;
import com.jme3.app.state.AppState;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Spatial;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.system.AppSettings;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import java.awt.GraphicsEnvironment;
import java.util.Iterator;

/**
 *
 * @author
 * phr00t
 */
public class VRGuiManager {
    
    public enum POSITIONING_MODE {
        MANUAL, AUTO_CAM_ALL, AUTO_CAM_ALL_SKIP_PITCH, AUTO_OBSERVER_POS_CAM_ROTATION, AUTO_OBSERVER_ALL, AUTO_OBSERVER_ALL_CAMHEIGHT
    }
    
    private Camera camLeft, camRight;
    private float guiDistance = 1.5f, guiScale = 1f, guiPositioningElastic;
    private POSITIONING_MODE posMode = POSITIONING_MODE.AUTO_CAM_ALL;
    
    private final Matrix3f orient = new Matrix3f();
    private Vector2f screenSize;
    protected boolean wantsReposition;

    private VRAppState app = null;
    private Application application = null;
    
    /**
     * Create a new GUI manager attached to the given app state.
     * @param app the VR app state that this manager is attached to.
     */
    public VRGuiManager(){
    }
    
    /**
     * Get the VR app state to which this GUI manager is attached.
     * @return the VR app state to which this GUI manager is attached.
     */
    public VRAppState getVRAppState(){
    	return app;
    }
    
    /**
     * Attach the GUI manager to an app state and an Application. 
     * The application has to be the one that the app state is attached.
     * This method should be called from the {@link AppState#initialize(com.jme3.app.state.AppStateManager, Application) initialize} 
     * method of the {@link AppState} instance.
     * @param app the VR app state that this manager is attached to.
     * @param application the application to whitch the app state is attcached.
     */
    public void attach(VRAppState app, Application application){
    	this.app = app;
    	this.application = application;
    }
    
    /**
     * 
     * Makes auto GUI positioning happen not immediately, but like an
     * elastic connected to the headset. Setting to 0 disables (default)
     * Higher settings make it track the headset quicker.
     * 
     * @param elastic amount of elasticity
     */
    public void setPositioningElasticity(float elastic) {
        guiPositioningElastic = elastic;
    }
    
    public float getPositioningElasticity() {
        return guiPositioningElastic;
    }
    
    public void setPositioningMode(POSITIONING_MODE mode) {
        posMode = mode;
    }
    
    public Vector2f getCanvasSize() {
        if( screenSize == null ) {
            if( app.isInVR() && app.getVRHardware() != null ) {
                screenSize = new Vector2f();
                app.getVRHardware().getRenderSize(screenSize);
                screenSize.multLocal(app.getVRViewManager().getResolutionMuliplier());
            } else {
                AppSettings as = application.getContext().getSettings();
                screenSize = new Vector2f(as.getWidth(), as.getHeight());
            }
        }
        return screenSize;
    }
    
    private Vector2f ratio;
    
    public Vector2f getCanvasToWindowRatio() {
        if( ratio == null ) {
            ratio = new Vector2f();
            Vector2f canvas = getCanvasSize();
            int width = Integer.min(GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getWidth(),
            		application.getContext().getSettings().getWidth());
            int height = Integer.min(GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getHeight(),
            		application.getContext().getSettings().getHeight());
            ratio.x = Float.max(1f, canvas.x / width);
            ratio.y = Float.max(1f, canvas.y / height);
        }
        return ratio;
    }        
    
    public POSITIONING_MODE getPositioningMode() {
        return posMode;
    }    
    
    public void positionGui() {
        wantsReposition = true;
    }
    
    private final Vector3f EoldPos = new Vector3f();
    private final Quaternion EoldDir = new Quaternion();
    private void positionTo(Vector3f pos, Quaternion dir, float tpf) {
        Vector3f guiPos = guiQuadNode.getLocalTranslation();
        guiPos.set(0f, 0f, guiDistance);
        dir.mult(guiPos, guiPos);
        guiPos.x += pos.x;
        guiPos.y += pos.y + app.getVRHeightAdjustment();
        guiPos.z += pos.z;        
        if( guiPositioningElastic > 0f && posMode != POSITIONING_MODE.MANUAL ) {
            // mix pos & dir with current pos & dir            
            guiPos.interpolateLocal(EoldPos, guiPos, Float.min(1f, tpf * guiPositioningElastic));
            EoldPos.set(guiPos);
        }
    }
    
    protected void updateGuiQuadGeometricState() {
        guiQuadNode.updateGeometricState();
    }
    
    protected void positionGuiNow(float tpf) {
        wantsReposition = false;
        if( app.isInVR() == false ) return;
        guiQuadNode.setLocalScale(guiDistance * guiScale * 4f, 4f * guiDistance * guiScale, 1f);
        
        switch( posMode ) {
            case MANUAL:
            case AUTO_CAM_ALL_SKIP_PITCH:
            case AUTO_CAM_ALL:
                if( camLeft != null && camRight != null ) {
                    // get middle point
                    temppos.set(camLeft.getLocation()).interpolateLocal(camRight.getLocation(), 0.5f);
                    positionTo(temppos, camLeft.getRotation(), tpf);
                }
                rotateScreenTo(camLeft.getRotation(), tpf);

                break;
            case AUTO_OBSERVER_POS_CAM_ROTATION:
                Object obs = app.getObserver();
                if( obs != null ) {
                    if( obs instanceof Camera ) {
                        positionTo(((Camera)obs).getLocation(), camLeft.getRotation(), tpf);
                    } else {
                        positionTo(((Spatial)obs).getWorldTranslation(), camLeft.getRotation(), tpf);                        
                    }
                }
                rotateScreenTo(camLeft.getRotation(), tpf);

                break;
            case AUTO_OBSERVER_ALL:
            case AUTO_OBSERVER_ALL_CAMHEIGHT:
                obs = app.getObserver();
                if( obs != null ) {
                    Quaternion q;
                    if( obs instanceof Camera ) {
                        q = ((Camera)obs).getRotation();                        
                        temppos.set(((Camera)obs).getLocation());
                    } else {
                        q = ((Spatial)obs).getWorldRotation();
                        temppos.set(((Spatial)obs).getWorldTranslation());
                    }
                    if( posMode == POSITIONING_MODE.AUTO_OBSERVER_ALL_CAMHEIGHT ) {
                        temppos.y = camLeft.getLocation().y;
                    }
                    positionTo(temppos, q, tpf);
                    rotateScreenTo(q, tpf);

                }                
                break;
               
        }
        
        
    }
    
    private final Vector3f look = new Vector3f(), left = new Vector3f(), temppos = new Vector3f(), up = new Vector3f();
    private final Quaternion tempq = new Quaternion();
    
    private void rotateScreenTo(Quaternion dir, float tpf) {
        dir.getRotationColumn(2, look).negateLocal();
        dir.getRotationColumn(0, left).negateLocal();
        orient.fromAxes(left, dir.getRotationColumn(1, up), look);        
        Quaternion rot = tempq.fromRotationMatrix(orient);
        if( posMode == POSITIONING_MODE.AUTO_CAM_ALL_SKIP_PITCH ) VRUtil.stripToYaw(rot);
        if( guiPositioningElastic > 0f && posMode != POSITIONING_MODE.MANUAL ) {
            // mix pos & dir with current pos & dir            
            EoldDir.nlerp(rot, tpf * guiPositioningElastic);
            guiQuadNode.setLocalRotation(EoldDir);
        } else {
            guiQuadNode.setLocalRotation(rot);
        }
    }
    
    public void setGuiDistance(float newGuiDistance) {
        guiDistance = newGuiDistance;                
    }
    
    public void setGuiScale(float scale) {
        guiScale = scale;
    }
    
    public float getGuiDistance() {
        return guiDistance;
    }
    
    public void adjustGuiDistance(float adjustAmount) {
        guiDistance += adjustAmount;
    }
    
    protected void setupGui(Camera leftcam, Camera rightcam, ViewPort left, ViewPort right) {
        if( app.hasTraditionalGUIOverlay() ) {
            camLeft = leftcam;
            camRight = rightcam;            
            Spatial guiScene = getGuiQuad(camLeft);
            left.attachScene(guiScene);
            if( right != null ) right.attachScene(guiScene);
            setPositioningMode(posMode);
        }
    }
    
    /*
        do not use, set by preconfigure routine in VRApplication
    */
    public void _enableCurvedSuface(boolean set) {
        useCurvedSurface = set;
    }
    
    /*
        do not use, set by preconfigure routine in VRApplication
    */
    public void _enableGuiOverdraw(boolean set) {
        overdraw = set;
    }
    
    private boolean useCurvedSurface = false, overdraw = false;
    private Geometry guiQuad;
    private Node guiQuadNode;
    private ViewPort offView;
    private Texture2D guiTexture;
    
    private Spatial getGuiQuad(Camera sourceCam){
        if( guiQuadNode == null ) {
            Vector2f guiCanvasSize = getCanvasSize();
            Camera offCamera = sourceCam.clone();
            offCamera.setParallelProjection(true);
            offCamera.setLocation(Vector3f.ZERO);
            offCamera.lookAt(Vector3f.UNIT_Z, Vector3f.UNIT_Y);

            offView = application.getRenderManager().createPreView("GUI View", offCamera);
            offView.setClearFlags(true, true, true);            
            offView.setBackgroundColor(ColorRGBA.BlackNoAlpha);

            // create offscreen framebuffer
            FrameBuffer offBuffer = new FrameBuffer((int)guiCanvasSize.x, (int)guiCanvasSize.y, 1);

            //setup framebuffer's texture
            guiTexture = new Texture2D((int)guiCanvasSize.x, (int)guiCanvasSize.y, Format.RGBA8);
            guiTexture.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
            guiTexture.setMagFilter(Texture.MagFilter.Bilinear);

            //setup framebuffer to use texture
            offBuffer.setDepthBuffer(Format.Depth);
            offBuffer.setColorTexture(guiTexture);

            //set viewport to render to offscreen framebuffer
            offView.setOutputFrameBuffer(offBuffer);

            // setup framebuffer's scene
            Iterator<Spatial> spatialIter = application.getGuiViewPort().getScenes().iterator();
            while(spatialIter.hasNext()){
            	offView.attachScene(spatialIter.next());
            }
            

            if( useCurvedSurface ) {
                guiQuad = (Geometry)application.getAssetManager().loadModel("Common/Util/gui_mesh.j3o");
            } else {
                guiQuad = new Geometry("guiQuad", new CenterQuad(1f, 1f));
            }
            
            Material mat = new Material(application.getAssetManager(), "Common/MatDefs/VR/GuiOverlay.j3md");            
            mat.getAdditionalRenderState().setDepthTest(!overdraw);
            mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
            mat.getAdditionalRenderState().setDepthWrite(false);
            mat.setTexture("ColorMap", guiTexture);
            guiQuad.setQueueBucket(Bucket.Translucent);
            guiQuad.setMaterial(mat);
            
            guiQuadNode = new Node("gui-quad-node");
            guiQuadNode.setQueueBucket(Bucket.Translucent);
            guiQuadNode.attachChild(guiQuad);
        }
        return guiQuadNode;
    }
}
