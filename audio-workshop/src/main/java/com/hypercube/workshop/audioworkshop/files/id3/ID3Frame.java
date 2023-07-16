package com.hypercube.workshop.audioworkshop.files.id3;

public record ID3Frame(String id, int flag1, int flag2, byte[] data, String strContent) {
}
