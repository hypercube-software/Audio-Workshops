package com.hypercube.mpm.app;

public record RequestStatus(String errorMessage, byte[] payload) {
    public static RequestStatus of(String errorMessage) {
        return new RequestStatus(errorMessage, null);
    }

    public static RequestStatus of(byte[] payload) {
        return new RequestStatus(null, payload);
    }
}
