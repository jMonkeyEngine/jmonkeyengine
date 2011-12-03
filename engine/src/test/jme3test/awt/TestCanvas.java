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
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.Callable;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;
import javax.swing.*;

public class TestCanvas {

    private static JmeCanvasContext context;
    private static Canvas canvas;
    private static Application app;
    private static JFrame frame;
    private static Container canvasPanel1, canvasPanel2;
    private static Container currentPanel;
    private static JTabbedPane tabbedPane;
    private static final String appClass = "jme3test.post.TestRenderToTexture";

    private static void createTabs(){
        tabbedPane = new JTabbedPane();
        
        canvasPanel1 = new JPanel();
        canvasPanel1.setLayout(new BorderLayout());
        tabbedPane.addTab("jME3 Canvas 1", canvasPanel1);
        
        canvasPanel2 = new JPanel();
        canvasPanel2.setLayout(new BorderLayout());
        tabbedPane.addTab("jME3 Canvas 2", canvasPanel2);
        
        frame.getContentPane().add(tabbedPane);
        
        currentPanel = canvasPanel1;
    }
    
    private static void createMenu(){
        JMenuBar menuBar = new JMenuBar();
        frame.setJMenuBar(menuBar);

        JMenu menuTortureMethods = new JMenu("Canvas Torture Methods");
        menuBar.add(menuTortureMethods);

        final JMenuItem itemRemoveCanvas = new JMenuItem("Remove Canvas");
        menuTortureMethods.add(itemRemoveCanvas);
        itemRemoveCanvas.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (itemRemoveCanvas.getText().equals("Remove Canvas")){
                    currentPanel.remove(canvas);

                    itemRemoveCanvas.setText("Add Canvas");
                }else if (itemRemoveCanvas.getText().equals("Add Canvas")){
                    currentPanel.add(canvas, BorderLayout.CENTER);
                    
                    itemRemoveCanvas.setText("Remove Canvas");
                }
            }
        });
        
        final JMenuItem itemHideCanvas = new JMenuItem("Hide Canvas");
        menuTortureMethods.add(itemHideCanvas);
        itemHideCanvas.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (itemHideCanvas.getText().equals("Hide Canvas")){
                    canvas.setVisible(false);
                    itemHideCanvas.setText("Show Canvas");
                }else if (itemHideCanvas.getText().equals("Show Canvas")){
                    canvas.setVisible(true);
                    itemHideCanvas.setText("Hide Canvas");
                }
            }
        });
        
        final JMenuItem itemSwitchTab = new JMenuItem("Switch to tab #2");
        menuTortureMethods.add(itemSwitchTab);
        itemSwitchTab.addActionListener(new ActionListener(){
           public void actionPerformed(ActionEvent e){
               if (itemSwitchTab.getText().equals("Switch to tab #2")){
                   canvasPanel1.remove(canvas);
                   canvasPanel2.add(canvas, BorderLayout.CENTER);
                   currentPanel = canvasPanel2;
                   itemSwitchTab.setText("Switch to tab #1");
               }else if (itemSwitchTab.getText().equals("Switch to tab #1")){
                   canvasPanel2.remove(canvas);
                   canvasPanel1.add(canvas, BorderLayout.CENTER);
                   currentPanel = canvasPanel1;
                   itemSwitchTab.setText("Switch to tab #2");
               }
           } 
        });
        
        JMenuItem itemSwitchLaf = new JMenuItem("Switch Look and Feel");
        menuTortureMethods.add(itemSwitchLaf);
        itemSwitchLaf.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Throwable t){
                    t.printStackTrace();
                }
                SwingUtilities.updateComponentTreeUI(frame);
                frame.pack();
            }
        });
        
        JMenuItem itemSmallSize = new JMenuItem("Set size to (0, 0)");
        menuTortureMethods.add(itemSmallSize);
        itemSmallSize.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                Dimension preferred = frame.getPreferredSize();
                frame.setPreferredSize(new Dimension(0, 0));
                frame.pack();
                frame.setPreferredSize(preferred);
            }
        });
        
        JMenuItem itemKillCanvas = new JMenuItem("Stop/Start Canvas");
        menuTortureMethods.add(itemKillCanvas);
        itemKillCanvas.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                currentPanel.remove(canvas);
                app.stop(true);

                createCanvas(appClass);
                currentPanel.add(canvas, BorderLayout.CENTER);
                frame.pack();
                startApp();
            }
        });

        JMenuItem itemExit = new JMenuItem("Exit");
        menuTortureMethods.add(itemExit);
        itemExit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                frame.dispose();
                app.stop();
            }
        });
    }
    
    private static void createFrame(){
        frame = new JFrame("Test");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter(){
            @Override
            public void windowClosed(WindowEvent e) {
                app.stop();
            }
        });

        createTabs();
        createMenu();
    }

    public static void createCanvas(String appClass){
        AppSettings settings = new AppSettings(true);
        settings.setWidth(640);
        settings.setHeight(480);

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
        app.startCanvas();

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
        
        createCanvas(appClass);
        
        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
        }
        
        SwingUtilities.invokeLater(new Runnable(){
            public void run(){
                JPopupMenu.setDefaultLightWeightPopupEnabled(false);

                createFrame();
                
                currentPanel.add(canvas, BorderLayout.CENTER);
                frame.pack();
                startApp();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }

}
