package com.hypercube.mpm.midi.group;

import com.hypercube.mpm.config.ConfigurationService;
import com.hypercube.workshop.midiworkshop.api.ports.local.in.MidiInPort;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
public class MidiInPortGroup extends MidiPortGroup<MidiInPort> {

    public MidiInPortGroup(ConfigurationService configurationService, String groupName, Consumer<MidiInPort> openCallback, Consumer<MidiDeviceDefinition> closeCallback) {
        super(configurationService, groupName, openCallback, closeCallback);
    }

    @Override
    protected Optional<MidiInPort> getPort(String name) {
        return getCfg().getMidiPortsManager()
                .getInput(name);
    }

    @Override
    protected String getPort(MidiDeviceDefinition device) {
        return device.getInputMidiDevice();
    }
}
