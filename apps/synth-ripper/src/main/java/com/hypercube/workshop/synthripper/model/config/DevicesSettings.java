package com.hypercube.workshop.synthripper.model.config;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class DevicesSettings {
    private String inputAudioDevice;
    private String outputAudioDevice;
    private String outputMidiDevice;

}
