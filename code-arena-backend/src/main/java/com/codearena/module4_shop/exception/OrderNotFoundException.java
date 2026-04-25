package com.codearena.module4_shop.exception;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(String id) {
        super("Order not found with id: " + id);
    }
}