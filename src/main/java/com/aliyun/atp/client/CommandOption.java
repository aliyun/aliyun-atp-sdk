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

import java.util.Arrays;

class CommandOption {
    private final String name;
    private final String[] valueSet;
    private final boolean mandatory;
    private String value;

    CommandOption(String name, String value, boolean mandatory, String[] valueSet) {
        this.name = name;
        this.value = value;
        this.mandatory = mandatory;
        this.valueSet = valueSet;
    }

    private static String extractValue(String arg) {
        // e.g. -opt=value => "value"
        //      -opt       => ""
        if (arg.contains("=")) {
            return arg.split("=")[1];
        } else {
            return "";
        }
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) throws Exception {
        String userValue = extractValue(value);
        if (valueSet != null && valueSet.length > 0) {
            boolean match = false;
            for (int i = 0; i < valueSet.length; i++) {
                if (valueSet[i].equals(userValue)) {
                    match = true;
                    break;
                }
            }
            if (!match) {
                throw new Exception("Option " + name + " accepts " + Arrays.toString(valueSet) + " but got " + userValue);
            }
        }
        this.value = userValue;
    }

    public String getName() {
        return name;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    @Override
    public String toString() {
        String opt = name;
        if (valueSet != null && valueSet.length > 0) {
            opt += "=" + Arrays.toString(valueSet);
        }
        opt += "(";
        opt += mandatory ? "mandatory" : "optional";
        opt += ")";
        return opt;
    }
}
