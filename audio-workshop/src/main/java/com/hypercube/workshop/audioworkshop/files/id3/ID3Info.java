package com.hypercube.workshop.audioworkshop.files.id3;

import com.hypercube.workshop.audioworkshop.files.meta.AudioMetadata;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class ID3Info {
    private final String version;
    private final Map<String, ID3Frame> frames = new HashMap<>();
    private final AudioMetadata metadata = new AudioMetadata();

    public ID3Info(String version) {
        this.version = version;
    }
}
