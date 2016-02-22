package jme3test.app;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;

/**
 * @author john01dav
 */
public class TestEnqueueRunnable extends SimpleApplication{
    private ExampleAsyncTask exampleAsyncTask;
    
    public static void main(String[] args){
        new TestEnqueueRunnable().start();
    }
    
    @Override
    public void simpleInitApp(){
        Geometry geom = new Geometry("Box", new Box(1, 1, 1));
        Material material = new Material(getAssetManager(), "/Common/MatDefs/Misc/Unshaded.j3md");
        material.setColor("Color", ColorRGBA.Blue); //a color is needed to start with
        geom.setMaterial(material);
        getRootNode().attachChild(geom);
        
        exampleAsyncTask = new ExampleAsyncTask(material);
        exampleAsyncTask.getThread().start();
    }

    @Override
    public void destroy(){
        exampleAsyncTask.endTask();
        super.destroy();
    }
    
    private class ExampleAsyncTask implements Runnable{
        private final Thread thread;
        private final Material material;
        private volatile boolean running = true;

        public ExampleAsyncTask(Material material){
            this.thread = new Thread(this);
            this.material = material;
        }

        public Thread getThread(){
            return thread;
        }
        
        public void run(){
            while(running){
                enqueue(new Runnable(){ //primary usage of this in real applications would use lambda expressions which are unavailable at java 6
                    public void run(){
                        material.setColor("Color", ColorRGBA.randomColor());
                    }
                });
                
                try{
                    Thread.sleep(1000);
                }catch(InterruptedException e){}
            }
        }
        
        public void endTask(){
            running = false;
            thread.interrupt();
        }
        
    }
    
}
