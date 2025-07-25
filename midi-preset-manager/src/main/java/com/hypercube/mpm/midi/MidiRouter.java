package com.hypercube.mpm.midi;

import com.hypercube.mpm.config.ProjectConfiguration;
import com.hypercube.mpm.javafx.error.ApplicationError;
import com.hypercube.workshop.midiworkshop.common.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.common.MidiInDevice;
import com.hypercube.workshop.midiworkshop.common.MidiOutDevice;
import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import com.hypercube.workshop.midiworkshop.common.listener.MidiListener;
import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiDeviceController;
import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiDeviceDefinition;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class is responsible to orchestrate the different MIDI connections inside the application
 * <h3>Midi Routing:</h3>
 * <ul>
 *     <li>We have a main input, the master keyboard</li>
 *     <li>We have a main output, the midi synth we want to play with</li>
 *     <li>We also listen what the midi synth say to notify the GUI (display the CC id of any knob typically)</li>
 *     <li>We also send the midi events to one or more secondary outputs, typically the DAW (to record)</li>
 *     <li>Finally, we also redirect secondary inputs (typically various tracks in the DAW) to the right synth</li>
 * </ul>
 * <h3>Midi CC/NRPN Translation:</h3>
 * All of this go through multiple instances of {@link MidiTransformer} to adapt the controllers events in a right way
 * <lu>
 *     <li><b>What we try to achieve here is to resolve the incompatibilities of various DAWs regarding 14 bits NRPN/CC</b></li>
 *     <li>They just don't know how to display or edit those, so it's better to send 7 bits CC to the DAW only</li>
 *     <li>This class will convert back to the right controller supported by the synth</li>
 *     <li>Of course, we loose precision, but this is better than not being able to edit what we record</li>
 * </lu>
 * <p>The Midi Device Library contains a special device called "DAW", defining which 7 bits CC we use in the DAW
 * <h3>No MIDI Learn:</h3>
 * I strongly believe MIDI learn is a pain in the ass. We don't want that. we want automatic translation based on configuration.
 * This is what we are doing here.
 * <ul>
 *     <li>Automatic controller mapping is based on their name</li>
 *     <li>If a daw uses a 7 bit CC for "Filter Cutoff" and a Novation Submit uses a 14 bit CC for "Filter Cutoff", then an automatic translation will be installed</li>
 *     <li>CC/NRPN ranges are properly scaled using {@link MidiDeviceController#getMinValue()} and {@link MidiDeviceController#getMaxValue()}</li>
 * </ul>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MidiRouter {
    /**
     * For primary input and secondary inputs towards synths
     * <p>{@link MidiTransformer} are bound to a specific {@link MidiInDevice} name
     */
    private final Map<String, MidiTransformer> inputTransformers = new HashMap<>();
    /**
     * For primary input towards secondary outputs (DAW)
     * <p>{@link MidiTransformer} are bound to a specific {@link MidiOutDevice} name
     */
    private final Map<String, MidiTransformer> secondaryTransformers = new HashMap<>();
    /**
     * Configuration, especially, {@link com.hypercube.workshop.midiworkshop.common.sysex.library.MidiDeviceLibrary}
     */
    private final ProjectConfiguration cfg;
    /**
     * Used to protect the access of the list {@link #secondaryOutputs} in a threadsafe way
     */
    private final Object secondaryOutputsGuardian = new Object();
    /**
     * Inputs from the DAW, typically
     */
    private final List<MidiInDevice> secondaryInputs = new ArrayList<>();
    /**
     * This listener will be set by the GUI to display CC or NRPN identifiers received from the MIDI (it's just informative)
     */
    @Setter
    private MidiTransformerListener controllerMessageListener;
    /**
     * Outputs towards the DAW, typically
     */
    private List<MidiOutDevice> secondaryOutputs = new ArrayList<>();
    /**
     * List of secondary outputs given by the GUI (device names, or ports names)
     */
    private List<String> deviceOrPortNames = new ArrayList<>();
    /**
     * Input master Keyboard controller
     */
    private MidiDeviceDefinition mainSource;
    /**
     * {@link MidiInDevice} bound to the master keyboard controller
     */
    private MidiInDevice mainSourceMidiIn;
    /**
     * Output to the synth currently used
     */
    private MidiDeviceDefinition mainDestination;
    // As you should know, in JAVA, two method references pointing to the same method are not equals
    // So it is crucial to use the same all the time
    private final MidiListener mainDestinationEventListener = this::onMainDestinationEvent;
    /**
     * {@link MidiInDevice} bound to the synth currently used
     */
    private MidiInDevice mainDestinationMidiIn;
    /**
     * {@link MidiOutDevice} bound to the synth currently used (only used to display CC information to the user)
     */
    private MidiOutDevice mainDestinationMidiOut;
    /**
     * Override the Midi channel of incoming messages from {@link #mainDestinationMidiIn}
     * <p>Range is [0-15], not [1-16]</p>
     */
    private int outputChannel = 0;
    private final MidiListener mainSourceEventListener = this::onMainSourceEvent;

    /**
     * The GUI run this method on startup
     * <ul>
     *     <li>All devices in the library using "dawMidiDevice" will receive the MIDI traffic from this port</li>
     *     <li>A "special" device called "DAW" (see constant {@link MidiDeviceDefinition#DAW_DEVICE_NAME} defined at the root of the library is used to map controllers</li>
     * </ul>
     * <p>Any CC or NRPN coming from the DAW will be notified to {@link #controllerMessageListener} (it's just informative)</p>
     */
    public void listenDawOutputs() {
        log.info("------------------------------------------------------------------------");
        log.info("Auto-routing DAW outputs to devices:");
        cfg.getMidiDeviceLibrary()
                .getDevices()
                .values()
                .stream()
                .filter(d -> d.getDawMidiDevice() != null && d.getOutputMidiDevice() != null)
                .forEach(outputDevice -> {
                    MidiInDevice dawMidiInDevice = cfg.getMidiDeviceManager()
                            .getInput(outputDevice.getDawMidiDevice())
                            .orElse(null);
                    MidiOutDevice midiOutDevice = cfg.getMidiDeviceManager()
                            .getOutput(outputDevice.getOutputMidiDevice())
                            .orElse(null);
                    if (dawMidiInDevice != null && midiOutDevice != null) {
                        var dawDevice = cfg.getMidiDeviceLibrary()
                                .getDevice(MidiDeviceDefinition.DAW_DEVICE_NAME)
                                .orElse(null);
                        log.info("{} will go to {}", outputDevice.getDawMidiDevice(), outputDevice.getOutputMidiDevice());
                        dawMidiInDevice.open();
                        // TODO: Memory leak on this listener
                        dawMidiInDevice.addListener((device, event) -> onDAWEvent(outputDevice, midiOutDevice, event));
                        secondaryInputs.add(dawMidiInDevice);
                        try {
                            String key = outputDevice.getDeviceName();
                            var dup = inputTransformers.get(key);
                            if (dup != null) {
                                throw new IllegalArgumentException("Transformer already set for " + key + ": " + dup.getOutputDevice()
                                        .getDeviceName());
                            }
                            inputTransformers.put(key, new MidiTransformer(dawDevice, outputDevice, controllerMessageListener));
                            dawMidiInDevice.startListening();
                        } catch (MidiError e) {
                            throw new ApplicationError(e);
                        }
                    }
                });
        log.info("------------------------------------------------------------------------");
    }

    public void changeMainSource(String deviceOrPortName) {
        closeSecondaryOutputs();
        mainSource = cfg.getMidiDeviceLibrary()
                .getDevice(deviceOrPortName)
                .orElse(null);

        var portName = cfg.getMidiDeviceLibrary()
                .getDevice(deviceOrPortName)
                .map(MidiDeviceDefinition::getInputMidiDevice)
                .orElse(deviceOrPortName);

        closeMainSourceListener();
        mainSourceMidiIn = cfg.getMidiDeviceManager()
                .getInput(portName)
                .orElse(null);
        if (mainSourceMidiIn != null) {
            mainSourceMidiIn.open();
            installMainSourceListenerWithTransformer(mainSource, mainDestination);
        }
        openSecondaryOutputs();
    }

    /**
     * The GUI call this method to change the main MIDI destination
     */
    public void changeMainDestination(MidiDeviceDefinition device) {
        closeMainDestinationListener();
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

    /**
     * The GUI call this method to select secondary MIDI destinations
     *
     * @param deviceOrPortNames {@link MidiDeviceDefinition} names or {@link MidiOutDevice} names
     */
    public void changeSecondaryOutputs(List<String> deviceOrPortNames) {
        closeSecondaryOutputs();
        this.deviceOrPortNames = new ArrayList<>(deviceOrPortNames); // make it read/write
        openSecondaryOutputs();
    }

    /**
     * This listener receive messages from the master keyboard controller and redirect the traffic to the currently selected synth
     * <p>A specific {@link MidiTransformer} will take care of CC/NRPN conversion</p>
     */
    public void onMainSourceEvent(MidiInDevice device, CustomMidiEvent event) {
        if (event.getMessage()
                .getStatus() <= 0xF0) {
            log.info("onMainSourceEvent: Receive from {}: {}", device.getName(), event.toString());
        }
        // note that transformed can be empty, especially when NPRN are received in multiple Midi events
        List<CustomMidiEvent> transformed = inputTransformers.get(device.getName())
                .transform(outputChannel, event);
        if (mainDestinationMidiOut != null) {
            redirectTrafficToMainOutput(device, event, transformed);
        }
        redirectTrafficToSecondaryOutputs(device, event);
    }

    public void changeOutputChannel(Integer channel) {
        log.info("New output channel in MIDI router: {}", channel);
        outputChannel = channel;
    }

    public void terminate() {
        log.info("Shutdown Midi Router...");
        try {
            if (mainDestinationMidiOut != null) {
                log.info("Close {}", mainDestinationMidiOut.getName());
                mainDestinationMidiOut.close();
            }
            for (MidiOutDevice currentMidiOut : secondaryOutputs) {
                log.info("Close {}", currentMidiOut.getName());
                currentMidiOut.close();
            }
            closeMainSourceListener();
            closeMainDestinationListener();
            for (MidiInDevice dawMidiIn : secondaryInputs) {
                log.info("Stop listening {}", dawMidiIn.getName());
                dawMidiIn.stopListening();
                log.info("Close {}", dawMidiIn.getName());
                dawMidiIn.close();
            }
        } catch (IOException e) {
            throw new ApplicationError(e);
        }
        log.info("Midi Router terminated.");
    }

    private void closeSecondaryOutputs() {
        deviceOrPortNames.stream()
                .map(deviceOrPortName -> cfg.getMidiDeviceLibrary()
                        .getDevice(deviceOrPortName))
                .flatMap(Optional::stream)
                .forEach(device -> secondaryTransformers.remove(device.getOutputMidiDevice()));
        synchronized (secondaryOutputsGuardian) {
            for (MidiOutDevice midiOut : secondaryOutputs) {
                try {
                    midiOut.close();
                } catch (IOException e) {
                    log.warn("Unable to close {}", midiOut.getName());
                }
            }
            secondaryOutputs.clear();
        }
        deviceOrPortNames.clear();
    }

    private void openSecondaryOutputs() {
        // open outputs
        secondaryOutputs = deviceOrPortNames.stream()
                .map(deviceOrPortName -> cfg.getMidiDeviceLibrary()
                        .getDevice(deviceOrPortName)
                        .map(MidiDeviceDefinition::getInputMidiDevice)
                        .orElse(deviceOrPortName))
                .map(cfg.getMidiDeviceManager()::getOutput)
                .flatMap(Optional::stream)
                .collect(Collectors.toList());

        // install transformers
        cfg.getMidiDeviceLibrary()
                .getDevice(MidiDeviceDefinition.DAW_DEVICE_NAME)
                .ifPresent(dawDevice -> {
                    secondaryOutputs.forEach(out -> secondaryTransformers.put(out.getName(), new MidiTransformer(mainSource, dawDevice, controllerMessageListener)));
                });

        synchronized (secondaryOutputsGuardian) {
            for (MidiOutDevice midiOut : secondaryOutputs) {
                midiOut.open();
            }
        }
    }

    /**
     * This listener receive message from the currently selected synth.
     * <p>We just notify the GUI about what we receive</p>
     */
    private void onMainDestinationEvent(MidiInDevice midiInDevice, CustomMidiEvent event) {
        if (event.getMessage()
                .getStatus() <= 0xF0) {
            log.info("onMainDestinationEvent: Receive from {}: {}", mainDestination.getDeviceName(), event.toString());
        }
    }

    /**
     * Send events to the secondary outputs (typically the DAW)
     * <p>If one of them is the same as the input, we send the origin event instead of the transformed ones</p>
     */
    private void redirectTrafficToSecondaryOutputs(MidiInDevice device, CustomMidiEvent event) {
        synchronized (secondaryOutputsGuardian) {
            for (MidiOutDevice midiOut : secondaryOutputs) {
                if (midiOut.getName()
                        .equals(device.getName())) {
                    log.info("'As Is' passthru " + midiOut.getName());
                    midiOut.send(event);
                } else {
                    List<CustomMidiEvent> transformed = secondaryTransformers.get(midiOut.getName())
                            .transform(outputChannel, event);
                    for (var evt : transformed) {
                        log.info("Transformed passthru {}: {}", midiOut.getName(), evt.toString());
                        midiOut.send(evt);
                    }
                }
            }
        }
    }

    /**
     * Send events to the main output (the currently selected synth)
     * <p>If output is the same as the input, we send the origin event instead of the transformed ones</p>
     */
    private void redirectTrafficToMainOutput(MidiInDevice device, CustomMidiEvent event, List<CustomMidiEvent> transformed) {
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

    /**
     * This listener redirect traffic from DAW ({@link #secondaryInputs}) to the right synth ({@link #secondaryOutputs})
     * <p>A specific {@link MidiTransformer} will take care of CC/NRPN conversion</p>
     */
    private void onDAWEvent(MidiDeviceDefinition outputDevice, MidiOutDevice midiOutDevice, CustomMidiEvent event) {
        if (event.getMessage()
                .getStatus() <= 0xF0) {
            log.info("Receive from DAW, targeting {}: {}", outputDevice.getDeviceName(), event.toString());
        }

        List<CustomMidiEvent> transformed = inputTransformers.get(outputDevice.getDeviceName())
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

    /**
     * Stop listening the master keyboard controller
     */
    private void closeMainSourceListener() {
        if (mainSourceMidiIn != null) {
            log.info("Stop listening {}", mainSourceMidiIn.getName());
            mainSourceMidiIn.stopListening();
            mainSourceMidiIn.removeListener(mainSourceEventListener);
        }
    }

    /**
     * Stop listening the selected synth
     */
    private void closeMainDestinationListener() {
        if (mainDestinationMidiIn != null) {
            log.info("Stop listening {}", mainDestinationMidiIn.getName());
            mainDestinationMidiIn.stopListening();
            mainDestinationMidiIn.removeListener(mainDestinationEventListener);
        }
    }

    /**
     * Start listening the selected synth
     */
    private void addMainDestinationListener(MidiDeviceDefinition device) {
        if (mainDestinationMidiIn != null) {
            try {
                mainDestinationMidiIn.addListener(mainDestinationEventListener);
                mainDestinationMidiIn.startListening();
            } catch (MidiError e) {
                throw new ApplicationError(e);
            }
        }
    }

    /**
     * Start listening the master keyboard controller
     * <p>{@link #controllerMessageListener} will be notified when a CC or NRPN is detected</p>
     */
    private void installMainSourceListenerWithTransformer(MidiDeviceDefinition input, MidiDeviceDefinition output) {
        if (mainSourceMidiIn != null && secondaryOutputs != null) {
            mainSourceMidiIn.addListener(mainSourceEventListener);
            try {
                inputTransformers.put(mainSourceMidiIn.getName(), new MidiTransformer(input, output, controllerMessageListener));
                mainSourceMidiIn.startListening();
            } catch (MidiError e) {
                throw new ApplicationError(e);
            }
        }
    }

    private void dumpListeners(String name, MidiInDevice device) {
        log.info("#### LISTENERS for {} on {}:", name, device.getName());
        device.getListeners()
                .forEach(l -> log.info("#### LISTENER " + l.getClass()
                        .getName()));
    }


}
