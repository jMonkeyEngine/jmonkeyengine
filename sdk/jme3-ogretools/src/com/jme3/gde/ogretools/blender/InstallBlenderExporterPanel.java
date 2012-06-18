/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * InstallBlenderExporterPanel.java
 *
 * Created on 08.05.2010, 18:41:38
 */
package com.jme3.gde.ogretools.blender;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.openide.filesystems.FileChooserBuilder;
import org.openide.util.Exceptions;
import org.openide.util.Utilities;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.prefs.Preferences;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.swing.JOptionPane;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.NotifyDescriptor.Confirmation;

/**
 *
 * @author normenhansen
 */
public class InstallBlenderExporterPanel extends javax.swing.JDialog {

    /** Creates new form InstallBlenderExporterPanel */
    public InstallBlenderExporterPanel(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                jTextField1.setText(findBlenderFolder());
            }
        });
    }

    private String findBlenderFolder() {
//        if (Utilities.isMac()) {
//            File scriptsDir = new File(System.getProperty("user.home") + "/.blender/scripts/");
//            if (!scriptsDir.exists()) {
//                scriptsDir = new File("/Applications/blender.app/Contents/Resources/MacOS/.blender/scripts/");
//                if (!scriptsDir.exists()) {
//                    scriptsDir = new File(System.getProperty("user.home") + "/Applications/blender.app/Contents/Resources/MacOS/.blender/scripts/");
//                    if (!scriptsDir.exists()) {
//                        scriptsDir = new File("/Applications/blender/blender.app/Contents/Resources/MacOS/.blender/scripts/");
//                    }
//                }
//            }
//            if (!scriptsDir.exists()) {
//                FileChooserBuilder builder = new FileChooserBuilder("/Applications/");
//                builder.setDirectoriesOnly(true);
//                builder.setTitle("Select Blender.app Application");
//                File file = builder.showOpenDialog();
//                if (file != null) {
//                    scriptsDir = new File(file.getPath() + "/Contents/Resources/MacOS/.blender/scripts/");
//                }
//                return scriptsDir.getPath();
//            } else {
//                return scriptsDir.getPath();
//            }
//        } else if (Utilities.isUnix()) {
//            File scriptsDir = new File("/usr/share/blender/");
//            if (!scriptsDir.exists()) {
//                scriptsDir = new File("/usr/lib/blender/");
//            }
//            if (!scriptsDir.exists()) {
//                FileChooserBuilder builder = new FileChooserBuilder("");
//                builder.setDirectoriesOnly(true);
//                builder.setTitle("Select Blender Scripts Directory");
//                File file = builder.showOpenDialog();
//                if (file != null) {
//                    scriptsDir = file;
//                }
//                return scriptsDir.getPath();
//            } else {
//                return scriptsDir.getPath();
//            }
//        } else if (Utilities.isWindows()) {
//            File scriptsDir = new File(System.getProperty("user.home") + "\\AppData\\Roaming\\Blender Foundation\\Blender\\scripts\\");
//            if (!scriptsDir.exists()) {
//                scriptsDir = new File(System.getProperty("user.home") + "\\Application Data\\Roaming\\Blender Foundation\\Blender\\scripts\\");
//                if (!scriptsDir.exists()) {
//                    scriptsDir = new File(System.getProperty("user.home") + "C:\\Program Files\\Blender\\.blender\\scripts\\");
//                }
//            }
//            if (!scriptsDir.exists()) {
//                FileChooserBuilder builder = new FileChooserBuilder("");
//                builder.setDirectoriesOnly(true);
//                builder.setTitle("Select Blender Scripts Directory");
//                File file = builder.showOpenDialog();
//                if (file != null) {
//                    scriptsDir = file;
//                }
//                return scriptsDir.getPath();
//            } else {
//                return scriptsDir.getPath();
//            }
//        }
        return "please set blender scripts folder";
    }
    static final int BUFFER = 2048;

    private void installBlenderPlugin(String scriptsFolder) {
        try {
            File scriptsFolderFile = new File(scriptsFolder);
            if (!scriptsFolderFile.exists()) {
                Confirmation msg = new NotifyDescriptor.Confirmation(
                        "Folder does not exist!",
                        NotifyDescriptor.DEFAULT_OPTION,
                        NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(msg);
                return;
            }
            //        "https://ogreaddons.svn.sourceforge.net/svnroot/ogreaddons/trunk/blendersceneexporter/ogredotscene.py"
            //
            BufferedOutputStream dest = null;
            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(getClass().getResourceAsStream("/com/jme3/gde/ogretools/blender/scripts.zip")));
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                int count;
                byte data[] = new byte[BUFFER];
                if (entry.getName().contains(".svn") || entry.getName().contains(".DS_Store")) {
                    continue;
                }
                if (entry.isDirectory()) {
                    File dir = new File(scriptsFolder + File.separator + entry.getName());
                    dir.mkdirs();
                    continue;
                }
                // write the files to the disk
                FileOutputStream fos = new FileOutputStream(scriptsFolder + File.separator + entry.getName());
                dest = new BufferedOutputStream(fos, BUFFER);
                while ((count = zis.read(data, 0, BUFFER))
                        != -1) {
                    dest.write(data, 0, count);
                }
                dest.flush();
                dest.close();
            }
            zis.close();
            Confirmation msg = new NotifyDescriptor.Confirmation(
                    "Sucessuflly Installed Blender Exporter!",
                    NotifyDescriptor.DEFAULT_OPTION,
                    NotifyDescriptor.INFORMATION_MESSAGE);
            DialogDisplayer.getDefault().notify(msg);
            dispose();

        } catch (IOException ex) {
            Confirmation msg = new NotifyDescriptor.Confirmation(
                    "Error:\n" + ex.toString(),
                    NotifyDescriptor.DEFAULT_OPTION,
                    NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(msg);
            Exceptions.printStackTrace(ex);
        }

    }

    @SuppressWarnings("unchecked")
    private static String getWindowsRegistryBlenderPath() {

        final int HKEY_CURRENT_USER = 0x80000001;
        final int KEY_QUERY_VALUE = 1;
        final int KEY_SET_VALUE = 2;
        final int KEY_READ = 0x20019;

        String value = null;

        final Preferences userRoot = Preferences.userRoot();
        final Class clz = userRoot.getClass();
        try {
            Class[] parms1 = {byte[].class, int.class, int.class};
            final Method mOpenKey = clz.getDeclaredMethod("openKey",
                    parms1);
            mOpenKey.setAccessible(true);

            Class[] parms2 = {int.class};
            final Method mCloseKey = clz.getDeclaredMethod("closeKey",
                    parms2);
            mCloseKey.setAccessible(true);

            Class[] parms3 = {int.class, byte[].class};
            final Method mWinRegQueryValue = clz.getDeclaredMethod(
                    "WindowsRegQueryValueEx", parms3);
            mWinRegQueryValue.setAccessible(true);

            Class[] parms4 = {int.class, int.class, int.class};
            final Method mWinRegEnumValue = clz.getDeclaredMethod(
                    "WindowsRegEnumValue1", parms4);
            mWinRegEnumValue.setAccessible(true);

            Class[] parms5 = {int.class};
            final Method mWinRegQueryInfo = clz.getDeclaredMethod(
                    "WindowsRegQueryInfoKey1", parms5);
            mWinRegQueryInfo.setAccessible(true);

            // Should be: HKEY_CURRENT_USER\Volatile Environment, key NWUSERNAME
            final String subKey = "Volatile Environment";

            Object[] objects1 = {toByteArray(subKey), new Integer(KEY_READ), new Integer(KEY_READ)};
            Integer hSettings = (Integer) mOpenKey.invoke(userRoot,
                    objects1);

            Object[] objects2 = {hSettings, toByteArray("NWUSERNAME")};
            byte[] b = (byte[]) mWinRegQueryValue.invoke(userRoot,
                    objects2);
            value = (b != null ? new String(b).trim() : null);
            System.out.println(value);

            Object[] objects3 = {hSettings};
            mCloseKey.invoke(Preferences.userRoot(), objects3);

        } catch (Exception e) {
            System.out.println("Error getting value Windows registry: ");
            e.printStackTrace();
        }
        return value;
    }

    private static byte[] toByteArray(String str) {
        byte[] result = new byte[str.length() + 1];
        for (int i = 0; i < str.length(); i++) {
            result[i] = (byte) str.charAt(i);
        }
        result[str.length()] = 0;
        return result;
    }

    static final String[] browsers = {"google-chrome", "firefox", "opera",
        "konqueror", "epiphany", "seamonkey", "galeon", "kazehakase", "mozilla"};
    static final String errMsg = "Error attempting to launch web browser";

    public static void openURL(String url) {
//        try {  //attempt to use Desktop library from JDK 1.6+ (even if on 1.5)
//            Class<?> d = Class.forName("java.awt.Desktop");
//            d.getDeclaredMethod("browse", new Class[]{java.net.URI.class}).invoke(
//                    d.getDeclaredMethod("getDesktop").invoke(null),
//                    new Object[]{java.net.URI.create(url)});
//            //above code mimics:
//            //   java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
//        } catch (Exception ignore) {  //library not available or failed
            String osName = System.getProperty("os.name");
            try {
                if (osName.startsWith("Mac OS")) {
                    Class.forName("com.apple.eio.FileManager").getDeclaredMethod(
                            "openURL", new Class[]{String.class}).invoke(null,
                            new Object[]{url});
                } else if (osName.startsWith("Windows")) {
                    Runtime.getRuntime().exec(
                            "rundll32 url.dll,FileProtocolHandler " + url);
                } else { //assume Unix or Linux
                    boolean found = false;
                    for (String browser : browsers) {
                        if (!found) {
                            found = Runtime.getRuntime().exec(
                                    new String[]{"which", browser}).waitFor() == 0;
                            if (found) {
                                Runtime.getRuntime().exec(new String[]{browser, url});
                            }
                        }
                    }
                    if (!found) {
                        throw new Exception(Arrays.toString(browsers));
                    }
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, errMsg + "\n" + e.toString());
            }
//        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/jme3/gde/ogretools/blender/ogre-logo24.png"))); // NOI18N
        jLabel1.setText(org.openide.util.NbBundle.getMessage(InstallBlenderExporterPanel.class, "InstallBlenderExporterPanel.jLabel1.text")); // NOI18N

        jTextField1.setText(org.openide.util.NbBundle.getMessage(InstallBlenderExporterPanel.class, "InstallBlenderExporterPanel.jTextField1.text")); // NOI18N

        jButton1.setText(org.openide.util.NbBundle.getMessage(InstallBlenderExporterPanel.class, "InstallBlenderExporterPanel.jButton1.text")); // NOI18N
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setText(org.openide.util.NbBundle.getMessage(InstallBlenderExporterPanel.class, "InstallBlenderExporterPanel.jButton2.text")); // NOI18N
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jButton3.setText(org.openide.util.NbBundle.getMessage(InstallBlenderExporterPanel.class, "InstallBlenderExporterPanel.jButton3.text")); // NOI18N
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setText(org.openide.util.NbBundle.getMessage(InstallBlenderExporterPanel.class, "InstallBlenderExporterPanel.jLabel2.text")); // NOI18N

        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel3.setText(org.openide.util.NbBundle.getMessage(InstallBlenderExporterPanel.class, "InstallBlenderExporterPanel.jLabel3.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jTextField1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton1))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jButton3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton2))
                    .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 45, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton2)
                    .addComponent(jButton3))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        FileChooserBuilder builder = new FileChooserBuilder("");
        builder.setDirectoriesOnly(true);
        builder.setTitle("Select Blender Scripts Directory");
        File file = builder.showOpenDialog();
        if (file != null) {
            jTextField1.setText(file.getPath());
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        installBlenderPlugin(jTextField1.getText());
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        dispose();
    }//GEN-LAST:event_jButton3ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JTextField jTextField1;
    // End of variables declaration//GEN-END:variables
}
