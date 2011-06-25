/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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

package jme3test.awt;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeCanvasContext;
import com.jme3.util.JmeFormatter;
import java.awt.Canvas;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.Callable;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

public class TestCanvas {

    private static JmeCanvasContext context;
    private static Canvas canvas;
    private static Application app;
    private static JFrame frame;
    private static final String appClass = "jme3test.post.TestMultiplesFilters";

    private static void createFrame(){
        frame = new JFrame("Test");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter(){
            @Override
            public void windowClosed(WindowEvent e) {
                app.stop();
            }
        });

        JMenuBar menuBar = new JMenuBar();
        frame.setJMenuBar(menuBar);

        JMenu menuFile = new JMenu("File");
        menuBar.add(menuFile);

        final JMenuItem itemRemoveCanvas = new JMenuItem("Remove Canvas");
        menuFile.add(itemRemoveCanvas);
        itemRemoveCanvas.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (itemRemoveCanvas.getText().equals("Remove Canvas")){
                    frame.getContentPane().remove(canvas);

                    // force OS to repaint over canvas ..
                    // this is needed since AWT does not handle
                    // that when a heavy-weight component is removed.
                    frame.setVisible(false);
                    frame.setVisible(true);
                    frame.requestFocus();

                    itemRemoveCanvas.setText("Add Canvas");
                }else if (itemRemoveCanvas.getText().equals("Add Canvas")){
                    frame.getContentPane().add(canvas);
                    itemRemoveCanvas.setText("Remove Canvas");
                }
            }
        });

        JMenuItem itemKillCanvas = new JMenuItem("Stop/Start Canvas");
        menuFile.add(itemKillCanvas);
        itemKillCanvas.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                frame.getContentPane().remove(canvas);
                app.stop(true);

                createCanvas(appClass);
                frame.getContentPane().add(canvas);
                frame.pack();
                startApp();
            }
        });

        JMenuItem itemExit = new JMenuItem("Exit");
        menuFile.add(itemExit);
        itemExit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                frame.dispose();
                app.stop();
            }
        });

        JMenu menuEdit = new JMenu("Edit");
        menuBar.add(menuEdit);
        JMenuItem itemDelete = new JMenuItem("Delete");
        menuEdit.add(itemDelete);

        JMenu menuView = new JMenu("View");
        menuBar.add(menuView);
        JMenuItem itemSetting = new JMenuItem("Settings");
        menuView.add(itemSetting);

        JMenu menuHelp = new JMenu("Help");
        menuBar.add(menuHelp);
    }

    public static void createCanvas(String appClass){
        AppSettings settings = new AppSettings(true);
        settings.setWidth( Math.max(640, frame.getContentPane().getWidth()) );
        settings.setHeight( Math.max(480, frame.getContentPane().getHeight()) );

        try{
            Class<? extends Application> clazz = (Class<? extends Application>) Class.forName(appClass);
            app = clazz.newInstance();
        }catch (ClassNotFoundException ex){
            ex.printStackTrace();
        }catch (InstantiationException ex){
            ex.printStackTrace();
        }catch (IllegalAccessException ex){
            ex.printStackTrace();
        }

        app.setPauseOnLostFocus(false);
        app.setSettings(settings);
        app.createCanvas();

        context = (JmeCanvasContext) app.getContext();
        canvas = context.getCanvas();
        canvas.setSize(settings.getWidth(), settings.getHeight());
    }

    public static void startApp(){
        app.startCanvas();
        app.enqueue(new Callable<Void>(){
            public Void call(){
                if (app instanceof SimpleApplication){
                    SimpleApplication simpleApp = (SimpleApplication) app;
                    simpleApp.getFlyByCamera().setDragToRotate(true);
                }
                return null;
            }
        });
        
    }

    public static void main(String[] args){
        JmeFormatter formatter = new JmeFormatter();

        Handler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(formatter);

        Logger.getLogger("").removeHandler(Logger.getLogger("").getHandlers()[0]);
        Logger.getLogger("").addHandler(consoleHandler);
        
        SwingUtilities.invokeLater(new Runnable(){
            public void run(){
                JPopupMenu.setDefaultLightWeightPopupEnabled(false);

                createFrame();
                createCanvas(appClass);
                
                frame.getContentPane().add(canvas);
                frame.pack();
                startApp();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }

}
