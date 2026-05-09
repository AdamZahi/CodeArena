package com.codearena.module4_shop.exception;

public class CartEmptyException extends RuntimeException {
    public CartEmptyException() {
        super("Cart cannot be empty");
    }
}