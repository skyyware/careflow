package com.skyyware.careflow;

public class CareCaseNotFoundException extends RuntimeException {
    public CareCaseNotFoundException(String id) {
        super("Care case not found: " + id);
    }
}
