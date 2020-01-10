package com.github.kiulian.downloader.cipher;

/*-
 * #
 * Java youtube video and audio downloader
 *
 * Copyright (C) 2019 Igor Kiulian
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

import com.github.kiulian.downloader.YoutubeException;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Cipher {

    private static Pattern[] INITIAL_FUNCTION_PATTERNS = new Pattern[]{
            Pattern.compile("\\b[cs]\\s*&&\\s*[adf]\\.set\\([^,]+\\s*,\\s*encodeURIComponent\\s*\\(\\s*([a-zA-Z0-9$]+)\\("),
            Pattern.compile("\\b[a-zA-Z0-9]+\\s*&&\\s*[a-zA-Z0-9]+\\.set\\([^,]+\\s*,\\s*encodeURIComponent\\s*\\(\\s*([a-zA-Z0-9$]+)\\("),
            Pattern.compile("([a-zA-Z0-9$]+)\\s*=\\s*function\\(\\s*a\\s*\\)\\s*\\{\\s*a\\s*=\\s*a\\.split\\(\\s*\"\"\\s*\\)"),
            Pattern.compile("([\"'])signature\\1\\s*,\\s*([a-zA-Z0-9$]+)\\("),
            Pattern.compile("\\.sig\\|\\|([a-zA-Z0-9$]+)\\("),
            Pattern.compile("yt\\.akamaized\\.net/\\)\\s*\\|\\|\\s*.*?\\s*[cs]\\s*&&\\s*[adf]\\.set\\([^,]+\\s*,\\s*(?:encodeURIComponent\\s*\\()?\\s*()$"),
            Pattern.compile("\\b[cs]\\s*&&\\s*[adf]\\.set\\([^,]+\\s*,\\s*([a-zA-Z0-9$]+)\\("),
            Pattern.compile("\\b[a-zA-Z0-9]+\\s*&&\\s*[a-zA-Z0-9]+\\.set\\([^,]+\\s*,\\s*([a-zA-Z0-9$]+)\\("),
            Pattern.compile("\\bc\\s*&&\\s*a\\.set\\([^,]+\\s*,\\s*\\([^)]*\\)\\s*\\(\\s*([a-zA-Z0-9$]+)\\("),
            Pattern.compile("\\bc\\s*&&\\s*[a-zA-Z0-9]+\\.set\\([^,]+\\s*,\\s*\\([^)]*\\)\\s*\\(\\s*([a-zA-Z0-9$]+)\\("),
            Pattern.compile("\\bc\\s*&&\\s*[a-zA-Z0-9]+\\.set\\([^,]+\\s*,\\s*\\([^)]*\\)\\s*\\(\\s*([a-zA-Z0-9$]+)\\(")
    };

    private static Map<Pattern, CipherFunction> functionsMap = new HashMap<Pattern, CipherFunction>() {
        {
            put(Pattern.compile("\\{\\w\\.reverse\\(\\)}"), new ReverseFunction());
            put(Pattern.compile("\\{\\w\\.splice\\(0,\\w\\)}"), new SpliceFunction());
            put(Pattern.compile("\\{var\\s\\w=\\w\\[0];\\w\\[0]=\\w\\[\\w%\\w.length];\\w\\[\\w]=\\w}"), new SwapFunctionV1());
            put(Pattern.compile("\\{var\\s\\w=\\w\\[0];\\w\\[0]=\\w\\[\\w%\\w.length];\\w\\[\\w%\\w.length]=\\w}"), new SwapFunctionV2());
        }
    };

    private static Pattern JS_FUNCTION_PATTERN = Pattern.compile("\\w+\\.(\\w+)\\(\\w,(\\d+)\\)");

    public static String getSignature(String js, String cipheredSignature) throws YoutubeException.CipherException {
        String[] transformFunctions = getTransformFunctions(js);
        String var = transformFunctions[0].split("\\.")[0];

        Map<String, CipherFunction> transformMap = getTransformMap(var, js);

        char[] signature = cipheredSignature.toCharArray();

        for (String jsFunction : transformFunctions) {
            String[] parsedFunction = parseFunction(jsFunction);
            String name = parsedFunction[0];
            String argument = parsedFunction[1];

            try {
                signature = transformMap.get(name).apply(signature, argument);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return String.valueOf(signature);
    }

    private static String[] getTransformFunctions(String js) throws YoutubeException.CipherException {
        String name = getInitialFunctionName(js).replaceAll("[^A-Za-z0-9_]", "");

        Pattern pattern = Pattern.compile(name + "=function\\(\\w\\)\\{[a-z=\\.\\(\\\"\\)]*;(.*);(?:.+)}");

        Matcher matcher = pattern.matcher(js);
        if (matcher.find()) {
            return matcher.group(1).split(";");
        }

        throw new YoutubeException.CipherException("Transformation functions not found");
    }

    private static String getInitialFunctionName(String js) throws YoutubeException.CipherException {
        for (Pattern pattern : INITIAL_FUNCTION_PATTERNS) {
            Matcher matcher = pattern.matcher(js);
            if (matcher.find()) {
                String group = matcher.group(1);
                return group;
            }
        }

        throw new YoutubeException.CipherException("Initial function name not found");
    }

    private static Map<String, CipherFunction> getTransformMap(String var, String js) throws YoutubeException.CipherException {
        String[] transformObject = getTransformObject(var, js);
        Map<String, CipherFunction> mapper = new HashMap<>();
        for (String obj : transformObject) {
            String[] split = obj.split(":", 2);
            String name = split[0];
            String jsFunction = split[1];

            CipherFunction function = mapFunction(jsFunction);
            mapper.put(name, function);
        }
        return mapper;
    }

    private static String[] getTransformObject(String var, String js) throws YoutubeException.CipherException {
        var = var.replaceAll("[^A-Za-z0-9_]", "");
        Pattern pattern = Pattern.compile(String.format("var %s=\\{(.*?)};", var), Pattern.DOTALL);
        Matcher matcher = pattern.matcher(js);
        if (matcher.find()) {
            return matcher.group(1).replaceAll("\n", " ").split(", ");
        }

        throw new YoutubeException.CipherException("Transofrm object not found");
    }


    private static CipherFunction mapFunction(String jsFunction) throws YoutubeException.CipherException {
        for (Map.Entry<Pattern, CipherFunction> entry : functionsMap.entrySet()) {
            Matcher matcher = entry.getKey().matcher(jsFunction);
            if (matcher.find()) {
                return entry.getValue();
            }
        }

        throw new YoutubeException.CipherException("Map function not found");
    }

    private static String[] parseFunction(String jsFunction) throws YoutubeException.CipherException {
        Matcher matcher = JS_FUNCTION_PATTERN.matcher(jsFunction);

        String[] nameAndArgument = new String[2];
        if (matcher.find()) {
            nameAndArgument[0] = matcher.group(1);
            nameAndArgument[1] = matcher.group(2);
            return nameAndArgument;
        }

        throw new YoutubeException.CipherException("Could not parse js function");
    }

}
