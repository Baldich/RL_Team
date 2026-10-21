package com.tecnocampus.LS2.protube_back.services;

public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException() {
        super("This email is already registered.");
    }
}
