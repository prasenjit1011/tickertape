package com.tickertape.tickertape_spring.exception;

public class UpstreamDataException extends RuntimeException {

    public UpstreamDataException(String message) {
        super(message);
    }
}
