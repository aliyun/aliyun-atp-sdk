/* Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2022, Alibaba Group Holding Limited. All Rights Reserved.
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
package com.aliyun.atp.client;

import sun.tools.attach.BsdVirtualMachine;
import sun.tools.attach.LinuxVirtualMachine;
import sun.tools.attach.VirtualMachineImpl;
import sun.tools.attach.WindowsVirtualMachine;

import java.io.*;
import java.lang.reflect.Constructor;
import java.util.Random;

// Do not use advance syntax and JDK methods, this class is expected to work with JRE6
@SuppressWarnings("unused")
public abstract class HotSpotVM {
    private static final String OS_NAME = System.getProperty("os.name");
    private static final String JAVA_VERSION = System.getProperty("java.version");
    private final static String PROTOCOL_VERSION = "1";
    private final static String TMP_DIR = "/tmp";
    private final static int ATTACH_ERROR_BAD_VERSION = 101;

    private static boolean isMacOS() {
        return OS_NAME.toLowerCase().contains("mac");
    }

    private static boolean isWindows() {
        return OS_NAME.toLowerCase().contains("win");
    }

    private static boolean isLinux() {
        return OS_NAME.toLowerCase().contains("nux");
    }

    public static HotSpotVM creatHotSpotVM(int pid) throws Exception {
        System.loadLibrary("attach");

        String expectedVM;
        expectedVM = (getJreVersion() < 9 ? "Old" : "New");
        expectedVM += isMacOS() ? "Bsd" : "";
        expectedVM += isLinux() ? "Linux" : "";
        expectedVM += isWindows() ? "Windows" : "";
        expectedVM += "HotSpotVM";
        Class<?>[] vmSet = HotSpotVM.class.getDeclaredClasses();
        for (Class<?> vm : vmSet) {
            if (vm.getSimpleName().equals(expectedVM)) {
                Constructor<? extends HotSpotVM> ctor =
                    (Constructor<? extends HotSpotVM>) vm.getConstructor(int.class/*pid*/);
                return ctor.newInstance(pid);
            }
        }
        throw new Exception("Can not find expected VM for JRE:" + getJreVersion() + ", OS:" + OS_NAME);
    }

    private static int getJreVersion() {
        String version = JAVA_VERSION;
        if (version.startsWith("1.")) {
            version = version.substring(2, 3);
        } else {
            int dot = version.indexOf(".");
            if (dot != -1) {
                version = version.substring(0, dot);
            }
        }
        return Integer.parseInt(version);
    }

    protected static void readRemaining(InputStream sis) throws IOException {
        byte[] b = new byte[256];
        int n;
        do {
            n = sis.read(b);
            if (n > 0) {
                String s = new String(b, 0, n, "UTF-8");
                System.out.print(s);
            }
        } while (n > 0);
        sis.close();
    }

    protected static String readErrorMessage(InputStream sis) throws IOException {
        byte[] b = new byte[1024];
        int n;
        StringBuilder message = new StringBuilder();
        while ((n = sis.read(b)) != -1) {
            message.append(new String(b, 0, n, "UTF-8"));
        }
        return message.toString();
    }

    protected static int readInt(InputStream sis) throws IOException {
        StringBuilder sb = new StringBuilder();

        // read to \n or EOF
        int n;
        byte[] buf = new byte[1];
        do {
            n = sis.read(buf, 0, 1);
            if (n > 0) {
                char c = (char) buf[0];
                if (c == '\n') {
                    break;
                } else {
                    sb.append(c);
                }
            }
        } while (n > 0);

        if (sb.length() == 0) {
            throw new IOException("Premature EOF");
        }

        int value;
        try {
            value = Integer.parseInt(sb.toString());
        } catch (NumberFormatException x) {
            throw new IOException("Non-numeric value found - int expected");
        }
        return value;
    }

    private static int readOneByte(InputStream is) throws IOException {
        byte[] b = new byte[1];
        int n = is.read(b, 0, 1);
        if (n == 1) {
            return b[0] & 0xff;
        } else {
            return -1;
        }
    }

    private static void checkBeforeRead(byte[] bs, int off, int len) {
        if ((off < 0) || (off > bs.length) || (len < 0) ||
            ((off + len) > bs.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        }
    }

    private static void checkExecReturn(IOException ioe, String cmd, InputStream sis) throws Exception {
        int completionStatus;
        try {
            completionStatus = readInt(sis);
        } catch (IOException x) {
            sis.close();
            if (ioe != null) {
                throw ioe;
            } else {
                throw x;
            }
        }

        if (completionStatus != 0) {
            String message = readErrorMessage(sis);
            sis.close();
            if (completionStatus == ATTACH_ERROR_BAD_VERSION) {
                throw new IOException("Invalid protocol when interacting with attach listener");
            }
            if (cmd.equals("load")) {
                throw new Exception("Failed to load agent library due to" + message);
            } else {
                throw new Exception("Failed to execute command " + cmd + " " + message);
            }
        }
    }

    private static void writeString(int fd, String s) throws Exception {
        if (s.length() > 0) {
            byte[] b;
            b = s.getBytes("UTF-8");
            if (getJreVersion() < 9) {
                if (isMacOS()) {
                    BsdVirtualMachine.write(fd, b, 0, b.length);
                } else if (isLinux()) {
                    LinuxVirtualMachine.write(fd, b, 0, b.length);
                } else {
                    throw new Exception("Should not reach here");
                }
            } else {
                if (isWindows()) {
                    throw new Exception("Should not reach here");
                }
                VirtualMachineImpl.write(fd, b, 0, b.length);
            }
        }
        byte[] b = new byte[1];
        if (getJreVersion() < 9) {
            if (isMacOS()) {
                BsdVirtualMachine.write(fd, b, 0, 1);
            } else if (isLinux()) {
                LinuxVirtualMachine.write(fd, b, 0, 1);
            } else {
                throw new Exception("Should not reach here");
            }
        } else {
            if (isWindows()) {
                throw new Exception("Should not reach here");
            }
            VirtualMachineImpl.write(fd, b, 0, 1);
        }
    }

    private static IOException writeCommand(int fd, String cmd, Object... args) throws Exception {
        IOException ioe = null;

        try {
            writeString(fd, PROTOCOL_VERSION);
            writeString(fd, cmd);

            for (int i = 0; i < 3; i++) {
                if (i < args.length && args[i] != null) {
                    writeString(fd, (String) args[i]);
                } else {
                    writeString(fd, "");
                }
            }
        } catch (IOException x) {
            ioe = x;
        }
        return ioe;
    }

    public abstract void detach() throws IOException;

    public abstract void execute(String cmd, Object... args) throws Exception;

    private static class OldWindowsHotSpotVM extends HotSpotVM {
        private static final byte[] stub;

        static {
            WindowsVirtualMachine.init();
            stub = WindowsVirtualMachine.generateStub();
        }

        private volatile long proc;

        public OldWindowsHotSpotVM(int pid) throws Exception {
            proc = WindowsVirtualMachine.openProcess(pid);
            // The target VM might be a pre-6.0 VM so we enqueue a "null" command
            try {
                WindowsVirtualMachine.enqueue(proc, stub, null, null);
            } catch (IOException x) {
                throw new Exception(x.getMessage());
            }
        }

        public void detach() throws IOException {
            if (proc != -1) {
                WindowsVirtualMachine.closeProcess(proc);
                proc = -1;
            }
        }

        public void execute(String cmd, Object... args) throws Exception {
            int r = (new Random()).nextInt();
            String pipeName = "\\\\.\\pipe\\javatool" + r;
            long hPipe = WindowsVirtualMachine.createPipe(pipeName);

            if (proc == -1) {
                WindowsVirtualMachine.closePipe(hPipe);
                throw new IOException("Detached from target VM");
            }

            try {
                WindowsVirtualMachine.enqueue(proc, stub, cmd, pipeName, args);
                WindowsVirtualMachine.connectPipe(hPipe);
                OldWindowsPipedInputStream is = new OldWindowsPipedInputStream(hPipe);
                HotSpotVM.checkExecReturn(null, cmd, is);
                HotSpotVM.readRemaining(is);
            } catch (IOException ioe) {
                WindowsVirtualMachine.closePipe(hPipe);
                throw ioe;
            }
        }

        private static class OldWindowsPipedInputStream extends InputStream {
            private long hPipe;

            public OldWindowsPipedInputStream(long hPipe) {
                this.hPipe = hPipe;
            }

            public synchronized int read() throws IOException {
                return HotSpotVM.readOneByte(this);
            }

            public synchronized int read(byte[] bs, int off, int len) throws IOException {
                if (len == 0) {
                    return 0;
                }
                checkBeforeRead(bs, off, len);
                return WindowsVirtualMachine.readPipe(hPipe, bs, off, len);
            }

            public void close() throws IOException {
                if (hPipe != -1) {
                    WindowsVirtualMachine.closePipe(hPipe);
                    hPipe = -1;
                }
            }
        }
    }

    private static class OldBsdHotSpotVM extends HotSpotVM {
        private static final String tmpdir;

        static {
            tmpdir = BsdVirtualMachine.getTempDir();
        }

        private String path;

        public OldBsdHotSpotVM(int pid) throws Exception {
            path = findSocketFile(pid);
            if (path == null) {
                File f = new File(tmpdir, ".attach_pid" + pid);
                BsdVirtualMachine.createAttachFile(f.getPath());
                try {
                    BsdVirtualMachine.sendQuitTo(pid);
                    for (int i = 0; i < 5; i++) {
                        try {
                            Thread.sleep(1000);
                        } catch (Throwable r) {
                        }
                        path = findSocketFile(pid);
                    }
                    if (path == null) {
                        throw new Exception("Unable to open socket file after many retries");
                    }
                } finally {
                    f.delete();
                }
            }

            BsdVirtualMachine.checkPermissions(path);
            int s = BsdVirtualMachine.socket();
            try {
                BsdVirtualMachine.connect(s, path);
            } finally {
                BsdVirtualMachine.close(s);
            }
        }

        public void detach() throws IOException {
            if (this.path != null) {
                this.path = null;
            }
        }

        public void execute(String cmd, Object... args) throws Exception {
            int s = BsdVirtualMachine.socket();

            try {
                BsdVirtualMachine.connect(s, path);
            } catch (IOException x) {
                BsdVirtualMachine.close(s);
                throw x;
            }
            IOException ioe = HotSpotVM.writeCommand(s, cmd, args);
            OldBsdSocketInputStream sis = new OldBsdSocketInputStream(s);
            HotSpotVM.checkExecReturn(ioe, cmd, sis);
            HotSpotVM.readRemaining(sis);
        }

        private String findSocketFile(int pid) {
            String fn = ".java_pid" + pid;
            File f = new File(tmpdir, fn);
            return f.exists() ? f.getPath() : null;
        }

        private static class OldBsdSocketInputStream extends InputStream {
            private final int s;

            public OldBsdSocketInputStream(int s) {
                this.s = s;
            }

            public synchronized int read() throws IOException {
                return HotSpotVM.readOneByte(this);
            }

            public synchronized int read(byte[] bs, int off, int len) throws IOException {
                if (len == 0) {
                    return 0;
                }
                checkBeforeRead(bs, off, len);
                return BsdVirtualMachine.read(s, bs, off, len);
            }

            public void close() throws IOException {
                BsdVirtualMachine.close(s);
            }
        }
    }

    static class OldLinuxHotSpotVM extends HotSpotVM {
        private String path;

        public OldLinuxHotSpotVM(int pid)
            throws Exception {

            path = findSocketFile(pid);
            if (path == null) {
                File f = createAttachFile(pid);
                try {
                    // NOTE, OpenJDK does not officially support LinuxThreads anymore, and has not been tested for
                    // LinuxThreads since a long time (so support for it is probably broken by now anyway). So here
                    // WE DO NOT SUPPORT LinuxThread as well for alpine JDK8
                    // https://bugs.openjdk.org/browse/JDK-8078513
                    LinuxVirtualMachine.sendQuitTo(pid);

                    for (int i = 0; i < 5; i++) {
                        try {
                            Thread.sleep(1000);
                        } catch (Throwable r) {
                        }
                        path = findSocketFile(pid);
                    }
                    if (path == null) {
                        throw new Exception("Unable to open socket file");
                    }
                } finally {
                    f.delete();
                }
            }

            LinuxVirtualMachine.checkPermissions(path);
            int s = LinuxVirtualMachine.socket();
            try {
                LinuxVirtualMachine.connect(s, path);
            } finally {
                LinuxVirtualMachine.close(s);
            }
        }

        public void detach() throws IOException {
            if (this.path != null) {
                this.path = null;
            }
        }

        public void execute(String cmd, Object... args) throws Exception {
            int s = LinuxVirtualMachine.socket();

            try {
                LinuxVirtualMachine.connect(s, path);
            } catch (IOException x) {
                LinuxVirtualMachine.close(s);
                throw x;
            }
            IOException ioe = HotSpotVM.writeCommand(s, cmd, args);
            OldLinuxSocketInputStream sis = new OldLinuxSocketInputStream(s);
            HotSpotVM.checkExecReturn(ioe, cmd, sis);
            HotSpotVM.readRemaining(sis);
        }

        private String findSocketFile(int pid) {
            File f = new File(TMP_DIR, ".java_pid" + pid);
            if (!f.exists()) {
                return null;
            }
            return f.getPath();
        }

        private File createAttachFile(int pid) throws IOException {
            String fn = ".attach_pid" + pid;
            String path = "/proc/" + pid + "/cwd/" + fn;
            File f = new File(path);
            try {
                f.createNewFile();
            } catch (IOException x) {
                f = new File(TMP_DIR, fn);
                f.createNewFile();
            }
            return f;
        }

        private static class OldLinuxSocketInputStream extends InputStream {
            private final int fd;

            public OldLinuxSocketInputStream(int s) {
                this.fd = s;
            }

            public synchronized int read() throws IOException {
                return HotSpotVM.readOneByte(this);
            }

            public synchronized int read(byte[] bs, int off, int len) throws IOException {
                if (len == 0) {
                    return 0;
                }
                checkBeforeRead(bs, off, len);
                return LinuxVirtualMachine.read(fd, bs, off, len);
            }

            public void close() throws IOException {
                LinuxVirtualMachine.close(fd);
            }
        }
    }

    private static class NewLinuxHotSpotVM extends HotSpotVM {
        private String socketPath;

        public NewLinuxHotSpotVM(int pid) throws Exception {
            int ns_pid = getNamespacePid(pid);
            File socket_file = findSocketFile(pid, ns_pid);
            socketPath = socket_file.getPath();
            if (!socket_file.exists()) {
                File f = createAttachFile(pid, ns_pid);
                try {
                    VirtualMachineImpl.sendQuitTo(pid);
                    for (int i = 0; i < 5; i++) {
                        Thread.sleep(1000);
                        if (i >= 2 && !socket_file.exists()) {
                            VirtualMachineImpl.sendQuitTo(pid);
                        }
                    }
                    if (!socket_file.exists()) {
                        throw new Exception("Unable to open socket file " + socket_file);
                    }
                } finally {
                    f.delete();
                }
            }
            VirtualMachineImpl.checkPermissions(socketPath);
            VirtualMachineImpl.checkConnection(socketPath);
        }

        public void detach() throws IOException {
            if (socketPath != null) {
                socketPath = null;
            }
        }

        public void execute(String cmd, Object... args) throws Exception {
            int fd = VirtualMachineImpl.connectToVM(socketPath);
            IOException ioe = HotSpotVM.writeCommand(fd, cmd, args);
            NewLinuxSocketInputStream sis = new NewLinuxSocketInputStream(fd);
            HotSpotVM.checkExecReturn(ioe, cmd, sis);
            HotSpotVM.readRemaining(sis);
        }

        private File findSocketFile(int pid, int ns_pid) {
            String root = "/proc/" + pid + "/root/" + TMP_DIR;
            return new File(root, ".java_pid" + ns_pid);
        }

        private File createAttachFile(int pid, int nsPid) throws IOException {
            String fn = ".attach_pid" + nsPid;
            String path = "/proc/" + pid + "/cwd/" + fn;
            File f = new File(path);
            try {
                f.createNewFile();
            } catch (IOException x) {
                String root;
                if (pid != nsPid) {
                    root = "/proc/" + pid + "/root/" + TMP_DIR;
                } else {
                    root = TMP_DIR;
                }
                f = new File(root, fn);
                f.createNewFile();
            }
            return f;
        }

        private int getNamespacePid(int pid) throws Exception {
            String statusFile = "/proc/" + pid + "/status";
            File f = new File(statusFile);
            if (!f.exists()) {
                return pid;
            }

            BufferedReader br = null;
            FileReader fr = null;
            try {
                fr = new FileReader(f);
                br = new BufferedReader(fr);
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(":");
                    if (parts.length == 2 && parts[0].trim().equals("NSpid")) {
                        parts = parts[1].trim().split("\\s+");
                        return Integer.parseInt(parts[parts.length - 1]);
                    }
                }
                return pid;
            } catch (NumberFormatException x) {
                throw new Exception("Unable to parse namespace");
            } catch (IOException ioe) {
                throw new Exception("Unable to parse namespace");
            } finally {
                try {
                    if (fr != null) {

                        fr.close();
                    }
                    if (br != null) {
                        br.close();
                    }
                } catch (IOException ioe1) {
                    throw new Exception("Unable to close stream when parsing NSpid");
                }
            }
        }

        private static class NewLinuxSocketInputStream extends InputStream {
            private final int sock;

            public NewLinuxSocketInputStream(int s) {
                this.sock = s;
            }

            public synchronized int read() throws IOException {
                return HotSpotVM.readOneByte(this);
            }

            public synchronized int read(byte[] bs, int off, int len) throws IOException {
                if (len == 0) {
                    return 0;
                }
                checkBeforeRead(bs, off, len);
                return VirtualMachineImpl.read(sock, bs, off, len);
            }

            public void close() throws IOException {
                VirtualMachineImpl.close(sock);
            }
        }
    }

    static class NewBsdHotSpotVM extends HotSpotVM {
        private static final String tmpdir;

        static {
            tmpdir = VirtualMachineImpl.getTempDir();
        }

        String socketFile;

        public NewBsdHotSpotVM(int pid) throws Exception {
            File socketFile = new File(tmpdir, ".java_pid" + pid);
            this.socketFile = socketFile.getPath();
            if (!socketFile.exists()) {
                File f = createAttachFile(pid);
                try {
                    VirtualMachineImpl.sendQuitTo(pid);
                    for (int i = 0; i < 5; i++) {
                        Thread.sleep(1000);
                        if (i >= 2 && !socketFile.exists()) {
                            VirtualMachineImpl.sendQuitTo(pid);
                        }
                    }
                    if (!socketFile.exists()) {
                        throw new Exception("Unable to open socket file " + socketFile);
                    }
                } finally {
                    f.delete();
                }
            }

            VirtualMachineImpl.checkPermissions(this.socketFile);
            VirtualMachineImpl.checkConnection(this.socketFile);
        }

        public void detach() throws IOException {
            if (socketFile != null) {
                socketFile = null;
            }
        }

        public void execute(String cmd, Object... args) throws Exception {
            int fd = VirtualMachineImpl.connectToVM(socketFile);
            IOException ioe = HotSpotVM.writeCommand(fd, cmd, args);
            NewBsdSocketInputStream sis = new NewBsdSocketInputStream(fd);
            HotSpotVM.checkExecReturn(ioe, cmd, sis);
            HotSpotVM.readRemaining(sis);
        }

        private File createAttachFile(int pid) {
            File f = new File(tmpdir, ".attach_pid" + pid);
            VirtualMachineImpl.createAttachFile0(f.getPath());
            return f;
        }

        private static class NewBsdSocketInputStream extends InputStream {
            private final int sock;

            public NewBsdSocketInputStream(int s) {
                this.sock = s;
            }

            public synchronized int read() throws IOException {
                return HotSpotVM.readOneByte(this);
            }

            public synchronized int read(byte[] bs, int off, int len) throws IOException {
                if (len == 0) {
                    return 0;
                }
                checkBeforeRead(bs, off, len);
                return VirtualMachineImpl.read(sock, bs, off, len);
            }

            public void close() throws IOException {
                VirtualMachineImpl.close(sock);
            }
        }
    }

    private static class NewWindowsHotSpotVM extends HotSpotVM {
        private static final byte[] stub;

        static {
            VirtualMachineImpl.init();
            stub = VirtualMachineImpl.generateStub();
        }

        private volatile long proc;


        public NewWindowsHotSpotVM(int pid) throws Exception {
            proc = VirtualMachineImpl.openProcess(pid);
            try {
                VirtualMachineImpl.enqueue(proc, stub, null, null);
            } catch (IOException x) {
                throw new Exception(x.getMessage());
            }
        }

        public void detach() throws IOException {
            if (proc != -1) {
                VirtualMachineImpl.closeProcess(proc);
                proc = -1;
            }
        }

        public void execute(String cmd, Object... args) throws Exception {
            Random rnd = new Random();
            int r = rnd.nextInt();
            String pipePrefix = "\\\\.\\pipe\\javatool";
            String pipename = pipePrefix + r;
            long hPipe;
            try {
                hPipe = VirtualMachineImpl.createPipe(pipename);
            } catch (IOException ce) {
                r = rnd.nextInt();
                pipename = pipePrefix + r;
                hPipe = VirtualMachineImpl.createPipe(pipename);
            }
            if (proc == -1) {
                VirtualMachineImpl.closePipe(hPipe);
                throw new IOException("Detached from target VM");
            }

            try {
                VirtualMachineImpl.enqueue(proc, stub, cmd, pipename, args);
                VirtualMachineImpl.connectPipe(hPipe);
                NewWindowsPipedInputStream in = new NewWindowsPipedInputStream(hPipe);
                HotSpotVM.checkExecReturn(null, cmd, in);
                HotSpotVM.readRemaining(in);
            } catch (IOException ioe) {
                VirtualMachineImpl.closePipe(hPipe);
                throw ioe;
            }
        }

        private static class NewWindowsPipedInputStream extends InputStream {
            private long pipe;

            public NewWindowsPipedInputStream(long hPipe) {
                this.pipe = hPipe;
            }

            public synchronized int read() throws IOException {
                return HotSpotVM.readOneByte(this);
            }

            public synchronized int read(byte[] bs, int off, int len) throws IOException {
                if (len == 0) {
                    return 0;
                }
                checkBeforeRead(bs, off, len);
                return VirtualMachineImpl.readPipe(pipe, bs, off, len);
            }

            public void close() throws IOException {
                if (pipe != -1) {
                    VirtualMachineImpl.closePipe(pipe);
                    pipe = -1;
                }
            }
        }
    }
}
