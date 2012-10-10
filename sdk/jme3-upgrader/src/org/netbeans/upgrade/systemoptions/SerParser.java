/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.netbeans.upgrade.systemoptions;

import java.io.*;
import java.util.*;
import org.openide.util.NotImplementedException;

/* The following may be useful to Emacs users:

(defun comma-sep-decimal-encode ()
  (interactive)
  (while
      (let ((p (point)))
        (and (re-search-forward "\\(-?[0-9][0-9]?[0-9]?\\),?" nil t)
             (= p (match-beginning 0))))
    (replace-match (char-to-string
                    (let ((x (string-to-int (match-string 1))))
                      (when (< x 0) (setq x (+ x 256)))
                      x))
                   t t)))

After evaluating this, go into an old cpanel.xml or similar
(M-x find-file-literally, by the way) and type
M-x comma-sep-decimal-encode right after the opening quote
of the value. The contents will be converted to raw binary and
may be saved as a .ser file parsable by this class.

(defun hex-encode ()
  (interactive)
  (while
      (let ((p (point)))
        (and (re-search-forward "\\s-*\\([a-fA-F0-9][a-fA-F0-9]\\)" nil t)
             (= p (match-beginning 0))))
    (replace-match (char-to-string
                    (string-to-int (match-string 1) 16))
                   t t)))

Same for hexadecimal serialized data. For .settings files, it is
easier to select Customize Bean... in the IDE and save as *.ser.
*/

/** Parser for Java serialization files.
 * Does no classloading or per-class semantics, simply parses the
 * raw serialization structure.
 * @author Jesse Glick
 */
public final class SerParser implements ObjectStreamConstants {
    
    private static final boolean DEBUG = Boolean.getBoolean("org.netbeans.modules.clazz.SerParser.DEBUG"); // NOI18N
    
    private final InputStream is;
    private int seq = 0;
    private final List<Object> refs = new ArrayList<Object>(100);
    
    public SerParser(InputStream is) {
        this.is = is;
    }
    
    private int makeRef(Object o) {
        refs.add(o);
        int i = seq;
        seq++;
        if (DEBUG) System.err.println("makeRef[" + i + "]=" + o); // NOI18N
        return i;
    }
    
    private Object getRef(int i) throws CorruptException {
        int idx = i - baseWireHandle;
        if (idx < 0 || idx >= seq) throw new CorruptException("Invalid reference: " + i); // NOI18N
        Object o = refs.get(idx);
        if (o == null) throw new CorruptException("Invalid reference: " + i); // NOI18N
        return o;
    }
    
    public Stream parse() throws IOException, CorruptException {
        Stream s = new Stream();
        s.magic = readShort();
        s.version = readShort();
        if (s.magic != STREAM_MAGIC || s.version != STREAM_VERSION) {
           throw new CorruptException("stream version mismatch: " + hexify(s.magic) + " != " + hexify(STREAM_MAGIC) + " or " + hexify(s.version) + " != " +  hexify(STREAM_VERSION)); // NOI18N
        }
        s.contents = new ArrayList<Object>(10);
        while (peek() != -1) {
            s.contents.add(readContent());
        }
        if (DEBUG) System.err.println("parsed: " + s); // NOI18N
        return s;
    }
    
    public static final class CorruptException extends IOException {
        public CorruptException() {
        }
        public CorruptException(String m) {
            super(m);
        }
    }
    
    private int pushback = -1;
    private int rb() throws IOException {
        if (pushback != -1) {
            int c = pushback;
            pushback = -1;
            return c;
        }
        int c = is.read();
        if (DEBUG) System.err.println("read: " + Integer.toHexString(c)); // NOI18N
        if (c == -1) {
            throw new EOFException();
        } else {
            return c;
        }
    }
    private int peek() throws IOException {
        if (pushback != -1) throw new IllegalStateException("can only peek once"); // NOI18N
        pushback = is.read();
        if (DEBUG) System.err.println("read: " + Integer.toHexString(pushback)); // NOI18N
        return pushback;
    }
    
    static String hexify(byte b) {
        int i = b;
        if (i < 0) i += 256;
        String s = Integer.toHexString(i).toUpperCase(Locale.US);
        return "0x" + pad(s, 2); // NOI18N
    }
    static String hexify(short s) {
        int i = s;
        if (i < 0) i += 65536;
        String st = Integer.toHexString(i).toUpperCase(Locale.US);
        return "0x" + pad(st, 4); // NOI18N
    }
    static String hexify(int i) {
        String s = Integer.toHexString(i).toUpperCase(Locale.US);
        return "0x" + pad(s, 4); // NOI18N
    }
    static String hexify(long l) {
        String s1 = Integer.toHexString((int)((l & 0xFFFFFFFF00000000L) << 32)).toUpperCase(Locale.US);
        String s2 = Integer.toHexString((int)(l & 0x00000000FFFFFFFFL)).toUpperCase(Locale.US);
        return "0x" + pad(s1, 4) + pad(s2, 4); // NOI18N
    }
    static String hexify(byte[] b) {
        StringBuffer buf = new StringBuffer(2 + b.length * 2);
        buf.append("0x"); // NOI18N
        for (int i = 0; i < b.length; i++) {
            int x = b[i];
            if (x < 0) x += 256;
            buf.append(pad(Integer.toHexString(x).toUpperCase(Locale.US), 2));
        }
        return buf.toString();
    }
    private static String pad(String s, int size) {
        int i = s.length();
        if (i == size) {
            return s;
        } else {
            StringBuffer b = new StringBuffer(size);
            for (int k = 0; k < size - i; k++) {
                b.append('0'); // NOI18N
            }
            b.append(s);
            return b.toString();
        }
    }
    
    private long readLong() throws IOException {
        long x1 = rb();
        long x2 = rb();
        long x3 = rb();
        long x4 = rb();
        long x5 = rb();
        long x6 = rb();
        long x7 = rb();
        long x8 = rb();
        long l = (x1 << 56) + (x2 << 48) + (x3 << 40) + (x4 << 32) + (x5 << 24) + (x6 << 16) + (x7 << 8) + x8;
        if (DEBUG) System.err.println("readLong: " + l); // NOI18N
        return l;
    }
    
    private int readInt() throws IOException {
        int x1 = rb();
        int x2 = rb();
        int x3 = rb();
        int x4 = rb();
        int i = (x1 << 24) + (x2 << 16) + (x3 << 8) + x4;
        if (DEBUG) System.err.println("readInt: " + i); // NOI18N
        return i;
    }
    
    private short readShort() throws IOException {
        int x1 = rb();
        int x2 = rb();
        short s = (short)((x1 << 8) + x2);
        //System.err.println("x1=" + hexify(x1) + " x2=" + hexify(x2) + " s=" + hexify(s));
        //if (DEBUG) System.err.println("x1=" + x1 + " x2=" + x2 + " s=" + s);
        if (DEBUG) System.err.println("readShort: " + s); // NOI18N
        return s;
    }
    
    private byte readByte() throws IOException {
        return (byte)rb();
    }
    
    private String readUTF() throws IOException {
        short len = readShort();
        if (len < 0) throw new NotImplementedException();//XXX
        byte[] buf = new byte[len];
        for (int i = 0; i < len; i++) {
            buf[i] = readByte();
        }
        String s = new String(buf, "UTF-8"); // NOI18N
        if (DEBUG) System.err.println("readUTF: " + s); // NOI18N
        return s;
    }
    
    /*
    private String readLongUTF() throws IOException {
        long len = readLong();
        if (len < 0) throw new NotImplementedException();//XXX
        if (len > Integer.MAX_VALUE) throw new NotImplementedException();// XXX
        int ilen = (int)len;
        byte[] buf = new byte[ilen];
        for (int i = 0; i < ilen; i++) {
            buf[i] = readByte();
        }
        String s = new String(buf, "UTF-8"); // NOI18N
        if (DEBUG) System.err.println("readUTF: " + s); // NOI18N
        return s;
    }
     */
    
    // See "Rules of the Grammar" in Java Object Serialization Specification
    // for explanation of all these objects.
    
    public static final class Stream /*extends Thing*/ {
        public short magic;
        public short version;
        public List<Object> contents;
        public String toString() {
            return "Stream[contents=" + contents + "]"; // NOI18N
        }
    }
    
    public static final Object NULL = "null"; // NOI18N
    
    private Object readContent() throws IOException {
        byte tc = readByte();
        switch (tc) {
        case TC_OBJECT:
            return readNewObject();
        case TC_CLASS:
            return readNewClass();
        case TC_ARRAY:
            return readNewArray();
        case TC_CLASSDESC:
            return readNewClassDesc();
        case TC_PROXYCLASSDESC:
            // XXX too complicated:
            throw new NotImplementedException("TC_PROXYCLASSDESC"); // NOI18N
            //return readNewProxyClassDesc();
        case TC_STRING:
            return readNewString();
        case TC_LONGSTRING:
            // XXX later
            throw new NotImplementedException("TC_LONGSTRING"); // NOI18N
            //return readNewLongString();
        case TC_REFERENCE:
            return readReference();
        case TC_NULL:
            return NULL;
        case TC_EXCEPTION:
            // XXX what is this??
            throw new NotImplementedException("TC_EXCEPTION"); // NOI18N
        case TC_RESET:
            // XXX what is this??
            throw new NotImplementedException("TC_RESET"); // NOI18N
        case TC_BLOCKDATA:
            return readBlockData();
        case TC_BLOCKDATALONG:
            return readBlockDataLong();
        default:
            throw new CorruptException("Unknown typecode: " + hexify(tc)); // NOI18N
        }
    }
    
    public static final class ObjectWrapper {
        public ClassDesc classdesc;
        public List<Object> data; // <Union2<NameValue,Object>>
        public String toString() {
            return "Object[class=" + classdesc.name + ",data=<omitted>]"; // NOI18N
        }
    }
    
    public static final class NameValue {
        public NameValue(FieldDesc name, Object value) {
            this.name = name;
            this.value = value;
        }
        public final FieldDesc name;
        public final Object value;
        public String toString() {
            return name.toString() + "=" + value.toString(); // NOI18N
        }
    }
    
    public static final class ClassDesc {
        public String name;
        public long svuid;
        public boolean writeMethod;
        public boolean blockData;
        public boolean serializable;
        public boolean externalizable;
        public List<FieldDesc> fields;
        public List annotation; // List<Object>
        public ClassDesc superclass;
        public String toString() {
            return "Class[name=" + name + "]"; // NOI18N
        }
    }
    
    private ObjectWrapper readNewObject() throws IOException {
        ObjectWrapper ow = new ObjectWrapper();
        ow.classdesc = readClassDesc();
        makeRef(ow);
        ow.data = new ArrayList<Object> (10);
        LinkedList<ClassDesc> hier = new LinkedList<ClassDesc>();
        for (ClassDesc cd = ow.classdesc; cd != null; cd = cd.superclass) {
            hier.addFirst(cd);
        }
        for (ClassDesc cd: hier) {
            if (cd.serializable) {
                ow.data.addAll(readNoWrClass(cd));
                if (cd.writeMethod) {
                    ow.data.addAll(readContents());
                }
            } else {
                if (cd.blockData) {
                    ow.data.addAll(readContents());
                } else {
                    // Old externalization. If this is not object content,
                    // the stream could now become corrupted. Oh well.
                    ow.data.add(readContent());
                }
            }
        }
        if (DEBUG) System.err.println("readNewObject: " + ow); // NOI18N
        return ow;
    }
    
    private ClassDesc readClassDesc() throws IOException {
        Object o = readContent();
        if (o instanceof ClassDesc) {
            return (ClassDesc)o;
        } else if (o == NULL) {
            return null;
        } else {
            throw new CorruptException("Expected class desc, got: " + o); // NOI18N
        }
    }
    
    private ClassDesc readNewClass() throws IOException {
        ClassDesc cd = readClassDesc();
        makeRef(cd);
        return cd;
    }
    
    private ClassDesc readNewClassDesc() throws IOException {
        ClassDesc cd = new ClassDesc();
        cd.name = readUTF();
        if (! cd.name.startsWith("[") && // NOI18N
                ! (cd.name.length() == 1 && "BSIJFDCZ".indexOf(cd.name) != -1) && // NOI18N
                ! cd.name.endsWith(";")) { // NOI18N
            // Canonicalize. It seems class names read normally need this; those
            // read as part of an array do not. ??
            cd.name = "L" + cd.name + ";"; // NOI18N
        }
        cd.svuid = readLong();
        makeRef(cd);
        byte cdf = readByte();
        cd.writeMethod = (cdf & SC_WRITE_METHOD) != 0;
        cd.blockData = (cdf & SC_BLOCK_DATA) != 0;
        cd.serializable = (cdf & SC_SERIALIZABLE) != 0;
        cd.externalizable = (cdf & SC_EXTERNALIZABLE) != 0;
        short count = readShort();
        cd.fields = new ArrayList<FieldDesc>(count);
        for (int i = 0; i < count; i++) {
            cd.fields.add(readFieldDesc());
        }
        cd.annotation = readContents();
        cd.superclass = readClassDesc();
        if (DEBUG) System.err.println("readNewClassDesc: " + cd); // NOI18N
        return cd;
    }
    
    public static class FieldDesc {
        public String name;
        public String type;
        public String toString() {
            return "Field[name=" + name + ",type=" + type + "]"; // NOI18N
        }
    }
    public static final class ObjFieldDesc extends FieldDesc {
        public boolean array;
        public String toString() {
            return "Field[name=" + name + ",type=" + type + (array ? "[]" : "") + "]"; // NOI18N
        }
    }
    
    private FieldDesc readFieldDesc() throws IOException {
        char tc = (char)readByte();
        FieldDesc fd;
        switch (tc) {
        case 'B':
        case 'C':
        case 'D':
        case 'F':
        case 'I':
        case 'J':
        case 'S':
        case 'Z':
            fd = new FieldDesc();
            fd.type = new String(new char[] {tc});
            break;
        case '[':
            fd = new ObjFieldDesc();
            ((ObjFieldDesc)fd).array = true;
            break;
        case 'L':
            fd = new ObjFieldDesc();
            ((ObjFieldDesc)fd).array = false;
            break;
        default:
            throw new CorruptException("Strange field type: " + tc); // NOI18N
        }
        fd.name = readUTF();
        if (fd instanceof ObjFieldDesc) {
            String clazz = (String)readContent();
            /*
            if (((ObjFieldDesc)fd).array) {
                if (! clazz.startsWith("[")) throw new CorruptException("Field type: " + clazz); // NOI18N
                clazz = clazz.substring(1, clazz.length());
            }
            if (! (clazz.startsWith("L") && clazz.endsWith(";"))) throw new CorruptException("Field type: " + clazz); // NOI18N
            fd.type = clazz.substring(1, clazz.length() - 1).replace('/', '.'); // NOI18N
             */
            fd.type = clazz;
        }
        if (DEBUG) System.err.println("readFieldDesc: " + fd); // NOI18N
        return fd;
    }
    
    private List<Object> readContents() throws IOException {
        List<Object> l = new ArrayList<Object>(10);
        while (peek() != TC_ENDBLOCKDATA) {
            l.add(readContent());
        }
        if (readByte() != TC_ENDBLOCKDATA) throw new IllegalStateException();
        if (DEBUG) System.err.println("readContents: " + l); // NOI18N
        return l;
    }
    
    public static final class ArrayWrapper {
        public ClassDesc classdesc;
        public List<Object> values;
        public String toString() {
            return classdesc.name + "{" + values + "}"; // NOI18N
        }
    }
    
    private ArrayWrapper readNewArray() throws IOException {
        ArrayWrapper aw = new ArrayWrapper();
        aw.classdesc = readClassDesc();
        makeRef(aw);
        int size = readInt();
        if (size < 0) throw new NotImplementedException();
        aw.values = new ArrayList<Object>(size);
        for (int i = 0; i < size; i++) {
            if (aw.classdesc.name.equals("[B")) { // NOI18N
                aw.values.add(new Byte(readByte()));
            } else if (aw.classdesc.name.equals("[S")) { // NOI18N
                aw.values.add(new Short(readShort()));
            } else if (aw.classdesc.name.equals("[I")) { // NOI18N
                aw.values.add(new Integer(readInt()));
            } else if (aw.classdesc.name.equals("[J")) { // NOI18N
                aw.values.add(new Long(readLong()));
            } else if (aw.classdesc.name.equals("[F")) { // NOI18N
                aw.values.add(new Float(Float.intBitsToFloat(readInt())));
            } else if (aw.classdesc.name.equals("[D")) { // NOI18N
                aw.values.add(new Double(Double.longBitsToDouble(readLong())));
            } else if (aw.classdesc.name.equals("[C")) { // NOI18N
                aw.values.add(new Character((char)readShort()));
            } else if (aw.classdesc.name.equals("[Z")) { // NOI18N
                aw.values.add(readByte() == 1 ? Boolean.TRUE : Boolean.FALSE);
            } else {
                aw.values.add(readContent());
            }
        }
        if (DEBUG) System.err.println("readNewArray: " + aw); // NOI18N
        return aw;
    }
    
    private String readNewString() throws IOException {
        String s = readUTF();
        makeRef(s);
        return s;
    }
    
    private Object readReference() throws IOException {
        int i = readInt();
        Object r = getRef(i);
        if (DEBUG) System.err.println("readReference: " + r); // NOI18N
        return r;
    }
    
    private byte[] readBlockData() throws IOException {
        int size = readByte();
        if (size < 0) size += 256;
        byte[] b = new byte[size];
        for (int i = 0; i < size; i++) {
            b[i] = readByte();
        }
        if (DEBUG) System.err.println("readBlockData: " + size + " bytes"); // NOI18N
        return b;
    }
    
    private byte[] readBlockDataLong() throws IOException {
        int size = readInt();
        if (size < 0) throw new NotImplementedException();
        byte[] b = new byte[size];
        for (int i = 0; i < size; i++) {
            b[i] = readByte();
        }
        if (DEBUG) System.err.println("readBlockDataLong: " + size + " bytes"); // NOI18N
        return b;
    }
    
    private List<NameValue> readNoWrClass(ClassDesc cd) throws IOException {
        List<FieldDesc> fields = cd.fields;
        List<NameValue> values = new ArrayList<NameValue>(fields.size());
        for (int i = 0; i < fields.size(); i++) {
            FieldDesc fd = (FieldDesc)fields.get(i);
            if (fd.type.equals("B")) { // NOI18N
                values.add(new NameValue(fd, new Byte(readByte())));
            } else if (fd.type.equals("S")) { // NOI18N
                values.add(new NameValue(fd, new Short(readShort())));
            } else if (fd.type.equals("I")) { // NOI18N
                values.add(new NameValue(fd, new Integer(readInt())));
            } else if (fd.type.equals("J")) { // NOI18N
                values.add(new NameValue(fd, new Long(readLong())));
            } else if (fd.type.equals("F")) { // NOI18N
                values.add(new NameValue(fd, new Float(Float.intBitsToFloat(readInt()))));
            } else if (fd.type.equals("D")) { // NOI18N
                values.add(new NameValue(fd, new Double(Double.longBitsToDouble(readLong()))));
            } else if (fd.type.equals("C")) { // NOI18N
                values.add(new NameValue(fd, new Character((char)readShort())));
            } else if (fd.type.equals("Z")) { // NOI18N
                values.add(new NameValue(fd, readByte() == 1 ? Boolean.TRUE : Boolean.FALSE));
            } else {
                values.add(new NameValue(fd, readContent()));
            }
        }
        if (DEBUG) System.err.println("readNoWrClass: " + values); // NOI18N
        return values;
    }

}
