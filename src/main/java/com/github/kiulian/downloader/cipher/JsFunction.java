package com.github.kiulian.downloader.cipher;

/*-
 * #
 * Java youtube video and audio downloader
 *
 * Copyright (C) 2020 Igor Kiulian
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #
 */

public class JsFunction {
    private final String var;
    private final String name;
    private final String argument;

    public JsFunction(String var, String name, String argument) {
        this.var = var;
        this.name = name;
        this.argument = argument;
    }

    public String getVar() {
        return var;
    }

    public String getName() {
        return name;
    }

    public String getArgument() {
        return argument;
    }
}
