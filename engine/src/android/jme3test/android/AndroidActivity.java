
/*
 *
 * Android Activity for OpenGL ES2 based tests
 * requires Android 2.2+
 *
 * created: Mon Nov  8 00:08:07 EST 2010
 */
package jme3test.android;

import android.app.Activity;
import android.content.res.Resources;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import com.jme3.system.AppSettings;
import com.jme3.system.JmeSystem;
import com.jme3.system.android.OGLESContext;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;

public class AndroidActivity extends Activity {

    private final static java.util.logging.Logger logger = java.util.logging.Logger.getLogger(AndroidActivity.class.getName());
    private OGLESContext ctx;
    private GLSurfaceView view;
    private boolean useVA = false;
    private boolean verboseLogging = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        JmeSystem.setResources(getResources());

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        AppSettings settings = new AppSettings(true);

        String testClassName = getIntent().getStringExtra(AndroidActivity.class.getName() + ".TEST_CLASS_NAME");

        logger.info("test class name: [" + testClassName + "]");

        String appClass = (testClassName != null ? testClassName : "jme3test.android.SimpleTexturedTest");

        useVA = getIntent().getBooleanExtra(AndroidActivity.class.getName() + ".USE_VA", false);

        logger.info("USE_VA -> [" + useVA + "]");

        settings.putBoolean("USE_VA", useVA);

        verboseLogging = getIntent().getBooleanExtra(AndroidActivity.class.getName() + ".VERBOSE_LOGGING", false);

        settings.putBoolean("VERBOSE_LOGGING", verboseLogging);

        Application app = null;

        try {
            Class<? extends Application> clazz = (Class<? extends Application>) Class.forName(
                    appClass);

            app = clazz.newInstance();
            /*
            app = (Application) java.lang.reflect.Proxy.newProxyInstance(
            this.getClass().getClassLoader(),
            new Class[] {Class.forName(appClass)},
            
            new java.lang.reflect.InvocationHandler() {
            public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) throws Throwable {
            if (
            method.getName().equals("loadFPSText") ||
            method.getName().equals("loadStatsView")
            ) {
            logger.info("ignoring method: [" + method + "]");
            return null;
            }
            
            return method.invoke(proxy, args);
            }
            }
            );
             */


            if (app instanceof SimpleApplication) {
                ((SimpleApplication) app).setShowSettings(false);
            }

            logger.info("setting settings ...");
            app.setSettings(settings);
            logger.info("setting settings ... done.");

            logger.info("starting app ...");
            app.start();
            logger.info("starting app ... done.");

            if (app instanceof SimpleApplication) {
                ((SimpleApplication) app).getGuiNode().detachAllChildren();
            }

            logger.info("creating context ...");
            ctx = (OGLESContext) app.getContext();
            logger.info("creating context ... done.");

            ctx.setSettings(settings);

            logger.info("creating view ...");
            view = ctx.createView(this);
            logger.info("creating view ... done.");

            logger.info("setting content view ...");
            setContentView(view);
            logger.info("setting content done ...");

        } catch (Throwable exception) {
            logger.warning("exception: " + exception);
            exception.printStackTrace(System.err);
        }
    }

    @Override
    protected void onResume() {
        logger.info("onResume ...");
        super.onResume();
        logger.info("view.onResume ...");

        view.onResume();

        logger.info("view.onResume ... done.");
        logger.info("onResume ... done.");
    }

    @Override
    protected void onPause() {
        super.onPause();
        view.onPause();
    }
//	@Override
//	protected void onDestroy(){
//		super.onDestroy();
//		Debug.stopMethodTracing();
//	}
}
