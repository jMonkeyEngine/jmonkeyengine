package com.jme3.scene.plugins.blender.materials;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.Savable;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Format;
import com.jme3.scene.VertexBuffer.Usage;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.file.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.materials.MaterialHelper.DiffuseShader;
import com.jme3.scene.plugins.blender.materials.MaterialHelper.SpecularShader;
import com.jme3.scene.plugins.blender.textures.CombinedTexture;
import com.jme3.scene.plugins.blender.textures.TextureHelper;
import com.jme3.texture.Texture;
import com.jme3.util.BufferUtils;

/**
 * This class holds the data about the material.
 * @author Marcin Roguski (Kaelthas)
 */
public final class MaterialContext implements Savable {
    private static final Logger              LOGGER     = Logger.getLogger(MaterialContext.class.getName());

    // texture mapping types
    public static final int                  MTEX_COL   = 0x01;
    public static final int                  MTEX_NOR   = 0x02;
    public static final int                  MTEX_SPEC  = 0x04;
    public static final int                  MTEX_EMIT  = 0x40;
    public static final int                  MTEX_ALPHA = 0x80;
    public static final int                  MTEX_AMB   = 0x800;

    public static final int                  FLAG_TRANSPARENT   = 0x10000;
    
    /* package */final String                name;
    /* package */final List<CombinedTexture> loadedTextures;

    /* package */final ColorRGBA             diffuseColor;
    /* package */final DiffuseShader         diffuseShader;
    /* package */final SpecularShader        specularShader;
    /* package */final ColorRGBA             specularColor;
    /* package */final float                 ambientFactor;
    /* package */final float                 shininess;
    /* package */final boolean               shadeless;
    /* package */final boolean               vertexColor;
    /* package */final boolean               transparent;
    /* package */final boolean               vTangent;
    /* package */FaceCullMode                faceCullMode;

    /* package */MaterialContext(Structure structure, BlenderContext blenderContext) throws BlenderFileException {
        name = structure.getName();

        int mode = ((Number) structure.getFieldValue("mode")).intValue();
        shadeless = (mode & 0x4) != 0;
        vertexColor = (mode & 0x80) != 0;
        vTangent = (mode & 0x4000000) != 0; // NOTE: Requires tangents

        int diff_shader = ((Number) structure.getFieldValue("diff_shader")).intValue();
        diffuseShader = DiffuseShader.values()[diff_shader];
        ambientFactor = ((Number) structure.getFieldValue("amb")).floatValue();

        if (shadeless) {
            float r = ((Number) structure.getFieldValue("r")).floatValue();
            float g = ((Number) structure.getFieldValue("g")).floatValue();
            float b = ((Number) structure.getFieldValue("b")).floatValue();
            float alpha = ((Number) structure.getFieldValue("alpha")).floatValue();

            diffuseColor = new ColorRGBA(r, g, b, alpha);
            specularShader = null;
            specularColor = null;
            shininess = 0.0f;
        } else {
            diffuseColor = this.readDiffuseColor(structure, diffuseShader);

            int spec_shader = ((Number) structure.getFieldValue("spec_shader")).intValue();
            specularShader = SpecularShader.values()[spec_shader];
            specularColor = this.readSpecularColor(structure);
            float shininess = ((Number) structure.getFieldValue("har")).floatValue();// this is (probably) the specular hardness in blender
            this.shininess = shininess > 0.0f ? shininess : MaterialHelper.DEFAULT_SHININESS;
        }

        TextureHelper textureHelper = blenderContext.getHelper(TextureHelper.class);
        loadedTextures = textureHelper.readTextureData(structure, new float[] { diffuseColor.r, diffuseColor.g, diffuseColor.b, diffuseColor.a }, false);

        long flag = ((Number)structure.getFieldValue("flag")).longValue();
        if((flag & FLAG_TRANSPARENT) != 0) {
            // veryfying if the transparency is present
            // (in blender transparent mask is 0x10000 but its better to verify it because blender can indicate transparency when
            // it is not required
            boolean transparent = false;
            if (diffuseColor != null) {
                transparent = diffuseColor.a < 1.0f;
                if (loadedTextures.size() > 0) {// texutre covers the material color
                    diffuseColor.set(1, 1, 1, 1);
                }
            }
            if (specularColor != null) {
                transparent = transparent || specularColor.a < 1.0f;
            }
            this.transparent = transparent;
        } else {
            transparent = false;
        }
    }

    /**
     * @return the name of the material
     */
    public String getName() {
        return name;
    }

    /**
     * Applies material to a given geometry.
     * 
     * @param geometry
     *            the geometry
     * @param geometriesOMA
     *            the geometries OMA
     * @param userDefinedUVCoordinates
     *            UV coords defined by user
     * @param blenderContext
     *            the blender context
     */
    public void applyMaterial(Geometry geometry, Long geometriesOMA, Map<String, List<Vector2f>> userDefinedUVCoordinates, BlenderContext blenderContext) {
        Material material = null;
        if (shadeless) {
            material = new Material(blenderContext.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");

            if (!transparent) {
                diffuseColor.a = 1;
            }

            material.setColor("Color", diffuseColor);
        } else {
            material = new Material(blenderContext.getAssetManager(), "Common/MatDefs/Light/Lighting.j3md");
            material.setBoolean("UseMaterialColors", Boolean.TRUE);

            // setting the colors
            if (!transparent) {
                diffuseColor.a = 1;
            }
            material.setColor("Diffuse", diffuseColor);

            material.setColor("Specular", specularColor);
            material.setFloat("Shininess", shininess);

            material.setColor("Ambient", new ColorRGBA(ambientFactor, ambientFactor, ambientFactor, 1f));
        }
        
        // initializing unused "user-defined UV coords" to all available
        Map<String, List<Vector2f>> unusedUserDefinedUVCoords = Collections.emptyMap();
        if(userDefinedUVCoordinates != null && !userDefinedUVCoordinates.isEmpty()) {
            unusedUserDefinedUVCoords = new HashMap<>(userDefinedUVCoordinates);
        }

        // applying textures
        int textureIndex = 0;
        if (loadedTextures != null && loadedTextures.size() > 0) {
            if (loadedTextures.size() > TextureHelper.TEXCOORD_TYPES.length) {
                LOGGER.log(Level.WARNING, "The blender file has defined more than {0} different textures. JME supports only {0} UV mappings.", TextureHelper.TEXCOORD_TYPES.length);
            }
            for (CombinedTexture combinedTexture : loadedTextures) {
                if (textureIndex < TextureHelper.TEXCOORD_TYPES.length) {
                    String usedUserUVSet = combinedTexture.flatten(geometry, geometriesOMA, userDefinedUVCoordinates, blenderContext);

                    this.setTexture(material, combinedTexture.getMappingType(), combinedTexture.getResultTexture());
                    
                    if(usedUserUVSet == null || unusedUserDefinedUVCoords.containsKey(usedUserUVSet)) {
                        List<Vector2f> uvs = combinedTexture.getResultUVS();
                        if(uvs != null && uvs.size() > 0) {
                            VertexBuffer uvCoordsBuffer = new VertexBuffer(TextureHelper.TEXCOORD_TYPES[textureIndex++]);
                            uvCoordsBuffer.setupData(Usage.Static, 2, Format.Float, BufferUtils.createFloatBuffer(uvs.toArray(new Vector2f[uvs.size()])));
                            geometry.getMesh().setBuffer(uvCoordsBuffer);
                        }//uvs might be null if the user assigned non existing UV coordinates group name to the mesh (this should be fixed in blender file)

                        // Remove used "user-defined UV coords" from the unused collection
                        if(usedUserUVSet != null) {
                	       unusedUserDefinedUVCoords.remove(usedUserUVSet);
                        }
                    }
                } else {
                    LOGGER.log(Level.WARNING, "The texture could not be applied because JME only supports up to {0} different UV's.", TextureHelper.TEXCOORD_TYPES.length);
                }
            }
        }

        if (unusedUserDefinedUVCoords != null && unusedUserDefinedUVCoords.size() > 0) {
            LOGGER.fine("Storing unused, user defined UV coordinates sets.");
            if (unusedUserDefinedUVCoords.size() > TextureHelper.TEXCOORD_TYPES.length) {
                LOGGER.log(Level.WARNING, "The blender file has defined more than {0} different UV coordinates for the mesh. JME supports only {0} UV coordinates buffers.", TextureHelper.TEXCOORD_TYPES.length);
            }
            for (Entry<String, List<Vector2f>> entry : unusedUserDefinedUVCoords.entrySet()) {
                if (textureIndex < TextureHelper.TEXCOORD_TYPES.length) {
                    List<Vector2f> uvs = entry.getValue();
                    VertexBuffer uvCoordsBuffer = new VertexBuffer(TextureHelper.TEXCOORD_TYPES[textureIndex++]);
                    uvCoordsBuffer.setupData(Usage.Static, 2, Format.Float, BufferUtils.createFloatBuffer(uvs.toArray(new Vector2f[uvs.size()])));
                    geometry.getMesh().setBuffer(uvCoordsBuffer);
                } else {
                    LOGGER.log(Level.WARNING, "The user's UV set named: '{0}' could not be stored because JME only supports up to {1} different UV's.", new Object[] {
                		entry.getKey(), TextureHelper.TEXCOORD_TYPES.length
                    });
                }
            }
        }

        // applying additional data
        material.setName(name);
        if (vertexColor) {
            material.setBoolean(shadeless ? "VertexColor" : "UseVertexColor", true);
        }
        material.getAdditionalRenderState().setFaceCullMode(faceCullMode != null ? faceCullMode : blenderContext.getBlenderKey().getFaceCullMode());
        if (transparent) {
            material.setTransparent(true);
            material.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
            geometry.setQueueBucket(Bucket.Transparent);
        }

        geometry.setMaterial(material);
    }

    /**
     * Sets the texture to the given material.
     * 
     * @param material
     *            the material that we add texture to
     * @param mapTo
     *            the texture mapping type
     * @param texture
     *            the added texture
     */
    private void setTexture(Material material, int mapTo, Texture texture) {
        switch (mapTo) {
            case MTEX_COL:
                material.setTexture(shadeless ? MaterialHelper.TEXTURE_TYPE_COLOR : MaterialHelper.TEXTURE_TYPE_DIFFUSE, texture);
                break;
            case MTEX_NOR:
                material.setTexture(MaterialHelper.TEXTURE_TYPE_NORMAL, texture);
                break;
            case MTEX_SPEC:
                material.setTexture(MaterialHelper.TEXTURE_TYPE_SPECULAR, texture);
                break;
            case MTEX_EMIT:
                material.setTexture(MaterialHelper.TEXTURE_TYPE_GLOW, texture);
                break;
            case MTEX_ALPHA:
                if (!shadeless) {
                    material.setTexture(MaterialHelper.TEXTURE_TYPE_ALPHA, texture);
                } else {
                    LOGGER.warning("JME does not support alpha map on unshaded material. Material name is " + name);
                }
                break;
            case MTEX_AMB:
                material.setTexture(MaterialHelper.TEXTURE_TYPE_LIGHTMAP, texture);
                break;
            default:
                LOGGER.severe("Unknown mapping type: " + mapTo);
        }
    }

    /**
     * @return <b>true</b> if the material has at least one generated texture and <b>false</b> otherwise
     */
    public boolean hasGeneratedTextures() {
        if (loadedTextures != null) {
            for (CombinedTexture generatedTextures : loadedTextures) {
                if (generatedTextures.hasGeneratedTextures()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * This method sets the face cull mode.
     * @param faceCullMode
     *            the face cull mode
     */
    public void setFaceCullMode(FaceCullMode faceCullMode) {
        this.faceCullMode = faceCullMode;
    }

    /**
     * This method returns the diffuse color.
     * 
     * @param materialStructure
     *            the material structure
     * @param diffuseShader
     *            the diffuse shader
     * @return the diffuse color
     */
    private ColorRGBA readDiffuseColor(Structure materialStructure, DiffuseShader diffuseShader) {
        // bitwise 'or' of all textures mappings
        int commonMapto = ((Number) materialStructure.getFieldValue("mapto")).intValue();

        // diffuse color
        float r = ((Number) materialStructure.getFieldValue("r")).floatValue();
        float g = ((Number) materialStructure.getFieldValue("g")).floatValue();
        float b = ((Number) materialStructure.getFieldValue("b")).floatValue();
        float alpha = ((Number) materialStructure.getFieldValue("alpha")).floatValue();
        if ((commonMapto & 0x01) == 0x01) {// Col
            return new ColorRGBA(r, g, b, alpha);
        } else {
            switch (diffuseShader) {
                case FRESNEL:
                case ORENNAYAR:
                case TOON:
                    break;// TODO: find what is the proper modification
                case MINNAERT:
                case LAMBERT:// TODO: check if that is correct
                    float ref = ((Number) materialStructure.getFieldValue("ref")).floatValue();
                    r *= ref;
                    g *= ref;
                    b *= ref;
                    break;
                default:
                    throw new IllegalStateException("Unknown diffuse shader type: " + diffuseShader.toString());
            }
            return new ColorRGBA(r, g, b, alpha);
        }
    }

    /**
     * This method returns a specular color used by the material.
     * 
     * @param materialStructure
     *            the material structure filled with data
     * @return a specular color used by the material
     */
    private ColorRGBA readSpecularColor(Structure materialStructure) {
        float specularIntensity = ((Number) materialStructure.getFieldValue("spec")).floatValue();
        float r = ((Number) materialStructure.getFieldValue("specr")).floatValue() * specularIntensity;
        float g = ((Number) materialStructure.getFieldValue("specg")).floatValue() * specularIntensity;
        float b = ((Number) materialStructure.getFieldValue("specb")).floatValue() * specularIntensity;
        float alpha = ((Number) materialStructure.getFieldValue("alpha")).floatValue();
        return new ColorRGBA(r, g, b, alpha);
    }

    @Override
    public void write(JmeExporter e) throws IOException {
        throw new IOException("Material context is not for saving! It implements savable only to be passed to another blend file as a Savable in user data!");
    }

    @Override
    public void read(JmeImporter e) throws IOException {
        throw new IOException("Material context is not for loading! It implements savable only to be passed to another blend file as a Savable in user data!");
    }
}
