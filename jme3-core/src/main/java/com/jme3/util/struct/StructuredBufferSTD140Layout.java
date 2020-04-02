package com.jme3.util.struct;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.jme3.util.struct.serializers.Std140Serializer;

/**
 * ClassBufferLayout
 */
public class StructuredBufferSTD140Layout extends StructuredBufferLayout{
    private  final Std140Serializer serializer = new Std140Serializer();
    
    public StructuredBufferSTD140Layout(){
    }

    // ArrayList<Integer> structStack=new ArrayList<Integer>();


    protected void clearLayout() {
        super.clearLayout();
    }

   
    public void setLayout(List<StructField> fields){
        clearLayout();
        addToLayout(fields);    
    }

    private void addToLayout(List<StructField> fields){
        for(StructField f:fields){
            Object v=f.getValue();
            if(v instanceof List&&((List)v).get(0) instanceof StructField){ // sub struct
                setBufferEnd(serializer.align(getBufferEnd(),16));
                addToLayout((List<StructField>)v);
                setBufferEnd(serializer.align(getBufferEnd(),16));
            }else{
                int basicAlignment=serializer.getBasicAlignment(v);
                int length=serializer.estimateSize(v);
                setBufferEnd(serializer.align(getBufferEnd(),basicAlignment));
                LayoutRegion r=new LayoutRegion(getBufferEnd(),getBufferEnd()+length);
                setBufferEnd(getBufferEnd()+length);
                super.addRegionToLayout(r);
            }
        }
        assert verifyRegions() ;
    }

    private boolean verifyRegions(){
        int i=0;
        for(LayoutRegion r:getRegions()){
            assert r.size==r.end-r.start : "Issue with region "+i+": "+r+" size doesn't seem to match";
            i++;
        }
        return true;
    }




    public void serialize(ByteBuffer out,Object o){
        serializer.serialize(out, o);
    }

}