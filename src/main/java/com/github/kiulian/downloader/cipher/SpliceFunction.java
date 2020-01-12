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


class SpliceFunction implements CipherFunction {

    @Override
    public char[] apply(char[] array, String argument) {
        int deleteCount = Integer.parseInt(argument);
        char[] spliced = new char[array.length - deleteCount];
        System.arraycopy(array, 0, spliced, 0, deleteCount);
        System.arraycopy(array, deleteCount * 2, spliced, deleteCount, spliced.length - deleteCount);

        return spliced;
    }

}
