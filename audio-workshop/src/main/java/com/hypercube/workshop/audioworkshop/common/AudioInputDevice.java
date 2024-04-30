package com.hypercube.workshop.audioworkshop.common;

import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class AudioInputDevice extends AbstractAudioDevice {
    private List<DataLine.Info> datalines;

    public AudioInputDevice(Mixer.Info mixerInfo, List<DataLine.Info> dataLines) {
        super(mixerInfo);
        this.datalines = dataLines;
    }

    public void logFormats() {
        log.info("INPUT: " + getName());
        datalines.forEach(dataline -> {
            log.info("\t" + dataline.toString());
            Arrays.stream(dataline.getFormats())
                    .forEach(format -> log.info("\t\t" + format.toString()));
        });
    }
}
