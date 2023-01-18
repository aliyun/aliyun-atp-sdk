/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package sun.tools.attach;

import java.io.IOException;

// JNI methods reflection
public class VirtualMachineImpl {
    public static native void sendQuitTo(int pid) throws IOException;

    public static native void checkPermissions(String path) throws IOException;

    public static native int socket() throws IOException;

    public static native void connect(int fd, String path) throws IOException;

    public static native void close(int fd) throws IOException;

    public static native int read(int fd, byte[] buf, int off, int bufLen) throws IOException;

    public static native void write(int fd, byte[] buf, int off, int bufLen) throws IOException;

    public static native void createAttachFile0(String path);

    public static native String getTempDir();

    public static native void init();

    public static native byte[] generateStub();

    public static native long openProcess(int pid) throws IOException;

    public static native void closeProcess(long hProcess) throws IOException;

    public static native long createPipe(String name) throws IOException;

    public static native void closePipe(long hPipe) throws IOException;

    public static native void connectPipe(long hPipe) throws IOException;

    public static native int readPipe(long hPipe, byte[] buf, int off, int buflen) throws IOException;

    public static native void enqueue(long hProcess, byte[] stub,
                                      String cmd, String pipename, Object... args) throws IOException;

    public static int connectToVM(String socketPath) throws IOException {
        int fd = socket();
        try {
            connect(fd, socketPath);
        } catch (IOException x) {
            close(fd);
            throw x;
        }
        return fd;
    }

    public static void checkConnection(String socketPath) throws IOException {
        int fd = socket();
        try {
            connect(fd, socketPath);
        } finally {
            close(fd);
        }
    }
}
