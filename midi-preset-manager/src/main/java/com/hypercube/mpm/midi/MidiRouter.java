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

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.ShortMessage;
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
    private int outputChannel = 0;

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
            CustomMidiEvent outputEvent = forgeOutputEvent(event);
            log.info("Main " + mainMidiOut.getName());
            mainMidiOut.send(outputEvent);
        }
        synchronized (currentMidiOutsGuardian) {
            for (MidiOutDevice midiOut : currentMidiOuts) {
                log.info("Passthru " + midiOut.getName());
                midiOut.send(event);
            }
        }
    }

    public void changeOutputChannel(Integer channel) {
        log.info("New output channel in MIDI router: " + channel);
        outputChannel = channel;
    }

    private CustomMidiEvent forgeOutputEvent(CustomMidiEvent event) {
        MidiMessage msg = event.getMessage();
        byte[] payload = msg
                .getMessage();
        int command = payload[0] & 0xF0;
        int data1 = payload[1];
        int data2 = payload[2];
        try {
            return new CustomMidiEvent(new ShortMessage(command, outputChannel, data1, data2));
        } catch (InvalidMidiDataException e) {
            throw new ApplicationError(e);
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
