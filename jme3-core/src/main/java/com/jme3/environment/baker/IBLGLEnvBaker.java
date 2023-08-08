package com.jme3.environment.baker;

import com.jme3.asset.AssetManager;
import com.jme3.environment.baker.IBLEnvBaker;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture.MagFilter;
import com.jme3.texture.Texture.MinFilter;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.texture.Texture2D;
import com.jme3.texture.TextureCubeMap;
import com.jme3.texture.image.ColorSpace;
import com.jme3.ui.Picture;


/**
 *  An env baker for IBL that runs on the GPU 
 * 
 * @author Riccardo Balbo
 */
public class IBLGLEnvBaker extends GenericEnvBaker implements IBLEnvBaker{
    protected Texture2D brtf;
    protected TextureCubeMap irradiance;
    protected TextureCubeMap specular;

    public IBLGLEnvBaker(RenderManager rm,AssetManager am,
                        Format format,
                        Format depthFormat,
                        int env_size,int specular_size,
                        int irradiance_size,
                        int brtf_size
    ){
        super(rm,am,format,depthFormat,env_size,false);  

        irradiance=new TextureCubeMap(irradiance_size,irradiance_size,format);
        irradiance.setMagFilter(MagFilter.Bilinear);
        irradiance.setMinFilter(MinFilter.BilinearNoMipMaps);
        irradiance.setWrap(WrapMode.EdgeClamp);
        irradiance.getImage().setColorSpace(ColorSpace.Linear);

        specular=new TextureCubeMap(specular_size,specular_size,format);
        specular.setMagFilter(MagFilter.Bilinear);
        specular.setMinFilter(MinFilter.BilinearNoMipMaps);
        specular.setWrap(WrapMode.EdgeClamp);
        specular.getImage().setColorSpace(ColorSpace.Linear);
        int nbMipMaps=(int)(Math.log(specular_size)/Math.log(2)+1);
        if(nbMipMaps>6)nbMipMaps=6;
        int[] sizes=new int[nbMipMaps];
        for(int i=0;i<nbMipMaps;i++){
            int size=(int)FastMath.pow(2,nbMipMaps-1-i);
            sizes[i]=size*size*(specular.getImage().getFormat().getBitsPerPixel()/8);
        }
        specular.getImage().setMipMapSizes(sizes);

        brtf=new Texture2D(brtf_size,brtf_size,format);
        brtf.setMagFilter(MagFilter.Bilinear);
        brtf.setMinFilter(MinFilter.BilinearNoMipMaps);
        brtf.setWrap(WrapMode.EdgeClamp);
        brtf.getImage().setColorSpace(ColorSpace.Linear);
    }


    public TextureCubeMap getSpecularIBL(){
        return specular;
    }
       
    public TextureCubeMap getIrradiance(){
        return irradiance;
    }

    @Override
    public void bakeSpecularIBL() {
        Box boxm=new Box(1,1,1);
        Geometry screen=new Geometry("BakeBox",boxm);
    
        Material mat=new Material(assetManager,"Common/IBL/IBLKernels.j3md");
        mat.setBoolean("UseSpecularIBL",true);
        mat.setTexture("EnvMap",env);
        screen.setMaterial(mat);
    
        for(int mip=0;mip<specular.getImage().getMipMapSizes().length;mip++){
            int mipWidth=(int)(specular.getImage().getWidth()*FastMath.pow(0.5f,mip));
            int mipHeight=(int)(specular.getImage().getHeight()*FastMath.pow(0.5f,mip));

            FrameBuffer specularbaker=new FrameBuffer(mipWidth,mipHeight,1);
            specularbaker.setSrgb(false);

            for(int i=0;i<6;i++)specularbaker.addColorTarget( FrameBuffer.newTarget(specular).level(mip).face(i) );
            
            float roughness=(float)mip/(float)(specular.getImage().getMipMapSizes().length-1);
            mat.setFloat("Roughness",roughness);

            for(int i=0;i<6;i++){
                specularbaker.setTargetIndex(i);
                mat.setInt("FaceId",i);

                screen.updateLogicalState(0);
                screen.updateGeometricState();

                renderManager.setCamera(getCam(i,specularbaker.getWidth(),specularbaker.getHeight(),Vector3f.ZERO,1,1000),false);
                renderManager.getRenderer().setFrameBuffer(specularbaker);
                renderManager.renderGeometry(screen);
            }
            specularbaker.dispose();
        }        
        specular.setMinFilter(MinFilter.Trilinear);        
    }

    @Override
    public Texture2D genBRTF() {
        
        Picture screen=new Picture("BakeScreen",true);
        screen.setWidth(1);
        screen.setHeight(1);

        FrameBuffer brtfbaker=new FrameBuffer(brtf.getImage().getWidth(),brtf.getImage().getHeight(),1);
        brtfbaker.setSrgb(false);
        brtfbaker.addColorTarget(FrameBuffer.newTarget(brtf));

        Camera envcam=getCam(0,brtf.getImage().getWidth(),brtf.getImage().getHeight(),Vector3f.ZERO,1,1000);

        Material mat=new Material(assetManager,"Common/IBL/IBLKernels.j3md");
        mat.setBoolean("UseBRDF",true);
        screen.setMaterial(mat);

        renderManager.getRenderer().setFrameBuffer(brtfbaker);
        renderManager.setCamera(envcam,false);

        screen.updateLogicalState(0);
        screen.updateGeometricState();       
        renderManager.renderGeometry(screen);
       
        brtfbaker.dispose();
     
        return brtf;
    }

    @Override
    public void bakeIrradiance() {
     
        Box boxm=new Box(1,1,1);
        Geometry screen=new Geometry("BakeBox",boxm);
    

        FrameBuffer irradiancebaker=new FrameBuffer(irradiance.getImage().getWidth(),irradiance.getImage().getHeight(),1);
        irradiancebaker.setSrgb(false);
        
        for(int i=0;i<6;i++) irradiancebaker.addColorTarget(FrameBuffer.newTarget(irradiance).face(TextureCubeMap.Face.values()[i]));

        Material mat=new Material(assetManager,"Common/IBL/IBLKernels.j3md");
        mat.setBoolean("UseIrradiance",true);
        mat.setTexture("EnvMap",env);
        screen.setMaterial(mat);

        for(int i=0;i<6;i++){
            irradiancebaker.setTargetIndex(i);

            mat.setInt("FaceId",i);

            screen.updateLogicalState(0);
            screen.updateGeometricState();

            renderManager.setCamera(
                getCam(i,irradiancebaker.getWidth(),irradiancebaker.getHeight(),Vector3f.ZERO,1,1000)
            ,false);
            renderManager.getRenderer().setFrameBuffer(irradiancebaker);
            renderManager.renderGeometry(screen);
        }

        irradiancebaker.dispose();

    }


}