/**
 * MIT License
 * Copyright (c) 2022, 2023 Alibaba Cloud
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.aliyun.atp.tool;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
public class ATPClient {
    private static final String ATP_CLIENT_TOOL_JAR = "atp-client-tool.jar";
    private static final Random RAND = new Random();

    private static String findJavaCommand() {
        String javaHome = System.getProperty("java.home");
        if (javaHome != null && !javaHome.isEmpty()) {
            String jdkTool = javaHome + File.separator + "bin" + File.separator;
            if (isWindows()) {
                jdkTool += "java.exe";
            } else {
                jdkTool += "java";
            }
            return jdkTool;
        }
        return isWindows() ? "java.exe" : "java";
    }

    private static boolean isWindows() {
        String os = System.getProperty("os.name").toLowerCase();
        return os != null && os.contains("win");
    }

    private static boolean isJavaCommandExist(String javaCmd) {
        try {
            Process process = Runtime.getRuntime().exec(javaCmd + " -version");
            int exitValue = process.waitFor();
            return exitValue == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    private static File extractClientTool() throws ClientException {
        File file;
        try {
            file = File.createTempFile(".ATP", ATP_CLIENT_TOOL_JAR);
        } catch (IOException e) {
            throw new ClientException("Failed to create temporary file: " + e.getMessage());
        }

        ClassLoader loader = ATPClient.class.getClassLoader();
        if (loader == null) {
            throw new ClientException("Can not find client tool: it should not be loaded by bootclassloader");
        }

        InputStream link = loader.getResourceAsStream(ATP_CLIENT_TOOL_JAR);
        if (link == null) {
            throw new ClientException("Can not find client tool");
        }

        try {
            Files.copy(link, file.getAbsoluteFile().toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            // fallback to create temporary file in working directory
            file = new File(".T" + RAND.nextInt() + ATP_CLIENT_TOOL_JAR);
            try {
                Files.copy(link, file.getAbsoluteFile().toPath(), StandardCopyOption.REPLACE_EXISTING);
                return file;
            } catch (IOException ex) {
            }
            throw new ClientException("Failed to extract client tool from jar: " + e.getMessage());
        }

        return file;
    }

    public static Process startProcess(String javaCommand, String clientToolPath, String[] args) throws IOException {
        // launch client tool jar as standalone application
        ArrayList<String> cmdArgs = new ArrayList<>();
        cmdArgs.add(javaCommand);
        cmdArgs.add("-Xbootclasspath/a:" + clientToolPath);
        cmdArgs.add("-jar");
        cmdArgs.add(clientToolPath);
        if (args != null) {
            cmdArgs.addAll(Arrays.asList(args));
        }
        ProcessBuilder ps = new ProcessBuilder(cmdArgs);
        ps.redirectErrorStream(true);
        return ps.start();
    }

    public static void execute(String[] args) throws ClientException {
        File clientTool = extractClientTool();

        // before launching client tool, we need to check if "java" exists
        String javaCommand = findJavaCommand();
        if (!isJavaCommandExist(javaCommand)) {
            throw new ClientException("Failed to execute client tool: can not find java command");
        }

        try {
            Process pr = startProcess(javaCommand, clientTool.getAbsolutePath(), args);
            try (InputStreamReader ir = new InputStreamReader(pr.getInputStream());
                 BufferedReader br = new BufferedReader(ir)) {
                String line;
                while ((line = br.readLine()) != null) {
                    System.out.println(line);
                }
                pr.waitFor(1, TimeUnit.HOURS);
            }
        } catch (IOException | InterruptedException ex) {
            throw new ClientException("Failed to execute client tool: " + ex.getMessage());
        } finally {
            clientTool.delete();
        }
    }
}
