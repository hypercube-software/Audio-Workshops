package com.hypercube.workshop.midiworkshop.api.errors;

import lombok.Getter;
import lombok.experimental.StandardException;

@StandardException
@Getter
public class MidiDeviceTimeout extends MidiError {
    int timeoutInSec;

    public MidiDeviceTimeout(String message, int timeoutInSec) {
        super(message);
        this.timeoutInSec = timeoutInSec;
    }
}
