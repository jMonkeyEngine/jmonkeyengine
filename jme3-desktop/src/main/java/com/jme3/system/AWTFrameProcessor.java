/*
 * Copyright (c) 2009-2021 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.system;

import java.awt.Component;
import java.awt.EventQueue;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.jme3.app.Application;
import com.jme3.input.AWTKeyInput;
import com.jme3.input.AWTMouseInput;
import com.jme3.post.SceneProcessor;
import com.jme3.profile.AppProfiler;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;

/**
 * A frame processor that enables to render JMonkey frame buffer onto an AWT component.
 * <p>
 * This class is based on the <a href="http://www.oracle.com/technetwork/java/javase/overview/javafx-overview-2158620.html">JavaFX</a> original code provided by Alexander Brui (see <a href="https://github.com/JavaSaBr/JME3-JFX">JME3-FX</a>)
 * </p>
 * @author Julien Seinturier - COMEX SA - <a href="http://www.seinturier.fr">http://www.seinturier.fr</a>
 * @author Alexander Brui (JavaSaBr)
 *
 */
public class AWTFrameProcessor implements SceneProcessor, PropertyChangeListener {

    public enum TransferMode {
        ALWAYS,
        ON_CHANGES
    }

    private Application application = null;

    /**
     * The width listener.
     */
    protected PropertyChangeListener widthListener;

    /**
     * The height listener.
     */
    protected PropertyChangeListener heightListener;

    /**
     * The ration listener.
     */
    protected PropertyChangeListener rationListener;

    /**
     * The flag to decide when we should resize.
     */
    private final AtomicInteger reshapeNeeded;

    /**
     * The render manager.
     */
    private RenderManager renderManager;

    /**
     * The source view port.
     */
    private ViewPort viewPort;

    /**
     * The frame transfer.
     */
    private AWTComponentRenderer frameTransfer;

    /**
     * The transfer mode.
     */
    private TransferMode transferMode;

    /**
     * The destination of jMe frames.
     */
    protected volatile Component destination;

    /**
     * The flag is true if this processor is main.
     */
    private volatile boolean main;

    private int askWidth;
    private int askHeight;

    private boolean askFixAspect;
    private boolean enabled;

    @Override
    public void initialize(RenderManager rm, ViewPort vp) {
        this.renderManager = rm;
    }

    @Override
    public void reshape(ViewPort vp, int w, int h) {
        // TODO Auto-generated method stub
    }

    @Override
    public boolean isInitialized() {
        return frameTransfer != null;
    }

    @Override
    public void preFrame(float tpf) {
        // TODO Auto-generated method stub

    }

    @Override
    public void postQueue(RenderQueue rq) {
        // TODO Auto-generated method stub

    }

    @Override
    public void postFrame(FrameBuffer out) {
        if (!isEnabled()) {
            return;
        }

        final AWTComponentRenderer frameTransfer = getFrameTransfer();
        if (frameTransfer != null) {
            frameTransfer.copyFrameBufferToImage(getRenderManager());
        }

        // for the next frame
        if (hasDestination() && reshapeNeeded.get() > 0 && reshapeNeeded.decrementAndGet() >= 0) {

            if (frameTransfer != null) {
                frameTransfer.dispose();
            }

            setFrameTransfer(reshapeInThread(askWidth, askHeight, askFixAspect));
        }
    }

    @Override
    public void cleanup() {
        final AWTComponentRenderer frameTransfer = getFrameTransfer();

        if (frameTransfer != null) {
            frameTransfer.dispose();
            setFrameTransfer(null);
        }
    }

    @Override
    public void setProfiler(AppProfiler profiler) {
            // not implemented
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        System.out.println("Property changed: "+evt.getPropertyName()+" "+evt.getOldValue()+" -> "+evt.getNewValue());
    }

    public AWTFrameProcessor() {
        transferMode = TransferMode.ALWAYS;
        askWidth = 1;
        askHeight = 1;
        main = true;
        reshapeNeeded = new AtomicInteger(2);    
    }

    /**
     * Notify about that the ratio was changed.
     *
     * @param newValue the new value of the ratio.
     */
    protected void notifyChangedRatio(Boolean newValue) {
        notifyComponentResized(destination.getWidth(), destination.getHeight(), newValue);
    }

    /**
     * Notify about that the height was changed.
     *
     * @param newValue the new value of the height.
     */
    protected void notifyChangedHeight(Number newValue) {
        notifyComponentResized(destination.getWidth(), newValue.intValue(), isPreserveRatio());
    }

    /**
     * Notify about that the width was changed.
     *
     * @param newValue the new value of the width.
     */
    protected void notifyChangedWidth(Number newValue) {
        notifyComponentResized(newValue.intValue(), destination.getHeight(), isPreserveRatio());
    }

    /**
     * Gets the application.
     *
     * @return the application.
     */
    protected Application getApplication() {
        return application;
    }

    /**
     * Sets the application.
     *
     * @param application the application.
     */
    protected void setApplication(Application application) {
        this.application = application;
    }

    /**
     * Gets the current destination.
     *
     * @return the current destination.
     */
    protected Component getDestination() {
        return destination;
    }

    /**
     * Sets the destination.
     *
     * @param destination the destination.
     */
    protected void setDestination(Component destination) {
        this.destination = destination;
    }

    /**
     * Checks of existing destination.
     * @return true if destination is exists.
     */
    protected boolean hasDestination() {
        return destination != null;
    }

    /**
     * Checks of existing application.
     * @return true if destination is exists.
     */
    protected boolean hasApplication() {
        return application != null;
    }


    /**
     * Gets the frame transfer.
     * @return the file transfer.
     */
    protected AWTComponentRenderer getFrameTransfer() {
        return frameTransfer;
    }

    /**
     * Sets the frame transfer.
     *
     * @param frameTransfer the file transfer.
     */
    protected void setFrameTransfer(AWTComponentRenderer frameTransfer) {
        this.frameTransfer = frameTransfer;
    }

    /**
     * Gets the view port.
     *
     * @return the view port.
     */
    protected ViewPort getViewPort() {
        return viewPort;
    }

    /**
     * Gets the render manager.
     *
     * @return the render manager.
     */
    protected RenderManager getRenderManager() {
        return renderManager;
    }

    public boolean isMain() {
        return main;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Handle resizing.
     *
     * @param newWidth  the new width.
     * @param newHeight the new height.
     * @param fixAspect true to fix the aspect ratio.
     */
    protected void notifyComponentResized(int newWidth, int newHeight, boolean fixAspect) {

        newWidth = Math.max(newWidth, 1);
        newHeight = Math.max(newHeight, 1);

        if (askWidth == newWidth && askWidth == newHeight && askFixAspect == fixAspect) {
            return;
        }

        askWidth = newWidth;
        askHeight = newHeight;
        askFixAspect = fixAspect;
        reshapeNeeded.set(2);
    }

    public void reshape() {
        reshapeNeeded.set(2);
    }

    /**
     * Is preserve ratio.
     *
     * @return is preserve ratio.
     */
    protected boolean isPreserveRatio() {
        return false;
    }

    /**
     * Gets destination width.
     *
     * @return the destination width.
     */
    protected int getDestinationWidth() {
        return getDestination().getWidth();
    }

    /**
     * Gets destination height.
     *
     * @return the destination height.
     */
    protected int getDestinationHeight() {
        return getDestination().getHeight();
    }

    /**
     * Bind this processor.
     *
     * @param destination the destination.
     * @param application the application.
     */
    public void bind(Component destination, Application application) {
        final RenderManager renderManager = application.getRenderManager();

        if (renderManager == null) {
            throw new RuntimeException("No render manager available from the application.");
        }

        List<ViewPort> postViews = renderManager.getPostViews();
        if (postViews.isEmpty()) {
            throw new RuntimeException("the list of a post view is empty.");
        }

        bind(destination, application, postViews.get(postViews.size() - 1), true);
    }

    /**
     * Bind this processor.
     *
     * @param destination the destination.
     * @param application the application.
     * @param viewPort    the view port.
     */
    public void bind(Component destination, Application application, ViewPort viewPort) {
        bind(destination, application, viewPort, true);
    }

    /**
     * Bind this processor.
     *
     * @param destination the destination.
     * @param application the application.
     * @param viewPort    the view port.
     * @param main        true if this processor is main.
     */
    public void bind(final Component destination, final Application application, ViewPort viewPort, boolean main) {

        if (hasApplication()) {
            throw new RuntimeException("This process is already bonded.");
        }

        setApplication(application);
        setEnabled(true);

        this.main = main;
        this.viewPort = viewPort;
        this.viewPort.addProcessor(this);

        if (EventQueue.isDispatchThread()) {
            bindDestination(application, destination);
        } else {
            EventQueue.invokeLater(new Runnable() {

                @Override
                public void run() {
                    bindDestination(application, destination);
                }});
        }
    }

    /**
     * Unbind this processor from its current destination.
     */
    public void unbind() {

        if (viewPort != null) {
            viewPort.removeProcessor(this);
            viewPort = null;
        }

        if (EventQueue.isDispatchThread()) {
            unbindDestination();
        } else {
            EventQueue.invokeLater(new Runnable() {

                @Override
                public void run() {
                    unbindDestination();
                }});
        }

    }

    /**
     * Bind this processor.
     *
     * @param application the application.
     * @param destination the destination.
     */
    protected void bindDestination(Application application, Component destination) {

        if (!EventQueue.isDispatchThread()) {
            throw new RuntimeException("bind has to be done from the Event Dispatching thread.");
        }

        if (isMain()) {

            if (application.getContext() != null) {
                if (application.getContext() instanceof AWTContext) {
                    AWTContext context = (AWTContext) application.getContext();
                    AWTMouseInput mouseInput = context.getMouseInput();
                    mouseInput.bind(destination);
                    AWTKeyInput keyInput = context.getKeyInput();
                    keyInput.bind(destination);

                    setDestination(destination);
                    bindListeners();

                    notifyComponentResized(getDestinationWidth(), getDestinationHeight(), isPreserveRatio());

                } else {
                    throw new IllegalArgumentException("Underlying application has to use AWTContext (actually using "+application.getContext().getClass().getSimpleName()+")");
                }
            } else {
                throw new IllegalArgumentException("Underlying application has to use a valid AWTContext (context is null)");
            }
        }
    }

    /**
     * Unbind this processor from destination.
     */
    protected void unbindDestination() {

        if (!EventQueue.isDispatchThread()) {
            throw new RuntimeException("unbind has to be done from the Event Dispatching thread.");
        }

        if (hasApplication() && isMain()) {
            final AWTContext context = (AWTContext) getApplication().getContext();
            final AWTMouseInput mouseInput = context.getMouseInput();
            mouseInput.unbind();
            final AWTKeyInput keyInput = context.getKeyInput();
            keyInput.unbind();
        }

        setApplication(null);

        if (hasDestination()) {
            unbindListeners();
            setDestination(null);
        }
    }


    protected void bindListeners() {
        Component destination = getDestination();
        destination.addPropertyChangeListener(this);
        destination.addPropertyChangeListener(this);
    }


    protected void unbindListeners() {
        Component destination = getDestination();
        destination.removePropertyChangeListener(this);
        destination.removePropertyChangeListener(this);
    }

    /**
     * Reshape the current frame transfer for the new size.
     *
     * @param width     the width.
     * @param height    the height.
     * @param fixAspect true to fix the aspect ratio.
     * @return the new frame transfer.
     */
    protected AWTComponentRenderer reshapeInThread(final int width, final int height, final boolean fixAspect) {

        reshapeCurrentViewPort(width, height);

        ViewPort viewPort = getViewPort();
        RenderManager renderManager = getRenderManager();
        FrameBuffer frameBuffer = viewPort.getOutputFrameBuffer();

        AWTComponentRenderer frameTransfer = createFrameTransfer(frameBuffer, width, height);
        frameTransfer.init(renderManager.getRenderer(), isMain());

        if (isMain()) {
            AWTContext context = (AWTContext) getApplication().getContext();
            context.setHeight(height);
            context.setWidth(width);
        }

        return frameTransfer;
    }

    /**
     * Create a new frame transfer.
     *
     * @param frameBuffer the frame buffer.
     * @param width       the width.
     * @param height      the height.
     * @return the new frame transfer.
     */
    protected AWTComponentRenderer createFrameTransfer(FrameBuffer frameBuffer, int width, int height) {
        return new AWTComponentRenderer(getDestination(), getTransferMode(), isMain() ? null : frameBuffer, width, height);
    }

    /**
     * Reshape the current view port.
     *
     * @param width  the width.
     * @param height the height.
     */
    protected void reshapeCurrentViewPort(int width, int height) {

        ViewPort viewPort = getViewPort();
        Camera camera = viewPort.getCamera();
        int cameraAngle = getCameraAngle();
        float aspect = (float) camera.getWidth() / camera.getHeight();

        if (isMain()) {
            getRenderManager().notifyReshape(width, height);
            camera.setFrustumPerspective(cameraAngle, aspect, 1f, 10000);
            return;
        }

        camera.resize(width, height, true);
        camera.setFrustumPerspective(cameraAngle, aspect, 1f, 10000);

        final List<SceneProcessor> processors = viewPort.getProcessors();

        boolean found = false;
        Iterator<SceneProcessor> iter = processors.iterator();
        while(!found && iter.hasNext()) {
            if (!(iter.next() instanceof AWTFrameProcessor)) {
                found = true;
            }
        }

        if (found) {

            FrameBuffer frameBuffer = new FrameBuffer(width, height, 1);
            frameBuffer.setDepthBuffer(Image.Format.Depth);
            frameBuffer.setColorBuffer(Image.Format.RGBA8);
            frameBuffer.setSrgb(true);

            viewPort.setOutputFrameBuffer(frameBuffer);
        }

        for (final SceneProcessor sceneProcessor : processors) {
            if (!sceneProcessor.isInitialized()) {
                sceneProcessor.initialize(renderManager, viewPort);
            } else {
                sceneProcessor.reshape(viewPort, width, height);
            }
        }
    }

    /**
     * Gets camera angle.
     *
     * @return the camera angle.
     */
    protected int getCameraAngle() {
        final String angle = System.getProperty("awt.frame.transfer.camera.angle", "45");
        return Integer.parseInt(angle);
    }

    public TransferMode getTransferMode() {
        return transferMode;
    }

    public void setTransferMode(TransferMode transferMode) {
        this.transferMode = transferMode;
    }
}
