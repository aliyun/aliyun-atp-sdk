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
package com.aliyun.atp.client;

public class ThreadDumpCommand extends Command {
    private static final String VM_OPERATION_THREAD_DUMP = "threaddump";

    public ThreadDumpCommand(String commandName, String description) {
        super(commandName, description, new CommandOption[]{
            new CommandOption("-lock", "", false, null),
            new CommandOption("-extend", "", false, null),
        });
    }

    @Override
    protected void execute(HotSpotVM vm, String[] args) throws Exception {
        boolean printLock = getOption("-lock") != null;
        boolean printExtend = getOption("-extend") != null;
        String dumpOption = "";
        if (printLock) {
            dumpOption += "-l ";
        }
        if (printExtend) {
            dumpOption += "-e ";
        }

        vm.execute(VM_OPERATION_THREAD_DUMP, dumpOption.trim());
    }
}
