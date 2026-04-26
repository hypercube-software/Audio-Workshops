package com.hypercube.mpm.midi.group;

import com.hypercube.mpm.config.ConfigurationService;
import com.hypercube.workshop.midiworkshop.api.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.api.ports.local.out.MidiOutPort;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
public class MidiOutPortGroup extends MidiPortGroup<MidiOutPort> {

    public MidiOutPortGroup(ConfigurationService configurationService, String groupName, Consumer<MidiOutPort> openCallback, Consumer<MidiDeviceDefinition> closeCallback) {
        super(configurationService, groupName, openCallback, closeCallback);
    }

    public void send(CustomMidiEvent event) {
        forEach(midiPort -> midiPort.send(event));
    }

    @Override
    protected Optional<MidiOutPort> getPort(String name) {
        return getCfg().getMidiPortsManager()
                .getOutput(name);
    }

    @Override
    protected String getPort(MidiDeviceDefinition device) {
        return device.getOutputMidiDevice();
    }
}
