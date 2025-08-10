package com.hypercube.workshop.midiworkshop.api.errors;

import lombok.Getter;
import lombok.experimental.StandardException;

import javax.sound.midi.MidiDevice;

@StandardException
@Getter
public class MidiError extends RuntimeException {
    private MidiDevice device;
    private String deviceName;

    public MidiError(MidiDevice device, Throwable cause) {
        super(cause);
        this.device = device;
        setDeviceName(device);
    }

    public MidiError(MidiDevice device, String msg) {
        super(msg);
        this.device = device;
        setDeviceName(device);
    }

    private void setDeviceName(MidiDevice device) {
        try {
            deviceName = device.getDeviceInfo()
                    .getName();
        } catch (Throwable e) {
            deviceName = null;
        }
    }


}
