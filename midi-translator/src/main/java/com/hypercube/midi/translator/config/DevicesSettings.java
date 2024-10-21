package com.hypercube.midi.translator.config;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class DevicesSettings {
    private String inputMidiDevice;
    private String outputMidiDevice;
    private int outputBandwidth;
}
