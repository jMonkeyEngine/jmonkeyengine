package com.jme3.util.struct;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;

/**
 * BufferLayout
 */
public abstract class StructuredBufferLayout implements Savable, Cloneable {

    private ArrayList<LayoutRegion> regions = new ArrayList<LayoutRegion>();
    private int bufferEnd=0;

    private transient DirtyRegionsIterator dirtyRegionsIterator;

    public static class LayoutRegion implements Savable,Cloneable{
        public int start=-1;
        public int end=-1;
        public boolean dirty = true;

        public int size = -1;

        public LayoutRegion(int size) {
            this.size = size;
        }

        public LayoutRegion(int start,int end) {
            this.start=start;
            this.end=end;
        }

        @Override
        public String toString(){
            return "Region [start="+start+", end="+end+", size="+size+", dirty="+dirty+"]";
        }


        public LayoutRegion() {

        }

        @Override
        public void write(JmeExporter ex) throws IOException {
            OutputCapsule oc = ex.getCapsule(this);
            oc.write(start,"start",0);
            oc.write(end,"end",0);
            oc.write(dirty,"dirty",false);
            oc.write(size,"size",0);
        }

        @Override
        public void read(JmeImporter im) throws IOException {
            InputCapsule ic = im.getCapsule(this);
            start=ic.readInt("start",0);
            end=ic.readInt("end",0);
            dirty=ic.readBoolean("dirty",false);
            size=ic.readInt("size",0);
        }

        @Override
        public LayoutRegion clone() throws CloneNotSupportedException {
            return (LayoutRegion) super.clone();
        }
    }

    public static class DirtyRegion extends LayoutRegion {
        public boolean full = false;
    }

    @Override
    public StructuredBufferLayout clone() throws CloneNotSupportedException {
        StructuredBufferLayout clone= (StructuredBufferLayout) super.clone();
        for(LayoutRegion r:regions){
            clone.regions.add(r.clone());
        }
        clone.dirtyRegionsIterator=null;
        return clone;
    }

        
    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.writeSavableArrayList(regions, "regions", null);
        oc.write(bufferEnd, "bufferEnd", 0);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        regions=ic.readSavableArrayList("regions", null);
        bufferEnd=ic.readInt("bufferEnd",0);
    }


    protected void clearLayout() {
        regions.clear();
        regions.trimToSize();
        bufferEnd=0;
    
    }


    protected int getBufferEnd(){
        return bufferEnd;
    }

    protected void setBufferEnd(int n){
        bufferEnd=n;
    }

    protected void addRegionToLayout(LayoutRegion lr) {
        if( lr.size==-1){
            lr.size=lr.end-lr.start;
        }else{
            lr.start = regions.size() == 0 ? 0 : regions.get(regions.size() - 1).end;
            lr.end = lr.start + lr.size;
        }

        bufferEnd=lr.end;
     
        regions.add(lr);
    }

    public List<LayoutRegion> getRegions(){
        return regions;
    }

    public static class DirtyRegionsIterator {
        private StructuredBufferLayout layout;

        public DirtyRegionsIterator(StructuredBufferLayout layout) {
            this.layout = layout;
        }

        private final DirtyRegion dirtyRegion = new DirtyRegion();
        private int pos = 0;

        public void rewind() {
            pos = 0;
        }

        public DirtyRegion getNext() {
            int read = 0;
            int dirtRegions=0;
            boolean empty = true;


            while (pos < layout.regions.size()) {
                LayoutRegion dr = layout.regions.get(pos++);
                if (dr.dirty) {
                    dr.dirty = false;
                    if (empty) {
                        dirtyRegion.start = read;
                        empty = false;
                    }
                    read += dr.size;
                    dirtRegions++;
                }else{
                    break;
                }
   
            }

            if (empty) return null;

            if(dirtRegions==layout.countRegions()){
                dirtyRegion.full=true;
                dirtyRegion.end = layout.getEnd();
                dirtyRegion.start=0;
            }else{
                dirtyRegion.full=false;
                dirtyRegion.end = read;
            }
            dirtyRegion.size=dirtyRegion.end-dirtyRegion.start;

            return dirtyRegion;
        }
    }

    public DirtyRegionsIterator getDirtyRegions() {
        if(dirtyRegionsIterator==null)dirtyRegionsIterator= new DirtyRegionsIterator(this);
        dirtyRegionsIterator.rewind();
        return dirtyRegionsIterator;
    }

   
    public LayoutRegion getRegion(int i) {
        return regions.get(i);
    }

    public int countRegions() {
        return regions.size();
    }




    public int getEnd() {
        return bufferEnd;
    }

    public int getStart() {
        return regions.get(0).start;
    }

    public void markAllDirty() {
        for(LayoutRegion r:regions)r.dirty=true;
    }

    @Override
    public String toString(){
        StringBuilder sb=new StringBuilder();
        sb.append(getClass().getSimpleName()).append("{\n");
        for(LayoutRegion r:regions){
            sb.append("    ").append(r).append("\n");
        }
        sb.append("}");
        return sb.toString();
    }


    public abstract void serialize(ByteBuffer out,Object o);
    public abstract void setLayout(List<StructField> fields);
}