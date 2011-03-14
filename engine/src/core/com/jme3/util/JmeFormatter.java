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

package com.jme3.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * More simple formatter than the default one used in Java logging.
 * Example output: <br/>
 * INFO Display3D 12:00 PM: Display created.
 */
public class JmeFormatter extends Formatter {

    private Date calendar = new Date();
    private String lineSeperator;
    private MessageFormat format;
    private Object args[] = new Object[1];
    private StringBuffer store = new StringBuffer();

    public JmeFormatter(){
        lineSeperator = System.getProperty("line.separator");
        format = new MessageFormat("{0,time}");
    }

    @Override
    public String format(LogRecord record) {
        StringBuffer sb = new StringBuffer();

        calendar.setTime(record.getMillis());
        args[0] = calendar;
        store.setLength(0);
        format.format(args, store, null);

        String clazz = null;
        try{
            clazz = Class.forName(record.getSourceClassName()).getSimpleName();
        } catch (ClassNotFoundException ex){
        }
        
        sb.append(record.getLevel().getLocalizedName()).append(" ");
        sb.append(clazz).append(" ");
        sb.append(store.toString()).append(" ");
        sb.append(formatMessage(record)).append(lineSeperator);

        if (record.getThrown() != null) {
            try {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                record.getThrown().printStackTrace(pw);
                pw.close();
                sb.append(sw.toString());
            } catch (Exception ex) {
            }
        }

        return sb.toString();
    }
}
