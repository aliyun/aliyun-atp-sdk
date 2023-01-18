/**
 * MIT License
 * Copyright (c) 2022 Alibaba Cloud
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
public class ATPClient {
    private static final String ATP_CLIENT_TOOL_JAR = "atp-client-tool.jar";
    private static final Random RAND = new Random();

    public static void execute(String[] args) throws Exception {
        File file = new File(".T" + RAND.nextInt() + ATP_CLIENT_TOOL_JAR);

        if (file.exists()) {
            throw new Exception("Can not create client due to its existence");
        }

        InputStream link = ATPClient.class.getClassLoader().getResourceAsStream(ATP_CLIENT_TOOL_JAR);
        if (link == null) {
            throw new Exception("Can not create client due to missing dependencies");
        }

        Files.copy(link, file.getAbsoluteFile().toPath());

        ArrayList<String> cmdArgs = new ArrayList<>();
        cmdArgs.add("java");
        cmdArgs.add("-Xbootclasspath/a:" + file.getAbsolutePath());
        cmdArgs.add("-jar");
        cmdArgs.add(file.getAbsolutePath());
        if (args != null) {
            cmdArgs.addAll(Arrays.asList(args));
        }
        ProcessBuilder ps = new ProcessBuilder(cmdArgs);
        ps.redirectErrorStream(true);
        try {
            Process pr = ps.start();
            try (InputStreamReader ir = new InputStreamReader(pr.getInputStream()); BufferedReader br = new BufferedReader(ir)) {
                String line;
                while ((line = br.readLine()) != null) {
                    System.out.println(line);
                }
                pr.waitFor(10, TimeUnit.MINUTES);
            }
        } finally {
            file.delete();
        }
    }
}
