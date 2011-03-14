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

package com.jme3.network.connection;

import javax.net.ssl.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.logging.Level;

/**
 * The SSLTCPConnection. Handles all SSL traffic for both client
 *  and server. Please do not use this class, as it does not work.
 * Replacement is custom encryption over TCP or UDP, without using SSL.
 *
 * @author Lars Wesselius
 */
public class SSLTCPConnection extends TCPConnection {

    // Incoming data. Encrypted.
    protected ByteBuffer    incDataEncrypted;

    // Incoming data. Decrypted.
    protected ByteBuffer    incDataDecrypted;

    // Outgoing data. Encrypted.
    protected ByteBuffer    outDataEncrypted;

    // Used for operations that don't consume any data.
    protected ByteBuffer    dummy;

    protected SSLEngine     sslEngine;
    protected boolean       initialHandshake;
    protected SSLEngineResult.HandshakeStatus
                            handshakeStatus;
    protected SSLEngineResult.Status
                            status;

    protected ArrayList<Client>
                            handshakingConnectors = new ArrayList<Client>();

    public SSLTCPConnection(String name) {
        label = name;
        createSSLEngine();


        SSLSession session = sslEngine.getSession();

        incDataDecrypted    = ByteBuffer.allocateDirect(session.getApplicationBufferSize());
        incDataEncrypted    = ByteBuffer.allocateDirect(session.getPacketBufferSize());
        outDataEncrypted    = ByteBuffer.allocateDirect(session.getPacketBufferSize());

        incDataEncrypted.position(incDataEncrypted.limit());
		outDataEncrypted.position(outDataEncrypted.limit());
        dummy               = ByteBuffer.allocate(0);
    }

    private void createSSLEngine() {
        try
        {
            KeyStore ks = KeyStore.getInstance("JKS");
            File kf = new File("keystore");
            ks.load(new FileInputStream(kf), "lollercopter".toCharArray());

            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, "lollercopter".toCharArray());

            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ks);

            TrustManager[] trustAllCerts = new TrustManager[]
            {
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), trustAllCerts, null);

            sslEngine = sslContext.createSSLEngine();
        } catch (Exception e) {
            log.log(Level.SEVERE, "[{0}][TCP] Could not create SSL engine: {1}", new Object[]{label, e.getMessage()});
        }
    }

    private void doHandshake(SocketChannel channel) throws IOException {
        while (true) {
            SSLEngineResult result;
            log.log(Level.FINEST, "[{0}][TCP] Handshake Status is now {1}.", new Object[]{label, handshakeStatus});
            switch (handshakeStatus) {
                case NOT_HANDSHAKING:
                    log.log(Level.SEVERE, "[{0}][TCP] We're doing a handshake while we're not handshaking.", label);
                    break;

                case FINISHED:
                    initialHandshake = false;
                    channel.keyFor(selector).interestOps(SelectionKey.OP_READ);

                    return;

                case NEED_TASK:
                    // TODO: Run this task in another thread or something.
                    Runnable task;
                    while ((task = sslEngine.getDelegatedTask()) != null) {
                        task.run();
                    }
                    handshakeStatus = sslEngine.getHandshakeStatus();
                    break;

                case NEED_UNWRAP:
                    readAndUnwrap(channel);

                    if (initialHandshake && status == SSLEngineResult.Status.BUFFER_UNDERFLOW) {
                        channel.keyFor(selector).interestOps(SelectionKey.OP_READ);
                        return;
                    }

                    break;

                case NEED_WRAP:

                    if (outDataEncrypted.hasRemaining()) {
                        log.log(Level.FINE, "[{0}][TCP] We found data that should be written out.", label);
                        return;
                    }

                    // Prepare to write
                    outDataEncrypted.clear();
                    result = sslEngine.wrap(dummy, outDataEncrypted);
                    log.log(Level.FINEST, "[{0}][TCP] Wrapping result: {1}.", new Object[]{label, result});

                    if (result.bytesProduced() == 0) log.log(Level.SEVERE, "[{0}][TCP] No net data produced during wrap.", label);
                    if (result.bytesConsumed() != 0) log.log(Level.SEVERE, "[{0}][TCP] App data consumed during handshake wrap.", label);
                    handshakeStatus = result.getHandshakeStatus();
                    outDataEncrypted.flip();

                    // Now send the data and come back here only when
                    // the data is all sent
                    System.out.println("WRITING TO: " + channel + " : " + channel.socket());
                    if (!flushData(channel)) {
                        // There is data left to be send. Wait for it
                        return;
                    }
                    break;

            }
        }
    }

    public void connect(SocketAddress address) throws IOException {
        super.connect(address);
    }

    public void bind(SocketAddress address) throws IOException {
        super.bind(address);

        sslEngine.setUseClientMode(false);
        sslEngine.setNeedClientAuth(false);
    }

    public void connect(SelectableChannel channel) throws IOException {
        super.connect(channel);
        initialHandshake = true;
        sslEngine.setUseClientMode(true);
        sslEngine.beginHandshake();
        socketChannel.keyFor(selector).interestOps(SelectionKey.OP_WRITE);
        handshakeStatus = sslEngine.getHandshakeStatus();
        doHandshake(socketChannel);
    }

    public void accept(SelectableChannel channel) throws IOException {
        super.accept(channel);

        Client con = connections.get(connections.size() - 1);
        handshakingConnectors.add(con);
        //con.getChannel().keyFor(selector).interestOps(SelectionKey.OP_WRITE);

        initialHandshake = true;
        sslEngine.beginHandshake();
        handshakeStatus = sslEngine.getHandshakeStatus();
        doHandshake(con.getSocketChannel());
    }

    public void read(SelectableChannel channel) throws IOException {
        if (initialHandshake) {
            doHandshake((SocketChannel)channel);
            return;
        }
        super.read(channel);
    }

    public void readAndUnwrap(SocketChannel channel) throws IOException {
        incDataEncrypted.flip();
        int bytesRead = channel.read(incDataEncrypted);

        if (bytesRead == 0) {
            System.out.println("BUFFER INFO: " + incDataEncrypted);
        }
        if (bytesRead == -1) {
            log.log(Level.FINE, "[{0}][TCP] -1 bytes read, closing stream.", new Object[]{label, bytesRead});
            return;
        }
        log.log(Level.FINE, "[{0}][TCP] Read {1} bytes.", new Object[]{label, bytesRead});

        incDataDecrypted.clear();
        incDataEncrypted.flip();

        SSLEngineResult result;
        do {
            result = sslEngine.unwrap(incDataEncrypted, incDataDecrypted);
            log.log(Level.FINE, "[{0}][TCP] Unwrap result: {1}.", new Object[]{label, result});
        } while (result.getStatus() == SSLEngineResult.Status.OK &&
                result.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_UNWRAP &&
                result.bytesProduced() == 0);

        // We could have finished the handshake.
        if (result.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.FINISHED) {
            initialHandshake = false;
            channel.keyFor(selector).interestOps(SelectionKey.OP_READ);
        }

        // Check if we unwrapped everything there is to unwrap.
        if (incDataDecrypted.position() == 0 &&
            result.getStatus() == SSLEngineResult.Status.OK && incDataEncrypted.hasRemaining()) {
            result = sslEngine.unwrap(incDataEncrypted, incDataDecrypted);
            log.log(Level.FINE, "[{0}][TCP] Unwrap result: {1}.", new Object[]{label, result});
        }

        // Update statuses
        status = result.getStatus();
        handshakeStatus = result.getHandshakeStatus();

        incDataEncrypted.compact();
        incDataDecrypted.flip();

        if (handshakeStatus == SSLEngineResult.HandshakeStatus.NEED_TASK ||
            handshakeStatus == SSLEngineResult.HandshakeStatus.NEED_WRAP ||
            handshakeStatus == SSLEngineResult.HandshakeStatus.FINISHED)
        {
            log.log(Level.FINE, "[{0}][TCP] Rehandshaking..", label);
            doHandshake(channel);
        }

    }

    public void send(Object object) throws IOException {
        super.sendObject(object);
    }

    public void send(SocketChannel channel, Object object) throws IOException {
        super.send(channel, object);
    }

    public void write(SelectableChannel channel) throws IOException {
		//super.write(channel);
        SocketChannel socketChannel = (SocketChannel)channel;

        if (flushData(socketChannel)) {
            if (initialHandshake) {
                doHandshake(socketChannel);
            }
        }
    }

    private boolean flushData(SocketChannel channel) throws IOException {
		int written = 0;
		try {
            ///
            while (outDataEncrypted.hasRemaining()) {
			    written += channel.write(outDataEncrypted);
            }
		} catch (IOException ioe) {
			outDataEncrypted.position(outDataEncrypted.limit());
			throw ioe;
		}
        
        log.log(Level.FINE, "[{0}][TCP] Wrote {1} bytes to {2}.", new Object[]{label, written, channel.socket().getRemoteSocketAddress()});
		if (outDataEncrypted.hasRemaining()) {
			SelectionKey key = channel.keyFor(selector);
            key.interestOps(SelectionKey.OP_WRITE);
			return false;
		} else {
            return true;
		}
	}


}
