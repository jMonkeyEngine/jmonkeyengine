/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.util;

import com.jme3.app.VREnvironment;
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
import com.jme3.scene.CenterQuad;
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
 * A class dedicated to the management and the display of a Graphical User Interface (GUI) within a VR environment.
 * @author reden - phr00t - https://github.com/phr00t
 * @author Julien Seinturier - (c) 2016 - JOrigin project - <a href="http://www.jorigin.org">http:/www.jorigin.org</a>
 *
 */
public class VRGuiManager {

	private Camera camLeft, camRight;
	private float guiDistance = 1.5f;
	private float guiScale = 1f;
	private float guiPositioningElastic;

	private VRGUIPositioningMode posMode = VRGUIPositioningMode.AUTO_CAM_ALL;

	private final Matrix3f orient = new Matrix3f();
	private Vector2f screenSize;
	protected boolean wantsReposition;

	private Vector2f ratio;

	private final Vector3f EoldPos = new Vector3f();

	private final Quaternion EoldDir = new Quaternion();

	private final Vector3f look    = new Vector3f();
	private final Vector3f left    = new Vector3f();
	private final Vector3f temppos = new Vector3f();
	private final Vector3f up      = new Vector3f();

	private boolean useCurvedSurface = false;
	private boolean overdraw = false;
	private Geometry guiQuad;
	private Node guiQuadNode;
	private ViewPort offView;
	private Texture2D guiTexture;

	private final Quaternion tempq = new Quaternion();

	private VREnvironment environment = null;

	/**
	 * Create a new GUI manager attached to the given app state.
	 * @param environment the VR environment to which this manager is attached to.
	 */
	public VRGuiManager(VREnvironment environment){
		this.environment = environment;
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

	/**
	 * Get the GUI {@link VRGUIPositioningMode positioning mode}.
	 * @return the GUI {@link VRGUIPositioningMode positioning mode}.
	 * @see #setPositioningMode(VRGUIPositioningMode)
	 */
	public VRGUIPositioningMode getPositioningMode() {
		return posMode;
	}

	/**
	 * Set the GUI {@link VRGUIPositioningMode positioning mode}.
	 * @param mode the GUI {@link VRGUIPositioningMode positioning mode}.
	 * @see #getPositioningMode()
	 */
	public void setPositioningMode(VRGUIPositioningMode mode) {
		posMode = mode;
	}

	/**
	 * Get the GUI canvas size. This method return the size in pixels of the GUI available area within the VR view.
	 * @return the GUI canvas size. This method return the size in pixels of the GUI available area within the VR view.
	 */
	public Vector2f getCanvasSize() {

		if (environment != null){

			if (environment.getApplication() != null){
				if( screenSize == null ) {
					if( environment.isInVR() && environment.getVRHardware() != null ) {
						screenSize = new Vector2f();
						environment.getVRHardware().getRenderSize(screenSize);
						screenSize.multLocal(environment.getVRViewManager().getResolutionMuliplier());
					} else {
						AppSettings as = environment.getApplication().getContext().getSettings();
						screenSize = new Vector2f(as.getWidth(), as.getHeight());
					}
				}
				return screenSize;
			} else {
				throw new IllegalStateException("VR GUI manager underlying environment is not attached to any application.");
			}
		} else {
			throw new IllegalStateException("VR GUI manager is not attached to any environment.");
		}

	}   

	/**
	 * Get the ratio between the {@link #getCanvasSize() GUI canvas size} and the application main windows (if available) or the screen size.
	 * @return the ratio between the {@link #getCanvasSize() GUI canvas size} and the application main windows (if available).
	 * @see #getCanvasSize()
	 */
	public Vector2f getCanvasToWindowRatio() {

		if (environment != null){

			if (environment.getApplication() != null){
				if( ratio == null ) {
					ratio = new Vector2f();
					Vector2f canvas = getCanvasSize();
					int width = Integer.min(GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getWidth(),
							environment.getApplication().getContext().getSettings().getWidth());
					int height = Integer.min(GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getHeight(),
							environment.getApplication().getContext().getSettings().getHeight());
					ratio.x = Float.max(1f, canvas.x / width);
					ratio.y = Float.max(1f, canvas.y / height);
				}
				return ratio;

			} else {
				throw new IllegalStateException("VR GUI manager underlying environment is not attached to any application.");
			}
		} else {
			throw new IllegalStateException("VR GUI manager is not attached to any environment.");
		}
	}          

	/**
	 * Inform this manager that it has to position the GUI.
	 */
	public void positionGui() {
		wantsReposition = true;
	}

	/**
	 * Position the GUI to the given location.
	 * @param pos the position of the GUI.
	 * @param dir the rotation of the GUI.
	 * @param tpf the time per frame.
	 */
	private void positionTo(Vector3f pos, Quaternion dir, float tpf) {

		if (environment != null){
			Vector3f guiPos = guiQuadNode.getLocalTranslation();
			guiPos.set(0f, 0f, guiDistance);
			dir.mult(guiPos, guiPos);
			guiPos.x += pos.x;
			guiPos.y += pos.y + environment.getVRHeightAdjustment();
			guiPos.z += pos.z;        
			if( guiPositioningElastic > 0f && posMode != VRGUIPositioningMode.MANUAL ) {
				// mix pos & dir with current pos & dir            
				guiPos.interpolateLocal(EoldPos, guiPos, Float.min(1f, tpf * guiPositioningElastic));
				EoldPos.set(guiPos);
			}
		} else {
			throw new IllegalStateException("VR GUI manager is not attached to any environment.");
		}
	}

	/**
	 * Update the GUI geometric state. This method should be called after GUI modification.
	 */
	protected void updateGuiQuadGeometricState() {
		guiQuadNode.updateGeometricState();
	}

	/**
	 * Position the GUI without delay.
	 * @param tpf the time per frame.
	 */
	protected void positionGuiNow(float tpf) {

		if (environment != null){
			wantsReposition = false;
			if( environment.isInVR() == false ){
				return;
			}

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
				Object obs = environment.getObserver();
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
				obs = environment.getObserver();
				if( obs != null ) {
					Quaternion q;
					if( obs instanceof Camera ) {
						q = ((Camera)obs).getRotation();                        
						temppos.set(((Camera)obs).getLocation());
					} else {
						q = ((Spatial)obs).getWorldRotation();
						temppos.set(((Spatial)obs).getWorldTranslation());
					}
					if( posMode == VRGUIPositioningMode.AUTO_OBSERVER_ALL_CAMHEIGHT ) {
						temppos.y = camLeft.getLocation().y;
					}
					positionTo(temppos, q, tpf);
					rotateScreenTo(q, tpf);

				}                
				break;  
			}
		} else {
			throw new IllegalStateException("VR GUI manager is not attached to any environment.");
		} 
	}

	/**
	 * Rotate the GUI to the given direction.
	 * @param dir the direction to rotate to.
	 * @param tpf the time per frame.
	 */
	private void rotateScreenTo(Quaternion dir, float tpf) {
		dir.getRotationColumn(2, look).negateLocal();
		dir.getRotationColumn(0, left).negateLocal();
		orient.fromAxes(left, dir.getRotationColumn(1, up), look);        
		Quaternion rot = tempq.fromRotationMatrix(orient);
		if( posMode == VRGUIPositioningMode.AUTO_CAM_ALL_SKIP_PITCH ){
			VRUtil.stripToYaw(rot);
		}

		if( guiPositioningElastic > 0f && posMode != VRGUIPositioningMode.MANUAL ) {
			// mix pos & dir with current pos & dir            
			EoldDir.nlerp(rot, tpf * guiPositioningElastic);
			guiQuadNode.setLocalRotation(EoldDir);
		} else {
			guiQuadNode.setLocalRotation(rot);
		}
	}

	/**
	 * Get the GUI distance from the observer.
	 * @return the GUI distance from the observer.
	 * @see #setGuiDistance(float)
	 */
	public float getGuiDistance() {
		return guiDistance;
	}

	/**
	 * Set the GUI distance from the observer.
	 * @param newGuiDistance the GUI distance from the observer.
	 * @see #getGuiDistance()
	 */
	public void setGuiDistance(float newGuiDistance) {
		guiDistance = newGuiDistance;                
	}

	/**
	 * Get the GUI scale.
	 * @return the GUI scale.
	 * @see #setGuiScale(float)
	 */
	public float getGUIScale(){
		return guiScale;
	}

	/**
	 * Set the GUI scale.
	 * @param scale the GUI scale.
	 * @see #getGUIScale()
	 */
	public void setGuiScale(float scale) {
		guiScale = scale;
	}

	/**
	 * Adjust the GUI distance from the observer. 
	 * This method increment / decrement the {@link #getGuiDistance() GUI distance} by the given value. 
	 * @param adjustAmount the increment (if positive) / decrement (if negative) value of the GUI distance.
	 */
	public void adjustGuiDistance(float adjustAmount) {
		guiDistance += adjustAmount;
	}

	/**
	 * Set up the GUI.
	 * @param leftcam the left eye camera.
	 * @param rightcam the right eye camera.
	 * @param left the left eye viewport.
	 * @param right the right eye viewport.
	 */
	protected void setupGui(Camera leftcam, Camera rightcam, ViewPort left, ViewPort right) {

		if (environment != null){
			if( environment.hasTraditionalGUIOverlay() ) {
				camLeft = leftcam;
				camRight = rightcam;            
				Spatial guiScene = getGuiQuad(camLeft);
				left.attachScene(guiScene);
				if( right != null ) right.attachScene(guiScene);
				setPositioningMode(posMode);
			}
		} else {
			throw new IllegalStateException("VR GUI manager is not attached to any environment.");
		} 
	}

	/**
	 * Get if the GUI has to use curved surface.
	 * @return <code>true</code> if the GUI has to use curved surface and <code>false</code> otherwise.
	 * @see #setCurvedSurface(boolean)
	 */
	public boolean isCurverSurface(){
		return useCurvedSurface;
	}

	/**
	 * Set if the GUI has to use curved surface.
	 * @param set <code>true</code> if the GUI has to use curved surface and <code>false</code> otherwise.
	 * @see #isCurverSurface()
	 */
	public void setCurvedSurface(boolean set) {
		useCurvedSurface = set;
	}

	/**
	 * Get if the GUI has to be displayed even if it is behind objects.
	 * @return <code>true</code> if the GUI has to use curved surface and <code>false</code> otherwise.
	 * @see #setGuiOverdraw(boolean)
	 */
	public boolean isGuiOverdraw(){
		return overdraw;
	}

	/**
	 * Set if the GUI has to be displayed even if it is behind objects.
	 * @param set <code>true</code> if the GUI has to use curved surface and <code>false</code> otherwise.
	 * @see #isGuiOverdraw()
	 */
	public void setGuiOverdraw(boolean set) {
		overdraw = set;
	}

	/**
	 * Create a GUI quad for the given camera.
	 * @param sourceCam the camera
	 * @return a GUI quad for the given camera.
	 */
	private Spatial getGuiQuad(Camera sourceCam){

		if (environment != null){

			if (environment.getApplication() != null){
				if( guiQuadNode == null ) {
					Vector2f guiCanvasSize = getCanvasSize();
					Camera offCamera = sourceCam.clone();
					offCamera.setParallelProjection(true);
					offCamera.setLocation(Vector3f.ZERO);
					offCamera.lookAt(Vector3f.UNIT_Z, Vector3f.UNIT_Y);

					offView = environment.getApplication().getRenderManager().createPreView("GUI View", offCamera);
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
					Iterator<Spatial> spatialIter = environment.getApplication().getGuiViewPort().getScenes().iterator();
					while(spatialIter.hasNext()){
						offView.attachScene(spatialIter.next());
					}


					if( useCurvedSurface ) {
						guiQuad = (Geometry)environment.getApplication().getAssetManager().loadModel("Common/Util/gui_mesh.j3o");
					} else {
						guiQuad = new Geometry("guiQuad", new CenterQuad(1f, 1f));
					}

					Material mat = new Material(environment.getApplication().getAssetManager(), "Common/MatDefs/VR/GuiOverlay.j3md");            
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
			} else {
				throw new IllegalStateException("VR GUI manager underlying environment is not attached to any application.");	
			}
		} else {
			throw new IllegalStateException("VR GUI manager is not attached to any environment.");
		}



	}
}
