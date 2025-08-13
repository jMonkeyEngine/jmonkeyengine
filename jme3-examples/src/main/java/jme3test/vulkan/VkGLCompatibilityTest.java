package jme3test.vulkan;

import com.jme3.app.FlyCamAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.system.AppSettings;
import com.jme3.system.vulkan.LwjglVulkanContext;

public class VkGLCompatibilityTest extends SimpleApplication {

    public static void main(String[] args) {
        VulkanHelperTest app = new VulkanHelperTest();
        AppSettings settings = new AppSettings(true);
        settings.setWidth(768);
        settings.setHeight(768);
        settings.setRenderer("CUSTOM" + LwjglVulkanContext.class.getName());
        app.setSettings(settings);
        app.setShowSettings(false);
        app.start();
    }

    public VkGLCompatibilityTest() {
        super(new FlyCamAppState());
    }

    @Override
    public void simpleInitApp() {

        Material material;

    }

}
