package com.hypercube.workshop.syntheditor.model;

import com.hypercube.workshop.midiworkshop.common.MidiDeviceManager;
import com.hypercube.workshop.midiworkshop.common.MidiInDevice;
import com.hypercube.workshop.midiworkshop.common.MidiOutDevice;
import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import com.hypercube.workshop.syntheditor.model.error.SynthEditorException;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Setter
@Getter
public class SynthEditorService {
    private final MidiDeviceManager midiDeviceManager = new MidiDeviceManager();
    private MidiInDevice inputDevice;
    private MidiOutDevice outputDevice;

    @PostConstruct
    public void init() {
        midiDeviceManager.collectDevices();
    }

    public void closeCurrentInputDevice() {
        if (inputDevice != null) {
            try {
                inputDevice.close();
                inputDevice = null;
            } catch (IOException e) {
                throw new SynthEditorException(e);
            }
        }
    }

    public void closeCurrentOutputDevice() {
        if (outputDevice != null) {
            try {
                outputDevice.close();
                outputDevice = null;
            } catch (IOException e) {
                throw new SynthEditorException(e);
            }
        }
    }

    public void changeInput(String deviceName) {
        closeCurrentInputDevice();
        inputDevice = midiDeviceManager.getInput(deviceName)
                .orElseThrow(() -> new MidiError("Device not found:" + deviceName));
        inputDevice.open();
    }

    public void changeOutput(String deviceName) {
        closeCurrentOutputDevice();
        outputDevice = midiDeviceManager.getOutput(deviceName)
                .orElseThrow(() -> new MidiError("Device not found:" + deviceName));
        outputDevice.open();
    }
}
