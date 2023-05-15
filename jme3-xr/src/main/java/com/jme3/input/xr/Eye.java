package com.jme3.input.xr;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.FrameBuffer.FrameBufferTarget;
import com.jme3.texture.Image.Format;

public class Eye {
	SimpleApplication app;
	float posX;
	Vector3f tmpVec = new Vector3f();
	Texture2D offTex;
    Geometry offGeo;
    Camera offCamera;
    Vector3f centerPos = new Vector3f(0f, 0f, -5f);
    Quaternion centerRot = new Quaternion();
    
    public Eye(SimpleApplication app, float posX)
    {
    	this.app = app;
    	this.posX = posX;
    	setupOffscreenView(app);
        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setTexture("ColorMap", offTex);
        
        offGeo = new Geometry("box", new Box(1, 1, 1));
        offGeo.setMaterial(mat);
    }
    
    /** Moves the camera.
     * @param The new absolute center position. */
    public void moveAbs(Vector3f newPos)
    {
    	centerPos.set(newPos);
    	rotateAbs(offCamera.getRotation());
    }
    
    /** Rotates the camera, and moves left/right.
     * @param The new rotation. */
    public void rotateAbs(Quaternion newRot)
    {
    	tmpVec.set(posX, 0.0f, 0.0f);
    	newRot.multLocal(tmpVec);
    	offCamera.setLocation(tmpVec.addLocal(centerPos));
    	offCamera.setRotation(newRot);
    }
    
    private void setupOffscreenView(SimpleApplication app)
    {
    	int w = app.getContext().getSettings().getWidth();
    	int h = app.getContext().getSettings().getHeight();
        offCamera = new Camera(w, h);

        ViewPort offView = app.getRenderManager().createPreView("OffscreenViewX" + posX, offCamera);
        offView.setClearFlags(true, true, true);
        offView.setBackgroundColor(ColorRGBA.DarkGray);
        FrameBuffer offBuffer = new FrameBuffer(w, h, 1);

        //setup framebuffer's cam
        offCamera.setFrustumPerspective(45f, 1f, 1f, 1000f);
        offCamera.setLocation(new Vector3f(-posX, 0f, -5f));
        offCamera.lookAt(new Vector3f(0f, 0f, 0f), Vector3f.UNIT_Y);

        //setup framebuffer's texture
        offTex = new Texture2D(w, h, Format.RGBA8);
        offTex.setMinFilter(Texture.MinFilter.Trilinear);
        offTex.setMagFilter(Texture.MagFilter.Bilinear);

        //setup framebuffer to use texture
        offBuffer.setDepthTarget(FrameBufferTarget.newTarget(Format.Depth));
        offBuffer.addColorTarget(FrameBufferTarget.newTarget(offTex));

        //set viewport to render to offscreen framebuffer
        offView.setOutputFrameBuffer(offBuffer);
        offView.attachScene(app.getRootNode());
    }
    
    public void render()
    {
    	app.getRenderManager().renderGeometry(offGeo);
    }
}
