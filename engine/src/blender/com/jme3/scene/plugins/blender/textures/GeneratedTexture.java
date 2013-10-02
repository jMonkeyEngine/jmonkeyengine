package com.jme3.scene.plugins.blender.textures;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.jme3.bounding.BoundingBox;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.BlenderContext.LoadedFeatureDataType;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.textures.TriangulatedTexture.TriangleTextureElement;
import com.jme3.scene.plugins.blender.textures.UVCoordinatesGenerator.UVCoordinatesType;
import com.jme3.scene.plugins.blender.textures.generating.TextureGenerator;
import com.jme3.scene.plugins.blender.textures.io.PixelIOFactory;
import com.jme3.scene.plugins.blender.textures.io.PixelInputOutput;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import com.jme3.texture.TextureCubeMap;

/**
 * The generated texture loaded from blender file. The texture is not generated
 * after being read. This class rather stores all required data and can compute
 * a pixel in the required 3D space position.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
/* package */class GeneratedTexture extends Texture {
    private static final int       POSITIVE_X       = 0;
    private static final int       NEGATIVE_X       = 1;
    // private static final int POSITIVE_Y = 2;
    private static final int       NEGATIVE_Y       = 3;
    private static final int       POSITIVE_Z       = 4;
    private static final int       NEGATIVE_Z       = 5;

    // flag values
    public static final int        TEX_COLORBAND    = 1;
    public static final int        TEX_FLIPBLEND    = 2;
    public static final int        TEX_NEGALPHA     = 4;
    public static final int        TEX_CHECKER_ODD  = 8;
    public static final int        TEX_CHECKER_EVEN = 16;
    public static final int        TEX_PRV_ALPHA    = 32;
    public static final int        TEX_PRV_NOR      = 64;
    public static final int        TEX_REPEAT_XMIR  = 128;
    public static final int        TEX_REPEAT_YMIR  = 256;
    public static final int        TEX_FLAG_MASK    = TEX_COLORBAND | TEX_FLIPBLEND | TEX_NEGALPHA | TEX_CHECKER_ODD | TEX_CHECKER_EVEN | TEX_PRV_ALPHA | TEX_PRV_NOR | TEX_REPEAT_XMIR | TEX_REPEAT_YMIR;

    /** Material-texture link structure. */
    private final Structure        mTex;
    /** Texture generateo for the specified texture type. */
    private final TextureGenerator textureGenerator;

    /**
     * Constructor. Reads the required data from the 'tex' structure.
     * 
     * @param tex
     *            the texture structure
     * @param mTex
     *            the material-texture link data structure
     * @param textureGenerator
     *            the generator for the required texture type
     * @param blenderContext
     *            the blender context
     */
    public GeneratedTexture(Structure tex, Structure mTex, TextureGenerator textureGenerator, BlenderContext blenderContext) {
        this.mTex = mTex;
        this.textureGenerator = textureGenerator;
        this.textureGenerator.readData(tex, blenderContext);
        super.setImage(new GeneratedTextureImage(textureGenerator.getImageFormat()));
    }

    /**
     * This method computes the textyre color/intensity at the specified (u, v,
     * s) position in 3D space.
     * 
     * @param pixel
     *            the pixel where the result is stored
     * @param u
     *            the U factor
     * @param v
     *            the V factor
     * @param s
     *            the S factor
     */
    public void getPixel(TexturePixel pixel, float u, float v, float s) {
        textureGenerator.getPixel(pixel, u, v, s);
    }

    /**
     * This method triangulates the texture. In the result we get a set of small
     * flat textures for each face of the given mesh. This can be later merged
     * into one flat texture.
     * 
     * @param mesh
     *            the mesh we create the texture for
     * @param geometriesOMA
     *            the old memory address of the geometries group that the given
     *            mesh belongs to (required for bounding box calculations)
     * @param coordinatesType
     *            the types of UV coordinates
     * @param blenderContext
     *            the blender context
     * @return triangulated texture
     */
    @SuppressWarnings("unchecked")
    public TriangulatedTexture triangulate(Mesh mesh, Long geometriesOMA, UVCoordinatesType coordinatesType, BlenderContext blenderContext) {
        List<Geometry> geometries = (List<Geometry>) blenderContext.getLoadedFeature(geometriesOMA, LoadedFeatureDataType.LOADED_FEATURE);

        int[] coordinatesSwappingIndexes = new int[] { ((Number) mTex.getFieldValue("projx")).intValue(), ((Number) mTex.getFieldValue("projy")).intValue(), ((Number) mTex.getFieldValue("projz")).intValue() };
        List<Vector3f> uvs = UVCoordinatesGenerator.generateUVCoordinatesFor3DTexture(mesh, coordinatesType, coordinatesSwappingIndexes, geometries);
        Vector3f[] uvsArray = uvs.toArray(new Vector3f[uvs.size()]);
        BoundingBox boundingBox = UVCoordinatesGenerator.getBoundingBox(geometries);
        Set<TriangleTextureElement> triangleTextureElements = new TreeSet<TriangleTextureElement>(new Comparator<TriangleTextureElement>() {
            public int compare(TriangleTextureElement o1, TriangleTextureElement o2) {
                return o1.faceIndex - o2.faceIndex;
            }
        });
        int[] indices = new int[3];
        for (int i = 0; i < mesh.getTriangleCount(); ++i) {
            mesh.getTriangle(i, indices);
            triangleTextureElements.add(new TriangleTextureElement(i, boundingBox, this, uvsArray, indices, blenderContext));
        }
        return new TriangulatedTexture(triangleTextureElements, blenderContext);
    }

    /**
     * Creates a texture for the sky. The result texture has 6 layers.
     * @param size
     *            the size of the texture (width and height are equal)
     * @param horizontalColor
     *            the horizon color
     * @param zenithColor
     *            the zenith color
     * @return the sky texture
     */
    public TextureCubeMap generateSkyTexture(int size, ColorRGBA horizontalColor, ColorRGBA zenithColor) {
        Image image = ImageUtils.createEmptyImage(Format.RGB8, size, size, 6);
        PixelInputOutput pixelIO = PixelIOFactory.getPixelIO(image.getFormat());
        TexturePixel pixel = new TexturePixel();

        float delta = 1 / (float) (size - 1);
        float sideV, sideS = 1, forwardU = 1, forwardV, upS;

        long time = System.currentTimeMillis();
        for (int x = 0; x < size; ++x) {
            sideV = 1;
            forwardV = 1;
            upS = 0;
            for (int y = 0; y < size; ++y) {
                textureGenerator.getPixel(pixel, 1, sideV, sideS);
                pixelIO.write(image, NEGATIVE_X, ImageUtils.color(pixel, horizontalColor, zenithColor), x, y);// right

                textureGenerator.getPixel(pixel, 0, sideV, 1 - sideS);
                pixelIO.write(image, POSITIVE_X, ImageUtils.color(pixel, horizontalColor, zenithColor), x, y);// left

                textureGenerator.getPixel(pixel, forwardU, forwardV, 0);
                pixelIO.write(image, POSITIVE_Z, ImageUtils.color(pixel, horizontalColor, zenithColor), x, y);// front

                textureGenerator.getPixel(pixel, 1 - forwardU, forwardV, 1);
                pixelIO.write(image, NEGATIVE_Z, ImageUtils.color(pixel, horizontalColor, zenithColor), x, y);// back

                textureGenerator.getPixel(pixel, forwardU, 0, upS);
                pixelIO.write(image, NEGATIVE_Y, ImageUtils.color(pixel, horizontalColor, zenithColor), x, y);// top

                // textureGenerator.getPixel(pixel, forwardU, 1, upS);
                // pixelIO.write(image, POSITIVE_Y, ImageUtils.color(pixel, horizontalColor, zenithColor), x, y);//bottom

                sideV = FastMath.clamp(sideV - delta, 0, 1);
                forwardV = FastMath.clamp(forwardV - delta, 0, 1);
                upS = FastMath.clamp(upS + delta, 0, 1);
            }
            sideS = FastMath.clamp(sideS - delta, 0, 1);
            forwardU = FastMath.clamp(forwardU - delta, 0, 1);
        }

        System.out.println(System.currentTimeMillis() - time);

        return new TextureCubeMap(image);
    }

    @Override
    public void setWrap(WrapAxis axis, WrapMode mode) {
    }

    @Override
    public void setWrap(WrapMode mode) {
    }

    @Override
    public WrapMode getWrap(WrapAxis axis) {
        return null;
    }

    @Override
    public Type getType() {
        return Type.ThreeDimensional;
    }

    @Override
    public Texture createSimpleClone() {
        return null;
    }

    /**
     * Private class to give the format of the 'virtual' 3D texture image.
     * 
     * @author Marcin Roguski (Kaelthas)
     */
    private static class GeneratedTextureImage extends Image {
        public GeneratedTextureImage(Format imageFormat) {
            super.format = imageFormat;
        }
    }
}
