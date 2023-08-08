package com.jme3.environment.baker;

import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.function.Function;

import com.jme3.asset.AssetManager;
import com.jme3.environment.baker.EnvBaker;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture.MagFilter;
import com.jme3.texture.Texture.MinFilter;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.texture.TextureCubeMap;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;


/**
 * Render the environment into a cubemap
 *
 * @author Riccardo Balbo
 */
public abstract class GenericEnvBaker implements EnvBaker{

    protected static Vector3f[] axisX=new Vector3f[6];
    protected static Vector3f[] axisY=new Vector3f[6];
    protected static Vector3f[] axisZ=new Vector3f[6];
    static{
        //PositiveX axis(left, up, direction)
        axisX[0]=Vector3f.UNIT_Z.mult(1.0F);
        axisY[0]=Vector3f.UNIT_Y.mult(-1.0F);
        axisZ[0]=Vector3f.UNIT_X.mult(1.0F);
        //NegativeX
        axisX[1]=Vector3f.UNIT_Z.mult(-1.0F);
        axisY[1]=Vector3f.UNIT_Y.mult(-1.0F);
        axisZ[1]=Vector3f.UNIT_X.mult(-1.0F);
        //PositiveY
        axisX[2]=Vector3f.UNIT_X.mult(-1.0F);
        axisY[2]=Vector3f.UNIT_Z.mult(1.0F);
        axisZ[2]=Vector3f.UNIT_Y.mult(1.0F);
        //NegativeY
        axisX[3]=Vector3f.UNIT_X.mult(-1.0F);
        axisY[3]=Vector3f.UNIT_Z.mult(-1.0F);
        axisZ[3]=Vector3f.UNIT_Y.mult(-1.0F);
        //PositiveZ
        axisX[4]=Vector3f.UNIT_X.mult(-1.0F);
        axisY[4]=Vector3f.UNIT_Y.mult(-1.0F);
        axisZ[4]=Vector3f.UNIT_Z;
        //NegativeZ
        axisX[5]=Vector3f.UNIT_X.mult(1.0F);
        axisY[5]=Vector3f.UNIT_Y.mult(-1.0F);
        axisZ[5]=Vector3f.UNIT_Z.mult(-1.0F);
    }

    protected TextureCubeMap env;
    protected Format depthFormat;


    protected final RenderManager renderManager;
    protected final AssetManager assetManager;
    protected final Camera cam;
    protected final boolean copyToRam;


    public GenericEnvBaker(
        RenderManager rm,
        AssetManager am,
        Format colorFormat,
        Format depthFormat,
        int env_size,
        boolean copyToRam
    ){
        this.copyToRam=copyToRam;
        this.depthFormat=depthFormat;

        renderManager=rm;
        assetManager=am;


        cam=new Camera(128,128);
        
        env=new TextureCubeMap(env_size,env_size,colorFormat);
        env.setMagFilter(MagFilter.Bilinear);
        env.setMinFilter(MinFilter.BilinearNoMipMaps);
        env.setWrap(WrapMode.EdgeClamp);
        env.getImage().setColorSpace(ColorSpace.Linear);
    }

    public TextureCubeMap getEnvMap(){
        return env;
    }   

    Camera getCam(int id,int w,int h,Vector3f position,float frustumNear,float frustumFar){
        cam.resize(w,h,false);
        cam.setLocation(position);
        cam.setFrustumPerspective(90.0F,1F,frustumNear,frustumFar);
        cam.setRotation(new Quaternion().fromAxes(axisX[id],axisY[id],axisZ[id]));
        return cam;
    }    

    @Override
    public void clean(){
        env.getImage().dispose();
        System.gc();
        System.gc();        
    }


    @Override
    public void bakeEnvironment(Spatial scene,   Vector3f position, float frustumNear, float frustumFar,Function<Geometry,Boolean> filter) {       
        FrameBuffer envbaker=new FrameBuffer(env.getImage().getWidth(),env.getImage().getHeight(),1);
        envbaker.setDepthTarget(FrameBuffer.newTarget(depthFormat));
        envbaker.setSrgb(false);

        for(int i=0;i<6;i++) envbaker.addColorTarget(FrameBuffer.newTarget(env).face(TextureCubeMap.Face.values()[i]));
            
          for(int i=0;i<6;i++){
            envbaker.setTargetIndex(i);

            ViewPort viewPort=new ViewPort("EnvBaker",getCam(i,envbaker.getWidth(),envbaker.getHeight(),position,frustumNear,frustumFar));
            viewPort.setClearFlags(true,true,true);
            viewPort.setBackgroundColor(ColorRGBA.Pink);
        
            viewPort.setOutputFrameBuffer(envbaker);
            viewPort.clearScenes();
            viewPort.attachScene(scene);

            scene.updateLogicalState(0);
            scene.updateModelBound();
            scene.updateGeometricState();
           
            Function<Geometry,Boolean> ofilter= renderManager.getRenderFilter();

            renderManager.setRenderFilter(filter);
            renderManager.renderViewPort(viewPort,0.16f);
            renderManager.setRenderFilter(ofilter);

            if(copyToRam){
                ByteBuffer face=BufferUtils.createByteBuffer(
                    (
                        env.getImage().getWidth()*env.getImage().getHeight()*(
                            env.getImage().getFormat().getBitsPerPixel()/8
                        )
                    )
                ); 
                renderManager.getRenderer().readFrameBufferWithFormat(envbaker, face,env.getImage().getFormat());
                face.rewind();
                env.getImage().setData(i,face);

            }
        }

        env.getImage().clearUpdateNeeded();
  
        envbaker.dispose();
    }

    
}