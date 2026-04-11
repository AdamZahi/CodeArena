package com.codearena.module2_battle.exception;

public class UnsupportedLanguageException extends RuntimeException {

    public UnsupportedLanguageException(String language) {
        super("Language '" + language + "' is not supported. Supported: java, python, javascript, cpp, go, rust");
    }
}
