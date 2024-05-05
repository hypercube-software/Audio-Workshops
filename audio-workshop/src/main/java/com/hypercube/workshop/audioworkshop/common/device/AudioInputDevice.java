package com.hypercube.workshop.audioworkshop.common.device;

import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class AudioInputDevice extends AbstractAudioDevice {
    private List<DataLine.Info> inputLines;

    public AudioInputDevice(Mixer.Info mixerInfo, List<DataLine.Info> inputLines) {
        super(mixerInfo);
        this.inputLines = inputLines;
    }

    public void logFormats() {
        log.info("AUDIO INPUT : " + getName());
        inputLines.forEach(dataline -> {
            log.info("\tDATA LINE: " + dataline.toString());
            Arrays.stream(dataline.getFormats())
                    .forEach(format -> log.info("\t\t" + format.toString()));
        });
    }
}
