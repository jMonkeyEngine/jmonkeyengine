package net.java.nboglpack.glsleditor;

/**
 * Created on 26. March 2007, 00:49
 *
 */
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.openide.loaders.DataObject;
import org.openide.util.RequestProcessor;

/**
 * Observes an DataObject for document updates and starts compilation automatically.
 * @author Michael Bien
 */
public class GlslShaderFileObserver implements DocumentListener {

    private final DataObject observedDao;
    private final static RequestProcessor RP = new RequestProcessor("compiler");
    private final RequestProcessor.Task compilerTask;
    private boolean runOnDocUpdate = true;
    private int compileDelay = 500;

    public GlslShaderFileObserver(DataObject dao) {
        observedDao = dao;

        compilerTask = RP.create(new Runnable() {

            public void run() {
//                GLSLCompilerService compiler = Lookup.getDefault().lookup(GLSLCompilerService.class);
//                compiler.compileShader(new DataObject[] {observedDao}, false);
            }
        });
        compilerTask.setPriority(Thread.MIN_PRIORITY);
    }

    // DocumentListener
    public void insertUpdate(DocumentEvent arg0) {
        if (runOnDocUpdate) {
            runCompileTask();
        }
    }

    public void removeUpdate(DocumentEvent arg0) {
        if (runOnDocUpdate) {
            runCompileTask();
        }
    }

    public void changedUpdate(DocumentEvent arg0) {
    }

    public final void runCompileTask() {
        compilerTask.schedule(compileDelay);
    }

    public void setCompileDelay(int compileDelay) {
        this.compileDelay = compileDelay;
    }

    public void setRunOnDocUpdate(boolean runOnDocUpdate) {
        this.runOnDocUpdate = runOnDocUpdate;
    }
}
