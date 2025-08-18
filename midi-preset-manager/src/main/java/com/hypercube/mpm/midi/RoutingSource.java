package com.hypercube.mpm.midi;

import com.hypercube.workshop.midiworkshop.api.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.api.devices.MidiInDevice;
import com.hypercube.workshop.midiworkshop.api.devices.MidiOutDevice;
import com.hypercube.workshop.midiworkshop.api.listener.MidiListener;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@EqualsAndHashCode(of = "port.name")
public final class RoutingSource implements Closeable {
    /**
     * Input device, can be null, in this case the source is just a midi port, no transformers can be used
     */
    @Getter
    private final MidiDeviceDefinition device;
    /**
     * Low level MIDI port (bound {@link #device} if not null)
     */
    private final MidiInDevice port;
    /**
     * RoutingSource towards secondary outputs (DAW) if {@link #device} is not null
     * <p>{@link MidiTransformer} are bound to a specific {@link MidiOutDevice} name
     */
    private final Map<String, MidiTransformer> secondaryTransformers = new HashMap<>();

    public RoutingSource(MidiDeviceDefinition device, MidiInDevice port) {
        this.device = device;
        this.port = port;
        if (port == null) {
            throw new IllegalArgumentException("Port cannot be null");
        }
    }

    public void startListening() {
        port.startListening();
    }

    public void stopListening() {
        port.stopListening();
    }

    public boolean withDevice() {
        return device != null;
    }

    public void addListener(MidiListener listener) {
        port.addListener(listener);
    }

    public String getPortName() {
        return port.getName();
    }

    public void removeListener(MidiListener listener) {
        port.removeListener(listener);
    }

    public void addSecondaryOutputTransformer(MidiOutDevice secondaryOutput, MidiDeviceDefinition dawDevice, MidiTransformerListener controllerMessageListener) {
        secondaryTransformers
                .put(secondaryOutput.getName(), new MidiTransformer(device, dawDevice, controllerMessageListener));
    }

    public List<CustomMidiEvent> transformToSecondaryOutput(MidiOutDevice secondaryOutput, int outputChannel, CustomMidiEvent event) {
        return secondaryTransformers
                .get(secondaryOutput.getName())
                .transform(outputChannel, event);
    }

    public void removeSecondaryTransformer(String secondaryOutputName) {
        secondaryTransformers.remove(secondaryOutputName);
    }

    public String getDeviceName() {
        return Optional.ofNullable(device)
                .map(MidiDeviceDefinition::getDeviceName)
                .orElse(null);
    }

    @Override
    public void close() throws IOException {
        port.close();
    }
}
