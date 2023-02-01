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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public class Main {
    private static final Set<Command> commands = new LinkedHashSet<Command>();

    static {
        registerCommands();
    }

    private static void registerCommands() {
        commands.add(new HeapDumpCommand("heap",
            "Generate heap dump of Java process"));
        commands.add(new ThreadDumpCommand("thread",
            "Print all threads and their stack traces"));
        commands.add(new HeapHistogramCommand("list_heap",
            "List class and number of instance in Java heap"));
    }

    private static void registerJcmdCommands(HotSpotVM vm) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(bos);
        try {
            new JcmdCommand("", "help", "")
                .redirectOutput(ps)
                .executeCommand(vm, null);
            String availableJcmd = bos.toString();
            String[] lines = availableJcmd.split("\n");
            for (int i = 0; i < lines.length; i++) {
                if (lines[i].matches("[a-zA-Z_]+?\\.[a-zA-Z_]+")) {
                    // Read help document of jcmd subcommands
                    bos = new ByteArrayOutputStream();
                    ps = new PrintStream(bos);
                    new JcmdCommand("", "help " + lines[i], "")
                        .redirectOutput(ps)
                        .executeCommand(vm, null);
                    String jcmdHelp = bos.toString();
                    String[] jcmdHelpLines = jcmdHelp.split("\n");
                    String description = "";
                    if (jcmdHelpLines.length > 2) {
                        description = jcmdHelpLines[1];
                    }
                    // Register jcmd command accordingly
                    String cmdName = lines[i];
                    cmdName = cmdName.replace(".", "_");
                    cmdName = cmdName.toLowerCase();
                    commands.add(new JcmdCommand(cmdName, lines[i], description));
                }
            }
        } catch (Exception e) {
            // Failed to execute Jcmd operations
            // Skip...
        } finally {
            try {
                bos.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void usage() {
        System.err.println("Usage:");
        System.err.println(Main.class.getSimpleName() + " <pid> <subcommand> <options>");
        System.err.println();
        System.err.println("Subcommands:");
        Iterator<Command> iter = commands.iterator();
        while (iter.hasNext()) {
            Command cmd = iter.next();
            System.err.println(cmd.toString());
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            usage();
            return;
        }
        try {
            int pid = Integer.parseInt(args[0]);
            HotSpotVM vm = HotSpotVM.creatHotSpotVM(pid);
            registerJcmdCommands(vm);

            if (args.length < 2) {
                usage();
                return;
            }

            String subCommand = args[1];
            Iterator<Command> iter = commands.iterator();
            boolean found = false;
            while (iter.hasNext()) {
                Command cmd = iter.next();
                if (cmd.getName().equals(subCommand)) {
                    found = true;
                    cmd.executeCommand(vm, args);
                    break;
                }
            }
            if (!found) {
                throw new Exception("Unknown subcommand " + subCommand);
            }
            vm.detach();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
