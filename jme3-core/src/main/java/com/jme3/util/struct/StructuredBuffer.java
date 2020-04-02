package com.jme3.util.struct;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.util.BufferUtils;
import com.jme3.util.NativeObject;
import com.jme3.util.struct.StructuredBufferLayout.DirtyRegionsIterator;
import com.jme3.util.struct.StructuredBufferLayout.LayoutRegion;

/**
 *
 * @author Riccardo Balbo
 */
public abstract class StructuredBuffer extends NativeObject implements Cloneable, Savable {
    private  static final java.util.logging.Logger logger =  java.util.logging.Logger.getLogger( StructuredBuffer.class.getName());

    private static Map<String, Class<? extends StructuredBufferLayout>> registeredLayouts = new ConcurrentHashMap<String, Class<? extends StructuredBufferLayout>>();

    public static void registerLayout(String name, Class<? extends StructuredBufferLayout> layout) {
        registeredLayouts.put(name, layout);
    }

    public static String getRegisteredLayoutName(Class<? extends StructuredBufferLayout> layout) {
        for (Entry<String, Class<? extends StructuredBufferLayout>> e : registeredLayouts.entrySet()) {
            if(e.getValue().equals(layout)){
                return e.getKey();
            }
        }        
        return null;
    }

    public static Class<? extends StructuredBufferLayout> getRegisteredLayoutFromName(String name){
        return registeredLayouts.get(name);
    }


    static{
        registerLayout("std140",StructuredBufferSTD140Layout.class);
    }


    protected ByteBuffer data = null;
    private Class<? extends StructuredBufferLayout> layoutDef;
    private StructuredBufferLayout layout;

    private transient Class<? extends Struct> rootStruct;
    private transient List<StructField> resolvedFields;
    private transient boolean invalidLayout = true;


    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(layout,"layout",null);
        oc.write(data,"data",null);
        assert layoutDef!=null;
        String defname=getRegisteredLayoutName(layoutDef);
        assert defname!=null;
        oc.write(defname, "layoutDef", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);

        layout= (StructuredBufferLayout) ic.readSavable("layout", null);
        layout.markAllDirty();
        invalidateLayout();    

        data=ic.readByteBuffer("data",null);

        layoutDef=getRegisteredLayoutFromName( ic.readString("layoutDef", null));
        assert layoutDef!=null;

    }

    protected StructuredBuffer(Class<? extends StructuredBufferLayout> layoutDef) {
        super();
        this.layoutDef = layoutDef;
    }

    /** 
     * Internal only
     */
    protected StructuredBuffer() {
        super();
    }

    /** 
     * Internal only
     */
    protected StructuredBuffer(int id) {
        super(id);
    }


    protected void invalidateLayout(){
        new Exception().printStackTrace();
        if(logger.isLoggable(java.util.logging.Level.  FINE  )){
            logger.log(java.util.logging.Level.FINE,
            "Invalidate layout"
            );
        }
        invalidLayout = true;
        resolvedFields = null;
        rootStruct = null;
    }

    protected void invalidateData() {
        invalidateLayout();

        if(logger.isLoggable(java.util.logging.Level.  FINE  )){
            logger.log(java.util.logging.Level.FINE,
            "Invalidate data"
            );
        }
        if (data != null) {
            BufferUtils.destroyDirectBuffer(data);
            data = null;
            setUpdateNeeded(false);
        }

    }

   

    protected int updateData(List<StructField> fields,int id){
        if (invalidLayout) {
            invalidLayout = false;
            try {
                layout = layoutDef.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            layout.setLayout(fields);
            if (logger.isLoggable(java.util.logging.Level.FINE)) {
                logger.log(java.util.logging.Level.FINE, "Set layout for buffer {0} : \n{1}", new Object[] { this, layout });
            }
        }

        if(logger.isLoggable(java.util.logging.Level.  FINER  )){
            logger.log(java.util.logging.Level.FINER,
                "Serialize {0} fields"  , new Object[]{fields.size()}
            );
        }
        boolean updateAll=false;

        if(data==null){
            updateAll = true;
            data = BufferUtils.createByteBuffer(layout.getEnd());
        }

        for(StructField f:fields){
            Object v=f.getValue();
            if(v instanceof List&&((List)v).get(0) instanceof StructField){
                id=updateData((List<StructField>)v,id);
                f.clearUpdateNeeded();
            }else{
                LayoutRegion reg=layout.getRegion(id);
                if (updateAll || f.isUpdateNeeded()) {     
                    if(logger.isLoggable(java.util.logging.Level.  FINER  )){
                        logger.log(java.util.logging.Level.FINER,
                            "Serialize {0} ({1}) in  {2}"  , new Object[]{f,this,reg}
                        );
                    }
                    data.position(reg.start);
                    layout.serialize(data, f.getValue());
                    assert data.position()==reg.end;
                    assert data.position()-reg.start==reg.size;
                    reg.dirty = true;
                    f.clearUpdateNeeded();
                    setUpdateNeeded(false);
                }else{
                    if(logger.isLoggable(java.util.logging.Level.  FINER  )){
                        logger.log(java.util.logging.Level.FINER,
                            "Already up to date. Skip {0} ({1}) in  {2}"  , new Object[]{f,this,reg}
                        );
                    }
                }
                id++;
            }
        }     
        return id;   
    }

    public void updateDataAndLayout( List<StructField> fields) {
        invalidateData();
        updateData(fields);
    }

    public void updateData(List<StructField> fields){
        fields=StructUtils.sortFields(fields);
        updateData(fields,0);
    }

    public void updateData(Struct struct) {        
        if(rootStruct!=struct.getClass()){
            if(logger.isLoggable(java.util.logging.Level.  FINE  )){
                logger.log(java.util.logging.Level.FINE,
                "Change in layout {0} =/= {1} ",new Object[]{rootStruct,struct.getClass()}
                );
            }
            invalidateData();
            resolvedFields=StructUtils.getFromClass(struct);
            rootStruct=struct.getClass();
        }
        updateData(resolvedFields,0);
    }

  


    public void  setUpdateNeeded(boolean dirtyAll){
        if(dirtyAll){
           if(layout!=null)layout.markAllDirty();
        }
        super.setUpdateNeeded();
    }


    protected void rewindData(){
        data.position(layout.getStart());
        data.limit(layout.getEnd());       
    }

    public ByteBuffer getData(){
        rewindData(); 
        return data;
    }


    public ByteBuffer getData(int start,int end){
        data.position(start);
        data.limit(end);
        return data;
    }

    public DirtyRegionsIterator getDirtyRegions(){
        return layout.getDirtyRegions();
    }


    @Override
    public void resetObject() {
        this.id = -1;
        invalidateData();
    }

    @Override
    protected void deleteNativeBuffers() {
        invalidateData();
        super.deleteNativeBuffers();
    }


    @Override
    public StructuredBuffer clone(){
        StructuredBuffer clone= (StructuredBuffer) super.clone();
        rewindData();
        clone.data=  BufferUtils.clone(data);
        clone.invalidateLayout();
        clone.setUpdateNeeded(true);
        return clone;
    }

}
