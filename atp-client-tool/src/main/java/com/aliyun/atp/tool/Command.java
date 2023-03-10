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

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;

abstract class Command {
    private final String name;
    private final String description;
    private final CommandOption[] options;
    private PrintStream newOut;


    public Command() {
        this.name = "";
        this.description = "";
        this.options = null;
    }

    public Command(String name) {
        this.name = name;
        this.description = "";
        this.options = null;
    }

    public Command(String name, String description, CommandOption[] options) {
        this.name = name;
        this.description = description;
        this.options = options;
    }

    public String getName() {
        return name;
    }

    public CommandOption getOption(String name) {
        for (int i = 0; i < options.length; i++) {
            if (options[i].getName().equals(name)) {
                return options[i];
            }
        }
        return null;
    }

    protected void parseInputArguments(String[] args) throws Exception {
        for (int i = 0; options != null && i < options.length; i++) {
            CommandOption option = options[i];
            if (option.isMandatory()) {
                boolean found = false;
                for (int k = 0; k < args.length; k++) {
                    if (args[k].startsWith(option.getName())) {
                        found = true;
                        option.setValue(args[k]);
                        break;
                    }
                }
                if (!found) {
                    throw new Exception("Option " + option.getName() + " is mandatory");
                }
            } else {
                for (int k = 0; k < args.length; k++) {
                    if (args[i].startsWith(option.getName())) {
                        option.setValue(args[i]);
                        break;
                    }
                }
            }
        }
    }

    public Command redirectOutput(PrintStream os) {
        newOut = os;
        return this;
    }

    public final void executeCommand(HotSpotVM vm, String[] args) throws Exception {
        parseInputArguments(args);
        PrintStream oldOut = null;
        if (newOut != null) {
            oldOut = System.out;
            System.setOut(newOut);
        }
        try {
            execute(vm, args);
        } finally {
            if (oldOut != null) {
                System.setOut(oldOut);
            }
        }
    }

    protected abstract void execute(HotSpotVM vm, String[] args) throws Exception;

    @Override
    public String toString() {
        String str = String.format("%-30s%s", name, description != null ? description : "");
        if (options != null && options.length > 0) {
            str += "\n                              " + Arrays.toString(options);
        }
        return str;
    }
}
