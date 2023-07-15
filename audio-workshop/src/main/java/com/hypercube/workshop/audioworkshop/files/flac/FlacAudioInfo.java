package com.hypercube.workshop.audioworkshop.files.flac;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class FlacAudioInfo {
    int sampleRate;

    int numChannels;

    int bitDepth;

    int duration;

    String durationString = "";

    String comment = "";

    byte[] md5 = new byte[0];

    List<FlacPicture> pictures = new ArrayList<>();
}
