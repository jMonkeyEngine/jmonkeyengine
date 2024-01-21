package jme3test.material;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Matrix3f;
import com.jme3.math.Matrix4f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import com.jme3.util.struct.Struct;
import com.jme3.util.struct.StructStd140BufferObject;
import com.jme3.util.struct.fields.ColorRGBAField;
import com.jme3.util.struct.fields.FloatField;
import com.jme3.util.struct.fields.IntField;
import com.jme3.util.struct.fields.Matrix3fField;
import com.jme3.util.struct.fields.Matrix4fField;
import com.jme3.util.struct.fields.SubStructArrayField;
import com.jme3.util.struct.fields.SubStructField;
import com.jme3.util.struct.fields.Vector3fField;

/**
 * This is an example of UBO usage and an unit test for the Struct Buffer Object.
 * If everything works as expected, a green square should appear, if not the square will be red.
 * 
 * RenderDOC can be used to see the UBO on the gpu.
 */
public class TestUBO extends SimpleApplication {
    
    public final static class PointLight implements Struct {
        public final Vector3fField position = new Vector3fField(0, "position", new Vector3f());
        public final FloatField radius = new FloatField(1, "radius", 1f);
        public final ColorRGBAField color = new ColorRGBAField(2, "color", new ColorRGBA());
        
        PointLight() {
        }

        PointLight(Vector3f position, float radius, ColorRGBA color) {
            this.position.getValueForUpdate().set(position);
            this.color.getValueForUpdate().set(color);
            this.radius.setValue(radius);
        }
    }

    public final static class DirectLight implements Struct {
        public final Vector3fField direction = new Vector3fField(0, "direction", new Vector3f());
        public final ColorRGBAField color = new ColorRGBAField(1, "color", new ColorRGBA());

        DirectLight() {
        }

        DirectLight(Vector3f direction, ColorRGBA color) {
            this.direction.getValueForUpdate().set(direction);
            this.color.getValueForUpdate().set(color);
        }
    }

    public final static class Lights implements Struct {
        public final IntField nDirectLights = new IntField(0, "nDirectLights", 1);

        public final SubStructField<DirectLight> test1 = new SubStructField<DirectLight>(1, "test1", new DirectLight(
            new Vector3f(0,1,0),
            ColorRGBA.Blue
        ));

        public final IntField test2 = new IntField(2, "test2", 111);

        public final SubStructField<PointLight> test3 = new SubStructField<PointLight>(3, "test3", new PointLight(
            new Vector3f(7,9,7),
            99f,
            ColorRGBA.Red
        ));

        public final IntField test4 = new IntField(4, "test4", 222);

        public final Matrix3fField test5 = new Matrix3fField(5,"test5",new Matrix3f(1,2,3,4,5,6,7,8,9));
        public final IntField test6 = new IntField(6, "test6", 333);

        public final Matrix4fField test7 = new Matrix4fField(7, "test7", new Matrix4f(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16));
        public final IntField test8 = new IntField(8, "test8", 444);

        public final SubStructArrayField<DirectLight> directLights = new SubStructArrayField<DirectLight>(9, "directLights",  new DirectLight[] {
                new DirectLight(new Vector3f(0, 0, 1), ColorRGBA.Green),
        });
        
        public final IntField nPointLights = new IntField(10, "nPointLights", 2);

        public final SubStructArrayField<PointLight> pointLights = new SubStructArrayField<PointLight>(11, "pointLights", new PointLight[] {
                new PointLight(new Vector3f(5, 9, 7), 9f, ColorRGBA.Red),
                new PointLight(new Vector3f(5, 10, 7), 8f, ColorRGBA.Green),
        });
        public final SubStructArrayField<PointLight> pointLights2 = new SubStructArrayField<PointLight>(12, "pointLights2", new PointLight[] {
                new PointLight(new Vector3f(3, 9, 7), 91f, ColorRGBA.Green),
                new PointLight(new Vector3f(3, 10, 7), 90f, ColorRGBA.Blue),
        });

        public final IntField test13 = new IntField(13, "test13", 555);

    }

    StructStd140BufferObject lightsBO;
    StructStd140BufferObject lightsBO2;
    Lights lights = new Lights();
    int n = 0;

    @Override
    public void simpleInitApp() {
        flyCam.setEnabled(false);

        lightsBO = new StructStd140BufferObject(lights);
        lightsBO2 = lightsBO.clone();
        
        Material mat = new Material(assetManager, "jme3test/ubo/TestUBO.j3md");
        mat.setUniformBufferObject("TestStruct1", lightsBO);
        mat.setUniformBufferObject("TestStruct2", lightsBO2);

        Geometry geo = new Geometry("Test", new Box(1, 1, 1));
        geo.setMaterial(mat);
        rootNode.attachChild(geo);
        geo.setLocalTranslation(0, 0, 1);
        
        cam.lookAt(geo.getWorldTranslation(), Vector3f.UNIT_Y);
    }

    @Override
    public void update() {
        super.update();
        n++;
        if (n > 10) {
            lights.test8.setValue(999999);
            lights.test13.setValue(111);
            lightsBO2.update(lights);
        }
    }


    public static void main(String[] args) {
        AppSettings sett = new AppSettings(true);
        sett.putBoolean("GraphicsDebug", true);
        sett.setRenderer(AppSettings.LWJGL_OPENGL32);
        TestUBO app = new TestUBO();
        app.setSettings(sett);
        app.setPauseOnLostFocus(false);
        app.start();
    }

}

