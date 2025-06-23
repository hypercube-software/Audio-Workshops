package com.hypercube.mpm.midi;

import com.hypercube.mpm.config.ProjectConfiguration;
import com.hypercube.mpm.javafx.error.ApplicationError;
import com.hypercube.workshop.midiworkshop.common.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.common.MidiInDevice;
import com.hypercube.workshop.midiworkshop.common.MidiOutDevice;
import com.hypercube.workshop.midiworkshop.common.listener.MidiListener;
import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiDeviceDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sound.midi.MidiUnavailableException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class MidiRouter implements MidiListener {
    @Autowired
    ProjectConfiguration cfg;

    private MidiInDevice currentMidiIn;
    private Object currentMidiOutsGuardian = new Object();
    private List<MidiOutDevice> currentMidiOuts = new ArrayList<>();
    private MidiOutDevice mainMidiOut;

    public void changeSource(String deviceOrPortName) {
        var portName = cfg.getMidiDeviceLibrary()
                .getDevice(deviceOrPortName)
                .map(MidiDeviceDefinition::getInputMidiDevice)
                .orElse(deviceOrPortName);

        closeListener();
        currentMidiIn = cfg.getMidiDeviceManager()
                .getInput(portName)
                .orElse(null);
        if (currentMidiIn != null) {
            if (!currentMidiIn.isOpen()) {
                currentMidiIn.open();
            }
            installListener();
        }
    }

    public void changeMainDestination(MidiDeviceDefinition device) {
        if (device != null) {
            mainMidiOut = cfg.getMidiDeviceManager()
                    .getOutput(device.getOutputMidiDevice())
                    .orElse(null);
            if (mainMidiOut != null) {
                mainMidiOut.open();
            }
        }
    }

    public void changeDestinations(List<String> deviceOrPortNames) {
        currentMidiOuts = deviceOrPortNames.stream()
                .map(deviceOrPortName -> cfg.getMidiDeviceLibrary()
                        .getDevice(deviceOrPortName)
                        .map(MidiDeviceDefinition::getInputMidiDevice)
                        .orElse(deviceOrPortName))
                .map(cfg.getMidiDeviceManager()::getOutput)
                .flatMap(Optional::stream)
                .toList();
        openMidiOut();
    }

    @Override
    public void onEvent(MidiInDevice device, CustomMidiEvent event) {
        log.info("Receive " + event.getHexValuesSpaced());
        if (mainMidiOut != null) {
            log.info("Main " + mainMidiOut.getName());
            mainMidiOut.send(event);
        }
        synchronized (currentMidiOutsGuardian) {
            for (MidiOutDevice midiOut : currentMidiOuts) {
                log.info("Passthru " + midiOut.getName());
                midiOut.send(event);
            }
        }
    }

    private void openMidiOut() {
        synchronized (currentMidiOutsGuardian) {
            for (MidiOutDevice midiOut : currentMidiOuts) {
                if (midiOut != null) {
                    if (!midiOut.isOpen()) {
                        midiOut.open();
                    }
                }
            }
        }
    }

    private void closeListener() {
        if (currentMidiIn != null) {
            currentMidiIn.stopListening();
            currentMidiIn.removeListener(this);
        }
    }

    private void installListener() {
        if (currentMidiIn != null && currentMidiOuts != null) {
            currentMidiIn.addListener(this);
            try {
                currentMidiIn.startListening();
            } catch (MidiUnavailableException e) {
                throw new ApplicationError(e);
            }
        }
    }

}
