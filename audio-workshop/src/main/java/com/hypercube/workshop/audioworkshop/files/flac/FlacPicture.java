package com.hypercube.workshop.audioworkshop.files.flac;

public record FlacPicture(FlacPictureType type, String mime, String description, int width, int height, byte[] data) {
}
