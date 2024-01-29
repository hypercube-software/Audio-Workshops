package com.hypercube.workshop.audioworkshop.files.meta;

import com.hypercube.workshop.audioworkshop.files.riff.RiffAudioInfo;

import java.util.List;

public record Instrument(String name, String path, List<RiffAudioInfo> samples) {
    public String fullPath() {
        return path + "/" + name;
    }
}
