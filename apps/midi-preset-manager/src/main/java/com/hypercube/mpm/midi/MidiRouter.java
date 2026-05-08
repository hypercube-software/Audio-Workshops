package com.hypercube.mpm.midi;

import com.hypercube.mpm.config.ConfigurationService;
import com.hypercube.mpm.javafx.error.ApplicationError;
import com.hypercube.mpm.midi.group.MidiOutPortGroup;
import com.hypercube.workshop.midiworkshop.api.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.listener.MidiListener;
import com.hypercube.workshop.midiworkshop.api.listener.SysExMidiListener;
import com.hypercube.workshop.midiworkshop.api.ports.local.in.MidiInPort;
import com.hypercube.workshop.midiworkshop.api.ports.local.out.MidiOutPort;
import com.hypercube.workshop.midiworkshop.api.sysex.library.MidiDeviceLibrary;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceController;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
public class MidiRouter {
    /**
     * For primary input and secondary inputs towards synths
     * <p>{@link MidiTransformer} are bound to a specific {@link MidiInPort} name
     */
    private final Map<String, MidiTransformer> inputTransformers = new ConcurrentHashMap<>();

    /**
     * Configuration, especially, {@link MidiDeviceLibrary}
     */
    private final ConfigurationService configurationService;
    /**
     * Inputs from the DAW, typically
     */
    private final List<MidiInPort> secondaryInputs = new ArrayList<>();
    /**
     * Input master Keyboard controllers, the map key is the input midi port name
     */
    private final Map<String, RoutingSource> sources = new HashMap<>();
    private final MidiOutPortGroup devicePassThrough;
    private final MidiOutPortGroup inputsPassThrough;
    /**
     * This listener will be set by the GUI to display CC or NRPN identifiers received from the MIDI (it's just informative)
     */
    @Setter
    private MidiTransformerListener controllerMessageListener;

    /**
     * Output to the synth currently used
     */
    private MidiDeviceDefinition mainDestination;

    // As you should know, in JAVA, two method references pointing to the same method are not equals
    // So it is crucial to use the same all the time
    private final MidiListener mainDestinationEventListener = new SysExMidiListener(this::onMainDestinationEvent);
    /**
     * {@link MidiInPort} bound to the INPUT of the synth currently used
     */
    private MidiInPort mainDestinationMidiIn;
    /**
     * {@link MidiOutPort} bound to the OUTPUT of the synth currently used (only used to display CC information to the user)
     */
    private MidiOutPort mainDestinationMidiOut;
    /**
     * The user can temporarily disable the output
     */
    private boolean mainDestinationMidiOutMuted;
    /**
     * Override the Midi channel of incoming messages from {@link #mainDestinationMidiIn}
     * <p>Range is [0-15], not [1-16]</p>
     */
    private int outputChannel = 0;
    private final MidiListener mainSourceEventListener = new SysExMidiListener(this::onMainSourceEvent);

    public MidiRouter(ConfigurationService configurationService) {
        this.configurationService = configurationService;
        this.devicePassThrough = new MidiOutPortGroup(configurationService, "device passthrough", null, null);
        this.inputsPassThrough = new MidiOutPortGroup(configurationService, "inputs passthrough", this::onInputsPassthroughOpen, this::onInputsPassThroughClosed);
    }


    /**
     * Changes the MIDI channel of a MidiEvent.
     * System messages (SysEx, Meta) are returned as-is since they don't have channels.
     *
     * @param event      The source MidiEvent
     * @param newChannel The target channel (0-15)
     * @return A new MidiEvent with the updated channel, or the original if not applicable.
     */
    public static CustomMidiEvent enforceMidiChannel(CustomMidiEvent event, int newChannel) {
        if (event == null || event.getMessage() == null) return event;

        MidiMessage message = event.getMessage();
        byte[] data = message.getMessage();

        // Status byte interpretation:
        // 0x80 to 0xEF are channel-based messages.
        // 0xF0 and above are System Messages (No channel).
        int status = data[0] & 0xFF;

        if (status < 0xF0) {
            try {
                int command = status & 0xF0; // Extract the command (e.g., 0x90 for Note On)
                int channel = newChannel & 0x0F; // Ensure channel is within 0-15 range

                ShortMessage newMessage = new ShortMessage();

                // Set message based on existing data length
                if (data.length <= 1) {
                    newMessage.setMessage(command | channel);
                } else if (data.length == 2) {
                    newMessage.setMessage(command | channel, data[1], 0);
                } else {
                    newMessage.setMessage(command | channel, data[1], data[2]);
                }

                return new CustomMidiEvent(new MidiEvent(newMessage, event.getTick()).getMessage());
            } catch (InvalidMidiDataException e) {
                throw new MidiError(e);
            }
        }

        // Return original for SysEx or MetaMessages
        return event;
    }

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
        var cfg = configurationService.getProjectConfiguration();
        cfg.getMidiDeviceLibrary()
                .getDevices()
                .values()
                .stream()
                .filter(d -> d.getDawMidiDevice() != null && d.getOutputMidiDevice() != null)
                .forEach(outputDevice -> {
                    MidiInPort dawHardwareMidiInPort = cfg.getMidiPortsManager()
                            .getInput(outputDevice.getDawMidiDevice())
                            .orElse(null);
                    MidiOutPort midiOutPort = cfg.getMidiPortsManager()
                            .getOutput(outputDevice.getOutputMidiDevice())
                            .orElse(null);
                    if (dawHardwareMidiInPort != null && midiOutPort != null) {
                        var dawDevice = cfg.getMidiDeviceLibrary()
                                .getDevice(MidiDeviceDefinition.DAW_DEVICE_NAME)
                                .orElse(null);
                        log.info("{} will go to {}", outputDevice.getDawMidiDevice(), outputDevice.getOutputMidiDevice());
                        dawHardwareMidiInPort.open();
                        // TODO: Memory leak on this listener
                        dawHardwareMidiInPort.addListener((device, event) -> onDAWEvent(outputDevice, midiOutPort, event));
                        secondaryInputs.add(dawHardwareMidiInPort);
                        try {
                            String key = outputDevice.getDeviceName();
                            var dup = inputTransformers.get(key);
                            if (dup != null) {
                                log.warn("Transformer already set for {}: {}, overriding...", key, dup.getOutputDevice()
                                        .getDeviceName());
                            }
                            inputTransformers.put(key, new MidiTransformer(dawDevice, outputDevice, controllerMessageListener));
                        } catch (MidiError e) {
                            throw new ApplicationError(e);
                        }
                    }
                });
        log.info("------------------------------------------------------------------------");
    }

    public RoutingSource getRoutingSourceByName(String deviceOrPortName) {
        var cfg = configurationService.getProjectConfiguration();

        Optional<MidiDeviceDefinition> optionalDevice = cfg.getMidiDeviceLibrary()
                .getDevice(deviceOrPortName);

        var device = optionalDevice.orElse(null);

        var portName = optionalDevice
                .map(MidiDeviceDefinition::getInputMidiDevice)
                .orElse(deviceOrPortName);

        var port = cfg.getMidiPortsManager()
                .getInput(portName)
                .orElseThrow(() ->
                        optionalDevice.map(d -> new MidiError("The port '" + portName +
                                        "' declared in the device '" + d.getDeviceName() + "' does not exists in the system. If you just plugged in the device, restart this application"))
                                .orElse(new MidiError("The port " + portName +
                                        " does not exists in the system. If you just plugged in the device, restart this application")));
        return new RoutingSource(device, port);
    }

    /**
     * The GUI controller call this method to change the master inputs (MIDI controller devices)
     * <p>Secondary output need to be restarted because their transformers are not the same anymore</p>
     */
    public void changeMasterInputs(List<String> masterInputsNames) {
        List<RoutingSource> newRoutingSources = masterInputsNames.stream()
                .map(this::getRoutingSourceByName)
                .toList();

        List<RoutingSource> routingSourcesToClose = sources.values()
                .stream()
                .filter(routingSource -> !newRoutingSources.contains(routingSource))
                .toList();
        List<RoutingSource> routingSourcesAlreadyOpen = sources.values()
                .stream()
                .filter(newRoutingSources::contains)
                .toList();
        routingSourcesToClose.forEach(routingSource -> {
            try {
                routingSource.close();
            } catch (IOException e) {
                throw new MidiError(e);
            }
        });
        closeMainSourceListener();
        sources.clear();
        newRoutingSources
                .forEach(routingSource -> {
                    try {
                        if (!routingSourcesAlreadyOpen.contains(routingSource)) {
                            routingSource.open();
                        }
                        sources.put(routingSource.getPortName(), routingSource);
                    } catch (MidiError e) {
                        log.error("Unable to open routing source for {}", routingSource.getDeviceName());
                    }
                });

        installMainSourceListenerWithTransformer(mainDestination);
        inputsPassThrough.restart();
    }

    /**
     * The GUI controller call this method to change the main MIDI destination
     * <p>Not only we open the MIDI out of this device, but also the MIDI in</p>
     */
    public void changeMainDestination(MidiDeviceDefinition device) {
        var cfg = configurationService.getProjectConfiguration();

        closeMainDestinationListener();
        closeMainOutput();
        if (device != null) {
            mainDestination = device;
            mainDestinationMidiOut = cfg.getMidiPortsManager()
                    .getOutput(device.getOutputMidiDevice())
                    .orElse(null);
            mainDestinationMidiIn = cfg.getMidiPortsManager()
                    .getInput(device.getInputMidiDevice())
                    .orElse(null);
            if (mainDestinationMidiOut != null) {
                mainDestinationMidiOut.open();
                installMainSourceListenerWithTransformer(mainDestination);
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
     * @param deviceOrPortNames {@link MidiDeviceDefinition} names or {@link MidiOutPort} names
     */
    public void changeSecondaryOutputs(List<String> deviceOrPortNames) {
        inputsPassThrough.changePorts(deviceOrPortNames);
    }

    public void changeDevicePassThrough(List<String> deviceOrPortNames) {
        devicePassThrough.changePorts(deviceOrPortNames);
    }

    /**
     * This listener receive messages from the master keyboard controller and redirect the traffic to the currently selected synth
     * <p>A specific {@link MidiTransformer} will take care of CC/NRPN conversion</p>
     */
    public void onMainSourceEvent(MidiInPort midiInPort, CustomMidiEvent event) {
        String deviceName = midiInPort.getName();
        if (event.getMessage()
                .getStatus() <= 0xF0) {
            log.info("onMainSourceEvent: Receive from {}: {}", deviceName, event);
        }
        RoutingSource source = sources.get(deviceName);

        // note that transformed can be empty, especially when NPRN are received in multiple Midi events
        CustomMidiEvent inputEventWithOutputChannel = enforceMidiChannel(event, outputChannel);
        List<CustomMidiEvent> transformed = Optional.ofNullable(inputTransformers.get(deviceName))
                .map(t -> t.transform(outputChannel, inputEventWithOutputChannel))
                .orElse(List.of(inputEventWithOutputChannel));
        if (mainDestinationMidiOut != null && !mainDestinationMidiOutMuted) {
            redirectTrafficToMainOutput(source, event, transformed);
        }
        redirectTrafficToSecondaryOutputs(source, event);
    }

    public void changeOutputChannel(Integer channel) {
        log.info("New output channel in MIDI router: {}", channel);
        outputChannel = channel;
    }

    public void terminate() {
        log.info("Shutdown Midi Router...");
        try {
            if (mainDestinationMidiOut != null) {
                log.info("Close Main output: '{}'", mainDestinationMidiOut.getName());
                mainDestinationMidiOut.close();
            }
            inputsPassThrough.closePorts();
            for (RoutingSource source : sources.values()) {
                log.info("Close Routine Source input: '{}'", source.getPortName());
                source.close();
            }
            closeMainDestinationListener();
            for (MidiInPort dawMidiIn : secondaryInputs) {
                log.info("Close DAW input '{}'", dawMidiIn.getName());
                dawMidiIn.close();
            }

        } catch (IOException e) {
            throw new ApplicationError(e);
        }
        log.info("Midi Router terminated.");
    }

    public void mute(String device, boolean mute) {
        if (mainDestination != null && mainDestination.getDeviceName()
                .equals(device)) {
            mainDestinationMidiOutMuted = mute;
        }
        inputsPassThrough.mute(device, mute);
        devicePassThrough.mute(device, mute);
    }

    public void sendToMainDestination(CustomMidiEvent outputEvent) {
        if (mainDestinationMidiOut != null && !mainDestinationMidiOutMuted) {
            mainDestinationMidiOut.send(outputEvent);
        }
    }

    private void onInputsPassThroughClosed(MidiDeviceDefinition midiDeviceDefinition) {
        sources.values()
                .forEach(src -> src.removeSecondaryTransformer(midiDeviceDefinition.getOutputMidiDevice()));
    }

    private void closeMainOutput() {
        if (mainDestinationMidiOut != null) {
            mainDestinationMidiOut.close();
        }
    }

    private void onInputsPassthroughOpen(MidiOutPort midiOutPort) {
        // install transformers
        var cfg = configurationService.getProjectConfiguration();
        cfg.getMidiDeviceLibrary()
                .getDevice(MidiDeviceDefinition.DAW_DEVICE_NAME)
                .ifPresent(dawDevice -> sources.values()
                        .forEach(src -> src.addSecondaryOutputTransformer(midiOutPort, dawDevice, controllerMessageListener)));
    }

    /**
     * This listener receive message from the currently selected synth.
     * <p>We notify the GUI about what we receive and forward to selected ports</p>
     */
    private void onMainDestinationEvent(MidiInPort midiInPort, CustomMidiEvent event) {
        if (event.getMessage()
                .getStatus() != ShortMessage.ACTIVE_SENSING && event.getMessage()
                .getStatus() != ShortMessage.TIMING_CLOCK) {
            log.info("onMainDestinationEvent: Receive from {}: {}", mainDestination.getDeviceName(), event.toString());
        }
        devicePassThrough.send(event);
    }

    /**
     * Send events to the secondary outputs (typically the DAW)
     * <p>If one of them is the same as the input, we send the origin event instead of the transformed ones</p>
     */
    private void redirectTrafficToSecondaryOutputs(RoutingSource source, CustomMidiEvent event) {
        inputsPassThrough.forEach(midiOut -> {
            String sourceName = source.withDevice() ? source.getDeviceName() : source.getPortName();
            String targetPort = midiOut.getName();
            if (targetPort
                    .equals(source.getPortName())) {
                log.info("\tSecondaryOutput routed 'As Is' from '{}' to '{}': {}", sourceName, targetPort, event.toString());
                midiOut.send(event);
            } else {
                List<CustomMidiEvent> transformed = source.transformToSecondaryOutput(midiOut, outputChannel, event);
                for (var evt : transformed) {
                    log.info("\tSecondaryOutput routed 'Transformed' from '{}' to '{}': {}", sourceName, targetPort, evt.toString());
                    midiOut.send(evt);
                }
            }
        });
    }

    /**
     * Send events to the main output (the currently selected synth)
     * <p>If output is the same as the input, we send the origin event instead of the transformed ones</p>
     */
    private void redirectTrafficToMainOutput(RoutingSource source, CustomMidiEvent event, List<CustomMidiEvent> transformed) {
        if (mainDestinationMidiOut.getName()
                .equals(source.getPortName())) {
            log.info("\tMainOutput routed 'As Is' to '{}': {}", mainDestination.getDeviceName(), event.toString());
            sendToMainDestination(event);
        } else {
            for (CustomMidiEvent outputEvent : transformed) {
                if (outputEvent.getMessage()
                        .getStatus() <= 0xF0) {
                    log.info("\tMainOutput routed 'Transformed' to '{}': {}", mainDestination.getDeviceName(), outputEvent);
                }
                sendToMainDestination(outputEvent);
            }
        }
    }

    /**
     * This listener redirect traffic from DAW ({@link #secondaryInputs}) to the right synth
     * <p>A specific {@link MidiTransformer} will take care of CC/NRPN conversion</p>
     */
    private void onDAWEvent(MidiDeviceDefinition outputDevice, MidiOutPort midiOutPort, CustomMidiEvent event) {
        if (event.getMessage()
                .getStatus() <= 0xF0) {
            log.info("Receive from DAW, targeting {}: {}", outputDevice.getDeviceName(), event);
        }

        List<CustomMidiEvent> transformed = inputTransformers.get(outputDevice.getDeviceName())
                .transform(-1, event);
        for (CustomMidiEvent outputEvent : transformed) {
            if (midiOutPort != null) {
                if (outputEvent.getMessage()
                        .getStatus() <= 0xF0) {
                    log.info("   Route to {}: {}", midiOutPort.getName(), outputEvent);
                }
                midiOutPort.send(outputEvent);
            }
        }
    }

    /**
     * Stop listening the master keyboard controller
     */
    private void closeMainSourceListener() {
        sources.forEach((inputPortName, src) ->
        {
            log.info("Stop listening {}", src.getPortName());
            src.removeListener(mainSourceEventListener);
        });
    }

    /**
     * Stop listening the selected synth
     */
    private void closeMainDestinationListener() {
        if (mainDestinationMidiIn != null) {
            log.info("Stop Main input '{}'", mainDestinationMidiIn.getName());
            mainDestinationMidiIn.removeListener(mainDestinationEventListener);
            mainDestinationMidiIn.close();
            mainDestinationMidiIn = null;
        }
    }

    /**
     * Start listening the selected synth
     */
    private void addMainDestinationListener(MidiDeviceDefinition device) {
        if (mainDestinationMidiIn != null) {
            try {
                mainDestinationMidiIn.addListener(mainDestinationEventListener);
            } catch (MidiError e) {
                throw new ApplicationError(e);
            }
        }
    }

    /**
     * Start listening the master keyboard controller
     * <p>{@link #controllerMessageListener} will be notified when a CC or NRPN is detected</p>
     */
    private void installMainSourceListenerWithTransformer(MidiDeviceDefinition output) {
        sources.values()
                .forEach(src -> {
                    try {
                        if (src.withDevice()) {
                            log.info("Start listening MIDI Port '{}' for device {}", src.getPortName(), src.getDeviceName());
                        } else {
                            log.info("Start listening MIDI Port '{}'", src.getPortName());
                        }
                        src.addListener(mainSourceEventListener);
                        if (src.withDevice()) {
                            inputTransformers.put(src.
                                    getPortName(), new MidiTransformer(src.getDevice(), output, controllerMessageListener));
                        }
                    } catch (MidiError e) {
                        throw new ApplicationError(e);
                    }
                });
    }

    private void dumpListeners(String name, MidiInPort device) {
        log.info("#### LISTENERS for {} on {}:", name, device.getName());
        device.getListeners()
                .forEach(l -> log.info("#### LISTENER {}", l.getClass()
                        .getName()));
    }


}
