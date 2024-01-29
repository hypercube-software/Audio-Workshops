package com.hypercube.workshop.audioworkshop.files.meta;

public record Version(int major, int minor, int release, int build) {
    @Override
    public String toString() {
        return "%d.%d.%d.%d".formatted(major, minor, release, build);
    }
}
