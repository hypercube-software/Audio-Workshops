package com.hypercube.workshop.audioworkshop.files.exceptions;

public class UnexpectedNullChunk extends RuntimeException {
    public UnexpectedNullChunk(String filename, long position) {
        super("Null chunk at 0x%X, in %s".formatted(position, filename));
    }
}
