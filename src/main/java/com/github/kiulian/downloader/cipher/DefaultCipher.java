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


import java.util.List;
import java.util.Map;

public class DefaultCipher implements Cipher {

    private final Map<String, CipherFunction> functionsMap;
    private final List<JsFunction> functions;

    public DefaultCipher(List<JsFunction> transformFunctions, Map<String, CipherFunction> transformFunctionsMap) {
        this.functionsMap = transformFunctionsMap;
        this.functions = transformFunctions;
    }

    @Override
    public String getSignature(String cipheredSignature) {
        char[] signature = cipheredSignature.toCharArray();
        for (JsFunction jsFunction : functions) {
            signature = functionsMap.get(jsFunction.getName()).apply(signature, jsFunction.getArgument());
        }
        return String.valueOf(signature);
    }

}
