package com.hypercube.mpm.midi;

import com.hypercube.mpm.config.ProjectConfiguration;
import com.hypercube.mpm.javafx.error.ApplicationError;
import com.hypercube.workshop.midiworkshop.common.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.common.MidiInDevice;
import com.hypercube.workshop.midiworkshop.common.MidiOutDevice;
import com.hypercube.workshop.midiworkshop.common.listener.MidiListener;
import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiDeviceDefinition;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sound.midi.MidiUnavailableException;
import java.io.IOException;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class MidiRouter {
    private final ProjectConfiguration cfg;
    private final Map<String, MidiTransformer> midiTransformers = new HashMap<>();
    @Setter
    private MidiTransformerListener listener;
    private final Object currentMidiOutsGuardian = new Object();
    private List<MidiOutDevice> currentMidiOuts = new ArrayList<>();
    private final List<MidiInDevice> dawMidiIns = new ArrayList<>();
    private final MidiListener mainDestinationEventListener = this::onMainDestinationEvent;
    private final MidiListener mainSourceEventListener = this::onMainSourceEvent;
    private MidiDeviceDefinition mainSource;
    private MidiDeviceDefinition mainDestination;
    private MidiInDevice mainSourceMidiIn;
    private MidiInDevice mainDestinationMidiIn;
    private MidiOutDevice mainDestinationMidiOut;
    private int outputChannel = 0;

    public void listenDawOutputs() {
        log.info("------------------------------------------------------------------------");
        log.info("Auto-routing DAW outpus to devices:");
        cfg.getMidiDeviceLibrary()
                .getDevices()
                .values()
                .stream()
                .filter(d -> d.getDawMidiDevice() != null && d.getOutputMidiDevice() != null)
                .forEach(d -> {
                    MidiInDevice dawMidiInDevice = cfg.getMidiDeviceManager()
                            .getInput(d.getDawMidiDevice())
                            .orElse(null);
                    MidiOutDevice midiOutDevice = cfg.getMidiDeviceManager()
                            .getOutput(d.getOutputMidiDevice())
                            .orElse(null);
                    if (dawMidiInDevice != null && midiOutDevice != null) {
                        var dawDevice = cfg.getMidiDeviceLibrary()
                                .getDevice("DAW")
                                .orElse(null);
                        log.info("{} will go to {}", d.getDawMidiDevice(), d.getOutputMidiDevice());
                        dawMidiInDevice.open();
                        dawMidiInDevice.addListener((device, event) -> onDAWEvent(device, event, midiOutDevice));
                        dumpListeners("dawMidiInDevice", dawMidiInDevice);
                        dawMidiIns.add(dawMidiInDevice);
                        try {
                            midiTransformers.put(dawMidiInDevice.getName(), new MidiTransformer(dawDevice, d, listener));
                            if (dawMidiInDevice.isListening()) {
                                log.info("dawMidiInDevice Already listening {}", dawMidiInDevice.getName());
                            }
                            dawMidiInDevice.startListening();
                        } catch (MidiUnavailableException e) {
                            throw new ApplicationError(e);
                        }
                    }
                });
        log.info("------------------------------------------------------------------------");
    }

    public void changeMainSource(String deviceOrPortName) {
        mainSource = cfg.getMidiDeviceLibrary()
                .getDevice(deviceOrPortName)
                .orElse(null);

        var portName = cfg.getMidiDeviceLibrary()
                .getDevice(deviceOrPortName)
                .map(MidiDeviceDefinition::getInputMidiDevice)
                .orElse(deviceOrPortName);

        closeListener();
        mainSourceMidiIn = cfg.getMidiDeviceManager()
                .getInput(portName)
                .orElse(null);
        if (mainSourceMidiIn != null) {
            mainSourceMidiIn.open();
            installMainSourceListenerWithTransformer(mainSource, mainDestination);
        }
    }

    public void changeMainDestination(MidiDeviceDefinition device) {
        removeMainDestinationListener();
        if (device != null) {
            mainDestination = device;
            mainDestinationMidiOut = cfg.getMidiDeviceManager()
                    .getOutput(device.getOutputMidiDevice())
                    .orElse(null);
            mainDestinationMidiIn = cfg.getMidiDeviceManager()
                    .getInput(device.getInputMidiDevice())
                    .orElse(null);
            if (mainDestinationMidiOut != null) {
                mainDestinationMidiOut.open();
                if (mainSource != null) {
                    installMainSourceListenerWithTransformer(mainSource, mainDestination);
                }
            } else {
                log.warn("Device {} has no output MIDI port", device.getDeviceName());
            }
            if (mainDestinationMidiIn != null) {
                mainDestinationMidiIn.open();
                addMainDestinationListener(device);
            } else {
                log.warn("Device {} has no input MIDI port", device.getDeviceName());
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

    private void onMainDestinationEvent(MidiInDevice midiInDevice, CustomMidiEvent event) {
        if (event.getMessage()
                .getStatus() <= 0xF0) {
            log.info("onMainDestinationEvent: Receive from {}: {}", mainDestination.getDeviceName(), event.toString());
        }
    }

    public void onMainSourceEvent(MidiInDevice device, CustomMidiEvent event) {
        if (event.getMessage()
                .getStatus() <= 0xF0) {
            log.info("onMainSourceEvent: Receive from {}: {}", device.getName(), event.toString());
        }
        List<CustomMidiEvent> transformed = midiTransformers.get(device.getName())
                .transform(outputChannel, event);
        if (mainDestinationMidiOut != null) {
            if (mainDestinationMidiOut.getName()
                    .equals(device.getName())) {
                log.info("\tSend 'As Is' to '{}': {}", mainDestinationMidiOut.getName(), event.toString());
                mainDestinationMidiOut.send(event);
            } else {
                for (CustomMidiEvent outputEvent : transformed) {
                    if (outputEvent.getMessage()
                            .getStatus() <= 0xF0) {
                        log.info("\tSend Transformed to '{}': {}", mainDestinationMidiOut.getName(), outputEvent.toString());
                    }
                    mainDestinationMidiOut.send(outputEvent);
                }
            }
        }
        synchronized (currentMidiOutsGuardian) {
            for (MidiOutDevice midiOut : currentMidiOuts) {
                if (midiOut.getName()
                        .equals(device.getName())) {
                    log.info("'As Is' passthru " + midiOut.getName());
                    midiOut.send(event);
                } else {
                    for (var evt : transformed) {
                        log.info("Transformed passthru {}: {}", midiOut.getName(), evt.toString());
                        midiOut.send(evt);
                    }
                }
            }
        }
    }

    public void terminate() {
        log.info("Shutdown Midi Router...");
        try {
            if (mainDestinationMidiOut != null) {
                log.info("Close {}", mainDestinationMidiOut.getName());
                mainDestinationMidiOut.close();
            }
            for (MidiOutDevice currentMidiOut : currentMidiOuts) {
                log.info("Close {}", currentMidiOut.getName());
                currentMidiOut.close();
            }
            closeListener();
            removeMainDestinationListener();
            for (MidiInDevice dawMidiIn : dawMidiIns) {
                log.info("Stop listening {}", dawMidiIn.getName());
                dawMidiIn.stopListening();
                log.info("Close {}", dawMidiIn.getName());
                dawMidiIn.close();
            }
        } catch (IOException e) {
            throw new ApplicationError(e);
        }
    }

    private void onDAWEvent(MidiInDevice device, CustomMidiEvent event, MidiOutDevice midiOutDevice) {
        if (event.getMessage()
                .getStatus() <= 0xF0) {
            log.info("Receive from DAW {}: {}", device.getName(), event.toString());
        }

        List<CustomMidiEvent> transformed = midiTransformers.get(device.getName())
                .transform(-1, event);
        for (CustomMidiEvent outputEvent : transformed) {
            if (midiOutDevice != null) {
                if (outputEvent.getMessage()
                        .getStatus() <= 0xF0) {
                    log.info("   Route to {}: {}", midiOutDevice.getName(), outputEvent.toString());
                }
                midiOutDevice.send(outputEvent);
            }
        }
    }

    public void changeOutputChannel(Integer channel) {
        log.info("New output channel in MIDI router: {}", channel);
        outputChannel = channel;
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
        if (mainSourceMidiIn != null) {
            log.info("Stop listening {}", mainSourceMidiIn.getName());
            mainSourceMidiIn.stopListening();
            mainSourceMidiIn.removeListener(mainSourceEventListener);
        }
    }

    private void removeMainDestinationListener() {
        if (mainDestinationMidiIn != null) {
            log.info("Remove listener from {}", mainDestinationMidiIn.getName());
            mainDestinationMidiIn.removeListener(mainDestinationEventListener);
        }
    }

    private void dumpListeners(String name, MidiInDevice device) {
        log.info("#### LISTENERS for {} on {}:", name, device.getName());
        device.getListeners()
                .forEach(l -> log.info("#### LISTENER " + l.getClass()
                        .getName()));
    }

    private void addMainDestinationListener(MidiDeviceDefinition device) {
        if (mainDestinationMidiIn != null) {
            try {
                mainDestinationMidiIn.addListener(mainDestinationEventListener);
                dumpListeners("mainDestinationMidiIn", mainDestinationMidiIn);
                if (!mainDestinationMidiIn.isListening()) {
                    log.info("mainDestinationMidiIn Already listening {}", mainDestinationMidiIn.getName());
                }
                mainDestinationMidiIn.startListening();
            } catch (MidiUnavailableException e) {
                throw new ApplicationError(e);
            }
        }
    }

    private void installMainSourceListenerWithTransformer(MidiDeviceDefinition input, MidiDeviceDefinition output) {
        if (mainSourceMidiIn != null && currentMidiOuts != null) {
            mainSourceMidiIn.addListener(mainSourceEventListener);
            dumpListeners("mainSourceMidiIn", mainSourceMidiIn);
            try {
                midiTransformers.put(mainSourceMidiIn.getName(), new MidiTransformer(input, output, listener));
                if (!mainSourceMidiIn.isListening()) {
                    log.info("mainMidiIn Already listening {}", mainSourceMidiIn.getName());
                }
                mainSourceMidiIn.startListening();
            } catch (MidiUnavailableException e) {
                throw new ApplicationError(e);
            }
        }
    }

}
