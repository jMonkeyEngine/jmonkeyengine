package com.jme3.terrain.heightmap;
import java.io.IOException;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.math.FastMath;

/**
 * <code>RawArrayHeightMap</code> creates a height map directly from a float
 * Array.
 *
 * @author OzoneGrif
 * @version $Id$
 */
public class RawArrayHeightMap extends AbstractHeightMap implements Savable
{
    /**
     * Empty constructor, used by the <code>Savable</code> implementation.
     * Do not use manually.
     */
    public RawArrayHeightMap()
    {
    }
    
    /**
     * Creates an empty heightMap
     *
     * @param size
     */
    public RawArrayHeightMap( int size )
    {
        this.size = size;
        heightData = new float[ size * size ];
    }
    
    /**
     * Creates a heightMap from a pre-existing float Array
     */
    public RawArrayHeightMap( float[] buffer ) throws Exception
    {
        size = (int) FastMath.sqr( buffer.length );
        if ( (size * size) == buffer.length )
            heightData = buffer;
        else
            throw new Exception( "buffer size must be 2^n" );
    }
    
    /**
     * Not needed for <code>RawArrayHeightMap</code>, will return false if not
     * initialized
     */
    @Override
    public boolean load()
    {
        return ((size > 0) && (heightData != null));
    }
    
    /**
     * Checks if both heightMaps are strictly identical
     */
    @Override
    public boolean equals( Object o )
    {
        if ( o instanceof AbstractHeightMap )
        {
            RawArrayHeightMap rhm = (RawArrayHeightMap) o;
            if ( (size == rhm.size) && (heightScale == rhm.heightScale) && (filter == rhm.filter) )
            {
                for ( int i = 0 ; i < heightData.length ; i++ )
                    if ( heightData[ i ] != rhm.heightData[ i ] )
                        return false;
                return true;
            } else
                return false;
        } else
            return false;
    }
    
    /**
     * Returns an unique hashCode strictly linked to this heightMap's data
     */
    @Override
    public int hashCode()
    {
        int hashCode = (size ^ Float.floatToIntBits( heightScale ) ^ Float.floatToIntBits( filter ));
        for ( float element : heightData )
            hashCode ^= Float.floatToIntBits( element );
        return hashCode;
    }
    
    /**
     * Save the heightMap for a BinaryExporter
     *
     * @param out
     * @throws IOException
     */
    @Override
    public void write( JmeExporter ex ) throws IOException
    {
        OutputCapsule oc = ex.getCapsule( this );
        oc.write( size, "size", 0 );
        oc.write( heightScale, "heightScale", 1.0f );
        oc.write( filter, "filter", 0.5f );
        oc.write( heightData, "heightData", null );
    }
    
    /**
     * Load the heightMap for a BinaryImporter
     *
     * @param in
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @Override
    public void read( JmeImporter im ) throws IOException
    {
        InputCapsule ic = im.getCapsule( this );
        ic.readInt( "size", 0 );
        ic.readFloat( "heightScale", 1.0f );
        ic.readFloat( "filter", 0.5f );
        ic.readFloatArray( "heightData", null );
    }
}