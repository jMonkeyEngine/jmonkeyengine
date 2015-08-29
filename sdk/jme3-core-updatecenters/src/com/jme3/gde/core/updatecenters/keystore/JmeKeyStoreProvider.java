package com.jme3.gde.core.updatecenters.keystore;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import org.netbeans.spi.autoupdate.KeyStoreProvider;
import org.openide.util.Exceptions;

/**
 * Loads the jMonkeyEngine SDK Plugins certificates into the AutoUpdate system.
 *
 * @author Kirill Vainer
 */
public class JmeKeyStoreProvider implements KeyStoreProvider {

    @Override
    public KeyStore getKeyStore() {
        InputStream in = null;
        try {
            in = JmeKeyStoreProvider.class.getResourceAsStream("trustedcerts.jks");
            KeyStore store = KeyStore.getInstance("JKS");
            store.load(in, "trustedcerts".toCharArray());
            return store;
        } catch (KeyStoreException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } catch (NoSuchAlgorithmException ex) {
            Exceptions.printStackTrace(ex);
        } catch (CertificateException ex) {
            Exceptions.printStackTrace(ex);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }
        return null;
    }

}
