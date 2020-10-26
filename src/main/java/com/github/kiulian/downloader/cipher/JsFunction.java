package com.github.kiulian.downloader.cipher;


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
