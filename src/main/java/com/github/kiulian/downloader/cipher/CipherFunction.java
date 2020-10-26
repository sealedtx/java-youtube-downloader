package com.github.kiulian.downloader.cipher;


public interface CipherFunction {

    char[] apply(char[] array, String argument);
}
