package com.jme3.shader;

import java.io.IOException;
import java.lang.ref.WeakReference;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.renderer.Renderer;
import com.jme3.util.NativeObject;
import com.jme3.util.struct.StructuredBuffer;
import com.jme3.util.struct.StructuredBufferLayout;

/**
*  A generic memory buffer
 * @author  Riccardo Balbo
 */
public class BufferObject extends StructuredBuffer {

    public static enum AccessHint {
        /**
         * The data store contents will be modified once and used many times.
         */
        Static,
        /**
         * The data store contents will be modified once and used at most a few times.
         */
        Stream,
        /**
         * The data store contents will be modified repeatedly and used many times.
         */
        Dynamic,
        /**
         * Used only by the cpu.
         */
        CpuOnly,

        Default
    }

    public static enum NatureHint {
        /**
         * The data store contents are modified by the application, and used as the source for GL drawing and image specification commands.
         */
        Draw,
        /**
         * The data store contents are modified by reading data from the GL, and used to return that data when queried by the application.
         */
        Read,
        /**
         * The data store contents are modified by reading data from the GL, and used as the source for GL drawing and image specification commands.
         */
        Copy,

        Default
    }




 


    private AccessHint accessHint=AccessHint.Default;
    private NatureHint natureHint=NatureHint.Default;

    private transient WeakReference<BufferObject> weakRef;
    private transient int binding;

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(accessHint.ordinal(),"accessHint",0);
        oc.write(natureHint.ordinal(),"natureHint",0);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        accessHint=AccessHint.values()[ic.readInt("accessHint",0)];
        natureHint=NatureHint.values()[ic.readInt("natureHint",0)];
    }



    @Override
    public BufferObject clone(){
        BufferObject vb =   (BufferObject) super.clone();
        vb.binding = -1;
        vb.weakRef=null;
        return vb;
    }

    
    public BufferObject(){
        super();
    }

    protected BufferObject(int id){
        super(id);
    }

    public BufferObject(Class<? extends StructuredBufferLayout> layoutDef) {
        super(layoutDef);
    }

    public int getBinding() {
        return binding;
    }

    @Override
    public void resetObject() {
        this.id = -1;
        invalidateData();
    }

    @Override
    public void deleteObject(final Object rendererObject) {
        if (!(rendererObject instanceof Renderer)) {
            throw new IllegalArgumentException("This bo can't be deleted from " + rendererObject);
        }
        ((Renderer) rendererObject).deleteBuffer(this);
    }

    @Override
    public NativeObject createDestructableClone() {
        return new BufferObject( getId());
    }

    @Override
    protected void deleteNativeBuffers() {
        invalidateData();
        super.deleteNativeBuffers();
    }

    @Override
    public long getUniqueId() {
        return ((long) OBJTYPE_BO << 32) | ((long) id);
    }

    public void setBinding(final int binding) {
        this.binding = binding;
    }

    public WeakReference<BufferObject> getWeakRef() {
        if (weakRef == null) weakRef = new WeakReference<BufferObject>(this);
        return weakRef;
    }

    public AccessHint getAccessHint() {
        return accessHint;
    }

    public void setAccessHint(AccessHint accessHint) {
        this.accessHint = accessHint;
        setUpdateNeeded(true);
    }

    public NatureHint getNatureHint() {
        return natureHint;
    }

    public void setNatureHint(NatureHint natureHint) {
        this.natureHint = natureHint;
        setUpdateNeeded(true);
    }
}
