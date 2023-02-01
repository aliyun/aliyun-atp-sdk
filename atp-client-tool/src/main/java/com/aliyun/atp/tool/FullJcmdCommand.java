package com.aliyun.atp.tool;

import java.io.*;
import java.util.Arrays;

public class FullJcmdCommand extends Command {
    private static final String[] VALID = {
        "Compiler.CodeHeap_Analytics",
        "Compiler.codecache",
        "Compiler.codelist",
        "Compiler.directives_print",
        "Compiler.queue",
        "GC.class_histogram",
        "GC.class_stats",
        "GC.finalizer_info",
        "GC.heap_info",
        "JVMTI.data_dump",
        "Thread.print",
        "VM.class_hierarchy",
        "VM.classloader_stats",
        "VM.classloaders",
        "VM.command_line",
        "VM.dynlibs",
        "VM.flags",
        "VM.info",
        "VM.log",
        "VM.metaspace",
        "VM.native_memory",
        "VM.print_touched_methods",
        "VM.stringtable",
        "VM.symboltable",
        "VM.system_properties",
        "VM.systemdictionary",
        "VM.uptime",
        "VM.version",
    };

    FullJcmdCommand(String name, String description) {
        super(name, description, new CommandOption[]{
            new CommandOption("-file", "jcmd.log", false, null),
        });
    }

    @Override
    protected void execute(HotSpotVM vm, String[] args) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        FileOutputStream fos = null;
        try {
            new JcmdCommand("", "help", "")
                .redirectOutput(new PrintStream(bos))
                .executeCommand(vm, null);
            fos = new FileOutputStream(getOption("-file").getValue());
            String availableJcmd = bos.toString();
            String[] lines = availableJcmd.split("\n");
            for (int i = 0; i < lines.length; i++) {
                if (lines[i].matches("[a-zA-Z_]+?\\.[a-zA-Z_]+")) {
                    for (String valid : VALID) {
                        if (lines[i].contains(valid)) {
                            String head = "#LN_START#" + lines[i] + "#LN_END#\n";
                            fos.write(head.getBytes());
                            // Read help document of jcmd subcommands
                            new JcmdCommand("", lines[i], "")
                                .redirectOutput(new PrintStream(fos))
                                .executeCommand(vm, null);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Failed to execute Jcmd operations
            // Skip...
        } finally {
            try {
                bos.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (fos != null) {
                fos.close();
            }
        }
    }
}
