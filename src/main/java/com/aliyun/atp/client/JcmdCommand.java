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

import sun.tools.attach.HotSpotVM;

import java.util.Arrays;

class JcmdCommand extends Command {
    private static final String VM_OPERATION_JCMD = "jcmd";
    private final String jcmdCommand;

    JcmdCommand(String commandName, String jcmdCommand, String description) {
        super(commandName, description, new CommandOption[]{});
        this.jcmdCommand = jcmdCommand;
    }

    @Override
    protected void execute(HotSpotVM vm, String[] args) throws Exception {
        String[] p;
        p = new String[1 + (args != null ? args.length : 0)];
        p[0] = jcmdCommand;
        if (args != null) {
            System.arraycopy(args, 0, p, 1, args.length);
        }
        vm.execute(VM_OPERATION_JCMD, p);
    }
}
